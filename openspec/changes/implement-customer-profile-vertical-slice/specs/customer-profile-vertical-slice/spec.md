# Spec Delta

## Purpose

Décrit la tranche technique exécutable qui met en œuvre les exigences Customer
existantes US-01 à US-03 et les contrats techniques retenus, sans redéfinir
leurs règles métier.

## ADDED Requirements

### Requirement: Démarrage local du parcours Customer

The Customer profile slice MUST start locally with PostgreSQL, Keycloak, the API Gateway, and customer-service using reproducible configuration, while PostgreSQL remains the authoritative store for Customer profiles.

#### Scenario: Démarrage et disponibilité
- **GIVEN** Docker Compose is available and required ports are free
- **WHEN** the learner starts the Customer slice using the documented command
- **THEN** PostgreSQL, Keycloak, Gateway and customer-service reach their documented readiness state
- **AND** the Gateway exposes only the configured Customer routes
- **AND** internal Actuator endpoints are not exposed through the Gateway
- **AND** a restart preserves Customer records in PostgreSQL

#### Scenario: Reproductible database initialization
- **GIVEN** an empty Customer database or a previously migrated database
- **WHEN** customer-service starts
- **THEN** Flyway creates or advances the Customer schema in order
- **AND** application startup fails visibly if a migration cannot be applied

### Requirement: Profil Customer owned by customer-service

The customer-service MUST implement the existing Customer profile requirements using its own PostgreSQL schema and must enforce the approved identity, uniqueness, validation, and idempotency contracts.

#### Scenario: Auto-registration of a profile
- **GIVEN** an authenticated CUSTOMER with a valid `sub`, a verified email claim, and no existing profile
- **WHEN** le client soumet un nom non vide à `POST /api/v1/customers` avec un `Idempotency-Key` valide
- **THEN** customer-service persiste un seul profil lié aux claims vérifiés du jeton
- **AND** il retourne HTTP 201, la représentation créée et un en-tête `Location`

#### Scenario: Invalid profile or duplicate identity
- **GIVEN** un nom vide ou trop long, un claim e-mail absent/non vérifié, un `sub` déjà associé ou un e-mail déjà utilisé
- **WHEN** la création du profil est tentée
- **THEN** aucun second profil n’est persisté
- **AND** le service retourne l’erreur RFC 9457 et le code stable prévu
- **AND** les autres erreurs de validation sont HTTP 400 `VALIDATION_FAILED`
- **AND** l’absence ou la non-vérification de l’e-mail retourne HTTP 400 `EMAIL_NOT_VERIFIED`
- **AND** les conflits de profil `sub` et d’e-mail sont HTTP 409 `CUSTOMER_PROFILE_EXISTS` et `EMAIL_ALREADY_EXISTS`

#### Scenario: Caller attempts to select identity attributes
- **GIVEN** le client soumet `sub` ou `email` dans le corps de création
- **WHEN** la requête est validée
- **THEN** elle est rejetée en HTTP 400 `VALIDATION_FAILED`
- **AND** les attributs d’identité proviennent exclusivement du JWT validé

#### Scenario: Operator attempts profile creation for another person
- **GIVEN** un OPERATOR authentifié
- **WHEN** il soumet une demande de création de profil
- **THEN** le service retourne HTTP 403 avec le code `FORBIDDEN_PROFILE_CREATION`
- **AND** aucun profil n’est créé

### Requirement: Customer API enforces owner-scoped reads

The customer-service MUST implement the existing US-02 and US-03 read behavior and authorize each request using claims from its independently validated access token.

#### Scenario: Customer reads own profile
- **GIVEN** un CUSTOMER authentifié avec un profil lié à son `sub`
- **WHEN** il demande sa liste ou son profil
- **THEN** la réponse contient uniquement son profil

#### Scenario: Operator reads profiles
- **GIVEN** un OPERATOR authentifié
- **WHEN** il liste les profils ou demande un profil existant par identifiant
- **THEN** customer-service retourne les profils autorisés

#### Scenario: Cross-customer or missing profile detail
- **GIVEN** un CUSTOMER demande un profil absent ou lié à un autre `sub`
- **WHEN** customer-service autorise la demande de détail
- **THEN** il retourne HTTP 404 dans les deux cas

#### Scenario: Deterministic pagination
- **GIVEN** une requête autorisée de liste des profils Customer
- **WHEN** les paramètres de pagination sont absents ou valides
- **THEN** le service utilise un index zéro-based, une taille par défaut de 20, un maximum de 100 et l’ordre `createdAt DESC, id ASC`
- **AND** il retourne les métadonnées de page convenues
- **AND** des paramètres invalides ou une taille supérieure à 100 retournent HTTP 400 sans plafonnement silencieux

### Requirement: Customer slice has verifiable automated checks

The Customer slice MUST provide repeatable automated tests for its migrations, REST contract, authorization, validation, data isolation, and idempotent profile creation, with no dependency on shared developer services.

#### Scenario: Database and security tests
- **GIVEN** no pre-existing PostgreSQL or Keycloak test environment
- **WHEN** the documented Maven verification command runs
- **THEN** the tests start isolated PostgreSQL and Keycloak dependencies as needed
- **AND** test successful and failed JWT validation, roles, ownership, unique constraints, migration initialization, and restart-safe persistence behavior

#### Scenario: Idempotent profile creation under replay and concurrency
- **GIVEN** multiple identical or conflicting create requests using an idempotency key
- **WHEN** the test suite submits them sequentially and concurrently
- **THEN** identical requests replay the original success status/body within seven days
- **AND** only one profile is created
- **AND** a different effective request returns HTTP 409 with `IDEMPOTENCY_KEY_REUSED`
