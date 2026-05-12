#!/usr/bin/env bash
# Full rebuild + redeploy: zbuildí Spring Boot aplikáciu cez Gradle, postaví nový Docker
# image, recreatne kontajner coinapp (garantuje, že beží **najnovšia** verzia JAR-u),
# a navyše spustí aj lokálnu coinapp na porte 8185 cez `java -jar` (ekvivalent IntelliJ
# Run). Skript je idempotentný — môžeš ho spúšťať opakovane.
#
# Sekvencia:
#   1. zastaví predošlú lokálnu app (lock na build/libs/*.jar)
#   2. Gradle clean build → nový build/libs/coinapp-0.0.1-SNAPSHOT.jar
#   3. docker compose down (s --reset-db aj DB volume)
#   4. docker compose build coinapp (nový image s čerstvým JAR-om)
#   5. docker compose up -d --force-recreate (3 kontajnery, vždy nová inštancia)
#   6. wait for container health → http://localhost:8030/coinapp/actuator/health
#   7. verify že image z kroku 4 naozaj beží (grep „Tomcat started" v logu)
#   8. spustí lokálnu app v pozadí → http://localhost:8185/coinapp
#
# Použitie:
#   scripts/redeploy.sh                # plný flow (default)
#   scripts/redeploy.sh --skip-tests   # Gradle build bez testov (rýchle)
#   scripts/redeploy.sh --reset-db     # zmaže aj DB volume — Flyway seed nahrá nanovo
#   scripts/redeploy.sh --no-cache     # docker build bez cache (čistý Dockerfile rebuild)
#   scripts/redeploy.sh --no-local     # iba kontajnery, lokálnu app nespúšťa
#   scripts/redeploy.sh --stop-local   # iba zastaví lokálnu app a ukončí
#   scripts/redeploy.sh --logs         # po štarte follow-uje logy coinapp kontajnera
#   scripts/redeploy.sh --help         # vypíše tento help
#
# Flagy sa dajú kombinovať, napr.:
#   scripts/redeploy.sh --skip-tests --reset-db --no-cache

set -euo pipefail

# Farby pre čitateľnejší výstup ($'...' = bash interpretuje escape sekvencie pri definícii,
# takže premenné obsahujú reálne ESC byty a fungujú aj v here-doc)
BLUE=$'\033[1;34m'
GREEN=$'\033[1;32m'
YELLOW=$'\033[1;33m'
RED=$'\033[1;31m'
NC=$'\033[0m'

log()   { printf "${BLUE}==>${NC} %s\n" "$*"; }
ok()    { printf "${GREEN}    ✓${NC} %s\n" "$*"; }
warn()  { printf "${YELLOW}    !${NC} %s\n" "$*"; }
err()   { printf "${RED}    ✗${NC} %s\n" "$*"; }

# Konštanty pre lokálnu app
LOCAL_PORT=8185
LOCAL_HEALTH_URL="http://localhost:${LOCAL_PORT}/coinapp/actuator/health"
LOCAL_PID_FILE="build/local-bootrun.pid"
LOCAL_LOG_FILE="build/local-bootrun.log"

# Defaultné hodnoty
SKIP_TESTS=false
RESET_DB=false
FOLLOW_LOGS=false
NO_LOCAL=false
STOP_LOCAL_ONLY=false
NO_CACHE=false

for arg in "$@"; do
    case "$arg" in
        --skip-tests) SKIP_TESTS=true ;;
        --reset-db)   RESET_DB=true ;;
        --logs)       FOLLOW_LOGS=true ;;
        --no-local)   NO_LOCAL=true ;;
        --stop-local) STOP_LOCAL_ONLY=true ;;
        --no-cache)   NO_CACHE=true ;;
        -h|--help)
            sed -n '2,28p' "$0" | sed 's/^# \{0,1\}//'
            exit 0
            ;;
        *)
            err "Neznámy parameter: $arg (použi --help pre zoznam)"
            exit 1
            ;;
    esac
done

# Preskoč do project root (skript môže byť spustený zo subadresára)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

# ----------------------------------------------------------------------------
# Helpery pre lokálnu app (java -jar v pozadí, PID + log v build/)
# ----------------------------------------------------------------------------

# Vráti cestu k `java` binárke s Java 21+ (JAR je compilenutý s toolchain Java 21).
# Skúša postupne: $JAVA_HOME, /usr/libexec/java_home -v 21 (macOS), default `java` na PATH.
# Pri neúspechu vráti chybu a pošle hlášku ako čo nainštalovať.
find_java_21() {
    local home v binary

    # 1) JAVA_HOME
    if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
        v=$("$JAVA_HOME/bin/java" -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
        if [ "${v:-0}" -ge 21 ] 2>/dev/null; then
            echo "$JAVA_HOME/bin/java"
            return 0
        fi
    fi

    # 2) macOS: /usr/libexec/java_home -v 21
    if [ -x /usr/libexec/java_home ]; then
        home=$(/usr/libexec/java_home -v 21 2>/dev/null || true)
        if [ -n "$home" ] && [ -x "$home/bin/java" ]; then
            echo "$home/bin/java"
            return 0
        fi
    fi

    # 3) default `java` na PATH
    if command -v java >/dev/null 2>&1; then
        v=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
        if [ "${v:-0}" -ge 21 ] 2>/dev/null; then
            command -v java
            return 0
        fi
    fi

    return 1
}

stop_local_app() {
    if [ -f "$LOCAL_PID_FILE" ]; then
        local pid
        pid=$(cat "$LOCAL_PID_FILE")
        if kill -0 "$pid" 2>/dev/null; then
            log "Zastavujem predošlú lokálnu app (PID $pid)"
            kill "$pid" 2>/dev/null || true
            for _ in $(seq 1 15); do
                kill -0 "$pid" 2>/dev/null || break
                sleep 1
            done
            kill -9 "$pid" 2>/dev/null || true
            ok "Lokálna app zastavená"
        fi
        rm -f "$LOCAL_PID_FILE"
    fi
}

start_local_app() {
    # Skontroluj, že nikto iný neblokuje port (napr. IntelliJ ručne spustená app)
    if command -v lsof >/dev/null 2>&1 && lsof -ti :"$LOCAL_PORT" >/dev/null 2>&1; then
        local occupant
        occupant=$(lsof -ti :"$LOCAL_PORT" | head -1)
        err "Port $LOCAL_PORT je obsadený iným procesom (PID $occupant)."
        err "Pravdepodobne máš spustenú appku z IntelliJ — zastav ju, alebo použi --no-local."
        return 1
    fi

    local jar_file
    jar_file=$(ls build/libs/coinapp-*.jar 2>/dev/null | grep -v -- '-plain' | head -1)
    if [ -z "$jar_file" ]; then
        err "Build artefakt build/libs/coinapp-*.jar nenájdený — spusti predtým Gradle build."
        return 1
    fi

    # JAR je kompilovaný cez Gradle toolchain Java 21 — `java` na PATH nemusí stačiť
    local java_bin
    if ! java_bin=$(find_java_21); then
        err "Nenašiel som Java 21+ runtime — JAR je kompilovaný s Java 21 a tvoj 'java' na PATH je staršia verzia."
        err "Riešenia:"
        err "  • macOS: brew install --cask temurin@21"
        err "  • alebo nastav JAVA_HOME na Java 21 JDK"
        err "  • alebo spusti skript s --no-local a appku spusti z IntelliJ"
        return 1
    fi
    local java_ver
    java_ver=$("$java_bin" -version 2>&1 | head -1)

    mkdir -p build
    log "Štartujem lokálnu app: $java_bin -jar $jar_file"
    log "    ($java_ver, profil localdev, port $LOCAL_PORT)"
    # Profil sa neuvádza explicitne — application.yml má `spring.profiles.default: localdev`
    nohup "$java_bin" -jar "$jar_file" > "$LOCAL_LOG_FILE" 2>&1 &
    echo $! > "$LOCAL_PID_FILE"
    ok "Lokálna app spustená v pozadí (PID $(cat "$LOCAL_PID_FILE")), log: $LOCAL_LOG_FILE"

    log "Čakám na health-check ($LOCAL_HEALTH_URL)..."
    local healthy=false
    for i in $(seq 1 120); do
        if curl -sf "$LOCAL_HEALTH_URL" >/dev/null 2>&1; then
            ok "Lokálna app je healthy ($i s)"
            healthy=true
            break
        fi
        # Ak proces zomrel, nečakaj
        if ! kill -0 "$(cat "$LOCAL_PID_FILE")" 2>/dev/null; then
            err "Lokálna app spadla počas štartu — posledné riadky logu:"
            tail -30 "$LOCAL_LOG_FILE" >&2
            return 1
        fi
        sleep 1
    done
    if ! $healthy; then
        err "Lokálna app neodpovedá po 120 s — pozri $LOCAL_LOG_FILE"
        return 1
    fi
}

# ----------------------------------------------------------------------------
# Skratka: --stop-local len zastaví lokálnu app a skončí
# ----------------------------------------------------------------------------
if $STOP_LOCAL_ONLY; then
    stop_local_app
    exit 0
fi

# Sanity check — overíme, že potrebné nástroje sú dostupné
command -v docker >/dev/null || { err "docker nie je v PATH"; exit 1; }
command -v ./gradlew >/dev/null || [ -x ./gradlew ] || { err "./gradlew nie je v project root"; exit 1; }
docker info >/dev/null 2>&1 || { err "Docker daemon nebeží — spusti Docker Desktop"; exit 1; }
# Java 21 check — `find_java_21` pošle hlášku až pri štarte lokálnej app, tu len rýchle upozornenie

# ----------------------------------------------------------------------------
# 1) Pred buildom — zastav predošlú lokálnu app (drží lock na build/libs/...)
# ----------------------------------------------------------------------------
stop_local_app

# ----------------------------------------------------------------------------
# 2) Gradle build (s pred-formátovaním cez Spotless / Palantir)
# ----------------------------------------------------------------------------
log "Spotless apply (auto-format zdrojákov pred buildom)"
./gradlew spotlessApply

if $SKIP_TESTS; then
    log "Gradle build (bez testov)"
    ./gradlew clean build -x test
else
    log "Gradle build (s testami + spotlessCheck)"
    ./gradlew clean build
fi
ok "Build hotový — artefakt: build/libs/coinapp-0.0.1-SNAPSHOT.jar"

# ----------------------------------------------------------------------------
# 3) Stop existujúcich kontajnerov
# ----------------------------------------------------------------------------
if $RESET_DB; then
    log "Zastavujem kontajnery a mažem DB volume (--reset-db)"
    docker compose down -v
    warn "DB volume vymazaný — Flyway pri ďalšom štarte nahrá seed znova"
else
    log "Zastavujem existujúce kontajnery (DB volume zostáva)"
    docker compose down
fi

# ----------------------------------------------------------------------------
# 4) Docker image build (explicitne, vidíš každý krok)
# ----------------------------------------------------------------------------
if $NO_CACHE; then
    log "Buildujem Docker image coinapp (--no-cache, čistý rebuild)"
    docker compose build --no-cache coinapp
else
    log "Buildujem Docker image coinapp z čerstvého JAR-u"
    docker compose build coinapp
fi
ok "Image hotový"

# ----------------------------------------------------------------------------
# 5) Štart stacku s force-recreate (garantuje, že kontajner je novo vytvorený
#    z najnovšieho image-u — nie len reštartnutý)
# ----------------------------------------------------------------------------
log "Štartujem stack (db + keycloak + coinapp) s --force-recreate"
docker compose up -d --force-recreate

# ----------------------------------------------------------------------------
# 6) Wait for container app health
# ----------------------------------------------------------------------------
log "Čakám na kontajnerovú app health-check (http://localhost:8030/coinapp/actuator/health)..."
HEALTHY=false
for i in $(seq 1 90); do
    if curl -sf http://localhost:8030/coinapp/actuator/health >/dev/null 2>&1; then
        ok "Kontajnerová app je healthy ($i s)"
        HEALTHY=true
        break
    fi
    sleep 1
done

if ! $HEALTHY; then
    err "Kontajnerová app neodpovedá po 90 s — pozri 'docker compose logs coinapp'"
    docker compose ps
    exit 1
fi

# ----------------------------------------------------------------------------
# 7) Verify že beží naozaj nový image (z kroku 4)
#    Image ID kontajnera musí súhlasiť s aktuálnym image-om v repo
# ----------------------------------------------------------------------------
log "Overujem, že kontajner beží z najnovšieho image-u"
EXPECTED_IMAGE_ID=$(docker compose images --quiet coinapp 2>/dev/null || true)
RUNNING_IMAGE_ID=$(docker inspect --format='{{.Image}}' coinapp 2>/dev/null | sed 's|sha256:||' | cut -c1-12 || true)
EXPECTED_SHORT=$(echo "$EXPECTED_IMAGE_ID" | cut -c1-12)
if [ -n "$EXPECTED_SHORT" ] && [ -n "$RUNNING_IMAGE_ID" ] && [ "$EXPECTED_SHORT" = "$RUNNING_IMAGE_ID" ]; then
    ok "Image ID match: $RUNNING_IMAGE_ID"
else
    warn "Image ID check skipped/mismatch (running=$RUNNING_IMAGE_ID, expected=$EXPECTED_SHORT)"
fi
STARTUP_LINE=$(docker logs coinapp 2>&1 | grep -E "Tomcat started on port" | tail -1 || true)
if [ -n "$STARTUP_LINE" ]; then
    ok "Tomcat: ${STARTUP_LINE##*--- }"
fi

# ----------------------------------------------------------------------------
# 8) Lokálna app (ekvivalent IntelliJ Run / bootRun) — voliteľné
# ----------------------------------------------------------------------------
if $NO_LOCAL; then
    log "Preskakujem štart lokálnej app (--no-local)"
else
    start_local_app
fi

# ----------------------------------------------------------------------------
# 9) Súhrn
# ----------------------------------------------------------------------------
log "Stav stacku:"
docker compose ps

LOCAL_INFO=""
if ! $NO_LOCAL; then
    LOCAL_INFO="
  • coinapp (lokál)   → http://localhost:8185/coinapp  (PID $(cat "$LOCAL_PID_FILE"), log: $LOCAL_LOG_FILE)
                        Zastav cez: scripts/redeploy.sh --stop-local"
fi

cat <<EOF

${GREEN}Hotovo.${NC} Aplikácia beží:
${LOCAL_INFO}
  • coinapp (docker)  → http://localhost:8030/coinapp  (health: /coinapp/actuator/health, Swagger: /coinapp/swagger-ui.html)
  • keycloak          → http://localhost:8081  (admin / admin)
  • postgres          → jdbc:postgresql://localhost:5432/coinapp  (coinuser / coinpass)
  • JDWP debug        → localhost:5005          (IntelliJ → Remote JVM Debug)

EOF

if $FOLLOW_LOGS; then
    log "Sledujem logy coinapp kontajnera (Ctrl+C ukončí)"
    docker compose logs -f coinapp
fi
