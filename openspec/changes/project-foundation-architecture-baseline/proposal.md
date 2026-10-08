# Project Foundation & Architecture Baseline

## Why

Le workspace du projet contient actuellement `PROJECT-CONTEXT.md` et
`requirements.md`, mais aucun code applicatif permettant de vérifier localement
les composants qu'ils décrivent. Le dépôt Youssfi est accessible et sert de
référence pédagogique, mais son implémentation ne doit être ni copiée ni ajoutée
comme dépendance.

Avant toute évolution fonctionnelle, il faut établir une baseline de System
Design qui distingue les constats observés dans cette référence, les déductions
architecturales et les informations à confirmer dans le futur code du projet.
Cette baseline doit rendre les flux et les compromis défendables sans présenter
la référence comme l'implémentation locale.

## What Changes

- Formaliser, dans le change OpenSpec, les responsabilités, données, interfaces,
  dépendances et communications des composants identifiés par `requirements.md`.
- Décrire les flux frontend/Gateway, EBank/Customer et Bot/Spring AI/MCP à partir
  du code de référence consulté.
- Distinguer REST synchrone, Feign comme client REST déclaratif, MCP et streaming
  HTTP; identifier qu'aucune communication événementielle métier n'est observée.
- Documenter Eureka, Gateway, H2/JPA, Resilience4j et les deux applications
  Angular avec leurs limites observées.
- Présenter les choix, alternatives, compromis, risques et questions ouvertes
  dans une section System Design détaillée.
- Aligner `PROJECT-CONTEXT.md` sur la baseline vérifiée et alimenter
  `interview.md` pour cette évolution, sans convertir les constats de la
  référence en faits locaux.
- Conserver une portée documentaire : aucun code, aucune modification runtime,
  aucune intégration/copie du dépôt de référence et aucune nouvelle technologie.

## Capabilities

### New Capabilities

- `architecture-baseline`: formalisation de l'architecture observée, de ses flux
  et de son System Design avec traçabilité entre faits, déductions et points à
  confirmer.

### Modified Capabilities

- Aucune.

## Impact

- Ajout des artefacts OpenSpec de ce change; les tâches de réalisation prévoient
  aussi les mises à jour documentaires demandées par `requirements.md`.
- Sources d'analyse : `PROJECT-CONTEXT.md`, `requirements.md` et le dépôt de
  référence Youssfi observé sur `main` au commit
  `bf4c7f2750e4c731662e8138023fd8e6b4e4a475`.
- Le workspace local ne contient pas les sources des services, les POMs Maven
  ou les projets Angular. Les observations d'implémentation sont donc attribuées
  explicitement au dépôt de référence; elles ne sont pas qualifiées de faits
  locaux.
- Aucun changement de comportement applicatif et aucun test runtime ne sont
  attendus pour cette évolution documentaire.
- Aucune technologie hors périmètre n'est introduite.
