# Spécification — Périmètre pédagogique du projet

## Vocabulaire et statut

Les statuts **OBSERVÉ**, **DÉDUIT**, **DÉCIDÉ** et **À CONFIRMER** conservent le
sens défini dans les documents des étapes précédentes.

Une décision pédagogique du propriétaire peut imposer l’étude ou l’intégration
d’une technologie sans valider pour autant le besoin métier, le scénario
d’affaires, les utilisateurs ou les règles d’accès correspondants.

## ADDED Requirements

### Requirement: Décisions pédagogiques distinctes des besoins métier

La documentation du périmètre DOIT distinguer les décisions pédagogiques
explicitement prises par le propriétaire des besoins métier qui restent
candidats.

#### Scenario: Choix pédagogique confirmé

- **WHEN** une technologie est obligatoire pour le fil rouge pédagogique
- **THEN** cette obligation est indiquée comme **DÉCIDÉE** au niveau du projet
- **AND** elle n’est pas présentée comme une exigence métier validée par ce seul
  fait
- **AND** le problème métier ou le cas d’usage concret reste traçable et
  **À CONFIRMER** tant qu’il n’a pas été approuvé

### Requirement: Kafka pédagogique et consommateur métier à confirmer

Le projet DOIT intégrer Kafka avec un producteur, un topic et un consommateur
réels dans une future évolution d’implémentation. Le choix pédagogique de Kafka
ne DOIT PAS être conditionné à H-6.

#### Scenario: Préserver les décisions de DEC-07

- **WHEN** le périmètre Kafka est spécifié
- **THEN** les événements `CustomerCreated` et `AccountCreated` et les éléments
  de fiabilité consignés dans DEC-07 sont préservés : transactional outbox,
  livraison au moins une fois, identifiants d’événement, idempotence du
  producteur et du consommateur, clés de partition par agrégat, retries bornés,
  file d’échecs et versionnement des événements
- **AND** la sélection du scénario métier et du consommateur concret reste
  **À CONFIRMER**
- **AND** `audit-service` est présenté comme candidat, pas comme exigence métier
  décidée
- **AND** Kafka Streams et `statistics-service` restent hors périmètre initial
- **AND** toute évolution vers des statistiques dérivées des événements requiert
  un change SDD/OpenSpec distinct si le besoin est confirmé

### Requirement: Intégration Spring AI/MCP à cas d’usage concret

Le fil rouge DOIT intégrer Spring AI et MCP au moyen d’un cas d’usage concret.
Le choix de la forme du cas d’usage proposé ne DOIT PAS être présenté comme
validé avant approbation du propriétaire.

#### Scenario: Outils MCP contrôlés

- **WHEN** un parcours assistant utilise MCP
- **THEN** les outils MCP sont en lecture seule et réutilisent les API métier
  des services propriétaires
- **AND** les contrôles d’identité et d’autorisation sont appliqués par les
  services propriétaires, pas délégués au modèle
- **AND** MCP n’expose aucune écriture métier et n’est pas rendu public par le
  Gateway
- **AND** les outils, données, utilisateurs et règles d’autorisation non
  approuvés restent **À CONFIRMER**

#### Scenario: Proposition de parcours non validée

- **WHEN** le parcours « À quel client appartient ce compte ? » est présenté
- **THEN** il est marqué comme proposition à examiner, non comme exigence
  produit approuvée
- **AND** la correspondance proposée avec UC-05 et UC-02 est vérifiée contre les
  contrats métier retenus avant toute implémentation

### Requirement: Harmonisation des décisions sans réécriture historique

La clarification DOIT établir le statut actuel de H-6, UC-07, DEC-07 et
ADR-03/04/12 sans modifier leur contenu historique.

#### Scenario: Statuts cohérents pour les travaux futurs

- **WHEN** les documents du nouveau périmètre sont examinés avec les changes
  antérieures
- **THEN** Kafka est présenté comme choix pédagogique **DÉCIDÉ**
- **AND** H-6 conserve l’incertitude sur le scénario métier et le consommateur
  concrets
- **AND** l’intégration pédagogique Spring AI/MCP est **DÉCIDÉE**
- **AND** le détail et l’approbation métier de UC-07 restent explicitement
  **À CONFIRMER**
- **AND** les références historiques à DEC-07 et ADR-03/04/12 sont conservées
  comme état des décisions des étapes précédentes

### Requirement: Documentation uniquement

Cette change DOIT rester documentaire.

#### Scenario: Aucun changement applicatif

- **WHEN** la change est préparée et revue
- **THEN** elle ne contient ni code, ni dépendance, ni configuration runtime,
  ni déploiement, ni implémentation
- **AND** aucune tâche n’est cochée avant son exécution et sa vérification
- **AND** aucun apply ni archivage n’est exécuté
