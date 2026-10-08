# Requirements — Project Foundation & Architecture Baseline

## 1. Objectif

Établir la première fondation formelle du projet afin de pouvoir faire évoluer progressivement le repository vers une architecture moderne Java/Spring orientée microservices, System Design et AI Engineering.

Cette première évolution ne doit pas implémenter Kafka, Keycloak, RAG, Redis, Kubernetes ou les autres fonctionnalités avancées.

Elle doit d'abord formaliser l'architecture actuelle et préparer le projet à ses évolutions futures.

---

# 2. Contexte

Le projet est basé sur le repository de référence :

https://github.com/mohamedYoussfi/totale-micro-services-spring-ai-mcp-angular-telegram-discord

Le repository contient actuellement plusieurs composants :

* `discovery-service`
* `gateway-service`
* `customer-service`
* `ebank-service`
* `ebank-bot`
* `angular-front`
* `ebank-ang-front`

L'architecture actuelle repose notamment sur :

* Java
* Spring Boot
* Spring Cloud
* Eureka
* Spring Cloud Gateway
* OpenFeign
* Resilience4j
* JPA
* H2
* Spring AI
* MCP
* OpenAI
* Telegram
* Discord
* Angular

Cette première évolution doit permettre de transformer cette base en projet pédagogique structuré et maîtrisé.

---

# 3. Objectifs fonctionnels

## REQ-001 — Identifier clairement les composants

Le projet doit disposer d'une vision claire du rôle de chaque composant :

* service discovery ;
* API Gateway ;
* Customer Service ;
* EBank Service ;
* EBank Bot ;
* applications frontend.

Chaque composant doit avoir une responsabilité clairement identifiée.

---

## REQ-002 — Formaliser les flux principaux

Les flux suivants doivent être documentés :

### Flux frontend

```text
Angular
   ↓
Gateway
   ↓
Business Service
```

### Communication inter-service

```text
EBank Service
   ↓
Feign / REST
   ↓
Customer Service
```

### Flux AI / MCP

```text
Utilisateur
   ↓
Telegram / Discord
   ↓
EBank Bot
   ↓
Spring AI
   ↓
LLM
   ↓
MCP Client
   ↓
MCP Server
   ↓
Business Service
```

---

## REQ-003 — Distinguer les modes de communication

Le projet doit permettre de comprendre et de distinguer clairement :

* communication REST synchrone ;
* communication Feign entre microservices ;
* communication MCP entre un client AI et les capacités exposées par les services.

Kafka sera introduit ultérieurement pour étudier la communication événementielle asynchrone.

---

# 4. Objectifs techniques

## REQ-004 — Conserver une architecture simple

L'architecture initiale doit rester volontairement simple.

Ne pas introduire prématurément :

* Kafka ;
* Redis ;
* Keycloak ;
* PostgreSQL ;
* Kubernetes ;
* Helm ;
* RAG ;
* OpenTelemetry ;
* Prometheus ;
* Grafana.

Ces technologies appartiennent aux évolutions futures du projet.

---

## REQ-005 — Préparer les évolutions futures

L'architecture et la documentation doivent permettre d'introduire progressivement :

```text
Security
    ↓
Kafka
    ↓
Spring AI
    ↓
Tool Calling
    ↓
MCP
    ↓
AI Agent
    ↓
RAG
    ↓
Redis
    ↓
Observability
    ↓
Docker
    ↓
Kubernetes
    ↓
Helm
    ↓
CI/CD
```

Chaque évolution devra être traitée comme une évolution architecturale distincte.

---

# 5. System Design

## REQ-006 — Documenter les responsabilités

Pour chaque composant important, identifier :

* responsabilité ;
* dépendances ;
* données manipulées ;
* communications entrantes ;
* communications sortantes ;
* risques de disponibilité ;
* possibilités de scaling ;
* problèmes potentiels.

---

## REQ-007 — Identifier les choix architecturaux

Pour les choix importants, documenter :

* problème rencontré ;
* solution choisie ;
* raison du choix ;
* alternatives possibles ;
* compromis.

Exemples :

* pourquoi Gateway ?
* pourquoi Eureka ?
* pourquoi Feign ?
* pourquoi REST ?
* pourquoi MCP ?
* pourquoi Kafka plus tard ?

---

# 6. Contraintes pédagogiques

## REQ-008 — Compréhension obligatoire

Aucune technologie ne doit être ajoutée uniquement pour être présente dans le projet.

Chaque technologie doit répondre à un problème ou à un objectif pédagogique clairement identifié.

---

## REQ-009 — Pas de sur-engineering

Le projet doit privilégier :

* simplicité ;
* lisibilité ;
* compréhension ;
* maintenabilité ;
* architecture réaliste.

Éviter :

* abstractions inutiles ;
* design patterns artificiels ;
* microservices artificiels ;
* couches inutiles ;
* technologies sans cas d'utilisation.

---

## REQ-010 — Une évolution à la fois

Chaque évolution importante doit être traitée séparément.

Une évolution doit suivre :

```text
Requirements
    ↓
Specification
    ↓
Design
    ↓
Tasks
    ↓
Implementation
    ↓
Tests
    ↓
Review
    ↓
Documentation
    ↓
Commit
```

---

# 7. Tests

## REQ-011 — Tests obligatoires

Toute fonctionnalité implémentée doit disposer de tests adaptés.

Selon le besoin :

* tests unitaires ;
* tests Spring Boot ;
* tests d'intégration ;
* tests API ;
* tests de sécurité ;
* tests Kafka ;
* Testcontainers.

Pour cette première évolution, aucun nouveau comportement métier complexe n'est demandé.

---

# 8. Documentation

## REQ-012 — PROJECT-CONTEXT.md

Le fichier `PROJECT-CONTEXT.md` doit rester la mémoire permanente du projet.

Il doit être mis à jour lorsque :

* l'architecture évolue ;
* une décision importante est prise ;
* une technologie est ajoutée ;
* l'état du projet change significativement ;
* une task importante est terminée.

---

## REQ-013 — interview.md

Le fichier `interview.md` devra être alimenté progressivement.

Pour chaque évolution importante, il devra contenir :

* question d'entretien ;
* réponse courte ;
* réponse détaillée ;
* question piège ;
* exemple concret du projet.

---

# 9. Git

## REQ-014 — Évolutions traçables

Chaque évolution logique doit être identifiable par un commit Git.

Exemples :

```text
docs: add project context
docs: define architecture requirements
feat(security): add keycloak authentication
feat(kafka): introduce customer events
feat(ai): add spring ai
feat(mcp): expose customer tools
```

Les commits doivent éviter de mélanger des évolutions sans rapport.

---

# 10. Critères d'acceptation

Cette première évolution sera considérée comme correctement formalisée lorsque :

### Architecture

* les composants principaux sont identifiés ;
* leurs responsabilités sont documentées ;
* les flux principaux sont compris ;
* les communications REST / Feign / MCP sont distinguées.

### Documentation

Les documents suivants pourront être produits à partir de ces requirements :

```text
PROJECT-CONTEXT.md
requirements.md
specification.md
design.md
tasks.md
```

### SDD

Le workflow SDD/OpenSpec est clairement défini.

### Pédagogie

Les choix architecturaux importants sont expliqués et pourront être défendus en entretien.

### Scope

Aucune technologie avancée supplémentaire n'est introduite dans cette première évolution.

---

# 11. Hors périmètre

Les éléments suivants sont explicitement hors périmètre de cette première évolution :

* implémentation Kafka ;
* Keycloak ;
* OAuth2/OIDC ;
* JWT ;
* PostgreSQL ;
* Redis ;
* RAG ;
* pgvector ;
* AI Agent avancé ;
* observabilité avancée ;
* Dockerisation complète ;
* Kubernetes ;
* Helm ;
* CI/CD.

Ils seront traités dans des évolutions ultérieures.

---

# 12. Principe directeur

Le projet doit permettre à l'utilisateur de comprendre et défendre chaque décision technique.

La priorité n'est donc pas :

> produire rapidement beaucoup de code.

La priorité est :

> comprendre → spécifier → concevoir → implémenter → tester → expliquer.

ChatGPT agit comme mentor, architecte et reviewer.

GitHub Copilot agit comme assistant d'implémentation.

L'utilisateur reste responsable de la compréhension et de la validation de la solution.
