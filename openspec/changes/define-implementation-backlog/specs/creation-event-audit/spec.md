# Spec Delta

## Purpose

Définit la publication fiable des faits Customer/Account décidés pour Kafka et
la conservation interne, idempotente et limitée dans le temps de leurs traces.

## ADDED Requirements

### Requirement: US-07 — Publier les faits de création Customer et Account

The system MUST publish the owner-approved Customer and Account creation facts using the reliability guarantees recorded in DEC-07.

#### Scenario: Création Customer validée
- **GIVEN** une création Customer validée et persistée
- **WHEN** sa transaction métier est commitée
- **THEN** le fait `CustomerCreated` correspondant est durablement associé à cette transaction et publié vers Kafka

#### Scenario: Création Account validée
- **GIVEN** une création Account validée et persistée
- **WHEN** sa transaction métier est commitée
- **THEN** le fait `AccountCreated` correspondant est durablement associé à cette transaction et publié vers Kafka

#### Scenario: Acheminement des deux faits
- **GIVEN** les faits `CustomerCreated` et `AccountCreated`
- **WHEN** ils sont publiés
- **THEN** ils sont acheminés vers au moins un topic Kafka décidé avant implémentation

#### Scenario: Échec d’une création métier
- **GIVEN** une création Customer ou Account refusée
- **WHEN** aucune transaction métier n’est commitée
- **THEN** aucun fait de création correspondant n’est publié

### Requirement: Livraison et traitement idempotents des événements

The system MUST preserve DEC-07 delivery, identity, ordering, retry, failure-queue, and versioning guarantees.

#### Scenario: Livraison répétée d’un événement
- **GIVEN** un événement peut être livré au moins une fois
- **WHEN** `audit-service` reçoit plusieurs fois le même `eventId`
- **THEN** une seule trace d’audit est conservée pour cet événement

#### Scenario: Ordre et évolution du schéma
- **GIVEN** des événements de créations sont produits
- **WHEN** ils sont publiés et consommés
- **THEN** leur partitionnement conserve l’ordre par agrégat
- **AND** chaque événement porte un identifiant unique et une version de schéma

#### Scenario: Erreur de traitement
- **GIVEN** un événement ne peut pas être traité
- **WHEN** le consumer échoue
- **THEN** les retries restent bornés et l’événement non traité suit le mécanisme de file d’échecs décidé avant implémentation

### Requirement: Conserver uniquement les métadonnées d’audit

The system MUST retain only the owner-approved event metadata for audit and MUST NOT expose a functional audit-query capability in v1.

#### Scenario: Écriture de trace d’audit
- **GIVEN** `audit-service` traite un événement Customer ou Account
- **WHEN** il persiste sa trace
- **THEN** elle contient uniquement `eventId`, le type d’événement, `aggregateId`, la date et la version
- **AND** elle ne contient ni données métier complètes, ni jeton, ni secret

#### Scenario: Absence de consultation fonctionnelle
- **GIVEN** une trace d’audit est conservée
- **WHEN** un acteur de l’application demande une consultation d’audit
- **THEN** aucune API ou interface fonctionnelle de consultation n’est fournie en v1

#### Scenario: Expiration d’une trace
- **GIVEN** une trace a atteint sa durée de conservation d’un mois
- **WHEN** le mécanisme de purge s’exécute
- **THEN** la trace est supprimée du stockage d’audit
