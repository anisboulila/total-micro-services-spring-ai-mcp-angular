# Design

## Context

Le projet dispose déjà d’une lecture Customer sécurisée par `sub` OIDC, d’une Gateway stateless et de tests d’accès. La change de backlog existante prévoit ensuite Spring AI/MCP pour US-08. Voir `proposal.md` pour la motivation et `specs/assisted-profile-lookup/spec.md` pour les garanties observables.

L’objectif pédagogique est désormais de rendre chaque mécanisme observable séparément. Une étape n’introduit le composant suivant que lorsque l’étape précédente est comprise, testée et supprimable sans casser le socle métier.

## Goals / Non-Goals

**Goals:**

- Comprendre le modèle d’appel de Spring AI avec une configuration locale remplaçable.
- Observer un Tool Call vers une méthode Java simple avant toute dépendance MCP.
- Comprendre ce que MCP ajoute : contrat de capacité, découverte, client/serveur, transport et erreurs.
- Remplacer le démonstrateur par un outil Customer limité à la lecture du profil courant.
- Réutiliser l’autorisation Customer et la sécurité Gateway existantes.
- Conserver des tests et des points de validation à chaque palier.
- Préparer US-08 par réemploi de l’assistant, du client MCP et du résultat validé.

**Non-Goals:**

- Modifier US-02, US-03, US-08 ou US-09.
- Créer une API métier générique pilotée par le modèle.
- Donner au modèle un identifiant de profil ou un accès direct à PostgreSQL.
- Introduire Agent autonome, RAG, mémoire longue, écriture ou multi-agents.
- Réaliser dans ce POC token exchange, circuit breaker, évaluation statistique de LLM, FinOps ou observabilité distribuée complète.

## Decisions

### Décision 1 — Progression Spring AI → outil Java → MCP

```text
Etape 1 : Spring AI seulement
          | ChatClient / prompt / configuration
          v
Etape 2 : outil Java local
          | schema / selection / appel / resultat
          v
Etape 3 : outil expose par MCP
          | discovery / client / server / transport
          v
Etape 4 : outil MCP Customer reel
          | REST / identite / autorisation
          v
Etape 5 : fiabilite et garde-fous
          v
Etape 6 : integration locale et US-08
```

Chaque étape produit un résultat exécutable ou testable. L’outil Java de l’étape 2 est un démonstrateur pédagogique non métier, disponible uniquement dans le contexte de test ou local. Il est retiré de l’allow-list finale avant connexion à Customer.

L’alternative consistant à démarrer directement avec Assistant + MCP + JWT a été rejetée : elle empêche d’attribuer correctement une erreur à Spring AI, au Tool Calling, au transport MCP ou à la sécurité.

### Décision 2 — MCP est une frontière, pas une seconde API métier

MCP introduit une interface standardisée entre l’assistant et une capacité distante. Le serveur MCP final expose uniquement `getMyCustomerProfile`, sans identifiant métier. Il traduit l’intention en appel `GET /api/v1/customers` ; Customer continue d’appliquer l’autorisation par `sub`.

MCP est conservé séparé de `customer-service` pour éviter de coupler Spring AI/MCP au domaine métier. Les alternatives « MCP dans Customer » ou « outil local REST sans MCP » restent pédagogiquement utiles à comparer, mais ne satisfont pas la découverte du protocole MCP prévue par le backlog.

### Décision 3 — Sécurité ajoutée par couches, sans fenêtre publique non sécurisée

La première étape Spring AI ne reçoit aucun trafic de données réelles : elle est testée dans le contexte de test ou local non publié. Le démonstrateur d’outil Java ne lit ni profil, ni JWT, ni base. Le serveur MCP pédagogique n’est accessible que sur le réseau de test/local interne.

Avant de relier l’outil à Customer :

- l’assistant valide le JWT ;
- MCP reçoit uniquement une identité validée ;
- `CUSTOMER` sans `OPERATOR` est exigé dans ce parcours ;
- la Gateway obtient une route explicite uniquement vers l’assistant ;
- MCP n’obtient aucune route publique ;
- Customer reste l’autorité finale d’accès.

La propagation contrôlée du JWT est retenue pour le POC. Le token exchange n’est pas nécessaire pour apprendre Spring AI/MCP et est reporté au durcissement production. Cette décision ne relâche aucune exigence : signature, issuer, expiration, audience, autorités et filtrage par `sub` restent obligatoires dans le parcours métier.

### Décision 4 — Une seule capacité utile au modèle

Le parcours final autorise zéro ou un appel à `getMyCustomerProfile`. Aucun identifiant, email, rôle ou sujet n’est accepté comme argument du modèle. Une instruction demandant un profil tiers, une écriture, un outil inconnu ou un contournement est refusée ou résulte en une réponse contrôlée.

Le résultat de MCP est validé comme DTO structuré avant la réponse textuelle. `FOUND`, `NOT_FOUND` et `UNAVAILABLE` sont distincts ; une valeur générée hors du DTO n’est jamais acceptée.

### Décision 5 — Réutiliser les mécanismes existants

- identité : JWT Keycloak et convertisseurs déjà présents ;
- routage : règle explicite de la Gateway ;
- métier : API REST Customer, filtrage par `sub` et 404 neutre ;
- corrélation : identifiant de requête déjà utilisé dans le projet ;
- tests : doubles déterministes, tests Spring et tests Gateway/Customer existants.

Cette réutilisation évite de créer au même moment un framework de sécurité ou de test agentique parallèle.

### Décision 6 — Fiabilité minimale mais suffisante

La première démonstration conserve les contrôles nécessaires à la sécurité : allow-list, un appel maximum, timeout, absence de retry métier, validation de sortie, messages contrôlés, tests de non-divulgation et absence de token dans les prompts/logs.

Sont reportés parce qu’ils augmentent l’effort sans être requis pour comprendre le parcours : token exchange, circuit breaker, évaluation statistique de prompts, budget/mesure de coût, backend de télémétrie et tracing distribué. Ils restent explicitement nécessaires avant une cible production, mais feront l’objet d’un jalon séparé.

### Décision 7 — Préparer US-08 sans anticiper Account

Les éléments suivants doivent rester réutilisables : politique de l’assistant, client MCP, validation du résultat, propagation d’identité et réponse neutre. US-08 ajoutera ultérieurement un serveur/outil Account et une orchestration bornée Account → Customer. Cette change ne crée pas de contrat Account et ne modifie pas US-08.

## Progression pédagogique

| Étape | Ce que nous apprenons | Résultat concret attendu |
|---|---|---|
| 1. Spring AI | configuration du modèle, `ChatClient`, prompt, réponse, erreur | requête simple répondue dans `assistant-service` et testée sans outil |
| 2. Outil Java | description d’outil, choix du modèle, arguments, retour structuré | modèle simulé et local peuvent invoquer un outil déterministe sans réseau |
| 3. MCP | découverte, client/serveur, transport, schéma d’outil, erreurs réseau | l’outil simple fonctionne via MCP sur le réseau interne local |
| 4. JWT Customer | propagation, audience, rôle, autorisation par `sub`, Gateway | l’assistant retourne uniquement le profil du client authentifié |
| 5. Fiabilité | injection, écriture, hallucination, absence et indisponibilité | résultats non fiables bloqués et réponses neutres testées |
| 6. Intégration | Compose, CI déterministe, documentation et limites | démonstration locale reproductible et préparation explicite d’US-08 |

## Risks / Trade-offs

- [Étapes éducatives laissées en production] → limiter le démonstrateur Java au test/local et vérifier sa suppression de l’allow-list finale.
- [MCP de test sans sécurité] → réseau interne uniquement et aucune donnée métier avant la couche JWT ; pas de route Gateway vers MCP.
- [Réduction prématurée des contrôles] → conserver identité, audiences, lecture seule, validation et tests adversariaux dans chaque étape métier.
- [Parcours trop simple] → considérer cet incrément comme socle ; US-08 apporte ensuite l’enchaînement de deux outils.
- [Couplage au modèle local] → utiliser l’abstraction Spring AI et une configuration externe.
- [Réponse non fiable] → résultat structuré, validation, champs allow-listés et réponse contrôlée.
- [Report confondu avec abandon] → nommer chaque mécanisme avancé dans la section suivante et le futur backlog de durcissement.

## Migration Plan

1. Ajouter et valider Spring AI sans aucun accès métier.
2. Valider le Tool Calling avec une fonction Java pédagogique.
3. Remplacer progressivement l’appel local par un serveur MCP interne sans données réelles.
4. Connecter l’outil final à Customer seulement après validation JWT et rôle.
5. Exposer l’endpoint assistant par une route Gateway explicite ; MCP reste interne.
6. Ajouter les tests de fiabilité, puis la démonstration locale et la documentation.
7. Rollback : désactiver la route assistant et les nouveaux services. Customer et les routes REST existantes restent inchangés.

## Open Questions

- Quel modèle local et quelle version seront utilisés pour la démonstration ?
- Le transport MCP retenu pour le POC est-il le transport HTTP standard compatible avec les versions Spring AI verrouillées ?
- Quels champs minimaux de CustomerProfile sont autorisés : `id` et `name`, ou `id`, `name` et `email` ?

Ces choix restent bornés à la configuration ou au contrat DTO. Ils ne changent ni les responsabilités des composants ni l’ordre pédagogique des étapes.

## Mécanismes explicitement différés

- Token exchange Keycloak et audiences/scopes de production : jalon de sécurité avant production.
- Circuit breaker et stratégie de retry résiliente : jalon d’exploitation après mesure des dépendances.
- Évaluation LLM statistique avancée : change dédié lorsqu’un modèle de référence est stabilisé.
- Métriques de tokens/coût et FinOps : à ajouter lorsque le fournisseur et l’unité de coût sont choisis.
- Tracing distribué complet et backend d’observabilité : dépend de la décision TD-13.

Le report ne signifie pas l’acceptation de données non fiables, d’un accès non authentifié ou d’outils d’écriture. Ces risques restent bloquants dans cette démonstration.
