# Documenter les décisions d’architecture — Étape 3 — ADR / Trade-offs

## Pourquoi

L’Étape 2 a défini une architecture cible et tracé ses choix sous forme de
`DEC-xx` dans `openspec/changes/define-target-system-design/design.md`.
L’Étape 3 rend ces choix plus faciles à examiner et à défendre en entretien,
au moyen d’ADR concis qui explicitent le problème, les options et les
conséquences. Elle ne refait pas le Target System Design et ne transforme pas
les exigences candidates en besoins validés.

## Modifications

- Regrouper les `DEC-01` à `DEC-22` dans des décisions d’architecture cohérentes,
  sans créer un ADR par détail d’implémentation.
- Décrire pour chaque ADR son statut, son contexte, la décision et sa
  justification, les alternatives, compromis, conséquences, critères de
  réévaluation et références.
- Préserver la distinction entre **OBSERVÉ**, **DÉDUIT**, **DÉCIDÉ** et
  **À CONFIRMER**, notamment les conditions H-6 (Kafka/consommateur) et UC-07
  (assistant/Spring AI/MCP).
- Garder la change documentaire et soumise à revue avant toute application.

## Périmètre

Cette change prépare les enregistrements ADR et leur spécification. Elle ne
modifie pas le contenu du Target System Design source, ne valide pas les
exigences métier et n’autorise aucune implémentation.

Hors périmètre :

- code applicatif, dépendances, configuration runtime, images, manifestes,
  pipeline ou scripts ;
- révision de `PROJECT-CONTEXT.md` ou des changes antérieures ;
- exécution de `/opsx:apply` et archivage de cette change ou d’une autre.

## Capacités

### Nouvelle capacité

- `architecture-decisions` : documentation traçable des décisions
  d’architecture et de leurs compromis à partir des `DEC-xx` de l’Étape 2.

### Capacités modifiées

- Aucune.

## Impact

- Ajoute `proposal.md`, `specs/architecture-decisions/spec.md`, `design.md` et
  `tasks.md` dans cette change, selon l’organisation OpenSpec des changes
  existantes.
- Sources : `PROJECT-CONTEXT.md`, `requirements.md`, la spécification
  `business-requirements`, les analyses de la référence dans
  `project-foundation-architecture-baseline`, et les quatre artefacts de
  `define-target-system-design`.
- La référence Youssfi reste une source d’observations attribuées, pas un
  blueprint ni une preuve de comportement local.
- Aucun code ni comportement runtime n’est modifié.
