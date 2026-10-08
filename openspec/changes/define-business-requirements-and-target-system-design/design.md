# Conception de la baseline des exigences — Étape 1 — Requirements / Exigences

## Objectif et périmètre

Ce document décrit uniquement la méthode d’établissement et d’examen de la
baseline des exigences de l’**Étape 1 — Requirements / Exigences**. Le System
Design cible demeure une phase ultérieure du projet, mais il est explicitement
**hors périmètre de cette évolution**. Aucune frontière de service, aucun
protocole, aucune infrastructure, aucun framework ni aucun modèle de
déploiement n’est choisi ou conçu ici.

Le workspace local et la référence Youssfi sont des sources de preuve
distinctes. Le workspace local contient la documentation du projet, mais
aucune source applicative. La référence a été examinée dans
`mohamedYoussfi/totale-micro-services-spring-ai-mcp-angular-telegram-discord`,
branche `main`, commit `bf4c7f2750e4c731662e8138023fd8e6b4e4a475`.

## Niveaux de preuve et statut des exigences

Chaque affirmation importante de la spécification est assortie d’un niveau de
preuve et d’un statut décisionnel :

- **OBSERVÉ** — directement étayé par un fichier source ou la documentation
  actuelle du projet. Les constats issus de la référence citent un chemin dans
  la révision Youssfi retenue et ne doivent pas être présentés comme des
  comportements locaux.
- **DÉDUIT** — besoin candidat concernant le produit ou sa qualité, inféré des
  fonctionnalités observées ou de l’objectif déclaré du projet. Il ne s’agit
  pas d’un fait vérifié à l’exécution.
- **DÉCIDÉ** — explicitement choisi pour le projet par son propriétaire. Ce
  statut s’applique uniquement aux objectifs du projet et aux contraintes de
  processus expressément formulés pour cette évolution ; il ne signifie pas qu’une
  décision d’architecture a été prise.
- **À CONFIRMER** — nécessite une validation du propriétaire du produit ou du
  Business Analyst, ou des éléments de preuve actuellement indisponibles.

Une capacité observée dans la référence ne devient pas automatiquement une
exigence approuvée pour le produit cible. Les exigences candidates déduites de
cette capacité restent marquées **À CONFIRMER** tant que le périmètre, les
acteurs et les règles métier n’ont pas été validés.

## Organisation des exigences

La spécification des exigences organise le contenu selon les thèmes suivants :

1. Contexte métier et objectif du produit.
2. Acteurs et parties externes.
3. Périmètre, exclusions et cas d’usage.
4. Exigences fonctionnelles candidates et preuves traçables.
5. Exigences non fonctionnelles candidates, sans objectifs numériques inventés.
6. Contraintes du projet et décisions explicites.
7. Hypothèses et questions produit non résolues.

Les cas d’usage décrivent des objectifs et des résultats observables, et non
des séquences entre services ou des composants d’implémentation. Les énoncés
fonctionnels décrivent des capacités comme consulter des clients ou des
comptes ; ils ne prescrivent ni REST, ni Feign, ni événements, ni bases de
données, ni frontends, ni architecture d’IA.

## Inventaire des preuves ayant servi à établir les exigences candidates

Les chemins suivants de la référence ont été examinés pour établir
l’inventaire initial des capacités :

- API REST Customer et comportement métier :
  `customer-service/src/main/java/net/youssfi/customerservice/controllers/CustomerRestController.java`,
  `customer-service/src/main/java/net/youssfi/customerservice/service/CustomerService.java`,
  `customer-service/src/main/java/net/youssfi/customerservice/entities/Customer.java`.
- API REST des comptes, modèle métier et comportement :
  `ebank-service/src/main/java/net/youssfi/ebankservice/controllers/EbankRestController.java`,
  `ebank-service/src/main/java/net/youssfi/ebankservice/services/EbankService.java`,
  `ebank-service/src/main/java/net/youssfi/ebankservice/entities/BankAccount.java`.
- Interface conversationnelle et orchestration de l’IA :
  `ebank-bot/src/main/java/net/youssfi/ebankbot/controllers/EbankChatbotController.java`,
  `ebank-bot/src/main/java/net/youssfi/ebankbot/agents/EbankAIAgent.java`.
- Canaux de messagerie :
  `ebank-bot/src/main/java/net/youssfi/ebankbot/telegram/TelegramBot.java`,
  `ebank-bot/src/main/java/net/youssfi/ebankbot/doscord/DiscordBot.java`.
- Interactions depuis les navigateurs :
  `angular-front/src/app/accounts/accounts.ts`,
  `angular-front/src/app/bot-ui/bot-ui.ts`,
  `ebank-ang-front/src/app/accounts/accounts.ts`,
  `ebank-ang-front/src/app/bot-ui/bot-ui.ts`.

### Traçabilité des capacités observées vers les cas d’usage candidats

| Cas d’usage candidat | Capacité observée dans la référence | Sources consultées |
|---|---|---|
| UC-01 — Consulter les clients | `GET /customers` renvoie la liste Customer. | `customer-service/src/main/java/net/youssfi/customerservice/controllers/CustomerRestController.java`; `customer-service/src/main/java/net/youssfi/customerservice/service/CustomerService.java` |
| UC-02 — Consulter le détail d’un client | `GET /customers/{id}` recherche un client par identifiant. | `customer-service/src/main/java/net/youssfi/customerservice/controllers/CustomerRestController.java`; `customer-service/src/main/java/net/youssfi/customerservice/service/CustomerService.java` |
| UC-03 — Créer un client | `POST /customers` reçoit un objet Customer; les champs métier observés sont `name` et `email`. | `customer-service/src/main/java/net/youssfi/customerservice/controllers/CustomerRestController.java`; `customer-service/src/main/java/net/youssfi/customerservice/entities/Customer.java` |
| UC-04 — Consulter les comptes | `GET /accounts` renvoie la liste BankAccount. | `ebank-service/src/main/java/net/youssfi/ebankservice/controllers/EbankRestController.java`; `ebank-service/src/main/java/net/youssfi/ebankservice/services/EbankService.java` |
| UC-05 — Consulter le détail d’un compte | `GET /accounts/{id}` recherche un compte par identifiant. | `ebank-service/src/main/java/net/youssfi/ebankservice/controllers/EbankRestController.java`; `ebank-service/src/main/java/net/youssfi/ebankservice/services/EbankService.java` |
| UC-06 — Créer un compte | `POST /accounts` reçoit un objet BankAccount; champs observés : `type`, `balance`, `customerId` (ainsi que les champs générés/retournés). | `ebank-service/src/main/java/net/youssfi/ebankservice/controllers/EbankRestController.java`; `ebank-service/src/main/java/net/youssfi/ebankservice/services/EbankService.java`; `ebank-service/src/main/java/net/youssfi/ebankservice/entities/BankAccount.java` |
| UC-07 — Poser une question sur les clients ou les comptes | Chat Web et messages Telegram/Discord sont transmis à l’agent conversationnel; cela n’établit pas que chaque requête interroge les outils ni que la réponse est exacte. | `ebank-bot/src/main/java/net/youssfi/ebankbot/controllers/EbankChatbotController.java`; `ebank-bot/src/main/java/net/youssfi/ebankbot/agents/EbankAIAgent.java`; `ebank-bot/src/main/java/net/youssfi/ebankbot/telegram/TelegramBot.java`; `ebank-bot/src/main/java/net/youssfi/ebankbot/doscord/DiscordBot.java` |

Les fichiers Angular consultés confirment une interaction navigateur pour la
liste de comptes et le chat; ils ne démontrent pas que les interfaces assurent
toutes les opérations Customer/EBank. Les constats portent sur le code de la
référence à la révision indiquée et ne constituent pas des tests d’exécution.

Ces preuves n’établissent pas, à elles seules, la propriété du produit, les
règles d’autorisation, les catégories d’utilisateurs, les critères d’éligibilité
des comptes, le comportement des transactions, la conservation des données,
les objectifs de qualité opérationnelle ni l’inclusion de ces capacités dans
notre produit.

## Approche de révision

La proposition et la spécification doivent être examinées avant toute
implémentation ou tout System Design cible. Ce dernier reste hors périmètre de
cette évolution. La dernière tâche doit vérifier les
points suivants :

- chaque exigence fonctionnelle candidate porte une étiquette de preuve et un
  statut pour le projet cible ;
- aucune capacité déduite n’est implicitement promue au rang de périmètre
  décidé ;
- les besoins non fonctionnels sont exprimés comme résultats attendus ou
  questions, sans SLO ni solution technique inventés ;
- les questions métier non résolues restent **À CONFIRMER** ;
- aucune prescription d’architecture n’a été introduite dans la baseline des
  exigences.

La baseline pourra être affinée à la suite des retours du propriétaire du
produit. Toute proposition d’architecture ultérieure devra s’appuyer sur les
exigences acceptées, et non utiliser cette évolution pour introduire
rétroactivement des décisions d’architecture.
