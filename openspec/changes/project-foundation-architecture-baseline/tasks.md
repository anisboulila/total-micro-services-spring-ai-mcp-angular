# Tasks — Project Foundation & Architecture Baseline

> Règle de conception applicable à toutes les tâches de ce change : décrire
> séparément l'architecture observée dans la référence, son analyse, et
> l'architecture cible de notre projet. La référence n'impose ni ses services,
> ni ses frontends, ni ses protocoles; toute décision cible doit être justifiée
> par les exigences et les critères System Design. Cette clarification ne marque
> aucune tâche 2.x à 5.x comme exécutée.

## 1. Établir les sources et niveaux de preuve

- [x] 1.1 Confirmer le périmètre local disponible et consigner l'absence de code applicatif, de POMs Maven et de projets Angular dans le workspace analysé.
- [x] 1.2 Identifier la révision du dépôt Youssfi utilisée comme référence et conserver l'attribution des constats à cette référence pédagogique.
- [x] 1.3 Pour chaque affirmation d'architecture, vérifier qu'elle est marquée comme fait vérifié, déduction ou point à confirmer.
- [x] 1.4 Établir la règle durable selon laquelle le dépôt Youssfi est une référence pédagogique et métier, non un blueprint de l'architecture cible.

## 2. Formaliser l'architecture et les flux

- [x] 2.1 Documenter les responsabilités, technologies, dépendances, données et communications des composants observés dans la référence, puis distinguer leur pertinence éventuelle pour notre cible sans présumer de leur conservation.
- [x] 2.2 Recenser les routes REST observées et décrire les appels frontend passant par le Gateway.
- [ ] 2.3 Décrire Eureka et le discovery locator du Gateway, en séparant configuration observée et comportement runtime non vérifié.
- [ ] 2.4 Décrire EBank → Feign → REST Customer, incluant le circuit breaker, son fallback et le risque associé à la création de comptes.
- [ ] 2.5 Décrire les outils MCP, le bot Spring AI, le LLM et les entrées Telegram/Discord; distinguer destinations MCP statiques et découverte Eureka.
- [ ] 2.6 Comparer les frontends et consigner leurs écarts observés ainsi que leur rôle encore indéterminé.
- [ ] 2.7 Expliquer la distinction REST synchrone, Feign, MCP et streaming HTTP sans présenter d'événementiel métier comme existant.

## 3. Finaliser le System Design documentaire

- [ ] 3.1 Vérifier que `design.md` couvre frontières, propriété des données, couplages, disponibilité, résilience, performance, scalabilité, sécurité, observabilité et fragilités.
- [ ] 3.2 Documenter les choix observés, problèmes adressés, alternatives et compromis dans le périmètre de `requirements.md`.
- [ ] 3.3 Maintenir explicitement les questions non résolues comme points à confirmer, sans supposition ni implémentation.
- [ ] 3.4 Vérifier qu'aucune technologie hors périmètre, dépendance ou intégration du dépôt Youssfi n'est introduite.

## 4. Cohérence documentaire du projet

- [ ] 4.1 Mettre à jour `PROJECT-CONTEXT.md` uniquement pour refléter la baseline et son niveau de preuve, sans présenter le code de référence comme code local.
- [ ] 4.2 Alimenter `interview.md` avec les décisions de cette évolution, si le fichier est créé ou mis à jour dans le cadre de l'évolution documentaire.
- [ ] 4.3 Vérifier l'alignement entre proposition, spécification, design, tâches, `requirements.md` et les critères d'acceptation REQ-001 à REQ-014.

## 5. Revue de clôture

- [ ] 5.1 Confirmer que les changements se limitent aux artefacts/documentation de baseline et qu'aucun code ou runtime n'a été modifié.
- [ ] 5.2 Vérifier que chaque point à confirmer demeure non résolu tant que les sources locales ou une vérification runtime ne sont pas disponibles.
- [ ] 5.3 Effectuer une revue documentaire finale; aucun test runtime n'est attendu en l'absence de changement applicatif.
