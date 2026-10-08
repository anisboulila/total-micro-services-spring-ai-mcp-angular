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
