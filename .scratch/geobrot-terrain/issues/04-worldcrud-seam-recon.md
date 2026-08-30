# Recon the WorldCRUD generator seam

Type: research
Status: resolved
Blocked by: (none)

## Question

Map how WorldCRUD (v1.3.0, `Plugins/worldcrud`) exposes creative-world generators, so the later (fogged) integration can be specced. **Read-only** investigation of the sibling repo.

Find:
- The **`WorldGenerators` seam + registry**: how `FLAT` and `SKYBLOCK_CLASSIC` are defined and registered.
- The **extension point** a new `MANDELBROT` generator would plug into (interface / enum / registry entry).
- How a creative-world type is **selected/created** by users, and whether a generator can be provided by *another plugin* (a named `ChunkGenerator`) vs. must live in-tree.
- WorldCRUD's **license** (expected AGPL) and package/group conventions.

**Answer records:** the seam's shape with `file:line` pointers, the two viable integration paths — **(A) port the generator into WorldCRUD** vs. **(B) keep geobrot separate, expose a named generator** — with pros/cons, and any constraint that bears on that port-vs-separate decision (it feeds the "WorldCRUD integration architecture" fog patch on the map).

Findings asset: write to `.scratch/geobrot-terrain/research/04-worldcrud-seam.md` and link here.

## Answer

**Seam shape:** WorldCRUD's generator seam is a **three-point, in-tree registry**, not an SPI. To
add a generator you edit three coordinated things in WorldCRUD's own tree: (1) the `WorldTypes`
enum (`WorldTypes.java:5-8`), (2) the `WorldGenerators` registry — id constants + the `idFor`
switch (`generators/WorldGenerators.java:40-49`) and the `attach(WorldCreator, id)` switch that
`new`s up the concrete generator (`generators/WorldGenerators.java:56-73`), and (3) a concrete
`ChunkGenerator` subclass under `generators/` (mirroring `FlatChunkGenerator.java:33`). The
generator is persisted as a String id in `WorldEntry` and reattached on create/auto-load/reset
via that same `attach` switch. Creation is user-facing via `/worldcrud create <name> [type]`
(`WorldCRUDCommand.java:122-180`); the shared creative world is hardcoded to `WorldTypes.FLAT`
(`CreativeWorldService.java:93`).

**Hard constraint:** `getDefaultWorldGenerator` is **not** overridden and `plugin.yml` has **no
`generator:` key**, so WorldCRUD has **no generator-by-name hook** — it cannot reference another
plugin's `ChunkGenerator` today. `attach()` only instantiates concrete classes from its own
package.

**Recommended path — (A) port the Mandelbrot generator into WorldCRUD's `WorldGenerators`
registry as a new `MANDELBROT` type.** One-line why: because there is no generator-by-name seam,
Path B (separate plugin, named generator) does **not** avoid editing WorldCRUD — it would still
need a new `attach` branch plus cross-plugin load-order fragility — while A matches the existing
FLAT/SKYBLOCK_CLASSIC pattern exactly and the only historical blocker (license) is already cleared
by the locked CC BY-NC → AGPL relicense. License confirmed **AGPL-3.0-or-later** (`pom.xml:12-18`,
`LICENSE`); conventions Java 25 / Paper 26.1.2 / api-version 26.1 / group `org.xpfarm`.

Full findings with `file:line` evidence: [../research/04-worldcrud-seam.md](../research/04-worldcrud-seam.md).
