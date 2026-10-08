# PROJECT CONTEXT — EBank AI / System Design

> Document de mémoire permanente du projet.
> Ce fichier doit être maintenu à jour à chaque évolution importante.
> Il sert de contexte de référence pour ChatGPT, GitHub Copilot et OpenSpec.

---

## 1. Objectif du projet

Construire progressivement un projet fil rouge basé sur une architecture microservices Java/Spring, enrichie progressivement avec les technologies modernes de backend, cloud, DevOps et AI.

Le projet doit permettre à Anis de consolider son positionnement :

**Senior Java / Tech Lead + AI Engineer**

avec un profil particulièrement solide sur :

* Java / Spring Boot
* architecture backend
* microservices
* System Design
* communication distribuée
* sécurité
* Kafka
* DevOps / CI-CD
* Spring AI
* MCP
* AI Agents
* RAG
* observabilité
* Docker / Kubernetes

L'objectif n'est pas simplement de construire une application fonctionnelle.

L'objectif principal est de pouvoir :

1. comprendre chaque composant ;
2. expliquer chaque choix technique ;
3. justifier les choix d'architecture ;
4. identifier les compromis ;
5. comprendre les problèmes de production ;
6. défendre l'architecture en entretien Senior / Tech Lead / AI Engineer.

---

## 2. Repository de référence

Repository utilisé comme base pédagogique :

https://github.com/mohamedYoussfi/totale-micro-services-spring-ai-mcp-angular-telegram-discord

IMPORTANT :

Le repository ne doit PAS être simplement cloné puis considéré comme le projet final.

Il sert de :

* base d'apprentissage ;
* support de reverse engineering ;
* source d'inspiration ;
* base pour reconstruire progressivement une architecture maîtrisée ;
* support pour comprendre Spring AI et MCP.

Les fonctionnalités et technologies absentes du repository seront ajoutées progressivement.

---

## 3. Profil de départ

Profil actuel :

* Senior Java
* Tech Lead
* forte expérience backend
* Spring / Spring Boot
* architecture
* microservices
* DevOps
* CI/CD
* Kafka à consolider
* Angular en montée en compétence
* AI Engineering en développement

Le projet doit donc privilégier :

**Backend → Architecture → System Design → DevOps → AI → Frontend**

Angular reste secondaire.

L'objectif n'est pas de devenir Frontend Developer.

---

# 4. Architecture actuelle du repository

Architecture actuellement identifiée :

```text
                        Angular
                           |
                           v
                    Gateway Service
                           |
                         Eureka
                           |
              +------------+------------+
              |                         |
              v                         v
      Customer Service           EBank Service
              |                         |
             H2                        H2
                                        |
                                      Feign
                                        |
                                        v
                                Customer Service


                    EBank Bot
                       |
          +------------+------------+
          |                         |
      Telegram                   Discord
          |                         |
          +------------+------------+
                       |
                    Spring AI
                       |
                      LLM
                       |
                  MCP Client
                       |
              +--------+--------+
              |                 |
              v                 v
       Customer MCP       EBank MCP
          Server             Server
```

---

# 5. Modules actuels

## 5.1 discovery-service

Responsabilité :

* Eureka Server
* service discovery

Objectif pédagogique :

Comprendre :

* service registry ;
* service discovery ;
* découverte dynamique ;
* communication entre microservices ;
* différence entre adresse fixe et découverte dynamique.

---

## 5.2 gateway-service

Responsabilité :

* API Gateway
* point d'entrée HTTP des clients

Architecture :

```text
Client
  |
  v
Gateway
  |
  +--> Customer Service
  |
  +--> EBank Service
```

Évolutions futures possibles :

* authentication ;
* authorization ;
* routing ;
* rate limiting ;
* correlation ID ;
* observability ;
* sécurité.

Ne pas ajouter toutes ces responsabilités immédiatement.

---

## 5.3 customer-service

Responsabilité :

* gestion des clients ;
* exposition d'API REST ;
* exposition de capacités via MCP Server.

Technologies actuelles :

* Java
* Spring Boot
* Spring Web
* Spring Data JPA
* Hibernate
* H2
* Eureka Client
* Actuator
* OpenAPI
* Spring AI MCP Server

Architecture actuelle :

```text
REST Client
    |
    v
Customer Service
    |
    v
H2
```

et :

```text
MCP Client
    |
    v
Customer MCP Server
    |
    v
Customer Service
```

---

## 5.4 ebank-service

Responsabilité :

* logique bancaire ;
* communication avec Customer Service ;
* exposition de capacités MCP.

Technologies actuelles :

* Java
* Spring Boot
* Spring Web
* Spring Data JPA
* H2
* Eureka Client
* OpenFeign
* Resilience4j
* Spring AI MCP Server

Communication actuelle :

```text
EBank Service
      |
      | Feign / REST
      v
Customer Service
```

Cette communication servira de base pour comparer ensuite :

* REST / Feign ;
* Kafka ;
* MCP.

---

## 5.5 ebank-bot

Responsabilité :

* interface conversationnelle ;
* intégration Telegram ;
* intégration Discord ;
* Spring AI ;
* communication MCP.

Technologies actuelles :

* Spring AI
* OpenAI
* MCP Client
* Eureka
* Telegram
* Discord
* Spring Web
* Actuator

Flux conceptuel :

```text
Utilisateur
    |
    v
Telegram / Discord
    |
    v
EBank Bot
    |
    v
Spring AI
    |
    v
LLM
    |
    v
MCP Client
    |
    v
MCP Server
    |
    v
Business Service
```

Ce module deviendra progressivement le cœur AI du projet.

---

## 5.6 angular-front

Responsabilité :

* interface Angular.

Objectif pédagogique :

Comprendre suffisamment Angular pour :

* consommer les APIs ;
* comprendre le flux frontend/backend ;
* maintenir une interface simple.

Angular ne doit pas devenir le centre du projet.

---

## 5.7 ebank-ang-front

Deuxième application frontend Angular présente dans le repository.

Même principe :

* comprendre son rôle ;
* conserver uniquement ce qui est utile ;
* éviter de multiplier inutilement les applications frontend.

---

# 6. Technologies actuellement présentes

Le repository actuel utilise notamment :

* Java 21
* Spring Boot 3.5.x
* Spring Cloud 2025.x
* Spring Web
* Spring Data JPA
* Hibernate
* H2
* Eureka
* Spring Cloud Gateway
* OpenFeign
* Resilience4j
* Spring AI
* MCP Server
* MCP Client
* OpenAI
* Telegram
* Discord
* Angular

IMPORTANT :

H2 est actuellement utilisé.

PostgreSQL n'est pas encore la base principale du projet.

Kafka, Redis, Keycloak, Kubernetes, Helm, observabilité avancée et RAG seront introduits progressivement.

---

# 7. Architecture cible progressive

L'architecture cible doit évoluer progressivement vers :

```text
                           Angular
                              |
                              v
                       API Gateway
                              |
                       Authentication
                              |
                         Keycloak
                              |
                 +------------+------------+
                 |                         |
                 v                         v
          Customer Service          EBank Service
                 |                         |
                 v                         v
            PostgreSQL                PostgreSQL
                 |                         |
                 +------------+------------+
                              |
                            Kafka
                              |
                 +------------+------------+
                 |            |            |
                 v            v            v
              Consumer     Consumer      ...

                             
                         Redis
                    (si vrai besoin)


                       AI Agent
                          |
             +------------+------------+
             |                         |
             v                         v
          MCP Client                  RAG
             |                         |
             v                         v
        MCP Servers                pgvector
             |
             v
       Business Services


                 Observability
                       |
          +------------+-------------+
          |            |             |
        Logs        Metrics        Traces
          |            |             |
          +------------+-------------+
                       |
                 OpenTelemetry
                       |
              Prometheus / Grafana


                 Docker
                    |
                 Kubernetes
                    |
                   Helm
                    |
                  CI/CD
```

Cette architecture est une **cible pédagogique**, pas l'état actuel du projet.

---

# 8. Technologies à introduire progressivement

## Sécurité

* Spring Security
* OAuth2
* OIDC
* JWT
* Keycloak
* rôles
* permissions

## Communication distribuée

* REST
* OpenFeign
* Kafka
* événements métier
* retry
* DLT
* idempotence
* ordering
* partitions
* consumer groups
* offsets
* commit automatique et manuel
* at-most-once
* at-least-once
* exactly-once

## Data

* PostgreSQL
* Redis
* éventuellement pgvector

## AI

* Spring AI
* ChatModel
* prompts
* structured output
* tool calling
* AI Agent
* memory lorsque nécessaire

## MCP

* MCP Server
* MCP Client
* tools
* resources
* interaction Agent ↔ MCP
* comparaison REST / MCP

## RAG

* documents
* chunking
* embeddings
* vector store
* pgvector
* retrieval
* context
* hallucinations
* limites du RAG

## Résilience

* timeout
* retry
* circuit breaker
* fallback
* bulkhead

## Observabilité

* logs
* metrics
* traces
* OpenTelemetry
* Prometheus
* Grafana

## DevOps

* Docker
* Kubernetes
* Kind ou Minikube
* Helm
* GitHub Actions
* Maven
* tests
* Sonar

---

# 9. Principe System Design

System Design doit guider les évolutions importantes.

Avant toute évolution majeure, réfléchir à :

### Fonctionnel

* Que doit faire le système ?
* Quels sont les utilisateurs ?
* Quels sont les flux principaux ?

### Non fonctionnel

* performance ;
* scalabilité ;
* disponibilité ;
* résilience ;
* sécurité ;
* observabilité ;
* cohérence ;
* maintenabilité.

### Architecture

* synchrone ou asynchrone ?
* REST ou événement ?
* Feign ou Kafka ?
* transaction locale ou distribuée ?
* cache nécessaire ou non ?
* scaling horizontal ?
* stateless ou stateful ?

### Production

Toujours se demander :

> Que se passe-t-il si ce composant tombe ?

> Que se passe-t-il si la requête est exécutée deux fois ?

> Que se passe-t-il si le message Kafka est consommé deux fois ?

> Que se passe-t-il si le réseau est lent ?

> Que se passe-t-il si le service dépendant est indisponible ?

> Comment diagnostiquer le problème ?

---

# 10. Règle fondamentale : simplicité

Ne pas ajouter une technologie simplement parce qu'elle est populaire.

Chaque technologie doit avoir :

1. un problème à résoudre ;
2. une justification ;
3. un bénéfice ;
4. un coût/compromis identifié.

Éviter :

* sur-engineering ;
* abstractions inutiles ;
* design patterns artificiels ;
* microservices artificiels ;
* couches inutiles ;
* Kubernetes complexe sans besoin ;
* Redis sans cas d'utilisation ;
* Kafka partout ;
* Agent AI complexe sans raison.

---

# 11. Workflow SDD / OpenSpec

Le projet utilise une approche Specification Driven Development.

Workflow :

```text
requirements
     |
     v
specification
     |
     v
design
     |
     v
tasks
     |
     v
implementation
     |
     v
tests
     |
     v
review
     |
     v
documentation
     |
     v
commit
```

OpenSpec doit être utilisé progressivement comme support de ce workflow.

---

# 12. Rôle de ChatGPT

ChatGPT joue le rôle de :

* mentor ;
* architecte ;
* professeur ;
* reviewer ;
* spécialiste System Design ;
* accompagnateur AI Engineering.

ChatGPT doit :

* expliquer les concepts ;
* proposer l'architecture ;
* rédiger requirements/spec/design/tasks ;
* expliquer les choix ;
* analyser les impacts ;
* revoir le code ;
* proposer les tests ;
* mettre à jour la documentation ;
* mettre à jour `interview.md`.

ChatGPT ne doit PAS générer systématiquement tout le code à la place de l'utilisateur.

---

# 13. Rôle de GitHub Copilot

GitHub Copilot est principalement utilisé comme :

**assistant d'implémentation.**

Workflow préféré :

```text
ChatGPT
   |
   | architecture / spec / task
   v
GitHub Copilot
   |
   | implementation
   v
Utilisateur
   |
   | exécution / vérification
   v
ChatGPT
   |
   | review / explication / tests
   v
Commit
```

Le code doit rester compréhensible et maîtrisé par l'utilisateur.

---

# 14. Règles pédagogiques de code

Le code doit rester :

* simple ;
* lisible ;
* idiomatique ;
* pédagogique ;
* proche des pratiques professionnelles.

Pour les parties importantes, ajouter des commentaires pédagogiques en français lorsque cela facilite la compréhension.

Ne pas commenter chaque ligne inutilement.

L'objectif est de comprendre le code et pas seulement de le faire fonctionner.

---

# 15. Tests

Les tests sont obligatoires.

Selon le besoin :

* JUnit
* Mockito
* Spring Boot Test
* tests d'intégration
* Testcontainers
* tests API
* tests Kafka
* tests de sécurité

Chaque nouvelle fonctionnalité importante doit être accompagnée de tests adaptés.

---

# 16. Documentation d'entretien

Le fichier :

```text
interview.md
```

doit être mis à jour régulièrement.

Pour chaque évolution majeure, ajouter :

### Question

Question d'entretien probable.

### Réponse courte

Réponse de quelques phrases.

### Réponse détaillée

Explication technique.

### Question piège

Point susceptible d'être utilisé par un interviewer.

### Exemple projet

Application concrète dans notre projet.

Exemple :

```text
Question :
Pourquoi utiliser Kafka plutôt que REST ?

Réponse courte :
REST convient aux échanges synchrones...
Kafka permet une communication asynchrone...

Question piège :
Kafka garantit-il qu'un message n'est jamais traité deux fois ?

Réponse :
Non. La consommation doit notamment gérer...
```

---

# 17. Git

Les commits doivent représenter des évolutions logiques.

Exemples :

```text
feat(security): add keycloak authentication

feat(kafka): introduce customer events

test(kafka): add consumer integration tests

feat(ai): add spring ai chat model

feat(mcp): expose customer tools

feat(rag): add document retrieval

feat(observability): add tracing

docs(architecture): document kafka communication
```

Éviter les commits mélangeant plusieurs sujets sans rapport.

---

# 18. Règle : une task à la fois

Le projet doit progresser par petites évolutions.

Pour chaque task :

1. comprendre l'objectif ;
2. comprendre les concepts ;
3. vérifier requirements/spec/design ;
4. identifier les fichiers concernés ;
5. implémenter ;
6. tester ;
7. reviewer ;
8. vérifier les effets secondaires ;
9. mettre à jour la documentation ;
10. mettre à jour `interview.md` ;
11. mettre à jour `PROJECT-CONTEXT.md` si nécessaire ;
12. proposer le commit.

Ne pas avancer automatiquement vers plusieurs grosses fonctionnalités.

---

# 19. Roadmap

## Phase 0 — Reverse Engineering

* analyser le repository ;
* comprendre chaque module ;
* comprendre les flux ;
* comprendre REST / Feign / Eureka / Resilience4j ;
* comprendre MCP existant.

## Phase 1 — Fondation

* nettoyer/comprendre le socle ;
* stabiliser les services ;
* documentation initiale ;
* System Design de départ.

## Phase 2 — SDD / OpenSpec

* requirements ;
* specification ;
* design ;
* tasks ;
* workflow d'implémentation.

## Phase 3 — Security

* Keycloak ;
* OAuth2/OIDC ;
* JWT ;
* Spring Security ;
* rôles/permissions.

## Phase 4 — Communication synchrone et résilience

* Feign ;
* timeout ;
* retry ;
* circuit breaker ;
* fallback.

## Phase 5 — Kafka

Apprendre progressivement :

* broker ;
* topic ;
* partition ;
* offset ;
* producer ;
* consumer ;
* consumer group ;
* ordering ;
* replication ;
* retention ;
* commits ;
* at-most-once ;
* at-least-once ;
* exactly-once ;
* retry ;
* DLT ;
* idempotence ;
* événements métier.

## Phase 6 — Spring AI

* ChatModel ;
* prompts ;
* structured output ;
* tool calling.

## Phase 7 — MCP

* MCP Server ;
* MCP Client ;
* tools ;
* resources ;
* interaction avec l'Agent ;
* REST vs MCP.

## Phase 8 — AI Agent

Construire un Agent capable de :

```text
question
   ↓
LLM
   ↓
choix d'un outil
   ↓
appel tool
   ↓
résultat
   ↓
LLM
   ↓
réponse
```

## Phase 9 — RAG

* documents ;
* chunking ;
* embeddings ;
* pgvector ;
* retrieval ;
* context ;
* hallucinations ;
* limites.

## Phase 10 — Redis

Introduire Redis uniquement avec un vrai cas d'utilisation :

* cache ;
* session ;
* données temporaires ;
* éventuellement autre besoin justifié.

## Phase 11 — Tests avancés

* intégration ;
* Testcontainers ;
* Kafka ;
* PostgreSQL ;
* sécurité ;
* API.

## Phase 12 — Docker

Dockeriser progressivement les composants.

## Phase 13 — Observabilité

* logs ;
* metrics ;
* traces ;
* OpenTelemetry ;
* Prometheus ;
* Grafana.

## Phase 14 — Kubernetes

* concepts fondamentaux ;
* Deployment ;
* Service ;
* ConfigMap ;
* Secret ;
* probes ;
* scaling.

Utiliser Kind ou Minikube localement.

## Phase 15 — Helm

Transformer les manifests Kubernetes en chart Helm.

## Phase 16 — CI/CD

GitHub Actions :

```text
push
 ↓
build
 ↓
tests
 ↓
quality
 ↓
Docker image
 ↓
deployment
```

## Phase 17 — System Design final

Reprendre toute l'architecture et être capable d'expliquer :

* scalabilité ;
* disponibilité ;
* résilience ;
* sécurité ;
* communication ;
* données ;
* Kafka ;
* AI ;
* MCP ;
* RAG ;
* observabilité ;
* Docker ;
* Kubernetes ;
* CI/CD.

---

# 20. Environnement privilégié

Le projet doit rester réalisable avec des outils gratuits/localement lorsque cela est possible.

Privilégier :

* OpenJDK
* Maven
* Spring Boot
* PostgreSQL
* Kafka
* Redis
* Keycloak
* Docker
* Kind / Minikube
* GitHub Free / GitHub Actions dans les limites disponibles
* Ollama lorsque possible pour les expérimentations LLM locales

Éviter les services cloud payants sauf nécessité pédagogique clairement identifiée.

---

# 21. État actuel

## Déjà analysé

Repository de référence :

```text
totale-micro-services-spring-ai-mcp-angular-telegram-discord
```

Architecture actuelle comprise à haut niveau :

```text
Angular
  ↓
Gateway
  ↓
Eureka
  ↓
Customer / EBank

EBank
  ↓ Feign
Customer

EBank Bot
  ↓
Spring AI
  ↓
MCP Client
  ↓
MCP Servers
  ↓
Business services
```

Technologies actuelles identifiées :

* Java 21
* Spring Boot
* Spring Cloud
* Eureka
* Gateway
* Feign
* Resilience4j
* H2
* Spring AI
* MCP
* OpenAI
* Telegram
* Discord
* Angular

---

# 22. Ce qui n'est PAS encore implémenté

Ne pas considérer comme déjà réalisé :

* Keycloak
* OAuth2/OIDC
* JWT
* Kafka
* PostgreSQL
* Redis
* RAG
* pgvector
* AI Agent avancé
* OpenTelemetry
* Prometheus
* Grafana
* Docker complet
* Kubernetes
* Helm
* CI/CD complet

Ces éléments appartiennent à la roadmap future.

---

# 23. Task courante

**Phase : Reverse Engineering**

Objectif actuel :

Comprendre suffisamment le repository de référence pour pouvoir reconstruire et faire évoluer l'architecture de manière maîtrisée.

La prochaine étape après cette cartographie est :

**Créer les documents SDD/OpenSpec initiaux.**

Ordre prévu :

```text
PROJECT-CONTEXT.md
        ↓
requirements.md
        ↓
specification.md
        ↓
design.md
        ↓
tasks.md
```

Puis première task d'implémentation.

---

# 24. Règle absolue du projet

Ne jamais introduire une technologie uniquement pour pouvoir dire qu'elle a été utilisée.

Pour chaque technologie :

```text
Problème
   ↓
Besoin
   ↓
Solution
   ↓
Choix technologique
   ↓
Trade-off
   ↓
Implémentation
   ↓
Test
   ↓
Explication entretien
```

L'objectif final est que l'utilisateur puisse expliquer et défendre chaque choix technique.

---

# 25. Objectif final d'entretien

À la fin du projet, être capable de répondre naturellement à des questions comme :

* Pourquoi des microservices ?
* Pourquoi Eureka ?
* Pourquoi Gateway ?
* Pourquoi Feign ?
* REST ou Kafka ?
* Quand utiliser Kafka ?
* Comment gérer les doublons Kafka ?
* At-most-once vs at-least-once ?
* Comment fonctionne un consumer group ?
* Comment gérer une panne d'un consumer ?
* Pourquoi une DLT ?
* Pourquoi Keycloak ?
* OAuth2 vs OIDC ?
* Comment fonctionne un JWT ?
* Où vérifier les rôles ?
* Pourquoi Redis ?
* Pourquoi PostgreSQL ?
* Comment scaler un microservice ?
* Comment gérer la résilience ?
* Timeout vs retry vs circuit breaker ?
* Pourquoi Spring AI ?
* Qu'est-ce qu'un Tool Calling ?
* Qu'est-ce qu'un AI Agent ?
* REST vs MCP ?
* Pourquoi MCP ?
* Comment fonctionne un MCP Server ?
* Qu'est-ce qu'un RAG ?
* Pourquoi les embeddings ?
* Pourquoi pgvector ?
* Comment limiter les hallucinations ?
* Comment observer un système distribué ?
* Pourquoi OpenTelemetry ?
* Pourquoi Docker ?
* Pourquoi Kubernetes ?
* Pourquoi Helm ?
* Comment construire une CI/CD ?
* Quels sont les compromis de cette architecture ?

Le projet doit servir de support concret à chacune de ces réponses.

---

## 26. Principe pédagogique final

Le projet ne doit jamais être traité comme :

> "Donne-moi le code qui fonctionne."

Il doit être traité comme :

> "Explique-moi le problème, le choix d'architecture, les compromis, puis construisons la solution et vérifions-la."

ChatGPT = mentor / architecte / reviewer.

GitHub Copilot = assistant d'implémentation.

Utilisateur = responsable technique du projet et doit comprendre ce qui est produit.
