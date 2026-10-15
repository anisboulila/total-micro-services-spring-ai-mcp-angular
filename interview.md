# Entretien — Project Foundation & Architecture Baseline

> Exemples ci-dessous issus uniquement de la référence pédagogique Youssfi
> (`main`, commit `bf4c7f2750e4c731662e8138023fd8e6b4e4a475`). Ils ne décrivent
> pas de code local ni des décisions déjà prises pour notre architecture cible.

## Pourquoi ne pas reprendre directement l'architecture de référence?

**Réponse courte**  
Une architecture de référence est une source d'apprentissage, pas une
architecture cible obligatoire. Chaque frontière et technologie doit répondre
à nos exigences et à un besoin démontré.

**Explication**  
Il faut d'abord distinguer les faits observés, leur analyse et les décisions
cibles. Garder, fusionner, remplacer ou supprimer un composant peut être
pertinent selon les besoins, la simplicité, l'exploitation et l'évolution
attendue.

**Piège éventuel**  
Confondre une architecture pédagogique fonctionnelle ou intéressante avec une
architecture automatiquement adaptée à notre contexte.

**Exemple provenant de la référence**  
Deux frontends Angular proches sont présents, mais le rôle distinct de chacun
n'est pas établi; leur nombre cible reste donc une question.

## À quoi sert un API Gateway et quel est son compromis?

**Réponse courte**  
Il peut fournir une entrée commune et acheminer les requêtes vers des services,
au prix d'un composant et d'un saut réseau supplémentaires.

**Explication**  
Il faut justifier les responsabilités centralisées et mesurer leur valeur face
aux routes directes ou à des routes explicites. Le Gateway peut devenir une
dépendance importante; sa présence seule ne prouve ni sécurité ni disponibilité.

**Piège éventuel**  
Présumer que CORS authentifie les clients, ou que les routes déclarées
fonctionnent sans validation runtime.

**Exemple provenant de la référence**  
Les frontends utilisent `localhost:9999` avec les préfixes
`EBANK-SERVICE`/`EBANK-BOT`; le Gateway déclare un
`DiscoveryClientRouteDefinitionLocator`, mais le routage effectif n'a pas été
testé.

## Quel est le rôle d'Eureka dans cette référence?

**Réponse courte**  
Eureka fournit un registre de services et la configuration observée permet aux
clients d'utiliser la découverte; l'enregistrement et la résolution réels ne
sont pas vérifiés.

**Explication**  
La découverte dynamique évite de dépendre uniquement d'adresses d'instances
fixes, mais ajoute une dépendance opérationnelle. Son intérêt dépend de la
topologie et du mode d'exécution.

**Piège éventuel**  
Déduire d'une dépendance Eureka Client que tous les appels ou toutes les URL
utilisent Eureka.

**Exemple provenant de la référence**  
Le Bot déclare Eureka Client, mais ses connexions MCP sont configurées avec des
URL `localhost` fixes.

## Feign est-il un protocole différent de REST?

**Réponse courte**  
Non. Feign est un client déclaratif; dans cet exemple, il appelle Customer via
HTTP/REST.

**Explication**  
Le code appelant attend le résultat de `GET /customers/{id}` avant de
poursuivre. Cette communication est donc synchrone et conserve un couplage de
disponibilité et de latence.

**Piège éventuel**  
Présenter Feign comme un protocole réseau ou croire qu'il élimine les erreurs
réseau.

**Exemple provenant de la référence**  
`CustomerRestClient` est annoté `@FeignClient(name = "customer-service")` et
délègue à `GET /customers/{id}`.

## Quel risque le fallback Resilience4j présente-t-il ici?

**Réponse courte**  
Le fallback renvoie un Customer synthétique; le chemin de sauvegarde ne vérifie
pas ses valeurs avant de persister le compte.

**Explication**  
Un fallback peut préserver une réponse technique tout en masquant un échec
métier. L'effet réel dépend du déclenchement du circuit et n'a pas été testé;
la règle métier attendue doit être clarifiée.

**Piège éventuel**  
Assimiler « un fallback existe » à une garantie de résilience correcte ou à une
validation métier réussie.

**Exemple provenant de la référence**  
Le fallback renvoie `"Not available"` pour le nom et l'email; `EbankService.save`
ne contrôle pas ces champs avant `accountRepository.save`.

## Quelle est la différence entre REST, MCP et HTTP streaming?

**Réponse courte**  
REST expose des opérations HTTP, MCP expose des capacités au client MCP, et le
streaming HTTP fournit progressivement la réponse d'une requête HTTP.

**Explication**  
Feign est une manière de faire un appel REST. Le transport MCP configuré
`streamable` n'est pas un événement métier. Un flux de réponse HTTP ne constitue
pas un système de messagerie.

**Piège éventuel**  
Confondre `Flux<String>`, transport MCP streamable et Kafka.

**Exemple provenant de la référence**  
`/chatStream` retourne `Flux<String>`; aucun broker ni flux Kafka métier n'a été
observé dans les sources inspectées.

## Que permet MCP dans le parcours AI observé, et quelles limites restent?

**Réponse courte**  
Le Bot utilise Spring AI `ChatClient` avec un `ToolCallbackProvider` pour rendre
des outils MCP disponibles au modèle configuré.

**Explication**  
Customer et EBank exposent des méthodes métier annotées `@McpTool`. Le Bot
configure OpenAI `gpt-4o` et des URL MCP fixes. Les sources ne démontrent pas
qu'une requête donnée choisit un outil, ni les contrôles d'accès, timeouts ou la
persistance de la mémoire.

**Piège éventuel**  
Décrire chaque échange comme un agent autonome avancé ou affirmer que MCP
utilise Eureka.

**Exemple provenant de la référence**  
Les destinations du client MCP sont `http://localhost:8056/mcp` et
`http://localhost:8057/mcp`, tandis que le Bot déclare séparément Eureka Client.

## Quelles conclusions tirer sur la sécurité et l'observabilité?

**Réponse courte**  
Les dépendances visibles ne suffisent pas à établir les contrôles ou capacités
effectivement actifs; il faut distinguer les indices du code et les garanties
runtime.

**Explication**  
L'absence de Spring Security dans les POMs consultés et le CORS wildcard sont
des constats limités à la référence analysée. Actuator est déclaré, mais cela
ne prouve pas quels endpoints sont exposés ni quelles métriques sont collectées.

**Piège éventuel**  
Conclure qu'aucune protection externe n'existe ou qu'Actuator implique une
observabilité de bout en bout.

**Exemple provenant de la référence**  
Les outils MCP incluent des opérations de création; leur protection effective
et l'exposition des endpoints de gestion restent à confirmer.

## Entretien — Baseline des exigences métier

> Cette section traite uniquement de l’Étape 1 — Requirements / Exigences.
> Elle ne valide pas le périmètre produit et ne prend aucune décision de
> System Design.

## Comment transformer une référence technique en exigences produit?

**Réponse courte**
Séparer les capacités observées dans la référence des besoins déduits et faire
confirmer le périmètre par le propriétaire du produit.

**Explication**
Un endpoint ou une interface prouve qu’une capacité existe dans le code de
référence; cela ne prouve ni que notre produit en a besoin, ni qui est autorisé
à l’utiliser, ni quelles règles métier doivent s’appliquer.

**Piège éventuel**
Traiter la référence pédagogique comme une spécification validée ou recopier
automatiquement ses fonctionnalités.

**Exemple provenant de la référence Youssfi**
Customer permet de lister, rechercher et créer des clients. Ces capacités sont
des preuves pour UC-01 à UC-03, mais leur inclusion dans notre produit et les
règles de création restent à confirmer.

## À quoi servent les niveaux OBSERVÉ, DÉDUIT, DÉCIDÉ et À CONFIRMER?

**Réponse courte**
Ils rendent visible la provenance d’une affirmation et empêchent de confondre
un fait, un besoin candidat, un choix explicite et une question non résolue.

**Explication**
Une capacité directement présente dans le code est **OBSERVÉE**. Un besoin
logiquement inféré est **DÉDUIT**; il peut rester **À CONFIRMER** tant qu’un
responsable métier ne l’a pas validé. **DÉCIDÉ** est réservé aux choix
explicitement formulés pour notre projet.

**Piège éventuel**
Présenter une déduction plausible comme une exigence approuvée ou une décision
du propriétaire.

**Exemple provenant de la référence Youssfi**
Le Bot accepte du texte et est configuré avec des outils client/compte; le
besoin d’un accès conversationnel dans notre produit est seulement candidat et
reste à confirmer.

## Pourquoi recenser les questions ouvertes dans une baseline?

**Réponse courte**
Pour rendre explicites les informations nécessaires à la validation produit
et éviter de combler les lacunes par des hypothèses cachées.

**Explication**
Les critères d’acceptation et les règles métier doivent être assez précis pour
être révisés. En leur absence, il faut consigner l’incertitude, sans fabriquer
de seuils de qualité ni anticiper les solutions techniques.

**Piège éventuel**
Inventer des règles ou des SLO numériques, ou traiter l’absence de réponse
comme un accord implicite.

**Exemple provenant de la référence Youssfi**
Le compte contient `balance`, `type` et `customerId`, mais ces champs ne
déterminent pas à eux seuls la devise, la signification du solde, l’éligibilité
du client ou les règles de création attendues pour notre produit.

## Pourquoi les exigences ne définissent-elles pas déjà le System Design?

**Réponse courte**
Les exigences décrivent les besoins et résultats attendus; le System Design
évalue ensuite des moyens de les satisfaire.

**Explication**
La baseline peut exprimer des besoins de confidentialité, de disponibilité ou
de temps de réponse sans choisir une architecture, une technologie ni un
mécanisme d’implémentation. Le System Design cible est une phase ultérieure,
hors périmètre de cette évolution.

**Piège éventuel**
Transformer un besoin qualité en prescription technique avant d’avoir confirmé
le besoin et évalué les alternatives.

**Exemple provenant de la référence Youssfi**
L’existence d’un Gateway ou de deux interfaces dans la référence n’établit pas
que notre projet doit les reprendre; ces choix ne relèvent pas de la baseline
Requirements.

## Entretien — Environnement et incrément Customer local

> Les exemples ci-dessous distinguent les choix approuvés, les vérifications
> rapportées par l'apprenant et les éléments qui restent à confirmer. Le reactor
> Maven est approuvé mais n'a pas encore été créé.

## Quelle différence entre le client Docker et le daemon?

**Réponse courte**
`docker --version` vérifie que le client CLI répond; `docker info` interroge le
daemon et son contexte d'exécution.

**Explication détaillée**
Le client peut être installé même lorsque Docker Desktop est arrêté ou que le
contexte cible est indisponible. Il faut donc vérifier les deux. De même, la
version de Docker Compose ne prouve pas à elle seule qu'une image peut être
téléchargée et un conteneur exécuté.

**Exemple concret dans notre projet**
L'inventaire rapporté indiquait Java 21.0.2, Maven 3.9.9, Docker 29.8.2,
Compose 5.5.1 et OpenSpec 1.14.0; `docker info` confirmait un daemon Linux
joignable. `docker run --rm hello-world` a ensuite terminé avec succès. Les
chemins exacts des exécutables n'ont pas été consignés.

**Piège ou erreur fréquente**
Déduire qu'un environnement est entièrement opérationnel uniquement parce que
`docker --version` ou `docker compose version` affiche une version.

## Pourquoi choisir un reactor Maven multi-module avant de créer les services?

**Réponse courte**
Le POM racine coordonne les modules et leurs versions; chaque service garde
son propre module et son cycle de construction.

**Explication détaillée**
Un reactor agrège plusieurs projets Maven et permet une construction cohérente
depuis la racine. Il ne fusionne pas les responsabilités métier et n'impose
pas de partager les données entre services. Le layout doit être arbitré avant
de générer les POMs afin d'éviter de créer une structure implicite.

**Exemple concret dans notre projet**
CPS-DES-01 a approuvé un `pom.xml` agrégateur à la racine, avec
`customer-service/` et `api-gateway/`, chacun destiné à avoir son POM. Java 21
et Spring Boot 3.5.x sont retenus. Aucun POM n'est encore créé : CPS-MVN-01
sera la première tâche d'implémentation du reactor.

**Piège ou erreur fréquente**
Présenter une structure approuvée sur papier comme un build Maven déjà généré
ou vérifié.

## Pourquoi publier PostgreSQL uniquement sur la boucle locale?

**Réponse courte**
Le bind `127.0.0.1:5432:5432` permet un accès depuis l'hôte pour le
développement, sans publier le port sur toutes les interfaces réseau.

**Explication détaillée**
Le port du conteneur reste 5432. Les autres conteneurs du réseau Compose
utilisent le nom DNS du service (`postgres:5432`), tandis qu'un outil local
comme DBeaver se connecte via `127.0.0.1:5432`. Un volume nommé conserve les
données au redémarrage du conteneur. Le healthcheck `pg_isready` prouve que la
base accepte des connexions, pas que chaque schéma applicatif est correct.

**Exemple concret dans notre projet**
`docker compose config` a réussi. `docker compose ps postgres` a affiché
`healthy`, et `pg_isready` a retourné que le serveur acceptait les connexions.
Après `docker compose restart postgres`, la lecture de la sonde
`persistence_probe` a retourné `id = 1`. Ne pas utiliser `docker compose down
-v` lors d'une vérification de persistance.

**Piège ou erreur fréquente**
Confondre un healthcheck réussi avec une preuve de persistance, ou utiliser
`localhost:5432` depuis un autre conteneur au lieu de `postgres:5432`.

## À quoi sert un realm Keycloak, et pourquoi le client doit-il être dans le bon realm?

**Réponse courte**
Un realm isole ses utilisateurs, rôles et clients. Le client OIDC doit être
déclaré dans le realm qui émet l'issuer utilisé par l'application.

**Explication détaillée**
L'issuer identifie l'autorité qui émet les jetons. Un client créé dans `master`
n'est pas automatiquement disponible dans `customer-dev`. Une discovery
correcte ne suffit donc pas : le client doit aussi exister et être activé dans
le realm attendu.

**Exemple concret dans notre projet**
Le realm cible est `customer-dev`, avec issuer
`http://localhost:8081/realms/customer-dev`. `customer-local` y est configuré
comme client public avec Standard Flow et PKCE S256, et une URI de redirection
exacte `http://127.0.0.1:8765/callback`. La définition d'import se trouve dans
`infra/keycloak/realm-dev.json`, mais son import dans un realm persistant n'a
pas été testé.

**Piège ou erreur fréquente**
Confondre une définition de client dans un fichier JSON ou dans `master` avec
la présence effective du client dans le realm `customer-dev`.

## Comment séparer le rôle Customer du rôle opérateur?

**Réponse courte**
`CUSTOMER` est le rôle realm par défaut pour les nouvelles inscriptions;
`OPERATOR` n'est jamais attribué par défaut et doit être accordé manuellement
par un administrateur.

**Explication détaillée**
Le formulaire public ne doit proposer aucun choix de rôle privilégié. La
séparation doit être vérifiée dans les affectations effectives : un compte
inscrit reçoit `CUSTOMER`, tandis qu'un compte opérateur distinct reçoit
`OPERATOR` par une action administrative. La présence du rôle dans la console
ne prouve pas qu'il est inclus dans un ID token; cela dépend notamment des
mappings et scopes du client.

**Exemple concret dans notre projet**
La console a confirmé les rôles realm `CUSTOMER` et `OPERATOR`, `CUSTOMER`
seul dans les rôles par défaut, et l'inscription activée dans `customer-dev`
mais désactivée dans `master`. Un compte de test avait `CUSTOMER` sans
`OPERATOR`; un compte distinct a reçu `OPERATOR` comme affectation directe.
Le test PKCE n'a toutefois affiché aucun de ces rôles dans l'ID token; le
mapper correspondant reste à examiner.

**Piège ou erreur fréquente**
Croire qu'un rôle n'est pas accordé parce qu'il est absent d'un ID token, sans
vérifier séparément l'affectation effective et la configuration du mapper de
rôles pour ce type de jeton.

## Comment PKCE protège-t-il le flux Authorization Code d'un client public?

**Réponse courte**
Le client crée un `code_verifier` aléatoire et envoie son empreinte S256 comme
`code_challenge`. Il doit présenter le verifier original pour échanger le code.

**Explication détaillée**
Le flux démarre avec une requête d'autorisation et revient au callback local.
`state` lie la réponse à la demande initiée par le client et protège contre
une réponse injectée. `nonce` lie l'ID token à la demande et réduit le risque
de rejeu. Le code et le verifier restent en mémoire pendant l'échange; un
client public ne peut pas garder un secret client de façon confidentielle.
La validation du nonce ou le décodage de claims ne remplacent pas la validation
cryptographique du JWT.

**Exemple concret dans notre projet**
Le script `scripts/dev/oidc-pkce-login.ps1` cible Windows PowerShell 5.1,
ouvre le navigateur et écoute uniquement sur `127.0.0.1:8765` à
`/callback`. La commande a terminé l'échange OIDC et affiché l'issuer attendu.
`email_verified` valait `False` et aucun rôle n'était présent dans l'ID token.
Le script n'a pas validé la signature; cette tâche revient au Resource Server
Spring Security ultérieur.

**Piège ou erreur fréquente**
Dire que le navigateur « utilise un client secret PKCE », ou qu'un ID token
est fiable parce que son payload a été décodé et que `iss` correspond. Sans
validation de signature et des contraintes du jeton, les claims ne sont pas
cryptographiquement authentifiés.
