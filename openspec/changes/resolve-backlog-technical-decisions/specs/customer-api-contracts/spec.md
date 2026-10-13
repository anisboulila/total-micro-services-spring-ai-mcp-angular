# Spec Delta

## Purpose

Définit les contrats techniques retenus pour démarrer les API Customer et
Account sans changer les règles métier déjà arbitrées.

## ADDED Requirements

### Requirement: Validation des entrées REST

The Customer and Account APIs MUST validate request parameters and authenticated identity claims before applying a business operation, and MUST return structured client errors without exposing stack traces or sensitive values.

#### Scenario: Requête invalide
- **GIVEN** une requête authentifiée dont un champ obligatoire est absent, vide ou invalide
- **WHEN** le service valide la requête
- **THEN** il retourne HTTP 400 avec `application/problem+json` et le code `VALIDATION_FAILED`
- **AND** aucune écriture métier n’est effectuée
- **AND** le détail identifie les champs en erreur sans recopier de données sensibles

#### Scenario: Nom Customer normalisé ou hors limites
- **GIVEN** un nom Customer avec des espaces périphériques, vide après suppression de ces espaces, ou dépassant 120 caractères
- **WHEN** le service valide la création du profil
- **THEN** il supprime les espaces périphériques d’un nom non vide de 120 caractères maximum
- **AND** il retourne HTTP 400 `VALIDATION_FAILED` si le nom est vide ou trop long

#### Scenario: Identité Customer sans e-mail vérifié
- **GIVEN** un JWT valide dont le claim `email_verified` est absent ou différent de `true`, ou dont le claim `email` est absent
- **WHEN** le client demande la création de son profil Customer
- **THEN** le service retourne HTTP 400 avec le code `EMAIL_NOT_VERIFIED`
- **AND** aucun profil n’est créé

#### Scenario: E-mail dupliqué sans distinction de casse
- **GIVEN** une fiche Customer dont l’e-mail correspond sans tenir compte de la casse à l’e-mail vérifié de l’identité
- **WHEN** le client tente de créer son profil
- **THEN** le service retourne HTTP 409 avec le code `EMAIL_ALREADY_EXISTS`
- **AND** aucune seconde fiche n’est créée

#### Scenario: Champ contrôlé par le serveur fourni par le client
- **GIVEN** un client authentifié qui soumet un champ réservé à l’identité ou au propriétaire, tel que `sub`, `email` ou `customerId`
- **WHEN** le service valide le corps de création
- **THEN** la requête est rejetée en HTTP 400 avec le code `VALIDATION_FAILED`
- **AND** aucune ressource n’est créée

### Requirement: Pagination déterministe

The Customer and Account collection APIs MUST use a zero-based page index, a configurable default page size of 20, a configurable maximum page size of 100, and a deterministic order.

#### Scenario: Pagination omise ou valide
- **GIVEN** une demande de collection sans paramètres de pagination ou avec des paramètres valides
- **WHEN** le service retourne la page
- **THEN** il applique la taille par défaut de 20 si elle est omise
- **AND** il retourne `content`, `page`, `size`, `totalElements` et `totalPages`
- **AND** `page` commence à zéro et les résultats suivent l’ordre `createdAt DESC, id ASC`

#### Scenario: Paramètres de pagination invalides
- **GIVEN** une demande avec une page ou une taille négative, mal formée, ou une taille supérieure à 100
- **WHEN** le service valide les paramètres
- **THEN** il retourne HTTP 400 avec le code `INVALID_PAGE` ou `INVALID_PAGE_SIZE`
- **AND** la taille n’est jamais plafonnée silencieusement

### Requirement: Erreurs HTTP structurées et sûres

The Customer and Account APIs MUST use RFC 9457 Problem Details with stable error codes and a correlation identifier, preserving the business-approved HTTP behavior and masking unauthorized resources as not found.

#### Scenario: Erreur métier ou technique
- **GIVEN** une erreur de validation, d’autorisation, de ressource, de conflit ou de dépendance
- **WHEN** le service répond à l’appelant
- **THEN** la réponse `application/problem+json` contient un statut HTTP cohérent, un code stable, un message sûr et un identifiant de corrélation
- **AND** elle ne contient ni stack trace, jeton, secret, SQL ni donnée interne sensible

#### Scenario: Ressource absente ou non autorisée
- **GIVEN** une ressource Customer ou Account absente ou non accessible au client
- **WHEN** celui-ci la demande
- **THEN** le service retourne HTTP 404 dans les deux cas

#### Scenario: Dépendance Customer indisponible pendant une création Account
- **GIVEN** une création Account nécessitant la validation d’un Customer existant
- **WHEN** `customer-service` est indisponible, expire ou a ouvert son circuit
- **THEN** le service retourne HTTP 503 selon le contrat d’erreur
- **AND** aucun compte n’est persisté

### Requirement: Propagation et validation de l’identité OIDC

The API Gateway and each owning service MUST validate bearer access tokens independently, and an owning service MUST make authorization decisions from verified token claims rather than caller-supplied identity headers.

#### Scenario: Appel utilisateur authentifié via la Gateway
- **GIVEN** un appel REST avec un jeton d’accès valide
- **WHEN** la Gateway le route vers un service propriétaire
- **THEN** le service valide lui-même le jeton, son émetteur, sa signature, son expiration et son audience
- **AND** il fonde l’identité utilisateur sur le claim `sub`
- **AND** les appels synchrones délégués transmettent le jeton original uniquement au service destinataire prévu

#### Scenario: Jeton absent ou invalide
- **GIVEN** un appel sans jeton, avec un jeton expiré ou invalide
- **WHEN** la Gateway ou un service propriétaire reçoit l’appel
- **THEN** l’accès est refusé
- **AND** le jeton n’est jamais journalisé

#### Scenario: Rôle insuffisant ou accès croisé
- **GIVEN** un client authentifié dont le jeton ne porte pas le rôle requis ou qui demande une ressource étrangère
- **WHEN** le service propriétaire applique l’autorisation
- **THEN** il retourne HTTP 403 si le rôle requis manque
- **AND** il masque une ressource étrangère derrière HTTP 404 lorsque la règle métier l’exige

### Requirement: Idempotence persistante des créations

The Customer and Account creation APIs MUST enforce the owner-approved actor-and-operation scope and seven-day retention atomically with resource creation, and MUST replay the original successful response for an identical retry.

#### Scenario: Nouvelle clé et création réussie
- **GIVEN** un POST de création valide avec une clé d’idempotence inutilisée dans le périmètre `(actor_sub, operation, service)`
- **WHEN** le service traite la requête
- **THEN** il persiste la ressource et le statut/corps rejouable de manière atomique dans sa base PostgreSQL
- **AND** il conserve l’enregistrement pendant sept jours à partir du commit de succès

#### Scenario: Rejeu identique
- **GIVEN** une création réussie et une nouvelle requête de même acteur, opération, clé et contenu effectif dans les sept jours
- **WHEN** le service traite le rejeu
- **THEN** il retourne le statut et le corps initiaux sans créer une seconde ressource

#### Scenario: Même clé avec contenu effectif différent
- **GIVEN** une clé déjà liée au même acteur et à la même opération
- **WHEN** une requête avec un contenu effectif différent est reçue
- **THEN** le service retourne HTTP 409 avec le code `IDEMPOTENCY_KEY_REUSED`
- **AND** il n’effectue pas une nouvelle création

#### Scenario: Requêtes identiques concurrentes
- **GIVEN** plusieurs requêtes simultanées avec le même acteur, la même opération, la même clé et le même contenu
- **WHEN** elles sont traitées par une ou plusieurs instances du même service
- **THEN** une seule ressource est créée
- **AND** les requêtes concurrentes reçoivent le résultat initial une fois disponible

#### Scenario: Clé réutilisée hors périmètre
- **GIVEN** la même valeur textuelle de clé utilisée par des acteurs, opérations ou services propriétaires différents
- **WHEN** chacun traite sa requête
- **THEN** chaque clé est isolée dans son périmètre `(actor_sub, operation, service)`

#### Scenario: Clé après expiration
- **GIVEN** un enregistrement d’idempotence dont la rétention de sept jours est échue
- **WHEN** une requête réutilise la clé
- **THEN** le service traite la requête comme une nouvelle commande
- **AND** elle peut créer une nouvelle ressource
- **AND** aucun résultat antérieur n’est prétendu rejouable après expiration

#### Scenario: Clé manquante ou mal formée
- **GIVEN** un POST de création dont l’en-tête `Idempotency-Key` est absent, vide ou dépasse 255 caractères
- **WHEN** le service valide la requête
- **THEN** il retourne HTTP 400 avec le code `INVALID_IDEMPOTENCY_KEY`
- **AND** aucune ressource ni entrée d’idempotence n’est créée

### Requirement: Migrations et tests du premier incrément Customer

The Customer persistence schema MUST be versioned with Flyway, and the first Customer increment MUST have repeatable automated tests at the domain, API, persistence, and authorization boundaries using Spring Boot Test, PostgreSQL Testcontainers, and a Keycloak Testcontainer for OIDC integration.

#### Scenario: Base neuve et mise à niveau
- **GIVEN** une base PostgreSQL vide ou à une version précédente connue
- **WHEN** les migrations Flyway sont exécutées
- **THEN** le schéma est créé ou mis à niveau de manière ordonnée
- **AND** la contrainte d’unicité du `sub` et de l’e-mail est appliquée dans PostgreSQL

#### Scenario: Tests d’intégration reproductibles
- **GIVEN** PostgreSQL et Keycloak ne sont pas déjà démarrés sur le poste
- **WHEN** la suite d’intégration est lancée avec Testcontainers
- **THEN** les conteneurs éphémères requis sont démarrés et nettoyés de manière déterministe
- **AND** les tests vérifient validation, unicité, autorisation, pagination, erreurs et idempotence sans dépendre d’un environnement partagé
