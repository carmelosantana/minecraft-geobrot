<!-- wayfinder:map -->
# Wayfinder map: GeoBrot terrain revival

## Destination

The GeoBrot Mandelbrot generator is **fixed** (world creates on the main thread; terrain is a coherent centered fractal, not the current lopsided sliver) and **tuned** into a high, gentle, buildable creative-world terrain — a natural build surface sitting just under cloud height, a geode underside, and a trimmed stone base — **specced into milestones ready to hand to `/sdd` implementation chips**. Exposing it as an *additional* creative-world option in WorldCRUD (never a replacement for the existing superflat world) is part of the destination but stays **fogged** until the terrain is proven.

This is a **planning** effort: the map is done when every decision is locked and nothing remains to decide before implementation chips execute.

## Notes

**Domain:** xpfarm.org Minecraft plugin ecosystem (Paper). Router: `minecraft-plugin-ecosystem`. Runtime baseline: the **Legendary Java Minecraft Geyser Floodgate** stack (build success alone is NOT compatibility evidence). Integration target: sibling repo `Plugins/worldcrud` (active, v1.3.0).

**Standing decisions locked during charting** (constraints for every ticket):
- **Reactivation:** GeoBrot is being **deliberately reactivated** from the ecosystem's excluded/abandoned list — an owner-only decision, now made. It re-enters the lifecycle gates. → **ADR candidate.**
- **Standards migration:** adopt current standards — **Java 25, Paper 26.1.2 build 74, `api-version: '26.1'`**, keep Maven group `org.xpfarm`.
- **License:** relicense **CC BY-NC 4.0 → AGPL-3.0-or-later** (required for any future code-merge into AGPL WorldCRUD). → **ADR candidate.**
- **"Fixed" means:** (a) `createWorld()` runs on the **main thread** (today it's called from `runTaskAsynchronously` — illegal in Paper, the prime crash suspect), (b) coherent **centered fractal**, (c) hits the height/stone targets below.
- **Fractal fidelity:** faithful, recognizable, centered/zoomable Mandelbrot — fix the coordinate mapping (use `FractalMath.worldToFractal`, center+zoom on spawn).
- **Surface height:** lower the surface band to **~Y160–170** (clouds render client-side at fixed Y≈192; leave headroom to build *up* to them — the plugin cannot move clouds).
- **Stone mass:** **raise the solid floor** (thin plinth, no bottomless void) — cut the current bedrock→surface stone plinth.
- **Relief:** **gentle, buildable plateaus** (low vertical amplitude; fractal detail as contour, not cliffs).
- **Palette:** simple **natural top** build surface (grass/dirt/stone); **geode palette** (amethyst/prismarine/copper, tiered by escape-time depth) on the sub-surface / underside / interior.

**Skills every session should consult:** `minecraft-plugin-ecosystem` (router), `minecraft-plugin-dev` (harness + runtime verify), `powerbank` (git identity / repo state). Git identity: `Carmelo Santana <me@carmelosantana.com>`; GitHub owner `carmelosantana` (never `herobrinesystems`).

**Execution handoff:** implementation milestones go to `/sdd` chips that MUST invoke the full **`superpowers:subagent-driven-development`** skill by name **plus `minecraft-plugin-dev`**. Formal lifecycle entry is **`minecraft-plugin-plan` (gate 1)** — geobrot has no `docs/PLUGIN_CHECKLIST.md`, so it counts as unplanned until that runs.

**Deferred to the plan gate:** autonomy mode (interactive vs autonomous).

## Decisions so far

<!-- one line per closed ticket: gist + link -->

- [Recon the WorldCRUD generator seam](issues/04-worldcrud-seam-recon.md): seam is a 3-point in-tree registry (`WorldTypes` enum + `WorldGenerators` `idFor`/`attach` switches + a `ChunkGenerator` subclass); no generator-by-name hook exists, so it points to **path A — port `MANDELBROT` into WorldCRUD's registry** (B can't avoid editing WorldCRUD anyway).

## Not yet specified

- **WorldCRUD integration milestone (path settled, execution deferred)** — the port-vs-separate *decision* is resolved by [04](issues/04-worldcrud-seam-recon.md): WorldCRUD has no generator-by-name hook, so integration = **port the Mandelbrot generator into WorldCRUD's `WorldGenerators` registry as a new `MANDELBROT` type** (three in-tree edits: `WorldTypes` enum + `WorldGenerators` `idFor`/`attach` + a `ChunkGenerator` subclass, mirroring FLAT/SKYBLOCK_CLASSIC). Still fogged as a *later milestone*: it stays deferred until (a) the geobrot terrain is fixed+tuned+proven and (b) the AGPL relicense is in place (a port needs it first). Graduates into an `/sdd` implementation milestone at that point — no wayfinder decision remains.
- **Formal reactivation lifecycle run** — plan gate-1 → standards migration → terrain tune → release → updater enrollment. Graduates into milestone tickets once diagnosis + terrain model are locked.
- **Geode-depth tiering detail** — exact materials at exact escape-time depths on the underside (config sketches deep / medium-deep / medium / shallow). Graduates from the terrain-model decision ([05](issues/05-terrain-model-spec.md)).

## Out of scope

- **Additional fractal types** (Julia, Burning Ship, Tricorn, Buddhabrot) — `FractalMath` already carries them, but exposing them is feature expansion beyond fix+tune.
- **Replacing the existing superflat creative world** — geobrot is an *additional* option, never a replacement.
- **`/mandel` command feature expansion** beyond what's needed to create/test worlds.
