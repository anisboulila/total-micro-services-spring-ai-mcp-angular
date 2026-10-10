# Target System Design — Étape 2 — Architecture cible

## Objectif et vocabulaire

Cette spécification définit ce que doit contenir et garantir le Target System
Design de notre projet. Elle ne décrit pas du code. Les décisions elles-mêmes
sont consignées dans `design.md` (identifiants `DEC-xx`).

Statuts utilisés :

- **OBSERVÉ** — constaté dans la référence Youssfi ou les documents existants.
- **DÉDUIT** — conséquence logique des besoins ou des exigences.
- **DÉCIDÉ** — décision d’architecture prise pour notre projet.
- **À CONFIRMER** — dépend d’une validation métier ou d’une information
  manquante.

## Requirement: Traçabilité des décisions vers les exigences

Chaque décision d’architecture DOIT être reliée à une exigence candidate de la
baseline `business-requirements` (cas d’usage UC-01 à UC-07, exigences
fonctionnelles, non fonctionnelles, contraintes) ou à une contrainte de
processus du projet.

#### Scenario: Décision reliée à son besoin

- **WHEN** une décision `DEC-xx` est consignée
- **THEN** elle identifie le besoin ou le problème traité et l’exigence ou la
  contrainte source
- **AND** elle porte un statut **DÉCIDÉ** ou **À CONFIRMER**

#### Scenario: Exigence non validée

- **WHEN** une décision dépend d’une exigence marquée **À CONFIRMER** à l’Étape 1
- **THEN** la dépendance est explicite et la décision est conditionnelle ou
  **À CONFIRMER**
- **AND** aucune exigence candidate n’est présentée comme validée par le
  propriétaire du produit

## Requirement: Alternatives, justification et compromis

Chaque choix technologique ou architectural important DOIT être justifié par un
besoin réel, comparé à ses principales alternatives et assorti de ses
compromis.

#### Scenario: Enregistrement de décision complet

- **WHEN** un choix important est consigné (style architectural, découpage,
  persistance, communication, passerelle, découverte, identité, résilience,
  AI/MCP/RAG/Agent, déploiement…)
- **THEN** l’enregistrement contient : besoin, alternatives, option retenue,
  raisons, compromis, statut et conditions de réévaluation
- **AND** les technologies présentes dans la référence ne sont pas retenues par
  défaut

#### Scenario: Technologie non retenue ou différée

- **WHEN** aucun besoin démontré ne justifie une technologie (par exemple un
  cache, un pipeline RAG, un agent autonome ou une découverte de services)
- **THEN** le design consigne explicitement qu’elle n’est pas retenue ou qu’elle
  est différée, avec le déclencheur qui justifierait de la réévaluer

## Requirement: Couverture des domaines d’architecture

Le Target System Design DOIT couvrir les domaines suivants.

#### Scenario: Structure et données

- **WHEN** `design.md` est examiné
- **THEN** il décrit l’architecture globale, le contexte et les frontières, les
  responsabilités des composants, la propriété des données, le choix de
  persistance et les règles de cohérence (y compris le traitement des
  transactions distribuées)

#### Scenario: Communication

- **WHEN** `design.md` est examiné
- **THEN** il distingue communication synchrone et asynchrone, justifie REST
  et, le cas échéant, les événements, et traite la passerelle d’API et la
  découverte de services comme des décisions à justifier

#### Scenario: Qualités transverses

- **WHEN** `design.md` est examiné
- **THEN** il couvre sécurité et identité, autorisation et propagation de
  l’identité, résilience (timeouts, retries, circuit breaker, idempotence),
  disponibilité, performance, scalabilité, observabilité (logs, métriques,
  traces), gestion des erreurs, configuration et secrets

#### Scenario: AI et canaux

- **WHEN** `design.md` est examiné
- **THEN** il relie Spring AI, MCP, RAG et Agent à des cas d’usage réels
  (notamment UC-07), justifie leur adoption ou leur non-adoption et traite les
  canaux conversationnels et le frontend

#### Scenario: Exploitation

- **WHEN** `design.md` est examiné
- **THEN** il couvre conteneurisation, orchestration, stratégie de
  déploiement, CI/CD, stratégie de test et de validation, risques, fragilités
  et trajectoire d’évolution

## Requirement: Questions d’entretien

Le design DOIT permettre de répondre de manière argumentée aux questions
d’entretien listées dans le design (architecture, services, Kafka, REST vs
événements, propriété des données, sécurité, doublons, pannes, scalabilité,
observabilité, MCP, Agent, RAG, évolution).

#### Scenario: Réponses traçables

- **WHEN** une question d’entretien du design est examinée
- **THEN** sa réponse renvoie à des décisions `DEC-xx` et à leurs compromis
- **AND** distingue ce qui est ferme de ce qui reste à confirmer

## Requirement: Cohérence et simplicité

Le Target System Design DOIT rester cohérent avec l’objectif pédagogique et
professionnel du projet et avec la règle de simplicité de `PROJECT-CONTEXT.md`.

#### Scenario: Pas de technologie gratuite

- **WHEN** la revue de cohérence est effectuée
- **THEN** chaque composant déployable et chaque technologie sont reliés à un
  besoin documenté
- **AND** l’objectif pédagogique, lorsqu’il motive un choix, est déclaré comme
  tel (contrainte **DÉCIDÉE** à l’Étape 1) et non présenté comme un besoin
  métier

#### Scenario: Hypothèses visibles

- **WHEN** le design repose sur une hypothèse
- **THEN** elle est listée comme hypothèse avec son impact si elle est fausse

## Requirement: Périmètre documentaire

Cette évolution DOIT rester documentaire.

#### Scenario: Aucune implémentation

- **WHEN** la change est examinée
- **THEN** elle ne contient aucun code applicatif, aucune dépendance, aucune
  configuration runtime et aucune copie de code de la référence
- **AND** les changes `project-foundation-architecture-baseline` et
  `define-business-requirements-and-target-system-design` ne sont ni modifiées
  ni archivées
- **AND** aucune tâche n’est marquée terminée sans réalisation vérifiée
