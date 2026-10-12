# Périmètre pédagogique — décisions et clarifications proposées

## Cadre de lecture

Cette change clarifie les décisions du propriétaire et leur relation avec
l’Étape 1 (exigences), l’Étape 2 (Target System Design) et l’Étape 3 (ADR).
Elle ne réécrit pas ces documents historiques et ne vaut pas validation métier
supplémentaire.

Les choix du propriétaire ci-dessous sont **DÉCIDÉS**. Les scénarios et
paramètres marqués **À CONFIRMER** ne sont pas promus en exigences approuvées.

## Décisions explicites du propriétaire

| Sujet | Statut dans le périmètre pédagogique | Ce que cela ne valide pas |
|---|---|---|
| Projet fil rouge pédagogique | **DÉCIDÉ** | Aucun besoin métier particulier |
| Kafka dans le projet | **DÉCIDÉ** | Le bénéfice métier et le consommateur concret |
| Producteur, topic et consommateur réels | **DÉCIDÉ** comme objectif d’intégration | Le scénario d’affaires à implémenter |
| Spring AI et MCP | **DÉCIDÉ** comme objectifs d’intégration | Le détail et l’approbation produit de UC-07 |
| MCP lecture seule et contrôles des services | **DÉCIDÉ** comme contrainte de conception | Les rôles et autorisations détaillés |
| OpenSpec/SDD | **DÉCIDÉ** comme processus | Le contenu des futurs changes d’implémentation |

## Kafka : harmonisation de H-6 et DEC-07

DEC-07 reste la source technique de référence pour les types d’événements et
leurs garanties. La nouvelle clarification porte sur le statut de l’adoption,
pas sur une redéfinition de l’outbox ou du protocole.

- **Adoption pédagogique de Kafka : DÉCIDÉE.** Elle n’est plus conditionnée à
  H-6.
- **Producteur, topic et consommateur : DÉCIDÉS comme objectif d’intégration
  réelle.**
- **Consommateur métier concret et valeur de son scénario : À CONFIRMER.**
  `audit-service` reste un candidat de DEC-07, pas un besoin confirmé.
- H-6 ne doit plus servir de condition à l’étude ou à l’adoption pédagogique de
  Kafka. Il continue de signaler l’incertitude sur le scénario métier et le
  consommateur précis.
- Préserver `CustomerCreated` et `AccountCreated`, transactional outbox,
  livraison au moins une fois, idempotence producteur et consommateur,
  partitionnement par agrégat, retries bornés puis file d’échecs, et
  versionnement tels que décrits dans DEC-07.
- Ne pas inclure Kafka Streams, `statistics-service` ou un scénario de
  statistiques dérivées dans le périmètre initial. Si un tel besoin apparaît,
  il fera l’objet d’un change SDD/OpenSpec distinct.

Aucun consommateur alternatif n’est retenu dans cette proposition. En
particulier, elle ne substitue pas une vue de synthèse des comptes au candidat
`audit-service`.

## Spring AI/MCP : intégration décidée, scénario à approuver

L’intégration pédagogique de Spring AI et MCP est **DÉCIDÉE** par le
propriétaire. Elle ne transforme pas automatiquement UC-07 en exigence produit
validée : l’acteur, la question retenue, les données accessibles et les
autorisations restent à préciser.

### Parcours candidat : « À quel client appartient ce compte ? »

Ce parcours est une **proposition**, non une décision validée :

1. L’utilisateur demande à l’assistant l’identité du client associé à un compte
   identifié.
2. Un outil en lecture seule appelle l’API métier du service Account pour
   consulter le compte et obtenir sa référence `customerId`.
3. Un outil en lecture seule appelle l’API métier du service Customer pour
   consulter ce client.
4. L’assistant formule une réponse à partir des champs que les autorisations
   de l’utilisateur permettent de retourner.

Les opérations de lecture correspondent à la consultation d’un compte (UC-05)
et d’un client (UC-02) telles que décrites dans la baseline. Le parcours devra
être revu avec les contrats cibles exacts avant d’être spécifié pour
implémentation.

### Garde-fous de conception proposés

- MCP reste un adaptateur interne au-dessus des API métier; aucun accès direct
  du modèle aux bases.
- Chaque service propriétaire valide le jeton et applique l’autorisation de
  l’utilisateur pour les données demandées. Le modèle et la passerelle ne
  remplacent pas cette décision.
- Les outils ne modifient ni client ni compte et ne peuvent pas exposer plus de
  données que l’API autorisée.
- Les règles de rôles, la propagation d’identité vers les outils, la politique
  de réponse lorsqu’un accès est refusé et les champs permis restent
  **À CONFIRMER**.
- L’existence de l’intégration Spring AI/MCP ne décide pas d’un Agent, du RAG,
  d’un fournisseur LLM externe ou de canaux Telegram/Discord. Les positions
  DEC-17/18 restent inchangées.

## Correspondance avec les décisions antérieures

| Élément historique | État dans la source existante | Clarification proposée pour les travaux futurs |
|---|---|---|
| H-6 | Hypothèse sur l’utilité d’un consommateur métier | Ne conditionne plus Kafka pédagogique; valeur et scénario du consommateur restent **À CONFIRMER** |
| DEC-07 | Kafka décidé sous condition H-6; consommateur concret à confirmer | Adoption pédagogique **DÉCIDÉE**; préserver les garanties et laisser le scénario métier ouvert |
| ADR-03 | REST par défaut; Kafka conditionnel à H-6 | Kafka obligatoire pédagogiquement; REST demeure le défaut pour requêtes/réponses |
| ADR-04 | Outbox et idempotence conditionnelles à H-6 | Préserver ces mécanismes pour les événements Kafka; le consommateur et son traitement métier restent à confirmer |
| UC-07 | Cas conversationnel **DÉDUIT / À CONFIRMER** | Intégration AI/MCP pédagogique décidée; détail et statut produit du parcours restent **À CONFIRMER** |
| ADR-12 | Assistant et MCP conditionnels à UC-07 | Intégration pédagogique décidée; utiliser un parcours approuvé, MCP lecture seule et sécurisé |
| DEC-15/16 | Forme et contrôles d’architecture décrits dans l’Étape 2 | Préserver les choix non concernés; préciser leur application au parcours approuvé |
| DEC-17 | Agent et RAG non retenus sans besoin distinct | Inchangé |
| DEC-20 | Kubernetes optionnel, Helm différé | Inchangé |

Les formulations historiques ne sont pas modifiées. Après approbation, cette
clarification sert de référence de statut pour les changes ultérieures.

## Questions à arbitrer

- Le besoin métier servi par le consommateur Kafka et l’adoption ou non
  d’`audit-service`.
- Le topic ou les topics nécessaires et le consommateur responsable du
  traitement; le choix ne doit pas altérer les contrats DEC-07 sans revue.
- L’approbation ou le remplacement du parcours candidat « À quel client
  appartient ce compte ? ».
- Les acteurs, rôles, autorisations, propagation d’identité et champs de données
  autorisés pour les outils MCP.
- La portée produit de UC-07, distincte de l’objectif pédagogique Spring AI/MCP.
- La politique de modèle LLM et l’éventuel envoi de données à un fournisseur
  externe, conformément aux points ouverts de DEC-15.

## Roadmap indicative après validation documentaire

La séquence ci-dessous reprend les étapes de la trajectoire de l’Étape 2, en
retirant les conditions d’adoption pédagogique qui ont été levées par le
propriétaire. Les choix d’implémentation et leur ordre détaillé restent à
spécifier dans des changes dédiées.

1. Services Customer/Account et PostgreSQL, sur les capacités métier confirmées.
2. Sécurité Keycloak, résilience et idempotence.
3. API Gateway et frontend Angular.
4. Kafka : événements et consommateur métier après décision du scénario concret.
5. Observabilité.
6. Assistant Spring AI puis MCP pour le parcours approuvé.
7. Docker Compose et CI/CD; industrialisation progressive.
8. Kubernetes uniquement si sa valeur pédagogique et opérationnelle est
   justifiée; Helm reste distinct et différé.

Cette roadmap ne constitue pas une autorisation d’implémenter les scénarios
encore ouverts.
