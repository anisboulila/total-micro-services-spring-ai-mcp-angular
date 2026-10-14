# Tasks

> Parcours pédagogique interactif : une seule tâche est traitée à la fois.
> Après chaque tâche, l’apprenant exécute les commandes et partage les
> résultats; Copilot les analyse et attend une validation explicite avant
> toute modification ou tâche suivante. Toutes les cases restent décochées
> jusqu’à réalisation et vérification. Le backlog global n’est jamais coché
> par ce fichier.

## 1. Environnement et décisions de layout

- [x] 1.1 **CPS-ENV-01** — Inventorier les outils

- **Objectif / résultat :** connaître l’état réel de Java, Maven, Docker, Compose et OpenSpec avant installation.
- **Dépendances :** aucune.
- **Concepts :** version, `PATH`, client Docker vs daemon.
- **Apprenant :** lance les sondes; partage versions/messages uniquement, aucun secret.
- **Fichiers Copilot :** aucun.
- **Commandes :** `Get-Command java,mvn,docker,openspec -ErrorAction SilentlyContinue | Select-Object Name,Source`; `java -version`; `mvn -version`; `docker --version`; `docker compose version`; `docker info`; `openspec --version`.
- **Vérification / acceptation :** tableau des versions disponibles et outils absents clairement identifiés; aucune hypothèse d’installation.
- **Erreurs / diagnostic :** `Get-Command` absent = outil absent ou `PATH`; échec `docker info` = daemon/contexte, même si `docker --version` répond.
- **Point d’arrêt :** attendre le compte rendu avant installation.

- [ ] 1.2 **CPS-ENV-02** — Installer ou réparer Java 21

- **Objectif / résultat :** avoir un JDK 21 sélectionné par le nouveau terminal.
- **Dépendances :** CPS-ENV-01, uniquement si nécessaire.
- **Concepts :** JDK/JRE, `JAVA_HOME`, `PATH`.
- **Apprenant :** installe ou corrige l’environnement.
- **Fichiers Copilot :** aucun.
- **Commandes :** si absent et `winget` disponible, `winget install --id EclipseAdoptium.Temurin.21.JDK -e`; ouvrir un nouveau PowerShell puis `java -version`; `where.exe java`; `$env:JAVA_HOME`.
- **Vérification / acceptation :** version sélectionnée 21 et chemin correspondant au JDK attendu.
- **Erreurs / diagnostic :** plusieurs Java dans le `PATH`; vérifier `where.exe java`, corriger l’ordre du `PATH`, ouvrir un nouveau terminal.
- **Point d’arrêt :** attendre la preuve Java 21 avant la suite.

- [ ] 1.3 **CPS-ENV-03** — Installer ou réparer Maven

- **Objectif / résultat :** disposer d’un Maven fonctionnel utilisant Java 21.
- **Dépendances :** CPS-ENV-01, CPS-ENV-02 si Maven n’est pas disponible.
- **Concepts :** Maven CLI, `MAVEN_HOME`, sélection Java.
- **Apprenant :** installe/répare Maven.
- **Fichiers Copilot :** aucun.
- **Commandes :** si absent et `winget` disponible, `winget install --id Apache.Maven -e`; nouveau terminal : `mvn -version`; `where.exe mvn`.
- **Vérification / acceptation :** Maven répond et affiche Java 21.
- **Erreurs / diagnostic :** `mvn` non reconnu = `PATH`; Java incorrect = vérifier `JAVA_HOME` et le terminal.
- **Point d’arrêt :** attendre la preuve Maven avant la suite.

- [ ] 1.4 **CPS-ENV-04** — Installer et démarrer Docker Desktop

- **Objectif / résultat :** daemon Docker local démarré en mode Linux.
- **Dépendances :** CPS-ENV-01, uniquement si Docker/daemon indisponible.
- **Concepts :** Docker Desktop, WSL2/virtualisation, client et serveur.
- **Apprenant :** installe si nécessaire et démarre Docker Desktop.
- **Fichiers Copilot :** aucun.
- **Commandes :** si absent et `winget` disponible, `winget install --id Docker.DockerDesktop -e`; démarrer Docker Desktop puis `docker info`.
- **Vérification / acceptation :** `docker info` affiche serveur et `OSType: linux`.
- **Erreurs / diagnostic :** WSL2/virtualisation/droits ou daemon arrêté; utiliser le message exact de Docker Desktop, ne pas supprimer de volumes.
- **Point d’arrêt :** attendre la connexion daemon réussie.

- [x] 1.5 **CPS-ENV-05** — Valider Docker Compose

- **Objectif / résultat :** prouver que Compose et l’exécution d’un conteneur fonctionnent.
- **Dépendances :** CPS-ENV-04.
- **Concepts :** Compose CLI, téléchargement d’image et conteneur éphémère.
- **Apprenant :** exécute un conteneur de vérification.
- **Fichiers Copilot :** aucun.
- **Commandes :** `docker compose version`; `docker run --rm hello-world`.
- **Vérification / acceptation :** Compose répond et `hello-world` termine avec succès.
- **Erreurs / diagnostic :** proxy/DNS/réseau d’image différent d’un daemon arrêté; lire l’erreur Docker et confirmer `docker info`.
- **Point d’arrêt :** attendre les deux résultats avant Compose applicatif.

- [x] 1.6 **CPS-DES-01** — Approuver le layout du dépôt

- **Objectif / résultat :** décider le reactor Maven et chemins avant de générer quoi que ce soit.
- **Dépendances :** CPS-ENV-01.
- **Concepts :** mono-dépôt, modules, conventions de chemins, ports.
- **Apprenant :** approuve/corrige Maven root, `customer-service`, `api-gateway`, Compose et répertoire Keycloak; confirme Java 21/Spring Boot 3.5.x.
- **Fichiers Copilot :** cette tâche, si acceptée, modifie uniquement `design.md` et `tasks.md`.
- **Commandes :** `Get-ChildItem -Force`; `Get-ChildItem openspec/changes/implement-customer-profile-vertical-slice -Force`.
- **Vérification / acceptation :** chemins et ports de développement choisis et inscrits; aucun choix implicite.
- **Résultat approuvé :** `pom.xml` racine agrégateur; modules `customer-service/` et `api-gateway/` avec leurs POMs; `compose.yaml` à la racine; Keycloak sous `infra/keycloak/`; Java 21, Spring Boot 3.5.x; Gateway 8080, Customer 8082, Keycloak 8081, callback OIDC `127.0.0.1:8765`; PostgreSQL interne via `postgres:5432`, publié uniquement sur `127.0.0.1:5432` pour DBeaver.
- **Erreurs / diagnostic :** aucun projet applicatif local n’existe; les changements historiques ne sont pas des preuves de layout runtime.
- **Point d’arrêt :** attendre approbation explicite du layout; ne rien générer avant.

## 2. Docker Compose et services d’infrastructure

- [x] 2.1 **CPS-DOCKER-01** — Décrire le service PostgreSQL dans Compose

- **Objectif / résultat :** config Compose valide pour une base Customer locale.
- **Dépendances :** CPS-DES-01, CPS-ENV-05.
- **Concepts :** service, image, port, volume nommé, healthcheck, variable d’environnement.
- **Apprenant :** examine les variables, ports et stockage proposés.
- **Fichiers Copilot :** `compose.yaml`, `.env.example`, `.gitignore` si nécessaire.
- **Commandes :** `docker compose config`.
- **Vérification / acceptation :** configuration résolue, port documenté, volume nommé, aucun secret réel.
- **Erreurs / diagnostic :** YAML mal indenté ou variable manquante; traiter la ligne indiquée par Compose.
- **Point d’arrêt :** attendre `config` réussi avant démarrage.

- [ ] 2.2 **CPS-PG-01** — Démarrer et vérifier PostgreSQL

- **Objectif / résultat :** base locale disponible et healthy.
- **Dépendances :** CPS-DOCKER-01.
- **Concepts :** healthcheck, readiness, `pg_isready`.
- **Apprenant :** démarre la base et examine ses journaux.
- **Fichiers Copilot :** `compose.yaml` uniquement si un défaut démontré doit être corrigé.
- **Commandes :** `docker compose up -d postgres`; `docker compose ps`; `docker compose logs --tail 50 postgres`; `docker compose exec -T postgres sh -c 'pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"'`.
- **Vérification / acceptation :** conteneur healthy et `pg_isready` confirme l’acceptation de connexions.
- **Erreurs / diagnostic :** port occupé ou variables erronées; inspecter les logs, ne pas supprimer le volume.
- **Point d’arrêt :** attendre la preuve de readiness.

- [ ] 2.3 **CPS-PG-02** — Vérifier la persistance PostgreSQL

- **Objectif / résultat :** démontrer la conservation des données au redémarrage.
- **Dépendances :** CPS-PG-01.
- **Concepts :** volume Docker et cycle de vie conteneur.
- **Apprenant :** écrit et relit une ligne-sonde après restart.
- **Fichiers Copilot :** aucun.
- **Commandes :** `docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "CREATE TABLE IF NOT EXISTS persistence_probe (id integer primary key); INSERT INTO persistence_probe VALUES (1) ON CONFLICT DO NOTHING;"'`; `docker compose restart postgres`; `docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT * FROM persistence_probe;"'`.
- **Vérification / acceptation :** la ligne-sonde survit au restart.
- **Erreurs / diagnostic :** vérifier health et nom de DB; ne jamais utiliser `docker compose down -v`.
- **Point d’arrêt :** attendre preuve de persistance.

- [ ] 2.4 **CPS-KC-01** — Ajouter la configuration locale Keycloak

- **Objectif / résultat :** service Keycloak défini de manière reproductible.
- **Dépendances :** CPS-DOCKER-01.
- **Concepts :** fournisseur OIDC, configuration de développement, secrets.
- **Apprenant :** relit port, stockage et variables.
- **Fichiers Copilot :** `compose.yaml`, `infra/keycloak/` si import de realm retenu.
- **Commandes :** `docker compose config`.
- **Vérification / acceptation :** config valide et aucun secret personnel versionné.
- **Erreurs / diagnostic :** conflit de port ou interpolation manquante, signalés par Compose.
- **Point d’arrêt :** attendre configuration valide avant démarrer.

- [ ] 2.5 **CPS-KC-02** — Démarrer et vérifier Keycloak

- **Objectif / résultat :** endpoint discovery OIDC local disponible.
- **Dépendances :** CPS-KC-01.
- **Concepts :** démarrage Keycloak, issuer et discovery.
- **Apprenant :** démarre Keycloak et attend la disponibilité.
- **Fichiers Copilot :** `compose.yaml` en cas de correction ciblée.
- **Commandes :** `docker compose up -d keycloak`; `docker compose ps`; `docker compose logs --tail 80 keycloak`; `$KeycloakUrl='http://localhost:8081'; Invoke-RestMethod "$KeycloakUrl/realms/master/.well-known/openid-configuration"`.
- **Vérification / acceptation :** métadonnées JSON du realm master renvoyées.
- **Erreurs / diagnostic :** démarrage initial lent ou port mapping incorrect; vérifier logs et URL effective.
- **Point d’arrêt :** attendre discovery valide.

- [ ] 2.6 **CPS-KC-03** — Créer le realm de développement

- **Objectif / résultat :** realm isolé pour l’application.
- **Dépendances :** CPS-KC-02.
- **Concepts :** realm, issuer distinct, configuration reproductible.
- **Apprenant :** crée le realm dans la console locale et confirme son identifiant.
- **Fichiers Copilot :** export de développement `infra/keycloak/realm-dev.json`, sans secrets.
- **Commandes :** `$KeycloakUrl='http://localhost:8081'; $Realm='customer-dev'; Invoke-RestMethod "$KeycloakUrl/realms/$Realm/.well-known/openid-configuration"`.
- **Vérification / acceptation :** discovery répond et issuer correspond au realm arrêté.
- **Erreurs / diagnostic :** 404 = faute de realm ou création non sauvegardée; recontrôler la console.
- **Point d’arrêt :** attendre l’issuer confirmé.

- [ ] 2.7 **CPS-KC-04** — Configurer le client public OIDC

- **Objectif / résultat :** client local Authorization Code + PKCE S256.
- **Dépendances :** CPS-KC-03.
- **Concepts :** client public, redirect URI, PKCE, audience.
- **Apprenant :** crée/configure le client sans secret et valide son redirect URI loopback.
- **Fichiers Copilot :** export `infra/keycloak/realm-dev.json` et documentation du profil local.
- **Commandes :** vérifier discovery : `$Oidc=Invoke-RestMethod "$KeycloakUrl/realms/$Realm/.well-known/openid-configuration"; $Oidc.issuer; $Oidc.authorization_endpoint; $Oidc.token_endpoint; $Oidc.jwks_uri`.
- **Vérification / acceptation :** client public avec PKCE S256 requis; redirect URI exacte `http://127.0.0.1:8765/callback`; issuer et endpoints consignés.
- **Erreurs / diagnostic :** `invalid_redirect_uri` ou `unauthorized_client`; comparer caractère par caractère le redirect URI et activer le flux standard.
- **Point d’arrêt :** attendre configuration relue avant rôles.

- [ ] 2.8 **CPS-KC-05** — Définir inscription et rôles Keycloak

- **Objectif / résultat :** client inscrit comme CUSTOMER; OPERATOR attribué seulement manuellement.
- **Dépendances :** CPS-KC-04.
- **Concepts :** realm roles, rôle par défaut, auto-inscription.
- **Apprenant :** active l’inscription locale, crée utilisateur de test et affecte OPERATOR manuellement à un compte distinct.
- **Fichiers Copilot :** export realm de développement seulement.
- **Commandes :** via console Keycloak locale; vérifier rôles attribués sans afficher/copier un JWT.
- **Vérification / acceptation :** aucun utilisateur ne peut s’attribuer OPERATOR à l’inscription; CUSTOMER/OPERATOR attribuables conformément à la règle.
- **Erreurs / diagnostic :** rôle absent du token = vérifier rôle realm, scope mapper et rôle par défaut.
- **Point d’arrêt :** attendre preuve visuelle de séparation des rôles.

- [ ] 2.9 **CPS-KC-06** — Tester OIDC localement avec PKCE

- **Objectif / résultat :** vérifier l’Authorization Code Flow sans frontend applicatif et sans exposer de jeton.
- **Dépendances :** CPS-KC-04 et CPS-KC-05.
- **Concepts :** verifier/challenge S256, `state`, `nonce`, loopback redirect, échange du code.
- **Apprenant :** lance l’outil local, s’authentifie dans le navigateur et ne partage jamais code/JWT.
- **Fichiers Copilot :** `scripts/dev/oidc-pkce-login.ps1` et guide local; le script ne sort que `iss`, `sub`, `email_verified` et rôles, jamais jetons, code, verifier ni secrets.
- **Commandes :** `pwsh -NoProfile -File .\scripts\dev\oidc-pkce-login.ps1 -Issuer "http://localhost:8081/realms/customer-dev" -ClientId "customer-local"`; le script ouvre le navigateur, écoute seulement sur `127.0.0.1:8765`, génère verifier/challenge/state/nonce cryptographiquement, échange le code en mémoire, vérifie state/nonce et n’affirme pas valider localement la signature JWT; cette validation est testée par le Resource Server.
- **Vérification / acceptation :** login réussi via S256; claims allowlistés affichés sans jeton; callback et listener local se ferment.
- **Erreurs / diagnostic :** callback occupé = vérifier port 8765; `invalid_grant` = code expiré ou verifier incorrect; issuer incorrect = comparer discovery. Ne pas désactiver PKCE et ne pas activer de transcript PowerShell pendant ce test.
- **Point d’arrêt :** attendre sortie expurgée; ne jamais demander ni accepter un JWT.

## 3. Reactor Maven et applications minimales

- [ ] 3.1 **CPS-MVN-01** — Créer le reactor Maven

- **Objectif / résultat :** parent Maven valide déclarant seulement Customer et Gateway.
- **Dépendances :** CPS-DES-01, CPS-ENV-02 et CPS-ENV-03.
- **Concepts :** parent POM, modules, version Java, gestion centralisée.
- **Apprenant :** lit le POM racine et lance Maven.
- **Fichiers Copilot :** `pom.xml` racine et POM de modules minimaux.
- **Commandes :** `mvn -N validate`.
- **Vérification / acceptation :** POM valide et modules référencés; Java 21; pas de dépendances Account/Kafka/Redis/AI.
- **Erreurs / diagnostic :** POM XML invalide ou version non résolue; suivre la première erreur Maven.
- **Point d’arrêt :** attendre validation parent.

- [ ] 3.2 **CPS-APP-01** — Créer et lancer Customer minimal

- **Objectif / résultat :** application Customer vide démarre et répond au healthcheck local.
- **Dépendances :** CPS-MVN-01.
- **Concepts :** classe Spring Boot, contexte, serveur embedded.
- **Apprenant :** lance le build puis l’application et l’arrête avec Ctrl+C.
- **Fichiers Copilot :** squelette `customer-service`, configuration minimale et test de contexte.
- **Commandes :** `mvn -pl customer-service -am test`; dans un terminal : `mvn -pl customer-service spring-boot:run`; second terminal : `Invoke-WebRequest http://localhost:8082/actuator/health`.
- **Vérification / acceptation :** test contexte vert et health répond; arrêt propre; aucun appel PostgreSQL métier requis.
- **Erreurs / diagnostic :** port déjà utilisé = `Get-NetTCPConnection -LocalPort 8082`; classe principale/config = examiner rapport Surefire/logs.
- **Point d’arrêt :** attendre preuve du lancement Customer.

- [ ] 3.3 **CPS-APP-02** — Créer et lancer Gateway minimale

- **Objectif / résultat :** application Gateway vide démarre et répond au healthcheck local.
- **Dépendances :** CPS-MVN-01.
- **Concepts :** Spring Cloud Gateway/WebFlux, port d’écoute.
- **Apprenant :** exécute le test et le lancement puis arrête avec Ctrl+C.
- **Fichiers Copilot :** module `api-gateway`, configuration minimale et test de contexte.
- **Commandes :** `mvn -pl api-gateway -am test`; `mvn -pl api-gateway spring-boot:run`; second terminal : `Invoke-WebRequest http://localhost:8080/actuator/health`.
- **Vérification / acceptation :** test de contexte vert, health répond et arrêt propre.
- **Erreurs / diagnostic :** conflit MVC/WebFlux ou port occupé; lire stacktrace, vérifier `Get-NetTCPConnection -LocalPort 8080`.
- **Point d’arrêt :** attendre preuve Gateway avant Compose applicatif.

- [ ] 3.4 **CPS-APP-03** — Conteneuriser les deux applications

- **Objectif / résultat :** services Customer et Gateway démarrent dans le réseau Compose.
- **Dépendances :** CPS-APP-01, CPS-APP-02, CPS-PG-01, CPS-KC-02.
- **Concepts :** Dockerfile, DNS Compose, healthcheck, port interne vs publié.
- **Apprenant :** construit et inspecte les conteneurs.
- **Fichiers Copilot :** Dockerfiles des deux modules et `compose.yaml`.
- **Commandes :** `docker compose config`; `docker compose up -d --build customer-service api-gateway`; `docker compose ps`; `docker compose logs --tail 50 customer-service api-gateway`.
- **Vérification / acceptation :** deux applications healthy; Customer n’est pas publié directement sur le host.
- **Erreurs / diagnostic :** URL interne doit utiliser nom Compose (`postgres`, `keycloak`), non `localhost`; examiner logs.
- **Point d’arrêt :** attendre readiness avant Flyway ou routage.

## 4. PostgreSQL, Flyway et modèle

- [ ] 4.1 **CPS-DB-01** — Configurer Flyway

- **Objectif / résultat :** Flyway exécute une migration de preuve sur PostgreSQL de test.
- **Dépendances :** CPS-APP-01, CPS-PG-01.
- **Concepts :** migration versionnée, datasource et fail-fast.
- **Apprenant :** lance le test de migration et lit la sortie Flyway.
- **Fichiers Copilot :** POM/configuration de `customer-service`, migration de preuve ou migration Customer si déjà prête.
- **Commandes :** `mvn -pl customer-service -am verify`; `docker compose up -d postgres`; `docker compose logs --tail 50 customer-service`.
- **Vérification / acceptation :** Flyway est détecté; migration de preuve exécutée sur PostgreSQL local et version enregistrée; application échoue visiblement en cas de migration invalide.
- **Erreurs / diagnostic :** datasource incorrecte ou mécanismes concurrents `schema.sql`; vérifier logs Flyway/datasource.
- **Point d’arrêt :** attendre test Flyway réussi avant modèle métier.

- [ ] 4.2 **CPS-DB-02** — Créer la table Customer et ses contraintes

- **Objectif / résultat :** schéma propre au service avec unicité prouvée.
- **Dépendances :** CPS-DB-01.
- **Concepts :** types PostgreSQL, contrainte unique, UTC, e-mail canonique.
- **Apprenant :** lit le SQL et examine les tests réels PostgreSQL.
- **Fichiers Copilot :** `V1__create_customer.sql` et test de migration.
- **Commandes :** `mvn -pl customer-service -am verify`.
- **Vérification / acceptation :** `id`, `sub`, `name`, email canonique et `created_at`; unicité de `sub` et e-mail insensible à la casse appliquée et inspectable sur PostgreSQL local; Testcontainers revalide ces contraintes dans CPS-TEST-02.
- **Erreurs / diagnostic :** ne pas valider avec H2; vérifier index/canonicalisation avec deux emails dont la casse diffère.
- **Point d’arrêt :** attendre contraintes prouvées.

- [ ] 4.3 **CPS-DOM-01** — Ajouter le modèle et le repository Customer

- **Objectif / résultat :** accès persistant Customer derrière modèle/repository, sans exposer l’entité.
- **Dépendances :** CPS-DB-02.
- **Concepts :** entité vs DTO, repository, couche applicative.
- **Apprenant :** lit mapping et lance tests unitaires ciblés.
- **Fichiers Copilot :** entité, repository, service minimal et tests unitaires.
- **Commandes :** `mvn -pl customer-service "-Dtest=*Customer*Test" test`.
- **Vérification / acceptation :** mapping déterministe et entité non utilisée comme contrat HTTP.
- **Erreurs / diagnostic :** test unitaire qui exige DB est mal isolé; déplacer le cas au test d’intégration.
- **Point d’arrêt :** attendre test ciblé avant API.

## 5. Contrats REST et sécurité

- [ ] 5.1 **CPS-API-01** — Implémenter les erreurs RFC 9457

- **Objectif / résultat :** gestionnaire d’erreurs REST commun conforme au contrat.
- **Dépendances :** CPS-APP-01.
- **Concepts :** exception mapping, code stable, correlation ID.
- **Apprenant :** compare le JSON à la spec et vérifie absence d’informations sensibles.
- **Fichiers Copilot :** gestionnaire d’exceptions, DTO d’erreur et tests MockMvc.
- **Commandes :** `mvn -pl customer-service -am test`.
- **Vérification / acceptation :** `application/problem+json`, `code`, `correlationId`, `errors` optionnel; aucun SQL, stack trace, token ou secret.
- **Erreurs / diagnostic :** ne pas refléter un correlation ID fourni non valide; contrôler le corps d’erreur exact.
- **Point d’arrêt :** attendre validation avant endpoints.

- [ ] 5.2 **CPS-SEC-01** — Valider les JWT dans customer-service

- **Objectif / résultat :** Resource Server rejette jetons invalides indépendamment de la Gateway.
- **Dépendances :** CPS-KC-04, CPS-KC-05, CPS-KC-06, CPS-APP-01.
- **Concepts :** signature, issuer, expiration, audience, claim roles.
- **Apprenant :** exécute tests avec jetons de test; conserve le token OIDC local, ne le partage jamais.
- **Fichiers Copilot :** configuration Security et tests de sécurité Customer.
- **Commandes :** `mvn -pl customer-service -am test`.
- **Vérification / acceptation :** token absent/expiré/mauvais issuer/audience refusé; `sub` dérivé du principal vérifié; rôles issus de `realm_access.roles`; header libre ignoré.
- **Erreurs / diagnostic :** 401 = authentification/validation; 403 = rôle insuffisant; comparer issuer/audience aux métadonnées et au client.
- **Point d’arrêt :** attendre matrice de sécurité verte.

- [ ] 5.3 **CPS-GW-01** — Sécuriser et router la Gateway

- **Objectif / résultat :** validation JWT en bordure et routes statiques Customer uniquement.
- **Dépendances :** CPS-APP-02, CPS-SEC-01, CPS-APP-03, CPS-KC-04.
- **Concepts :** filtres Gateway, bearer forwarding, surface exposée.
- **Apprenant :** lance tests Gateway et appelle route protégée sans token.
- **Fichiers Copilot :** config Gateway et tests de routage/sécurité.
- **Commandes :** `mvn -pl api-gateway -am test`; après démarrage, `Invoke-WebRequest http://localhost:8080/api/v1/customers`.
- **Vérification / acceptation :** appel sans token 401; routes Customer statiques; token non réécrit en identité par header; aucune route Actuator exposée.
- **Erreurs / diagnostic :** 404 peut signifier route absente, 401 filtre; examiner logs Gateway sans token.
- **Point d’arrêt :** attendre preuve du refus avant API métier.

- [ ] 5.4 **CPS-API-02** — Valider les champs du profil

- **Objectif / résultat :** validator du DTO applique les décisions retenues.
- **Dépendances :** CPS-API-01, CPS-SEC-01.
- **Concepts :** validation Jakarta, données serveur, normalisation.
- **Apprenant :** examine cas limites nom et champs réservés.
- **Fichiers Copilot :** DTO création, validator et tests.
- **Commandes :** `mvn -pl customer-service -am test`.
- **Vérification / acceptation :** `name` trimé, longueur 1–120; `sub`/email dans le corps rejetés `VALIDATION_FAILED`; email absent/non vérifié `EMAIL_NOT_VERIFIED`.
- **Erreurs / diagnostic :** tester longueur après trim et claim absent distinct du champ invalide.
- **Point d’arrêt :** attendre tests DTO avant POST.

- [ ] 5.5 **CPS-API-03** — Créer un profil Customer

- **Objectif / résultat :** `POST /api/v1/customers` réalise l’auto-inscription et renvoie 201.
- **Dépendances :** CPS-DOM-01, CPS-SEC-01, CPS-API-01, CPS-API-02, CPS-DB-02.
- **Concepts :** contrôleur, DTO, transaction, `Location`.
- **Apprenant :** lance tests MockMvc du cas nominal et du rôle interdit.
- **Fichiers Copilot :** contrôleur, service applicatif, mapping et tests.
- **Commandes :** `mvn -pl customer-service -am test`.
- **Vérification / acceptation :** profil lié au `sub`/email du token, HTTP 201 avec représentation et `Location`; OPERATOR reçoit 403 `FORBIDDEN_PROFILE_CREATION` et aucune écriture.
- **Erreurs / diagnostic :** claim absent vs contexte Security; examiner logs corrélés et ne jamais imprimer bearer.
- **Point d’arrêt :** attendre cas nominaux et refus avant conflits.

- [ ] 5.6 **CPS-API-04** — Traduire les conflits d’identité

- **Objectif / résultat :** conflits `sub` et e-mail donnent erreurs stables.
- **Dépendances :** CPS-API-03, CPS-DB-02.
- **Concepts :** contrainte unique, traduction sélective d’exception.
- **Apprenant :** exécute tests de doublons et vérifie que l’existant ne change pas.
- **Fichiers Copilot :** traduction des contraintes et tests ciblés.
- **Commandes :** `mvn -pl customer-service -am test`.
- **Vérification / acceptation :** `sub` existant => 409 `CUSTOMER_PROFILE_EXISTS`; email existant => 409 `EMAIL_ALREADY_EXISTS`; aucun email absent/non vérifié n’est traité comme erreur générique (`EMAIL_NOT_VERIFIED`).
- **Erreurs / diagnostic :** ne pas convertir toute `DataIntegrityViolationException` en doublon email; inspecter la contrainte.
- **Point d’arrêt :** attendre les deux tests de conflit.

## 6. Idempotence et concurrence

- [ ] 6.1 **CPS-IDEM-01** — Persister le résultat idempotent

- **Objectif / résultat :** enregistrer clé et réponse de succès dans la transaction Customer.
- **Dépendances :** CPS-API-03, CPS-DB-02.
- **Concepts :** hash canonique SHA-256, contrainte composée, atomicité.
- **Apprenant :** vérifie migration et test de rollback.
- **Fichiers Copilot :** migration idempotence, modèle/service et tests transactionnels.
- **Commandes :** `mvn -pl customer-service -am verify`.
- **Vérification / acceptation :** portée acteur/opération/service; réponse HTTP/corps stockés avec la ressource; expiration UTC de 7 jours; aucun token persisté/loggé.
- **Erreurs / diagnostic :** ressource sans clé ou clé sans ressource révèle transaction non atomique.
- **Point d’arrêt :** attendre la preuve du commit atomique.

- [ ] 6.2 **CPS-IDEM-02** — Tester rejeu, conflit et expiration

- **Objectif / résultat :** vérifier les cas séquentiels d’idempotence.
- **Dépendances :** CPS-IDEM-01.
- **Concepts :** replay exact, conflit de contenu, expiration.
- **Apprenant :** exécute tests ciblés; aucun délai réel de sept jours.
- **Fichiers Copilot :** tests idempotence et horloge contrôlable si nécessaire.
- **Commandes :** `mvn -pl customer-service "-Dtest=*Idempotency*Test" test`.
- **Vérification / acceptation :** même clé/contenu rejoue statut/corps; autre contenu 409; absent/vide/>255 = 400; expiration autorise nouvelle commande.
- **Erreurs / diagnostic :** vérifier empreinte insensible à l’ordre JSON mais sensible aux valeurs fonctionnelles.
- **Point d’arrêt :** attendre résultats séquentiels avant concurrence.

- [ ] 6.3 **CPS-IDEM-03** — Tester les requêtes concurrentes

- **Objectif / résultat :** prouver l’unicité en course sur PostgreSQL.
- **Dépendances :** CPS-IDEM-02, CPS-DB-02.
- **Concepts :** isolation/contrainte unique et concurrence multi-instance.
- **Apprenant :** lance tests avec barrières concurrentes.
- **Fichiers Copilot :** test d’intégration concurrente.
- **Commandes :** `mvn -pl customer-service "-Dtest=*Concurrent*Test" test`.
- **Vérification / acceptation :** N demandes identiques produisent un Customer et toutes reçoivent la réponse initiale, sans 500 parasite.
- **Erreurs / diagnostic :** tests séquentiels/H2 ne prouvent pas la concurrence; confirmer PostgreSQL Testcontainers et synchronisation effective.
- **Point d’arrêt :** attendre résultats avant lecture.

## 7. Lectures, tests d’intégration et bout en bout

- [ ] 7.1 **CPS-API-05** — Lister le profil du client

- **Objectif / résultat :** CUSTOMER ne liste que son profil.
- **Dépendances :** CPS-SEC-01, CPS-API-03.
- **Concepts :** filtrage par identité au service propriétaire.
- **Apprenant :** exécute tests avec client ayant profil et client sans profil.
- **Fichiers Copilot :** endpoint `GET /api/v1/customers`, tests service/API.
- **Commandes :** `mvn -pl customer-service -am test`.
- **Vérification / acceptation :** réponse contient uniquement profil lié à `sub`, ou page vide; aucun filtre identité choisi par client.
- **Erreurs / diagnostic :** filtrage fait uniquement par Gateway est refusé; tester service propriétaire.
- **Point d’arrêt :** attendre matrice CUSTOMER avant détail.

- [ ] 7.2 **CPS-API-06** — Consulter un Customer par identifiant

- **Objectif / résultat :** détail propriétaire avec masquage 404.
- **Dépendances :** CPS-API-05.
- **Concepts :** autorisation objet, prévention de l’énumération.
- **Apprenant :** vérifie profil propre, étranger et absent.
- **Fichiers Copilot :** endpoint `GET /api/v1/customers/{id}` et tests.
- **Commandes :** `mvn -pl customer-service -am test`.
- **Vérification / acceptation :** client lit le sien; absent et étranger donnent même HTTP 404 et contrat d’erreur.
- **Erreurs / diagnostic :** comparer status et corps des deux 404; ne pas révéler l’existence.
- **Point d’arrêt :** attendre les deux cas vérifiés.

- [ ] 7.3 **CPS-API-07** — Paginer la liste opérateur

- **Objectif / résultat :** liste globale OPERATOR triée/paginée selon contrats.
- **Dépendances :** CPS-API-05, CPS-SEC-01.
- **Concepts :** pagination zéro-based, tri stable, métadonnées.
- **Apprenant :** teste page 0, taille par défaut, 100, 101 et valeurs négatives.
- **Fichiers Copilot :** pagination Customer et tests MockMvc.
- **Commandes :** `mvn -pl customer-service -am test`.
- **Vérification / acceptation :** OPERATOR voit tous; défaut 20; max 100; ordre `createdAt DESC, id ASC`; invalides HTTP 400.
- **Erreurs / diagnostic :** vérifier ordre sur timestamps égaux et code `INVALID_PAGE_SIZE`; aucune troncature silencieuse.
- **Point d’arrêt :** attendre résultats sur limites.

- [ ] 7.4 **CPS-TEST-01** — Compléter les tests unitaires

- **Objectif / résultat :** règles du domaine testées sans infrastructure.
- **Dépendances :** CPS-DOM-01, CPS-API-02, CPS-API-04, CPS-API-07.
- **Concepts :** tests unitaires vs web/persistance.
- **Apprenant :** exécute suite sans containers.
- **Fichiers Copilot :** tests unitaires seulement.
- **Commandes :** `mvn -pl customer-service -Dtest='*Test' test`.
- **Vérification / acceptation :** validation, canonicalisation, mapping, codes et pagination couverts; pas de DB ou Keycloak requis.
- **Erreurs / diagnostic :** flakiness/ordre-dépendance; rejouer les tests et isoler fixtures.
- **Point d’arrêt :** attendre suite unitaire avant intégration.

- [ ] 7.5 **CPS-TEST-02** — Valider PostgreSQL, Flyway et Keycloak avec Testcontainers

- **Objectif / résultat :** une suite d’intégration isolée valide migrations, contraintes et OIDC réel.
- **Dépendances :** CPS-DB-02, CPS-KC-05, CPS-SEC-01, CPS-IDEM-03, CPS-ENV-05.
- **Concepts :** containers éphémères, JWKS/issuer réels, isolation des tests.
- **Apprenant :** vérifie daemon puis lance le profil intégré.
- **Fichiers Copilot :** tests PostgreSQL et Keycloak Testcontainers, configuration test dédiée.
- **Commandes :** `docker info`; `mvn -pl customer-service -Pintegration-tests verify`.
- **Vérification / acceptation :** PostgreSQL et Keycloak démarrés/nettoyés automatiquement; migration, JWT, rôles, unicité et idempotence validés; aucun serveur partagé.
- **Erreurs / diagnostic :** accès registry, daemon ou incompatibilité issuer; lire logs Testcontainers sans désactiver la validation.
- **Point d’arrêt :** attendre logs/tests complets avant E2E.

- [ ] 7.6 **CPS-E2E-01** — Valider le parcours via Gateway

- **Objectif / résultat :** vérifier inscription OIDC et US-01..03 de bout en bout.
- **Dépendances :** CPS-GW-01, CPS-API-03..07, CPS-IDEM-03, CPS-TEST-02.
- **Concepts :** corrélation multi-services et distinction 401/403/404.
- **Apprenant :** démarre Compose et utilise le helper PKCE avec `-ApiBaseUrl "http://localhost:8080" -CreateProfileName "Customer Demo" -IdempotencyKey "demo-profile-001"`; le bearer token reste en mémoire et les appels n’affichent que statut, Location et corps de réponse; ne partage jamais le JWT.
- **Fichiers Copilot :** corrections strictement liées à un défaut reproduit.
- **Commandes :** `docker compose up -d --build`; `docker compose ps`; `docker compose logs --tail 80`; `pwsh -NoProfile -File .\scripts\dev\oidc-pkce-login.ps1 -Issuer "http://localhost:8081/realms/customer-dev" -ClientId "customer-local" -ApiBaseUrl "http://localhost:8080" -CreateProfileName "Customer Demo" -IdempotencyKey "demo-profile-001"`.
- **Vérification / acceptation :** création 201/Location, lecture propre, refus OPERATOR à la création, accès croisé 404, Gateway seule entrée exposée.
- **Erreurs / diagnostic :** 401 issuer/audience, 404 route/masquage, 503 PostgreSQL; corréler les logs sans tokens.
- **Point d’arrêt :** attendre vos résultats et validation explicite.

- [ ] 7.7 **CPS-E2E-02** — Vérifier redémarrage et persistance

- **Objectif / résultat :** Customer conservé après restart des services.
- **Dépendances :** CPS-E2E-01.
- **Concepts :** état durable PostgreSQL vs processus applicatifs.
- **Apprenant :** redémarre sans effacer les volumes puis répète le GET.
- **Fichiers Copilot :** aucun, sauf défaut démontré.
- **Commandes :** `docker compose restart postgres customer-service api-gateway`; `docker compose ps`; répéter GET via Gateway avec nouvelle authentification.
- **Vérification / acceptation :** profil toujours présent et services reviennent healthy.
- **Erreurs / diagnostic :** ne jamais utiliser `down -v`; inspecter logs si healthcheck échoue.
- **Point d’arrêt :** attendre confirmation de persistance.

## 8. Documentation et revue de fin

- [ ] 8.1 **CPS-DOC-01** — Documenter l’incrément vérifié

- **Objectif / résultat :** guide reproductible fondé sur exécution réelle.
- **Dépendances :** CPS-E2E-02.
- **Concepts :** documentation de faits observés, configuration sans secrets.
- **Apprenant :** rejoue les commandes depuis environnement arrêté.
- **Fichiers Copilot :** guide local convenu lors de CPS-DES-01; aucune change historique/backlog global.
- **Commandes :** commandes du guide puis `openspec validate implement-customer-profile-vertical-slice`.
- **Vérification / acceptation :** chaque commande a été exécutée; résultats/écarts consignés; aucune valeur secrète.
- **Erreurs / diagnostic :** étapes implicites ou version d’outil absente; faire relire le guide depuis terminal neuf.
- **Point d’arrêt :** attendre votre revue finale; aucune tâche suivante n’est démarrée automatiquement.
