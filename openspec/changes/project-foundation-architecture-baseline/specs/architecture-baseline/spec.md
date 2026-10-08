# Architecture Baseline

## Purpose

Décrire la baseline d'architecture et de System Design de la première évolution
à partir des documents locaux et de l'exploration du dépôt Youssfi de référence,
sans assimiler ce dépôt au code local ni l'intégrer au projet.

Le dépôt Youssfi est une source d'inspiration métier et pédagogique, pas un
blueprint ni une architecture cible obligatoire. La baseline décrit ce qui y est
observé et en analyse les choix; elle ne décide pas par défaut que ses services,
frontends, protocoles ou technologies doivent être conservés dans notre cible.

Le workspace local examiné contient `PROJECT-CONTEXT.md`, `requirements.md`,
`.gitignore`, les règles `.github/skills/`, la configuration `.idea/` et les
artefacts OpenSpec de ce change. Aucun code applicatif, POM Maven ou projet
Angular n'a été trouvé dans cet inventaire. Les éléments d'implémentation
rapportés par la baseline proviennent du dépôt pédagogique
`mohamedYoussfi/totale-micro-services-spring-ai-mcp-angular-telegram-discord`,
branche `main`, révision `bf4c7f2750e4c731662e8138023fd8e6b4e4a475`; ils ne
décrivent pas du code local.

Les formulations relatives à l'implémentation doivent distinguer explicitement :

- **Fait vérifié dans le code de référence** : constaté dans les sources ou
  configurations consultées.
- **Déduction architecturale** : conséquence ou interprétation raisonnable de
  faits explicités.
- **À confirmer** : élément non établi par le code consulté ou nécessitant une
  validation dans le projet local ou à l'exécution.

## ADDED Requirements

### Requirement: Source et niveau de preuve explicites

La baseline MUST identifier ses sources et MUST distinguer le contenu du
workspace local de celui du dépôt pédagogique de référence. Elle MUST NOT
présenter le code Youssfi comme du code local, une dépendance ou une
implémentation intégrée.

#### Scenario: Workspace sans sources applicatives

- **WHEN** les fichiers disponibles dans le workspace local sont recensés
- **THEN** la baseline indique que `PROJECT-CONTEXT.md` et `requirements.md`
  sont présents mais qu'aucun code applicatif, POM Maven ou projet Angular n'y
  a été trouvé
- **AND** elle attribue au dépôt Youssfi les constats issus de ses sources
- **AND** elle liste comme points à confirmer les comportements qui ne peuvent
  être vérifiés localement

#### Scenario: Fait, déduction ou incertitude

- **WHEN** une affirmation décrit une responsabilité, un flux, une qualité ou
  une limite
- **THEN** son statut de preuve est identifiable comme fait vérifié, déduction
  ou point à confirmer
- **AND** une déduction ne doit pas être présentée comme un comportement testé

### Requirement: Composants de référence, responsabilités et données

La baseline MUST document les composants observés dans la référence
(`discovery-service`, `gateway-service`, `customer-service`, `ebank-service`,
`ebank-bot`, `angular-front`, `ebank-ang-front`), leurs responsabilités, leurs
technologies vérifiées, leurs dépendances, données détenues et communications.
Elle MUST distinguer cette cartographie de toute décision de conserver ces
composants dans l'architecture cible du projet.

#### Scenario: Services métier

- **WHEN** customer-service et ebank-service sont décrits
- **THEN** customer-service est identifié comme propriétaire de l'entité
  Customer (`id`, `name`, `email`) persistée par JPA dans H2
- **AND** ebank-service est identifié comme propriétaire de BankAccount
  (`id`, `createdAt`, `balance`, `type`, `customerId`) persistée par JPA dans
  H2
- **AND** le champ `customer` de BankAccount est identifié comme `@Transient`,
  non persisté comme relation JPA
- **AND** les URL de datasource `jdbc:h2:mem:customer-db` et
  `jdbc:h2:mem:accounts-db` sont décrites comme bases en mémoire dans la
  configuration consultée

#### Scenario: Services de plateforme et applications clientes

- **WHEN** discovery-service, gateway-service, ebank-bot et les deux frontends
  sont décrits
- **THEN** leurs responsabilités vérifiées et leurs dépendances observées sont
  explicitées
- **AND** la raison d'être distincte des deux frontends est marquée à confirmer
  lorsque les sources ne la définissent pas
- **AND** la présence de ces composants dans la référence n'est pas utilisée
  comme preuve qu'ils sont requis dans la cible du projet

### Requirement: Interfaces REST et communications inter-services

La baseline MUST distinguer les routes REST observées dans la référence, le
routage Gateway et l'appel synchrone EBank vers Customer via Feign/HTTP. Ces
flux MUST être documentés comme architecture observée, et non comme décision
implicite sur les flux cibles du projet.

#### Scenario: API REST métier

- **WHEN** les interfaces REST du dépôt de référence sont recensées
- **THEN** Customer expose `GET /customers`, `GET /customers/{id}` et
  `POST /customers`
- **AND** EBank expose `GET /accounts`, `GET /accounts/{id}` et
  `POST /accounts`
- **AND** le bot expose `GET /chat?query=...` et
  `GET /chatStream?query=...`
- **AND** la baseline n'invente pas d'autres routes non vérifiées

#### Scenario: Frontend vers Gateway vers service

- **WHEN** les flux des frontends sont décrits
- **THEN** ils incluent les appels vérifiés vers
  `http://localhost:9999/EBANK-SERVICE/accounts` et
  `http://localhost:9999/EBANK-BOT/chat`
- **AND** `angular-front` appelle également
  `http://localhost:9999/EBANK-BOT/chatStream?query=...`
- **AND** la méthode cliente nommée `askAgentSteam` de `ebank-ang-front`
  appelle `/EBANK-BOT/chat?query=...`, et non `/chatStream`
- **AND** la baseline ne déduit pas une différence fonctionnelle voulue entre
  les frontends de ces seuls noms de méthode ou chemins
- **AND** la baseline distingue les appels observés dans les frontends de la
  configuration de routage du Gateway
- **AND** elle indique que le Gateway WebFlux déclare un
  `DiscoveryClientRouteDefinitionLocator` et dépend d'Eureka
- **AND** elle indique que les routes statiques `/customers/**` et `/accounts/**`
  du YAML sont commentées
- **AND** le comportement effectif du routage découvert reste à confirmer si
  aucune validation runtime n'a été effectuée

#### Scenario: Communication EBank vers Customer

- **WHEN** l'appel inter-service est décrit
- **THEN** la baseline identifie `CustomerRestClient` comme client Feign nommé
  `customer-service`
- **AND** elle précise que Feign réalise ici un appel HTTP/REST synchrone vers
  `GET /customers/{id}`, et n'est pas un protocole réseau distinct
- **AND** elle décrit son usage dans la lecture d'un compte par identifiant et
  dans l'enregistrement d'un compte
- **AND** elle indique que la liste des comptes n'effectue pas cet enrichissement
  dans le code consulté

### Requirement: Eureka, Gateway, Feign et résilience

La baseline MUST décrire les usages vérifiés de la découverte, du Gateway et de
Resilience4j sans attribuer de garanties runtime non démontrées.

#### Scenario: Eureka

- **WHEN** discovery-service est décrit
- **THEN** il est identifié comme serveur Eureka (`@EnableEurekaServer`) sur le
  port `8761`
- **AND** la configuration consultée indique
  `register-with-eureka=false` et `fetch-registry=false`
- **AND** gateway-service, Customer, EBank et le bot sont décrits comme clients
  Eureka lorsque leur POM/configuration le vérifie
- **AND** les URL MCP fixes du bot sont distinguées de la découverte Eureka

#### Scenario: Gateway et CORS

- **WHEN** les responsabilités du Gateway sont documentées
- **THEN** il est décrit comme point d'entrée observé pour les appels des
  frontends et comme Gateway Spring Cloud WebFlux sur le port `9999`
- **AND** la configuration CORS wildcard observée est mentionnée sans être
  présentée comme un mécanisme d'authentification ou d'autorisation
- **AND** l'absence de Spring Security dans les POMs consultés est rapportée
  comme constat limité aux sources inspectées

#### Scenario: Circuit breaker et fallback

- **WHEN** Resilience4j est décrit
- **THEN** la baseline mentionne l'annotation `@CircuitBreaker` présente sur
  l'appel Feign et le fallback qui retourne un Customer générique « Not available »
- **AND** elle signale que la création de compte persiste le compte après cet
  appel sans vérifier le contenu du Customer de fallback
- **AND** elle en déduit un risque de création de compte sans validation effective
  du client en cas de panne Customer
- **AND** les timeouts, retries, seuils et comportements effectifs non établis
  restent des points à confirmer

### Requirement: Capacités MCP et flux AI

La baseline MUST décrire les capacités MCP exposées par les services et le rôle
de Spring AI dans le bot, tout en les distinguant des APIs REST ordinaires.

#### Scenario: Serveurs MCP métier

- **WHEN** les serveurs MCP sont décrits
- **THEN** Customer et EBank sont associés au starter MCP Server WebMVC
  et au protocole `streamable` configuré dans les propriétés consultées
- **AND** les méthodes métier annotées `@McpTool` sont décrites comme capacités
  d'accès aux clients et aux comptes
- **AND** les capacités observées incluent lecture/création de clients et
  lecture/création de comptes
- **AND** la protection effective et les contrôles d'accès aux endpoints MCP
  restent à confirmer

#### Scenario: Bot, Spring AI, LLM et MCP

- **WHEN** le flux AI/MCP est formalisé
- **THEN** il représente l'entrée via l'API web du bot ou via Telegram/Discord,
  Spring AI ChatClient, le modèle OpenAI configuré et l'utilisation des outils
  MCP disponibles
- **AND** il distingue les connexions MCP configurées vers
  `http://localhost:8056/mcp` et `http://localhost:8057/mcp` de l'enregistrement
  Eureka du bot
- **AND** il précise que le bot configure un `ToolCallbackProvider` et un
  `MessageChatMemoryAdvisor`
- **AND** le stockage et la portée de la mémoire de conversation restent à
  confirmer
- **AND** le flux décrit n'est pas présenté comme une architecture d'agent AI
  avancé

#### Scenario: Synchrone, streaming et asynchrone

- **WHEN** les modes de communication sont comparés
- **THEN** REST et l'appel Feign sont qualifiés de communications
  request/response synchrones
- **AND** `/chatStream` est décrit comme streaming HTTP de réponse
- **AND** le transport MCP `streamable` n'est pas confondu avec de
  l'événementiel métier asynchrone
- **AND** aucun flux métier Kafka ou autre broker événementiel n'est décrit
  comme existant

### Requirement: System Design et décisions justifiées

La baseline MUST inclure un System Design substantiel couvrant responsabilités,
frontières, flux, couplages, données, disponibilité, résilience, performance,
scalabilité, sécurité, observabilité, fragilités et compromis. Les choix
importants de la référence MUST expliquer le besoin auquel ils répondent, les
alternatives et les compromis. Toute décision concernant l'architecture cible
du projet MUST être explicitement justifiée par les exigences et critères du
projet; si elle n'est pas prise dans cette évolution, elle MUST rester marquée
comme indécise. La baseline MUST NOT assimiler un choix observé à un choix déjà
validé pour le projet.

#### Scenario: Analyse de disponibilité et de scalabilité

- **WHEN** disponibilité, performance et scalabilité sont discutées
- **THEN** la baseline identifie les dépendances réseau Gateway, Eureka,
  Customer, LLM et MCP observées
- **AND** elle explique qualitativement les points de fragilité et les sauts
  réseau déduits
- **AND** elle identifie H2 en mémoire et les adresses MCP localhost comme
  limites pour une topologie distribuée ou multi-instance
- **AND** elle ne prétend pas avoir mesuré latence, capacité ou SLO

#### Scenario: Sécurité et observabilité

- **WHEN** sécurité et observabilité sont décrites
- **THEN** la baseline rapporte les faits vérifiés sur les dépendances,
  CORS, Actuator, routes et outils MCP
- **AND** elle marque les protections MCP, secrets, endpoints Actuator et
  mécanismes runtime non inspectés comme points à confirmer
- **AND** elle n'affirme pas l'existence de contrôles non présents dans les
  éléments examinés

#### Scenario: Alternatives et compromis

- **WHEN** Gateway, Eureka, Feign/REST et MCP sont justifiés
- **THEN** chaque décision comprend le besoin auquel elle répond, l'alternative
  pertinente et les compromis observables ou attendus
- **AND** les alternatives sont discutées comme options, jamais comme composants
  actuellement déployés
- **AND** Kafka demeure une évolution future mentionnée par les requirements et
  ne devient ni une dépendance ni un flux de cette baseline

#### Scenario: Décider de notre architecture indépendamment de la référence

- **WHEN** un composant, frontend, protocole ou choix technologique de la
  référence est évalué pour notre projet
- **THEN** la baseline sépare l'architecture observée, son analyse et la cible
  de notre projet
- **AND** conserver, fusionner, ajouter, remplacer ou supprimer un composant
  reste possible si cela est justifié par le besoin métier, les exigences
  fonctionnelles/non fonctionnelles, le System Design, la simplicité, la
  maintenabilité, l'évolutivité et la valeur pédagogique/interview
- **AND** aucun microservice, frontend, mode de communication ou technologie
  n'est obligatoire du seul fait de sa présence dans la référence ou une roadmap
- **AND** les décisions non prises restent explicitement « à décider »

### Requirement: Respect du périmètre documentaire

Cette évolution MUST rester une formalisation documentaire de l'architecture
baseline. Elle MUST NOT générer du code applicatif, modifier le runtime, copier
ou intégrer le dépôt de référence, ou ajouter une technologie hors périmètre.

#### Scenario: Change documentaire seulement

- **WHEN** les tâches de cette évolution sont exécutées
- **THEN** elles produisent et vérifient les artefacts SDD/OpenSpec et, selon les
  exigences documentaires, actualisent la documentation de contexte/interview
- **AND** elles ne modifient aucun service, configuration applicative, dépendance
  ou frontend
- **AND** aucun test runtime n'est requis en l'absence de changement de code
- **AND** les limites de preuve et questions ouvertes restent documentées au lieu
  d'être résolues par supposition
- **AND** le dépôt de référence reste une source d'inspiration et n'est ni copié,
  cloné, intégré ni transformé en architecture cible automatique
