# Tâches — Valider le périmètre pédagogique

> Préparation et revue documentaires uniquement. Les exigences métier
> conditionnelles restent **À CONFIRMER**; aucune tâche n’est cochée à la
> création.

## 1. Établir la traçabilité

- [x] 1.1 Relire les décisions explicites du propriétaire et les sources des
  Étapes 1 à 3; distinguer preuve **OBSERVÉE**, besoin **DÉDUIT**, choix
  **DÉCIDÉ** et point **À CONFIRMER**.
- [x] 1.2 Vérifier les correspondances H-6, UC-07, DEC-07, DEC-15/16/17 et
  ADR-03/04/12 sans modifier les changes historiques.

## 2. Clarifier Kafka sans dupliquer DEC-07

- [x] 2.1 Consigner Kafka et l’objectif producteur/topic/consommateur comme
  décisions pédagogiques, sans en faire des besoins métier approuvés.
- [x] 2.2 Préserver les événements, l’outbox et les garanties de DEC-07; ne pas
  redéfinir leurs détails dans une spécification concurrente.
- [x] 2.3 Maintenir le scénario métier et le consommateur concret **À CONFIRMER**;
  présenter `audit-service` uniquement comme candidat.
- [x] 2.4 Confirmer l’exclusion de Kafka Streams et de `statistics-service` du
  périmètre initial et noter qu’un besoin de statistiques exigerait un change
  dédié.

## 3. Définir la portée Spring AI/MCP

- [x] 3.1 Examiner le parcours candidat « À quel client appartient ce compte ? »
  à partir des opérations API Customer et Account décrites.
- [x] 3.2 Confirmer que le parcours choisi est marqué comme proposé jusqu’à
  approbation et distinguer l’intégration pédagogique de la validation
  fonctionnelle de UC-07.
- [x] 3.3 Vérifier les garde-fous MCP : lecture seule, API métier, autorisation
  dans les services propriétaires, identité propagée selon une règle approuvée.
- [x] 3.4 Conserver les exclusions d’Agent et de RAG et les points ouverts sur le
  fournisseur LLM et les données.

## 4. Revue de cohérence et de périmètre

- [x] 4.1 Vérifier la cohérence entre la proposition, la spec, le design et les
  tâches ainsi que le statut de chaque décision.
- [x] 4.2 Vérifier qu’aucune exigence métier candidate n’est implicitement
  validée et qu’aucun statut historique n’est présenté comme une validation
  propriétaire.
- [x] 4.3 Vérifier les critères d’acceptation, dépendances et questions à
  arbitrer; aligner `PROJECT-CONTEXT.md` seulement après approbation.
- [x] 4.4 Confirmer l’absence de code, dépendance, configuration runtime ou
  déploiement; soumettre le change à revue sans apply ni archivage.
