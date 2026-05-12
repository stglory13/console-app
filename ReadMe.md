# <a name="home" id="home"></a>CoinApp — Coin Account Application

- [Ako spustiť](#AkoSpustit)
- [Biznis a technický popis](#BiznisATechnickyPopis)
- [Konzumované služby a zdroje (závislosti)](#KonzumovaneSluzbyAZdroje)
- [Publikované služby](#PublikovaneSluzby)
- [Použité technológie](#PouziteTechnologie)
- [Moduly](#Moduly)
- [Build projektu](#BuildProjektu)
- [Konfigurácia](#Konfiguracia)
- [Alternatívne profily a nastavenia pre lokálny vývoj](#AlternativneProfily)
    - [Spring profily](#SpringProfily)
- [Lokálne spustenie](#LokalneSpustenie)
- [Prevádzka](#Prevadzka)
    - [Manažment](#Manazment)
    - [Operatíva](#Operativa)
    - [Monitoring](#Monitoring)

## <a name="AkoSpustit" id="AkoSpustit"></a>Ako spustiť  [&#8593;](#home)

### Profily a porty

| Prostredie | Aktívny profil | Port (vnútri aplikácie) | Mapping (host:kontajner) | Mapping debug portu (host:kontajner) | Prístup z hostiteľského OS |
|---|---|---|---|---|---|
| IntelliJ / `bootRun` | `localdev` (default) | **8185** | — (natívne) | — (natívne, debug-run v IntelliJ) | http://localhost:8185 |
| `docker compose up` | `docker` | **8080** | `8030:8080` | `5005:5005` (JDWP) | http://localhost:8030 |
| Keycloak (kontajner) | — | **8080** | `8081:8080` | — | http://localhost:8081 |
| PostgreSQL (kontajner) | — | **5432** | `5432:5432` | — | `jdbc:postgresql://localhost:5432/coinapp` |

Porty sú zámerne rozdielne medzi lokálnym a kontajnerovým behom — možno spustiť obidva paralelne (dev z IntelliJ na 8185 proti "produkčnému" kontajneru na 8030). Pripojenie debuggera na kontajner: viď [Pripojenie debuggera k bežiacemu kontajneru](#Operativa).

### Predkonfigurovaní Keycloak používatelia (realm `coinapp`)

| Username | Heslo | Role |
|---|---|---|
| `admin@example.com` | `admin` | `ADMIN`, `USER` |
| `user@example.com`  | `user`  | `USER` |

Realm a používatelia sa importujú z [`keycloak/coinapp-realm.json`](keycloak/coinapp-realm.json) pri prvom štarte Keycloaku.

### Pripojenie k PostgreSQL (z hostiteľského OS)

| Pole | Hodnota |
|---|---|
| Host | `localhost` |
| Port | `5432` |
| Database | `coinapp` |
| User | `coinuser` |
| Password | `coinpass` |

> Heslá v `docker-compose.yml` sú len pre lokálny vývoj. V produkcii cez secrets.

### Verejné endpointy (bez autentifikácie)

Endpointy povolené v `SecurityConfig` ako `permitAll()` — slúžia na health-check (Docker `HEALTHCHECK`, k8s probes) a OpenAPI dokumentáciu.

| Endpoint | Účel | Lokál (8185) | Docker (8030) |
|---|---|---|---|
| `/actuator/health` | Spring Boot health check (status DB, disk, ...) | http://localhost:8185/actuator/health | http://localhost:8030/actuator/health |
| `/swagger-ui.html` | Swagger UI — interaktívna dokumentácia API | http://localhost:8185/swagger-ui.html | http://localhost:8030/swagger-ui.html |
| `/v3/api-docs` | OpenAPI 3 JSON spec | http://localhost:8185/v3/api-docs | http://localhost:8030/v3/api-docs |

> `SecurityConfig` povoľuje aj `/actuator/health/**` (liveness/readiness probes pre k8s), ale tie nie sú v `application.yml` aktivované. Zapnúť cez `management.endpoint.health.probes.enabled: true` ak ich potrebuješ.

> V Swagger UI klikni **Authorize** a vlož samotný JWT access token (bez prefixu `Bearer `) — získanie tokenu cez Keycloak je popísané v sekcii [Lokálne spustenie → Príklady volaní](#LokalneSpustenie).

Všetky ostatné endpointy (`/v1/**`) vyžadujú **Bearer JWT** v `Authorization` hlavičke — viď [Publikované služby](#PublikovaneSluzby).

### 1) Lokálny beh z IntelliJ / Gradle (aplikácia natívne na hostiteľskom OS, DB + Keycloak v kontajneroch)

```bash
docker compose up -d db keycloak
# počkaj ~20s na Keycloak (importuje realm)
./gradlew bootRun
# konzola: "Tomcat started on port(s): 8185 (http)"
curl http://localhost:8185/actuator/health
```

### 2) Plne v kontajneroch

```bash
./gradlew clean build -x test
docker compose up --build
docker ps | grep coinapp        # 0.0.0.0:8030->8080/tcp
curl http://localhost:8030/actuator/health
```

## <a name="BiznisATechnickyPopis" id="BiznisATechnickyPopis"></a>Biznis a technický popis  [&#8593;](#home)

Demo Spring Boot aplikácia, ktorá simuluje jednoduchú finančnú knihu (ledger) — spravuje účty a presuny prostriedkov medzi nimi.

### Doménový model

- **Účet** (`Account`) — verejný GUID identifikátor, meno, povolené prečerpanie (`maximalOverdraft`) a aktuálny zostatok (`currentBalance`).
- **Transakcia** (`Ledger`) — záznam o presune prostriedkov medzi dvomi účtami so zachovanou históriou (suma, zostatok po, čas, popis).

Pri každej transakcii sa kontroluje, že suma je kladná a že zostatok zdrojového účtu po odpočítaní neklesne pod limit povoleného prečerpania. Audit zmien na entitách `Account` a `Ledger` zaznamenáva **Hibernate Envers** do `_aud` tabuliek. Konkurenčné úpravy sú chránené **optimistic locking** cez stĺpec `version`.

### Členenie doménového modelu

- `api/` — REST kontroléry + DTO
- `service/` — biznis logika (`AccountService`, `LedgerService`)
- `model/` — JPA entity (`Account`, `Ledger`)
- `repos/` — Spring Data repository
- `config/` — `ApiPaths`, `SecurityConfig`, `OpenApiConfig`
- `exception/` — vlastné výnimky + globálny handler
- `logging/` — SLF4J markery (`BUSINESS_MARKER`, `PAYLOAD_MARKER`, `TECHNICAL_MARKER`)

## <a name="KonzumovaneSluzbyAZdroje" id="KonzumovaneSluzbyAZdroje"></a>Konzumované služby a zdroje (závislosti)  [&#8593;](#home)

| Závislosť | Účel |
|---|---|
| **PostgreSQL 16** | aplikačná DB (Flyway DDL + seed data) |
| **Keycloak 25** | Identity Provider, vystavuje JWT access tokeny pre realm `coinapp` |

## <a name="PublikovaneSluzby" id="PublikovaneSluzby"></a>Publikované služby  [&#8593;](#home)

Bázová cesta: `/v1/account`. Všetky `/v1/**` volania vyžadujú **Bearer JWT** v hlavičke `Authorization`.

| Metóda | Endpoint | Popis | Status | Rola |
|---|---|---|---|---|
| `GET` | `/v1/account/{guid}` | Vráti detail účtu | `200 OK` | `ADMIN` alebo `USER` |
| `POST` | `/v1/account/tx` | Vykoná transakciu medzi účtami | `201 CREATED` | `ADMIN` |

**Otvorené (bez auth) endpointy:**

- `/actuator/health` — pre health-check (Docker `HEALTHCHECK`, k8s probes)
- `/v3/api-docs`, `/v3/api-docs/**` — OpenAPI definícia
- `/swagger-ui.html`, `/swagger-ui/**` — Swagger UI

### Mapovanie chýb na HTTP stavy

Centrálny [`GlobalExceptionHandler`](src/main/java/st/coinaccountapp/exception/GlobalExceptionHandler.java) prekladá výnimky na konzistentné HTTP odpovede:

| HTTP status | Spúšťacia výnimka / situácia | Telo odpovede |
|---|---|---|
| `400 Bad Request` | `BiznisValidationFailedException` — prekročený limit prečerpania | message z výnimky |
| `400 Bad Request` | `MethodArgumentNotValidException` — `@Valid` na request body padol (chýbajúce pole, `amount ≤ 0`, ...) | konkatenovaný zoznam field error-ov `field: message; ...` |
| `401 Unauthorized` | žiadny `Authorization` header alebo neplatný JWT *(Spring Security, mimo handler)* | default Spring |
| `403 Forbidden` | `AccessDeniedException` — platný token bez potrebnej role (napr. `USER` na `POST /tx`) | message z výnimky |
| `404 Not Found` | `NotFoundException` — entita s daným GUID-om neexistuje | `Data not found: <guid>` |
| `409 Conflict` | `ObjectOptimisticLockingFailureException` — konflikt pri konkurenčnej zmene (`version` stĺpec) | `Concurrent update conflict: <detail>` |
| `500 Internal Server Error` | akákoľvek neočakávaná výnimka (fallback) | `An unexpected error occurred: <message>` |

### Swagger / OpenAPI

Po spustení aplikácie (URL závisí od prostredia — viď [Ako spustiť](#AkoSpustit)):

- Swagger UI: http://localhost:8185/swagger-ui.html (lokál) · http://localhost:8030/swagger-ui.html (docker)
- OpenAPI JSON: http://localhost:8185/v3/api-docs (lokál) · http://localhost:8030/v3/api-docs (docker)

V Swagger UI klikni **Authorize** a vlož samotný JWT access token (bez prefixu `Bearer `).

## <a name="PouziteTechnologie" id="PouziteTechnologie"></a>Použité technológie  [&#8593;](#home)

| Knižnica / nástroj | Verzia | Poznámka |
|---|---|---|
| Java | 21 | Eclipse Temurin |
| Spring Boot | 3.3.4 | Web, Data JPA, Data REST, Validation, Actuator, OAuth2 Resource Server |
| Hibernate ORM | 6.x | JPA provider |
| Hibernate Envers | 6.x | audit `_aud` tabuliek |
| Flyway | core + postgresql | DB migrácie (`db/migration`, `db/dev_data_migration`) |
| PostgreSQL | 16 | runtime DB |
| Keycloak | 25 | Identity Provider (JWT) |
| Lombok | latest | `@RequiredArgsConstructor`, `@Slf4j`, `@Getter/@Setter`, `@NonNull` |
| MapStruct | 1.6.3 | mapovanie entít na DTO (`api/mapper/`) |
| springdoc-openapi | 2.6 | Swagger UI |
| JUnit 5 + Testcontainers | latest | integračné testy nad reálnym PostgreSQL kontajnerom |
| Gradle | 8.10 | build |
| Spotless + Palantir Java Format | 6.25 / 2.50 | automatický code formatter (`./gradlew spotlessApply`) |
| Docker + Docker Compose | — | lokálny dev stack (app + DB + Keycloak) |

## <a name="Moduly" id="Moduly"></a>Moduly  [&#8593;](#home)

Single-module Gradle projekt. Štruktúra zdrojov:

```
src/main/java/st/coinaccountapp/
├── CoinAccountApplication.java   # Spring Boot entry point
├── api/                          # REST kontroléry + DTO
├── service/                      # biznis logika
├── model/                        # JPA entity
├── repos/                        # Spring Data repository
├── config/                       # ApiPaths, SecurityConfig, OpenApiConfig
├── exception/                    # vlastné výnimky + globálny handler
└── logging/                      # SLF4J markery (LogsCategorization)

src/main/resources/
├── application.yml               # base config + default profile, žiadny server.port (Spring default 8080)
├── application-localdev.yml      # localdev profile — IntelliJ lokál, server.port 8185
├── application-docker.yml        # docker profile — kontajner, server.port ostáva 8080
└── db/
    ├── migration/                # Flyway DDL skripty (V1_x)
    └── dev_data_migration/       # Flyway seed dáta (V99_x, len pre dev)
```

## <a name="BuildProjektu" id="BuildProjektu"></a>Build projektu  [&#8593;](#home)

```bash
./gradlew clean build               # spustí aj testy (Testcontainers) a spotlessCheck
./gradlew clean build -x test       # bez testov
./gradlew spotlessApply             # preformátuje všetky Java súbory podľa Palantir Java Format
```

Build artefakt: `build/libs/coinapp-0.0.1-SNAPSHOT.jar` (použitý v `Dockerfile`).

### Code formatting

Projekt používa **Spotless + Palantir Java Format**. Pravidlá:

- 4-space indent, ~120 znakov maximálna dĺžka riadku
- automatické odstránenie nepoužitých importov, trailing whitespace a chýbajúcich newline-ov na konci súboru
- `tasks.named('check')` je naviazaný na `spotlessCheck` — `./gradlew build` zlyhá, ak formát nie je v poriadku

**Reformátuj lokálne:**
```bash
./gradlew spotlessApply
```

**IntelliJ → Reformat on Save:** Settings → Tools → Actions on Save → zaškrtni „Reformat code" + „Optimize imports". Nastaví sa však IntelliJ default formatter — ten sa nezhoduje úplne s Palantirom. Pre 100% zhodu nainštaluj plugin [Palantir Java Format](https://plugins.jetbrains.com/plugin/13180-palantir-java-format) a aktivuj ho v Settings → Editor → Code Style → Java → schema.

## <a name="Konfiguracia" id="Konfiguracia"></a>Konfigurácia  [&#8593;](#home)

### Aplikačné properties

| property.key<br/><br/>PROPERTY_ENV_VAR | Povinná_(*1) | Špecifická_(*2) | Default_value | Popis |
|---|:---:|---|---|---|
| spring.datasource.url<br/><br/>SPRING_DATASOURCE_URL | nie | ENV-SPECIFIC | `jdbc:postgresql://${DB_HOST:localhost}:5432/coinapp` | JDBC URL na PostgreSQL DB |
| --<br/><br/>DB_HOST | nie | ENV-SPECIFIC | `localhost` | Hostname DB (v Compose `db`, lokálne `localhost`) |
| spring.datasource.username<br/><br/>SPRING_DATASOURCE_USERNAME | nie | ENV-SPECIFIC | `coinuser` | DB používateľ |
| spring.datasource.password<br/><br/>SPRING_DATASOURCE_PASSWORD | **áno** | ENV-SPECIFIC, PASSWORD | `coinpass` (len dev) | Heslo DB používateľa |
| spring.security.oauth2.resourceserver.jwt.issuer-uri<br/><br/>KEYCLOAK_ISSUER | **áno** | ENV-SPECIFIC | `http://localhost:8081/realms/coinapp` | Issuer URI Keycloak realmu — Spring validuje `iss` claim JWT-ka |
| spring.security.oauth2.resourceserver.jwt.jwk-set-uri<br/><br/>KEYCLOAK_JWK_SET_URI | **áno** | ENV-SPECIFIC | `http://localhost:8081/realms/coinapp/protocol/openid-connect/certs` | JWKS endpoint — odkiaľ Spring fetchuje verejné kľúče pre overenie podpisu JWT |
| server.port | nie | | `8080` (docker) / `8185` (localdev) | HTTP port aplikácie — viď [Ako spustiť](#AkoSpustit) |
| spring.flyway.locations | nie | | `classpath:db/migration,classpath:db/dev_data_migration` | Cesty k Flyway migráciám (seed dáta sú v `dev_data_migration` — v prod profile by sa mali vylúčiť) |
| spring.jpa.hibernate.ddl-auto | nie | | `validate` | Hibernate DDL stratégia — DDL spravuje Flyway, Hibernate len validuje |

(\*1): *Povinná* — nemá default a aplikácia bez nastavenia nepobeží.
(\*2): *Špecifická* — info či je to _ENV-SPECIFIC_ bez defaultu a/alebo _PASSWORD_.

### Konfiguračné súbory

- [`src/main/resources/application.yml`](src/main/resources/application.yml) — base konfigurácia, spoločné parametrizované nastavenia (DB, Keycloak)
- [`src/main/resources/application-localdev.yml`](src/main/resources/application-localdev.yml) — `localdev` profile (port 8185, DEBUG logging)
- [`src/main/resources/application-docker.yml`](src/main/resources/application-docker.yml) — `docker` profile (kontajner-only odlišnosti)

## <a name="AlternativneProfily" id="AlternativneProfily"></a>Alternatívne profily a nastavenia pre lokálny vývoj  [&#8593;](#home)

### <a name="SpringProfily" id="SpringProfily"></a>Spring profily

| Profile | Účel | Konfigurácia |
|---|---|---|
| `localdev` | Lokálny vývoj z IntelliJ (default v `application.yml`) | `server.port=8185`, DEBUG logging, `health.show-details=always` |
| `docker` | Beh v Docker kontajneri (nastavený v `docker-compose.yml`) | `server.port=8080` (default), `health.show-details=when-authorized` |

Integračné testy bežia tiež s `localdev` profilom; DB endpoint si Testcontainers vloží automaticky cez `@ServiceConnection` a Keycloak JWT validáciu prebije stub `JwtDecoder` (`testsupport/IntegrationTestSecurityConfig`).

## <a name="LokalneSpustenie" id="LokalneSpustenie"></a>Lokálne spustenie  [&#8593;](#home)

### 1) Lokálne z IDE (aplikácia natívne na hostiteľskom OS, DB + Keycloak v kontajneroch)

```bash
docker compose up -d db keycloak
# počkaj ~20s, kým Keycloak nabootuje a importuje realm
# spusti CoinAccountApplication z IDE (default profile = localdev)
```

App beží na http://localhost:8185 (profil `localdev`), Keycloak na http://localhost:8081.

### 2) Plne v kontajneroch

```bash
./gradlew clean build -x test
docker compose up --build
```

App beží na http://localhost:8030 (profil `docker`, mapping `8030:8080`).

### 3) Testy

```bash
./gradlew test
```

Integračné testy automaticky vytvoria dočasný PostgreSQL kontajner cez Testcontainers; Keycloak nie je potrebný.

### Príklady volaní

Získanie access tokenu (Resource Owner Password Credentials grant):

```bash
TOKEN=$(curl -s -X POST http://localhost:8081/realms/coinapp/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=coinapp-api" \
  -d "username=admin@example.com" \
  -d "password=admin" \
  | jq -r .access_token)
```

Detail účtu (rola `ADMIN` alebo `USER`) — nahraď `8185` za `8030`, ak aplikácia beží v kontajneri:

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8185/v1/account/8d9d35e2-15b3-4fad-b853-f5731e9e19fa
```

Transakcia (len `ADMIN`):

```bash
curl -H "Authorization: Bearer $TOKEN" -X POST \
  http://localhost:8185/v1/account/tx \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountGuid": "6eb7e588-5d85-4285-8c64-3be32a70393b",
    "toAccountGuid":   "d1e39c65-48c9-42ef-9c50-8dd5a072e510",
    "amount": 50.00,
    "description": "Mesačný presun"
  }'
```

## <a name="Prevadzka" id="Prevadzka"></a>Prevádzka  [&#8593;](#home)

### <a name="Manazment" id="Manazment"></a>Manažment

| Príkaz | Účel |
|---|---|
| `docker compose up -d db keycloak` | spusti len DB + Keycloak (pre lokálny dev z IDE) |
| `docker compose up` | spusti celý stack (app + DB + Keycloak) |
| `docker compose down` | zhasni všetko |
| `docker compose down -v` | zhasni + vymaž data volume (reset DB) |

### <a name="Operativa" id="Operativa"></a>Operatíva

#### Prístup k databáze

Pripájacie údaje k DB sú uvedené v sekcii [Ako spustiť → Pripojenie k PostgreSQL](#AkoSpustit).

#### Debugging

- Logy aplikácie: `docker logs coinapp -f`
- Logy DB: `docker logs coinapp-db -f`
- Logy Keycloaku: `docker logs coinapp-keycloak -f`
- SLF4J markery pre kategorizáciu (`BIZ_MARK`, `PAYL_MARK`, `TECH_MARK`) — viď `logging/LogsCategorization`.

##### Pripojenie debuggera k bežiacemu kontajneru

Compose service `coinapp` má pridaný JDWP agent (`JAVA_OPTS=-agentlib:jdwp=...,address=*:5005`) a port `5005:5005` mapping. Pripojenie z IntelliJ:

1. **Run → Edit Configurations → + → Remote JVM Debug**
2. Host: `localhost`, Port: `5005`, Use module classpath: `coinapp`
3. Spusti `docker compose up`, počkaj na štart appky, potom **Debug** na novej konfigurácii.

Aktuálne nastavenie má `suspend=n` — kontajner sa nezastaví na štarte. Ak chceš debugovať aj bootstrap (`@PostConstruct`, Spring init), zmeň v `docker-compose.yml` na `suspend=y` a aplikácia počká, kým sa pripojí debugger.

> Lokálne z IntelliJ (`bootRun` cez Debug-run) debug funguje out-of-the-box, debug port netreba nastavovať.

### <a name="Monitoring" id="Monitoring"></a>Monitoring

#### HealthCheck

- http://localhost:8185/actuator/health (lokál) · http://localhost:8030/actuator/health (docker) — endpoint sa volá interne aj v `HEALTHCHECK` direktíve v `Dockerfile`-e (vždy proti `localhost:8080` vnútri kontajnera).
