# Tasks

> Arbitrages documentaires finalisés pour le premier incrément Customer.
> Les cases indiquent les décisions documentées et contrôles documentaires
> réalisés; elles ne signifient pas qu’un comportement runtime ou un test
> applicatif a été exécuté.

## 1. Vérifier les sources et les décisions existantes

- [x] 1.1 Comparer le contrat retenu aux décisions DEC-04, DEC-06, DEC-10/11, DEC-13/14 et DEC-22, ainsi qu’aux exigences US-01..US-06; consigner toute divergence sans modifier les sources historiques.
- [x] 1.2 Confirmer que les règles métier arbitrées restent inchangées, notamment les 404 de masquage, les 404/503 de création Account, les conflits 409, les tailles de page 20/100 et la rétention d’idempotence de sept jours.

## 2. Faire arbitrer les contrats techniques proposés

- [x] 2.1 Retenir la validation d’entrée, les statuts/codes d’erreur et le format structuré commun; vérifier par les scénarios et exemples HTTP.
- [x] 2.2 Retenir l’index de pagination, le tri déterministe et la politique au-delà de 100; vérifier par l’exemple de réponse de collection.
- [x] 2.3 Retenir les rôles/claims Keycloak et la stratégie de propagation du jeton; vérifier par les règles d’autorisation et de validation indépendante dans la spec.
- [x] 2.4 Retenir la portée, l’empreinte, la concurrence, les résultats persistés, la politique d’expiration et les réponses d’idempotence; vérifier par les scénarios nominaux, concurrents et conflictuels.
- [x] 2.5 Retenir l’outil de migration et la stratégie automatisée du premier incrément Customer; consigner que les choix de tests UI, Kafka et IA seront arbitrés dans leurs changes dédiées.

## 3. Vérifier la cohérence documentaire

- [x] 3.1 Vérifier que proposal, spec, design et tasks distinguent les décisions déjà actées, les décisions techniques de cette change et les choix différés, sans changer les anciennes changes ni les statuts H-6, UC-07 et DEC-07.
- [x] 3.2 Vérifier les exemples HTTP, les scénarios de spec et les tâches contre chaque contrat retenu; exécuter `openspec validate resolve-backlog-technical-decisions` et `openspec status --change resolve-backlog-technical-decisions`.
