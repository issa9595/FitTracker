# Phase 7 — Tests, qualité, CI/CD enrichie (version ALLÉGÉE pour la deadline)

> Dernière phase. Objectif : couverture JaCoCo mesurée et publiée, pipeline de **release** sur tag
> `v*` poussant l'image sur GHCR, et documentation de la stratégie de test. Cucumber = optionnel,
> ignoré sauf temps restant.
>
> Branche : `feature/phase-7-tests-cicd`, partant de `main` **après merge de la PR #6**.

## 1. JaCoCo — couverture mesurée et publiée

### pom.xml
Ajouter le `jacoco-maven-plugin` (0.8.12) avec **deux agents** (surefire + failsafe) car les tests
sont répartis entre unitaires (`*Test`, surefire) et intégration (`*IT`, failsafe) :

```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>0.8.12</version>
  <executions>
    <!-- Agent pour les tests unitaires (surefire) -->
    <execution><id>prepare-agent</id><goals><goal>prepare-agent</goal></goals></execution>
    <!-- Agent pour les tests d'intégration (failsafe) -->
    <execution><id>prepare-agent-integration</id><goals><goal>prepare-agent-integration</goal></goals></execution>
    <!-- Fusion unit + IT puis rapport -->
    <execution>
      <id>merge</id><phase>verify</phase><goals><goal>merge</goal></goals>
      <configuration>
        <fileSets><fileSet>
          <directory>${project.build.directory}</directory>
          <includes><include>jacoco.exec</include><include>jacoco-it.exec</include></includes>
        </fileSet></fileSets>
        <destFile>${project.build.directory}/jacoco-merged.exec</destFile>
      </configuration>
    </execution>
    <execution>
      <id>report</id><phase>verify</phase><goals><goal>report</goal></goals>
      <configuration><dataFile>${project.build.directory}/jacoco-merged.exec</dataFile></configuration>
    </execution>
    <execution>
      <id>check</id><phase>verify</phase><goals><goal>check</goal></goals>
      <configuration>
        <dataFile>${project.build.directory}/jacoco-merged.exec</dataFile>
        <rules>
          <!-- Règle services >= 80% lignes -->
          <rule>
            <element>PACKAGE</element>
            <includes><include>com.fittracker.*</include></includes>
            <limits><limit><counter>LINE</counter><value>COVEREDRATIO</value><minimum>0.70</minimum></limit></limits>
          </rule>
        </rules>
        <!-- Exclure DTO/entities/config/Application du dénominateur -->
        <excludes>
          <exclude>**/dto/**</exclude>
          <exclude>**/*Application.*</exclude>
          <exclude>**/config/**</exclude>
        </excludes>
      </configuration>
    </execution>
  </executions>
</plugin>
```

**IMPORTANT (anti-échec CI) :** d'abord lancer `./mvnw verify` et **lire le taux réel** dans
`target/site/jacoco/index.html`. Viser la cible du brief (**≥ 80% sur les services**, ≥ 70% global) :
si on y est, fixer la règle `check` à ce niveau ; sinon **ajouter quelques tests unitaires de
services** (Mockito + AssertJ, nommage `should_<expected>_when_<context>`) pour atteindre 80% sur les
`*Service`, plutôt que de baisser le seuil. Ne pas faire échouer la CI sur un seuil non atteint :
ajuster règle ET/OU tests de façon cohérente avec le brief.

Les entités (getters/setters), DTO, config et `FitTrackerApplication` sont exclus du calcul (sinon ils
diluent le ratio services).

### CI (ci.yml)
Dans le job `build`, après `./mvnw verify`, publier le rapport en artefact :

```yaml
      - name: Upload JaCoCo report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: jacoco-report
          path: target/site/jacoco/
          retention-days: 7
```

### Badge couverture (README)
Le plus simple sans service externe : action `cicirello/jacoco-badge-generator` qui lit l'exec et
écrit `badges/jacoco.svg`, référencé dans le README. Si trop lourd, se contenter d'une mention
textuelle « Couverture : rapport JaCoCo en artefact CI » + lien vers l'onglet Actions.

## 2. release.yml — release sur tag `v*` → image sur GHCR

Nouveau workflow `.github/workflows/release.yml` :

```yaml
name: Release
on:
  push:
    tags: ['v*']
permissions:
  contents: write   # créer la release
  packages: write   # push GHCR
jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: docker/setup-buildx-action@v3
      - name: Login GHCR
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - name: Build & push image
        uses: docker/build-push-action@v6
        with:
          context: .
          push: true
          tags: |
            ghcr.io/issa9595/fittracker:${{ github.ref_name }}
            ghcr.io/issa9595/fittracker:latest
          cache-from: type=gha
          cache-to: type=gha,mode=max
      - name: Create GitHub Release (notes auto)
        uses: softprops/action-gh-release@v2
        with:
          generate_release_notes: true
```

Vérif : `git tag v0.1.0 && git push origin v0.1.0` → l'image apparaît dans GHCR et une release est
créée. (À tester une fois la PR mergée, le tag se pose sur `main`.)

## 3. docs/testing.md
- Pyramide : unitaires (Mockito + AssertJ sur les services), intégration (Testcontainers Postgres pour
  ORM/relations/RGPD), bout-en-bout WebSocket (`WebSocketStompClient`).
- Convention de nommage `should_<expected>_when_<context>`.
- Commandes : `./mvnw test` (unit), `./mvnw verify` (tout + couverture), où lire le rapport JaCoCo.
- Récap de la couverture par couche, ce qui est exclu et pourquoi.

## 4. Mise à jour finale
- README : badge/section couverture, mention release GHCR, cocher phase 7.
- `taches.md` : phase 7 ✅, documentation finale complète.
- `docs/security.md`, `docs/architecture.md`, `docs/websockets.md`, `docs/api-examples.md`,
  `docs/twelve-factor.md`, `docs/testing.md` : vérifier qu'ils sont tous présents (livrable §5 du brief).

## Définition de « terminé »
`./mvnw verify` vert avec rapport JaCoCo généré et règle de couverture respectée ; `release.yml`
présent et valide ; `docs/testing.md` écrit ; README à jour. PR vers main, CI verte. C'est la
dernière phase → après merge, le projet est complet.

## Pièges connus
- Double agent JaCoCo : sans `prepare-agent-integration` + `merge`, la couverture des `*IT` (failsafe)
  est ignorée et le ratio chute. Bien fusionner `jacoco.exec` (unit) et `jacoco-it.exec` (IT).
- L'agent ajoute `argLine` ; si surefire/failsafe ont déjà un `argLine` custom, le combiner
  (`@{argLine}`), sinon l'agent ne s'attache pas.
- Ne pas faire échouer la CI sur un seuil trop ambitieux : mesurer d'abord, ajuster ensuite.
- GHCR : le package sera privé par défaut (lié au repo privé) ; c'est attendu.
