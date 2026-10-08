# Design — Project Foundation & Architecture Baseline

## 1. Contexte et périmètre

Cette évolution établit la première baseline formelle de System Design. Elle est
documentaire : elle ne change ni l'implémentation, ni le comportement runtime,
ni les dépendances applicatives.

**Faits vérifiés dans le workspace local** — l'inventaire récursif des fichiers
(hors métadonnées Git) a trouvé :

- À la racine : `.gitignore`, `PROJECT-CONTEXT.md`, `requirements.md`.
- Règles : `.github/skills/project-architecture/SKILL.md` et
  `.github/skills/agents/system-design-architect.agent.md`.
- Configuration IDE : `.idea/.gitignore`, `.idea/misc.xml`,
  `.idea/modules.xml`, `.idea/total-micro-services-spring-ai-mcp-angular.iml`,
  `.idea/vcs.xml`, `.idea/workspace.xml`.
- Change OpenSpec courant :
  `openspec/changes/project-foundation-architecture-baseline/proposal.md`,
  `design.md`, `tasks.md` et
  `specs/architecture-baseline/spec.md`.

`PROJECT-CONTEXT.md` et `requirements.md` sont bien présents. L'inventaire n'a
trouvé aucun code applicatif (`.java`, `.ts`, etc.), aucun POM Maven, ni projet
Angular (`package.json`, `angular.json` ou arborescence `src` d'application).
Il n'a pas trouvé non plus de `specification.md` à la racine; la spécification
de ce change est `specs/architecture-baseline/spec.md`.

Les détails d'implémentation ci-dessous proviennent du dépôt Youssfi
https://github.com/mohamedYoussfi/totale-micro-services-spring-ai-mcp-angular-telegram-discord,
branche `main`, révision
`bf4c7f2750e4c731662e8138023fd8e6b4e4a475`, déjà référencée dans les artefacts
d'exploration. Ils sont des constats de cette référence pédagogique, jamais des
constats du code local. Le dépôt n'a pas été copié, cloné ou intégré au projet.

Les mentions **Vérifié (référence)**, **Déduit** et **À confirmer** séparent les
niveaux de preuve.

## 2. Architecture observée dans la référence pédagogique

```mermaid
flowchart LR
  FE[Angular apps] --> GW[Gateway :9999]
  GW -. "route demandée par les frontends; résolution runtime à confirmer" .-> ES[EBank Service REST / MCP :8057]
  GW -. "route demandée par les frontends; résolution runtime à confirmer" .-> BOT[EBank Bot :8058]
  CS --> CDB[(Customer H2)]
  ES --> EDB[(Accounts H2)]
  ES -->|Feign / REST| CS
  TG[Telegram / Discord] --> BOT
  BOT --> AI[Spring AI]
  AI --> LLM[OpenAI]
  AI -->|MCP client, configured localhost URLs| CSMCP[Customer MCP tools]
  AI -->|MCP client, configured localhost URLs| ESMCP[EBank MCP tools]
  CSMCP --> CS
  ESMCP --> ES
  EUREKA[Discovery Service / Eureka :8761]
  EUREKA -. "Eureka Client; comportement runtime non validé" .- GW
  EUREKA -. "Eureka Client et discovery activée en configuration" .- CS
  EUREKA -. "Eureka Client et discovery activée en configuration" .- ES
  EUREKA -. "Eureka Client; comportement runtime non validé" .- BOT
```

**Déduction architecturale** : le diagramme résume les relations logiques
documentées dans la référence, mais ne constitue pas une validation des flux à
l'exécution. Les flèches pointillées du Gateway indiquent des destinations
demandées par les frontends et un mécanisme de découverte déclaré; la résolution,
la réécriture et le succès du routage restent **à confirmer**. Les liens Eureka
montrent les dépendances/configurations observées, pas une inscription ou
découverte testée. Les appels du bot aux serveurs MCP utilisent des URL locales
explicites dans la configuration de référence. Aucun composant de ce diagramme
n'est de ce fait automatiquement retenu dans l'architecture cible de notre
projet.

## 3. Composants et frontières

| Composant observé dans la référence | Faits vérifiés dans la référence | Analyse / pertinence pour notre cible |
|---|---|---|
| `discovery-service` | Serveur Eureka annoté `@EnableEurekaServer`, port 8761; ne s'enregistre pas lui-même et ne récupère pas de registre selon ses propriétés. | **Analyse** : le registre peut être un point de dépendance du routage dynamique; la haute disponibilité n'est pas démontrée. **À décider pour notre cible** : conserver une découverte dédiée, choisir une autre approche ou ne pas en avoir besoin. |
| `gateway-service` | Gateway WebFlux, Eureka Client, port 9999; bean `DiscoveryClientRouteDefinitionLocator`; CORS global wildcard. Les routes statiques du YAML sont commentées. | **Analyse** : le Gateway centralise les entrées web observées mais ajoute un intermédiaire. **À décider pour notre cible** : besoin réel d'une entrée dédiée, de ses responsabilités et de sa portée. |
| `customer-service` | Spring Web, JPA, H2, Eureka Client, Actuator, OpenAPI, MCP Server WebMVC; Customer id/name/email; REST customers. | **Analyse** : responsabilité de données client identifiable dans la référence. **À décider pour notre cible** : frontière métier autonome, fusion ou autre organisation selon besoins. |
| `ebank-service` | Spring Web, JPA, H2, Eureka Client, Feign, Resilience4j, MCP Server WebMVC; BankAccount et routes accounts. | **Analyse** : propriétaire des comptes dans la référence; `customerId` est une référence inter-service plutôt qu'une association JPA, avec couplage synchrone à Customer. **À décider pour notre cible** : frontière et communication adéquates. |
| `ebank-bot` | Spring Web, Spring AI OpenAI, client MCP, Eureka Client, Actuator, Telegram et Discord; API chat sur port 8058. | **Analyse** : adaptateur AI dépendant d'un LLM, des canaux et des outils. La persistance de mémoire n'est pas établie. **À décider pour notre cible** : rôle, frontières et canaux réellement requis. |
| `angular-front` | Angular 21.1; vues comptes et bot; appels via Gateway; utilise `/chatStream` dans le code observé. | **Analyse** : client web métier/conversationnel dans la référence. L'URL locale est codée en dur. **À décider pour notre cible** : conserver ou remplacer le frontend et ses responsabilités. |
| `ebank-ang-front` | Angular 21.1; vues comptes et bot; appels via Gateway. Le parcours nommé streaming appelle `/chat`, pas `/chatStream`. | **Analyse** : fonctions proches du premier frontend, mais la raison de la duplication reste à confirmer. **À décider pour notre cible** : conserver, fusionner ou supprimer selon besoins. |

### Dépendances Maven notables — faits vérifiés dans les POMs de référence

- Spring Boot parent 3.5.10 et Java 21 dans les POMs consultés.
- Spring Cloud BOM 2025.0.1 dans les services concernés.
- Spring AI BOM 1.1.2 pour les modules MCP et bot.
- Customer : Web, Data JPA, H2, Eureka Client, Config Client, Actuator,
  OpenAPI, MCP Server WebMVC, tests et Lombok.
- EBank : mêmes fondations métier, plus OpenFeign et
  `spring-cloud-starter-circuitbreaker-resilience4j`.
- Gateway : Gateway Server WebFlux, Eureka Client, Actuator, tests WebFlux.
- Bot : Web, Spring AI OpenAI, MCP Client, Eureka Client, Actuator, OpenAPI,
  starter Discord et Telegram.
- Discovery : Eureka Server, Actuator et tests.
- Le starter Config est présent dans Customer, EBank et Bot, mais le client
  Config est désactivé (`spring.cloud.config.enabled=false`) dans les propriétés
  consultées; aucun Config Server n'a été identifié.
- Les Angulars déclarent Angular 21.1, RxJS, Bootstrap/ngx-markdown et scripts
  `start`, `build`, `test`.

Cette liste reflète les POMs/package manifests consultés et ne prétend pas
décrire l'arbre transitif résolu ou le comportement d'exécution.

## 4. Interfaces et flux

### 4.1 API REST — faits vérifiés dans les contrôleurs de référence

| Service | Méthode et chemin | Utilisation observée |
|---|---|---|
| Customer | `GET /customers` | Liste les clients. |
| Customer | `GET /customers/{id}` | Retourne un client par identifiant; utilisé par Feign. |
| Customer | `POST /customers` | Crée un client. |
| EBank | `GET /accounts` | Liste les comptes sans enrichissement Customer dans le chemin consulté. |
| EBank | `GET /accounts/{id}` | Retourne un compte et appelle Customer pour renseigner le client. |
| EBank | `POST /accounts` | Appelle Customer puis persiste le compte. |
| Bot | `GET /chat?query=...` | Appel synchrone au ChatClient. |
| Bot | `GET /chatStream?query=...` | Produit un `Flux<String>` pour la réponse streamée HTTP. |

Les routes REST sont observées dans les contrôleurs consultés. Le comportement
HTTP en erreur, les validations, la pagination et les contrats d'API complets
ne sont pas décrits ici.

### 4.2 Frontend → Gateway → services — faits, déduction et confirmation

**Faits vérifiés dans les frontends de référence** : appels vers le port Gateway,
avec les chemins ci-dessous. Les URL ne prouvent pas à elles seules que le
routage réussit à l'exécution.

```text
angular-front / ebank-ang-front
        | GET /EBANK-SERVICE/accounts
        | GET /EBANK-BOT/chat (et selon le frontend /chatStream)
        v
Gateway WebFlux :9999
        |
        +-- destination de service via discovery locator (intention/config observée)
                |
                +--> EBank Service :8057 --> H2
                +--> EBank Bot :8058 --> Spring AI
```

**Vérifié (référence)** : clients HTTP directs vers le port 9999 avec les
identifiants `EBANK-SERVICE` et `EBANK-BOT`; le bean de discovery locator est
déclaré. Le YAML présente CORS autorisant toutes les origines, méthodes et
en-têtes. Les routes statiques sont commentées.

**Déduit** : le Gateway centralise les destinations vues par le navigateur et
permet le routage basé sur le registre plutôt que sur des routes fixes.

**À confirmer** : routes effectivement produites, normalisation des chemins,
équilibrage, comportement en perte d'Eureka, et inclusion éventuelle d'autres
chemins que les requêtes frontend.

### 4.3 EBank → Customer via Feign/REST

```text
Client
  -> EBank REST / MCP
  -> EbankService
  -> CustomerRestClient (@FeignClient(name = "customer-service"))
  -> HTTP GET /customers/{id}
  -> Customer Service
  -> H2 Customer
```

**Déduction architecturale** : l'échange est synchrone puisque l'appelant attend
la réponse. Feign fournit l'abstraction déclarative du client, mais l'interface
distante reste REST/HTTP. EBank enregistre le compte dans sa propre base.

**Fait vérifié (référence)** : `getBankAccountById` enrichit le compte avec la
réponse Customer; `save` consulte Customer avant la sauvegarde; `getAllBankAccounts`
retourne le résultat du dépôt sans appel Customer.

**Risque déduit** : l'annotation circuit breaker du Feign client peut appeler un
fallback synthétique (« Not available »); la sauvegarde ne valide pas ensuite que
le Customer retourné est réel. Une panne peut ainsi permettre l'enregistrement
d'un compte sans validation effective du client.

**À confirmer** : politiques de timeout/retry/circuit breaker réellement actives,
codes d'erreur et règles métier attendues lors d'une panne Customer.

### 4.4 Bot → Spring AI → MCP → services

```text
Utilisateur web --> Gateway --> Bot /chat ou /chatStream
Telegram (long polling) -------> Bot
Discord (message event) -------> Bot
                                  |
                                  v
                          Spring AI ChatClient
                          /               \
                    OpenAI LLM       ToolCallbackProvider
                                           |
                                      MCP Client
                                     /           \
                   http://localhost:8056/mcp   http://localhost:8057/mcp
                           |                            |
                     Customer MCP                 EBank MCP
                           |                            |
                  Customer methods            EbankService methods
                                                        |
                                                        +-- Feign --> Customer
```

**Déduction architecturale** : ce diagramme synthétise les liens observés et le
chemin logique; il ne constitue pas une trace d'exécution ni la preuve que chaque
requête appelle systématiquement le LLM et un outil MCP.

**Faits vérifiés (référence)** :

- Bot configure le modèle `gpt-4o`, un `ToolCallbackProvider` et un
  `MessageChatMemoryAdvisor`.
- Customer et EBank ont des méthodes métier annotées `@McpTool` et une
  configuration MCP server WebMVC avec protocole `streamable`.
- Le Bot configure des destinations MCP via localhost (`8056/mcp`, `8057/mcp`).
- Telegram reçoit les messages en long polling; Discord transmet les messages
  reçus à l'agent. La réponse est obtenue via `aiAgent.chat(...)`.

**Déductions** : les outils MCP réutilisent des méthodes métier et peuvent
entraîner les mêmes effets métier que les interfaces de service; le parcours
dépend du modèle externe et du service qui héberge l'outil. Les accès aux
plateformes et au LLM sont des dépendances supplémentaires du bot.

**À confirmer** : contrôle d'accès aux outils, disponibilité et filtrage des
outils, portée/stockage de la mémoire, traitement des erreurs, timeouts et
comportement du bot en cas d'indisponibilité LLM/MCP.

## 5. Modes de communication et propriété des données

| Mode | Statut de preuve et usage | Sémantique / limite |
|---|---|---|
| REST/HTTP | **Fait vérifié (référence)** : APIs Customer, EBank, bot; transport sous-jacent de Feign. | **Déduction** : request/response synchrone dans les parcours observés. |
| OpenFeign | **Fait vérifié (référence)** : EBank vers Customer via le nom de service `customer-service`. | **Déduction** : client déclaratif REST, pas protocole différent. |
| MCP | **Fait vérifié (référence)** : Bot client vers serveurs de capacités Customer et EBank; configuration serveur `streamable`. | **Déduction** : contrat de capacité destiné au client AI, distinct des routes REST frontend. |
| HTTP streaming | **Fait vérifié (référence)** : `/chatStream` du bot et appel correspondant depuis `angular-front`. | **Déduction** : réponse HTTP streamée; ne constitue pas un flux événementiel métier. |
| Événementiel métier | **Fait vérifié (référence)** : aucun broker ou flux événementiel métier observé dans les sources consultées. | Kafka reste hors périmètre de cette évolution selon REQ-003/004/REQ-011. |

La propriété des données est **déduite** du découpage et des persistences :
Customer détient Customer dans son H2; EBank détient BankAccount dans son H2 et
une référence `customerId`. Aucune relation transactionnelle inter-base ni
transaction distribuée n'est observée. Les deux bases H2 sont configurées en
mémoire et alimentées par des données d'initialisation; aucune durabilité au-delà
du cycle du processus ne doit être supposée.

## 6. System Design

### Frontières et couplages

- **Fait vérifié dans le code de référence** : Customer et EBank ont des services, contrôleurs, repositories et
  entités séparés; chacun possède sa datasource configurée.
- **Déduit** : la propriété séparée des données réduit le couplage de stockage,
  mais EBank reste fonctionnellement dépendant de Customer lors de deux parcours.
- **Fait vérifié dans le code de référence** : les frontends utilisent Gateway; le bot appelle les serveurs MCP
  par des destinations directes configurées.
- **Déduit** : la combinaison d'appels Gateway/discovery, d'appels Feign et
  d'appels LLM/MCP rend les chemins de panne différents selon le canal utilisé.

### Disponibilité et résilience

- **Fait vérifié dans le code de référence** : un seul service Eureka est configuré dans les sources étudiées;
  EBank déclare un circuit breaker et un fallback Customer.
- **Déduit** : Eureka, Gateway, Customer, EBank, bot, plateformes de messagerie,
  LLM et MCP peuvent constituer des dépendances critiques pour leurs chemins
  respectifs. Un arrêt du Gateway affecte les clients qui l'utilisent; un arrêt
  Customer affecte les opérations EBank qui l'appellent.
- **Fait vérifié dans le code de référence** : le fallback EBank crée un Customer synthétique; le service
  d'enregistrement du compte ne rejette pas ce résultat de fallback avant
  persistance.
- **À confirmer** : réplication, cache de registre, timeouts, retries, seuils,
  gestion d'indisponibilité, idempotence et traitement métier des erreurs.

### Performance et capacité

- **Fait vérifié dans le code de référence** : les frontends adressent Gateway; certains parcours EBank
  appellent ensuite Customer; le bot appelle un fournisseur LLM et éventuellement
  un outil MCP.
- **Déduit** : ces sauts réseau et appels externes augmentent la latence de bout
  en bout; le LLM et les outils MCP peuvent dominer le temps de réponse AI.
- **Fait vérifié dans le code de référence** : les datasources H2 utilisent `mem:` et les destinations MCP du bot
  sont localhost.
- **Déduit** : ces paramètres privilégient un démarrage local simple et limitent
  la démonstration d'une mise à l'échelle multi-instance avec état durable.
- **À confirmer** : débit, limites de concurrence, mesures de latence, objectifs
  de disponibilité et comportement de charge; aucun benchmark/SLO n'est fourni.

### Sécurité

- **Fait vérifié dans le code de référence** : les POMs consultés n'incluent pas Spring Security; le Gateway
  autorise globalement toutes les origines, méthodes et en-têtes via CORS; les
  méthodes MCP comprennent des opérations de création.
- **Déduit** : l'exposition des endpoints et outils sans mécanisme visible
  d'authentification est un risque d'accès, mais les contrôles externes ou
  d'environnement ne sont pas inspectés.
- **À confirmer** : exposition réseau réelle, protection des outils/endpoints MCP,
  secrets LLM/Telegram/Discord et exposition des consoles/endpoints de gestion.
- Cette évolution documente ces limites seulement; elle ne conçoit ni n'implémente
  un mécanisme de sécurité.

### Observabilité

- **Fait vérifié dans le code de référence** : Actuator est une dépendance listée pour Gateway, Discovery,
  Customer, EBank et Bot.
- **À confirmer** : endpoints exposés, health checks configurés, format/corrélation
  des logs et métriques réellement collectées.
- Aucune instrumentation distribuée avancée ne doit être supposée ou introduite
  dans ce change.

### Alternatives et compromis

| Décision observée dans la référence | Besoin adressé (interprétation) | Alternative pertinente (non implémentée) | Compromis (analyse architecturale) |
|---|---|---|---|
| Gateway + routage découvert | Entrée web et destinations de services centralisées. | URLs directes côté clients ou routes fixes. | Centralisation et découverte contre un composant supplémentaire et une dépendance de routage/registre. |
| Eureka | Découverte par identifiant plutôt que connaissance des adresses d'instances. | Configuration statique des adresses. | Découverte dynamique contre opération et disponibilité du registre; le mode haute disponibilité n'est pas démontré. |
| Feign sur REST | Client inter-service déclaratif, intégré à Spring Cloud. | Client HTTP explicite ou appels directs depuis le client. | Lisibilité et intégration discovery contre couplage d'exécution synchrone et latence réseau. |
| MCP pour outils AI | Décrire des capacités appelables par le client AI. | Appels REST directs du bot aux APIs métier. | Interface de capacités adaptée à l'usage AI contre protocole/contrat supplémentaire et exigences de contrôle d'accès à vérifier. |
| H2 mémoire | Exécution locale/pédagogique légère. | Stockage persistant déployé. | Simplicité d'exécution contre perte d'état à l'arrêt et limites de démonstration distribuée. |

Kafka n'est pas une alternative à implémenter dans ce change; seule sa place
future est mentionnée dans `requirements.md`.

## 7. Limites et questions ouvertes

1. **À confirmer** — les sources locales de services ne sont pas présentes; la correspondance
   entre la référence et le futur code du projet est à confirmer.
2. **À confirmer** — les routes et filtres effectivement créés par discovery locator n'ont pas été
   validés par démarrage du système.
3. **À confirmer** — la configuration runtime des circuits, timeouts et retries n'est pas établie.
4. **À confirmer** — le fallback peut permettre une création de compte sans validation Customer;
   la politique métier attendue doit être clarifiée, sans être modifiée ici.
5. **À confirmer** — les contrôles d'accès MCP et les secrets d'intégration ne sont pas déterminés.
6. **À confirmer** — le stockage et la portée de `ChatMemory` ne sont pas établis par les sources
   examinées.
7. **À confirmer** — `angular-front` et `ebank-ang-front` sont proches; les différences
   fonctionnelles attendues restent à établir, notamment leur comportement de
   streaming.
8. **À confirmer** — l'usage du starter Config alors que le client est désactivé, et l'absence de
   Config Server identifié, sont à expliquer si cela reste pertinent au projet.
9. **À confirmer** — l'état opérationnel Actuator, les objectifs de performance et les garanties
   de disponibilité ne sont pas documentés.

## 8. Conséquences pour cette évolution

- La baseline doit être utile sans runtime local ni hypothèses non prouvées.
- Les faits de la référence doivent être associés à la révision consultée.
- Les questions ouvertes restent explicitement ouvertes jusqu'à disponibilité
  des sources du projet ou vérification runtime ultérieure.
- Cette baseline décrit et analyse l'architecture de référence; elle ne valide
  pas automatiquement une architecture cible pour notre projet.
- **Architecture cible de notre projet : à décider** à partir des exigences
  métier, fonctionnelles et non fonctionnelles, d'une comparaison d'alternatives
  et des critères de simplicité, maintenabilité, évolutivité et valeur
  pédagogique. Les composants observés peuvent être conservés, fusionnés,
  ajoutés, remplacés ou supprimés; aucun choix de ce type n'est pris par ce
  document.
- La documentation de contexte/interview peut être mise à jour dans la phase
  d'exécution documentaire, conformément aux requirements.
- Aucun service, endpoint, dépendance, sécurité ou déploiement ne change.
