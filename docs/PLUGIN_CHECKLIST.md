# New or Edited Plugin Checklist

Copy this file for one plugin and replace every `<...>` field. Leave an unchecked box with a short explanation when a gate is not complete; do not silently remove inapplicable checks.

- Plugin name: `GeoBrot`
- Slug: `geobrot`
- Repository: `carmelosantana/geobrot` (existing repo — deviates from the `minecraft-<slug>` convention; see naming-chain note in §1)
- Owner: `Carmelo Santana`
- Target version: `0.2.0` (current `0.1.1`; reactivation + standards migration warrants a minor bump)
- Paper version: `26.1.2 build 74`
- Java version: `25`
- Updater destination: `geobrot.jar`
- External services: `none`
- Status: `experimental`
- Autonomy: `autonomous`

> **Autonomy note (recorded at gate 1, standing authorization):** The owner selected
> **autonomous** mode for this reactivation pipeline on 2026-08-30. Per
> `minecraft-plugin-ecosystem` §5, this choice **is** the GitHub push authorization for the
> entire pipeline, granted once here in writing: downstream skills (scaffold, dev, release,
> updater) do not stop for per-action approval. The pipeline still fails closed — a failed
> `mvn verify`, red/still-running CI, checksum mismatch, a plugin that will not enable, or an
> updater dry-run failure halts and reports, in autonomous mode exactly as in interactive.
> Harness command permissions are a separate layer and are **not** unlocked by this line.

## Standing decisions (owner-approved, locked at gate 1)

These three decisions were made by the owner during Wayfinder charting
(`.scratch/geobrot-terrain/map.md`, Notes → "Standing decisions locked during charting") and
are load-bearing constraints for every later gate. Two are recorded as ADRs under `docs/adr/`.

1. **Deliberate reactivation from the excluded list.** GeoBrot was on the ecosystem's
   permanently-excluded/abandoned set (alongside Agent Steve and Solar Power). The owner has
   deliberately reactivated it — an owner-only decision no skill makes on its own — so it
   re-enters the lifecycle gates starting here. → [ADR 0001](adr/0001-reactivation-from-excluded.md).
2. **Standards migration.** Adopt current ecosystem standards: **Java 25**, **Paper 26.1.2
   build 74**, **`api-version: '26.1'`** in `plugin.yml` (currently stale `1.21`). Keep the
   existing Maven group **`org.xpfarm`**. Migration is sequenced with the terrain tune
   (Milestone 3 / gate 4), not before diagnosis — the as-is `1.21` JAR already loads green on
   the current stack via backward-compat. It is required before any WorldCRUD code-merge.
3. **Relicense CC BY-NC 4.0 → AGPL-3.0-or-later.** Required for the active-plugin standard and
   for any future code-merge into AGPL-licensed WorldCRUD (a `-NC` work cannot be merged into
   AGPL). → [ADR 0002](adr/0002-relicense-to-agpl.md).

## 1. Scope

- [x] Status is explicitly recorded as active, experimental, or excluded. — **experimental**
      (see classification note below).
- [x] Purpose, commands, events, permissions, configuration, persistence, and acceptance checks are defined.
- [x] Known limitations and any intentionally withheld gates are recorded.

### Player-facing purpose

An operator/creative tool that generates explorable **floating-island worlds shaped like the
Mandelbrot fractal** and lets players teleport into them. On `play.xpfarm.org` this becomes an
**additional** creative-world option — never a replacement for the existing superflat creative
world. The revived terrain target is a high, gentle, buildable fractal surface sitting just
under client cloud height, with a geode underside and a trimmed stone base.

### Commands

Single root command `/mandel` (aliases `mandelbrot`, `fractal`), gated by `geobrot.use`
(default true). Subcommands (from `MandelCommand`):

| Subcommand | Args | Permission | Purpose |
|---|---|---|---|
| `help` | — | `geobrot.use` | Usage listing |
| `create` | `<name> [preset]` | `geobrot.create` (op) | Create a fractal world (preset from config `defaults.presets`) |
| `tp` / `teleport` | `<name>` | `geobrot.teleport` (true) | Teleport to a fractal world |
| `list` | — | `geobrot.list` (true) | List existing fractal worlds |
| `regen` / `regenerate` | `<name>` | `geobrot.regenerate` (op) | Regenerate a fractal world |
| `info` | `<name>` | `geobrot.use` | Show a world's fractal parameters |

### Events

- **Fires (via Bukkit, indirectly):** `WorldInitEvent` — triggered synchronously by
  `Bukkit.createWorld()` during world creation. The **root break** (diagnosis 03) is that
  `createWorld()`/`regen` currently run on an async thread, so `WorldInitEvent` is fired
  off-main and throws `IllegalStateException: WorldInitEvent may only be triggered
  synchronously`. Milestone 2 moves both to the main thread. GeoBrot registers **no
  `Listener`** of its own today.

### Permissions

`geobrot.use` (true), `geobrot.admin` (op), `geobrot.create` (op), `geobrot.teleport` (true),
`geobrot.list` (true), `geobrot.regenerate` (op) — as declared in `plugin.yml`. Note:
`geobrot.admin` is declared but not currently checked in `MandelCommand`; reconcile during dev
(either wire it or drop it, and assert the outcome in `PluginDescriptorTest`).

### Configuration

`config.yml` keys: `generation.*` (default-world-size, base-height, max-thickness, default-zoom,
min-escape-time), `materials.{deep,medium-deep,medium,shallow}` (per-tier core/middle/surface
block palette), `performance.*` (async-generation, max-concurrent-operations, generation-timeout),
`defaults.presets.{classic,spiral,seahorse,elephant}` (center-x/center-y/zoom), `worlds.*`
(auto-load, max-worlds, backup.*), `permissions.*` (allow-creation/teleport/list),
`debug.*` (enabled, log-calculations, log-timing).

The terrain-model revival adds config-driven **height** keys (solid floor Y, surface target Y,
relief amplitude) per the locked model (`.scratch/geobrot-terrain/issues/05-terrain-model-spec.md`) —
so heights come from config, not the hardcoded `(maxHeight-seaLevel)/2` formula. **Config
defect to fix in dev:** `ConfigValidator` checks a nonexistent `materials.palette` key (source
of the benign "Material palette not found" WARN); it must validate the real
`materials.{deep,medium-deep,medium,shallow}` structure instead.

### Persistence

Fractal worlds are persisted as **standard Bukkit world folders** on disk (created via
`Bukkit.createWorld`), plus per-world fractal parameters tracked by `FractalWorldManager`. No
database. No PDC. Backups are file-copy based (`worlds.backup.*`).

### Dependencies

- **Hard:** none beyond the Paper API.
- **Soft:** none.
- **Load order:** none required. (WorldCRUD integration is a *later, fogged* milestone and is
  **not** a dependency of this plugin — see Out of scope / Known limitations.)

### External integrations

`none`. GeoBrot makes no Ollama, Umami, or other outbound calls. (Gate 5's external-service
contract therefore applies vacuously — nothing to bound or disable.)

### Acceptance checks (basis for gate 6 unit tests and gate 7a runtime verification)

1. `/mandel create <name>` creates a world **on the main thread** with no
   `WorldInitEvent may only be triggered synchronously` exception, and a world folder is
   written to disk (today: crashes async, no folder — diagnosis 03).
2. `/mandel list` then reports the created world (today: "No fractal worlds found").
3. `/mandel regen <name>` regenerates on the main thread without the same async crash.
4. Generated terrain is a **coherent, centered Mandelbrot** (via `FractalMath.worldToFractal`,
   center+zoom on spawn) — not the current lopsided sliver.
5. Vertical model matches the locked spec: solid floor **Y135**, in-set surface **Y165**,
   `surfaceY = 153 + round(12·escapeTime/MAX_ITERATIONS)`, thickness ~18–30; material stack
   grass→dirt(~3)→geode-tier fill (by escape-time depth)→bedrock cap (Y135–136). No
   bedrock→surface plinth.
6. Geode palette + presets from `config.yml` are actually wired (today: unused/hardcoded).
7. `ConfigValidator` validates the real `materials.*` tiers; the "Material palette not found"
   WARN is gone.
8. Plugin loads **green** (enabled) on the Legendary stack with `api-version: '26.1'` after
   standards migration; `/mandel` registers; spawn is safe (solid column + 2 air).

### Known limitations / intentionally withheld gates

- **Status = experimental, and why.** The terrain generator currently produces **no terrain at
  all** (root cause confirmed in diagnosis 03: async `createWorld` → swallowed exception →
  null world). GeoBrot runs the pipeline through plan (1) → scaffold-metadata (2, 3) →
  standards migration + terrain fix/tune (4–6) → single-plugin runtime verify (7a), but the
  following gates are **intentionally withheld** until the terrain is fixed (Milestone 2) and
  tuned + runtime-proven (Milestone 3):
  - **Gate 9 (release)** — withheld: no proven terrain to release yet.
  - **Gate 10 (updater enrollment)** — withheld: not updater-managed until it releases.
  - **Gate 7b (full-roster matrix)** — withheld / out-of-band: not updater-managed yet, and
    not a per-cycle gate.
- **WorldCRUD integration is out of scope for this reactivation and is a later, fogged
  milestone.** Path is settled (recon 04: port `MANDELBROT` into WorldCRUD's `WorldGenerators`
  registry — three in-tree edits), but it stays deferred until (a) GeoBrot's terrain is proven
  and (b) the AGPL relicense (ADR 0002) is in place. It is **not** a dependency of this plugin.
- **Naming-chain deviation (documented).** The repository is `carmelosantana/geobrot`, not the
  convention's `carmelosantana/minecraft-geobrot`, because it is a pre-existing revived repo
  with a baseline commit and public history, not a fresh scaffold. Rest of the chain is
  consistent: slug `geobrot` = artifactId `geobrot` → shaded JAR `geobrot-<version>.jar` →
  updater destination `geobrot.jar` → `plugin.yml` name `GeoBrot`. Renaming the GitHub repo is
  out of scope for this milestone; if the owner wants the `minecraft-` prefix, that is a
  separate one-line decision recorded here rather than assumed.
- **Stale website metadata.** `plugin.yml` currently declares `website: https://hv2.world` (a
  pre-xpfarm relic). It must become `https://xpfarm.org` at the scaffold/metadata gate (§3).
  Recorded here so it is not missed; not changed at gate 1 (planning only).
- **Additional fractal types** (Julia, Burning Ship, Tricorn, Buddhabrot) exist in
  `FractalMath` but exposing them is feature expansion beyond fix+tune — out of scope.
- **`Biome.PLAINS`** must be re-verified against the Paper 26.1.2 biome registry post-fix
  (diagnosis 03: not a load blocker, but unreachable until the async crash is fixed).

## 2. Repository

_Repository items verified during Milestone 2 (standards migration folded scaffold-overlapping
metadata into this run)._

- [x] Repository is `carmelosantana/minecraft-<slug>` with an SSH `origin` and `main` branch. —
      Repo is `carmelosantana/geobrot` (documented deviation, §1). SSH `origin`
      (`git@github.com:carmelosantana/minecraft-geobrot.git`) and `main` exist; M2 work is on
      branch `claude/interesting-sanderson-099090` (worktree), base commit `74097bc`.
- [x] Existing user-owned worktree changes were identified and preserved. — Worktree clean at
      M2 start; only geobrot-owned files touched.
- [x] No `herobrinesystems` references remain in source, metadata, workflows, remotes, or
      documentation. — Verified: `grep -rniE herobrinesystems` over shipped source/metadata/docs
      returns nothing. The stale `hv2.world` website was fixed to `https://xpfarm.org` (§3).

## 3. Metadata

_Relicense (ADR 0002) and the `website` fix completed during Milestone 2 (folded into the
standards-migration run per the M2 task order)._

- [x] AGPL-3.0-or-later `LICENSE` and Maven license metadata are present and consistent. —
      `LICENSE` (full AGPL-3.0 text) created; pom `<licenses>` block added; README badge +
      License section rewritten to AGPL-3.0-or-later. No CC BY-NC wording remains anywhere in
      shipped files (commit `a0ffcd2`).
- [x] `https://xpfarm.org` metadata and Carmelo Santana author metadata are present. — Author
      present; website fixed `https://hv2.world` → `https://xpfarm.org` in pom.xml, plugin.yml,
      README (commit `a0ffcd2`); stale doc prose swept (commit `d5e71eb`).
- [x] `play.xpfarm.org` is recorded as the public Minecraft server hostname where server
      identity is documented. — README live-server links updated `play.hv2.world` → `play.xpfarm.org`.
- [x] New work uses the `org.xpfarm` Maven group, or an existing-coordinate compatibility decision is documented. —
      Group kept `org.xpfarm.geobrot` per standing decision 2 (documented existing-coordinate decision).
- [x] Repository slug, artifact, releasable JAR, updater destination, and `plugin.yml` names are consistent. —
      slug `geobrot` = artifactId `geobrot` → `geobrot-0.2.0.jar` → destination `geobrot.jar` →
      name `GeoBrot`; consistent apart from the documented repo-name deviation (§1).
- [x] No secrets committed in source, defaults, tests, logs, history, or documentation. — GeoBrot
      has no external services/credentials; no secrets present.

## 4. Compatibility

_Gate 4 (`minecraft-plugin-dev`) — COMPLETE (Milestone 2). Standing decision 2 (standards
migration) executed: the `1.21` → `'26.1'` change is a compatibility (bytecode) change, so
gate 6 + gate 7a were re-run below._

- [x] Java 25/Paper 26.1.2 build 74 compile succeeds and `plugin.yml` uses `api-version: '26.1'`. —
      `pom.xml`: `maven.compiler.release=25`, paper-api `26.1.2.build.74-stable`; `plugin.yml`:
      `api-version: '26.1'` (quoted String, guarded by `PluginDescriptorTest`). `mvn clean verify`
      BUILD SUCCESS on Java 25 (commit `a0ffcd2`).
- [x] Hard dependencies, soft dependencies, optional APIs, and load ordering were reviewed and declared. —
      None: GeoBrot depends only on the Paper API (provided). No `depend`/`softdepend`/`loadbefore`
      needed; `plugin.yml` declares none.
- [x] Geyser/Floodgate/ViaVersion review covers Bedrock-safe input, UI, inventory, identity, and protocol behavior. —
      `/mandel` is chat-command only (no forms, no inventory UI, no item interaction, no
      client-specific packets); no Bedrock-unsafe surface. Verified at gate 7a: GeoBrot ran green
      alongside Geyser-Spigot, floodgate, and ViaVersion together on Paper 26.1.2 (protocol 775).

## 5. External services

_Gate 5 (`minecraft-plugin-dev`) — COMPLETE (vacuous). GeoBrot makes no outbound calls (§1)._

- [x] External integrations are disabled by default or require explicit configuration and have bounded timeouts. —
      N/A: no external integrations (no Ollama/Umami/HTTP clients in the codebase).
- [x] Ollama/Umami-style external endpoints are optional and failure-tolerant when applicable. — N/A.
- [x] Endpoint failure cannot fail server/plugin startup, and diagnostics redact secrets. — N/A: no endpoints, no secrets.

## 6. Tests and build

_Gate 6 (`minecraft-plugin-dev`) — COMPLETE (Milestone 2)._

- [x] Unit tests cover separable logic, configuration, serialization, permissions, and failure paths where applicable. —
      `MandelbrotGeneratorTest` rewritten with 4 real assertions pinning the centered
      `worldToFractal` mapping (origin→center, symmetry, zoom-framing, in-set/out-of-set);
      `FractalMathTest`, `ConfigValidatorTest`, `GeoBrotPluginTest` retained. 29 tests total.
- [x] `PluginDescriptorTest` parses `plugin.yml`/`config.yml` and asserts name, main, `String`
      `api-version`, substituted version, every command and permission the code uses. —
      Created (commit `a0ffcd2`): asserts name `GeoBrot`, main class, `api-version` is a String
      `'26.1'`, resolved `${project.version}`, the `mandel` command, and permissions
      `geobrot.{use,create,teleport,list,regenerate}`. `geobrot.admin` reconciled by **dropping**
      it (unused; not checked by any `hasPermission`) — not asserted.
- [x] `mvn --batch-mode --no-transfer-progress clean verify` succeeds. — BUILD SUCCESS, 29/29
      tests, Java 25 / Maven 3.9.16.
- [x] The shaded releasable JAR and embedded `plugin.yml` were inspected; `original-*` JARs are excluded. —
      `target/geobrot-0.2.0.jar` inspected: embedded `plugin.yml` shows version `0.2.0`,
      `api-version '26.1'`, `website https://xpfarm.org`, `mandel` command, five permissions,
      main class present; JAR contains only `org.xpfarm.geobrot.*` classes (no Paper/Bukkit/Kyori
      API leak — nothing to shade, all deps provided/test scope).

## 7. Matrix

_Gate 7a (single-plugin runtime verify, `minecraft-plugin-dev`) — COMPLETE (Milestone 2), on a
disposable Legendary stack (Paper 26.1.2, protocol 775) via `scripts/test-stack.sh`. Gate 7b
(full-roster matrix) remains **withheld / out-of-band** (§1)._

- [ ] Fresh-volume Legendary stack test covers every updater-managed plugin. — **7b withheld:**
      GeoBrot is not updater-managed yet; out-of-band, not required for this reactivation.
- [ ] Each updater-managed plugin's manifest state and fresh-volume behavior are recorded separately. — 7b, withheld.
- [x] Paper, Geyser, Floodgate, and ViaVersion start successfully together. — 7a: RCON `plugins`
      showed `GeoBrot`, `Geyser-Spigot`, `floodgate`, `ViaVersion` all green together; Java port
      served a real Minecraft handshake (Paper 26.1.2, protocol 775).
- [x] Affected commands, permissions, persistence, and configuration reload were exercised over RCON with no server-wide hot reload. —
      7a: `/mandel create m2test` → **"Successfully created"** with **no** `WorldInitEvent`/
      `IllegalStateException` in logs (the root fix); a world folder with 4 populated region files
      (~500–600KB, around the origin) was written to disk → real terrain generated. `/mandel list`
      → "m2test - Loaded"; `/mandel info` read back params; `/mandel regen m2test` →
      "Successfully regenerated" with no async crash. Persistence = standard Bukkit world folders
      (confirmed on disk). No server-wide hot reload used.
- [x] Ollama and Umami unavailable-endpoint tests keep the server and plugins available when applicable. — N/A: no external endpoints.

## 8. CI/CD

_Gates 8a (workflow install, scaffold) / 8b (verify main CI, release)._

- [ ] Identical standard plugin Actions workflow is installed with the required triggers, Temurin 25 build, artifact, checksum, and release behavior. —
      No `.github/workflows/` present yet; scaffold installs it.
- [ ] Successful main Actions run is recorded before tagging.
- [ ] Workflow permissions contain no broader access than the documented contract.

## 9. Release

_Gate 9 (`minecraft-plugin-release`). **Withheld** until terrain proven (§1)._

- [ ] Semantic version matches the POM, plugin metadata, and `v<version>` tag.
- [ ] Successful tag Actions run and GitHub release are recorded.
- [ ] Release contains exactly one updater-matching JAR plus `SHA256SUMS.txt` and no `original-*` JAR.
- [ ] Downloaded release assets pass `sha256sum --check SHA256SUMS.txt`.

## 10. Updater

_Gate 10 (`minecraft-plugin-updater`). **Withheld** until released (§1)._

- [ ] Updater manifest/tests cover repository, destination, anchored asset regex, legacy globs, enabled state, and optional pin.
- [ ] Fresh install, upgrade, no-op, legacy archival, endpoint failure, and checksum failure behaviors pass.
- [ ] Updater dry-run uses a disposable directory and never a production plugin directory.
- [ ] Failure retains the installed JAR and default fail-open behavior permits Minecraft startup.

## 11. Deployment

Not a gate. Deployment is updater pickup: a verified release plus a correct manifest entry is all
this lifecycle owes. Leaving this section entirely unticked is the normal resting state and blocks
nothing — not release, not enrolment, not handoff.

- [ ] Enrolment confirmed live and correct: release sound, manifest entry on `origin/main`, gate 10 genuinely completed.
- [ ] Deployment evidence recorded, if and only if an operator relayed some. Otherwise note "enrolled, not known to be deployed" and leave unticked.

## 12. Handoff

_Gate 12 (`minecraft-plugin-handoff`). Not this milestone._

- [ ] Current-state documentation refreshed with release, CI, updater, deployment, and local pending state.
- [ ] Known limitations, skipped checks, configuration or migration notes, rollback guidance, and follow-up owner are recorded.
- [ ] Evidence distinguishes source commit, published tag/release, updater state, and deployed state without exposing secrets.
- [ ] Client play-test obligation recorded with a named owner and a target date: `<owner>` / `<date>`.
- [ ] Client play-test outcome recorded once performed, covering Java join, Bedrock join, and any form, inventory, or rendered item behavior this plugin introduces.
- [ ] Public deployment reachability confirmed during that pass: `play.xpfarm.org` reaches the intended Java and Bedrock entry points.

---

## Gate log

- **Gate 1 (plan) — COMPLETE (2026-08-30).** `minecraft-plugin-plan` run as the lifecycle entry
  for the deliberate reactivation. Scope captured from the existing source plus the Wayfinder
  map (`.scratch/geobrot-terrain/`, local-only). Status classified **experimental** with gates
  7b/9/10 intentionally withheld. Autonomy recorded **autonomous** (standing push
  authorization). Standing decisions 1–3 recorded; ADRs 0001 and 0002 written. Next step:
  Milestone 2 (terrain break fix) and Milestone 3 (standards migration + tune) via
  `superpowers:subagent-driven-development` + `minecraft-plugin-dev`; metadata/relicense land at
  the scaffold gate (§3).

- **Milestone 2 (standards migration + root-cause fix) — COMPLETE (2026-08-30).** Executed via
  `superpowers:subagent-driven-development` under `minecraft-plugin-dev` (three implementer tasks,
  each task-reviewed; one final whole-branch review on the most capable model). Note: the M2 task
  order folded the standards migration **and** the AGPL relicense into this milestone (the gate-1
  plan had tentatively split migration into M3); the M2 scope as delivered is authoritative.
  Commits `74097bc..6e9e6f1` on branch `claude/interesting-sanderson-099090`:
  - **Standards migration:** Java 25 (`maven.compiler.release=25`), paper-api `26.1.2.build.74-stable`,
    `plugin.yml` `api-version: '26.1'` (quoted), version `0.2.0`, group kept `org.xpfarm.geobrot`
    (standing decision 2). `PluginDescriptorTest` added.
  - **Relicense:** CC BY-NC 4.0 → AGPL-3.0-or-later (`LICENSE`, pom `<licenses>`, README); website
    `hv2.world` → `xpfarm.org`; stale Java-21/Paper-1.21.6 doc prose swept.
  - **Root fix:** `createFractalWorld`/`regenerateFractalWorld` moved to the main (command) thread
    — the `WorldInitEvent may only be triggered synchronously` crash is gone (runtime-confirmed).
  - **Mapping fix:** adopted `FractalMath.worldToFractal` (centered on world origin) in
    `MandelbrotGenerator.generateNoise`, replacing the naive `worldX/(zoom*100)` sliver mapping.
  - **Biome:** `Biome.PLAINS` verified — compiles against 26.1.2 and the world generated at 7a;
    left unchanged (registry form not needed).
  - **Gate 6 + 7a evidence:** `mvn clean verify` green (29 tests); shaded JAR inspected; on a live
    Legendary Paper 26.1.2 stack `/mandel create` produced a world with populated region files and
    NO async crash, alongside a green Geyser/Floodgate/ViaVersion.

  **Exit note — behaviors gate 7a could NOT reach (carry to gate 12 / play-test):**
  - **The rendered terrain shape.** 7a is headless (no client joins), so the *visual* "recognizable,
    centered Mandelbrot" was proven only at the code/arithmetic level (unit tests on the mapping) and
    by the on-disk region files existing around the origin — **not** by a human seeing the fractal.
    A real Java/Bedrock client walk-through is needed to confirm the shape reads as intended.
  - **Spawn/teleport UX.** `/mandel tp` teleporting a real player onto safe ground was not exercised
    (no player in a headless stack); the spawn-finder logic is unchanged from before and untested live.
  - GeoBrot introduces **no** forms, inventory UI, or custom item behavior, so there is no
    Bedrock-form/inventory rendering obligation beyond the above.

  **Explicitly deferred to Milestone 3** (NOT done here, by design): the final vertical terrain model
  (floor Y135 / surface Y165 / escape-time relief), the geode material palette + config presets
  wiring, and the `ConfigValidator` `materials.palette` key fix (the benign "Material palette not
  found" WARN still logs). Minor items also deferred: dead `plugin` field in `MandelCommand`,
  a duplicated test constant, and the `ConfigValidator` `permissions.admin` default that still names
  the now-removed `geobrot.admin` (fold into the M3 config cleanup).

  **Still outstanding (not M2 scope):** no `.github/workflows/` CI workflow is installed yet (§8a,
  scaffold item) — release (gate 9) stays withheld until Milestone 3, so this blocks nothing now.
  Next step: **Milestone 3** (terrain tune) via the same skill pair.
