# Design — Décisions techniques préalables au backlog v1

## Contexte et limites

Le backlog `define-implementation-backlog` contient 32 tâches non exécutées.
Ses décisions techniques ouvertes TD-03/04/05/06/07/08/14 bloquent en partie
les contrats et tests du premier incrément Customer. Cette change prépare des
choix documentés, mais ne réalise aucune tâche de backlog et ne modifie pas ses
specs métier ni les changes historiques.

Les règles fonctionnelles et décisions métier existantes restent la
référence. Les choix techniques retenus ci-dessous répondent à la demande
d’arbitrage de cette change; ils ne prétendent pas avoir été testés en runtime.

## Registre de preuve

| Statut | Sens |
|---|---|
| **DÉCIDÉ — source** | Décision déjà présente dans une source d’architecture ou un arbitrage explicite; cette change ne la redécide pas. |
| **DÉCIDÉ — change courante** | Arbitrage technique retenu dans cette change pour permettre le premier incrément. |
| **DIFFÉRÉ — change ultérieure** | Choix technique qui ne bloque pas le premier incrément et devra être arrêté avant sa fonctionnalité. |
| **INFORMATION INTROUVABLE** | Aucun choix correspondant n’a été retrouvé dans les documents consultés. |

## Décisions déjà actées à préserver

| Domaine | État déjà décidé |
|---|---|
| API et erreurs | REST synchrone; autorisation dans les services propriétaires; ressource absente/non autorisée masquée par HTTP 404; création Account refusée en HTTP 404 si le Customer n’existe pas et HTTP 503 si `customer-service` est indisponible; email dupliqué et conflit d’idempotence en HTTP 409. Le format commun d’erreur contient un code stable, un message sûr et un identifiant de corrélation. |
| Pagination | Taille par défaut 20, maximum 100, valeurs configurables. Convention de page, tri stable et action au-delà de 100 ouverts. |
| Identité | Keycloak émet les jetons OIDC; Gateway et services valident indépendamment les JWT; profil Customer lié au `sub` et à l’e-mail vérifié; rôles exacts non fixés par DEC-10. L’autorisation est effectuée dans les services. |
| Idempotence | Les POST de création utilisent une clé; même clé et même requête rejouent le résultat initial; même clé avec un contenu différent retourne un conflit; unicité garantie par la base du service et écriture atomique avec la création. L’arbitrage actuel fixe une portée par acteur et opération et une conservation d’une semaine. |
| Persistance et tests | PostgreSQL par service et migrations versionnées; stratégie de tests unitaires, d’intégration PostgreSQL/Keycloak et de sécurité décidée; outil de migration et outils de tests étaient à choisir. |
| Historique | H-6, UC-07 et DEC-07 gardent les formulations et statuts de leurs sources. Cette change ne décide ni Kafka, ni son consommateur, ni Spring AI/MCP. |

Sources : `define-target-system-design/design.md` (DEC-04, DEC-06,
DEC-10/11, DEC-13/14, DEC-22); `define-implementation-backlog/design.md`
(arbitrages métier et TD-03/04/05/06/07/08/14);
`specs/customer-account-management/spec.md` (US-01..US-06 et idempotence).

## Décisions techniques retenues

### TD-03 — Validation d’entrée

**DÉCIDÉ :** validation dans chaque service propriétaire. `name` est rogné
des espaces périphériques, doit rester non vide et ne dépasse pas 120
caractères. `sub`, `email` et `email_verified` sont issus exclusivement du
JWT validé; un claim `email_verified` absent est traité comme faux. Le client
ne peut pas soumettre l’e-mail ou le `sub` dans le corps Customer. Les champs
réservés au serveur, dont `customerId` fourni par un client lors de la création
Account, sont rejetés. E-mail vérifié et unique restent des règles métier; la
normalisation technique retenue est le rognage des espaces périphériques et
une comparaison insensible à la casse pour l’unicité; l’adresse canonique
persistée est en minuscules.

Les valeurs incorrectes et les champs réservés retournent HTTP 400 avec le
code stable `VALIDATION_FAILED`; le conflit d’unicité d’e-mail reste HTTP 409
avec `EMAIL_ALREADY_EXISTS`.

### TD-04 — Pagination

**DÉCIDÉ :** index de page commençant à zéro; taille par défaut 20; taille
maximale 100. Réponse avec `content`, `page`, `size`, `totalElements` et
`totalPages`. Ordre stable `createdAt DESC, id ASC`. Paramètres absents
prennent leur valeur par défaut; paramètres négatifs, mal formés ou taille
supérieure à 100 sont rejetés en HTTP 400 (`INVALID_PAGE` ou `INVALID_PAGE_SIZE`),
jamais plafonnés silencieusement. Les valeurs par défaut et maximum restent
configurables.

### TD-14 — Contrat d’erreur

**DÉCIDÉ :** utiliser `application/problem+json` conforme à RFC 9457 et les
extensions `code`, `correlationId` et, pour les erreurs de champs, `errors`
avec noms de champs et codes de validation. `detail` est sûr et non sensible.
Aucune stack trace, requête SQL, jeton, secret ou corps de requête ne figure
dans la réponse. Les codes sont en majuscules `UPPER_SNAKE_CASE`; un
identifiant de corrélation généré côté serveur est toujours retourné. Une
valeur entrante absente ou invalide est remplacée, pas reflétée.

L'en-tête entrant est `X-Correlation-ID`; une seule valeur au format UUID
canonique est acceptée et normalisée en minuscules. Une valeur absente,
multiple ou invalide est remplacée par un UUID généré côté serveur. Le même
identifiant figure dans le Problem Details et l'en-tête de réponse
`X-Correlation-ID`; il est ajouté au MDC et aux logs d'erreur, et ne sert
jamais à authentifier ou autoriser un appel. Une erreur serveur inattendue
utilise le code stable `INTERNAL_ERROR`.

Codes HTTP proposés :

| HTTP | Usage |
|---|---|
| 400 | Validation ou paramètres incorrects |
| 401 | Jeton absent, invalide ou expiré |
| 403 | Action interdite lorsqu’il n’y a pas de ressource à masquer |
| 404 | Ressource inexistante ou masquée comme non autorisée |
| 409 | Unicité métier, email dupliqué ou clé d’idempotence réutilisée avec un contenu différent |
| 503 | Dépendance requise indisponible, sans écriture partielle |

Les décisions métier de statut restent prioritaires : 404 masque l’absence et
l’accès interdit à une ressource; 404 si le Customer ciblé n’existe pas; 503
si `customer-service` est indisponible; 409 pour un e-mail déjà utilisé ou une
clé d’idempotence réutilisée avec un contenu différent.

Exemple proposé :

```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json
```

```json
{
  "type": "about:blank",
  "title": "Invalid request",
  "status": 400,
  "detail": "One or more request fields are invalid.",
  "code": "VALIDATION_FAILED",
  "correlationId": "a8a2e287-b820-4f74-9e6f-686b2d7e5b91",
  "errors": [
    {"field": "name", "code": "NOT_BLANK"}
  ]
}
```

Exemple de réponse de collection :

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

### TD-06 — Rôles et propagation OIDC

**DÉCIDÉ :** Keycloak est le fournisseur OIDC; la Gateway et chaque service
valident le JWT; le `sub` authentifié est l’identité stable; seuls les
services propriétaires prennent les décisions métier d’autorisation; le
frontend ne peut pas attribuer lui-même le rôle opérateur.

**DÉCIDÉ :** utiliser les rôles `CUSTOMER` et `OPERATOR` dans le claim
Keycloak `realm_access.roles`; `OPERATOR` correspond à l’acteur opérateur/admin
déjà défini par les arbitrages métier. L’attribution de ce rôle est manuelle
dans Keycloak et n’est jamais proposée par le frontend. La Gateway et chaque
service valident indépendamment signature, émetteur, expiration et audience du
JWT. Les services dérivent l’acteur de `sub` et prennent les décisions
d’autorisation. Aucun rôle ou `sub` transmis dans un en-tête libre n’est
autoritaire.

Pour les appels synchrones délégués au nom de l’utilisateur, transmettre le
bearer token original uniquement au service destinataire; le destinataire
valide aussi son audience. La configuration OIDC doit rendre le jeton accepté
par les ressources internes qui le reçoivent. Ne pas introduire d’échange de
jeton pour la v1 sans exigence de séparation d’audience.

Cette décision technique ne modifie pas les permissions métier, le masquage
404 ou le rôle réservé à l’auto-inscription Customer.

### TD-05 — Idempotence persistante

**DÉCIDÉ :** la durée de conservation est de sept jours à compter du commit de
succès; l’unicité est garantie par la base PostgreSQL du service propriétaire;
l’enregistrement de création et la ressource sont atomiques; même contenu
rejoue le résultat initial; contenu différent avec la même clé retourne HTTP
409; les clés sont distinctes par acteur et opération.

- Périmètre de contrainte unique : `(actor_sub, operation, idempotency_key)`
  dans la base de chaque service; le nom de service est ainsi implicite dans
  la base propriétaire.
- Exiger une valeur non vide dans l’en-tête `Idempotency-Key`, limitée à 255
  caractères; valeur absente, vide ou trop longue : HTTP 400 `INVALID_IDEMPOTENCY_KEY`.
- Calculer SHA-256 sur une représentation canonique du contenu effectif et de
  l’opération, après validation; inclure les valeurs dérivées qui influent sur
  la création et l’acteur (`sub`), exclure l’en-tête de clé, l’ordre et les
  espaces JSON et les en-têtes de transport. Les champs canoniques sont
  sérialisés dans l’ordre défini du DTO; aucune dépendance à l’ordre des
  propriétés d’entrée. Ne pas journaliser la clé ni le contenu sensible.
- Une contrainte unique sur `(actor_sub, operation, idempotency_key)` arbitre
  les courses multi-instances dans chaque base de service; le service
  propriétaire est implicite. La transaction qui crée la ressource écrit aussi
  la clé, l’empreinte, l’échéance UTC, le statut HTTP et le corps exact du
  succès; aucun bearer token n’est stocké. Ne persister que les succès. Les
  validations rejetées et erreurs transitoires avant commit ne consomment pas
  la clé.
- Une demande concurrente identique attend la transaction gagnante puis relit
  et rejoue son résultat; aucune seconde création n’est lancée. À
  `expires_at <= maintenant UTC`, le service verrouille et remplace atomiquement
  l’enregistrement expiré avant une nouvelle commande; le nettoyage physique
  peut être différé. Cela peut créer une nouvelle ressource Account après
  sept jours.

Exemples proposés :

```http
POST /api/customers
Authorization: Bearer <access-token>
Idempotency-Key: customer-profile-01
Content-Type: application/json

{"name":"Ada Lovelace"}
```

La réponse de succès est liée à `sub` et à l’opération de création Customer;
le client ne transmet ni `sub` ni `email`. Un rejeu identique reçoit le même
statut et le même corps. Pour une clé déjà utilisée avec un contenu effectif
différent :

```http
HTTP/1.1 409 Conflict
Content-Type: application/problem+json
```

```json
{
  "type": "about:blank",
  "title": "Idempotency key conflict",
  "status": 409,
  "detail": "The key was already used for a different request.",
  "code": "IDEMPOTENCY_KEY_REUSED",
  "correlationId": "a8a2e287-b820-4f74-9e6f-686b2d7e5b91"
}
```

### TD-07/08 — Migrations et stratégie de test initiale

**DÉCIDÉ :** Flyway pour les migrations SQL versionnées PostgreSQL; les
migrations s’exécutent dans le service propriétaire et les contraintes
d’unicité du `sub` et de l’e-mail sont garanties par PostgreSQL. Ne pas fixer
ici les longueurs de colonnes encore ouvertes.

Pour le premier incrément Customer :

- tests unitaires de domaine avec JUnit 5 et `spring-boot-starter-test`;
- tests API MVC avec Spring Boot Test et MockMvc;
- tests de persistance et migrations avec PostgreSQL Testcontainers;
- tests de sécurité de validation JWT avec des jetons de test signés et
  intégration OIDC avec un Keycloak Testcontainer; aucun test ne dépend d’un
  serveur partagé;
- vérifications répétables par Maven, nettoyage automatique des conteneurs et
  aucune dépendance à un état préexistant.

Les tests utilisent des bases et services isolés/reproductibles; aucune
dépendance à un environnement partagé. Les choix d’outillage n’installent ni
ne configurent des dépendances dans cette change documentaire.

**DIFFÉRÉ — avant les changements concernés :** framework de test Angular/UI,
outil de contrat REST interservice, tests de contrat Kafka et simulation/
évaluation LLM. Ils ne bloquent pas le premier incrément Customer. TD-08 est
résolu pour le périmètre Customer uniquement; ses autres volets restent
explicitement ouverts dans le backlog.

## Dépendances et périmètre non résolu

Les décisions ci-dessus lèvent les blocages techniques nécessaires aux tâches
1.1–1.4 et 3.1–3.3 du backlog pour le premier incrément Customer. Les décisions
de test propres à l’UI/IA et aux contrats Kafka restent différées; TD-08 est
résolu uniquement pour Customer. Cette change ne traite ni
TD-01/02/09/10/11/12/13, ni l’installation ou le démarrage de services.

Elle ne change pas les exigences métier, ne publie pas de nouvelles specs
canoniques, n’implémente aucun code et ne coche aucune tâche du backlog global.
