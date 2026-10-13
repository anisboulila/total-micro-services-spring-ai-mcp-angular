# Spec Delta

## Purpose

Définit les parcours Customer et Account de la v1, les données qu’ils
manipulent, les limites d’accès et les règles d’idempotence confirmées.

## ADDED Requirements

### Requirement: US-01 — Inscription et création du profil Customer

The system MUST allow an authenticated customer to create one profile linked to the customer's verified OIDC subject and email.

#### Scenario: Création de profil par auto-inscription
- **GIVEN** une identité Keycloak déjà créée avec un `sub` et un e-mail vérifié
- **WHEN** le client soumet un nom non vide pour créer son profil
- **THEN** le service crée une fiche liée au `sub` authentifié et à l’e-mail vérifié
- **AND** le client ne peut pas choisir un autre `sub` ou un autre e-mail

#### Scenario: Données de profil invalides ou déjà utilisées
- **GIVEN** un nom vide, un e-mail non vérifié ou déjà associé à une fiche, ou un profil déjà lié au `sub`
- **WHEN** le client tente de créer son profil
- **THEN** aucune nouvelle fiche Customer n’est créée
- **AND** un e-mail déjà utilisé retourne HTTP 409

#### Scenario: Un opérateur tente de créer un profil client
- **GIVEN** un opérateur/admin authentifié
- **WHEN** il soumet une création de fiche Customer pour une autre personne
- **THEN** la création est refusée

### Requirement: US-02 — Lister les fiches Customer autorisées

The system MUST return only customer profiles the authenticated actor is permitted to see.

#### Scenario: Client liste ses fiches
- **GIVEN** un client authentifié et une fiche liée à son `sub`
- **WHEN** il demande la liste des fiches Customer
- **THEN** la réponse ne contient que sa propre fiche

#### Scenario: Opérateur liste les fiches
- **GIVEN** un opérateur/admin authentifié
- **WHEN** il demande la liste des fiches Customer
- **THEN** la réponse peut contenir toutes les fiches Customer

#### Scenario: Pagination des fiches
- **GIVEN** une demande de liste sans taille précisée ou une taille supérieure au maximum
- **WHEN** le service construit la page
- **THEN** la taille par défaut est 20 et aucune page ne dépasse la limite 100
- **AND** les deux valeurs restent configurables

### Requirement: US-03 — Consulter une fiche Customer autorisée

The system MUST authorize Customer detail reads at the service that owns the profile.

#### Scenario: Client consulte sa fiche
- **GIVEN** une fiche Customer liée au `sub` du client authentifié
- **WHEN** il demande cette fiche par identifiant
- **THEN** le service retourne sa fiche

#### Scenario: Client consulte une fiche étrangère ou inexistante
- **GIVEN** un client authentifié et un identifiant inexistant ou appartenant à un autre client
- **WHEN** il demande la fiche
- **THEN** le service retourne HTTP 404 dans les deux cas

#### Scenario: Opérateur consulte une fiche
- **GIVEN** un opérateur/admin authentifié et une fiche Customer existante
- **WHEN** il demande cette fiche
- **THEN** le service retourne la fiche

### Requirement: US-04 — Lister les comptes autorisés

The system MUST return only accounts the authenticated actor is permitted to see.

#### Scenario: Client liste ses comptes
- **GIVEN** un client authentifié qui possède zéro ou plusieurs comptes
- **WHEN** il demande la liste des comptes
- **THEN** la réponse contient uniquement ses comptes
- **AND** une liste vide est retournée s’il n’en possède aucun

#### Scenario: Opérateur liste les comptes
- **GIVEN** un opérateur/admin authentifié
- **WHEN** il demande la liste des comptes
- **THEN** la réponse peut contenir les comptes de tous les clients

#### Scenario: Pagination des comptes
- **GIVEN** une demande de liste sans taille précisée ou une taille supérieure au maximum
- **WHEN** le service construit la page
- **THEN** la taille par défaut est 20 et aucune page ne dépasse la limite 100
- **AND** les deux valeurs restent configurables

### Requirement: US-05 — Consulter un compte autorisé

The system MUST authorize Account detail reads at the service that owns the account.

#### Scenario: Client consulte son compte
- **GIVEN** un compte appartenant au client authentifié
- **WHEN** il demande ce compte par identifiant
- **THEN** le service retourne le compte

#### Scenario: Client consulte un compte étranger ou inexistant
- **GIVEN** un client authentifié et un identifiant inexistant ou appartenant à un autre client
- **WHEN** il demande le compte
- **THEN** le service retourne HTTP 404 dans les deux cas

#### Scenario: Opérateur consulte un compte
- **GIVEN** un opérateur/admin authentifié et un compte existant
- **WHEN** il demande le compte
- **THEN** le service retourne le compte

### Requirement: US-06 — Créer un compte courant

The system MUST create only a zero-balance EUR current account for one existing Customer.

#### Scenario: Client crée son compte
- **GIVEN** un client authentifié lié à une fiche Customer existante
- **WHEN** il soumet une demande de création valide
- **THEN** le service crée un compte courant en EUR avec un solde initial nul
- **AND** le propriétaire est dérivé de l’identité authentifiée

#### Scenario: Opérateur crée un compte pour un client existant
- **GIVEN** un opérateur/admin authentifié et une fiche Customer existante
- **WHEN** il demande la création d’un compte pour cette fiche
- **THEN** le service crée le compte courant en EUR avec un solde initial nul pour ce client

#### Scenario: Client fournit un customerId
- **GIVEN** un client authentifié
- **WHEN** sa demande de création contient un `customerId`
- **THEN** la requête est rejetée et aucun compte n’est créé

#### Scenario: Client cible inexistant
- **GIVEN** une création opérateur visant un client inexistant
- **WHEN** le service Account valide le propriétaire auprès de Customer
- **THEN** la création est refusée avec HTTP 404 et aucun compte n’est persisté

#### Scenario: Service Customer indisponible
- **GIVEN** une création nécessitant une validation Customer
- **WHEN** `customer-service` est indisponible, expire ou ouvre son circuit
- **THEN** la création est refusée avec HTTP 503 et aucun compte n’est persisté

### Requirement: Idempotence des créations Customer et Account

The system MUST apply the owner-approved idempotency behavior to Customer and Account creation requests.

#### Scenario: Rejeu d’une création identique
- **GIVEN** une création terminée avec une clé d’idempotence
- **WHEN** le même acteur répète la même opération avec la même clé et le même contenu dans la période de conservation d’une semaine
- **THEN** le résultat initial est retourné exactement et aucune seconde ressource n’est créée

#### Scenario: Même clé avec un contenu différent
- **GIVEN** une clé déjà utilisée pour le même acteur et la même opération
- **WHEN** le contenu de la requête diffère
- **THEN** le service retourne HTTP 409 et n’exécute pas une seconde création

#### Scenario: Deux requêtes concurrentes identiques
- **GIVEN** deux requêtes simultanées avec le même acteur, la même opération, la même clé et le même contenu
- **WHEN** la première création est en cours
- **THEN** une seule ressource est créée
- **AND** la seconde requête attend et reçoit exactement le résultat initial

#### Scenario: Clés d’acteurs ou d’opérations distincts
- **GIVEN** deux acteurs ou deux opérations de création différents
- **WHEN** ils utilisent la même valeur textuelle de clé
- **THEN** leur idempotence est évaluée dans des périmètres distincts
