# Test manuel : création du profil Customer, tâches 5.5 à 6.3

Ce guide décrit les essais manuels complémentaires disponibles pour le parcours
Customer. Il n’est plus une dépendance de clôture pour les scénarios déjà couverts
de manière reproductible par les tests automatisés. Il utilise les services de
test existants : Keycloak, Gateway, Customer et PostgreSQL. Ne l’exécute pas sur
une base de production ou partagée sans autorisation. Les données de test peuvent
être modifiées ou supprimées.

## Couverture de clôture automatisée

Le 27 novembre 2026, `mvn -B -pl customer-service -Pintegration-tests verify` a
réussi avec 43 tests unitaires/MVC et 21 tests d’intégration. Les tests
PostgreSQL et Keycloak couvrent `Location`, le rejeu, les conflits de clé, de
`sub` et d’e-mail, le rôle interdit, l’e-mail non vérifié, l’expiration, le
rollback et la concurrence avec plusieurs connexions. Les tests de Gateway
couvrent séparément les JWT, le transfert du bearer token, les routes Customer,
les 401 et l’absence de route Account/Actuator.

Ne répéter une procédure manuelle que si elle apporte une preuve supplémentaire :
par exemple le passage réel par le navigateur/Postman, une dépendance réseau
spécifique ou une observation opérationnelle propre à l’environnement. Les cas
non rejoués manuellement doivent être notés comme tels ; une couverture
automatisée équivalente ne doit pas être présentée comme un test manuel.

## 1. Préparer les comptes et les outils

1. Vérifier que Docker, Postman et DBeaver sont disponibles. Dans PowerShell,
   depuis la racine du projet, vérifier `docker compose ps`. PostgreSQL,
   Gateway et Customer doivent être démarrés ; attendre l'état `healthy`
   lorsqu'il est affiché. Keycloak doit être accessible sur
   `http://localhost:8081`.
2. Préparer des comptes **de test** Keycloak :
   - Alice : rôle `CUSTOMER`, e-mail vérifié, pas de profil Customer existant.
   - Bob : rôle `CUSTOMER`, autre `sub`, autre e-mail vérifié, pas de profil.
   - Charlie : rôle `CUSTOMER`, autre `sub` et e-mail vérifié, sans profil ;
     il servira aux essais concurrents.
   - Opérateur : rôle `OPERATOR`, sans profil ; si possible, tester aussi un
     compte avec les deux rôles `CUSTOMER` et `OPERATOR`.
   - Pour le conflit d'e-mail : un quatrième `sub` avec le même e-mail vérifié
     qu'Alice, uniquement si la configuration Keycloak de test permet deux
     comptes de même e-mail. Sinon, marquer ce cas **non exécuté**.
3. Obtenir localement un access token pour chaque compte avec le flux habituel
   Keycloak du projet. Il doit être valide pour l'audience `customer-service`.
   Ne jamais partager les jetons, les captures qui les contiennent ni les
   valeurs de `sub`/e-mail. Renouveler un jeton expiré avant de recommencer.
4. Dans DBeaver, se connecter à la base PostgreSQL Customer de test. Par défaut,
   Compose expose `localhost:5432`, base `customer_db`, mais les valeurs locales
   peuvent être différentes. Vérifier que le schéma `cps_customer_schema`
   contient `customer` et `customer_idempotency` ; si V2 manque, arrêter et
   vérifier Flyway et l'image utilisée.
5. Si un profil de test existe déjà, choisir un autre compte ou, après avoir
   identifié explicitement la ligne dans DBeaver et vérifié qu'elle est bien
   supprimable, supprimer **uniquement ce profil de test**. La clé associée
   est aussi supprimée par `ON DELETE CASCADE`. Ne pas supprimer toutes les
   lignes ou les volumes.

Dans les requêtes SQL ci-dessous, remplacer les valeurs entre `<...>` par les
valeurs de test en les conservant privées. Le `sub` vient du jeton validé,
jamais du corps de la requête.

```sql
SELECT id, sub, name, email, created_at
FROM cps_customer_schema.customer
WHERE sub = '<sub-alice>';

SELECT actor_sub, operation, idempotency_key, customer_id, response_status,
       response_body, response_location, created_at, expires_at
FROM cps_customer_schema.customer_idempotency
WHERE actor_sub = '<sub-alice>';
```

Les résultats peuvent contenir des données personnelles : les consulter
localement et ne pas les copier dans un compte rendu partagé.

## 2. Vérifier la garde et la configuration locale

Le code source récent ne modifie pas une image Docker déjà construite. Pour
tester les derniers changements de 5.5 à 6.3, il faut reconstruire l'image
Customer. La configuration générale désactive la création, mais le profil
`local` l'active pour ces tests. Comme `local` est le profil par défaut lorsque
aucun profil n'est spécifié, cela active aussi la création dans ce cas.
Utiliser un profil explicite hors environnement local et ne pas activer la
fonctionnalité sur un déploiement non validé.

1. Dans Postman, créer une requête `POST`
   `http://localhost:8080/api/v1/customers`. Dans **Authorization**, choisir
   **Bearer Token** et saisir localement le jeton d'Alice. Dans **Headers**,
   ajouter `Content-Type: application/json` et
   `Idempotency-Key: alice-garde`. Dans **Body > raw > JSON**, saisir :

   ```json
   {"name":"Alice"}
   ```

2. Si la création est explicitement désactivée sur le service en cours,
   envoyer la requête : attendre `503 PROFILE_CREATION_DISABLED`, sans
   écriture. Si elle est activée, ne pas envoyer cette requête avant d'avoir
   vérifié les données existantes : elle pourrait créer un profil.
3. Reconstruire et recréer **seulement Customer**, sans supprimer les volumes.
   Avec le profil local par défaut, la création sera activée. Si l'environnement
   utilise aussi le fichier de debug, conserver son inclusion comme ci-dessous.
   Cette commande interrompt brièvement Customer :

   ```powershell
   docker compose -f compose.yaml -f compose.debug.yaml up -d --build --no-deps customer-service
   docker compose -f compose.yaml -f compose.debug.yaml ps customer-service
   ```

   Si le debug n'est pas utilisé, omettre `-f compose.debug.yaml`. Attendre
   `healthy`. Si le service ne démarre pas, consulter ses logs sans partager
   de jetons ni de données personnelles ; ne pas continuer les tests.

## 3. Règles pour lire les réponses

- Tous les appels métier ci-dessous utilisent `POST` et l'URL Gateway de la
  section 2. Changer explicitement le token, la clé et le corps indiqués.
- Un succès renvoie `201`, un corps avec `id`, `name`, `email`, `createdAt`,
  et un en-tête `Location` ciblant `GET /api/v1/customers/{id}`. Ce dernier est
  vérifié automatiquement et avait seulement manqué à la preuve manuelle initiale.
- Une erreur métier Customer attendue est en `application/problem+json`, avec
  notamment `status`, `code` et `correlationId`. Un refus de sécurité à la
  Gateway peut avoir un autre corps ; vérifier au minimum son statut HTTP.
- Avant et après chaque cas, interroger les deux tables pour l'acteur testé.
  Pour les erreurs, vérifier qu'aucun nouveau profil ni résultat d'idempotence
  n'a été ajouté et que le profil existant est inchangé.
- Utiliser des clés dédiées à ces essais. Ne pas utiliser de jeton ou de donnée
  personnelle comme clé, et ne pas publier les corps ni en-têtes sensibles.

## 4. Cas fonctionnels et sécurité

Cocher un cas seulement après observation. `Non exécuté` reste différent de
`réussi`.

- [ ] **M1 — Création d'Alice, 5.5 et 6.1.** Token Alice, clé
  `alice-create-01`, corps `{"name":"  Alice  "}`. Attendre `201`, nom
  normalisé `Alice`, e-mail issu du token et `Location` correspondant à l'id.
  Dans DBeaver : une ligne Customer pour son `sub` et une ligne d'idempotence
  pour cette clé, liée au même `customer_id`. Comparer localement statut,
  corps exact et `Location` avec ceux enregistrés. Vérifier que
  `expires_at - created_at` vaut sept jours. Cette différence **ne prouve pas**
  sept jours après le commit.
- [ ] **M2 — Rejeu identique, 6.2.** Renvoyer exactement M1 avec la même clé.
  Attendre de nouveau `201`, même corps brut et même `Location` ; toujours une
  seule ligne Customer et une seule ligne d'idempotence pour la clé.
- [ ] **M3 — Normalisation, 6.2.** Même token et clé, corps
  `{ "name" : "Alice" }`. Attendre le même `201`, corps et `Location` que M1 :
  les espaces JSON et ceux autour du nom ne changent pas le contenu effectif.
- [ ] **M4 — Même clé, contenu différent, 6.2.** Même token et clé, corps
  `{"name":"Autre"}`. Attendre `409 IDEMPOTENCY_KEY_REUSED` ; vérifier que
  M1 et son résultat enregistré sont inchangés.
- [ ] **M5 — Même acteur, nouvelle clé, 5.6.** Token Alice, clé
  `alice-nouvelle-01`, corps `{"name":"Alice"}`. Attendre
  `409 CUSTOMER_PROFILE_EXISTS` : une nouvelle clé ne supprime pas la
  contrainte d'unicité sur le `sub`. Aucun résultat de succès ne doit être
  stocké pour cette nouvelle clé.
- [ ] **M6 — Même clé, autre acteur, 6.2.** Token Bob, clé
  `alice-create-01`, corps `{"name":"Bob"}`. Attendre `201`, profil Bob
  distinct et propre ligne d'idempotence. La même clé peut être utilisée par
  deux acteurs ; vérifier que les deux réponses ne sont pas confondues.
- [ ] **M7 — E-mail déjà utilisé, 5.6.** Si le quatrième compte prévu en
  section 1 existe : utiliser son token, une clé neuve et un corps valide.
  Attendre `409 EMAIL_ALREADY_EXISTS` et aucune nouvelle ligne pour cet
  acteur. Si Keycloak interdit ce compte, noter **non exécuté** plutôt que
  modifier une configuration partagée ou fabriquer un jeton.
- [ ] **M8 — Rôle interdit, 5.5.** Avec le token OPERATOR, une clé neuve et
  un corps valide, attendre `403 FORBIDDEN_PROFILE_CREATION` et aucune
  écriture. Répéter avec le compte à deux rôles s'il existe ; sinon noter
  cette variante non exécutée.
- [ ] **M9 — Sans authentification, 5.5.** Désactiver l'autorisation dans
  Postman et envoyer un corps valide : attendre `401` et aucune écriture.
  Réactiver ensuite le token nécessaire.
- [ ] **M10 — Validation du corps.** Avec un token CUSTOMER valide et une clé
  neuve, essayer successivement `{"name":" "}`, un nom de 121 caractères,
  `{"name":"Alice","sub":"autre"}` et
  `{"name":"Alice","email":"autre@example.test"}`. Attendre pour chacun
  `400 VALIDATION_FAILED` et aucune clé de succès enregistrée. Pour un champ
  réservé, vérifier également la présence de l'erreur sur le champ.
- [ ] **M11 — Clé invalide, 6.2.** Avec un corps valide, essayer une clé
  absente, une clé vide ou uniquement composée d'espaces, puis une clé de
  256 caractères : attendre `400 INVALID_IDEMPOTENCY_KEY` sans écriture.
  Une clé de 255 caractères est valide : avec un compte sans profil, elle
  doit permettre un `201`, pas un `400`. Éviter les sauts de ligne dans
  l'en-tête. On peut générer les caractères dans Postman avec
  `pm.request.headers.upsert({key: "Idempotency-Key", value: "x".repeat(256)})`
  dans **Scripts > Pre-request** ; retirer le script après le cas.
- [ ] **M12 — E-mail non vérifié.** Si un compte de test CUSTOMER dispose d'un
  e-mail absent ou non vérifié dans son access token, utiliser une clé neuve
  et un corps valide : attendre `400 EMAIL_NOT_VERIFIED`, sans écriture.
  Sinon, noter **non exécuté**.

## 5. Expiration sans attendre sept jours

Ce test modifie volontairement la date d'une **ligne de test**. Ne pas le
faire pour des données réelles.

1. Reprendre la clé `alice-create-01` de M1 et vérifier son résultat initial.
2. Dans DBeaver, exécuter l'`UPDATE` suivant uniquement après avoir remplacé
   `<sub-alice>` et vérifié que la sélection identifie exactement une ligne.
   Modifier aussi `created_at` respecte la contrainte `expires_at > created_at`.

   ```sql
   SELECT actor_sub, operation, idempotency_key, created_at, expires_at
   FROM cps_customer_schema.customer_idempotency
   WHERE actor_sub = '<sub-alice>'
     AND operation = 'POST /api/v1/customers'
     AND idempotency_key = 'alice-create-01';

   UPDATE cps_customer_schema.customer_idempotency
   SET created_at = CURRENT_TIMESTAMP - INTERVAL '8 days',
       expires_at = CURRENT_TIMESTAMP - INTERVAL '1 day'
   WHERE actor_sub = '<sub-alice>'
     AND operation = 'POST /api/v1/customers'
     AND idempotency_key = 'alice-create-01';
   ```

   Valider la transaction DBeaver si l'autocommit est désactivé.
3. [ ] **M13 — Clé expirée et profil existant, 6.2.** Renvoyer M1.
   Attendre `409 CUSTOMER_PROFILE_EXISTS`, et non le `201` précédent :
   la clé est expirée, mais le profil existe toujours. Dans DBeaver, vérifier
   que le profil est inchangé et que la ligne expirée existe encore : sa
   suppression a été annulée avec la transaction qui a échoué.
4. Pour tester une nouvelle création après expiration, utiliser un compte de
   test neuf ou supprimer uniquement le profil de test identifié après accord.
   **Attention :** la suppression du profil supprime aussi sa clé par cascade ;
   elle ne démontre donc pas à elle seule le remplacement d'une ligne expirée.
   Ce remplacement est couvert par le test automatisé PostgreSQL, pas par M13.

## 6. Concurrence observable avec Postman et DBeaver

Deux clics rapides ne garantissent pas que les requêtes se chevauchent.
Cette procédure utilise un verrou temporaire sur la table Customer pour rendre
l'attente observable. **Elle bloque temporairement les écritures Customer de
tous les utilisateurs de cette base** : uniquement sur une base de test
autorisée, prévenir les autres utilisateurs et garder le verrou très peu de
temps. Ne pas arrêter ou redémarrer PostgreSQL.

1. Préparer Charlie sans profil, avec un token valide. Ouvrir deux onglets
   Postman identiques : token Charlie, clé `charlie-course-01`, corps
   `{"name":"Charlie"}`. Ne pas les envoyer encore.
2. Dans DBeaver, ouvrir **deux connexions SQL indépendantes** à la même base.
   Dans la première, exécuter les deux instructions suivantes ; laisser cette
   transaction ouverte, sans faire `COMMIT` :

   ```sql
   BEGIN;
   LOCK TABLE cps_customer_schema.customer IN SHARE MODE;
   ```

3. Envoyer la première requête depuis Postman. Elle devrait rester en attente
   avant l'insertion du profil. Dans la **seconde** connexion DBeaver, observer :

   ```sql
   SELECT l.pid, l.locktype, l.mode, l.granted,
          a.wait_event_type, a.wait_event
   FROM pg_locks l
   JOIN pg_stat_activity a ON a.pid = l.pid
   WHERE l.database = (SELECT oid FROM pg_database WHERE datname = current_database())
     AND l.locktype IN ('relation', 'advisory')
     AND NOT l.granted;
   ```

   Chercher une attente de type `relation`. Les résultats peuvent inclure
   d'autres sessions : l'identification précise des PID et des blocages via
   `pg_blocking_pids(pid)` est nécessaire si la base n'est pas isolée.
4. Envoyer le deuxième onglet avec la même clé et le même contenu pendant que
   le premier attend. Relancer la requête d'observation : chercher une attente
   `advisory` non accordée. Si les deux attentes ne sont pas observées, ne
   pas conclure que la concurrence a été démontrée.
5. **Immédiatement**, dans la première connexion DBeaver, libérer le verrou :

   ```sql
   ROLLBACK;
   ```

   Faire aussi ce `ROLLBACK` en cas d'erreur, d'interruption ou de doute, avant
   de fermer la connexion. Si une requête a expiré, libérer le verrou puis
   recommencer avec une clé neuve et vérifier les données avant toute action.
6. [ ] **M14 — Rejeu concurrent, 6.3.** Vérifier les deux réponses Postman :
   deux `201`, même corps brut et même `Location`, aucun `500`. Dans DBeaver :
   exactement un Customer Charlie et une ligne pour `charlie-course-01`.
7. [ ] **M15 — Contenu concurrent différent, 6.3.** Avec un autre compte neuf
   et une nouvelle clé, reprendre les étapes 1–5 : première requête avec un
   nom valide, seconde avec un autre nom et la même clé. Observer les attentes ;
   après `ROLLBACK`, attendre `201` pour la première et
   `409 IDEMPOTENCY_KEY_REUSED` pour la seconde, avec un seul profil et une
   seule ligne d'idempotence.

Ce test utilise plusieurs connexions mais peut ne solliciter qu'une seule
instance Customer. Pour prouver un cas **multi-instance**, il faut au moins
deux instances exécutant la même version et utilisant la même base, et pouvoir
vérifier que chaque requête atteint une instance différente. La configuration
Compose actuelle ne garantit pas cette répartition : sans cette preuve,
noter le cas multi-instance **non exécuté**. Ne pas activer la création sur
un mélange d'anciennes et de nouvelles versions.

## 7. Compléter la preuve et remettre l'environnement en état

- Le rollback après une panne simulée du gagnant et le remplacement atomique
  d'une clé expirée sont couverts par les tests automatisés PostgreSQL ; ce
  guide ne prétend pas les reproduire manuellement. La frontière exacte à
  sept jours après le **commit** reste une limite connue.
- Pour chaque cas M1–M15 réellement exécuté manuellement, noter localement
  `réussi`, `échec` ou `non exécuté`, le statut/code observé et la vérification
  DB, sans jeton, e-mail, `sub` ni réponse personnelle. Pour un échec, conserver
  uniquement le `correlationId` et des informations non sensibles. Pour un cas
  couvert uniquement par la suite automatisée, le noter `couvert automatiquement`
  et indiquer le test correspondant ; ne pas le compter comme recette manuelle.
- Désactiver la création après les essais en créant un fichier local
  `compose.manual-stop.yaml` (s'il existe déjà, ne pas l'écraser) :

  ```yaml
  services:
    customer-service:
      environment:
        CUSTOMER_PROFILE_CREATION_ENABLED: "false"
  ```

  Puis recréer Customer avec ce fichier, sans reconstruire l'image :

  ```powershell
  docker compose -f compose.yaml -f compose.debug.yaml -f compose.manual-stop.yaml up -d --no-deps --force-recreate customer-service
  docker compose -f compose.yaml -f compose.debug.yaml -f compose.manual-stop.yaml ps customer-service
  ```

  Omettre le fichier debug si non utilisé. Après retour à `healthy`, envoyer
  une requête valide authentifiée : attendre à nouveau `503
  PROFILE_CREATION_DISABLED`. Ne pas supprimer les volumes. Réutiliser le
  fichier de surcharge lors de futures recréations du conteneur : sans lui,
  le profil local réactive la création. Vérifier la configuration effective
  avant chaque déploiement.
- Nettoyer uniquement les profils de test identifiés et autorisés si souhaité ;
  leurs lignes d'idempotence associées sont supprimées par cascade. Ne pas
  cocher les tâches OpenSpec avant de revoir les résultats et les cas non
  exécutés.
