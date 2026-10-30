# Electrical Age, Minecraft 26.x port

Electrical Age (mod id `eln`) simulates electrical, thermal and mechanical networks in Minecraft. It uses a
circuit (MNA) solver, a thermal network and a fluid sim. This branch ports the 1.12 version (upstream branch
`ports/1.12`) to Minecraft 26.3 on NeoForge 26.3 (beta, pinned in `gradle.properties`), with Java 25 and Kotlin.
`README.md` and `CONTRIBUTING.md` still describe older versions. For this port, use this file and `docs/`.

## Build and test

JDKs: Gradle and the mod use JDK 25, and NeoForge's data and game-test runs also want JDK 21.
`gradle.properties` reads both from the `JDK25` and `JDK21` environment variables (no auto-download).
`nix develop` (flake.nix) sets them, along with `JAVA_HOME`, Python for `tools/icons`, and the client libraries.

| Task | Command |
|---|---|
| Build the jar (`build/libs/eln-26.3-0.1.0-port.jar`) | `./gradlew build` |
| Unit and property tests (JUnit 5 + vintage, jqwik) | `./gradlew test` |
| All GameTests headless (exit code = number of failed tests) | `./gradlew runGameTestServer` |
| Cable benchmark instead of the tests | `./gradlew runGameTestServer -PelnBench=result.json [-PelnBenchLabel=name]` |
| Datagen: lang, tags, recipes, item models into `src/generated/resources` | `./gradlew runClientData` |
| Dev client / server | `./gradlew runClient` / `./gradlew runServer` |

- `src/generated/resources` is committed. Rerun `runClientData` after you change items, models, recipes or lang,
  and commit the output with the change.
- A local wrapper script (for example `build26.sh [task]`) only needs to set the JDK variables and capture the
  log. The build needs nothing else.
- Before you push, `build`, `test` and `runGameTestServer` should all pass. The golden comparisons
  (`golden/1.12/*.json`) are GameTests. Every difference from them must be explained (see `docs/testing.md`).

## Commits and branches

- Work on `port-26` (or a topic branch off it). Push with `git push upstream HEAD:ports/26.3`; upstream is
  github.com/Electrical-Age/ElectricalAge.
- One logical change per commit. Messages are plain English: a short subject that says what changed for the
  player or the code (e.g. "Keep nodes this version cannot load instead of deleting them"), then a body that
  says why and notes any save-format or gameplay impact. Note Baughn's decisions where they drove the change.
- Never change saved NBT keys, `eln-ids.json` ids or registry names without an upgrade path
  (see `docs/persistence.md`).

## Coding conventions (as observed)

- Java and Kotlin are mixed, both under `src/main/java/mods/eln/`. The 1.12 heritage (sim, most devices) is Java.
  Kotlin covers mechanical devices, some chips and items, and `node/published.kt`. Keep new code in the language
  of the code around it, and don't group classes by language.
- 4-space indent, LF line endings, UTF-8 (`.editorconfig`). Mojang (official) names throughout.
- Comments are terse javadoc sentences that state what the code does and why, for example
  `/** readFromNBT(nbt, "") on every INBTTReady entry of the lists, in order; other entries are skipped. */`.
  Gameplay decisions are attributed in comments ("Baughn: ..."). Don't cite files outside the repo, and don't
  leave phase markers ("comes with P3").
- Behaviour that the 1.12 version got wrong is fixed rather than ported. Record the fix in a test and in
  `docs/porting-notes.md`.
- Every device change gets a GameTest under `src/main/java/mods/eln/gametest/cases`. The harness also runs a
  save/load round trip on every node a test builds.

## Specification (docs/)

- [docs/architecture.md](docs/architecture.md): entry points, package map, node kinds (six-node, transparent, voxel, grid, ghosts), descriptor/element/render, registries, lifecycle.
- [docs/simulation.md](docs/simulation.md): simulator tick order, element process lists, MNA solver, thermal network, failures, stability invariants and the tests that guard them.
- [docs/voxel-cables.md](docs/voxel-cables.md): voxel cables and pipes (VoxelNode, CableNet, routers, kits), the CableConverter six-node -> voxel upgrade, the benchmark.
- [docs/persistence.md](docs/persistence.md): ElnWorldSaves node files, NodeManager tags, save-tag upgrades, kept orphans, NBT keys, eln-ids.json, ghosts.
- [docs/networking-gui.md](docs/networking-gui.md): ElnNet payloads, publish/sync, GUI packets, menus and MenuSpecs, screens and JSON layouts.
- [docs/rendering.md](docs/rendering.md): the GL11 facade -> GlFrame -> GlSubmit pipeline, BERs, OBJ models, item rendering, sounds, HUDs.
- [docs/capabilities.md](docs/capabilities.md): energy, fluid and item capabilities, sides, burnt and client rules, invalidation.
- [docs/content.md](docs/content.md): items, ores and worldgen, oil, recipes and machine recipe lists, datagen, tags, config, tools/ and script/.
- [docs/testing.md](docs/testing.md): unit/property tests, the GameTest harness, golden data, the cable benchmark, how to add a test.
- [docs/cad.md](docs/cad.md): the checked model library (cad/): solids, emitters, the checker, textures, previews.
- [docs/porting-notes.md](docs/porting-notes.md): 26.x decisions, differences from 1.12, gameplay changes, deferred work.
