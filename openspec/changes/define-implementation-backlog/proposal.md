# Proposal

## Why

Le périmètre métier de la v1 et ses règles principales ont maintenant été
arbitrés, mais les documents existants restent une baseline d’exigences et des
choix d’architecture, pas un backlog ordonné prêt à implémenter. Cette change
les transforme en stories vérifiables, tâches techniques dépendantes et
incertitudes explicitement bloquantes, sans modifier les décisions historiques.

## What Changes

- Définir les stories `US-01` à `US-09`, leur rattachement à UC-01..UC-07 et
  leurs critères Given/When/Then à partir des arbitrages actuels du propriétaire.
- Spécifier le comportement v1 des parcours Customer/Account, du consommateur
  d’audit Kafka et de l’assistant Spring AI/MCP avec son historique.
- Traduire les décisions techniques déjà actées en tâches, sans les redécider;
  ordonner les tâches, tests et mises à jour documentaires selon leurs
  dépendances.
- Enregistrer les décisions techniques encore ouvertes, leur impact, leur
  dépendance, l’échéance d’arbitrage et les tâches bloquées.
- Garder cette change au stade de préparation : aucune tâche d’implémentation
  n’est exécutée ni cochée, et le backlog reste soumis à revue.

## Capabilities

### New Capabilities

- `customer-account-management`: parcours v1 d’inscription, de consultation et
  de création Customer/Account avec autorisations et règles métier arbitrées.
- `creation-event-audit`: publication des événements de création et conservation
  interne des métadonnées d’audit par le consommateur Kafka.
- `assisted-account-lookup`: recherche assistée en lecture seule du client
  propriétaire d’un compte et consultation de l’historique des conversations.

### Modified Capabilities

Aucune. Le dépôt ne contient pas encore de specs canoniques sous
`openspec/specs/`; les nouvelles capacités restent dans les deltas de cette
change jusqu’à revue et archivage ultérieurs.

## Impact

- Ajoute les artefacts de backlog à `openspec/changes/define-implementation-backlog/`
  uniquement.
- S’appuie sur `requirements.md`, `PROJECT-CONTEXT.md` et les changes des Étapes
  1 à 4, sans modifier ces sources ni réécrire H-6, UC-07 ou DEC-07.
- Aucun code applicatif, dépendance, fichier de configuration runtime,
  déploiement ou infrastructure n’est modifié.
- Toute implémentation attendra la revue et l’approbation explicites du backlog.
