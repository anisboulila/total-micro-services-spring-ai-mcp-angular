# Tasks

## 1. Comprendre Spring AI sans outil ni donnée métier

**Apprentissage :** rôle de `ChatClient`, configuration du modèle, prompt système, réponse et gestion d’indisponibilité.  
**Résultat attendu :** `assistant-service` peut produire une réponse conversationnelle simple dans un test reproductible, sans accès Customer, sans MCP et sans route Gateway.

- [ ] 1.1 Créer le module `assistant-service` et ajouter la dépendance Spring AI verrouillée au parent Maven ; vérifier que le reactor compile sans modifier les modules existants.
- [ ] 1.2 Configurer un fournisseur de modèle local externe au code et un double de modèle pour les tests ; vérifier qu’une réponse simple est obtenue en test sans secret cloud ni accès réseau externe obligatoire.
- [ ] 1.3 Créer un service minimal autour de `ChatClient` avec un prompt système statique et une taille de requête bornée ; vérifier par test le contenu transmis au modèle, une réponse normale et une erreur contrôlée.
- [ ] 1.4 Garder ce premier parcours dans le contexte de test ou sur une interface locale non routée par la Gateway ; vérifier qu’aucune nouvelle route publique ni donnée Customer n’est accessible.
- [ ] 1.5 Documenter ce qu’apportent `ChatClient`, le modèle et le prompt ainsi que le résultat observé ; vérifier que le guide distingue clairement réponse générée et donnée métier.

## 2. Comprendre le Tool Calling avec un outil Java simple

**Apprentissage :** description d’un outil, choix de l’outil par le modèle, arguments, résultat structuré et refus d’un outil inconnu.  
**Résultat attendu :** un outil Java déterministe peut être appelé par Spring AI dans un test, sans MCP et sans données sensibles.

- [ ] 2.1 Ajouter un outil Java pédagogique de lecture sans argument et avec une réponse fixe versionnée, limité au contexte test/local ; vérifier par un test direct le schéma et la valeur retournée.
- [ ] 2.2 Enregistrer cet outil dans une allow-list de démonstration pour `ChatClient` ; vérifier avec un modèle simulé qu’un prompt prévu produit exactement un Tool Call et une seule réponse finale.
- [ ] 2.3 Ajouter les cas déterministes « outil inconnu », « écriture demandée » et « identifiant fourni dans le prompt » ; vérifier qu’aucun outil n’est appelé hors de l’allow-list.
- [ ] 2.4 Comparer une réponse directe du modèle avec une réponse issue du résultat de l’outil ; vérifier que le service ne mélange pas texte inventé et résultat structuré.
- [ ] 2.5 Documenter le cycle choix d’outil → appel Java → résultat → réponse et les limites de cette démonstration ; vérifier qu’une commande de test reproduit le scénario localement.

## 3. Introduire MCP avant les données Customer

**Apprentissage :** protocole client/serveur, découverte des capacités, schéma d’outil, transport interne et erreur réseau.  
**Résultat attendu :** le même outil pédagogique est accessible via MCP, uniquement sur le réseau local/interne, et testable sans JWT métier ni base Customer.

- [ ] 3.1 Créer `customer-mcp-server` avec un serveur MCP interne et une unique capacité pédagogique équivalente à l’outil Java de l’étape 2 ; vérifier par test de découverte qu’aucune autre capacité n’est publiée.
- [ ] 3.2 Ajouter le client MCP dans `assistant-service` et pointer la configuration vers le serveur interne ; vérifier un appel de bout en bout modèle simulé → client MCP → serveur → réponse.
- [ ] 3.3 Simuler serveur MCP absent, découverte échouée, timeout et résultat mal formé ; vérifier que l’assistant retourne une erreur contrôlée sans inventer de résultat ni publier de détail interne.
- [ ] 3.4 Comparer dans la documentation l’appel Java local avec l’appel MCP ; vérifier que la comparaison explique explicitement la découverte, le transport, la frontière de déploiement et le coût supplémentaire du protocole.
- [ ] 3.5 Garder le port MCP non publié et sans route Gateway ; vérifier depuis Compose ou un test réseau qu’il n’est pas accessible comme endpoint public.

## 4. Remplacer le démonstrateur par la consultation Customer sécurisée

**Apprentissage :** adaptation MCP → REST, propagation JWT, audience, rôles, `sub` et autorisation dans le service propriétaire.  
**Résultat attendu :** `getMyCustomerProfile` retourne seulement le profil du client authentifié, avec les mêmes règles que l’API Customer existante.

- [ ] 4.1 Remplacer la capacité pédagogique par `getMyCustomerProfile`, sans argument d’identité, et définir un DTO minimal (`id`, `name` et éventuellement `email` selon décision de minimisation) ; vérifier par contrat que le schéma n’accepte aucun `customerId`, `sub` ou email fourni par le modèle.
- [ ] 4.2 Faire appeler à l’adaptateur MCP `GET /api/v1/customers` avec le token validé et un identifiant de corrélation ; vérifier avec une API simulée que zéro fiche donne `NOT_FOUND` et une fiche donne `FOUND`.
- [ ] 4.3 Valider signature, issuer, expiration, audience et autorités dans `assistant-service` et `customer-mcp-server` ; vérifier qu’un JWT absent, expiré, mal signé ou de mauvaise audience bloque l’appel avant Customer.
- [ ] 4.4 Restreindre ce parcours à `CUSTOMER` sans `OPERATOR` ; vérifier qu’un utilisateur opérateur ou double rôle ne peut pas obtenir la liste globale via l’assistant.
- [ ] 4.5 Propager le bearer token uniquement sur Assistant → MCP → Customer et ne pas le transmettre au modèle ; vérifier que les en-têtes `X-User-Id`/`X-Roles` libres sont ignorés et que le `sub` reçu par Customer correspond au JWT.
- [ ] 4.6 Ajouter une route Gateway explicite vers l’endpoint assistant tout en conservant MCP inaccessible ; vérifier les tests Gateway existants et l’absence de route MCP/non configurée.
- [ ] 4.7 Retirer l’outil pédagogique de l’allow-list finale ; vérifier par découverte MCP et test de Tool Calling que seule `getMyCustomerProfile` reste disponible dans le parcours métier.
- [ ] 4.8 Documenter le flux JWT et rappeler que le token exchange de production est différé ; vérifier que la documentation ne présente jamais cette propagation POC comme architecture de production définitive.

## 5. Sécuriser les résultats et couvrir les échecs essentiels

**Apprentissage :** différence entre sélection d’outil, preuve métier, réponse fiable, injection et indisponibilité.  
**Résultat attendu :** aucune écriture, fuite de profil, fiche inventée ou erreur interne n’est restituée à l’utilisateur.

- [ ] 5.1 Valider le résultat MCP contre le DTO autorisé avant toute formulation textuelle ; vérifier qu’un résultat multiple, incomplet, avec champ inconnu ou contradictoire devient `UNAVAILABLE`.
- [ ] 5.2 Ajouter un test ciblé d’hallucination dans lequel le modèle invente un identifiant, un nom ou un e-mail ; vérifier que seule la valeur présente dans le résultat validé peut être restituée.
- [ ] 5.3 Ajouter les tests adversariaux de Tool Calling pour écriture, outil inconnu, profil tiers, instruction d’ignorance des règles et demande d’afficher tous les clients ; vérifier zéro appel non autorisé.
- [ ] 5.4 Normaliser les réponses pour profil absent, accès masqué, timeout modèle, MCP indisponible et Customer indisponible ; vérifier que les messages publics ne révèlent ni cause de sécurité ni détails d’infrastructure.
- [ ] 5.5 Limiter à un appel outil et à des timeouts explicites, sans retry métier ; vérifier par test que le parcours s’arrête et qu’aucun profil n’est fabriqué lorsque la limite est atteinte.
- [ ] 5.6 Tester les logs, prompts et erreurs pour absence de bearer token, secret, `sub`, email et valeur de profil ; vérifier les assertions de redaction dans les trois composants.
- [ ] 5.7 Documenter la matrice minimale de réponses fiables/non fiables et les scénarios bloquants ; vérifier qu’elle correspond aux scénarios de `assisted-profile-lookup/spec.md`.

## 6. Intégrer localement sans cacher la prochaine étape

**Apprentissage :** assemblage des briques, démarrage reproductible, diagnostics, non-régression et limites du POC.  
**Résultat attendu :** la consultation conversationnelle du profil fonctionne localement et les étapes suivantes vers US-08 sont explicites.

- [ ] 6.1 Étendre Compose avec `assistant-service`, `customer-mcp-server` et le modèle local retenu ; vérifier démarrage, santé, redémarrage et absence de publication du port MCP.
- [ ] 6.2 Ajouter les nouveaux modules aux jobs CI avec tests unitaires, sécurité, contrats et intégration déterministe ; vérifier que le build standard ne dépend ni d’un LLM distant ni d’un secret fournisseur.
- [ ] 6.3 Exécuter le parcours intégré avec profil présent, profil absent, accès croisé, JWT invalide et indisponibilité de chaque dépendance ; vérifier tous les scénarios du spec et consigner les résultats.
- [ ] 6.4 Réexécuter les suites Customer et Gateway existantes ; vérifier que la route assistant est désactivable sans altérer les parcours REST ni les données Customer.
- [ ] 6.5 Documenter le démarrage, les variables, l’inspection du Tool Call, les erreurs fréquentes, le rollback et les limites ; vérifier les commandes dans un environnement propre.
- [ ] 6.6 Produire une note de passage vers US-08 montrant quels composants sont réutilisés et ce que l’outil Account ajoutera ; vérifier qu’aucun besoin Account, contrat ou résultat US-08 n’est inventé dans cette change.
- [ ] 6.7 Consigner les mécanismes reportés — token exchange, circuit breaker, évaluation LLM avancée, coût/FinOps et observabilité distribuée — dans le backlog futur ; vérifier qu’ils sont présentés comme durcissements ultérieurs et non comme absence de sécurité.
