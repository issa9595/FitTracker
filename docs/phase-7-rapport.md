# Phase 7 — Tests, couverture & CI/CD enrichie

**Branche :** `feature/phase-7-tests-cicd` → PR #7 mergée dans `main` (merge commit `305ed85`, 2026-06-16).
**Statut :** ✅ Terminée et mergée. Dernière phase → **projet complet**.

## 1. Objectif (version allégée pour la deadline)

Mesurer et publier la couverture JaCoCo, garantir une règle de couverture conforme au brief
(≥ 80 % services / ≥ 70 % global), ajouter un pipeline de release sur tag `v*` poussant l'image sur
GHCR, et documenter la stratégie de test. Cucumber volontairement ignoré. Aucun changement de
comportement des phases 1→6.

## 2. Méthode couverture (mesure d'abord, règle ensuite)

Point critique : la règle n'a pas été fixée au hasard.

1. JaCoCo configuré en **mesure seule** (agents + merge + report, sans `check`).
2. `./mvnw verify` lancé, taux réels lus dans le rapport (CSV) → **sous la cible** (services 50,6 %,
   global 62,0 %).
3. Ajout de tests unitaires de services pour **dépasser** la cible.
4. **Ensuite seulement**, règle `check` fixée au seuil du brief, avec marge.

| Scope | Avant | Après | Cible brief |
|---|---|---|---|
| `*Service` (lignes, agrégé) | 50,6 % | **98,5 %** | ≥ 80 % |
| Service le plus bas (`JwtService`) | — | **88,9 %** | ≥ 80 % |
| Global (hors dto/config/Application) | 62,0 % | **79,2 %** | ≥ 70 % |

## 3. Livrables

### a) JaCoCo double agent (`pom.xml`)
- `prepare-agent` (unitaires Surefire → `jacoco.exec`)
- `prepare-agent-integration` (`*IT` Failsafe → `jacoco-it.exec`)
- `merge` → `jacoco-merged.exec` (sinon la couverture des IT serait ignorée — piège connu)
- `report` (HTML/XML/CSV) + `check` sur la fusion
- Règle : `CLASS *Service ≥ 0.80` **et** `BUNDLE ≥ 0.70` ; exclusions `**/dto/**`, `**/config/**`,
  `**/*Application.*`. Une couverture sous le seuil fait échouer `verify`/CI.

### b) 45 tests unitaires de services (Mockito + AssertJ)
Nommage `should_<expected>_when_<context>`, couvrant les branches métier :
`UserServiceTest` (10), `ProgramServiceTest` (10, validation dates/ownership),
`TrainingSessionServiceTest` (12, création + event, ownership, détection de PR),
`FollowServiceTest` (10, forbidden/conflict/not-found), `RgpdExportServiceTest` (3, agrégation RGPD).

### c) CI (`.github/workflows/ci.yml`)
Job `build` : upload de `target/site/jacoco/` en artefact `jacoco-report-java-21` avec
`if: always()` (disponible même si la règle échoue, pour diagnostiquer).

### d) Release CD (`.github/workflows/release.yml` — nouveau)
Sur tag `v*` : login GHCR → build + push `ghcr.io/issa9595/fittracker:<tag>` et `:latest` (cache
GHA) → GitHub Release avec notes auto.

### e) Documentation
- `docs/testing.md` créé : pyramide (unit/intégration/WebSocket), double agent JaCoCo, commandes,
  couverture par couche, exclusions et pourquoi.
- `README.md` : état projet complet, section couverture + Release GHCR, lien `testing.md`, roadmap à jour.
- `taches.md` : phases 4-6 mergées, phase 7 cochée, livrable §5 complet.

## 4. Vérifications (preuves)

- `./mvnw verify` local : **71 tests unitaires + 6 IT**, « All coverage checks have been met. »,
  BUILD SUCCESS.
- CI sur PR #7 : **4 jobs verts** (Lint, Build & tests, Docker build, Compose stack).
- Artefact `jacoco-report-java-21` (520 Ko) publié par le job `build`.
- Livrable §5 — **6 docs présents** : architecture · twelve-factor · security · api-examples ·
  websockets · testing.

## 5. Commits (Conventional Commits, atomiques, bisect-safe)

Tests avant la règle de couverture :

```
13476b7  test(services): tests unitaires Mockito+AssertJ des services métier
60c2481  build(jacoco): couverture double agent (surefire+failsafe) + règle 80/70
3c6aafc  ci:   publie le rapport JaCoCo en artefact + release GHCR sur tag v*
103f2fb  docs: stratégie de test (testing.md), README couverture/release, phase 7
```

## 6. Non fait (assumé)

- **Cucumber** : ignoré (version allégée), documenté dans `docs/testing.md` §6.
- **Badge couverture** : mention textuelle + lien artefact plutôt qu'un service externe.
- **`release.yml` non testé** : ne se vérifie qu'avec un tag sur `main` (post-merge), optionnel.

## 7. Reste optionnel

- Tester la release : `git tag v0.1.0 && git push origin v0.1.0` (package GHCR privé par défaut).
- Ménage des branches mergées (`feature/phase-4/5/6/7`).
