# Tasks

> Backlog proposé, non approuvé et non exécuté. Les priorités indiquent l’ordre
> de dépendance suggéré, pas un nouvel arbitrage métier. Chaque case reste
> décochée jusqu’à réalisation et vérification effective.

## 1. Arbitrer les décisions techniques préalables

- [ ] 1.1 **P0 — TD-03/04/14** Fixer le contrat de validation, les erreurs de requête, l’ordre des listes et le dépassement de page au-delà de 100; vérifier par une table requête/réponse liée aux scénarios US-01 à US-06.
- [ ] 1.2 **P0 — TD-07/08** Choisir l’outil de migration et les outils de tests d’intégration, de contrat, UI et IA; vérifier par décisions documentées et un test de preuve reproductible.
- [ ] 1.3 **P0 — TD-06, DÉCIDÉ** Appliquer les rôles/claims Keycloak déjà décidés et la propagation contrôlée du bearer token original vers les services et MCP du POC, avec validation indépendante des audiences; vérifier par une matrice d’accès couvrant client, admin et accès croisé refusé, sans introduire de token exchange dans cette évolution.
- [ ] 1.4 **P0 — TD-05** Définir l’empreinte du corps, le traitement de clé expirée et la persistance du résultat d’idempotence; vérifier par une table de décision qui conserve les comportements déjà arbitrés.
- [ ] 1.5 **P2 — TD-09** Fixer le TTL Redis et le repli en cas de panne, puis déterminer si les résultats négatifs sont mis en cache et nécessitent une invalidation après création; vérifier que le cache reste limité aux lectures Customer et n’ajoute aucun cache métier.
- [ ] 1.6 **P1 — TD-01** Décider nom/topologie du topic, sérialisation, enveloppe et payload de `CustomerCreated`/`AccountCreated`; vérifier le contrat versionné et les exemples consommables.
- [ ] 1.7 **P1 — TD-02** Décider le relais outbox, les bornes/délais de retry et le traitement de la file d’échecs; vérifier par un scénario de panne, reprise et poison event.
- [ ] 1.8 **P1 — TD-10** Décider le stockage interne d’audit, le sens technique d’un mois et la cadence de purge; vérifier que seuls les champs d’audit arbitrés sont stockés et qu’aucune API de consultation n’est prévue.
- [ ] 1.9 **P1 — TD-11** Choisir le modèle/version local, le transport MCP, le prompt, les timeouts, la limite d’un appel outil et la stratégie de tests déterministes; vérifier une configuration reproductible sans fournisseur distant et sans imposer encore évaluation statistique ni métriques de coût.
- [ ] 1.10 **P1 — TD-12** Concevoir séparément le stockage d’historique, la définition d’une conversation, la rétention configurable, la purge et les erreurs à historiser; vérifier un contrat sans jeton/secret et conforme aux accès client/opérateur approuvés.
- [ ] 1.11 **P1 — TD-13** Choisir le backend d’observabilité dans les standards DEC-13; vérifier que le choix fonctionne dans le profil local sans introduire de SLO non décidé.

## 2. Préparer l’exécution locale et l’identité

- [x] 2.1 **P0 — DEC-19, CON-3** Préparer l’environnement local Compose pour PostgreSQL, Keycloak et le socle applicatif; vérifier le démarrage, les contrôles de disponibilité et la persistance des données après redémarrage, puis documenter les commandes. Kafka et Redis sont ajoutés avec leurs tâches dédiées. **Preuve :** `docker compose config` valide; `docker compose ps` montre PostgreSQL, Customer et Gateway `healthy`, Keycloak actif; migrations Compose et persistance après redémarrage vérifiées par les suites et la recette consignée.
- [x] 2.2 **P0 — DEC-10, US-01** Configurer l’inscription Keycloak, les claims `sub`/e-mail vérifié et les rôles client/opérateur attribués manuellement; vérifier les scénarios d’authentification et de refus d’auto-attribution admin. **Preuve :** realm local et audience configurés; `CustomerKeycloakPostgresIT` utilise de vrais JWT Keycloak; création Customer, rôles OPERATOR/dual, audience et absence d’auto-attribution sont couverts.

## 3. Construire les parcours Customer

- [x] 3.1 **P0 — US-01, UC-03, DEC-03/04/10** Créer le modèle et la persistance Customer avec unicité du `sub` et de l’e-mail, puis l’API d’auto-création liée à l’identité; vérifier migrations, nom vide, claim absent/non vérifié, doublons HTTP 409 et refus de création par admin. **Preuve :** `POST /api/v1/customers`, migrations V1/V2, contraintes PostgreSQL et tests MVC/Testcontainers réussis; 201/Location, 400, 403 et 409 couverts.
- [x] 3.2 **P0 — US-01, DEC-11** Ajouter l’idempotence de création Customer selon la portée et la rétention arbitrées; vérifier rejeu exact, contenu différent HTTP 409, concurrence et absence de doublon. **Preuve :** persistance atomique, expiration enregistrée à sept jours, rejeu, conflit, rollback, acteur distinct et concurrence PostgreSQL couverts par `CustomerCreationPostgresIT`; multi-instance explicitement hors preuve.
- [x] 3.3 **P0 — US-02/03, UC-01/02, DEC-06/10** Exposer liste et détail Customer paginés et filtrés au service propriétaire; vérifier client limité à sa fiche, admin global, tailles 20/100 configurables et HTTP 404 uniforme pour absent/non autorisé. **Preuve :** `GET /api/v1/customers` et `GET /{id}`, filtrage par `sub`, rôles, pagination déterministe, validation et accès croisé couverts par les tests Customer.
- [ ] 3.4 **P0 — US-01/02/03, DEC-18** Construire les vues Angular d’inscription/profil et de consultation Customer pour client/admin; vérifier les parcours de rôle et l’absence de donnée étrangère dans les tests UI.

## 4. Construire les parcours Account

- [ ] 4.1 **P0 — US-04/05, UC-04/05, DEC-03/04/06/10** Créer le modèle Account et ses API liste/détail paginées avec filtrage propriétaire; vérifier le lien opaque vers Customer, la cardinalité, les vues client/admin, les valeurs de pagination et HTTP 404 uniforme.
- [ ] 4.2 **P0 — US-06, UC-06, DEC-05/10/11** Implémenter la création d’un compte courant EUR à solde nul, dériver le propriétaire du `sub` client et autoriser l’admin à fournir un Customer existant; vérifier rejet du `customerId` client, HTTP 404 pour cible absente, HTTP 503 si Customer est indisponible et absence de persistance partielle.
- [ ] 4.3 **P0 — US-06, DEC-11** Ajouter l’idempotence de création Account selon les décisions préalables; vérifier résultat initial exact, clé/contenu différent HTTP 409, concurrence, portée acteur/opération et rétention d’une semaine.
- [ ] 4.4 **P0 — US-04/05/06, DEC-18** Construire les vues Angular de liste/détail et de création Account; vérifier par tests UI que le client ne peut cibler qu’un compte personnel et que l’admin peut choisir un client existant.
- [ ] 4.5 **P0 — DEC-05/06, US-01..US-06** Exposer les API Customer et Account via l’API Gateway; vérifier le routage et la transmission de l’identité, en laissant l’autorisation aux services propriétaires et en conservant le masquage HTTP 404 des ressources non autorisées.

## 5. Ajouter le cache pédagogique borné

- [ ] 5.1 **P2 — DEC-12, US-03** Ajouter le cache Redis partagé uniquement à la lecture Customer par identifiant; vérifier TTL décidé, repli PostgreSQL en cas d’indisponibilité et absence de cache de solde/session/rate limiting. N’ajouter l’invalidation après création que si la décision TD-09 retient la mise en cache des résultats négatifs.

## 6. Publier et auditer les événements Kafka

- [ ] 6.1 **P1 — US-07, DEC-07** Préparer Kafka pour l’exécution locale et implémenter le contrat décidé pour `CustomerCreated` et `AccountCreated` avec identifiant, version et partitionnement par agrégat; vérifier la compatibilité avec des tests de contrat versionnés.
- [ ] 6.2 **P1 — US-07, DEC-07** Publier depuis l’outbox transactionnelle avec la livraison au moins une fois et l’idempotence producteur; vérifier atomicité métier/outbox, retries bornés et routage des échecs selon les décisions TD-01/02.
- [ ] 6.3 **P1 — US-07, DEC-07** Implémenter le consommateur `audit-service` idempotent et le stockage interne limité aux métadonnées arbitrées; vérifier duplications, reprise, rétention d’un mois, purge et absence d’API de consultation.

## 7. Construire progressivement l’assistant puis son historique

Les tâches 7.1 à 7.4 décrivent des jalons globaux; leur découpage détaillé,
leurs apprentissages et leurs vérifications restent dans
`introduce-agentic-profile-lookup` afin d’éviter une duplication de backlog.

- [ ] 7.1 **P1 — DEC-15, préparation US-03/US-08** Valider Spring AI sans outil métier : `ChatClient`, modèle local, configuration, prompt, réponse et erreur; vérifier le parcours isolé par des tests déterministes sans donnée Customer ni route publique.
- [ ] 7.2 **P1 — DEC-15, préparation US-03/US-08** Introduire le Tool Calling avec un outil Java déterministe limité au test/local; vérifier choix, argument, résultat structuré, refus d’outil inconnu et absence d’écriture, sans dépendance MCP ni donnée métier.
- [ ] 7.3 **P1 — DEC-16, préparation US-03/US-08** Remplacer progressivement l’appel local par MCP interne avec découverte et transport client/serveur; vérifier l’équivalence de l’outil pédagogique, l’isolation réseau et les erreurs contrôlées, sans route Gateway vers MCP.
- [ ] 7.4 **P1 — US-03, DEC-10/15/16, TD-06/11** Relier l’outil final à la lecture Customer sécurisée par JWT, avec un adaptateur MCP interne et sans argument d’identité fourni par le modèle; vérifier filtrage par `sub`, accès croisé refusé, DTO validé, lecture seule, un appel maximum, timeouts et absence de retry métier.
- [ ] 7.5 **P1 — US-08, UC-07/05/02, DEC-15/16** Étendre le socle validé avec l’outil Account et une orchestration bornée Account puis Customer; vérifier identité propagée, autorisation des deux services, faits issus des DTO, sortie limitée à l’identifiant du compte et au nom, réponse neutre et indisponibilité contrôlée.
- [ ] 7.6 **P1 — US-09, UC-07, NFR-1, TD-12** Persister l’historique complet sans jetons/secrets, configurer sa rétention d’un mois et purger automatiquement; vérifier confidentialité, accès client à son historique, accès admin global et absence de suppression manuelle.
- [ ] 7.7 **P1 — US-08/09, DEC-18** Construire l’interface Angular du parcours assistant et de l’écran d’historique; vérifier les rôles, la restitution exacte depuis les résultats validés, l’affichage neutre des échecs et le cloisonnement des conversations.

## 8. Exploiter, observer et documenter

- [ ] 8.1 **P1 — DEC-13/14** Instrumenter les services avec le backend d’observabilité décidé et séparer configuration/secrets; vérifier corrélation des requêtes, absence de données sensibles dans les logs et démarrage local.
- [ ] 8.2 **P1 — DEC-21/22, REQ-011** Ajouter GitHub Actions pour build, tests, contrôles qualité et images; vérifier la réussite du pipeline sur les tests unitaires, sécurité, intégration et contrats choisis.
- [ ] 8.3 **P1 — REQ-012/013** Mettre à jour la documentation pédagogique et d’architecture après réalisation des stories concernées; vérifier que les faits, décisions, compromis et incertitudes reflètent le runtime livré sans modifier rétroactivement H-6/UC-07/DEC-07.

## 9. Vérifier la version intégrée

- [ ] 9.1 **P0 — US-01..US-09** Exécuter les parcours bout-en-bout locaux, y compris contrôle d’accès croisé, créations idempotentes, rejeu Kafka, consultation/purge d’historique et redémarrage; vérifier tous les critères Given/When/Then et documenter les écarts avant de déclarer la v1 terminée.
