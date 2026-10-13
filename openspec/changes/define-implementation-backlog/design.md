# Design — Backlog d’implémentation v1

## Context

La baseline de l’Étape 1 décrit UC-01..UC-07 comme capacités observées ou
candidates (`define-business-requirements-and-target-system-design/specs/business-requirements/spec.md:69-178`).
Les décisions d’architecture des Étapes 2 et 3 fixent les frontières et
principes techniques, mais conservent des paramètres et dépendances ouverts
(`define-target-system-design/design.md:90-225, 289-414, 429-478, 525-625`;
`document-architecture-decisions/design.md:91-145, 330-358`).
L’Étape 4 clarifie l’obligation pédagogique de Kafka et de Spring AI/MCP sans
modifier les décisions historiques
(`validate-pedagogical-project-scope/design.md:24-103`).

Les arbitrages récapitulés ci-dessous proviennent des réponses explicites du
propriétaire dans cet échange. Ils décrivent le périmètre actuel du produit;
ils ne modifient pas rétroactivement les étiquettes ni le texte des anciennes
changes. L’absence de code applicatif dans la baseline et les constats du dépôt
Youssfi ne sont pas des preuves de comportement de la v1 locale.

## Goals / Non-Goals

**Goals:**

- Rendre chaque parcours du périmètre v1 traçable vers UC-xx, priorisé et
  vérifiable par des critères d’acceptation.
- Distinguer décisions métier du propriétaire, décisions techniques déjà
  actées, objectifs pédagogiques, paramètres techniques ouverts et informations
  introuvables.
- Rendre visibles les dépendances entre APIs Customer/Account, sécurité,
  événements Kafka, assistant, historique, tests et exécution locale.

**Non-Goals:**

- Décrire l’implémentation au niveau des classes, des manifests ou des valeurs
  de configuration runtime.
- Ajouter les opérations de mise à jour/suppression, les transactions bancaires,
  les notifications, les statistiques Kafka ou des canaux de messagerie.
- Reprendre les statuts historiques de H-6, UC-07 ou DEC-07 comme s’ils avaient
  été réécrits ou validés dans leurs changes sources.
- Promettre un SLO, une disponibilité de production ou une cible de charge non
  arbitrés.

## Décisions

### Niveaux de preuve et de décision

| Étiquette | Usage dans ce backlog |
|---|---|
| **OBSERVÉ** | Fait relevé dans la référence Youssfi ou un document antérieur; pas une preuve de comportement local. |
| **CONFIRMÉ — arbitrage métier** | Décision explicite du propriétaire dans l’échange courant; base des critères v1. |
| **DÉCIDÉ — technique** | Décision d’architecture source, notamment DEC-xx; ne pas la redécider dans une story. |
| **HYPOTHÈSE PÉDAGOGIQUE** | Justifie un objectif d’apprentissage, mais ne prouve pas un besoin métier. |
| **À ARBITRER — technique** | Détail d’implémentation non choisi; inscrit au registre avec tâche bloquée ou non. |
| **INFORMATION INTROUVABLE** | Non documentée dans les sources consultées; ne pas inventer de contrat. |

### Arbitrages métier confirmés

| Domaine | Décision actuelle du propriétaire |
|---|---|
| Périmètre | UC-01 à UC-06 sont inclus dans la v1. Aucun UC supplémentaire n’est ajouté. |
| Identité et profil | La personne s’inscrit d’abord dans Keycloak, puis crée son profil Customer. Le profil est rattaché au `sub` OIDC; `name` est non vide; `email` vient du claim vérifié, n’est pas modifiable par UC-03 et est unique. |
| Acteurs | Un client agit sur ses propres données; opérateur/admin agit sur l’ensemble, sauf création de fiche Customer qui reste réservée à l’auto-inscription. Le rôle opérateur/admin est provisionné manuellement par un administrateur Keycloak. |
| Compte | Un compte courant en EUR, avec solde initial nul; exactement un client propriétaire et plusieurs comptes permis par client. Le client n’envoie pas `customerId`; le service le dérive du `sub`. L’admin peut cibler un client existant. |
| Accès | Les services propriétaires appliquent l’autorisation. Une ressource absente ou non autorisée est masquée par HTTP 404. Le `customerId` envoyé par un client lors de la création est rejeté. |
| API | 404 si le client visé pour un compte n’existe pas; 503 si `customer-service` est indisponible; 409 pour e-mail dupliqué ou clé d’idempotence réutilisée avec un contenu différent. Listes configurables : 20 éléments par défaut, maximum 100. |
| Idempotence API | Clé par acteur et opération dans chaque service; même clé/contenu rejoue exactement le résultat initial; une requête concurrente attend puis reçoit ce résultat; conservation d’une semaine. |
| Kafka/audit | `audit-service` consomme les créations Customer/Account. La trace ne conserve que `eventId`, type, `aggregateId`, date et version, pendant un mois; aucun accès fonctionnel à ces traces. |
| Assistant | UC-07 est approuvé pour « À quel client appartient ce compte ? ». L’assistant utilise un LLM local et ne restitue que l’identifiant du compte et le nom du client. Un compte absent ou inaccessible donne une réponse neutre; une indisponibilité donne un message contrôlé et permet une nouvelle tentative. |
| Historique | Questions et réponses complètes sont persistées sans jetons ni secrets; l’historique est consultable dans un écran par son propriétaire et par les opérateurs/admins. La rétention est configurable à un mois, avec purge automatique et sans suppression manuelle. |
| Qualité opérationnelle | Exécution locale et redémarrage en v1; aucun seuil numérique de latence, performance, disponibilité ou rétablissement. |

Les règles de rejet qui n’ont pas de code HTTP arbitré (par exemple nom vide ou
`customerId` soumis par un client) restent des comportements de rejet confirmés,
mais leur statut HTTP exact est une décision technique ouverte.

### Décisions techniques déjà actées par les Étapes 2–4

- **DEC-01..06, DEC-08..11** : deux domaines Customer/Account, propriété de
  données séparée, PostgreSQL, cohérence locale, REST synchrone par défaut,
  Gateway, Keycloak/OIDC avec validation des JWT à chaque service, résilience
  et contrat d’idempotence. Les choix se trouvent dans
  `define-target-system-design/design.md:90-193, 289-414`.
- **DEC-07** : conserver `CustomerCreated`, `AccountCreated`, transactional
  outbox, livraison au moins une fois, identifiant et idempotence des
  événements, partition par agrégat, retries bornés, file d’échecs et schéma
  versionné (`define-target-system-design/design.md:194-225`). Le scénario audit
  est maintenant choisi par arbitrage propriétaire; la formulation conditionnelle
  historique reste inchangée.
- **DEC-12** : Redis est décidé pour sa valeur pédagogique, après disponibilité
  des lectures Customer, limité au cache distribué de lecture Customer;
  PostgreSQL reste source de vérité. Aucun cache de solde, session ou rate
  limiting. TTL et comportement de secours restent ouverts
  (`define-target-system-design/design.md:429-478`).
- **DEC-13/14, DEC-15..19, DEC-21/22** : standards d’observabilité, séparation
  configuration/secrets, forme de l’assistant, MCP interne lecture seule,
  exclusion d’Agent/RAG, frontend Angular unique, Docker Compose, GitHub Actions
  et pyramide de tests. Les backends, outils et paramètres encore ouverts sont
  consignés plus bas
  (`define-target-system-design/design.md:429-478, 525-625`).
- **DEC-20 / Helm** : Kubernetes reste conditionnel et Helm différé; ils ne
  figurent pas dans le backlog d’implémentation v1.

### Stories, rattachement et ordre indicatif

Les priorités P0/P1/P2 sont des **propositions de séquencement**, pas des
arbitrages métier. P2 ne signifie pas que Redis est abandonné : sa tâche suit
les lectures Customer parce que DEC-12 la rend dépendante de celles-ci.

| Story | Résumé | UC associés | Priorité proposée |
|---|---|---|---|
| US-01 | S’inscrire et créer son profil Customer | UC-03 | P0 |
| US-02 | Lister les fiches Customer autorisées | UC-01 | P0 |
| US-03 | Consulter une fiche Customer autorisée | UC-02 | P0 |
| US-04 | Lister les comptes autorisés | UC-04 | P0 |
| US-05 | Consulter un compte autorisé | UC-05 | P0 |
| US-06 | Créer un compte courant autorisé | UC-06 | P0 |
| US-07 | Consommer et auditer les événements de création | UC-03, UC-06 (déclencheurs) | P1 |
| US-08 | Demander quel client possède un compte | UC-07, UC-05, UC-02 | P1 |
| US-09 | Consulter l’historique de conversation autorisé | UC-07 | P1 |

US-07 est un scénario système confirmé, pas un nouveau cas d’usage métier ni
une API de consultation d’audit. La Gateway, Redis, Compose, l’observabilité,
la CI et les tests restent des tâches techniques rattachées aux décisions
DEC-xx, pas de nouvelles capacités utilisateur.

### Dépendances entre parcours

```text
API Gateway ──► API Customer/Account (autorisation dans les services propriétaires)
Keycloak / rôles ──► US-01 ──► US-02, US-03
                         └──► US-06 (compte client)
US-01 + US-03 ──► US-04, US-05 ──► US-08
US-01 + US-06 ──► US-07 (outbox, Kafka, audit-service)
US-08 ──► US-09 (historique et écran)
US-03 ──► tâche Redis DEC-12
```

Les tests et la documentation nécessaires sont livrés avec chaque groupe de
tâches concerné; le groupe final ne contient que des vérifications
d’intégration globale.

### Registre des décisions techniques restantes

`Bloque` indique la première tâche d’implémentation qui ne peut pas commencer
sans arbitrage; cela ne retarde pas les stories sans dépendance vers ce point.

| ID | Point technique à trancher | Impact | Dépend de | Arbitrer au plus tard | Bloque |
|---|---|---|---|---|---|
| TD-01 | Nom/topologie du topic Kafka, sérialisation, enveloppe et champs de payload au-delà des métadonnées décidées | Compatibilité producteur/consommateur et contrats de test | US-01, US-06 | Avant le contrat d’événement | T-6.1, T-6.2, T-6.3 |
| TD-02 | Mode de relais outbox; bornes et délais de retry; stratégie et rétention de la file d’échecs | Risque de perte, reprise et exploitation | TD-01, DEC-07 | Avant le publisher | T-6.2, T-6.3 |
| TD-03 | Détails de validation non métier (longueur/normalisation de `name`, claim email absent/non vérifié, corps d’UC-06 pour les champs fixés, statut de rejet `customerId`) | Forme des DTO et assertions HTTP | US-01, US-06 | Avant contrats API | T-3.1, T-4.2 |
| TD-04 | Ordre stable des listes et traitement d’un `pageSize` supérieur à 100 (rejet ou plafonnement) | Contrat API et pagination testable | UC-01, UC-04 | Avant API de listes | T-3.3, T-4.1 |
| TD-05 | Empreinte canonique du corps, réutilisation après expiration, erreurs idempotentes autres que le conflit déjà arbitré | Rejeu fiable et stockage des réponses | Arbitrages idempotence | Avant POST idempotents | T-3.2, T-4.3 |
| TD-06 | Noms des rôles/claims Keycloak et propagation du jeton utilisateur vers les services et outils MCP (transfert ou échange) | Autorisations par service et confidentialité | DEC-10/16, US-01, US-08 | Avant sécurisation de la Gateway et de MCP | T-2.2, T-4.5, T-7.1 |
| TD-07 | Outil de migration PostgreSQL, contraintes exactes/indices et représentation persistée de la devise EUR | Évolution du schéma et intégrité | DEC-03/04 et arbitrages Customer/Account | Avant migrations métier | T-3.1, T-4.1, T-4.2 |
| TD-08 | Outils de tests d’intégration/contrat/UI, Testcontainers ou équivalent, simulation du LLM | Répétabilité de la stratégie DEC-22 | Décisions d’outillage | Avant les suites automatisées | T-1.2, tâches de test |
| TD-09 | TTL Redis, comportement en panne et éventuelle invalidation; aucune modification/suppression Customer n’est dans le périmètre, et l’invalidation à la création n’est utile que si des résultats négatifs sont mis en cache | Fraîcheur et disponibilité du cache | DEC-12, US-03 | Avant la tâche Redis | T-5.1 |
| TD-10 | Stockage de l’audit, sémantique de « un mois », fréquence de purge et accès opérationnel | Confidentialité et fin de rétention | US-07 | Avant le consumer d’audit | T-6.3 |
| TD-11 | Modèle local/version, limites/timeout, prompt et définition des résultats d’évaluation | Disponibilité, coût local et reproductibilité IA | DEC-15, US-08 | Avant l’intégration LLM | T-7.1 |
| TD-12 | Stockage des conversations, définition d’une conversation, configuration de rétention et purge; traitements d’erreur à historiser | Données personnelles et consultation admin/client | US-08, US-09 | Avant stockage/UI d’historique | T-7.2 |
| TD-13 | Backend d’observabilité et instrumentation retenue dans les standards ouverts | Exploitation locale sans introduire un backend non arbitré | DEC-13 | Avant Compose d’observabilité | T-8.1 |
| TD-14 | Forme du contrat d’erreur partagé et codes pour les rejets de validation hors 404/409/503 confirmés | Cohérence API et tests | DEC-06/11, TD-03 | Avant les contrats REST | T-3.1, T-4.2 |

Ces incertitudes ne changent pas les règles métier déjà arbitrées. Lorsqu’un
choix conditionne une implémentation, la tâche correspondante doit rester
bloquée tant que sa décision technique n’est pas consignée; aucune valeur
implicite n’est considérée comme approuvée.

### Incohérences documentaires et traitement

- `requirements.md:114-167` décrit historiquement Kafka comme étude future non
  automatique; l’addendum courant de `PROJECT-CONTEXT.md:1372-1396` et les
  arbitrages propriétaires rendent Kafka obligatoire pédagogiquement. Le
  backlog suit le périmètre actuel sans modifier ces textes sources.
- H-6 demeure l’hypothèse historique sur l’utilité métier du consommateur.
  L’arbitrage courant retient maintenant audit-service et la trace minimale;
  DEC-07, ADR-03/04 et les textes de l’Étape 4 restent inchangés.
- UC-07 demeure historiquement **DÉDUIT / À CONFIRMER** et ADR-12 demeure
  conditionnel dans la change ADR. L’arbitrage courant approuve le parcours
  produit; le backlog conserve les statuts historiques tout en utilisant la
  décision actuelle pour les futures tâches.
- Les anciennes sources évoquent une consultation d’historique comme ouverte;
  l’arbitrage actuel inclut un écran protégé et exclut la suppression manuelle.

### Risks / Trade-offs

- [Donnée client exposée par liste, ID ou assistant] → filtrer et autoriser dans
  chaque service propriétaire; tester l’accès croisé client/client et
  l’élévation opérateur.
- [Dérive/duplication d’événements] → transaction outbox, livraison au moins une
  fois, traitement idempotent, version et tests de rejeu selon DEC-07.
- [Transcript sensible conservé] → LLM local, exclusion des secrets, rôles
  stricts, rétention configurable d’un mois et purge automatique testée.
- [Redis indisponible ou périmé] → le cache ne devient jamais source de vérité;
  fixer le repli vers PostgreSQL avant la tâche Redis.
- [Surdimensionnement local] → introduire les composants dans l’ordre des
  dépendances; Kubernetes/Helm restent hors de cette v1.

### Migration Plan

Sans changement runtime dans cette change. Une future exécution du backlog
implémentera les tâches dans l’ordre indiqué par `tasks.md`, avec migrations
versionnées et tests par capability; les stratégies de rollback de schéma et de
rejeu Kafka seront arrêtées dans les tâches techniques concernées.
