# Définir le Target System Design — Étape 2 — Architecture cible

## Pourquoi

L’Étape 1 a établi une baseline d’exigences **candidates** pour notre
reconstruction de zéro (`define-business-requirements-and-target-system-design`,
spécification `business-requirements`). La baseline d’architecture de la
référence Youssfi (`project-foundation-architecture-baseline`) a documenté les
choix observés, leurs limites et leurs risques, sans les adopter.

Avant toute implémentation, il faut transformer ces entrées en une
**architecture cible cohérente, défendable et traçable**. L’Étape 2 est la
première étape autorisée à **prendre des décisions d’architecture** : style
architectural, frontières, propriété des données, communication, sécurité,
résilience, observabilité, AI/MCP, déploiement et stratégie de test.

Règle directrice (`PROJECT-CONTEXT.md`, section 10) : aucune technologie n’est
ajoutée parce qu’elle figure dans la référence ou parce qu’elle est populaire.
Chaque choix important documente le besoin traité, les alternatives, la
justification, les compromis et son statut (ferme ou à confirmer).

## Modifications

- Produire le **Target System Design** de notre projet dans `design.md` :
  contexte et frontières, responsabilités, propriété des données, interactions,
  communication synchrone/asynchrone, sécurité et identité, résilience,
  cohérence, performance et scalabilité, observabilité, configuration et
  secrets, AI/MCP/RAG/Agent, frontend, déploiement, CI/CD, tests, risques et
  évolution.
- Consigner chaque décision importante sous forme d’enregistrement traçable
  (identifiant `DEC-xx`) relié aux exigences de l’Étape 1, avec alternatives,
  compromis et statut.
- Utiliser les statuts **OBSERVÉ**, **DÉDUIT**, **DÉCIDÉ** et **À CONFIRMER**.
  Une exigence candidate non validée n’est jamais transformée en certitude : la
  décision qui en dépend est **DÉCIDÉE** sous condition ou **À CONFIRMER**.
- Définir les critères de revue de la cohérence du design (traçabilité,
  absence de technologie gratuite, hypothèses explicites).
- Fournir une feuille de route d’implémentation ordonnée, **sans l’exécuter**,
  que de futures changes détailleront.

## Périmètre

Cette évolution couvre **uniquement l’Étape 2 — Target System Design /
Architecture cible**. Elle produit de la **documentation de conception**.

Hors périmètre de cette évolution :

- tout code applicatif, fichier de configuration runtime, dépendance Maven/npm,
  image, manifeste, pipeline ou script ;
- la validation des exigences métier par le propriétaire du produit (elles
  restent candidates) ;
- le détail d’implémentation de chaque service (futures changes
  d’implémentation) ;
- l’archivage des changes `project-foundation-architecture-baseline` et
  `define-business-requirements-and-target-system-design`, qui ne sont pas
  modifiées.

## Capacités

### Nouvelles capacités

- `target-system-design` : exigences portant sur le contenu, la traçabilité et
  la qualité du Target System Design (architecture, décisions, justifications,
  risques) servant de base aux changes d’implémentation.

### Capacités modifiées

- Aucune.

## Impact

- Ajoute les artefacts OpenSpec de cette change : `proposal.md`,
  `specs/target-system-design/spec.md`, `design.md`, `tasks.md`.
- S’appuie sur `PROJECT-CONTEXT.md`, sur
  `openspec/changes/define-business-requirements-and-target-system-design/`
  (spécification, `design.md`, `proposal.md`), sur
  `openspec/changes/project-foundation-architecture-baseline/` et sur la
  référence Youssfi (`main`, commit
  `bf4c7f2750e4c731662e8138023fd8e6b4e4a475`) uniquement comme référence
  pédagogique.
- Le workspace local reste distinct du dépôt de référence ; aucun code de la
  référence n’est copié.
- Aucun code applicatif, aucune dépendance ni aucune configuration runtime
  n’est modifié. Aucune tâche n’est cochée à la création.
