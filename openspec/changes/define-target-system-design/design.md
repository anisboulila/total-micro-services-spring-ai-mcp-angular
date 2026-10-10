# Target System Design — Étape 2 — Architecture cible

> Document principal de conception. Il ne contient **aucun code**. Les décisions
> sont des **décisions de conception** ; leur implémentation sera réalisée par
> de futures changes, une task à la fois.

## 1. Cadre, sources et règles de lecture

**Sources** : `PROJECT-CONTEXT.md` ; baseline `business-requirements`
(Étape 1, exigences **candidates**) ; change `project-foundation-architecture-baseline`
(analyse de la référence) ; référence Youssfi (`main`, commit
`bf4c7f2750e4c731662e8138023fd8e6b4e4a475`) comme référence pédagogique
uniquement.

**Statuts** : **OBSERVÉ** (référence/documents), **DÉDUIT** (conséquence
logique), **DÉCIDÉ** (décision d’architecture pour notre projet),
**À CONFIRMER** (dépend d’une validation métier ou d’une information manquante).

**Règles** :

- Le workspace local ne contient pas de code applicatif ; rien ci-dessous ne
  décrit du code existant chez nous.
- Une exigence candidate n’est pas validée. Quand une décision en dépend, elle
  est signalée « conditionnelle ».
- Deux justifications sont distinguées : le **besoin** (exigence/NFR) et
  l’**objectif pédagogique** (contrainte **DÉCIDÉE** à l’Étape 1 : projet
  réaliste en contexte de production mais gérable à des fins pédagogiques).
  Lorsqu’un choix repose surtout sur l’objectif pédagogique, il est dit
  explicitement.

### 1.1 Exigences sources (rappel Étape 1)

| Réf. | Contenu (candidat, sauf mention) |
|---|---|
| UC-01..03 | Lister, consulter, créer un client (`name`, `email`) — OBSERVÉ dans la référence ; inclusion produit À CONFIRMER |
| UC-04..06 | Lister, consulter, créer un compte (`type`, `balance`, `customerId`) — idem |
| UC-07 | Questions en langage naturel sur clients/comptes — candidat DÉDUIT / À CONFIRMER |
| NFR-1 | Protection des informations client et compte — DÉDUIT / À CONFIRMER |
| NFR-2 | Exactitude de l’association client–compte et de la création de compte — DÉDUIT / À CONFIRMER |
| NFR-3 | Interaction compréhensible et utilisable — DÉDUIT / À CONFIRMER |
| NFR-4 | Disponibilité et temps de réponse adaptés (sans objectif numérique) — DÉDUIT / À CONFIRMER |
| CON-1 | Reconstruction de zéro, référence non copiée — DÉCIDÉ |
| CON-2 | Réalisme « production » mais gérable à des fins pédagogiques — DÉCIDÉ |
| CON-3 | Outils gratuits/locaux privilégiés (`PROJECT-CONTEXT.md` §20) — DÉCIDÉ |

### 1.2 Hypothèses du design (si fausses, voir impact)

| Id | Hypothèse | Impact si fausse |
|---|---|---|
| H-1 | Les capacités UC-01..06 sont dans le périmètre produit. | Réduire les services/APIs ; DEC-01/02 à réévaluer. |
| H-2 | Aucune règle de suppression/mise à jour n’existe encore. | Ajouter règles d’intégrité référentielle (DEC-05). |
| H-3 | Aucun objectif numérique de disponibilité/latence n’est imposé. | Dimensionnement, HA et déploiement (DEC-12, DEC-20) à revoir. |
| H-4 | Le public est composé d’utilisateurs authentifiés (rôles précis inconnus). | Modèle d’autorisation (DEC-10) à revoir. |
| H-5 | Le projet tourne en local/CI avec outils gratuits. | Choix de LLM (DEC-15), d’observabilité (DEC-13) et de déploiement (DEC-19). |
| H-6 | Un consommateur d’événements métier est utile (audit, notification, lecture). | DEC-07 devient inutile sans consommateur (voir déclencheurs). |

## 2. Architecture globale et contexte

### 2.1 Acteurs et systèmes externes

- Utilisateur humain via navigateur (rôle exact **À CONFIRMER**, H-4).
- Fournisseur d’identité (OIDC) pour l’authentification.
- Fournisseur de modèle de langage (LLM) pour UC-07.
- Canaux Telegram/Discord : **OBSERVÉS** dans la référence ; non retenus dans
  la portée initiale (DEC-18).

### 2.2 Vue logique cible (DÉCIDÉ, sous réserve H-1)

```
 Navigateur ──► Frontend Angular (statique)
      │ HTTPS + jeton OIDC
      ▼
  API Gateway ───────────────┬─────────────────────┬──────────────────────┐
      │ REST                  │ REST                │ REST/SSE             │
      ▼                       ▼                     ▼                      │
 customer-service ◄──REST── account-service    assistant-service         │
   (DB customer)  validation  (DB account)       (Spring AI ChatClient)    │
      │  ▲                      │                    │ MCP (lecture seule, si UC-07) │
      │  └──────── MCP ◄────────┼────────────────────┤                     │
      │  événements (outbox, si H-6) │  événements (outbox, si H-6)└──► LLM  │
      └──────► Kafka (si H-6) ◄────┘                                      │
                        │                                                  │
                  audit-service (consommateur, À CONFIRMER, H-6)           │
 Keycloak (OIDC) ◄──── validation de jetons par Gateway et chaque service ─┘
 Observabilité transverse : logs structurés, métriques, traces
```

### 2.3 Style architectural

**DEC-01 — Style : petit ensemble de services orientés domaine, avec un seul
point d’entrée, derrière des frontières claires.** — **DÉCIDÉ** (justification
mixte besoin + pédagogique).

- **Besoin** : séparer la responsabilité Customer et Account (propriété des
  données distincte, DÉDUIT de la référence et des UC) ; limiter la surface
  d’exposition (NFR-1).
- **Alternatives** : (a) monolithe modulaire ; (b) deux services métier + un
  assistant + un `audit-service` consommateur d’événements (retenu, les deux
  derniers étant conditionnels) ; (c) multiplication de microservices
  (notification, auth…).
- **Pourquoi (b)** : (a) serait techniquement le plus simple pour un périmètre
  aussi réduit (aucun besoin de scalabilité indépendante démontré). L’objectif
  pédagogique (CON-2 : concepts distribués, propriété des données, résilience,
  événements) justifie néanmoins de séparer Customer et Account, qui forment
  deux contextes cohérents (le compte référence un client). (c) est rejeté :
  « microservices artificiels ».
- **Compromis** : complexité d’exploitation, latence réseau, cohérence
  éventuelle ; coût assumé et explicitement pédagogique. Réévaluation si H-1
  est fausse : retomber sur (a).
- **Statut** : ferme pour deux services métier ; l’assistant dépend de UC-07
  (**À CONFIRMER**, voir DEC-15) et `audit-service` du besoin d’audit
  (**À CONFIRMER**, H-6, voir DEC-07).

## 3. Frontières, responsabilités et propriété des données

**DEC-02 — Composants et responsabilités** — **DÉCIDÉ** (conditionnel à H-1/UC-07).

| Composant | Responsabilité | Ne fait pas |
|---|---|---|
| `customer-service` | Cycle de vie des clients (UC-01..03) ; publie les faits client | Ne connaît pas les comptes |
| `account-service` | Cycle de vie des comptes (UC-04..06) ; valide l’existence du client à la création ; publie les faits compte | Ne stocke pas les données client |
| `assistant-service` | Orchestre UC-07 : reçoit la question, appelle le LLM, invoque des outils en lecture seule | N’accède à aucune base ; n’écrit pas de données métier |
| `audit-service` | Premier consommateur Kafka (DEC-07, conditionnel à H-6) : consomme `CustomerCreated`/`AccountCreated` de façon idempotente et conserve une trace d’audit minimale | N’expose aucune écriture métier ; n’est pas appelé de façon synchrone par les services métier |
| `api-gateway` | Point d’entrée unique : routage, CORS, validation de jeton, limites de taille | Pas de logique métier |
| Frontend Angular | Interface unique (DEC-18) | Pas de règle métier |
| Keycloak | Authentification/identité (DEC-10) | — |
| PostgreSQL | Une base logique par service (DEC-04) | — |
| Kafka | Transport d’événements métier (DEC-07) | Pas de requête synchrone |

Noms définitifs des services : **À CONFIRMER** (le nom `ebank-service` de la
référence n’est pas obligatoire).

**DEC-03 — Propriété des données** — **DÉCIDÉ**.

- `customer-service` possède `Customer` (`id`, `name`, `email`).
- `account-service` possède `BankAccount` (`id`, `createdAt`, `balance`,
  `type`, `customerId`). `customerId` est une **référence opaque** : pas de clé
  étrangère inter-bases, pas de lecture directe de la base de l’autre service.
- Toute donnée d’un autre service s’obtient par son API ou par événement.
- `audit-service` (conditionnel, H-6) possède ses propres enregistrements
  d’audit, dérivés des événements ; il n’est pas source de vérité des clients
  ou des comptes. Contenu, conservation et accès **À CONFIRMER** (NFR-1).
- Contraste **OBSERVÉ** : la référence utilise H2 en mémoire par service, et
  `EbankService.save` ne rejette pas un client de repli « Not available »
  (voir DEC-11).
- **Compromis** : pas de jointure inter-services ; affichages composés = appels
  multiples ou modèle de lecture (À CONFIRMER, hors périmètre).

**DEC-04 — Persistance : PostgreSQL, une base/schéma par service** — **DÉCIDÉ**.

- **Besoin** : données relationnelles, intégrité, solde monétaire exact
  (NFR-2), persistance durable (H2 en mémoire de la référence inadaptée).
- **Alternatives** : H2 (références/tests seulement) ; MySQL/MariaDB
  (équivalent, moins de fonctions pour la suite) ; MongoDB (modèle de document
  inutile ici) ; base partagée (rejetée : viole DEC-03).
- **Pourquoi** : PostgreSQL est relationnel, transactionnel, gratuit,
  exécutable en local/conteneur (CON-3).
- **Compromis** : un composant d’exploitation en plus ; migrations de schéma
  versionnées requises (outil **À CONFIRMER** à l’implémentation).
- Les montants utilisent une représentation décimale exacte ; devise et
  signification du solde **À CONFIRMER** (Étape 1).

**DEC-05 — Cohérence des données** — **DÉCIDÉ**.

- Transactions **locales** par service ; **pas** de transaction distribuée
  (2PC/XA) : aucun cas d’usage n’écrit simultanément dans deux services.
- La création d’un compte lit l’état du client (validation) puis écrit
  localement : pas de saga nécessaire.
- Cohérence **éventuelle** entre services via événements (DEC-07).
- Référence client supprimée/modifiée : **hors périmètre tant que H-2 tient** ;
  si la suppression devient une exigence, il faudra décider (interdiction,
  désactivation logique ou compensation) — **À CONFIRMER**.
- **Compromis** : un compte peut, brièvement, référencer un client dont l’état
  change ensuite ; accepté en l’absence de règle métier contraire.

## 4. Communication

**DEC-06 — REST synchrone par défaut** — **DÉCIDÉ**.

- **Besoin** : opérations de requête/réponse exigeant un résultat immédiat
  (UC-01..06, validation de client lors de la création de compte).
- **Alternatives** : tout asynchrone (rejeté : requêtes de lecture et
  validation nécessitent une réponse) ; gRPC (gain limité, outillage de
  navigateur moindre).
- **Règles** : contrats REST versionnés, DTO distincts des entités,
  validation d’entrée, format d’erreur unique (§8), pagination des listes
  (taille maximale **À CONFIRMER**).
- **Client inter-services** : le client déclaratif (Feign ou client HTTP
  Spring équivalent) est un choix d’implémentation, non structurant ; **À
  CONFIRMER** à l’implémentation. Feign n’est pas un protocole : c’est du REST.
- **Compromis** : couplage de disponibilité Account → Customer à la création,
  atténué par DEC-11.

**DEC-07 — Événements Kafka pour les faits métier, avec périmètre borné** —
**DÉCIDÉ sous condition H-6 (périmètre) / À CONFIRMER (consommateur concret)**.

- **Problème** : permettre à plusieurs composants de réagir à un fait métier
  important sans couplage synchrone ; exigence d’Étape 1 exprimée seulement
  comme besoin possible (H-6).
- **Honnêteté** : aucun consommateur métier n’est validé. Le choix est motivé
  par (i) l’objectif pédagogique **DÉCIDÉ** (Kafka, doublons, idempotence) et
  (ii) un besoin **DÉDUIT** plausible (audit/traçabilité des créations,
  notification, vue de lecture). Il n’est **pas** un besoin métier démontré.
- **Alternatives** : pas d’événements (le plus simple, valide si H-6 est faux) ;
  RabbitMQ (broker de messages, moins adapté à relecture/rejeu) ; événements
  Spring in-process (monolithe uniquement) ; appels REST de notification
  (couplage fort).
- **Décision conditionnelle à H-6** : événements immuables `CustomerCreated` et `AccountCreated`,
  publiés via **transactional outbox** ; REST reste l’interface par défaut.
  Pas de commande ni de requête par Kafka. Premier consommateur :
  `audit-service` (audit minimal des créations) ; le besoin d’audit reste
  **À CONFIRMER** ; sans consommateur validé, le broker n’est pas
  introduit en production-like (déclencheur d’abandon).
- **Garanties et doublons (réponse « comment éviter les doublons ? »)** :
  1. livraison **au moins une fois** (acceptée) ; 2. identifiant d’événement
  unique ; 3. outbox : l’écriture métier et l’événement sont dans la même
  transaction locale, puis un relais publie ; 4. producteur idempotent ;
  5. **consommateur idempotent** (table des identifiants déjà traités, ou
  opération naturellement idempotente) ; 6. clé de partition = identifiant
  d’agrégat (ordre par agrégat) ; 7. retries bornés puis file d’échecs
  (dead-letter) ; 8. schéma d’événement versionné.
- **Compromis** : un composant de plus (broker), cohérence éventuelle, courbe
  d’exploitation, ordre garanti seulement par partition.

**Évolution prévue — Kafka Streams** — **À CONFIRMER** (hors périmètre
initial de DEC-07).

L’exploitation des événements Kafka pour produire des statistiques temps réel
ou agrégées est identifiée comme une évolution potentielle du système.

Exemples de statistiques envisagées :

- nombre de clients créés par période ;
- nombre de comptes créés par période ;
- répartition des comptes par type ;
- agrégations temporelles (fenêtres) ;
- autres indicateurs dérivés des événements métier.

Cette évolution n’est **pas incluse dans le périmètre initial** de DEC-07 et ne
doit entraîner aucune implémentation Kafka Streams à ce stade.

Elle fera l’objet d’une **évolution SDD/OpenSpec dédiée**, avec :

1. clarification du besoin fonctionnel ;
2. étude Kafka Streams vs alternatives ;
3. définition des événements et agrégats nécessaires ;
4. décision sur le stockage et l’exposition des statistiques ;
5. choix éventuel d’un `statistics-service` distinct ;
6. spécification et implémentation.

**Déclencheur de réévaluation :** besoin confirmé de statistiques dérivées des
événements avec traitement temps réel ou quasi temps réel.

Le choix de Kafka Streams devra être justifié par le besoin de traitement de
flux (agrégation, groupement, fenêtres temporelles, etc.) et non uniquement par
un objectif pédagogique.

**DEC-08 — API Gateway** — **DÉCIDÉ**.

- **Besoin** : point d’entrée unique, validation de jeton en périphérie, CORS
  centralisé, limitation de l’exposition des services (NFR-1).
- **Alternatives** : accès direct aux services (plus simple, mais exposition
  multiple et CORS/jetons répétés) ; ingress/reverse proxy seul (pas de
  logique applicative, suffisant si le déploiement n’a pas de logique de
  routage) ; backend-for-frontend (pas justifié avec un seul frontend).
- **Choix** : Spring Cloud Gateway avec **routes statiques explicites** (pas de
  locator dynamique) : le jeu de services est fixe et petit.
- **Contraste OBSERVÉ** : la référence expose `localhost:9999` avec routes par
  découverte et CORS `*`, non testés en runtime ; nous ne reprenons pas ce
  réglage.
- **Compromis** : saut réseau supplémentaire ; point critique à rendre
  redondant en cas de scalabilité ; ne remplace pas l’autorisation dans les
  services.

**DEC-09 — Découverte de services : non retenue** — **DÉCIDÉ**.

- **Besoin** : trouver l’adresse des services. Dans notre topologie fixe,
  le DNS de l’environnement d’exécution (Compose/Kubernetes) suffit.
- **Alternatives** : Eureka (OBSERVÉ dans la référence) ; Consul ; DNS
  (retenu).
- **Compromis** : pas d’enregistrement dynamique applicatif ; le routage
  dépend de noms DNS stables. **Déclencheur de réévaluation** : instances
  dynamiques hors orchestrateur.
- **Config Server** : non retenu (voir DEC-14) ; la référence ne contient qu’un
  config client sans serveur identifié (OBSERVÉ).

## 5. Sécurité et identité

**DEC-10 — OIDC/OAuth2 avec Keycloak, validation de jetons à chaque saut** —
**DÉCIDÉ** (rôles **À CONFIRMER**).

- **Besoin** : NFR-1 — protéger les informations client/compte. **OBSERVÉ** :
  la référence ne montre pas de Spring Security dans les POMs consultés, un CORS
  `*` et des outils de création accessibles via le bot ; protections externes
  non vérifiées.
- **Alternatives** : pas d’authentification (non défendable pour des données
  clients) ; sessions applicatives ou JWT maison (risque de sécurité, effort) ;
  Spring Authorization Server (possible, plus de code à maintenir) ;
  fournisseur cloud (payant, CON-3). **Choix** : Keycloak, gratuit, standard
  OIDC, exécutable en local.
- **Responsabilités (trois notions à ne pas confondre : authentification,
  validation de jeton, autorisation)** :
  - **Keycloak** — fournisseur d’identité OIDC : **authentifie** les
    utilisateurs, **émet** les jetons et porte la configuration des clients et
    des rôles. C’est la seule composante qui joue le rôle de fournisseur
    d’identité.
  - **Spring Security** — sécurise les applications Spring : intègre la
    validation des jetons dans la chaîne de filtres et **applique les règles
    d’autorisation**. Ce n’est pas un fournisseur d’identité et n’authentifie
    pas les utilisateurs dans cette conception.
  - **Spring Security OAuth2 Resource Server** — module de Spring Security qui
    permet à une application de **valider les JWT** reçus (signature, émetteur,
    expiration) à partir des clés publiques publiées par Keycloak. Valider un
    jeton prouve son authenticité ; cela ne décide pas de ce que l’appelant a
    le droit de faire.
- **Utilisation prévue** (décision de conception ; rien n’est implémenté ni
  configuré à ce stade) :
  - **API Gateway** : Spring Security avec OAuth2 Resource Server pour valider
    les JWT entrants et appliquer les premières règles d’accès (cohérent avec
    DEC-08 : validation en périphérie, sans logique métier).
  - **`customer-service` et `account-service`** : chacun utilise Spring
    Security OAuth2 Resource Server pour **valider indépendamment** les JWT
    (défense en profondeur ; un service n’est jamais supposé protégé par le
    seul Gateway) ; aucun service n’est exposé directement.
  - **`assistant-service`** : même mécanisme, **uniquement si** le service est
    confirmé dans le périmètre (UC-07 **À CONFIRMER**, DEC-15).
  - Le frontend s’authentifie auprès de Keycloak (Authorization Code + PKCE) ;
    Spring Security n’intervient pas dans cette étape.
- **Autorisation** : contrôle par rôle (modèle minimal, rôles et permissions
  exacts **À CONFIRMER**, H-4) ; les contrôles sont effectués **dans les
  microservices**, et pas uniquement au Gateway, qui n’applique que des règles
  d’accès de premier niveau. Aucun rôle n’est fixé avant validation.
- **Propagation d’identité** : appels synchrones inter-services avec le jeton
  de l’utilisateur (ou échange de jeton si une règle l’exige, **À CONFIRMER**) ;
  événements : l’identité de l’acteur est un champ de métadonnées, jamais le
  jeton.
- **Autres mesures** : TLS aux frontières externes ; CORS limité à l’origine du
  frontend ; validation des entrées ; journaux sans données sensibles ni
  secrets ; secrets via DEC-14 (distincts de la configuration : par exemple
  l’adresse de l’émetteur OIDC relève de la configuration, tandis que les
  secrets de clients Keycloak sont injectés à l’exécution ; la validation de
  JWT par clés publiques n’exige aucun secret partagé côté ressource).
- **Compromis** : composant d’exploitation supplémentaire ; configuration de
  clients/rôles ; dépendance de disponibilité à Keycloak pour l’obtention de
  jetons (la validation est locale grâce aux clés publiques mises en cache) ;
  chaque service validant les jetons, la configuration de sécurité est
  répétée (atténuation possible par un socle commun, **À CONFIRMER** à
  l’implémentation).

## 6. Résilience, disponibilité et idempotence

**DEC-11 — Politique de résilience** — **DÉCIDÉ**.

| Mécanisme | Règle | Pourquoi |
|---|---|---|
| Timeouts | Obligatoires sur tout appel sortant (connexion et lecture) ; valeurs numériques **À CONFIRMER** (fixées à l’implémentation) | Éviter l’épuisement des threads (réseau lent) |
| Retries | Bornés (jamais illimités), avec backoff, et **uniquement** sur les opérations dont la répétition est sûre (lectures) ; nombre de tentatives et délais **À CONFIRMER** ; jamais sur une création sans mécanisme d’idempotence défini | Éviter les doublons |
| Circuit breaker | Sur l’appel `account-service` → `customer-service` ; sur l’appel au LLM **uniquement si** `assistant-service` est confirmé (UC-07 **À CONFIRMER**) ; seuils **À CONFIRMER** | Empêcher la cascade de pannes |
| Fallback | **Jamais de valeur synthétique valide** : en échec de validation du client, la création est **refusée** avec erreur explicite | NFR-2 |
| Idempotence | Clé d’idempotence fournie par le client pour `POST` de création ; règles détaillées ci-dessous | Requête exécutée deux fois |
| Limitation de charge | Limites de taille/requêtes au Gateway (seuils **À CONFIRMER**) | Protéger les services |

- **OBSERVÉ** : le fallback de la référence renvoie un client « Not available »
  et le chemin de sauvegarde ne contrôle pas ces valeurs ; **DÉDUIT** : un
  compte peut être créé pour un client non validé. Notre politique rejette ce
  comportement.
- **Compromis** : la création de compte est indisponible quand Customer est
  indisponible — compromis assumé en faveur de la justesse (NFR-2) plutôt que
  de la disponibilité d’écriture. Alternative écartée : copie locale des
  clients (cohérence éventuelle, risque d’obsolescence) ; réévaluer si la
  disponibilité d’écriture devient prioritaire.
- Librairie de résilience : Resilience4j (**OBSERVÉ** dans la référence ;
  retenu pour sa maturité dans l’écosystème Spring ; interchangeable).

**Idempotence des `POST` de création** (règles ; mécanisme technique détaillé
non décidé ici) :

- Le client fournit une clé d’idempotence pour chaque requête de création.
- Même clé et même requête : le résultat initial est rejoué, sans doublon.
- Même clé avec un contenu différent : requête rejetée avec une erreur
  explicite (typiquement HTTP `409` ou `422`, choix **À CONFIRMER**).
- Requêtes concurrentes avec la même clé : une seule création doit avoir
  lieu ; les autres attendent ou reçoivent le résultat initial / une erreur de
  conflit explicite.
- Le stockage de la clé doit **garantir l’unicité**, y compris en concurrence
  (garantie portée par la base du service propriétaire, et non par une simple
  vérification préalable) et être écrit dans la même transaction locale que la
  création.
- Durée de conservation des clés : **À CONFIRMER**.

**Validation du client par `account-service`** (appel à `customer-service`) :

| Situation | Comportement | Erreur (typique) |
|---|---|---|
| Client inexistant | Création refusée, réponse métier explicite | HTTP `404` |
| `customer-service` indisponible, timeout ou circuit ouvert | Création refusée, dépendance indisponible | HTTP `503` |
| Client valide | Création du compte | — |

- Aucun client fictif ni valeur synthétique ne peut autoriser la création.
- Ces erreurs utilisent le **format d’erreur unique** défini au §8 (code
  stable, message sûr, identifiant de corrélation) ; aucun format concurrent
  n’est introduit. Les codes HTTP exacts restent des conventions typiques, à
  confirmer à l’implémentation.
- Le circuit breaker ne remplace pas les timeouts : il complète, il ne
  supprime pas l’obligation d’en définir. Il ne fournit jamais de résultat
  métier fictif.

**Disponibilité (NFR-4, H-3)** : services sans état (sessions inexistantes),
sondes de liveness/readiness, arrêt gracieux. Objectifs numériques **À
CONFIRMER** ; aucune architecture haute disponibilité multi-zones n’est
décidée sans objectif.

- **Liveness** : indique si le processus est toujours fonctionnel ; un échec
  peut justifier un redémarrage.
- **Readiness** : indique si le service est prêt à recevoir du trafic ; un
  échec retire l’instance du routage sans la redémarrer.
- **Arrêt gracieux** : cesse d’accepter de nouvelles requêtes et laisse
  terminer proprement celles en cours.
- Une dépendance externe indisponible (autre service, broker, fournisseur
  d’identité) ne doit **pas** faire échouer la liveness, afin d’éviter des
  redémarrages en boucle ; son effet éventuel est limité à la readiness ou à
  des erreurs contrôlées. La politique exacte des sondes et des dépendances
  prises en compte reste **À CONFIRMER** selon le déploiement (DEC-19, DEC-20).

## 7. Performance et scalabilité

**DEC-12 — Scalabilité horizontale des services sans état ; cache Redis à
des fins pédagogiques, périmètre limité** — **DÉCIDÉ** (paramètres techniques
**À CONFIRMER**).

- **Besoin** : NFR-4 sans objectifs chiffrés. Conception sans état permettant
  la duplication d’instances derrière le Gateway.
- **Redis : retenu à des fins pédagogiques**, pour apprendre et démontrer le
  fonctionnement d’un **cache distribué partagé entre plusieurs instances** de
  microservices, en complément de l’expérience existante avec Ehcache et
  `@Cacheable`. Ce besoin est **pédagogique** (CON-2) : **aucun profil de
  charge ne démontre** aujourd’hui que Redis est nécessaire aux performances
  métier, et il ne doit pas être présenté comme tel.
- **Principes** : PostgreSQL reste la **source de vérité** des données
  métier ; Redis n’est jamais une dépendance obligatoire pour garantir la
  justesse des opérations métier (le cache est une optimisation, pas une
  source de données). Les clés d’idempotence (DEC-11) restent dans
  PostgreSQL.
- **Périmètre initial (volontairement limité)** :
  - Spring Cache avec Redis comme fournisseur de cache ;
  - premier cas d’usage : lectures de clients dans `customer-service`, par
    exemple la récupération d’un client par identifiant ;
  - un **TTL** limite la durée de vie des entrées (valeur **À CONFIRMER**) ;
  - invalidation ou mise à jour du cache après modification des données
    concernées, **si** les opérations de modification sont confirmées dans le
    périmètre (H-2) ;
  - démonstration qu’un cache partagé sert plusieurs instances de
    `customer-service` ;
  - **hors périmètre initial** : soldes des comptes, sessions, limitation de
    débit distribuée.
- **Alternatives** : pas de cache (le plus simple, suffisant pour les besoins
  métier actuels) ; cache local par instance (Ehcache/Caffeine : simple, mais
  non partagé et risque d’incohérence entre instances) ; réplicas de lecture
  PostgreSQL ; Redis (retenu, pour l’objectif pédagogique).
- **Règles de performance** : pagination obligatoire, index justifiés par les
  requêtes (par exemple sur `customerId`), pool de connexions borné, pas de
  requête N+1, appels inter-services limités (pas de chaînage profond).
- **Kafka** : scalabilité via partitions et groupes de consommateurs (usage
  limité, DEC-07).
- **Partie IA** : latence dominée par le LLM ; les appels sont limités, avec
  timeout et streaming éventuel (DEC-15).
- **Compromis** :
  - les lectures répétées peuvent éviter certaines requêtes PostgreSQL ;
  - un cache peut contenir temporairement une **donnée périmée** ; le TTL et
    l’invalidation limitent ce risque sans l’éliminer ;
  - Redis ajoute une **dépendance opérationnelle** et de la complexité
    (composant supplémentaire, sérialisation, supervision) ;
  - la politique en cas d’indisponibilité de Redis est à définir à
    l’implémentation, en privilégiant un comportement sûr et une **lecture
    PostgreSQL** lorsque cela est possible (**À CONFIRMER**) ;
  - sans mesures, les optimisations de performance restent prématurées ; les
    tests de charge légers (§12) fourniront la base d’évaluation, y compris
    pour juger l’effet réel du cache.
- **Aucun autre mécanisme de cache** n’est ajouté sans besoin justifié.

## 8. Observabilité et gestion des erreurs

**DEC-13 — Observabilité à trois piliers, standards ouverts** — **DÉCIDÉ**
(backends **À CONFIRMER**).

- **Besoin** : diagnostiquer un incident (« comment diagnostiquer ? »). Dans la
  référence, Actuator est déclaré mais l’exposition et la collecte réelles ne
  sont pas vérifiées (OBSERVÉ / à confirmer).
- **Logs** : structurés (JSON) sur stdout, identifiant de corrélation propagé,
  sans données sensibles.
- **Métriques** : Micrometer + endpoint de scraping ; tableaux de bord
  (Prometheus/Grafana comme option par défaut gratuite). Métriques minimales :
  latence/erreurs HTTP, circuit breaker, lag et échecs Kafka, latence LLM.
- **Traces** : OpenTelemetry avec propagation (HTTP et en-têtes Kafka) ;
  backend (Jaeger/Tempo) **À CONFIRMER**.
- **Alternatives** : stack hébergée payante (CON-3) ; logs seuls (insuffisant
  pour des flux distribués).
- **Compromis** : composants de monitoring supplémentaires ; à introduire par
  paliers.

**Gestion des erreurs** : format d’erreur unique (code stable, message sûr,
identifiant de corrélation), codes HTTP cohérents (validation, non trouvé,
conflit/idempotence, indisponibilité en amont), aucune trace de pile exposée.

## 9. Configuration et secrets

**DEC-14 — Configuration externalisée, secrets séparés, pas de Config Server**
— **DÉCIDÉ**.

- **Besoin** : même artefact dans plusieurs environnements ; pas de secret dans
  le code (sécurité).
- Configuration par variables d’environnement/fichiers de profil ;
  ConfigMap/fichiers en conteneur. **Secrets** (mots de passe de base, secrets
  de clients OIDC, clés d’API LLM) injectés à l’exécution, jamais versionnés,
  jamais journalisés (distincts de la configuration).
- **Alternatives** : Spring Cloud Config Server (OBSERVÉ comme client seul dans
  la référence, serveur non identifié) — pas de besoin de config dynamique
  centralisée ; Vault (justifié si rotation/audit des secrets requis).
  **Déclencheur** : nombre de services/environnements ou rotation dynamique.
- **Compromis** : pas de rafraîchissement dynamique de la configuration ;
  redémarrage nécessaire.

## 10. AI, MCP, RAG, Agent et canaux

**DEC-15 — Assistant conversationnel conditionnel à UC-07** — **DÉCIDÉ
(forme) / À CONFIRMER (existence)**.

- **Besoin** : UC-07 est candidat **DÉDUIT / À CONFIRMER** ; la référence le
  démontre mais pas l’exactitude des réponses ni l’usage systématique des
  outils (OBSERVÉ). L’objectif pédagogique inclut les phases Spring AI/MCP.
- **Décision de forme** : un `assistant-service` utilisant Spring AI
  `ChatClient`, **appel unique avec outils** (tool calling), mémoire de
  conversation bornée, aucune écriture métier par le LLM
  (**lecture seule**) : un LLM est non déterministe et peut halluciner ;
  les mutations restent des opérations explicites authentifiées via l’API.
- **LLM** : abstraction Spring AI ; par défaut un modèle local (Ollama) pour
  CON-3, fournisseur distant optionnel (clé gérée comme secret). Ne pas envoyer
  de données personnelles à un fournisseur externe sans décision — **À
  CONFIRMER** (NFR-1).
- **Streaming** : réponse HTTP progressive (SSE) optionnelle pour la
  perception de latence ; **ne** constitue **ni** Kafka **ni** événementiel
  métier. Le besoin d’affichage progressif est **À CONFIRMER** (la référence
  expose `/chatStream` dans un seul des deux frontends : OBSERVÉ).

**DEC-16 — MCP : outils de lecture exposés par les services propriétaires** —
**DÉCIDÉ sous condition UC-07** (justification surtout pédagogique et de
standardisation).

- **Besoin conditionnel à UC-07** : donner au LLM un accès contrôlé aux
  capacités métier.
- **Alternatives** : outils locaux Spring AI (`@Tool`) appelant les REST
  (plus simple tant qu’il n’y a qu’un consommateur) ; MCP (retenu).
- **Pourquoi MCP** : interface standard réutilisable par plusieurs clients
  d’IA, outils découvrables, séparation claire entre l’orchestrateur et les
  capacités ; l’objectif pédagogique est explicite. **Compromis** : surface
  d’attaque et saut réseau supplémentaires ; sans second consommateur, les
  outils locaux seraient plus simples.
- **Règles** : outils **en lecture seule** ; implémentés comme adaptateur
  au-dessus des services applicatifs propriétaires (pas de service dédié
  supplémentaire) ; **exposés uniquement en interne**, jamais via le
  Gateway public ; mêmes règles d’autorisation que REST (identité propagée,
  mécanisme **À CONFIRMER**) ; destinations configurées explicitement
  (contraste **OBSERVÉ** : URL `localhost` statiques dans la référence, et
  Eureka Client du bot non utilisé pour MCP) ; mutations/création **non**
  exposées.

**DEC-17 — Agent autonome et RAG : non retenus** — **DÉCIDÉ**.

- **Agent** : aucun besoin de planification multi-étapes (UC-07 =
  interrogation) ; coût, risque et non-déterminisme. **Déclencheur** : un cas
  d’usage confirmé nécessitant un enchaînement d’outils.
- **RAG** : aucun corpus documentaire non structuré n’est dans les exigences ;
  les données sont structurées et accessibles par outils. **Déclencheur** :
  base de connaissances (politiques, FAQ) confirmée. Alternative : recherche
  par outils sur données structurées (retenue).
- **Garde-fous** : limitation des tailles d’entrée/sortie, timeout, journalisation
  sans données sensibles, évaluation des réponses sur échantillons (§12).

**DEC-18 — Canaux conversationnels et frontend** — **DÉCIDÉ (portée initiale)
/ À CONFIRMER (suite)**.

- **Frontend unique** Angular : la référence en contient deux, quasi
  identiques, et la raison de la duplication n’est pas démontrée (OBSERVÉ) ;
  deux frontends n’apportent aucun bénéfice établi (maintenance doublée).
  Alternative : plusieurs frontends si des publics distincts sont confirmés
  (**À CONFIRMER**).
- **Chat Web** inclus si UC-07 est confirmé. **Telegram/Discord** :
  OBSERVÉS dans la référence mais **hors portée initiale** (nécessitent liaison
  d’identité des utilisateurs de canaux, secrets, webhooks) ; réévaluer sur
  demande confirmée.

## 11. Déploiement, conteneurisation et CI/CD

**DEC-19 — Conteneurs et Docker Compose pour l’exécution locale** —
**DÉCIDÉ**. Une image par composant applicatif ; infrastructure (PostgreSQL,
Kafka, Keycloak, observabilité) exécutée par Compose en local. **Besoin** :
reproductibilité. **Alternatives** : exécution native (peu reproductible) ;
Kubernetes seul (surdimensionné pour le développement). **Compromis** :
consommation de ressources locales ; Compose n’est pas un environnement de
production.

**DEC-20 — Kubernetes : cible optionnelle, non obligatoire** — **À CONFIRMER**.
Aucune exigence validée (H-3) ne demande haute disponibilité ou autoscaling.
L’objectif pédagogique de la roadmap (phases Kubernetes/Helm) pourrait le
justifier sur un cluster local (kind/Minikube). **Décision de conception
ferme** : applications sans état et configurables par l’extérieur, donc
déployables sur Kubernetes sans refonte ; sondes de santé ; configuration et
secrets externes. **Helm** : non décidé ; différé jusqu’à ce qu’un besoin de
paramétrage multi-environnements soit établi.

**DEC-21 — CI/CD minimale avec GitHub Actions** — **DÉCIDÉ**. Pipeline : build,
tests, vérification qualité de base, construction d’images. Déploiement
continu **non** décidé. **Compromis** : durée de pipeline (tests d’intégration
avec conteneurs) vs fiabilité ; limites gratuites de GitHub (CON-3).

## 12. Stratégie de test et de validation

**DEC-22 — Pyramide de tests** — **DÉCIDÉ**.

- Tests unitaires du domaine (règles : création de compte, idempotence).
- Tests de tranche (web, persistance) ; tests d’intégration avec PostgreSQL,
  Kafka et Keycloak réels en conteneurs éphémères (Testcontainers ou
  équivalent, outil **À CONFIRMER**).
- Tests de contrat REST et de schéma d’événement entre services (outil **À
  CONFIRMER**).
- Tests de résilience : timeout, circuit ouvert, **refus de création** sans
  client valide ; doublon d’événement ; rejeu de `POST` idempotent.
- Tests de sécurité : accès sans jeton refusé, rôle insuffisant refusé,
  absence de fuite de secrets.
- IA : modèle simulé pour les tests déterministes ; évaluation manuelle sur
  jeu de questions ; vérifier la lecture seule.
- Quelques tests de bout en bout ; test de charge léger pour valider H-3.
- Les tests de la référence ne sont pas une preuve pour notre système.

## 13. Risques, fragilités et alternatives rejetées

| Risque | Gravité | Atténuation | Décision liée |
|---|---|---|---|
| Sur-ingénierie (microservices/Kafka/K8s non requis par les besoins) | Élevée | Justification pédagogique explicite, déclencheurs, mise en œuvre incrémentale ; repli monolithe modulaire | DEC-01, 07, 20 |
| Exigences non validées | Élevée | Statuts, hypothèses H-x, décisions conditionnelles | Toutes |
| Création de compte pour client non validé | Élevée | Pas de fallback synthétique, validation stricte | DEC-11 |
| Doublons d’événements/messages | Moyenne | Outbox, idempotence consommateur, clés d’idempotence | DEC-07, 11 |
| Point unique de défaillance (Gateway, Keycloak, broker) | Moyenne | Redondance si objectif de HA confirmé ; validation de jeton locale | DEC-08, 10 |
| Fuite de données vers LLM externe | Élevée | LLM local par défaut, décision explicite sinon, lecture seule | DEC-15, 16 |
| Hallucination / injection de prompt via outils | Moyenne | Outils lecture seule, pas d’action destructive, validation | DEC-15–17 |
| Surface d’attaque MCP | Moyenne | Interne uniquement, authentification et autorisation | DEC-16 |
| Dérive de schéma d’événement | Moyenne | Versionnage, tests de contrat | DEC-07, 22 |
| Complexité d’exploitation locale | Moyenne | Compose, paliers d’introduction | DEC-19 |

## 14. Questions d’entretien → décisions

| Question | Réponse courte | Décisions |
|---|---|---|
| Pourquoi cette architecture ? | Domaines séparés par propriété des données, entrée unique, simplicité assumée ; justification pédagogique déclarée | DEC-01–03 |
| Pourquoi ces services ? | Customer et Account possèdent des données distinctes ; l’assistant isole l’IA | DEC-02 |
| Pourquoi Kafka ? | Faits métier immuables multi-consommateurs ; motif en partie pédagogique, périmètre borné | DEC-07 |
| REST ici, événements là ? | REST pour réponse immédiate ; événements pour réactions découplées | DEC-06–07 |
| Qui possède quelles données ? | Customer → clients ; Account → comptes ; `customerId` opaque | DEC-03 |
| Sécuriser les microservices ? | OIDC, validation à chaque saut, secrets externes, CORS restreint | DEC-10, 14 |
| Éviter les doublons Kafka ? | Au moins une fois + outbox + consommateur idempotent | DEC-07 |
| Gérer les pannes ? | Timeouts, retries idempotents, circuit breaker, refus explicite | DEC-11 |
| Scaler ? | Services sans état, partitions Kafka, cache Redis limité et pédagogique (PostgreSQL reste la source de vérité) | DEC-12 |
| Observer ? | Logs structurés, métriques, traces corrélées | DEC-13 |
| Pourquoi MCP ? | Outils standardisés réutilisables ; coût réseau/sécurité assumé | DEC-16 |
| Pourquoi un Agent ? | Non retenu : pas de besoin multi-étapes | DEC-17 |
| RAG ? | Non retenu : pas de corpus non structuré | DEC-17 |
| Évolution ? | Voir §15 | — |

## 15. Trajectoire d’évolution (indicative, non exécutée)

Ordre proposé pour les futures changes d’implémentation, chacune justifiée par
son besoin et validée avant la suivante : (1) services Customer/Account avec
PostgreSQL ; (2) sécurité OIDC ; (3) résilience et idempotence ; (4) Gateway
et frontend ; (5) événements (si H-6 validée) ; (6) observabilité ;
(7) assistant Spring AI puis MCP (si UC-07 validé) ; (8) conteneurs/CI ;
(9) Kubernetes/Helm (si retenu). Le cache Redis (DEC-12, périmètre initial
limité, objectif pédagogique) peut être introduit après la persistance des
clients ; son rang exact est **À CONFIRMER**. RAG et Agent : seulement sur
déclencheur documenté.

## 16. Questions ouvertes (À CONFIRMER)

- Périmètre produit : UC-01..07, rôles et autorisations, règles de cycle de
  vie (suppression/mise à jour).
- Devise, signification du solde, éligibilité d’un client.
- Existence d’un consommateur d’événements et son besoin (H-6).
- Objectifs numériques de disponibilité/latence ; HA ; besoin de Kubernetes.
- Besoin du chat, de l’affichage progressif et des canaux Telegram/Discord ;
  politique de données vers un LLM externe.
- Mécanisme de propagation d’identité vers les outils MCP.
- Noms de services, outils de migration, de contrat et de test, backends
  d’observabilité.

## 17. Revue de cohérence (critères de clôture)

- Chaque `DEC-xx` renvoie à un besoin, des alternatives, des compromis et un
  statut.
- Aucune technologie sans besoin ou sans objectif pédagogique déclaré (Redis
  retenu pour l’objectif pédagogique uniquement, sans besoin de performance
  démontré ; Eureka, Config Server, RAG, Agent explicitement non retenus ou
  différés).
- Aucune exigence candidate présentée comme validée.
- Aucun code, dépendance ou configuration runtime.
