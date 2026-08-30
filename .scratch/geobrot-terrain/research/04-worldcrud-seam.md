# WorldCRUD generator seam — recon findings

Research for ticket [04-worldcrud-seam-recon](../issues/04-worldcrud-seam-recon.md).
Primary source: `Plugins/worldcrud` @ v1.3.0 (read-only). Every claim below is a `file:line`
pointer into that tree. No files were modified.

## TL;DR

WorldCRUD's generator seam is a **three-point, in-tree registry** — there is **no dynamic
SPI and no generator-by-name hook**. To add a `MANDELBROT` creative generator you edit three
things in WorldCRUD's own source tree:

1. the `WorldTypes` enum (the user-facing type token),
2. the `WorldGenerators` registry (`idFor` + `attach` switches — the persisted-id ↔ concrete-generator seam),
3. a concrete `ChunkGenerator` subclass under `generators/`.

`getDefaultWorldGenerator(...)` is **not** overridden and `plugin.yml` has **no `generator:`
entry**, so WorldCRUD cannot today resolve a `ChunkGenerator` supplied by another plugin.
That single fact is the hard constraint that decides port-vs-separate — see §5.

## 1. The `WorldGenerators` seam + registry (how FLAT and SKYBLOCK_CLASSIC are wired)

The registry is one small final class, explicitly documented as the "single source of truth
for the persisted generator ids":

- `generators/WorldGenerators.java:25` — `public final class WorldGenerators`
- `generators/WorldGenerators.java:28` — `public static final String SKYBLOCK_CLASSIC = "SKYBLOCK_CLASSIC";`
- `generators/WorldGenerators.java:31` — `public static final String FLAT = "FLAT";`
- `generators/WorldGenerators.java:40-49` — `idFor(WorldTypes)`: pure `switch` mapping the
  enum → persisted String id (`SKYBLOCK_CLASSIC`, `FLAT`, or `null` for `NORMAL`).
- `generators/WorldGenerators.java:56-73` — `attach(WorldCreator, String generatorId)`: a
  hardcoded `switch (generatorId)` that news up the concrete generator and disables vanilla
  structures:
  - `SKYBLOCK_CLASSIC` → `creator.generator(new ClassicSkyblockGenerator()); creator.generateStructures(false);` (lines 61-64)
  - `FLAT` → `creator.generator(new FlatChunkGenerator()); creator.generateStructures(false);` (lines 65-68)
  - `default` → no-op, "leave vanilla terrain" (lines 69-71)

The enum that feeds `idFor`:

- `WorldTypes.java:5-8` — `enum WorldTypes { NORMAL, SKYBLOCK_CLASSIC, FLAT; }`
- `WorldTypes.java:18-28` — `parse(String)` → `Optional<WorldTypes>` (case-insensitive, empty on unknown).
- `WorldTypes.java:30-37` — `getValidTypes()` (drives the command's help/validation text).

The concrete generators are plain Bukkit `ChunkGenerator` subclasses in-tree:

- `generators/FlatChunkGenerator.java:33` — `public final class FlatChunkGenerator extends ChunkGenerator`
  (overrides `generateNoise`, `shouldGenerateNoise/Surface/Bedrock/Caves/Decorations/Structures`,
  `getFixedSpawnLocation` — see lines 46-103).
- `generators/SkyblockGenerator.java:21` — `public abstract class SkyblockGenerator extends ChunkGenerator`
- `generators/ClassicSkyblockGenerator.java:14` — `public class ClassicSkyblockGenerator extends SkyblockGenerator`

### How the id round-trips (why the seam is registry-shaped, not just a factory)

The persisted **String** id is the contract; it must survive restarts and reattach the same
generator on every path:

- `WorldEntry.java:36` — `private final String generator; // null for NORMAL, "SKYBLOCK_CLASSIC" ...`
- `WorldEntry.java:107` / `:127` — serialized to / read from the registry YAML map (`"generator"` key).
- Create path: `WorldManager.java:133-134` — `String generatorId = WorldGenerators.idFor(worldType); WorldGenerators.attach(creator, generatorId);`
- Auto-load path: `WorldRegistry.java:282-286` — `loadWorld` rebuilds a `WorldCreator` and calls
  `WorldGenerators.attach(creator, entry.getGenerator())`.
- Reset path: `WorldManager.java:420-423` — same `attach` on recreate.
- Marker fallback: `WorldManager.java:164` — writes a `WorldMarker(worldType, generatorId)` into
  the world folder so an orphaned world can be re-detected.

The registry doc comment itself explains the seam's reason for existing: "on auto-load, create,
reload and reset the same id must reattach the same generator, or a skyblock/flat world silently
degrades to NORMAL after a restart" (`WorldGenerators.java:18-24`).

## 2. The extension point a `MANDELBROT` generator plugs into

There is no interface/SPI to implement and no dynamic registration call. The "extension point"
is **three coordinated edits inside WorldCRUD**:

1. `WorldTypes.java:5-8` — add `MANDELBROT` to the enum (auto-flows into `getValidTypes()` and
   the `/worldcrud create` validation).
2. `WorldGenerators.java` — add a `public static final String MANDELBROT = "MANDELBROT";`
   constant, a `case MANDELBROT -> MANDELBROT;` arm in `idFor` (lines 44-48), and a
   `case MANDELBROT -> { creator.generator(new MandelbrotChunkGenerator()); creator.generateStructures(false); }`
   arm in `attach` (lines 60-72).
3. `generators/MandelbrotChunkGenerator.java` — a new `extends ChunkGenerator` class (the actual
   fractal terrain), matching the `FlatChunkGenerator` shape.

No other file needs touching: persistence, auto-load, reset, marker, and the create command all
route through `idFor`/`attach` already.

## 3. How a creative-world type is selected/created — and the plugin-supplied-generator question

**User-facing creation** is the `/worldcrud create <name> [type] [size]` command:

- `WorldCRUDCommand.java:122-180` — `handleCreateCommand`: absent type → `NORMAL`; unrecognized
  → reject loudly; valid → `WorldTypes.parse` then `worldManager.createWorld(worldName, worldType, borderSize)`.
- Valid types are enumerated from `WorldTypes.getValidTypes()` in the help text (`:128`, `:145`).

**The shared "creative" world** is a distinct, config-driven feature, not a per-user type pick:

- `CreativeWorldService.java:81-110` — `ensureWorld()` creates one shared superflat world on enable,
  **hardcoded to `WorldTypes.FLAT`** (`CreativeWorldService.java:93` — `worldManager.createWorld(name, WorldTypes.FLAT)`).
- Its config surface is only `enabled` / `name` / `icon` (`CreativeWorldService.java:150-163`), read
  live from the `creative-world` config section. There is no config key to swap the creative
  generator — it is a literal `WorldTypes.FLAT` in code.

**Can a generator come from another plugin (named `ChunkGenerator`)? — No, not today.**

- `getDefaultWorldGenerator(...)` is **not overridden anywhere** in `src/main` (grep: 0 hits).
- `plugin.yml` has **no `generator:` key** (only `main/name/version/api-version/softdepend/permissions/commands`).
- `attach()` (`WorldGenerators.java:56-73`) resolves generators by `new`-ing **concrete classes from
  WorldCRUD's own package**; there is no branch that calls `WorldCreator.generator(String, worldName)`
  or looks up another plugin's generator by name.

So the Bukkit "generator-by-name" seam (`WorldCreator.generator(String)` →
`JavaPlugin.getDefaultWorldGenerator(worldName, id)`) is **unused**. A generator currently **must
live in WorldCRUD's own tree** to be reachable by the `attach` switch. Supporting an external
named generator would itself require a new code branch in WorldCRUD (see §5, path B).

## 4. License + Java/Paper/api-version/Maven conventions

- **License: AGPL-3.0-or-later — confirmed.**
  - `pom.xml:12-18` — `<name>GNU Affero General Public License v3.0 or later</name>`, url agpl-3.0.
  - `LICENSE` — "GNU AFFERO GENERAL PUBLIC LICENSE Version 3, 19 November 2007".
  - Per-file headers, e.g. `WorldGenerators.java:1-9`, `CreativeWorldService.java:1-9`:
    "...either version 3 of the License, or (at your option) any later version."
- **Java:** `maven.compiler.release = 25` (`pom.xml:25`).
- **Paper API:** `io.papermc.paper:paper-api:26.1.2.build.74-stable`, `provided` scope (`pom.xml:57-62`).
- **api-version:** `'26.1'` (`plugin.yml:4`).
- **Maven group:** `org.xpfarm`, artifact `worldcrud`, version `1.3.0` (`pom.xml:7-9`); package
  root `org.xpfarm.worldcrud`.
- **Packaging:** shaded jar via `maven-shade-plugin` (`pom.xml:99-114`); Floodgate/Cumulus are
  `provided` soft deps, never shaded (`pom.xml:64-82`, `plugin.yml:14` `softdepend: [floodgate]`).

These exactly match geobrot's locked standards-migration targets (map.md:16), so a port introduces
no version drift.

## 5. Integration assessment — (A) port into WorldCRUD vs (B) separate plugin, named generator

### The hard constraint

WorldCRUD has **no generator-by-name / SPI seam**. `attach()` is a closed `switch` that
instantiates concrete classes from its own package (`WorldGenerators.java:56-73`); `getDefaultWorldGenerator`
is not overridden and `plugin.yml` declares no `generator:`. Therefore **both** paths require a
code change *inside WorldCRUD* — path B does not avoid touching WorldCRUD, it only changes *what*
the touch is. This neutralizes the usual "keep it decoupled, zero WorldCRUD edits" argument for B.

### Path A — port the Mandelbrot `ChunkGenerator` into WorldCRUD's registry

Add `MANDELBROT` to `WorldTypes`, add the constant + `idFor`/`attach` arms in `WorldGenerators`,
and drop a `MandelbrotChunkGenerator extends ChunkGenerator` into `generators/`.

- **Pros:** matches the existing pattern exactly (mirrors FLAT/SKYBLOCK_CLASSIC one-for-one);
  free persistence + reattach on create/auto-load/reset/marker via the existing id round-trip
  (§1); works with the existing `/worldcrud create` command and the `creative-world` feature
  (a one-line swap of `WorldTypes.FLAT` → `WorldTypes.MANDELBROT` at `CreativeWorldService.java:93`
  would even make it the shared creative world, though the map keeps geobrot *additional*, so a
  new type token is the right scope); single JAR, no load-order/soft-dep fragility; identical
  Java/Paper/group conventions (§4).
- **Cons:** geobrot's generator source physically lives in WorldCRUD's repo (loses geobrot as an
  independently releasable artifact); requires the AGPL relicense of geobrot's generator code —
  **already a locked decision** (map.md:17), so this is not a blocker; couples geobrot's release
  cadence to WorldCRUD's.

### Path B — keep geobrot a separate plugin exposing a named `ChunkGenerator`

geobrot overrides `getDefaultWorldGenerator(worldName, id)` to return its Mandelbrot generator and
declares itself in its own `plugin.yml`; WorldCRUD references it by name.

- **Pros:** geobrot stays an independently versioned/releasable plugin; the fractal code stays in
  geobrot's tree; in principle geobrot could keep a permissive/own license (but the merge-driver for
  AGPL is moot if code never merges).
- **Cons / blockers:** WorldCRUD has **no** branch that resolves an external named generator, so B
  **still requires a new WorldCRUD code path** — an `attach()` arm that calls
  `WorldCreator.generator("geobrot", worldName)` (or resolves via `Bukkit.getPluginManager()`),
  plus a persisted-id convention for it. It adds a **hard/soft cross-plugin dependency and load-order
  constraint** (geobrot must be enabled before WorldCRUD creates/loads such a world, or the world
  silently degrades to NORMAL — exactly the failure the registry doc warns about,
  `WorldGenerators.java:18-24`). More moving parts (two JARs, two release trains, a depend
  declaration) for no structural payoff, since the seam it would target does not exist yet and must
  be built anyway.

### Recommendation: **Path A (port into WorldCRUD's `WorldGenerators` registry).**

One-line why: WorldCRUD has no generator-by-name seam to plug into, so **B doesn't avoid editing
WorldCRUD** — it just adds cross-plugin load-order fragility on top of the same edit; A matches the
existing FLAT/SKYBLOCK_CLASSIC pattern exactly and the only historical blocker (license) is already
resolved by the locked CC BY-NC → AGPL relicense.

Caveat for the fog patch: A is contingent on the terrain being proven first (map.md:6,39) and on
geobrot's generator being clean-room portable under AGPL; if geobrot must remain a standalone
deployable for non-WorldCRUD servers, revisit B — but B's cost is building the missing named-generator
seam in WorldCRUD, not merely "referencing" one.

## Evidence index (file:line)

| Claim | Pointer |
|---|---|
| Registry class | `generators/WorldGenerators.java:25` |
| FLAT / SKYBLOCK ids | `generators/WorldGenerators.java:28,31` |
| `idFor` switch | `generators/WorldGenerators.java:40-49` |
| `attach` switch (news up concrete generators) | `generators/WorldGenerators.java:56-73` |
| Seam rationale doc | `generators/WorldGenerators.java:18-24` |
| Enum of types | `WorldTypes.java:5-8` |
| Concrete generators extend `ChunkGenerator` | `generators/FlatChunkGenerator.java:33`, `SkyblockGenerator.java:21`, `ClassicSkyblockGenerator.java:14` |
| Persisted id field | `WorldEntry.java:36` |
| Create → attach | `WorldManager.java:133-134` |
| Auto-load → attach | `WorldRegistry.java:282-286` |
| Reset → attach | `WorldManager.java:420-423` |
| `/worldcrud create` type handling | `WorldCRUDCommand.java:122-180` |
| Creative world hardcoded FLAT | `CreativeWorldService.java:93` |
| No `getDefaultWorldGenerator` / no `generator:` | grep 0 hits in `src/main`; `plugin.yml:1-38` |
| License AGPL-3.0-or-later | `pom.xml:12-18`, `LICENSE:1`, per-file headers |
| Java 25 / Paper 26.1.2 / api 26.1 / group org.xpfarm | `pom.xml:25,57-62,7-9`, `plugin.yml:4` |
