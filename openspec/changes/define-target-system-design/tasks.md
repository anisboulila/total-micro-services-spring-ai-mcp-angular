# Tâches — Définir le Target System Design — Étape 2 — Architecture cible

> Périmètre : **conception uniquement**. Aucun code applicatif, aucune
> dépendance, aucune configuration runtime. Chaque tâche produit ou vérifie de
> la documentation. Les statuts **OBSERVÉ / DÉDUIT / DÉCIDÉ / À CONFIRMER**
> doivent être utilisés. Les exigences de l’Étape 1 restent candidates.

## 1. Préparer le cadre de conception

- [x] 1.1 Relire `PROJECT-CONTEXT.md`, la spécification `business-requirements` et les analyses de la référence ; dresser la table des exigences sources (UC, NFR, contraintes) et la liste des hypothèses.
- [x] 1.2 Vérifier que chaque exigence reprise conserve son statut d’Étape 1 et que les exigences **À CONFIRMER** sont identifiées comme telles dans le design.

## 2. Structure, frontières et données

- [x] 2.1 Valider le style architectural (DEC-01) : alternatives, compromis, conditions de repli vers un monolithe modulaire.
- [x] 2.2 Valider les composants, responsabilités et exclusions (DEC-02) ainsi que le contexte/diagramme logique.
- [x] 2.3 Valider la propriété des données, la persistance et les règles de cohérence (DEC-03, DEC-04, DEC-05), y compris l’absence de transaction distribuée.

## 3. Communication

- [x] 3.1 Valider REST synchrone par défaut, contrats, erreurs et pagination (DEC-06).
- [x] 3.2 Valider le périmètre conditionnel à H-6 des événements Kafka, l’outbox, la gestion des doublons et le statut **À CONFIRMER** du consommateur (DEC-07).
- [x] 3.3 Valider l’API Gateway à routes statiques et la non-adoption de la découverte de services et du Config Server (DEC-08, DEC-09, DEC-14).

## 4. Qualités transverses

- [x] 4.1 Valider le modèle de sécurité : OIDC/Keycloak, validation à chaque saut, autorisation, propagation d’identité (DEC-10).
- [x] 4.2 Valider la politique de résilience : timeouts, retries, circuit breaker, refus de création sans client valide, idempotence (DEC-11).
- [x] 4.3 Valider disponibilité, performance, scalabilité et le cache Redis retenu à des fins pédagogiques, avec un périmètre initial limité et ses déclencheurs de réévaluation (DEC-12).
- [x] 4.4 Valider l’observabilité, la gestion des erreurs, la configuration et la séparation des secrets (DEC-13, DEC-14).

## 5. AI, canaux et frontend

- [x] 5.1 Valider la forme de l’assistant conditionnel à UC-07, la lecture seule et la politique LLM (DEC-15).
- [x] 5.2 Valider MCP conditionnel à UC-07 (justification, alternatives, exposition interne, autorisation) ainsi que la non-adoption de l’Agent et du RAG (DEC-16, DEC-17).
- [x] 5.3 Valider le frontend unique et la portée des canaux conversationnels (DEC-18).

## 6. Exploitation et validation

- [x] 6.1 Valider conteneurisation/Compose, position sur Kubernetes/Helm et CI/CD minimale (DEC-19, DEC-20, DEC-21).
- [x] 6.2 Valider la stratégie de tests et de validation (DEC-22).

## 7. Revue de clôture

- [x] 7.1 Vérifier la traçabilité de chaque `DEC-xx` vers une exigence/contrainte, ses alternatives, ses compromis et son statut.
- [x] 7.2 Vérifier qu’aucune technologie n’est ajoutée sans besoin ou objectif pédagogique déclaré, et que les risques et questions ouvertes sont à jour.
- [x] 7.3 Vérifier la couverture des questions d’entretien et la cohérence entre `proposal.md`, `spec.md`, `design.md`, `tasks.md` et `PROJECT-CONTEXT.md`.
- [x] 7.4 Confirmer l’absence de code, de dépendance, de changement runtime et de copie de la référence, puis soumettre le design à revue avant toute change d’implémentation.
