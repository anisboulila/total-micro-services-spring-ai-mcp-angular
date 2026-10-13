# Proposal

## Why

Le backlog v1 définit les parcours Customer et leurs règles métier, et le
change `resolve-backlog-technical-decisions` a fixé les contrats techniques
nécessaires au premier incrément. Le dépôt ne contient pas encore de service
applicatif exécutable. Cette change propose de réaliser un premier parcours
vertical testable, de l’identité Keycloak jusqu’aux API Customer et à leur
persistance PostgreSQL.

## What Changes

- Créer le socle exécutable Java 21 / Spring Boot 3.5.x du `customer-service`
  avec PostgreSQL et migrations Flyway.
- Configurer Keycloak pour l’auto-inscription et l’émission des claims requis;
  protéger la Gateway et le service par validation indépendante des JWT.
- Implémenter l’auto-création du profil Customer, sa lecture/liste autorisées,
  la pagination, la validation, les erreurs RFC 9457 et l’idempotence décidée.
- Fournir les tests unitaires, API, persistance, migrations, sécurité et
  concurrence pertinents pour cet incrément ainsi que les instructions de
  démarrage et validation locales.
- Exposer uniquement les routes Customer via les routes statiques de l’API
  Gateway pour valider le parcours de bout en bout.
- Avancer en mode pédagogique strictement séquentiel : une tâche est présentée,
  modifiée puis vérifiée à la fois; Copilot s’arrête après avoir fourni les
  commandes et attend les résultats et la validation explicite de l’apprenant
  avant de poursuivre.
- Utiliser une procédure locale Authorization Code + PKCE pour valider
  Keycloak sans frontend applicatif; les jetons et codes restent en mémoire sur
  le poste et ne sont jamais communiqués à Copilot.

## Capabilities

### New Capabilities

- `customer-profile-vertical-slice`: première tranche exécutable du profil
  Customer, fondée sur les exigences US-01 à US-03 existantes et le contrat
  technique `customer-api-contracts`.

### Modified Capabilities

Aucune. Les exigences métier faisant autorité restent dans
`define-implementation-backlog/specs/customer-account-management/spec.md`;
les contrats techniques restent dans
`resolve-backlog-technical-decisions/specs/customer-api-contracts/spec.md`.
Cette change les implémente sans recopier ni remplacer leurs règles.

## Impact

- Changements applicatifs et de configuration runtime prévus uniquement par
  cette future change d’implémentation, après revue et application explicite.
- Aucun fichier d’une change antérieure, aucun contrat source et aucune tâche
  du backlog global ne sont modifiés ou cochés par la préparation de cette
  proposition.
- Aucun Account, frontend Angular, Kafka, Redis, assistant Spring AI/MCP,
  observabilité avancée, CI ou déploiement Kubernetes n’est inclus.
