# Local development

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
or client secrets; it is intended only for local development. The Compose
configuration mounts this file for Keycloak's startup import. The import itself
was not exercised during this manual task, and the existing persistent realm
was not overwritten.

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
