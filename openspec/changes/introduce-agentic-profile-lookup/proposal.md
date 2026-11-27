# Proposal

## Why

Le backlog conserve US-08 (« À quel client appartient ce compte ? »), Spring AI, MCP et l’outil Customer en lecture seule. Cependant, commencer directement par une chaîne Assistant → Account → Customer mélangerait trop de notions nouvelles et rendrait la compréhension, le diagnostic et la démonstration difficiles.

Cette évolution transforme cette partie en apprentissage incrémental : Spring AI d’abord, puis un outil Java local, ensuite MCP, puis l’appel Customer sécurisé. Le besoin métier reste la consultation autorisée du profil courant selon US-03, préparant US-08 sans remplacer ni redéfinir les User Stories existantes.

## What Changes

- Introduire progressivement `assistant-service`, d’abord sans outil métier, pour comprendre le `ChatClient`, les prompts, les erreurs de modèle et la configuration d’un modèle local.
- Ajouter ensuite un outil Java simple, déterministe et non exposé au métier, afin d’observer et tester le cycle de Tool Calling avant d’introduire un protocole réseau.
- Introduire MCP avec le même principe d’outil, en comparant explicitement l’appel Java local, la découverte de capacités et la frontière client/serveur MCP.
- Remplacer le démonstrateur MCP par l’outil final `getMyCustomerProfile`, exposé par un `customer-mcp-server` interne et sans argument d’identité métier.
- Réutiliser l’API Customer existante comme source de vérité, sans accès direct à PostgreSQL depuis l’assistant ou MCP.
- Ajouter progressivement la validation JWT, les autorités, la propagation contrôlée de l’identité, le filtrage opérateur/client et une route Gateway explicite.
- Maintenir les contrôles indispensables : lecture seule, allow-list, aucun identifiant issu du prompt, validation structurée, réponse neutre, timeout, limite d’appels et absence de tokens/données dans les logs.
- Conserver les tests essentiels : sélection d’outil, injection, écriture interdite, accès croisé, absence de profil, dépendance indisponible et résultat non fiable.
- Décomposer l’ancienne tâche `7.1` du backlog en paliers : première consultation Customer, puis préparation d’US-08 avec Account. Les exigences US-08 et US-09 ne sont pas modifiées.
- Reporter hors de cette première démonstration les mécanismes avancés : token exchange opérationnel, circuit breaker, évaluation LLM statistique, métriques de coût et observabilité distribuée.

Aucune décision historique n’est annulée. Les mécanismes reportés restent des sujets de durcissement ou d’évolution ultérieure et ne réduisent ni les contrôles d’accès ni les exigences de lecture seule.

## Capabilities

### New Capabilities

- `assisted-profile-lookup`: consultation conversationnelle de son propre profil Customer, construite progressivement avec Spring AI, Tool Calling et MCP tout en conservant l’autorisation dans `customer-service`.

### Modified Capabilities

Aucune. US-03 garde son comportement REST. US-08 reste le parcours métier cible « propriétaire d’un compte » et US-09 conserve ses exigences d’historique. Cette change ajoute une modalité technique et un incrément de préparation, pas une nouvelle règle métier.

## Impact

- Ajoute progressivement `assistant-service` et `customer-mcp-server` au build Maven.
- Ajoute Spring AI et les dépendances MCP nécessaires, avec configuration locale remplaçable.
- Réutilise la Gateway, l’OIDC/Keycloak, l’API `GET /api/v1/customers` et le filtrage par `sub` existants.
- Ne modifie pas immédiatement le backlog historique ; ses artefacts restent la référence et la tâche future `7.1` devra dépendre de cet incrément.
- Ne réalise pas Account Service, US-08, US-09, Agent autonome, RAG, Flutter ou écriture métier.
- Les mécanismes reportés seront explicitement repris dans une change ultérieure avant tout déploiement de production : token exchange, résilience avancée, évaluations non déterministes, FinOps et traçage distribué complet.
