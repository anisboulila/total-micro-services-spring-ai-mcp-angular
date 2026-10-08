---

name: project-architecture
description: Architecture, System Design and pedagogical rules for the Java/Spring/AI project
---------------------------------------------------------------------------------------------

# Project Architecture Skill

## 1. Project objective

This project is a long-running pedagogical project whose goal is to evolve a Senior Java / Tech Lead / Backend / DevOps profile toward Senior Java + AI Engineer.

System Design is the central thread of the project.

The objective is not simply to make the application work. The user must be able to understand and defend every important technical choice in an interview.

Before implementing a significant evolution, reason about:

* functional requirements;
* non-functional requirements;
* architecture;
* responsibilities and boundaries;
* data ownership;
* synchronous vs asynchronous communication;
* scalability;
* availability;
* resilience;
* security;
* performance;
* observability;
* deployment;
* alternatives and trade-offs.

## 2. Reference repository

The repository

https://github.com/mohamedYoussfi/totale-micro-services-spring-ai-mcp-angular

is a pedagogical reference.

IMPORTANT:

* It is NOT the local application.
* It must NOT be copied or cloned into the project.
* It must NOT be presented as local source code.
* Its architecture and implementation may be studied to understand concepts.
* Any fact coming from this repository must remain clearly attributable to the reference repository.
* Do not invent local implementation details from the reference repository.

The local project and the reference repository must always be treated as two distinct things.

## 3. Current architecture-first approach

The project progresses incrementally.

The expected workflow is:

requirements
→ specification
→ design
→ tasks
→ implementation
→ tests
→ review
→ documentation
→ commit

Do not jump directly to implementation when architecture or requirements are not sufficiently understood.

For important architectural decisions, explicitly identify:

* the problem;
* the chosen solution;
* relevant alternatives;
* advantages;
* disadvantages;
* trade-offs;
* consequences.

## 4. Keep the architecture simple

Prefer the simplest architecture that satisfies the requirements.

Do NOT introduce:

* unnecessary design patterns;
* unnecessary abstractions;
* artificial microservices;
* unnecessary libraries;
* technologies only because they are fashionable;
* excessive Kubernetes complexity;
* unnecessary layers or interfaces.

Every new component or technology must have a concrete reason to exist.

## 5. Technology progression

Technologies are introduced progressively.

The project may eventually cover:

* Java;
* Spring Boot;
* Spring Cloud;
* microservices;
* System Design;
* Spring Security;
* OAuth2/OIDC;
* Keycloak;
* Kafka;
* PostgreSQL;
* Redis;
* Spring AI;
* MCP;
* AI Agents;
* RAG;
* Docker;
* Kubernetes;
* Helm;
* CI/CD;
* observability.

Do not introduce a future technology into the current evolution unless it is explicitly part of the current requirements.

For example, Kafka must not be introduced into an architecture baseline merely because it is planned for a later phase.

## 6. SDD / OpenSpec discipline

Use the project's SDD/OpenSpec workflow.

Before implementation:

1. understand requirements;
2. validate specification;
3. validate design;
4. validate tasks.

During implementation:

1. implement one logical task at a time;
2. keep the implementation aligned with the specification;
3. write tests;
4. review the implementation;
5. update documentation;
6. update interview material;
7. propose a logical Git commit.

Do not silently change the architecture while implementing a task.

If implementation reveals an architectural problem, stop and explain it before introducing a significant architectural change.

## 7. Code quality and pedagogy

The user is an experienced Java developer but is learning some technologies progressively.

Code must therefore be:

* simple;
* readable;
* idiomatic;
* maintainable;
* appropriately commented.

When code is added or modified for a pedagogical task, use concise French comments where they genuinely help explain:

* an architectural choice;
* a non-obvious mechanism;
* a framework behavior;
* a System Design concept;
* a potential interview question.

Do not comment every obvious line.

Avoid over-engineering.

## 8. Tests

Tests are mandatory for implementation tasks.

Use the simplest appropriate test level:

* JUnit;
* Mockito;
* Spring Boot Test;
* integration tests;
* Testcontainers;
* API tests;
* Kafka tests when Kafka is introduced.

Do not create tests merely for coverage numbers.

Tests should verify meaningful behavior and important failure cases.

## 9. Project memory

`PROJECT-CONTEXT.md` is the persistent project memory.

Keep it synchronized with important project decisions.

It should reflect:

* project objective;
* architecture;
* technologies;
* working rules;
* important decisions;
* current state;
* phases;
* current task;
* next task;
* known problems;
* learning points;
* interview topics.

Do not replace valid existing information without reason.

## 10. Interview learning

`interview.md` is an important learning artifact.

For each major completed task, update it with relevant questions.

Prefer this structure:

### Question

Short interview question.

### Short answer

Concise answer suitable for an interview.

### Detailed answer

Technical explanation.

### Trick question

Possible interviewer follow-up or trap.

### Project example

How the concept applies to this project.

The goal is to make the user capable of explaining and defending the architecture, not to memorize definitions.

## 11. Working style with GitHub Copilot

GitHub Copilot is an implementation assistant.

It should NOT replace architectural reasoning.

ChatGPT / OpenSpec provides:

* requirements;
* specification;
* architecture;
* System Design;
* task decomposition;
* architectural review.

Copilot assists with:

* implementation;
* tests;
* refactoring;
* repetitive code;
* code explanations.

Before generating significant code, verify the relevant requirements, specification, design and task.

## 12. Change discipline

For every task:

1. identify the requirement;
2. identify the relevant specification;
3. identify the relevant design decision;
4. identify the files affected;
5. implement only the requested scope;
6. test;
7. review side effects;
8. update documentation;
9. update `interview.md`;
10. update `PROJECT-CONTEXT.md` when the project state changes;
11. propose a logical Git commit.

Do not modify unrelated files.

Do not add technologies outside the task scope.

## 13. Communication style

When explaining technical subjects to the user:

* be concise;
* be structured;
* avoid unnecessary filler;
* explain the "why" before the "how";
* distinguish facts from assumptions;
* explicitly identify uncertainty;
* use concrete project examples.

When an architectural decision is important, explain it in terms of System Design and interview relevance.

## 14. Current foundation evolution

The current foundation evolution is documentary.

It formalizes the architecture baseline based on:

* local project documents;
* exploration of the Youssfi reference repository.

At this stage:

* no application code should be created;
* no application runtime should be modified;
* no dependency should be added;
* the reference repository must not be integrated;
* architectural facts, deductions and uncertainties must remain distinguishable.

Future evolutions will introduce implementation progressively.
