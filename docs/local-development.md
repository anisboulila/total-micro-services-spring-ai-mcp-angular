# Local development

## Environment and local containers

The learner-reported environment inventory was Java 21.0.2, Maven 3.9.9,
Docker 29.8.2, Docker Compose 5.5.1, and OpenSpec 1.14.0. `docker info`
confirmed a reachable Linux daemon, and `docker run --rm hello-world`
completed successfully. The exact executable paths from `Get-Command` were not
preserved.

Useful Windows PowerShell commands for checking the local tools and Compose
configuration:

```powershell
Get-Command java,mvn,docker,openspec -ErrorAction SilentlyContinue | Select-Object Name,Source
java -version
mvn -version
docker --version
docker compose version
docker info
openspec --version
docker compose config
```

### Maven reactor validation

From the repository root, inspect the Java runtime selected by Maven and
validate the reactor:

```powershell
mvn -version
mvn -N validate
mvn validate
```

`mvn -N validate` validates only the root POM. `mvn validate` traverses the
root project and its declared modules, validating their POMs and parent
relationships. These commands do not compile or run application code. The
root POM sets `java.version` to 21, inherits Spring Boot 3.5.10 dependency and
plugin management, and imports the Spring Cloud 2025.0.1 BOM.

During CPS-MVN-01, `mvn -N validate` and `mvn validate` both returned
`BUILD SUCCESS`. The shared `mvn -version` result reported Java 21.0.2 from
`C:\SpringBOOT\outils\jdk-21`; the Maven version number was not included in
that result.

### Run the minimal Customer application

From the repository root, run the module test and start Customer:

```powershell
mvn -pl customer-service -am test
mvn -pl customer-service spring-boot:run
```

In a second terminal, check the Actuator health endpoint:

```powershell
Invoke-WebRequest http://localhost:8082/actuator/health
```

The expected response is HTTP 200 with JSON `{"status":"UP"}`. Stop the
application with `Ctrl+C` in the Maven terminal. This minimal application
does not require PostgreSQL to start. If startup reports that port 8082 is
already in use, inspect the listener with
`Get-NetTCPConnection -LocalPort 8082` before taking any action.

During CPS-APP-01, the test command returned `BUILD SUCCESS`; startup reported
`Started CustomerServiceApplication in 1.536 seconds`; the health request
returned HTTP 200 and `{"status":"UP"}`, and the process stopped after
`Ctrl+C`.

### Run the minimal API Gateway

From the repository root, run the module test and start Gateway:

```powershell
mvn -pl api-gateway -am test
mvn -pl api-gateway spring-boot:run
```

In a second terminal, check the Actuator health endpoint:

```powershell
Invoke-WebRequest http://localhost:8080/actuator/health
```

The expected response is HTTP 200 with JSON `{"status":"UP"}`. Stop the
application with `Ctrl+C` in the Maven terminal. This initial Gateway has no
business routes. If startup reports that port 8080 is already in use, inspect
the listener with `Get-NetTCPConnection -LocalPort 8080` before taking any
action.

During CPS-APP-02, the test command returned `BUILD SUCCESS`; startup reported
`Started ApiGatewayApplication in 1.904 seconds`; the health request returned
HTTP 200 and `{"status":"UP"}`, and the process stopped after `Ctrl+C`.

### Build and run the applications with Docker Compose

From the repository root, validate the Compose file, build both images and
start the two applications:

```powershell
docker compose config
docker compose up -d --build customer-service api-gateway
docker compose ps
docker compose logs --tail 50 customer-service api-gateway
```

The Dockerfiles use the repository root as the build context so Maven can read
the parent POM and both module POMs. Each is a multi-stage build: Maven builds
the selected application with Java 21, then a Java 21 JRE image runs only its
executable JAR as a non-root UID. `.dockerignore` excludes local `.env` files,
Git metadata and Maven `target` directories from the context.

Compose's default network provides DNS names matching the service names.
Customer listens on container port 8082 and is not published on the host;
Gateway listens on container port 8080 and is published only on
`127.0.0.1:8080`. The Compose healthchecks call each container's own Actuator
health endpoint with Alpine's `wget`. No `depends_on` or PostgreSQL/Keycloak
connection settings are needed yet because neither minimal application uses
those services at startup.

Wait for `healthy` in `docker compose ps`; `Up` alone means only that the
container process is running. The current images are built from these Docker
contexts and can be stopped with `docker compose stop customer-service
api-gateway`.

During CPS-APP-03, the learner reported that `docker compose config` displayed
the resolved configuration without error and that `docker compose up -d
--build customer-service api-gateway` started the images. `docker compose ps`
showed both applications `Up (healthy)`, Customer with only `8082/tcp`, and
Gateway published as `127.0.0.1:8080->8080/tcp`; the logs showed `Started` for
both applications.

Docker CLI and Docker daemon checks are different: `docker --version` reports
the client, while `docker info` must be able to contact the server/daemon.
`docker compose config` resolves the Compose configuration; it does not start
containers.

The current Compose file defines PostgreSQL and Keycloak. Start or inspect just
those services with:

```powershell
docker compose up -d postgres keycloak
docker compose ps postgres keycloak
docker compose logs --tail 50 postgres
docker compose logs --tail 80 keycloak
```

PostgreSQL health and connection readiness can be checked with:

```powershell
docker compose exec -T postgres pg_isready -U customer_app -d customer_db
docker compose ps postgres
```

Use the database and username configured in the local environment if `.env`
overrides the Compose defaults. For DBeaver on the host, connect to
`127.0.0.1:5432` and use the configured database/user. Application containers
must use `postgres:5432`, not `localhost:5432`. The host port is bound only to
loopback. Never commit `.env` or share its password.

### Repeatable PostgreSQL persistence check

The following procedure uses only the dedicated test table and row. It resets
that one probe row so a repeated run can demonstrate a new insertion; it does
not delete application data or volumes.

```powershell
docker compose exec -T postgres psql -U customer_app -d customer_db -c "CREATE TABLE IF NOT EXISTS persistence_probe (id integer PRIMARY KEY); DELETE FROM persistence_probe WHERE id = 2; INSERT INTO persistence_probe (id) VALUES (2);"
docker compose restart postgres
docker compose ps postgres
docker compose exec -T postgres psql -U customer_app -d customer_db -c "SELECT id FROM persistence_probe WHERE id = 2;"
```

The expected evidence is that the service returns to `healthy` and the final
query returns row `2`. Do not use `docker compose down -v`: that removes named
volumes and invalidates a persistence check.

## Keycloak and OIDC

The local Keycloak realm is `customer-dev`. Its issuer is:

```text
http://localhost:8081/realms/customer-dev
```

The OpenID Connect discovery document is available at:

```text
http://localhost:8081/realms/customer-dev/.well-known/openid-configuration
```

Endpoints verified from the discovery document:

| Endpoint | URL |
|---|---|
| Authorization | `http://localhost:8081/realms/customer-dev/protocol/openid-connect/auth` |
| Token | `http://localhost:8081/realms/customer-dev/protocol/openid-connect/token` |
| User info | `http://localhost:8081/realms/customer-dev/protocol/openid-connect/userinfo` |
| JWKS | `http://localhost:8081/realms/customer-dev/protocol/openid-connect/certs` |
| Introspection | `http://localhost:8081/realms/customer-dev/protocol/openid-connect/token/introspect` |
| Logout | `http://localhost:8081/realms/customer-dev/protocol/openid-connect/logout` |

The `customer-local` client is a public OpenID Connect client for local
Authorization Code flow with PKCE S256. Client authentication is disabled; it
has no client secret. Its only valid redirect URI is
`http://127.0.0.1:8765/callback`. Keep this URI exact for the future local PKCE
login script.

## Registration and realm roles

Public self-registration is enabled only in the local `customer-dev` realm.
Newly registered users receive the realm role `CUSTOMER` through the default
roles. The realm role `OPERATOR` is not a default role and must only be assigned
manually by an administrator to a distinct account; it is not offered on the
registration form.

These settings were checked in the Keycloak console:

- `CUSTOMER` and `OPERATOR` exist as realm roles.
- `CUSTOMER` appears under **Realm settings → Default roles**; `OPERATOR` does
  not.
- A public registration form showed no role selector, and a test registration
  received `CUSTOMER` without `OPERATOR`.
- A separate test account received `OPERATOR` as a direct role mapping after
  administrator assignment.
- Public registration is enabled in `customer-dev` and disabled in `master`.

Test accounts and their credentials are intentionally excluded from the realm
import file and this documentation.

The realm import definition is maintained in `infra/keycloak/realm-dev.json`.
It contains the local registration and role defaults but no users, credentials,
or client secrets; it is intended only for local development. `compose.yaml`
contains a read-only mount for Keycloak's startup import. The configuration
after that mount was added was not documented as revalidated with Compose, and
the import itself was not exercised during this manual task. The existing
persistent realm was not overwritten.

To inspect the local Keycloak service and realm discovery from Windows
PowerShell:

```powershell
docker compose up -d keycloak
docker compose ps keycloak
$KeycloakUrl = 'http://localhost:8081'
$Realm = 'customer-dev'
$Oidc = Invoke-RestMethod "$KeycloakUrl/realms/$Realm/.well-known/openid-configuration"
$Oidc | Select-Object issuer,authorization_endpoint,token_endpoint,jwks_uri
```

The expected issuer for this local realm is
`http://localhost:8081/realms/customer-dev`. Realm/client/role configuration
was performed and inspected in the Keycloak console. Do not assume that
`--import-realm` updates an already-existing persistent realm: the import
behavior was not tested here, and the realm/volume must not be deleted merely
to force an import.

## Local OIDC login with PKCE

The diagnostic script `scripts/dev/oidc-pkce-login.ps1` is run with Windows
PowerShell 5.1:

```powershell
powershell.exe -NoProfile -File .\scripts\dev\oidc-pkce-login.ps1 -Issuer "http://localhost:8081/realms/customer-dev" -ClientId "customer-local"
```

The script reads the OIDC discovery document, verifies its issuer, and requires
the discovered authorization and token endpoints to remain on the local
Keycloak host. It generates a cryptographically random PKCE verifier, `state`,
and `nonce`; sends only the S256 challenge to the authorization endpoint; and
opens the system browser. A temporary TCP listener binds exclusively to
`127.0.0.1:8765` and accepts only `GET /callback`. The callback `state` is
checked before the authorization code is used. The code is exchanged in memory
with the original verifier, and the ID token payload is checked for the
expected issuer, audience, authorized party when applicable, and nonce.

The script does **not** validate the ID token signature or its full lifetime
and claim constraints. The displayed ID-token claims are decoded for this
local learning exercise and are not cryptographically trusted. Signature,
issuer, audience, expiry, and authorization enforcement must be validated by
the Spring Security Resource Server in a later task.

Only `iss`, `sub`, `email_verified`, and the `CUSTOMER`/`OPERATOR` realm roles
are displayed. The script does not display, save, or log access tokens, ID
tokens, refresh tokens, the authorization code, the verifier, or credentials.
The callback listener is closed after the response and in cleanup paths. Do
not enable PowerShell transcription during the test, and never share a token,
authorization code, or password.

### Troubleshooting

| Symptom | Check |
|---|---|
| Callback port 8765 cannot be opened | Close the process already listening on `127.0.0.1:8765`; rerun only after the port is free. |
| `invalid_redirect_uri` | Confirm the client has exactly `http://127.0.0.1:8765/callback`, with no wildcard, alternate host, or trailing slash. |
| `unauthorized_client` | Confirm `customer-local` is enabled, public, and has Standard Flow enabled. Keep PKCE S256 required. |
| Token exchange fails / `invalid_grant` | The code may have expired or already been used, or the verifier may not match. Start a new login; never disable PKCE. |
| Issuer mismatch | Compare the `issuer` in the discovery document with `http://localhost:8081/realms/customer-dev`. |
| Discovery fails | Confirm Keycloak is running and the discovery URL is reachable. |
| State or nonce mismatch | The script rejects the response and closes the listener. Restart the flow; do not bypass either check. |
| A role is absent from the output | Check the realm role, the user's effective role mappings/default roles, and the `roles` client scope/protocol mapper. Do not share a JWT. |

Manual runtime verification on 2026-10-15 completed successfully under Windows
PowerShell 5.1: Keycloak returned to the local callback and the script completed
the authorization-code exchange. The reported issuer matched the expected
realm. The test account's `email_verified` value was `False`, and the script
reported no `CUSTOMER` or `OPERATOR` role in the ID token. No token or
authorization code was retained in this documentation. This confirms the
interactive OIDC/PKCE flow but does not confirm role inclusion in the ID token
or validate the ID token signature.

If a realm role is expected but absent from the script output, first verify the
role exists in `customer-dev` and is present in the user's effective role
mapping. Then inspect the `customer-local` client scopes and the realm-role
protocol mapper, including whether it is configured to add realm roles to the
ID token (the script reads the ID token, not the access token). Do not copy or
share tokens while diagnosing this.
