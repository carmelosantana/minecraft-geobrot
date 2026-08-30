# Diagnose why GeoBrot terrain is broken (static + runtime repro)

Type: research
Status: open
Blocked by: 02

## Question

Confirm the root causes of the broken generator with a **runtime reproduction** in the harness, validating the static recon. Candidate causes (from static recon):

1. **Async world creation** — `createWorld()` is invoked inside `runTaskAsynchronously` (`MandelCommand.handleCreate` → `FractalWorldManager.createFractalWorld`). Creating a world off the main thread is illegal in Paper. **Prime crash suspect.**
2. **Naive coordinate mapping** — `fractalX = centerX + worldX/(zoom*100)`; `FractalMath.worldToFractal` (the proper normalizer) is unused → lopsided sliver over a 512-block world, not a centered fractal; zoom barely acts.
3. **`Biome.PLAINS`** — may need the 1.21.6+ registry form rather than the enum reference in `getDefaultBiomeProvider`.
4. **Height/stone model** — in-set surface fixed at `seaLevel + (maxHeight-seaLevel)/2` ≈ Y192 (maxHeight passed 512 → clamped 320); solid fill from `minHeight`→surface (the "too-tall stone bottom"); the config geode palette is entirely unused (generator hardcodes stone/dirt/grass).

Reproduce each in the harness (create a world via `/mandel create`, observe crash / terrain), capture the stacktrace, logs, and a screenshot.

**Answer records:** each cause confirmed vs. refuted, the actual failure signature (stacktrace), a screenshot/description of the current terrain, and the precise current height/material model as it actually runs.

Findings asset: write to `.scratch/geobrot-terrain/research/03-diagnosis.md` and link here.
