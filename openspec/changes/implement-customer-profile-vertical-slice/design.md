# Design — Premier incrément Customer

## Contexte et sources faisant autorité

Cette tranche concrétise les exigences métier US-01 (auto-inscription et
profil), US-02 (liste) et US-03 (détail), sans réécrire leurs scénarios :

- `define-implementation-backlog/specs/customer-account-management/spec.md`
- `resolve-backlog-technical-decisions/specs/customer-api-contracts/spec.md`
- `define-target-system-design/design.md` : DEC-01..06, DEC-08, DEC-10/11,
  DEC-14, DEC-19 et DEC-22.

L’identité est créée dans Keycloak avant que le client crée son profil métier.
Le client ne choisit ni son `sub` ni son e-mail; l’e-mail vérifié provient du
JWT. Les services propriétaires conservent les contrôles d’accès. Les décisions
actuelles du backlog prévalent; les statuts historiques de H-6, UC-07 et
DEC-07 restent inchangés.

## Objectif pédagogique et première tranche

Le chemin démontrable est : démarrer PostgreSQL et Keycloak localement,
s’inscrire/authentifier un client, créer sa fiche, puis la lire via une
Gateway et des API dont le service Customer valide lui-même le jeton.
L’incrément est autonome et vérifiable avant d’ajouter Account ou toute autre
fonctionnalité.

## Périmètre inclus

- Layout approuvé dans CPS-DES-01 : dépôt Maven mono-repo avec `pom.xml`
  agrégateur à la racine et modules `customer-service/` et `api-gateway/`,
  chacun avec son propre `pom.xml`. Java 21 et Spring Boot 3.5.x sont retenus;
  les versions compatibles sont centralisées dans le POM parent.
- `compose.yaml` est placé à la racine. Les fichiers d'import Keycloak de
  développement sont placés sous `infra/keycloak/`, notamment
  `infra/keycloak/realm-dev.json`; aucun secret réel n'est versionné.
- Ports de développement approuvés : Gateway `8080`, Customer `8082`,
  Keycloak `8081` et callback OIDC local `127.0.0.1:8765`. PostgreSQL écoute
  sur `5432` dans le réseau Compose (`postgres:5432`) et est publié sur l'hôte
  uniquement en boucle locale via `127.0.0.1:5432:5432` pour l'accès DBeaver.
- Compose local limité à PostgreSQL, Keycloak, Gateway et Customer, avec
  persistance PostgreSQL et configuration Keycloak reproductible sans secret
  réel versionné.
- `customer-service` seul propriétaire de la table Customer : identifiant,
  `sub` OIDC unique, nom, e-mail vérifié unique (comparaison insensible à la
  casse), et métadonnée technique `created_at` nécessaire au tri spécifié dans
  `resolve-backlog-technical-decisions/specs/customer-api-contracts/spec.md`.
  PostgreSQL est la source de vérité. Flyway gère les migrations versionnées;
  les contraintes d’unicité sont garanties par la base.
- API proposées et versionnées conformément à DEC-06 : `POST
  /api/v1/customers` crée le profil de l’utilisateur avec HTTP 201 et un
  en-tête `Location`; `GET /api/v1/customers` retourne la fiche du client ou
  la liste des fiches pour l’opérateur selon US-02; `GET
  /api/v1/customers/{id}` applique US-03. La Gateway expose des routes
  statiques vers ces seules API.
- Profil Customer limité à l’auto-inscription; un opérateur ne peut pas créer
  une fiche pour autrui. Autorisation par rôles Keycloak `CUSTOMER` et
  `OPERATOR` dans `realm_access.roles`, selon le contrat technique retenu.
- Validation `name` : suppression des espaces périphériques, non vide, au plus
  120 caractères. E-mail canonique rogné et en minuscules pour l’unicité; la
  valeur d’identité est tirée du claim vérifié.
- Pagination indexée à partir de zéro, taille 20 par défaut et maximum 100;
  tri `createdAt DESC, id ASC`; paramètres invalides et dépassement du maximum
  retournés comme erreurs structurées HTTP 400, sans plafonnement silencieux.
- Erreurs `application/problem+json` RFC 9457 avec code stable,
  `correlationId`, détails de champs sûrs et aucune donnée sensible.
- Idempotence de `POST` selon la spécification technique : clé requise,
  portée acteur/opération/base du service, empreinte SHA-256 canonique, succès
  persisté atomiquement avec la ressource, rejeu exact, concurrence sans
  doublon et expiration après sept jours. Ce mécanisme s’applique à la
  création de profil; les clés ne contiennent ni ne journalisent de secrets.
- Tests JUnit 5/Spring Boot Test et MockMvc, PostgreSQL et Keycloak avec
  Testcontainers; migrations, validations, autorisations, pagination,
  erreurs, unicité et idempotence.
- Documentation des commandes réellement vérifiées par l’apprenant et des
  résultats de tests.

Les endpoints de santé restent internes aux composants ou indiquent la
disponibilité du processus; la Gateway ne relaie pas d’endpoints Actuator
sensibles vers l’extérieur.

## Exclusions

- Compte, account-service, appel interservice Customer/Account et Gateway
  routant d’autres services.
- Angular ou tout autre frontend; l’inscription Keycloak peut être vérifiée
  par l’interface de développement Keycloak et des appels HTTP documentés.
- Kafka, événements, outbox et audit-service; Redis; Spring AI, MCP,
  assistant, Agent ou RAG.
- Tests UI, tests de contrats interservices, CI, stack d’observabilité,
  Kubernetes/Helm et exigences de performance/HA.
- Modification des exigences métier, des anciennes changes ou du backlog
  global.

## Décomposition d’implémentation proposée

Les 39 tâches CPS détaillées et leurs commandes de validation se trouvent dans
`tasks.md`. Les titres ont des identifiants stables; la numérotation OpenSpec
est locale à chaque groupe. Chaque tâche se termine par un arrêt obligatoire
jusqu’au partage des résultats et à la validation explicite de l’apprenant.

```text
CPS-ENV-01 ──► CPS-ENV-02/03/04 ──► CPS-ENV-05
      └──────► CPS-DES-01 ──► CPS-DOCKER-01 ──► CPS-PG-01 ──► CPS-PG-02
                                      └───────► CPS-KC-01 ──► KC-02..06
ENV-02/03 + DES-01 ──► CPS-MVN-01 ──► CPS-APP-01 (Customer)
                                      └──► CPS-APP-02 (Gateway)
APP-01 + PG-01 ──► CPS-DB-01 ──► CPS-DB-02 ──► CPS-DOM-01
APP-01 + PG-01 ───────────────────────────────────────────────┘
KC-04..06 + APP-01 ──► CPS-SEC-01 ──► CPS-GW-01
APP-01 + APP-02 + PG-01 + KC-02 ──► CPS-APP-03
DB-02 + DOM-01 + SEC-01 + API-01/02 ──► API-03/04
API-03 + DB-02 ──► IDEM-01 ──► IDEM-02 ──► IDEM-03
SEC-01 + API-03 ──► API-05 ──► API-06
API-05 + SEC-01 ──► API-07
API/domain tasks ──► TEST-01 ──► TEST-02 ──► E2E-01 ──► E2E-02 ──► DOC-01
```

Le numéro après le préfixe de groupe OpenSpec n’est pas l’identifiant CPS;
les dépendances faisant autorité sont indiquées dans chaque tâche. Les blocs
de préparation, services d’infrastructure, modules, persistence, sécurité,
API, idempotence et tests sont validés séparément avant le suivant.

Correspondance détaillée des tâches globales aux tâches CPS :

| Tâche du backlog global | Tâches CPS correspondantes |
|---|---|
| 1.1 — validation, erreurs et pagination | CPS-API-01, CPS-API-02, CPS-API-07 |
| 1.2 — outils de migration/tests Customer | CPS-DB-01, CPS-TEST-01, CPS-TEST-02 |
| 1.3 — rôles, claims et propagation OIDC | CPS-KC-03..06, CPS-SEC-01, CPS-GW-01 |
| 1.4 — détails d’idempotence Customer | CPS-IDEM-01..03 |
| 2.1 — Compose local et PostgreSQL | CPS-DOCKER-01, CPS-PG-01..02, CPS-KC-01..02, CPS-APP-03 |
| 2.2 — inscription et rôles Keycloak | CPS-KC-03..06, CPS-SEC-01 |
| 3.1 — profil Customer | CPS-DB-02, CPS-DOM-01, CPS-API-01..04 |
| 3.2 — idempotence Customer | CPS-IDEM-01..03 |
| 3.3 — lectures Customer | CPS-API-05..07 |
| 4.5 — routes Customer Gateway uniquement | CPS-APP-02..03, CPS-GW-01 |

Les installations/sondes CPS-ENV-01..05 et l’approbation CPS-DES-01 sont des
prérequis pédagogiques, pas des tâches du backlog global. Tâche 3.4 (Angular),
4.1–4.4 (Account) et les tâches globales 5–9 hors des validations de cette
tranche restent dans des changes ultérieures.

## Points d’attention

- La structure du dépôt n'était pas établie par les documents antérieurs.
  CPS-DES-01 approuve désormais le layout mono-dépôt et les ports décrits dans
  « Périmètre inclus »; aucune structure applicative n'a encore été générée.
- Le nom des endpoints n’était pas fixé dans les sources; le contrat est
  maintenant retenu pour cette tranche et détaillé ci-dessus.
- Contrat retenu pour cette tranche : `POST /api/v1/customers`, HTTP 201 et
  en-tête `Location`. Une tentative OPERATOR retourne HTTP 403
  `FORBIDDEN_PROFILE_CREATION`; un `sub` déjà lié retourne HTTP 409
  `CUSTOMER_PROFILE_EXISTS`; e-mail absent/non vérifié retourne HTTP 400
  `EMAIL_NOT_VERIFIED`, selon le contrat technique existant.
- La métadonnée `created_at` est ajoutée pour rendre possible le tri déjà
  retenu (`createdAt DESC, id ASC`); elle est générée côté service et n’est pas
  une nouvelle règle métier.
- La portée globale opérateur, l’auto-inscription dans Keycloak et le masquage
  404 sont repris des arbitrages actuels du backlog; aucune autorisation
  additionnelle n’est déduite.
- Les validations en runtime et l’interopérabilité des tokens/audiences devront
  être prouvées par les tests de cette change; elles ne sont pas garanties par
  la seule présence d’une configuration.
