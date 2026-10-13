# Proposal

## Why

Le backlog v1 fixe les parcours métier Customer/Account mais conserve plusieurs
décisions techniques qui bloquent les premiers contrats REST, la persistance,
la sécurité et leurs tests. Cette change consigne des choix techniques simples
et testables pour débloquer le premier incrément Customer, sans implémenter de
code.

## What Changes

- Retenir un contrat REST pour validation, pagination et erreurs structurées
  des API Customer/Account.
- Retenir les conventions de migrations PostgreSQL, de tests du premier
  incrément Customer et de propagation/autorisation OIDC.
- Fixer une stratégie d’idempotence compatible avec le périmètre,
  les portées, la concurrence, la rétention d’une semaine et le rejeu exact du
  résultat.
- Distinguer ces décisions techniques des choix qui ne bloquent pas encore le
  premier incrément et seront résolus dans leurs changes fonctionnelles.

## Capabilities

### New Capabilities

- `customer-api-contracts`: contrats techniques retenus pour les échanges
  REST, l’identité, la validation, la pagination, les erreurs et l’idempotence
  des parcours Customer/Account.

### Modified Capabilities

Aucune. Les exigences métier existantes restent dans
`define-implementation-backlog`; cette change en précise les contrats
techniques sans modifier les changes antérieures ni leurs statuts historiques.

## Impact

- Change documentaire limitée à
  `openspec/changes/resolve-backlog-technical-decisions/`.
- Sources prises en compte : backlog v1 et décisions DEC-04, DEC-06, DEC-10,
  DEC-11, DEC-13, DEC-14 et DEC-22.
- Aucun code, module Maven, projet Angular, configuration runtime, dépendance
  ou déploiement n’est créé.
- Les décisions documentaires ne réalisent aucune tâche d’implémentation;
  aucune tâche du backlog global n’est cochée.
