# Requirements — Project Foundation & Architecture Baseline

## 1. Objectif

Établir la première fondation formelle du projet afin de pouvoir faire évoluer progressivement le repository vers une architecture moderne adaptée à ses besoins, en s'appuyant sur Java/Spring, System Design et AI Engineering lorsque ces choix sont justifiés.

Cette première évolution ne doit pas implémenter Kafka, Keycloak, RAG, Redis, Kubernetes ou les autres fonctionnalités avancées.

Elle doit d'abord formaliser l'architecture observée dans le dépôt de référence, l'analyser et poser les critères qui guideront la conception de NOTRE architecture. Elle ne doit pas considérer l'architecture de référence comme une architecture cible obligatoire.

---

# 2. Contexte

Le projet s'inspire du repository de référence :

https://github.com/mohamedYoussfi/totale-micro-services-spring-ai-mcp-angular-telegram-discord

Le repository de référence contient notamment les composants suivants, qui sont des sujets d'analyse et non une liste de composants imposés à notre architecture :

* `discovery-service`
* `gateway-service`
* `customer-service`
* `ebank-service`
* `ebank-bot`
* `angular-front`
* `ebank-ang-front`

L'implémentation observée dans ce repository de référence utilise notamment :

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

Cette première évolution doit permettre de comprendre l'idée métier et les choix de cette référence, d'en analyser les avantages, limites et compromis, puis de concevoir progressivement un projet pédagogique structuré et maîtrisé qui peut s'en écarter.

---

# 3. Objectifs fonctionnels

## REQ-001 — Identifier clairement les composants

La documentation doit donner une vision claire du rôle de chaque composant observé dans la référence et de sa pertinence éventuelle pour notre projet :

* service discovery ;
* API Gateway ;
* Customer Service ;
* EBank Service ;
* EBank Bot ;
* applications frontend.

Pour chaque composant, distinguer sa responsabilité observée de toute décision de le conserver, fusionner, remplacer ou supprimer dans notre architecture cible. La présence d'un composant dans la référence ne rend pas sa conservation obligatoire.

---

## REQ-002 — Formaliser les flux principaux

Les flux suivants doivent être documentés comme flux observés ou décrits dans la référence pédagogique. Ils ne constituent pas automatiquement les flux cibles de notre projet.

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

Le projet doit permettre de comprendre et de distinguer clairement, dans l'architecture observée et dans les décisions futures lorsque pertinentes :

* communication REST synchrone ;
* communication Feign entre microservices ;
* communication MCP entre un client AI et les capacités exposées par les services.

La communication événementielle, notamment Kafka, peut être étudiée dans une évolution dédiée si un besoin métier/architectural et une valeur pédagogique le justifient. Son introduction n'est pas un engagement automatique.

---

# 4. Objectifs techniques

## REQ-004 — Conserver une architecture simple dans l'évolution actuelle

L'architecture initiale doit rester volontairement simple.

Dans cette première évolution documentaire, ne pas implémenter ni introduire :

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

Ces technologies et capacités ne sont ni requises ni exclues de façon absolue pour toute la durée du projet. Leur pertinence sera évaluée dans une évolution distincte, à partir d'un besoin démontré, et non selon une roadmap d'adoption automatique.

---

## REQ-005 — Préparer les évolutions futures

La documentation peut identifier des domaines d'apprentissage possibles, sans prescrire l'architecture cible ni garantir l'adoption de chaque technologie :

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

Chaque sujet retenu devra être traité comme une évolution architecturale distincte et justifiée. L'ordre ou la présence dans cette liste ne vaut pas décision d'architecture.

---

# 5. System Design

## REQ-006 — Documenter les responsabilités

Pour chaque composant observé dans la référence et chaque composant retenu dans notre architecture, identifier séparément :

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

Pour chaque choix important de NOTRE architecture, documenter :

* problème rencontré ;
* solution choisie et raison du choix, ou alternatives/critères et état « à décider » tant qu'aucun choix n'est validé ;
* alternatives possibles ;
* compromis.

L'analyse de la référence peut éclairer ces décisions, sans les prédéterminer. Exemples :

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

Chaque technologie retenue dans notre architecture doit répondre à un problème ou à un objectif pédagogique clairement identifié.

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
* microservices artificiels ou séparation de services héritée uniquement par fidélité à la référence ;
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

* les composants principaux observés dans le dépôt de référence sont identifiés et leurs responsabilités observées sont documentées ;
* les flux de référence sont compris et différenciés des flux éventuellement choisis pour notre architecture ;
* les communications REST / Feign / MCP sont distinguées ;
* la référence est analysée comme source d'inspiration et non comme blueprint architectural ; les choix de conserver, fusionner, remplacer ou supprimer ses composants ne sont pas présumés.

### Documentation

Les documents suivants pourront être produits à partir de ces requirements :

```text
PROJECT-CONTEXT.md
requirements.md
openspec/changes/<change>/specs/<capability>/spec.md
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

Les éléments suivants sont hors périmètre d'implémentation de cette première évolution documentaire (cela ne préjuge pas de décisions futures justifiées) :

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

## Référence pédagogique et liberté de conception

Le repository Youssfi est une référence pédagogique et métier, pas un blueprint
ni une architecture cible obligatoire. La démarche est :

```text
comprendre l'idée métier et l'architecture observée
    ↓
analyser choix, avantages, limites, risques et compromis
    ↓
comparer avec des alternatives pertinentes
    ↓
concevoir et justifier NOTRE architecture
```

Notre architecture peut conserver, fusionner, ajouter, remplacer ou supprimer
des services et des frontends, et choisir des communications différentes, si
cela répond mieux aux exigences fonctionnelles et non fonctionnelles. Aucune
technologie ou topologie n'est retenue au seul motif qu'elle apparaît dans la
référence ou dans une roadmap.

Pour chaque élément important de la documentation, séparer :

1. **Architecture observée** — faits provenant de la référence, explicitement attribués.
2. **Analyse** — déductions, limites, risques, alternatives et compromis.
3. **Architecture cible de notre projet** — décisions prises par nous, ou point « à décider » lorsqu'aucune décision n'est encore validée.

Les faits, déductions, questions ouvertes et décisions doivent rester
identifiables et ne pas être confondus.

Le projet doit permettre à l'utilisateur de comprendre et défendre chaque décision technique.

La priorité n'est donc pas :

> produire rapidement beaucoup de code.

La priorité est :

> comprendre → spécifier → concevoir → implémenter → tester → expliquer.

ChatGPT agit comme mentor, architecte et reviewer.

GitHub Copilot agit comme assistant d'implémentation.

L'utilisateur reste responsable de la compréhension et de la validation de la solution.
