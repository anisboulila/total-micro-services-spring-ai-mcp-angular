# ADR / Trade-offs — Étape 3 — Décisions d’architecture proposées

## Cadre de lecture

Ces ADR rendent les décisions de `define-target-system-design/design.md`
examinables; ils ne redéfinissent pas l’architecture cible. Les exigences
UC/NFR de l’Étape 1 restent candidates, sauf CON-1 à CON-3 explicitement
**DÉCIDÉES**. Les éléments attribués à Youssfi sont des constats **OBSERVÉS**
dans la référence et non dans le workspace local.

Le champ **Statut** décrit la posture de décision proposée par l’Étape 2,
pas l’approbation de cette change. Ainsi, une fiche « proposé » peut
transcrire un `DEC-xx` **DÉCIDÉ**; ses dépendances métier demeurent
**À CONFIRMER**.

## Regroupements

- **ADR-01** réunit le style et les frontières de services (DEC-01/02), car
  leur justification repose sur le même arbitrage entre séparation de domaines
  et coût de distribution.
- **ADR-02** réunit propriété, persistance et cohérence (DEC-03/04/05) : la
  base par service découle directement de la propriété et conditionne le
  modèle de cohérence.
- **ADR-03/04** séparent le choix des modes de communication de la fiabilité
  des événements. L’outbox et l’idempotence forment un problème de cohérence
  transactionnelle autonome, mais restent conditionnelles à H-6.
- **ADR-05/06** séparent les responsabilités de la passerelle du choix de
  découverte/configuration. Elles ont des problèmes et conditions de révision
  différents.
- **ADR-10/11** gardent observabilité et stratégie de test distinctes : l’une
  aide au diagnostic en exploitation, l’autre vérifie les comportements et
  contrats.
- **ADR-12** regroupe assistant, Spring AI, MCP, Agent et RAG comme une seule
  frontière de capacité conversationnelle conditionnée par UC-07.
- **ADR-13** relie le cycle de livraison local/CI à l’orchestration, tout en
  gardant Kubernetes conditionnel et Helm différé.

## ADR-01 — Services orientés domaine plutôt que monolithe modulaire

- **Statut : proposé** (DEC-01/02 : **DÉCIDÉ**, sous conditions indiquées).
- **Contexte et problème :** séparer les responsabilités Customer et Account
  sans multiplier artificiellement les services. H-1 et l’inclusion des
  UC-01..06 restent à confirmer; le besoin de scalabilité indépendante n’est
  pas démontré.
- **Décision et justification :** conserver un petit ensemble de services
  orientés domaine, avec deux services métier pour Customer et Account, motivé
  aussi par l’objectif pédagogique **DÉCIDÉ** (CON-2). L’assistant n’existe que
  si UC-07 est confirmé; le consommateur d’audit n’existe que si H-6 est
  confirmé.
- **Alternatives considérées :** monolithe modulaire; séparation Customer /
  Account; multiplication de microservices.
- **Avantages, inconvénients et compromis :** frontières et propriété métier
  visibles, pratique des concepts distribués; en contrepartie, latence réseau,
  pannes partielles, cohérence distribuée et charge d’exploitation. Le
  monolithe modulaire est plus simple pour un périmètre aussi réduit.
- **Conséquences techniques et opérationnelles :** contrats inter-services,
  déploiement et diagnostic séparés; repli vers un monolithe modulaire si H-1
  est faux ou si les frontières n’apportent pas de valeur.
- **Réévaluation :** validation du périmètre UC-01..06, évolution des domaines,
  coûts d’exploitation ou absence de valeur pédagogique/pratique démontrée.
- **Références :** CON-1/CON-2; UC-01..06 **OBSERVÉS** dans la référence,
  inclusion produit **À CONFIRMER**; DEC-01, DEC-02.

## ADR-02 — Propriété des données par service et PostgreSQL

- **Statut : proposé** (DEC-03/04/05 : **DÉCIDÉ**).
- **Contexte et problème :** préserver l’intégrité et l’autonomie des données
  sans base partagée ni transactions distribuées.
- **Décision et justification :** Customer possède ses clients; Account possède
  ses comptes et ne conserve qu’une référence opaque `customerId`. Chaque
  service utilise PostgreSQL dans une base logique distincte. Les transactions
  sont locales; la création d’un compte valide le client par API. Aucun
  besoin de transaction distribuée n’est identifié.
- **Alternatives considérées :** base partagée; H2 en mémoire comme référence
  locale uniquement; MySQL/MariaDB; MongoDB; lectures directes inter-services;
  XA/2PC.
- **Avantages, inconvénients et compromis :** autonomie, intégrité
  relationnelle et montants exacts; en contrepartie, pas de jointure
  inter-services, migrations par service et cohérence momentanément divergente.
  PostgreSQL et la séparation sont aussi des choix de conception, pas des
  exigences métier confirmées.
- **Conséquences techniques et opérationnelles :** aucune clé étrangère
  inter-bases ni accès direct à la base d’un autre service; appels API ou
  événements pour les données externes; migrations versionnées à choisir.
- **Réévaluation :** changement des règles de cycle de vie client-compte,
  besoins de consultation composée, contraintes de déploiement ou exigences
  produit validées.
- **Références :** UC-01..06 (besoins candidats); NFR-2 **DÉDUIT / À
  CONFIRMER**; DEC-03, DEC-04, DEC-05.

## ADR-03 — REST synchrone par défaut, événements Kafka conditionnels

- **Statut : conditionnel** pour Kafka; REST est **DÉCIDÉ** comme défaut
  (DEC-06/07).
- **Contexte et problème :** répondre aux requêtes qui exigent un résultat
  immédiat sans utiliser des appels synchrones pour des réactions découplées.
  La référence montre Feign sur HTTP/REST; aucun événement métier/Kafka n’y a
  été observé.
- **Décision et justification :** REST synchrone pour UC-01..06 et la
  validation Account → Customer. Kafka ne serait introduit que si H-6
  (consommateur utile, p. ex. audit/notification/lecture) est confirmé; pas de
  commande ni de requête métier via Kafka.
- **Alternatives considérées :** REST seul; tout asynchrone; Kafka; RabbitMQ;
  événements in-process; gRPC.
- **Avantages, inconvénients et compromis :** REST donne une réponse immédiate
  et reste simple; le lien synchrone couple latence et disponibilité.
  Kafka découple producteurs et consommateurs et permet le rejeu, mais ajoute
  broker, cohérence éventuelle et exploitation.
- **Conséquences techniques et opérationnelles :** contrats REST versionnés,
  DTO, validation, erreurs cohérentes et pagination; client HTTP déclaratif
  (Feign ou équivalent) laissé au choix d’implémentation. Les événements ne
  sont pas une exigence métier acquise.
- **Réévaluation :** confirmation ou rejet de H-6, nouveau besoin asynchrone
  multi-consommateurs, ou coût de disponibilité/latence REST devenu
  inacceptable.
- **Références :** UC-01..06 **OBSERVÉS** dans la référence / périmètre cible
  **À CONFIRMER**; DEC-06, DEC-07; H-6 **À CONFIRMER**.

## ADR-04 — Fiabilité événementielle : outbox et traitement idempotent

- **Statut : conditionnel** à H-6 (DEC-07; règles complémentaires DEC-11/22).
- **Contexte et problème :** éviter la perte d’un événement entre transaction
  métier et publication, et tolérer les redélivraisons d’une livraison au
  moins une fois.
- **Décision et justification :** si Kafka et un consommateur sont confirmés,
  écrire l’événement dans une outbox au sein de la transaction locale, puis le
  publier via un relais. Accepter une livraison au moins une fois, attribuer
  un identifiant unique à chaque événement, activer l’idempotence producteur
  et traiter les doublons de façon idempotente côté consommateur. L’outbox ne
  garantit pas une livraison exactement une fois.
- **Alternatives considérées :** publication directe après commit; transaction
  distribuée; pas d’événements; consommateur non idempotent.
- **Avantages, inconvénients et compromis :** réduit le risque de divergence
  entre données et événement et rend les reprises sûres; ajoute table, relais,
  stockage des marqueurs traités, surveillance du retard et gestion des échecs.
- **Conséquences techniques et opérationnelles :** schéma d’événement versionné,
  retries bornés, file d’échecs et ordre limité à la partition/agrégat selon
  DEC-07; consommateur d’audit concret **À CONFIRMER**. Sans consommateur
  validé, ne pas introduire le broker en production-like.
- **Réévaluation :** H-6 non confirmé, changement de garantie de livraison,
  exigences de rétention/rejeu ou volume/coût d’exploitation mesuré.
- **Références :** H-6 **À CONFIRMER**; DEC-07; DEC-11 (idempotence des API,
  traitée séparément dans ADR-08); DEC-22.

## ADR-05 — API Gateway comme point d’entrée unique

- **Statut : proposé** (DEC-08 : **DÉCIDÉ**).
- **Contexte et problème :** fournir aux clients une entrée commune et
  centraliser le routage, CORS, validation initiale du jeton et limites
  d’exposition, sans déplacer la logique métier hors des services.
- **Décision et justification :** retenir Spring Cloud Gateway avec routes
  statiques explicites pour un jeu de services petit et stable. La référence
  utilise un Gateway, mais ses routes et leur fonctionnement ne constituent
  pas une preuve de besoin ou de succès runtime cible.
- **Alternatives considérées :** accès direct aux services; reverse proxy /
  ingress seul; backend-for-frontend; absence de Gateway.
- **Avantages, inconvénients et compromis :** point d’entrée et politiques
  communes; ajoute un saut réseau et un composant critique. Le Gateway ne
  remplace ni l’autorisation métier ni la validation dans les services.
- **Conséquences techniques et opérationnelles :** routes explicites et
  supervision; redondance seulement si un objectif de disponibilité le
  justifie; aucun service métier exposé directement dans le modèle cible.
- **Réévaluation :** évolution de la topologie, apparition de plusieurs
  frontends ou démonstration que les responsabilités centralisées ne valent
  pas le coût.
- **Références :** NFR-1 **DÉDUIT / À CONFIRMER**; DEC-08; référence
  **OBSERVÉE** dans `project-foundation-architecture-baseline/design.md`.

## ADR-06 — DNS et configuration d’environnement plutôt qu’Eureka/Config Server

- **Statut : proposé** (non-adoption DEC-09/14 : **DÉCIDÉ**).
- **Contexte et problème :** résoudre les services et fournir la configuration
  sans ajouter un registre ou un serveur central quand le déploiement est
  petit et stable.
- **Décision et justification :** utiliser les noms DNS stables de
  l’environnement d’exécution; externaliser la configuration dans
  l’environnement/profils et injecter les secrets au runtime. Ne pas retenir
  Eureka ni Spring Cloud Config Server à ce stade. La présence de clients
  Eureka/Config dans la référence ne prouve pas leur utilité cible.
- **Alternatives considérées :** Eureka; Consul; DNS; Spring Cloud Config
  Server; Vault pour les secrets.
- **Avantages, inconvénients et compromis :** moins de composants et de
  dépendances d’exploitation; pas d’enregistrement dynamique ni de
  rafraîchissement central à chaud, et les noms doivent rester stables.
- **Conséquences techniques et opérationnelles :** config non secrète séparée
  des secrets; secrets jamais versionnés ni journalisés; redémarrage requis
  pour certaines modifications.
- **Réévaluation :** instances dynamiques hors orchestration, nombreux
  environnements/services, besoin de rafraîchissement centralisé ou exigences
  de rotation/audit des secrets.
- **Références :** CON-3 **DÉCIDÉ**; DEC-09, DEC-14.

## ADR-07 — Identité OIDC et validation JWT par les services

- **Statut : proposé** (DEC-10 : **DÉCIDÉ**; rôles **À CONFIRMER**).
- **Contexte et problème :** protéger les données candidates client/compte et
  ne pas traiter le Gateway comme l’unique frontière de confiance.
- **Décision et justification :** Keycloak fournit l’identité OIDC et émet les
  jetons; Spring Security Resource Server valide les JWT dans le Gateway et
  indépendamment dans chaque service. L’autorisation métier s’applique dans
  les services. Les rôles, permissions et propagation d’identité exacte
  restent à confirmer.
- **Alternatives considérées :** pas d’authentification; sessions; JWT maison;
  Spring Authorization Server; fournisseur cloud.
- **Avantages, inconvénients et compromis :** standards établis, validation
  locale par clés publiques et défense en profondeur; composant d’identité à
  opérer, configuration des clients/rôles et répétition de configuration de
  sécurité.
- **Conséquences techniques et opérationnelles :** obtention de jetons
  dépendante de Keycloak; validation locale des jetons déjà émis; secrets
  d’intégration séparés de la configuration; protections de MCP conditionnées
  à UC-07.
- **Réévaluation :** public, rôles, confidentialité ou déploiement confirmé
  incompatibles avec l’hypothèse d’utilisateurs authentifiés; exigence de
  disponibilité ou de fédération différente.
- **Références :** NFR-1 **DÉDUIT / À CONFIRMER**, H-4 **À CONFIRMER**; DEC-10,
  DEC-14.

## ADR-08 — Résilience des API et idempotence des créations

- **Statut : proposé** (DEC-11 : **DÉCIDÉ**; paramètres **À CONFIRMER**).
- **Contexte et problème :** limiter les cascades de panne sans transformer
  une défaillance en résultat métier fictif ni créer de doublons lors d’un
  rejeu.
- **Décision et justification :** imposer des timeouts sortants, retries bornés
  avec backoff uniquement pour les opérations sûres (les lectures); ne jamais
  réessayer une création sans mécanisme d’idempotence effectif. Appliquer un
  circuit breaker aux dépendances pertinentes et refuser explicitement la
  création de compte si le client n’est pas validé. Pour chaque POST de
  création, le client fournit une clé d’idempotence : même clé et même requête
  rejouent le résultat initial; même clé avec un contenu différent est rejetée;
  en concurrence, une seule création a lieu et les autres attendent, rejouent
  le résultat ou reçoivent un conflit explicite.
- **Alternatives considérées :** appels sans garde-fous; retries illimités;
  fallback synthétique; copie locale de l’état client; absence de clé
  d’idempotence.
- **Avantages, inconvénients et compromis :** protège l’intégrité, les
  ressources et les créations contre les doubles soumissions; certaines
  écritures échouent lorsque Customer est indisponible, choix privilégiant
  NFR-2 candidate plutôt que la disponibilité d’écriture.
- **Conséquences techniques et opérationnelles :** clé stockée dans la base
  du service propriétaire, avec une contrainte d’unicité garantie en base et
  écrite dans la même transaction locale que la création; une vérification
  préalable seule ne suffit pas. La portée de l’unicité au-delà de cette base
  n’est pas précisée par DEC-11. Durée de conservation, codes de conflit
  (409/422), seuils et paramètres de retry restent **À CONFIRMER**. Les
  créations ne sont pas réessayées par défaut; elles ne peuvent l’être que si
  la clé est effectivement appliquée. Ne pas confondre idempotence des API
  avec celle des consommateurs Kafka (ADR-04).
- **Réévaluation :** priorité de disponibilité différente, règles métier
  modifiées, charge/latence mesurées ou politique d’idempotence métier
  précisée.
- **Références :** NFR-2 **DÉDUIT / À CONFIRMER**; DEC-11; H-3 **À CONFIRMER**.

## ADR-09 — Redis limité, choix pédagogique sans besoin de performance démontré

- **Statut : proposé** (DEC-12 : choix **DÉCIDÉ** à objectif pédagogique).
- **Contexte et problème :** décider si un cache distribué apporte assez de
  valeur lorsque aucun profil de charge ne démontre un problème de performance.
- **Décision et justification :** retenir Redis principalement pour démontrer
  un cache partagé entre plusieurs instances; périmètre initial limité aux
  lectures de clients. PostgreSQL reste la source de vérité; le cache n’est
  pas nécessaire à la justesse des opérations et les clés d’idempotence restent
  dans PostgreSQL.
- **Alternatives considérées :** aucun cache; Ehcache/Caffeine local par
  instance; réplicas de lecture PostgreSQL; Redis distribué.
- **Avantages, inconvénients et compromis :** objectif d’apprentissage
  concret et partage entre instances; coût d’un composant supplémentaire,
  données temporairement périmées, invalidation et politique d’indisponibilité
  à définir. Aucun gain de performance métier n’est établi.
- **Conséquences techniques et opérationnelles :** cache initial de lectures
  client avec TTL **À CONFIRMER**. Si une modification client entre dans le
  périmètre (H-2, actuellement **À CONFIRMER**), invalider ou mettre à jour le
  cache concerné. En cas d’indisponibilité de Redis, privilégier une lecture
  PostgreSQL lorsque cela est possible; la politique exacte reste
  **À CONFIRMER**. Hors périmètre initial : cache de soldes, sessions et
  limitation de débit distribuée; mesurer avant d’élargir.
- **Réévaluation :** mesures de charge et de latence, coût opérationnel,
  nécessité démontrée de cohérence du cache ou confirmation que l’objectif
  pédagogique ne justifie plus Redis.
- **Références :** CON-2 **DÉCIDÉ**; NFR-4 **DÉDUIT / À CONFIRMER**; DEC-12.

## ADR-10 — Observabilité progressive avec standards ouverts

- **Statut : proposé** (DEC-13 : **DÉCIDÉ**; backends **À CONFIRMER**).
- **Contexte et problème :** diagnostiquer les flux répartis entre services
  sans considérer la présence d’Actuator dans la référence comme une
  observabilité effective.
- **Décision et justification :** logs structurés corrélés, métriques
  applicatives via Micrometer et scraping, et traces distribuées via
  OpenTelemetry; introduire les backends par paliers. Prometheus/Grafana est
  une option par défaut envisagée pour les tableaux de bord, pas une exigence
  confirmée; le backend de traces reste à choisir.
- **Alternatives considérées :** logs seuls; plateforme hébergée; stack locale
  Prometheus/Grafana et backend de traces Jaeger/Tempo à confirmer.
- **Avantages, inconvénients et compromis :** diagnostic transversal et
  instrumentation portable; charge de stockage, configuration, exposition et
  exploitation supplémentaires.
- **Conséquences techniques et opérationnelles :** protéger données sensibles
  dans les logs; sélectionner les backends et la rétention lorsque le
  déploiement est connu; aucune cible chiffrée d’observabilité n’est fixée.
- **Réévaluation :** besoins de support/exploitation, volumétrie, coût ou
  exigences de rétention et d’alerte confirmés.
- **Références :** NFR-4 **DÉDUIT / À CONFIRMER**; DEC-13; CON-3 **DÉCIDÉ**.

## ADR-11 — Pyramide de tests adaptée aux frontières

- **Statut : proposé** (DEC-22 : **DÉCIDÉ**; outils **À CONFIRMER**).
- **Contexte et problème :** vérifier les règles métier et interactions
  distribuées sans prendre les tests ou le comportement de la référence pour
  preuve du système cible.
- **Décision et justification :** tests unitaires de domaine, tests de tranche,
  intégration avec dépendances réelles en conteneurs, contrats REST/événements,
  résilience et sécurité, quelques tests de bout en bout et tests IA
  déterministes/sur échantillons si UC-07 est confirmé.
- **Alternatives considérées :** tests unitaires seuls; tests manuels/bout en
  bout uniquement; simulations de toutes les dépendances.
- **Avantages, inconvénients et compromis :** équilibre entre rapidité,
  couverture des contrats et confiance en intégration; l’intégration par
  conteneurs et les tests bout en bout allongent la CI et demandent des
  ressources.
- **Conséquences techniques et opérationnelles :** Testcontainers ou
  équivalent, outils de contrat et stratégie de jeux IA restent à choisir;
  adapter la suite aux dépendances effectivement retenues.
- **Réévaluation :** durée/coût de CI, défauts échappés, évolution de la
  topologie ou retrait des fonctionnalités conditionnelles.
- **Références :** REQ-011; DEC-22; DEC-07, DEC-10, DEC-11, DEC-15/16 si leurs
  composants sont retenus.

## ADR-12 — Assistant Spring AI / MCP conditionnel à UC-07; pas d’Agent ni RAG

- **Statut : conditionnel** à UC-07 (DEC-15/16); Agent et RAG **rejetés** en
  l’absence de besoin confirmé (DEC-17).
- **Contexte et problème :** déterminer si une interface conversationnelle
  répond à un besoin produit confirmé et quel accès de données elle exige.
  UC-07 est **DÉDUIT / À CONFIRMER**; la référence configure Spring AI et MCP
  mais ne prouve ni l’appel systématique d’outils ni l’exactitude des réponses.
- **Décision et justification :** si UC-07 est confirmé, assistant isolé avec
  Spring AI ChatClient, appel unique avec outils, lecture seule; MCP est retenu
  conditionnellement pour son interface standard et l’objectif pédagogique.
  Sans besoin multi-étapes, pas d’Agent; sans corpus non structuré confirmé,
  pas de RAG.
- **Alternatives considérées :** absence d’assistant; recherche UI/API REST;
  outils locaux Spring AI appelant REST; MCP; Agent autonome; RAG.
- **Avantages, inconvénients et compromis :** abstraction du LLM et outils
  réutilisables; coût, latence, non-déterminisme et surface d’attaque
  supplémentaires. Sans second consommateur confirmé, des outils Spring AI
  locaux appelant REST seraient plus simples. Les outils MCP restent en
  lecture seule, internes et soumis aux mêmes règles d’autorisation; pas
  d’écriture métier par le modèle.
- **Conséquences techniques et opérationnelles :** pas d’assistant, Spring AI,
  MCP, Agent, RAG ni canal conversationnel avant confirmation de UC-07;
  politique d’envoi de données à un LLM externe et propagation d’identité à
  confirmer. Telegram/Discord restent hors portée initiale.
- **Réévaluation :** UC-07 confirmé ou abandonné; besoin d’actions multi-étapes,
  corpus documentaire non structuré, second consommateur MCP ou exigences de
  confidentialité/fournisseur modifiées.
- **Références :** UC-07 **DÉDUIT / À CONFIRMER**; NFR-1 **DÉDUIT / À
  CONFIRMER**; DEC-15, DEC-16, DEC-17, DEC-18.

## ADR-13 — Compose et CI minimale; Kubernetes optionnel et Helm différé

- **Statut : proposé**; Kubernetes reste **conditionnel / À CONFIRMER** et
  Helm **différé** (DEC-19/20/21).
- **Contexte et problème :** rendre l’exécution locale reproductible et
  automatiser la validation sans imposer prématurément une plateforme
  d’orchestration de production.
- **Décision et justification :** Docker Compose pour l’environnement local;
  GitHub Actions pour build, tests, contrôles qualité de base et construction
  d’images. Les services restent sans état et configurables extérieurement
  pour une éventuelle portabilité Kubernetes. Le déploiement continu n’est pas
  décidé.
- **Alternatives considérées :** exécution native; Kubernetes seul en
  développement; aucun pipeline; déploiement continu.
- **Avantages, inconvénients et compromis :** démarrage reproductible et
  retour CI utile avec coût d’entrée contenu; Compose n’est pas une cible de
  production, et les tests d’intégration allongent la pipeline.
- **Conséquences techniques et opérationnelles :** pas de manifeste
  Kubernetes/Helm ni de déploiement automatisé dans cette décision; ressources
  et quotas gratuits GitHub à prendre en compte.
- **Réévaluation :** objectif validé de haute disponibilité/autoscaling,
  besoins multi-environnements ou paramétrage complexe justifiant Kubernetes
  et éventuellement Helm.
- **Références :** CON-3 **DÉCIDÉ**; NFR-4 **DÉDUIT / À CONFIRMER**, H-3/H-5
  **À CONFIRMER**; DEC-19, DEC-20, DEC-21.

## Traçabilité synthétique

| ADR | Décisions sources | Exigences / contraintes principales | Posture cible et limites |
|---|---|---|---|
| ADR-01 | DEC-01, 02 | UC-01..06, CON-2 | Deux domaines métier; repli monolithe si H-1 est faux |
| ADR-02 | DEC-03, 04, 05 | UC-01..06, NFR-2 | Propriété séparée; PostgreSQL; règles métier candidates |
| ADR-03 | DEC-06, 07 | UC-01..06, H-6 | REST par défaut; Kafka seulement si H-6 est confirmé |
| ADR-04 | DEC-07, 11, 22 | H-6 | Outbox et consommateurs idempotents uniquement avec Kafka retenu |
| ADR-05 | DEC-08 | NFR-1 | Gateway statique; autorisation conservée dans les services |
| ADR-06 | DEC-09, 14 | CON-3 | DNS/config externe; pas d’Eureka/Config Server actuellement |
| ADR-07 | DEC-10, 14 | NFR-1, H-4 | OIDC/JWT; rôles et public à confirmer |
| ADR-08 | DEC-11 | NFR-2, H-3 | Résilience et clés API; paramètres à confirmer |
| ADR-09 | DEC-12 | CON-2, NFR-4 | Redis pédagogique limité; pas de besoin de performance démontré |
| ADR-10 | DEC-13 | NFR-4, CON-3 | Standards ouverts; backends à confirmer |
| ADR-11 | DEC-22 | REQ-011 | Pyramide de tests; outils à confirmer |
| ADR-12 | DEC-15, 16, 17, 18 | UC-07, NFR-1 | Assistant/MCP conditionnels; lecture seule; sans Agent/RAG |
| ADR-13 | DEC-19, 20, 21 | CON-3, NFR-4, H-3/H-5 | Compose/CI; Kubernetes conditionnel, Helm différé |

## Points de vigilance conservés

- UC-01..07 et NFR-1..4 ne deviennent pas des exigences métier approuvées
  par leur présence dans les ADR.
- Kafka, l’outbox et le consommateur restent conditionnels à H-6. Sans besoin
  de consommateur confirmé, la décision source prévoit de ne pas introduire le
  broker en environnement production-like.
- Assistant, Spring AI et MCP restent conditionnels à UC-07; MCP expose
  uniquement des outils en lecture seule. Agent et RAG ne sont pas retenus
  sans besoins confirmés distincts.
- Redis est un choix **pédagogique**, avec un périmètre initial limité et sans
  profil de charge démontrant un besoin métier de performance.
- Kubernetes est une possibilité conditionnelle, pas une exigence de
  déploiement; Helm est différé.
- Aucune observation de la référence ne prouve un comportement runtime du
  système cible.
