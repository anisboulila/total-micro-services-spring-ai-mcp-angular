# Spec Delta

## Purpose

Définir une modalité conversationnelle protégée permettant à un utilisateur de consulter son propre profil Customer sans modifier les règles métier, l’identité ni les autorisations du parcours REST existant.

## ADDED Requirements

### Requirement: Consultation conversationnelle du profil courant
The system MUST let an authenticated customer request their own Customer profile through a conversational endpoint and MUST derive the target exclusively from the validated identity.

#### Scenario: Profil courant trouvé
- **GIVEN** un client authentifié possède une fiche Customer liée à son `sub` vérifié
- **WHEN** il demande son profil en langage naturel
- **THEN** l’assistant restitue uniquement les champs du profil autorisés provenant du DTO métier validé
- **AND** aucun identifiant de profil ou attribut métier fourni dans le texte n’est utilisé pour choisir la ressource ou construire la réponse

#### Scenario: Aucun profil courant
- **GIVEN** un client authentifié ne possède pas encore de fiche Customer
- **WHEN** il demande son profil
- **THEN** l’assistant répond qu’aucun profil n’est disponible pour son identité
- **AND** il n’invente ni identifiant ni attribut de profil

#### Scenario: Tentative de consultation d’un autre profil
- **GIVEN** un client authentifié cite l’identité ou l’identifiant d’une autre personne
- **WHEN** il demande le profil de cette personne ou tente de contourner la sélection du sujet courant
- **THEN** le système ignore l’identité fournie comme preuve d’accès
- **AND** il ne retourne que la fiche du sujet authentifié ou une réponse neutre

### Requirement: Outil de lecture borné
The system MUST expose to the assistant a read-only `getMyCustomerProfile` capability that requires no business identifier and delegates authorization to the Customer service.

#### Scenario: Lecture autorisée
- **GIVEN** une question relevant de la consultation du profil courant
- **WHEN** l’assistant doit obtenir les données métier
- **THEN** il peut appeler `getMyCustomerProfile` au plus une fois pour ce parcours
- **AND** l’outil appelle l’API métier Customer avec l’identité validée
- **AND** aucune reprise métier automatique n’est effectuée après l’échec

#### Scenario: Demande d’écriture
- **GIVEN** une instruction demande de créer, modifier ou supprimer un profil
- **WHEN** l’assistant analyse la demande
- **THEN** il refuse cette action
- **AND** aucun outil d’écriture n’est proposé ni appelé

#### Scenario: Outil non autorisé
- **GIVEN** un modèle tente d’appeler une capacité absente de l’allow-list
- **WHEN** l’orchestrateur reçoit la demande d’outil
- **THEN** l’appel est bloqué avant d’atteindre un service métier

### Requirement: Contrôle de l’identité au travers de MCP
The system MUST preserve the authenticated subject across the assistant and internal MCP adapter while allowing every receiving service to validate the token, issuer, lifetime, audience and authorities.

#### Scenario: Identité valide propagée
- **GIVEN** un JWT valide avec le sujet, l’audience et les autorités attendus
- **WHEN** la requête traverse l’assistant et l’adaptateur MCP
- **THEN** Customer reçoit une identité correspondant au sujet authentifié
- **AND** la ressource est filtrée et autorisée par Customer

#### Scenario: Identité absente ou invalide
- **GIVEN** le JWT est absent, expiré, mal signé ou destiné à une audience incorrecte
- **WHEN** une requête d’assistant est reçue
- **THEN** le système refuse l’appel avant toute lecture métier
- **AND** aucun outil n’est exécuté

#### Scenario: Rôle non admis pour le parcours initial
- **GIVEN** le JWT porte un rôle opérateur ou une combinaison incluant un rôle opérateur
- **WHEN** le client tente le parcours conversationnel « mon profil »
- **THEN** l’accès au parcours assistant est refusé
- **AND** aucune liste globale de profils n’est demandée

#### Scenario: Secret absent des journaux et prompts
- **GIVEN** une requête authentifiée traverse les composants agentiques
- **WHEN** des événements techniques ou des appels modèle sont produits
- **THEN** ni bearer token, secret ni matériel de signature n’est inclus dans le prompt ou les journaux

### Requirement: Résultat métier validé et source unique des faits
The system MUST validate the MCP tool result against the approved profile DTO and MUST construct every presented profile attribute exclusively from that validated DTO.

#### Scenario: Résultat conforme
- **GIVEN** Customer renvoie une fiche autorisée conforme au DTO
- **WHEN** l’orchestrateur valide le résultat
- **THEN** la réponse conversationnelle présente uniquement les champs autorisés issus du DTO validé

#### Scenario: Résultat incomplet ou contradictoire
- **GIVEN** un résultat d’outil ne respecte pas le DTO ou contient zéro référence attendue de façon ambiguë, plusieurs profils, un champ inconnu ou une valeur contradictoire
- **WHEN** l’orchestrateur reçoit le résultat
- **THEN** aucune fiche n’est présentée comme certaine
- **AND** une réponse d’indisponibilité contrôlée est retournée

#### Scenario: Texte du modèle non fiable
- **GIVEN** le modèle propose un identifiant, un nom, un e-mail ou tout autre attribut absent du DTO validé
- **WHEN** la réponse destinée à l’utilisateur est construite
- **THEN** l’attribut proposé est ignoré et ne peut pas devenir un fait métier
- **AND** la réponse est construite depuis les données validées ou remplacée par une réponse contrôlée

### Requirement: Défaillances neutres, bornées et sans retry métier
The system MUST return controlled responses for missing data, denied access, unavailable dependencies or model failure, respect explicit timeouts and MUST NOT automatically retry a failed business operation.

#### Scenario: Ressource absente ou inaccessible
- **GIVEN** Customer retourne une absence ou masque un accès refusé
- **WHEN** le parcours est traité
- **THEN** l’utilisateur reçoit la même réponse neutre de non-disponibilité du profil

#### Scenario: Dépendance indisponible
- **GIVEN** le modèle, MCP ou Customer est indisponible ou dépasse son timeout explicite
- **WHEN** le parcours échoue
- **THEN** l’assistant retourne un message contrôlé indiquant que le service ne peut pas être utilisé pour le moment
- **AND** aucun profil n’est fabriqué
- **AND** l’opération métier n’est pas automatiquement rejouée

#### Scenario: Limite d’appel respectée
- **GIVEN** le modèle tente d’appeler plusieurs fois l’outil ou d’enchaîner des capacités supplémentaires
- **WHEN** la limite d’un appel outil est atteinte
- **THEN** le parcours s’arrête avec une réponse contrôlée
- **AND** aucun appel supplémentaire ne franchit l’orchestrateur

### Requirement: Corrélation et diagnostic technique minimisés
The system MUST propagate a correlation identifier and MUST produce technical diagnostic events without logging sensitive prompts, profile values or access tokens.

#### Scenario: Corrélation de la requête
- **GIVEN** une requête reçoit un identifiant de corrélation
- **WHEN** elle traverse Gateway, assistant, MCP et Customer
- **THEN** les événements techniques peuvent être reliés par cet identifiant
- **AND** les événements indiquent au minimum le résultat technique et la durée locale utile au diagnostic

#### Scenario: Journalisation sans données sensibles
- **GIVEN** un parcours réussit ou échoue
- **WHEN** des logs techniques sont écrits
- **THEN** ils ne contiennent ni JWT, prompt complet, `sub`, email, nom ni autre valeur de profil

#### Scenario: Tests déterministes du choix d’outil
- **GIVEN** une suite de tests fournit des réponses de modèle reproductibles
- **WHEN** les demandes de profil, d’écriture, de profil tiers, d’outil interdit et de contournement sont évaluées
- **THEN** la sélection ou le blocage attendu de l’outil est vérifié sans LLM externe ni évaluation statistique
