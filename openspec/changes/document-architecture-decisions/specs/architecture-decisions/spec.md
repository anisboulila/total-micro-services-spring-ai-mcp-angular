# Spécification — Décisions d’architecture (ADR) — Étape 3

## Vocabulaire et statut

Les niveaux de preuve restent **OBSERVÉ**, **DÉDUIT**, **DÉCIDÉ** et
**À CONFIRMER**, conformément à `PROJECT-CONTEXT.md` et aux changes précédentes.
Ils qualifient les faits, besoins candidats et décisions sources; ils ne
remplacent pas le statut d’un ADR.

Le statut d’un ADR décrit la posture proposée pour la décision cible :
**proposé**, **accepté**, **conditionnel**, **différé** ou **rejeté**. Dans cette
change préparée pour revue, le statut des fiches indique la posture issue de
l’Étape 2 et ne signifie pas que la revue de l’Étape 3 a eu lieu. Une décision
conditionnelle ne valide pas l’exigence dont elle dépend.

## ADDED Requirements

### Requirement: Enregistrements ADR complets et traçables

Chaque ADR DOIT présenter un identifiant et un titre, un statut, le contexte et
le problème, la décision avec sa justification, les alternatives, avantages,
inconvénients et compromis, les conséquences techniques et opérationnelles,
les conditions de réévaluation, ainsi que les références aux exigences et aux
`DEC-xx` sources.

#### Scenario: Décision vérifiable

- **WHEN** un ADR est examiné
- **THEN** le lecteur peut retrouver la décision correspondante de l’Étape 2,
  les raisons du choix et les principales conséquences
- **AND** l’ADR distingue les observations sur la référence des choix pour le
  projet cible

### Requirement: Exigences candidates et décisions conditionnelles

Les ADR DOIVENT préserver les statuts et incertitudes des sources; aucune
exigence candidate ou hypothèse ne peut être présentée comme validée.

#### Scenario: Dépendance métier non confirmée

- **WHEN** une décision dépend de H-6 ou de UC-07
- **THEN** cette condition et son effet sur l’adoption sont explicites
- **AND** le consommateur Kafka reste **À CONFIRMER** sous H-6
- **AND** l’assistant et MCP restent conditionnels à UC-07

### Requirement: Regroupement lisible sans redondance

Les ADR DOIVENT regrouper les décisions connexes lorsque leur justification et
leurs compromis forment un même choix architectural, et référencer les `DEC-xx`
sans recopier le Target System Design.

#### Scenario: Détail non structurant

- **WHEN** un détail relève de l’implémentation plutôt que d’un trade-off
  architectural autonome
- **THEN** il est référencé dans l’ADR parent ou dans la source, sans créer une
  fiche indépendante artificielle

### Requirement: Documentation uniquement

La change DOIT rester limitée à ses livrables OpenSpec documentaires.

#### Scenario: Périmètre de la change

- **WHEN** la change est préparée pour revue
- **THEN** seuls `proposal.md`, `specs/architecture-decisions/spec.md`,
  `design.md` et `tasks.md` sont ajoutés
- **AND** aucune change existante ni `PROJECT-CONTEXT.md` n’est modifié
- **AND** aucune tâche n’est cochée sans réalisation vérifiée
- **AND** aucun code, dépendance, paramètre runtime, `/opsx:apply` ou archivage
  n’est introduit ou exécuté
