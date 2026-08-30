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

_Gate 2 (`minecraft-plugin-scaffold`). Not this milestone. Notes for that gate:_

- [ ] Repository is `carmelosantana/minecraft-<slug>` with an SSH `origin` and `main` branch. —
      Repo is `carmelosantana/geobrot` (documented deviation, §1). SSH `origin` and `main`
      exist (baseline commit `1a4b33a`); reactivation work is on branch
      `claude/blissful-curie-fb3c31`.
- [ ] Existing user-owned worktree changes were identified and preserved. — Worktree clean at
      gate-1 start.
- [ ] No `herobrinesystems` references remain in source, metadata, workflows, remotes, or
      documentation. — To be verified at scaffold; note the stale `hv2.world` website (§1) as a
      separate metadata fix.

## 3. Metadata

_Gate 3 (`minecraft-plugin-scaffold`). Not this milestone. Carries the relicense (ADR 0002)
and the `website` fix (§1)._

- [ ] AGPL-3.0-or-later `LICENSE` and Maven license metadata are present and consistent. —
      **Currently CC BY-NC 4.0 → relicense to AGPL-3.0-or-later per ADR 0002.** No `LICENSE`
      file present yet.
- [ ] `https://xpfarm.org` metadata and Carmelo Santana author metadata are present. — Author
      present; **website is stale `https://hv2.world` → fix to `https://xpfarm.org`.**
- [ ] `play.xpfarm.org` is recorded as the public Minecraft server hostname where server identity is documented.
- [ ] New work uses the `org.xpfarm` Maven group, or an existing-coordinate compatibility decision is documented. —
      Group already `org.xpfarm.geobrot`. ✓ (kept per standing decision 2).
- [ ] Repository slug, artifact, releasable JAR, updater destination, and `plugin.yml` names are consistent. —
      Chain recorded in §1; consistent apart from the documented repo-name deviation.
- [ ] No secrets committed in source, defaults, tests, logs, history, or documentation.

## 4. Compatibility

_Gate 4 (`minecraft-plugin-dev`, Milestone 3). Carries standing decision 2 (standards
migration) — the `1.21` → `'26.1'` `api-version` change is a compatibility change (bytecode),
not just metadata, so it needs gate 6 + gate 7a re-run._

- [ ] Java 25/Paper 26.1.2 build 74 compile succeeds and `plugin.yml` uses `api-version: '26.1'`. —
      Currently `api-version: 1.21` (loads green via backward-compat; migrate here).
- [ ] Hard dependencies, soft dependencies, optional APIs, and load ordering were reviewed and declared. —
      None expected (§1).
- [ ] Geyser/Floodgate/ViaVersion review covers Bedrock-safe input, UI, inventory, identity, and protocol behavior. —
      `/mandel` is chat-command only, no forms/inventory UI; low cross-play surface. Confirm at dev.

## 5. External services

_Gate 5 (`minecraft-plugin-dev`). Vacuous — GeoBrot has no external integrations (§1)._

- [ ] External integrations are disabled by default or require explicit configuration and have bounded timeouts. —
      N/A: no external integrations.
- [ ] Ollama/Umami-style external endpoints are optional and failure-tolerant when applicable. — N/A.
- [ ] Endpoint failure cannot fail server/plugin startup, and diagnostics redact secrets. — N/A.

## 6. Tests and build

_Gate 6 (`minecraft-plugin-dev`, Milestone 3)._

- [ ] Unit tests cover separable logic, configuration, serialization, permissions, and failure paths where applicable. —
      Existing tests: `FractalMathTest`, `MandelbrotGeneratorTest`, `ConfigValidatorTest`,
      `GeoBrotPluginTest`. Extend for the fixed model.
- [ ] `PluginDescriptorTest` parses `plugin.yml`/`config.yml` and asserts name, main, `String`
      `api-version`, substituted version, every command and permission the code uses. —
      Verify/create at dev; must assert the `geobrot.admin` reconciliation (§1).
- [ ] `mvn --batch-mode --no-transfer-progress clean verify` succeeds.
- [ ] The shaded releasable JAR and embedded `plugin.yml` were inspected; `original-*` JARs are excluded.

## 7. Matrix

_Gate 7a (single-plugin runtime verify, `minecraft-plugin-dev`, Milestone 3). Gate 7b withheld
(§1)._

- [ ] Fresh-volume Legendary stack test covers every updater-managed plugin. — **7b withheld:**
      GeoBrot is not updater-managed yet; out-of-band, not required for this reactivation.
- [ ] Each updater-managed plugin's manifest state and fresh-volume behavior are recorded separately. — 7b, withheld.
- [ ] Paper, Geyser, Floodgate, and ViaVersion start successfully together. — Verify at 7a
      (GeoBrot green alongside the cross-play stack).
- [ ] Affected commands, permissions, persistence, and configuration reload were exercised over RCON with no server-wide hot reload. — 7a.
- [ ] Ollama and Umami unavailable-endpoint tests keep the server and plugins available when applicable. — N/A.

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
