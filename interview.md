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

## Pourquoi choisir Spring Cloud Gateway avec WebFlux et Reactor Netty?

**Réponse courte**
Cette combinaison fournit un Gateway réactif et non bloquant. Elle est
distincte du modèle Servlet de Spring MVC; les deux starters ne doivent pas
être mélangés dans cette application.

**Explication détaillée**
Une API Gateway fournit un point d'entrée et peut appliquer des filtres et
acheminer les requêtes vers les services. Spring Cloud Gateway Server WebFlux
repose sur Spring WebFlux, Reactor et le serveur embarqué Reactor Netty. Ce
modèle traite les entrées/sorties de manière asynchrone et non bloquante.
Spring MVC utilise le modèle Servlet, habituellement avec Tomcat et une
architecture différente. Ajouter `spring-boot-starter-web` au Gateway peut
introduire Spring MVC et faire sélectionner une application Servlet ou causer
des incompatibilités. Un code bloquant exécuté sur les event loops réactives
peut aussi annuler le bénéfice du modèle non bloquant.

**Exemple concret dans notre projet**
`api-gateway/pom.xml` déclare
`spring-cloud-starter-gateway-server-webflux`, dont la version vient du BOM
Spring Cloud 2025.0.1 géré par le parent Spring Boot 3.5.10. Il n'ajoute pas
`spring-boot-starter-web`; `application.properties` fixe le port `8080`.
Pour CPS-APP-02, le test de contexte a réussi, l'application a démarré et le
healthcheck a retourné HTTP 200 avec `{"status":"UP"}`. Aucun routage métier
n'est encore configuré.

**Piège ou erreur fréquente**
Ajouter `spring-boot-starter-web` par habitude dans une Gateway réactive, ou
conclure que le healthcheck `UP` prouve que des routes vers Customer sont
configurées et fonctionnelles.

## Quelle différence entre un Dockerfile, une image et un conteneur?

**Réponse courte**
Le Dockerfile décrit la construction; l'image est le résultat réutilisable;
le conteneur est une instance de cette image en cours d'exécution.

**Explication détaillée**
Un build multi-stage sépare les outils nécessaires à la compilation du
runtime. La première étape peut contenir Maven et un JDK; l'étape finale ne
contient que le JRE et l'artefact exécutable. Cela réduit le contenu de l'image
de runtime et évite d'y embarquer les sources ou Maven. Exécuter l'application
avec un UID non privilégié réduit les permissions du processus dans le
conteneur.

**Exemple concret dans notre projet**
`customer-service/Dockerfile` et `api-gateway/Dockerfile` utilisent chacun
Maven 3.9.9 avec Java 21 pour construire le module depuis le reactor racine,
puis Eclipse Temurin JRE 21 pour lancer le JAR correspondant. Les noms des
artefacts sont fondés sur les `artifactId` Maven (`customer-service` et
`api-gateway`); les copies tolèrent leur version Maven par un motif ciblé. Le
`.dockerignore` exclut notamment `.env`, `.git` et `target`.

**Piège ou erreur fréquente**
Confondre `EXPOSE` avec une publication sur l'hôte, copier le dépôt complet
dans l'image finale, ou lancer l'application en tant que root sans raison.

## Comment Compose relie les services et distingue les ports?

**Réponse courte**
Les services d'un réseau Compose se joignent par leur nom DNS et leur port
interne. `ports` publie un port sur l'hôte; `expose` ne crée pas de publication
hôte.

**Explication détaillée**
Compose crée un réseau par défaut où les noms de services sont résolus par
DNS. Depuis un conteneur, `localhost` désigne ce conteneur lui-même, pas un
autre service. Une publication relie un port de l'hôte à un port du conteneur;
elle n'est pas nécessaire pour que deux services du réseau communiquent.

**Exemple concret dans notre projet**
`customer-service` écoute sur le port interne `8082` et n'a aucune entrée
`ports`; `api-gateway` écoute sur `8080` et publie
`127.0.0.1:8080:8080`. Les services PostgreSQL et Keycloak gardent leurs noms
Compose `postgres` et `keycloak`. Les applications minimales ne les appellent
pas encore, donc aucune URL de connexion n'est ajoutée.

**Piège ou erreur fréquente**
Utiliser `localhost` pour joindre PostgreSQL depuis Customer, publier
inutilement le port Customer sur l'hôte, ou considérer `EXPOSE` comme une
règle de pare-feu.

## Pourquoi un healthcheck Compose et quand utiliser `depends_on`?

**Réponse courte**
Un healthcheck mesure la readiness applicative; `depends_on` avec une
condition de santé ne convient que lorsqu'un service dépend réellement d'un
autre au démarrage.

**Explication détaillée**
L'état `running` signifie que le processus du conteneur tourne, pas qu'il
accepte les requêtes. Un healthcheck périodique peut tester une réponse HTTP
réelle et faire passer le conteneur à `healthy`. `depends_on` peut ordonner le
démarrage et attendre un état `healthy`, mais ne remplace ni les retries
applicatifs ni la résilience après le démarrage.

**Exemple concret dans notre projet**
Les deux images sondent leur propre endpoint Actuator `/actuator/health` avec
`wget`; les conteneurs sont devenus `Up (healthy)`. Il n'y a pas de
`depends_on` vers PostgreSQL ou Keycloak, car les squelettes ne s'y connectent
pas au démarrage. Customer est joignable sur `customer-service:8082` dans le
réseau même sans port hôte publié.

**Piège ou erreur fréquente**
Déduire la readiness de `Up`, ajouter des dépendances de démarrage qui ne
correspondent à aucun besoin, ou utiliser un outil absent de l'image dans le
healthcheck.

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
Le POM racine agrège les modules et centralise les règles communes; chaque
service reste un projet Maven distinct avec son propre artefact.

**Explication détaillée**
Le reactor est l'ensemble des projets Maven construits depuis un POM
agrégateur. Le POM racine est aussi le parent des modules, ce qui leur permet
d'hériter des coordonnées et des réglages communs. `mvn -N validate` limite
l'exécution au POM courant; `mvn validate` parcourt aussi les modules. La phase
`validate` vérifie le modèle et le cycle Maven, mais ne compile ni ne démarre
une application.

**Exemple concret dans notre projet**
CPS-DES-01 a approuvé le `pom.xml` racine et les modules `customer-service/`
et `api-gateway/`. Le POM racine de CPS-MVN-01 déclare seulement ces deux
modules; chacun référence le parent via `../pom.xml`. `mvn -N validate` et
`mvn validate` ont tous deux retourné `BUILD SUCCESS`.

**Piège ou erreur fréquente**
Confondre agrégation et héritage : un parent peut fournir des réglages sans
agréger de modules, et un agrégateur peut lister des modules sans être leur
parent. Ici, le POM racine joue les deux rôles. Un `BUILD SUCCESS` de
`validate` ne prouve pas encore que le code compile ou que l'application
démarre.

## Pourquoi utiliser la gestion centralisée des dépendances et un BOM?

**Réponse courte**
Un BOM aligne les versions d'un ensemble de bibliothèques compatibles; les
modules peuvent ensuite déclarer leurs dépendances sans répéter leurs versions.

**Explication détaillée**
La section `dependencyManagement` définit les versions qui seront appliquées
quand un module déclarera une dépendance correspondante; elle n'ajoute pas
elle-même ces dépendances au classpath. Le parent Spring Boot fournit la
gestion commune Spring Boot et des plugins. Le BOM Spring Cloud complète cette
gestion pour les composants Cloud et doit appartenir à une release train
compatible avec la version Spring Boot retenue.

**Exemple concret dans notre projet**
Le parent utilise Spring Boot 3.5.10, importe Spring Cloud 2025.0.1 via
`spring-cloud-dependencies` et fixe Java 21. Les versions sont cohérentes avec
la ligne Boot 3.5.x / Cloud 2025.0.x déjà présente dans les références du
dépôt. Les POM enfants ne déclarent encore aucune dépendance fonctionnelle.

**Piège ou erreur fréquente**
Croire qu'importer un BOM ajoute les bibliothèques à l'application, ou gérer
manuellement des versions Spring Cloud sans vérifier leur compatibilité avec
Spring Boot.

## La propriété `java.version` garantit-elle le JDK utilisé par Maven?

**Réponse courte**
Non. La propriété configure la version Java ciblée par le projet; `mvn
-version` montre le runtime Java qui exécute Maven.

**Explication détaillée**
Le parent Spring Boot s'appuie sur `java.version` pour configurer les
compilateurs gérés par son build. Cette propriété ne sélectionne pas à elle
seule un JDK dans le terminal. Il faut donc vérifier séparément le runtime
Maven, puis les builds qui compilent effectivement le code.

**Exemple concret dans notre projet**
Le parent fixe `<java.version>21</java.version>`. La commande `mvn -version`
a rapporté Java 21.0.2 et le runtime
`C:\SpringBOOT\outils\jdk-21`. Les commandes `mvn -N validate` et
`mvn validate` ont réussi, mais elles n'ont pas encore compilé de code Java.

**Piège ou erreur fréquente**
Déduire du seul POM que Maven utilise Java 21, ou déduire d'un `BUILD SUCCESS`
de `validate` que la compilation a été testée.

## Quel est le rôle de la classe principale Spring Boot?

**Réponse courte**
Elle active la configuration automatique et le scan des composants, puis
`SpringApplication.run()` démarre l'application et crée son contexte Spring.

**Explication détaillée**
`@SpringBootApplication` regroupe la configuration Spring Boot, l'auto-
configuration basée sur le classpath et le scan des composants depuis le
package de la classe. `SpringApplication.run()` prépare l'environnement,
construit le contexte d'application et déclenche le cycle de vie des beans.
Les composants placés dans les packages enfants sont donc découverts par
défaut.

**Exemple concret dans notre projet**
`customer-service/src/main/java/com/anisboulila/customerservice/CustomerServiceApplication.java`
est le point d'entrée du module. La commande `mvn -pl customer-service
spring-boot:run` a démarré cette classe; les logs rapportés contenaient
`Started CustomerServiceApplication in 1.536 seconds`.

**Piège ou erreur fréquente**
Placer des composants hors du package scanné et croire qu'ils seront
automatiquement détectés, ou confondre le contexte Spring avec le serveur HTTP.

## Que vérifie un test `@SpringBootTest`?

**Réponse courte**
Il charge le contexte Spring Boot pour vérifier que l'application peut
initialiser sa configuration et ses beans.

**Explication détaillée**
Le test de contexte détecte notamment les erreurs de configuration et
d'auto-configuration qui empêchent le démarrage. Par défaut, `@SpringBootTest`
utilise un environnement web simulé : il ne prouve pas à lui seul qu'un vrai
serveur écoute sur le port attendu ni qu'une requête HTTP fonctionne. Il faut
une vérification séparée pour ces critères.

**Exemple concret dans notre projet**
`CustomerServiceApplicationTests.contextLoads()` est un test sans logique
métier. `mvn -pl customer-service -am test` a retourné `BUILD SUCCESS`; le
healthcheck HTTP a ensuite confirmé séparément le serveur réel.

**Piège ou erreur fréquente**
Traiter le succès du test de contexte comme une preuve de disponibilité réseau,
ou y connecter une base partagée alors que ce squelette n'en a pas besoin.

## À quoi servent le serveur embarqué et Spring Boot Actuator?

**Réponse courte**
Le starter Web apporte un serveur HTTP embarqué; Actuator fournit des endpoints
de diagnostic, dont `/actuator/health`.

**Explication détaillée**
Le serveur embarqué permet de lancer l'application avec une seule commande,
sans installer ni déployer un serveur d'applications séparé. La propriété
`server.port` fixe le port d'écoute. Actuator expose des informations
opérationnelles; par défaut, l'endpoint de santé est exposé par HTTP, tandis
que les autres endpoints ne sont pas ouverts par défaut. Une réponse `UP`
indique que les indicateurs de santé actifs sont satisfaits; elle ne prouve
pas que des fonctionnalités métier existent.

**Exemple concret dans notre projet**
`customer-service/pom.xml` ajoute `spring-boot-starter-web` et
`spring-boot-starter-actuator`; `application.properties` fixe le port `8082`.
`Invoke-WebRequest http://localhost:8082/actuator/health` a retourné HTTP 200
et `{"status":"UP"}`. L'application a ensuite été arrêtée avec `Ctrl+C`.

**Piège ou erreur fréquente**
Exposer tous les endpoints Actuator sans besoin ni contrôle d'accès, ou
interpréter `UP` comme preuve que les opérations métier sont disponibles.
Depuis CPS-DB-01, l'indicateur `db` vérifie également la connexion datasource,
mais pas le bon fonctionnement des futures opérations métier.

## À quoi servent Flyway et les migrations versionnées?

**Réponse courte**
Flyway applique les scripts SQL versionnés dans un ordre déterministe et
enregistre leur état dans `flyway_schema_history`.

**Explication détaillée**
Une migration versionnée décrit une évolution SQL et n'est appliquée qu'une
fois pour une version donnée. Flyway compare les migrations disponibles avec
l'historique stocké dans la base et valide leurs checksums avant de migrer.
L'application peut ainsi initialiser ou faire évoluer son schéma au démarrage
sans dépendre d'une génération implicite du schéma par Hibernate ou
`schema.sql`.

**Exemple concret dans notre projet**
La datasource et Flyway sont configurés dans `customer-service`; la migration
de preuve `V1__flyway_proof.sql` se trouve uniquement dans les ressources de
test et exécute `SELECT 1`. La migration métier `V1__create_customer.sql`
réside dans les ressources principales et crée le schéma Customer. Les tests
vérifient que la version `1` apparaît dans `flyway_schema_history`.

**Piège ou erreur fréquente**
Modifier une migration déjà appliquée, supposer qu'un fichier SQL présent a
été exécuté, ou créer le même schéma en parallèle avec Hibernate et
`schema.sql`.

## Qu'est-ce qu'une datasource, et pourquoi son URL dépend-elle du contexte?

**Réponse courte**
La datasource fournit les connexions JDBC; son hôte dépend de l'endroit d'où
tourne l'application.

**Explication détaillée**
Un programme lancé directement sur le poste rejoint le port PostgreSQL publié
sur `localhost:5432`. Un conteneur ne doit pas utiliser son propre `localhost`
pour joindre un autre service; il utilise le nom DNS Compose du service.
Spring Boot construit la datasource avec le driver PostgreSQL et les valeurs
`SPRING_DATASOURCE_*`.

**Exemple concret dans notre projet**
Le profil local utilise `localhost:5432` par défaut. Dans Compose,
`customer-service` reçoit `jdbc:postgresql://postgres:5432/...` ainsi que les
identifiants issus des variables PostgreSQL. Compose attend que PostgreSQL
soit `healthy` avant de démarrer Customer, car Flyway doit accéder à la base
pendant l'initialisation du contexte.

**Piège ou erreur fréquente**
Utiliser `localhost` depuis un conteneur, versionner un vrai mot de passe, ou
confondre un conteneur PostgreSQL `running` avec une base prête à accepter des
connexions.

## Que prouve `flyway_schema_history` dans nos tests?

**Réponse courte**
Une ligne réussie dans `flyway_schema_history` prouve qu'une version donnée a
été reconnue et appliquée par Flyway sur la base interrogée.

**Explication détaillée**
Le test vérifie aussi le produit de base avec `SELECT version()`, puis lit la
version courante du bean Flyway et la ligne `version = '1'` avec `success =
TRUE`. Cela distingue une vraie exécution SQL sur PostgreSQL d'une simple
compilation des dépendances.

**Exemple concret dans notre projet**
Deux vérifications ont été exécutées. Le profil opt-in
`flyway-compose-postgres` a écrit dans le schéma persistant
`cps_flyway_probe` de la base partagée `customer_db`; il ne s'agit pas d'une
base isolée. Le profil `flyway-testcontainers` a répété la preuve dans un
PostgreSQL 17 jetable, sans modifier `customer_db`.

**Piège ou erreur fréquente**
Présenter un schéma de test dans une base partagée comme une base isolée, ou
affirmer qu'un test est passé alors qu'il n'a fait que compiler les
dépendances.

## Comment les contraintes de la table Customer protègent-elles les données?

**Réponse courte**
La migration PostgreSQL impose l'identité unique par `sub`, l'unicité de
l'e-mail après trim et mise en minuscules, ainsi que les limites du nom.

**Explication détaillée**
`V1__create_customer.sql` crée une clé `UUID` générée par PostgreSQL, un
`sub` non nul et unique, un nom limité à 120 caractères et non vide après
trim, un e-mail non nul et `created_at` obligatoire de type `TIMESTAMPTZ`.
L'index unique `lower(btrim(email))` applique l'unicité indépendamment de la
casse et des espaces périphériques. Il protège aussi contre les courses
concurrentes, contrairement à une vérification préalable faite uniquement
dans le code applicatif.

**Exemple concret dans notre projet**
`CustomerSchemaComposePostgresIT` exécute la vraie migration contre PostgreSQL
Compose dans le schéma dédié `cps_customer_schema` de `customer_db`. Il vérifie
la ligne Flyway, les colonnes et l'index, une insertion valide, puis le rejet
des doublons `sub`/e-mail et des noms invalides. Les transactions de test sont
annulées; le schéma et la table restent inspectables.

**Piège ou erreur fréquente**
Confondre le nom de la base (`customer_db`) avec le schéma (`public` ou
`cps_customer_schema`), croire que la base partagée a été réinitialisée, ou
croire qu'une vérification applicative seule remplace une contrainte unique
PostgreSQL.

## Quelle est la différence entre une entité JPA et un DTO HTTP?

**Réponse courte**
L'entité JPA représente une ligne persistée; un DTO représente un contrat
d'échange HTTP. Ils ne doivent pas être confondus.

**Explication détaillée**
L'entité porte le mapping objet-relationnel et les colonnes de la table. Un DTO
est conçu selon les besoins et règles de l'API; il ne doit pas exposer les
annotations, champs internes ou détails de persistance. Une couche de mapping
séparera les deux lorsque les endpoints seront implémentés.

**Exemple concret dans notre projet**
`Customer` mappe `customer` (`id`, `sub`, `name`, `email`, `created_at`) par
accès aux champs. Il n'existe encore ni contrôleur ni DTO HTTP dans CPS-DOM-01.

**Piège ou erreur fréquente**
Retourner directement une entité depuis un contrôleur et ainsi rendre le
contrat HTTP dépendant du schéma de base.

## Quel est le rôle du repository et du service applicatif?

**Réponse courte**
Le repository fournit l'accès persistant; le service applicatif porte le
parcours d'écriture et les transformations métier prévues.

**Explication détaillée**
`CustomerRepository` étend `JpaRepository<Customer, UUID>` et délègue les
opérations de persistance à Spring Data JPA. `CustomerApplicationService`
normalise le nom et l'e-mail, fixe `created_at` avec une `Clock` UTC, puis
demande au repository de sauvegarder l'entité. Il ne contient ni contrôleur,
ni DTO HTTP, ni règles d'autorisation; l'appelant devra fournir l'identité
issue d'un JWT validé.

**Exemple concret dans notre projet**
Le test utilise un repository Mockito et une `Clock.fixed(...)` pour vérifier
la normalisation et l'horodatage sans base PostgreSQL ni contexte Spring.

**Piège ou erreur fréquente**
Placer les requêtes SQL dans un contrôleur, laisser le repository décider de
la canonicalisation, ou confondre `save()` avec une validation de l'identité
OIDC.

## Pourquoi configurer Hibernate en `validate` plutôt qu'en `update`?

**Réponse courte**
Flyway possède le schéma; Hibernate vérifie le mapping sans créer ni modifier
les tables.

**Explication détaillée**
`spring.jpa.hibernate.ddl-auto=validate` compare les métadonnées mappées aux
tables existantes après l'application des migrations Flyway. Il ne crée pas
de schéma. La création ou l'évolution demeure explicite et versionnée dans
les migrations.

**Exemple concret dans notre projet**
`Customer` mappe le UUID, les colonnes `text`, `varchar(120)` et
`TIMESTAMPTZ` de `V1__create_customer.sql`. Le profil d'intégration de schéma
indique à Hibernate le schéma de test `cps_customer_schema`; la validation
de l'interaction JPA/PostgreSQL reste à couvrir par les tests d'intégration.

**Piège ou erreur fréquente**
Utiliser `ddl-auto=update` en parallèle de Flyway; les changements implicites
peuvent diverger des migrations versionnées.

## Que prouvent les tests unitaires de CPS-DOM-01?

**Réponse courte**
Ils prouvent la normalisation et le comportement du service en isolation, pas
que JPA écrit correctement dans PostgreSQL.

**Explication détaillée**
Le test mocke `CustomerRepository` et injecte une horloge fixe, sans charger
Spring, Docker ou une base. Le mapping réel, le générateur Hibernate de UUID
qui interroge `gen_random_uuid()` sur PostgreSQL et la persistance seront
validés à la frontière d'intégration prévue plus tard.

**Exemple concret dans notre projet**
La commande ciblée a rapporté trois tests exécutés, zéro échec, erreur ou
ignoré. L'entité n'a pas d'UUID avant l'insertion; le générateur Hibernate
personnalisé demande à PostgreSQL de le produire lors de la persistance.

**Piège ou erreur fréquente**
Présenter un test avec repository mocké comme preuve de persistance, ou
confondre l'UUID généré par Hibernate en Java avec celui demandé à PostgreSQL.

## À quoi sert RFC 9457 pour les erreurs HTTP?

**Réponse courte**
RFC 9457 fournit une structure commune `application/problem+json` pour décrire
une erreur HTTP sans exposer d'informations internes.

**Explication détaillée**
Le document Problem Details contient notamment `type`, `title`, `status`,
`detail` et, si pertinent, `instance`. Le contrat Customer ajoute le code
stable `code`, l'identifiant `correlationId` et, pour les erreurs de champs,
`errors`. Les détails sont sûrs; SQL, trace de pile, jeton, secret et contenu
de requête ne sont jamais retournés.

**Exemple concret dans notre projet**
`RestProblemExceptionHandler` construit un `ProblemDetail` avec
`type=about:blank`, le statut réel et les extensions prévues. Le test MockMvc
compare strictement le JSON d'une erreur 400, puis vérifie un 500 générique
avec `INTERNAL_ERROR`.

**Piège ou erreur fréquente**
Renvoyer `exception.getMessage()` sans contrôle, ou créer une forme JSON
différente pour chaque contrôleur.

## Quel est le rôle de `@RestControllerAdvice` et de l'exception mapping?

**Réponse courte**
`@RestControllerAdvice` centralise la conversion des exceptions REST en
réponses HTTP structurées.

**Explication détaillée**
Le handler convertit une `ApiProblemException` connue en statut, code et détail
sûr. Les erreurs de validation sont ramenées à `VALIDATION_FAILED`, avec les
noms de champs et codes de validation, sans valeur rejetée. Une exception
inattendue devient un 500 générique; sa trace est réduite à ses types et
frames pour les logs corrélés, sans son message potentiellement sensible.

**Exemple concret dans notre projet**
Le contrôleur `ProblemProbeController` existe uniquement dans les tests
MockMvc. Il déclenche une erreur prévue ou une exception interne; aucun
endpoint de production n'a été ajouté pour tester le handler.

**Piège ou erreur fréquente**
Mettre des règles métier dans le handler global ou transformer toutes les
exceptions en la même erreur client.

## Pourquoi garder un code d'erreur stable?

**Réponse courte**
Le client et les tests s'appuient sur `code`, pas sur le texte humain de
`detail`.

**Explication détaillée**
Les codes en `UPPER_SNAKE_CASE` restent stables quand un message est reformulé.
Le handler central ne choisit pas les codes métier futurs; les exceptions
applicatives connues fournissent leur code. L'erreur interne générique utilise
`INTERNAL_ERROR`, tandis qu'une erreur de validation utilise
`VALIDATION_FAILED`.

**Exemple concret dans notre projet**
Le test compare `VALIDATION_FAILED` dans la réponse Problem Details exacte et
vérifie `INTERNAL_ERROR` pour une exception imprévue.

**Piège ou erreur fréquente**
Utiliser le message d'exception comme code ou modifier un code public sans
considérer ses consommateurs.

## Comment un correlation ID aide-t-il sans devenir une identité de sécurité?

**Réponse courte**
Il relie l'erreur HTTP à ses logs, mais ne prouve ni l'identité ni les droits
de l'appelant.

**Explication détaillée**
Le filtre accepte `X-Correlation-ID` uniquement sous forme UUID canonique.
Une valeur absente, multiple ou invalide est remplacée par un UUID serveur.
Le même identifiant est placé dans le MDC, le corps Problem Details et
l'en-tête de réponse. Les logs d'erreur incluent cet ID; les messages bruts
d'exception, qui pourraient contenir un secret, sont exclus.

**Exemple concret dans notre projet**
MockMvc vérifie qu'un UUID valide est conservé, qu'une valeur invalide est
remplacée et que l'en-tête de réponse ne contient qu'une seule valeur.

**Piège ou erreur fréquente**
Réfléchir un en-tête arbitraire, traiter l'ID comme un jeton de confiance, ou
laisser un identifiant différent dans les logs et dans la réponse.

## Comment Flyway fait-il échouer le démarrage si une migration est invalide?

**Réponse courte**
Flyway s'exécute pendant l'initialisation Spring; si la migration échoue,
l'initialisation du contexte échoue aussi, faute de configuration qui masque
l'erreur.

**Explication détaillée**
Le service active Flyway, valide les migrations au démarrage et échoue si le
répertoire de migrations manque. Spring Boot exécute l'initialiseur Flyway
avant de déclarer l'application prête; une exception de migration remonte et
empêche le contexte de démarrer. Le healthcheck ne peut donc pas déclarer le
service `healthy` après un échec de migration.

**Exemple concret dans notre projet**
Le test Testcontainers a chargé un SQL invalide dans le schéma jetable
`cps_flyway_invalid` et vérifié que le contexte échouait avec une cause Flyway.
La base locale partagée n'a pas été utilisée pour ce test négatif.

**Piège ou erreur fréquente**
Désactiver Flyway ou ignorer les migrations invalides pour faire démarrer
l'application, ou tester volontairement un SQL cassé sur la base de
développement partagée.

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
ne prouve pas qu'il est inclus dans un access token; cela dépend notamment
des mappings et scopes du client.

**Exemple concret dans notre projet**
La console a confirmé les rôles realm `CUSTOMER` et `OPERATOR`, `CUSTOMER`
seul dans les rôles par défaut, et l'inscription activée dans `customer-dev`
mais désactivée dans `master`. Un compte de test avait `CUSTOMER` sans
`OPERATOR`; un compte distinct a reçu `OPERATOR` comme affectation directe.
Lors du test direct, le résumé local de l'access token a montré `CUSTOMER`.
L'ID token et l'access token sont des jetons distincts : c'est l'access token
qui est envoyé à `customer-service`.

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
`/callback`. Le script termine l'échange OIDC et ne montre jamais les valeurs
des jetons. Son résumé des claims d'access token est seulement décodé, pas
vérifié; c'est le Resource Server qui vérifie cryptographiquement le jeton reçu.

**Piège ou erreur fréquente**
Dire que le navigateur « utilise un client secret PKCE », ou qu'un ID token
est fiable parce que son payload a été décodé et que `iss` correspond. Sans
validation de signature et des contraintes du jeton, les claims ne sont pas
cryptographiquement authentifiés.

## Comment `customer-service` valide-t-il un JWT indépendamment de la Gateway?

**Réponse courte**
Le Resource Server Spring Security vérifie lui-même la signature et les claims
du bearer token avant d'exposer un principal authentifié à l'application.

**Explication détaillée**
Nimbus récupère les clés publiques depuis le JWKS de l'issuer configuré et
vérifie la signature. Les validateurs vérifient aussi l'issuer exact,
l'expiration et les contraintes temporelles standard; un validateur propre au
service exige l'audience `customer-service`. La Gateway ne constitue donc pas
une frontière de confiance suffisante : chaque microservice valide le token
qu'il reçoit.

**Exemple concret dans notre projet**
En local, l'issuer attendu est
`http://localhost:8081/realms/customer-dev`; dans Compose, le conteneur récupère
le JWKS par `http://keycloak:8080/...` tout en vérifiant la valeur `iss`
émise pour le navigateur. Le mapper d'audience Keycloak doit ajouter
`customer-service` aux access tokens. Le test local réel a confirmé l'issuer
et l'audience, puis donné 401 sans jeton, 200 avec le jeton Keycloak et 401
après altération de la signature. Les tests automatisés couvrent en complément
les autres cas négatifs de façon déterministe.

**Piège ou erreur fréquente**
Tester uniquement avec le helper Spring `jwt()` : il injecte un JWT déjà
authentifié et ne prouve pas que la signature reçue est vérifiée.

## D'où viennent `sub` et les autorités Spring Security?

**Réponse courte**
`sub` vient du principal JWT vérifié; les autorités viennent de
`realm_access.roles`.

**Explication détaillée**
Après validation, `JwtAuthenticationToken#getName()` expose le sujet JWT.
Le convertisseur du projet lit uniquement les chaînes du claim Keycloak
`realm_access.roles` et les mappe en autorités sans ajouter de préfixe. Les
headers `X-User-Id` ou `X-Roles` sont contrôlés par l'appelant et ne sont
jamais consultés pour établir l'identité ou les rôles.

**Exemple concret dans notre projet**
Le test automatisé signe un JWT synthétique dont `sub` vaut
`verified-user-123`. Le test local Keycloak a aussi renvoyé le `sub` du
principal vérifié et l'autorité `CUSTOMER`; sa valeur n'est pas conservée dans
la documentation. Un appel avec des headers falsifiés conserve l'identité et
les rôles issus du JWT.

**Piège ou erreur fréquente**
Confondre l'authentification (token valide) avec l'autorisation (droit
d'effectuer une action). Un rôle présent ne doit pas créer une permission
métier qui n'a pas été explicitement configurée.

## Quelle différence entre HTTP 401 et HTTP 403?

**Réponse courte**
401 signifie que l'appelant n'est pas authentifié; 403 signifie qu'il est
authentifié mais interdit pour la ressource demandée.

**Explication détaillée**
Un bearer token absent ou invalide est rejeté avec HTTP 401. Spring Security
renvoie HTTP 403 lorsqu'une règle d'autorisation configurée refuse un principal
authentifié. La réponse suit le format Problem Details du projet et ne révèle
pas la cause cryptographique détaillée du rejet.

**Exemple concret dans notre projet**
Les tests automatisés couvrent 401 pour token absent, expiré, signé par une
autre clé, mauvais issuer, audience incorrecte ou absente. Le test réel a
confirmé 401 sans jeton et avec signature altérée, puis 200 avec l'access
token Keycloak. CPS-SEC-01 ne définit pas encore d'autorisation métier par
rôle; le gestionnaire 403 est préparé pour les règles des tâches ultérieures.

**Piège ou erreur fréquente**
Renvoyer 403 pour un token invalide ou 401 pour un utilisateur authentifié
mais non autorisé.
