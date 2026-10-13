# Spec Delta

## Purpose

Définit la réponse assistée, strictement en lecture seule, à la question du
propriétaire d’un compte ainsi que l’accès protégé à l’historique de conversation.

## ADDED Requirements

### Requirement: US-08 — Identifier le client propriétaire d’un compte

The system MUST answer the approved account-owner question using only authorized Customer and Account reads.

#### Scenario: Client demande le propriétaire de son compte
- **GIVEN** un client authentifié demande « À quel client appartient ce compte ? » avec un compte qui lui appartient
- **WHEN** l’assistant consulte les services Account puis Customer au moyen d’outils MCP en lecture seule
- **THEN** il restitue uniquement l’identifiant du compte et le nom du client
- **AND** les services propriétaires valident l’identité et l’autorisation

#### Scenario: Opérateur demande le propriétaire d’un compte
- **GIVEN** un opérateur/admin authentifié demande le propriétaire d’un compte existant
- **WHEN** les lectures autorisées sont effectuées par les services propriétaires
- **THEN** l’assistant restitue uniquement l’identifiant du compte et le nom du client

#### Scenario: Compte absent ou inaccessible
- **GIVEN** un compte inexistant ou non autorisé pour le client
- **WHEN** l’API Account retourne une absence ou un accès masqué
- **THEN** l’assistant répond « compte introuvable ou indisponible »
- **AND** il ne révèle pas la cause de l’absence et n’invente pas de propriétaire

#### Scenario: LLM ou API métier indisponible
- **GIVEN** l’assistant ne peut pas obtenir une réponse fiable d’un service métier ou du LLM local
- **WHEN** la requête est traitée
- **THEN** l’utilisateur reçoit un message contrôlé indiquant l’indisponibilité et peut réessayer
- **AND** aucune réponse d’association n’est fabriquée

#### Scenario: Lecture seule et modèle local
- **GIVEN** l’assistant traite une question UC-07
- **WHEN** il choisit ses outils et son modèle
- **THEN** les outils MCP ne réalisent que des lectures via les API métier
- **AND** aucun accès direct à une base métier, écriture, Agent, RAG ou fournisseur LLM distant n’est utilisé

### Requirement: US-09 — Consulter l’historique de conversation autorisé

The system MUST persist and expose conversation history only to its owner and authorized operators/admins for the owner-approved retention period.

#### Scenario: Persistance d’un échange
- **GIVEN** une question et une réponse de l’assistant sont terminées
- **WHEN** la conversation est enregistrée
- **THEN** le texte complet de la question et de la réponse est conservé
- **AND** aucun jeton d’accès ni secret n’est conservé

#### Scenario: Client consulte son historique
- **GIVEN** un client authentifié et ses conversations persistées
- **WHEN** il ouvre l’écran d’historique
- **THEN** il ne voit que ses propres conversations et leurs échanges

#### Scenario: Opérateur consulte les historiques
- **GIVEN** un opérateur/admin authentifié
- **WHEN** il ouvre l’écran d’historique
- **THEN** il peut consulter les conversations de tous les utilisateurs

#### Scenario: Rétention et purge
- **GIVEN** une conversation atteint la rétention configurable d’un mois
- **WHEN** la purge automatique s’exécute
- **THEN** la conversation est supprimée
- **AND** aucune action de suppression manuelle n’est proposée en v1
