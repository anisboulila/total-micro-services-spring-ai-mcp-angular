# Baseline des exigences métier — Étape 1 — Requirements / Exigences

## Objectif et vocabulaire des niveaux de preuve

Cette spécification établit une baseline d’exigences candidates pour notre
projet construit de zéro. Elle s’appuie en partie sur une référence
pédagogique ; elle ne décrit pas le code applicatif local et ne définit pas
l’architecture cible. Le System Design cible appartient à une phase ultérieure
du projet et reste explicitement **hors périmètre de cette évolution**.

Chaque affirmation porte l’une des étiquettes suivantes :

- **OBSERVÉ** — directement étayé par la documentation identifiée du projet ou
  par le code source de la référence retenue.
- **DÉDUIT** — besoin candidat logique, inféré des capacités observées ou de
  l’objectif déclaré du projet.
- **DÉCIDÉ** — choix explicitement formulé pour notre projet par son
  propriétaire.
- **À CONFIRMER** — nécessite une validation métier ou du propriétaire du
  produit, ou des preuves actuellement manquantes.

Sauf indication contraire, les exigences candidates du produit déduites de la
référence sont **DÉDUITES / À CONFIRMER** et ne constituent pas un périmètre
approuvé.

## Requirement: Contexte métier et objectif du produit

Le projet est une **reconstruction à partir de zéro**, inspirée d’un exemple de
gestion de clients et de comptes bancaires. La référence sert à découvrir des
capacités métier possibles ; elle ne constitue ni une spécification ni une
implémentation à reproduire.

#### Scenario: Établir l’objectif du produit

- **WHEN** l’objectif du projet est décrit
- **THEN** il est formulé comme un objectif produit candidat visant à gérer
  des informations sur les clients et les comptes bancaires, et à rendre les
  informations pertinentes accessibles aux utilisateurs
- **AND** cette proposition porte le statut **DÉDUIT / À CONFIRMER**, car la
  référence démontre ces données et opérations, mais aucun propriétaire du
  produit n’a validé le produit visé ni son public
- **AND** l’objectif pédagogique de construire de zéro un projet réaliste dans
  un contexte de production tout en restant gérable à des fins pédagogiques
  porte le statut **DÉCIDÉ**, conformément à la demande explicite du propriétaire
- **AND** le dépôt Youssfi n’est considéré ni comme l’implémentation de notre
  produit ni comme une source d’exigences contraignante

## Requirement: Acteurs et parties externes

La baseline des exigences DOIT distinguer les acteurs directement attestés par
la référence des acteurs candidats pour le système cible.

#### Scenario: Identifier les acteurs

- **WHEN** les acteurs sont documentés
- **THEN** une personne interagissant par navigateur est identifiée comme
  **DÉDUIT / À CONFIRMER** ; les sources Angular attestent des interactions
  par navigateur, mais pas du rôle métier de l’utilisateur
- **AND** une personne envoyant des messages par Telegram ou Discord est
  identifiée comme **OBSERVÉE en tant qu’interaction avec un canal** et
  **À CONFIRMER en tant qu’acteur du produit cible**
- **AND** aucun rôle organisationnel de client, d’employé ou d’opérateur
  bancaire, d’administrateur ou autre n’est affirmé comme un fait ; leur
  existence et leurs autorisations sont **À CONFIRMER**
- **AND** OpenAI, Telegram et Discord sont recensés uniquement comme parties
  externes utilisées par la référence ; toute intégration cible est
  **À CONFIRMER**

## Requirement: Périmètre et cas d’usage

La baseline DOIT distinguer les capacités démontrées par la référence, le
périmètre candidat déduit de ces capacités et les décisions qui restent à
prendre.

#### Scenario: Définir les cas d’usage candidats

- **WHEN** les cas d’usage sont répertoriés
- **THEN** les cas d’usage candidats comprennent :
  - **UC-01 — Consulter les clients :** demander et consulter une liste de clients.
  - **UC-02 — Consulter le détail d’un client :** rechercher un client par identifiant.
  - **UC-03 — Créer un client :** soumettre le nom et l’adresse e-mail d’un client.
  - **UC-04 — Consulter les comptes :** demander et consulter une liste de comptes bancaires.
  - **UC-05 — Consulter le détail d’un compte :** rechercher un compte par identifiant.
  - **UC-06 — Créer un compte :** soumettre le type de compte, le solde et la référence client, sous réserve de règles métier restant à définir.
  - **UC-07 — Poser une question sur les clients ou les comptes :** soumettre une question en langage naturel par un canal conversationnel et recevoir une réponse, si cette capacité est retenue dans le périmètre du produit.
- **AND** les opérations décrites par UC-01 à UC-06 sont **OBSERVÉES** dans
  les contrôleurs et services Customer/EBank de la référence; UC-07 est
  **OBSERVÉ** comme interaction conversationnelle configurée, sans preuve
  qu’une requête donnée utilise les outils métier ou fournisse une réponse
  exacte
- **AND** chaque cas d’usage peut être relié au contrôleur et au service Customer ou EBank correspondants dans la référence
- **AND** poser des questions sur les clients ou les comptes bancaires au moyen d’une interaction conversationnelle est un cas d’usage candidat déduit du Bot, de Spring AI et des outils métier disponibles
- **AND** le besoin associé à chaque cas d’usage candidat, les acteurs autorisés, les règles métier et son inclusion dans notre produit restent **À CONFIRMER**
- **AND** les virements, paiements, relevés, calculs d’intérêts, devises, gestion des cartes et autres fonctions bancaires ne sont pas déduits de la référence et restent hors périmètre, sauf confirmation distincte

## Requirement: Exigences fonctionnelles

Les exigences fonctionnelles candidates DOIVENT exprimer des résultats métier
visibles par l’utilisateur ou le système, sans prescrire d’architecture ni de
technologie.

#### Scenario: Informations sur les clients

- **WHEN** les capacités de gestion des informations client sont évaluées
- **THEN** la baseline consigne comme **OBSERVÉ** dans la référence que celle-ci permet de lister les
  clients, d’en rechercher un par identifiant et d’en créer un avec les champs
  `name` et `email`
- **AND** elle consigne les champs de l’entité de référence : `id`, `name` et
  `email`
- **AND** toute exigence cible de consultation, création, mise à jour ou
  suppression d’informations client est **DÉDUITE / À CONFIRMER**, jusqu’à ce
  que le propriétaire du produit valide le périmètre, les validations et les
  règles de cycle de vie

#### Scenario: Informations sur les comptes bancaires

- **WHEN** les capacités relatives aux comptes sont évaluées
- **THEN** la baseline consigne comme **OBSERVÉ** dans la référence que celle-ci permet de lister
  les comptes, d’en rechercher un par identifiant et d’en créer un
- **AND** elle consigne les champs du compte de référence : `id`, `createdAt`,
  `balance`, `type` et `customerId`
- **AND** aucun cas d’usage de virement, retrait, dépôt, paiement ou historique
  des transactions n’est démontré dans le comportement REST ou de compte
  examiné dans la référence
- **AND** les règles applicables aux comptes cibles, les types de compte
  autorisés, la signification du solde, l’éligibilité du client, la propriété
  des comptes et la nécessité de fonctions de création ou de consultation
  restent **À CONFIRMER**

#### Scenario: Accès conversationnel aux informations

- **WHEN** les capacités conversationnelles sont évaluées
- **THEN** la baseline consigne comme **OBSERVÉ** que le Bot de référence accepte une requête
  textuelle par le chat Web et peut recevoir des messages de Telegram et Discord
- **AND** la référence configure l’accès à des outils portant sur les clients
  et les comptes, sans démontrer que chaque requête appelle un outil ni que
  les réponses font autorité
- **AND** la nécessité d’un accès conversationnel dans notre produit, les
  canaux à prendre en charge, les données pouvant être divulguées et la
  possibilité de modifier des données métier sont **DÉDUITES / À CONFIRMER**

#### Scenario: Le périmètre candidat n’est pas approuvé implicitement

- **WHEN** l’inventaire des capacités de référence sert à formuler les
  exigences cibles
- **THEN** aucun endpoint ni aucune interface utilisateur observés ne sont
  automatiquement considérés comme faisant partie du périmètre produit approuvé
- **AND** aucun comportement inconnu de validation, de gestion des erreurs,
  de pagination, de mise à jour ou de suppression n’est inventé
- **AND** le périmètre de cette évolution se limite à la découverte des exigences,
  à la traçabilité des preuves, aux besoins candidats et aux questions destinées
  au propriétaire du produit
- **AND** l’architecture cible et l’implémentation restent hors du périmètre de
  cette évolution
- **AND** cette spécification des exigences ne choisit pas une future
  architecture système cible

## Requirement: Exigences non fonctionnelles

La baseline DOIT identifier les besoins candidats en matière de qualité et les
objectifs d’acceptation manquants, sans choisir de mécanisme d’implémentation ni
inventer de niveaux de service numériques.

#### Scenario: Besoins de qualité à examiner

- **WHEN** les besoins non fonctionnels sont consignés
- **THEN** les préoccupations candidates comprennent la protection des
  informations client et compte, l’exactitude de l’association client-compte
  et de la création de compte, une interaction compréhensible et utilisable,
  ainsi que des attentes de disponibilité et de temps de réponse adaptées aux
  utilisateurs visés
- **AND** ces préoccupations sont marquées **DÉDUIT / À CONFIRMER**, sauf
  validation explicite par le propriétaire
- **AND** aucun objectif numérique de latence, débit, disponibilité,
  rétablissement, conservation, accessibilité ou concurrence n’est inventé
- **AND** aucune technologie d’authentification, solution de persistance,
  protocole de communication, passerelle, pile d’observabilité, modèle de
  déploiement ou architecture d’IA particulière n’est prescrite

## Requirement: Contraintes et décisions explicites

La baseline des exigences DOIT consigner les contraintes du projet séparément
des détails d’implémentation observés dans la référence.

#### Scenario: Contraintes du projet

- **WHEN** les contraintes sont énoncées
- **THEN** le projet est identifié comme une reconstruction réalisée de zéro
  et la référence ne doit être ni copiée ni intégrée (**DÉCIDÉ**)
- **AND** le produit/système doit être assez réaliste pour permettre des
  discussions d’entreprise proches du contexte de production, tout en restant
  gérable à des fins pédagogiques (**DÉCIDÉ**)
- **AND** cette évolution n’établit aucun choix d’architecture
- **AND** le System Design ultérieur doit évaluer les exigences avant de
  sélectionner des approches concernant les frontières, les communications,
  la gestion des données, la sécurité, l’IA ou l’exploitation

## Requirement: Hypothèses et questions ouvertes

Les énoncés produit non validés DOIVENT rester visibles en tant qu’hypothèses
ou questions nécessitant confirmation.

#### Scenario: Consigner les questions produit non résolues

- **WHEN** les hypothèses et les questions sont examinées
- **THEN** les hypothèses provisoires suivantes sont répertoriées comme
  **DÉDUITES / À CONFIRMER**, et non comme des faits :
  - des personnes ont besoin d’accéder aux informations client et compte ;
  - des comptes sont associés aux fiches client au moyen d’une référence client ;
  - un accès conversationnel pourrait être utile pour consulter des
    informations client ou compte
- **AND** les questions suivantes sont explicitement marquées **À CONFIRMER** :
  valeur visée du produit et groupes d’utilisateurs ; cycle de vie pris en
  charge pour les clients et les comptes ; éligibilité client-compte et
  cardinalité ; types de compte, devise, signification du solde et sémantique
  des transactions ; fonctions requises de création, consultation, mise à jour
  et suppression ; valeur de la fonctionnalité conversationnelle et actions
  autorisées ; canaux et public du frontend ; obligations d’autorisation et de
  confidentialité ; exactitude, conservation et suppression des données ;
  objectifs attendus de disponibilité, performance, accessibilité et
  rétablissement
- **AND** les hypothèses ne sont pas présentées comme des faits validés par le
  propriétaire du produit
- **AND** les options d’architecture sont reportées à une activité ultérieure
  de System Design
