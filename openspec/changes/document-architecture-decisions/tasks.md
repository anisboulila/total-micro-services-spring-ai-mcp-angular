# Tâches — Documenter les décisions d’architecture — Étape 3

> Périmètre : préparation documentaire pour revue. Les cases restent décochées
> tant que chaque tâche correspondante n’a pas été exécutée et vérifiée.

## 1. Préparer la traçabilité

- [x] 1.1 Relire les sources de l’Étape 1 et de l’Étape 2 ainsi que les analyses
  de référence nécessaires; conserver les niveaux **OBSERVÉ**, **DÉDUIT**,
  **DÉCIDÉ** et **À CONFIRMER**.
- [x] 1.2 Vérifier la correspondance entre les ADR proposés, les exigences/CON/H
  et les `DEC-xx` de l’Étape 2.

## 2. Examiner les ADR

- [x] 2.1 Revoir les regroupements et le périmètre des treize ADR; éliminer les
  redondances sans perdre de décision source.
- [x] 2.2 Revoir le statut, la justification, les alternatives, les compromis,
  conséquences et critères de réévaluation de chaque ADR.
- [x] 2.3 Confirmer que Kafka et son consommateur restent conditionnels à H-6,
  et que Spring AI/MCP restent conditionnels à UC-07.
- [x] 2.4 Confirmer le périmètre pédagogique limité de Redis et les positions
  sur Agent/RAG, Eureka/Config Server, observabilité, tests et Kubernetes/Helm.

## 3. Revue de cohérence et de périmètre

- [x] 3.1 Vérifier la cohérence entre `proposal.md`, la spécification,
  `design.md` et `tasks.md`, sans refaire le Target System Design.
- [x] 3.2 Vérifier que les exigences candidates et toutes les informations
  non confirmées restent explicitement non validées.
- [x] 3.3 Confirmer que seuls les quatre livrables de cette change ont été
  créés, qu’aucun fichier hors périmètre n’a été modifié et qu’aucun code ou
  runtime n’a été ajouté.
- [x] 3.4 Soumettre la préparation à la revue; ne pas exécuter `/opsx:apply`
  et ne pas archiver de change.
