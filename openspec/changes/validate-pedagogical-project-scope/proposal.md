# Valider le périmètre pédagogique — Étape 4 — Scope Validation

## Pourquoi

Les décisions explicites du propriétaire imposent un périmètre pédagogique
incluant Kafka, Spring AI et MCP. Le Target System Design (Étape 2) et les ADR
(Étape 3) conservent toutefois des statuts conditionnels pour Kafka/H-6 et
Spring AI/MCP/UC-07. Ce change doit clarifier ces statuts sans réécrire les
changes historiques ni promouvoir un scénario métier encore non confirmé.

## Modifications

- Formaliser les choix pédagogiques explicitement décidés, séparément des
  exigences métier candidates.
- Clarifier que Kafka est retenu pour l’apprentissage; H-6 ne conditionne plus
  ce choix pédagogique, mais le scénario et le consommateur métier concrets
  restent **À CONFIRMER**.
- Préserver les événements `CustomerCreated` et `AccountCreated` ainsi que les
  garanties et mécanismes déjà décrits par DEC-07.
- Formaliser l’intégration pédagogique de Spring AI/MCP et proposer un parcours
  concret, sans présenter ce parcours comme déjà validé.
- Clarifier les statuts de UC-07, DEC-07 et ADR-03/04/12 pour les travaux futurs,
  sans modifier les changes antérieures.

## Périmètre

Cette évolution prépare une clarification documentaire du périmètre
pédagogique, des questions ouvertes et de leur traçabilité dans OpenSpec.

Hors périmètre :

- tout code, dépendance, configuration runtime, déploiement ou implémentation ;
- Kafka Streams et `statistics-service` ;
- Agent et RAG ;
- l’adoption de `audit-service` comme consommateur métier sans confirmation ;
- la modification ou l’archivage des changes historiques.

## Capacités

### Nouvelle capacité

- `pedagogical-project-scope` : décisions pédagogiques du propriétaire,
  scénarios technologiques proposés, limites et critères d’acceptation à
  préserver dans les futures changes.

### Capacités modifiées

- Aucune dans les fichiers historiques. Cette change consigne une clarification
  transversale et sa correspondance avec les exigences, décisions et ADR
  existants.

## Impact

- Ajoute les artefacts de cette change, dont
  `specs/pedagogical-project-scope/spec.md`, conformément à la convention
  observée dans les changes existantes.
- S’appuie sur la baseline des exigences, le Target System Design et les ADR,
  en particulier DEC-07, DEC-15/16/17, H-6, UC-07 et ADR-03/04/12.
- Prévoit l’alignement de `PROJECT-CONTEXT.md` après validation documentaire.
- Ne publie pas directement une spec sous `openspec/specs/` et ne modifie aucune
  change historique.
