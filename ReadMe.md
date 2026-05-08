# CoinApp — Coin Account Application

Demo Spring Boot aplikácia, ktorá simuluje jednoduchú finančnú knihu (ledger) — spravuje účty a presuny prostriedkov medzi nimi.

## Prehľad

Aplikácia poskytuje **REST API** nad doménou účtov a transakcií:

- **Účet** (`Account`) — má verejný GUID identifikátor, meno, povolené prečerpanie (`maximalOverdraft`) a aktuálny zostatok (`currentBalance`).
- **Transakcia / Ledger záznam** (`Ledger`) — záznam o presune prostriedkov medzi dvomi účtami so zachovanou históriou (suma, zostatok po transakcii, čas, popis).

Pri každej transakcii sa kontroluje, že suma je kladná a že zostatok zdrojového účtu po odpočítaní neklesne pod limit povoleného prečerpania.

## Technologický stack

- **Java 21** (Eclipse Temurin)
- **Spring Boot 3.3.4**
  - Spring Web (REST API)
  - Spring Data JPA
  - Spring Data REST
  - Spring Web Services
  - Spring Security — OAuth2 Resource Server (JWT)
  - Spring Boot Actuator
  - Spring Retry (`@Retryable` pri konfliktoch optimistického zámku)
  - Spring Validation
- **PostgreSQL 16** (runtime DB)
- **Hibernate 6** (JPA provider, `ddl-auto: update` v dev profile)
- **Lombok**
- **springdoc-openapi 2.6** (Swagger UI)
- **JUnit 5** + **Testcontainers** (integračné testy nad reálnym PostgreSQL kontajnerom)
- **Gradle** (build)
- **Keycloak 25** (Identity Provider — vystavuje JWT access tokeny)
- **Docker** + **Docker Compose** (lokálny dev stack: appka + DB + Keycloak)

## Architektúra projektu

```
src/main/java/st/coinaccountapp/
├── CoinAccountApplication.java   # Spring Boot entry point
├── api/                          # REST controllery + DTO
│   ├── CoinApi.java
│   └── dto/
│       ├── AccountDetailDto.java
│       ├── CreateTransactionDto.java
│       └── LedgerDetailDto.java
├── service/                      # business logika
│   ├── AccountService.java
│   └── LedgerService.java
├── model/                        # JPA entity
│   ├── Account.java
│   └── Ledger.java
├── repos/                        # Spring Data repository
│   ├── AccountRepository.java
│   └── LedgerRepository.java
├── config/
│   ├── ApiPaths.java             # konštanty URL ciest
│   ├── OpenApiConfig.java        # OpenAPI metadata + Bearer auth scheme
│   └── SecurityConfig.java       # OAuth2 Resource Server, JWT + role mapping
├── exception/                    # vlastné výnimky + global handler
│   ├── BiznisValidationFailedException.java
│   ├── NotFoundException.java
│   └── GlobalExceptionHandler.java
├── anotation/
│   └── ConcurrentUpdateOrIntegrityRetry.java   # @Retryable wrapper
└── logging/
    └── LogsCategorization.java   # SLF4J markery
```

## REST API

Bázová cesta: `/v1/account`

| Metóda | Endpoint | Popis | Status | Auth |
|---|---|---|---|---|
| `GET` | `/v1/account/{guid}` | Vráti detail účtu (meno, GUID, zostatok, povolené prečerpanie) | `200 OK` | HTTP Basic |
| `POST` | `/v1/account/tx` | Vykoná transakciu medzi dvomi účtami | `201 CREATED` | HTTP Basic |

> Všetky volania na `/v1/...` vyžadujú **HTTP Basic auth**. Detaily v sekcii [Security](#security).

**Príklad — detail účtu:**

```http
GET /v1/account/3f5b9c12-1234-4abc-9def-0123456789ab
Authorization: Basic YWRtaW46YWRtaW4=
```

```json
{
  "guid": "3f5b9c12-1234-4abc-9def-0123456789ab",
  "name": "Savings Account",
  "maximalOverdraft": 0.0000,
  "currentBalance": 1000.0000
}
```

**Príklad — transakcia:**

```http
POST /v1/account/tx
Content-Type: application/json
Authorization: Basic YWRtaW46YWRtaW4=
```

```json
{
  "fromAccountGuid": "3f5b9c12-1234-4abc-9def-0123456789ab",
  "toAccountGuid":   "9a1b2c3d-1111-2222-3333-444455556666",
  "amount": 50.00,
  "description": "Mesačný presun"
}
```

### Swagger / OpenAPI

Po spustení appky je Swagger UI dostupné na:

- http://localhost:8080/swagger-ui.html (rovnaký port v oboch režimoch)

OpenAPI JSON: `/v3/api-docs`.

> Swagger UI a `/v3/api-docs` sú **otvorené bez auth**, ale samotné `/v1/...` volania zo Swagger UI musíš autorizovať — v UI klikni **Authorize** a vlož **JWT access token** získaný z Keycloaku (viď [Security](#security)).

## Security

App je chránená cez **Spring Security ako OAuth2 Resource Server** — validuje **JWT** access tokeny vystavené **Keycloakom**. Stateless, bez session, CSRF disabled.

### Identity Provider — Keycloak

Keycloak beží ako tretia služba v `docker-compose.yml` na `http://localhost:8081`. Realm a všetci useri sú **predkonfigurovaní** v `keycloak/coinapp-realm.json`, ktorý sa pri prvom štarte importuje. Žiadne klikanie v UI nikdy.

| Položka | Hodnota |
|---|---|
| Keycloak admin UI | http://localhost:8081 |
| Admin login (master realm) | `admin` / `admin` |
| Realm pre appku | `coinapp` |
| OAuth2 client | `coinapp-api` (public, direct access grants) |
| Issuer URI (v JWT `iss` claim) | `http://localhost:8081/realms/coinapp` |
| Token endpoint | `http://localhost:8081/realms/coinapp/protocol/openid-connect/token` |
| JWKS (verejné kľúče) | `http://localhost:8081/realms/coinapp/protocol/openid-connect/certs` |

### Predkonfigurovaní useri a role

| Username | Heslo | Role |
|---|---|---|
| `admin@example.com` | `admin` | `ADMIN`, `USER` |
| `user@example.com`  | `user`  | `USER` |

### Role na endpointoch (`@PreAuthorize`)

| Endpoint | Vyžadovaná rola |
|---|---|
| `GET /v1/account/{guid}` | `ADMIN` alebo `USER` |
| `POST /v1/account/tx` | `ADMIN` |

User s rolou `USER` dostane na transakciu **`403 Forbidden`**.

### Otvorené (nechránené) endpointy

- `/actuator/health` — pre health-check (Docker `HEALTHCHECK`, k8s probes).
- `/v3/api-docs`, `/v3/api-docs/**` — OpenAPI definícia.
- `/swagger-ui.html`, `/swagger-ui/**` — Swagger UI.

### Chránené endpointy

Všetko ostatné (`/v1/**`) — vyžaduje platný **Bearer JWT** v hlavičke `Authorization`.

### Konfigurácia v aplikácii

`application-dev.yml` — kde appka validuje JWT:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER:http://localhost:8081/realms/coinapp}
          jwk-set-uri: ${KEYCLOAK_JWK_SET_URI:http://localhost:8081/realms/coinapp/protocol/openid-connect/certs}
```

- `issuer-uri` — Spring kontroluje, že JWT `iss` claim sa zhoduje s touto hodnotou.
- `jwk-set-uri` — odkiaľ Spring fetchuje verejné kľúče na overenie podpisu JWT.

V Composeu sú obe override-nuté (app v kontajneri si ide po JWKS interne cez `keycloak:8080`, ale očakáva tokeny s issuerom `localhost:8081`).

### Mapovanie rolí (Keycloak → Spring)

Keycloak ukladá role v JWT claim `realm_access.roles` (zoznam stringov). Spring Security `hasRole('X')` ale očakáva authority s prefixom `ROLE_X`. V `SecurityConfig.java` je preto vlastný `KeycloakRealmRoleConverter`, ktorý každú rolu z claim-u prefixuje na `ROLE_*`.

### Príklady volaní

**1) Získaj access token (Resource Owner Password Credentials grant):**

```bash
TOKEN=$(curl -s -X POST http://localhost:8081/realms/coinapp/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=coinapp-api" \
  -d "username=admin@example.com" \
  -d "password=admin" \
  | jq -r .access_token)
```

> Token je platný 30 minút (`accessTokenLifespan: 1800` v realm.json). Po expirácii ho získaj nanovo.

**2) Volaj API s Bearer tokenom:**

```bash
# detail účtu (ADMIN aj USER)
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/v1/account/3f5b9c12-1234-4abc-9def-0123456789ab

# transakcia (len ADMIN)
curl -H "Authorization: Bearer $TOKEN" -X POST \
  http://localhost:8080/v1/account/tx \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountGuid": "3f5b9c12-1234-4abc-9def-0123456789ab",
    "toAccountGuid":   "9a1b2c3d-1111-2222-3333-444455556666",
    "amount": 50.00,
    "description": "Mesačný presun"
  }'
```

**3) Swagger UI:**

Klikni **Authorize** vpravo hore a do poľa `bearerAuth` vlož **iba samotný token** (bez prefixu `Bearer `), potom volaj endpointy z UI.

### Reakcie na nesprávnu auth

- Žiadny `Authorization` header → **`401 Unauthorized`**.
- Token expirovaný / neplatný podpis / zlý issuer → **`401 Unauthorized`**.
- Platný token bez správnej role (napr. `USER` na `POST /tx`) → **`403 Forbidden`**.

### Pre testy

Integračný test (`CoinAccountApplicationTests`) **nepotrebuje bežiaci Keycloak**. Cez `@TestConfiguration` poskytuje stub `JwtDecoder`, ktorý akýkoľvek token akceptuje a vráti pre neho rolu `ADMIN, USER`. Spring Boot autokonfiguráciu JWT validácie tým prebije.

Pre realistickejšie testovanie auth toku by sa použili Testcontainers s Keycloak image — momentálne to je out-of-scope, lebo by to predĺžilo testy o ~30s každý beh.

## Spring profily

| Profile | Účel | Konfigurácia |
|---|---|---|
| `dev` | Lokálny vývoj (default) | PostgreSQL, `ddl-auto: update`, hostname DB cez env premennú `DB_HOST` (default `localhost`) |
| `test` | Automatické testy | `ddl-auto: create-drop`, DB cez Testcontainers (PostgreSQL kontajner sa spustí počas testu) |
| `prod` | Produkcia | (zatiaľ neexistuje samostatný profile súbor) |

Default profile je `dev` (nastavené v `application.yml`).

## Docker setup a režimy spustenia

Projekt obsahuje **jeden `docker-compose.yml`** s **tromi službami**:

- **`coinapp`** — Spring Boot aplikácia (build z `Dockerfile` v root-e).
- **`db`** — PostgreSQL 16 (oficiálny image `postgres:16`).
- **`keycloak`** — Keycloak 25 (Identity Provider, predkonfigurovaný realm).

| Služba | Názov v sieti | `container_name` | Port mapping |
|---|---|---|---|
| App | `coinapp` | `coinapp` | `8080:8080` |
| DB | `db` | `coinapp-db` | `5432:5432` |
| Keycloak | `keycloak` | `coinapp-keycloak` | `8081:8080` |

### Spring profile vs. Docker Compose

Sú to **dve nezávislé veci** — netreba ich miešať:

- **Spring profile** (`dev`, `test`, `prod`) → runtime konfigurácia *vnútri appky*.
- **Docker Compose** → opisuje *aké služby a ako* bežia. Používa sa pre lokálny vývoj a integračné testy, **nie pre produkciu**.

### Tabuľka režimov spustenia

| Režim | Príkaz | Spring profile | `DB_HOST` | DB endpoint | App URL |
|---|---|---|---|---|---|
| Lokálne z IntelliJ (appka na hostiteľovi, DB v kontajneri) | `docker compose up -d db`, potom **Run** v IDE | `dev` | (default `localhost`) | `localhost:5432` | http://localhost:8080 |
| Plne v kontajneroch | `docker compose up` | `dev` | `db` (z `docker-compose.yml`) | `db:5432` cez Compose sieť | http://localhost:8080 |
| CI testy | `./gradlew test` | `test` | — (Testcontainers spustí dočasný PostgreSQL) | — | — |
| Produkcia | nasadenie cez k8s / cloud / VM — **nie Compose** | `prod` | (podľa nasadenia) | (podľa nasadenia) | (podľa nasadenia) |

### Ako to funguje pod kapotou

V `application-dev.yml` je URL databázy:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:5432/coinapp
```

- Keď environment premennú `DB_HOST` **nikto nenastaví** (typicky pri spustení z IntelliJ), použije sa default `localhost` a appka sa pripojí na PostgreSQL kontajner cez vystavený port `5432:5432`.
- Keď appka beží **v Composeu**, `docker-compose.yml` jej nastavuje `DB_HOST=db` a Compose sieť rozriesi `db` na IP kontajnera databázy.

Tým pádom **rovnaký `dev` profile funguje v oboch režimoch** bez úprav.

## Spustenie

### 1) Lokálne z IntelliJ (odporúčané pre vývoj)

```bash
# 1. spusti DB aj Keycloak v kontajneroch (appku NIE — tú spustíš z IDE)
docker compose up -d db keycloak

# 2. počkaj ~20s, kým Keycloak nabootuje a importuje realm
#    (over: curl http://localhost:8081/realms/coinapp/.well-known/openid-configuration)

# 3. v IntelliJ klikni Run na konfiguráciu CoinAccountApplication
#    (default profile = dev → DB_HOST default = localhost, KEYCLOAK_ISSUER default = localhost:8081)
```

App beží na http://localhost:8080, Keycloak na http://localhost:8081.

### 2) Plne v kontajneroch

```bash
# build jaru a spusti všetko (app + DB + Keycloak) v kontajneroch
./gradlew clean build -x test
docker compose up --build
```

App beží na http://localhost:8080, Keycloak na http://localhost:8081.

> Pozn.: Port `8080` je obsadený jednou inštanciou appky naraz. Ak ti beží IntelliJ Run, najprv ho zastav, než spustíš `docker compose up` (a naopak).

### 3) Testy

```bash
./gradlew test
```

Testy bežia s `test` profilom a Testcontainers automaticky vytvorí dočasný PostgreSQL kontajner.

## Bežné Docker príkazy

```bash
# spusti DB + Keycloak (pre lokálny dev z IntelliJ)
docker compose up -d db keycloak

# spusti celý stack (app + DB + Keycloak)
docker compose up

# zhasni všetko
docker compose down

# zhasni a vymaž aj data volume (reset DB; Keycloak realm sa pri novom štarte naimportuje znova)
docker compose down -v

# pozri stav kontajnerov
docker ps

# logy z DB / Keycloak / appky
docker logs coinapp-db -f
docker logs coinapp-keycloak -f
docker logs coinapp -f
```

## Prístup k databáze z hostiteľa

Keďže má DB port `5432:5432`, vieš sa na ňu pripojiť z DB klienta (DataGrip, DBeaver, `psql`) bežiaceho na hostiteľovi:

| Pole | Hodnota |
|---|---|
| Host | `localhost` |
| Port | `5432` |
| Database | `coinapp` |
| User | `coinuser` |
| Password | `coinpass` |

> Heslá sú v `docker-compose.yml` len pre lokálny vývoj. V produkcii by sa riešili cez secrets a do repo nepatria.

## Build artefakt

Build vyrobí `build/libs/coinapp-0.0.1-SNAPSHOT.jar`, ktorý sa použije v Dockerfile-e:

```dockerfile
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY build/libs/coinapp-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO- http://localhost:8080/actuator/health || exit 1
```

Pred `docker compose up --build` treba mať jar zostavený (`./gradlew build -x test`).

## Lombok

V IntelliJ je nutné mať zapnuté **Annotation Processing**:

`File | Settings | Build, Execution, Deployment | Compiler | Annotation Processors → Enable annotation processing`

(IntelliJ Community Edition aj Ultimate.)

## Actuator / Health-check

App má povolený Actuator s `health` endpointom:

- http://localhost:8080/actuator/health

Tento endpoint sa používa aj v `HEALTHCHECK` direktíve v Dockerfile-e.
