# Définir les exigences métier — Étape 1 — Requirements / Exigences

## Pourquoi

Le projet dispose d’une baseline documentaire des fonctionnalités et de
l’architecture observées dans la référence pédagogique de Mohamed Youssfi. Le
workspace local ne contient toujours pas d’implémentation applicative. L’étape
suivante consiste à reconstituer et valider les exigences métier et système de
notre propre projet, créé de zéro, avant d’entamer le System Design cible.

La référence apporte des éléments de preuve sur les fonctionnalités qu’elle
démontre, mais elle ne définit ni le périmètre de notre produit, ni ses acteurs,
ses règles métier, ses objectifs de qualité ou son architecture. Les exigences
déduites de son comportement doivent donc rester identifiées comme telles et
soumises à confirmation tant que le propriétaire du projet n’a pas pris de
décision explicite.

## Modifications

- Établir une baseline d’exigences révisable, assortie de niveaux de preuve,
  couvrant le contexte métier, les acteurs, le périmètre, les cas d’usage, les
  besoins fonctionnels et non fonctionnels, les contraintes, les hypothèses et
  les questions ouvertes du projet.
- Relier les capacités observées à la révision précise des sources Youssfi et
  séparer les faits du projet local de ceux de la référence.
- Identifier les exigences candidates déduites de la référence sans les
  présenter comme des décisions validées par le propriétaire du produit.
- Consigner les décisions de projet explicitement formulées par le propriétaire,
  notamment la reconstruction de zéro et l’objectif d’un projet d’allure
  professionnelle, mais gérable à des fins pédagogiques.

## Périmètre

Cette évolution couvre **uniquement l’Étape 1 — Requirements / Exigences**.
Le System Design cible reste une phase ultérieure du projet, mais se trouve
explicitement **hors périmètre de cette évolution**. Celle-ci ne conçoit pas
l’architecture cible, ne prend aucune décision d’architecture, ne crée pas
d’ADR et n’implémente aucun comportement applicatif. Les exigences produites
doivent être examinées avant la phase ultérieure de System Design.

## Capacités

### Nouvelles capacités

- `business-requirements` : exigences métier et système de notre projet,
  fondées sur des preuves et soumises à révision, séparées de l’implémentation
  de référence et des futurs choix d’architecture.

### Capacités modifiées

- Aucune.

## Impact

- Ajoute, dans cette évolution, une proposition OpenSpec, une spécification des
  exigences, des notes de conception consacrées à leur modélisation et des
  tâches d’exécution.
- S’appuie sur `PROJECT-CONTEXT.md`, sur l’évolution terminée
  `project-foundation-architecture-baseline` ainsi que sur la référence Youssfi
  pointée sur la branche `main`, commit
  `bf4c7f2750e4c731662e8138023fd8e6b4e4a475`.
- Le workspace local reste distinct du dépôt de référence. Aucun code de cette
  référence n’est copié ni intégré.
- Aucun code applicatif, aucune configuration d’exécution, dépendance ou
  infrastructure n’est modifié.
- L’évolution de baseline terminée reste disponible comme source documentaire ;
  cette proposition ne la modifie pas et ne l’archive pas.
