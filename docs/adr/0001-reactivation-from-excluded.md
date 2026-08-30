---
status: accepted
date: 2026-08-30
---

# Reactivate GeoBrot from the ecosystem's excluded/abandoned list

GeoBrot was on the xpfarm.org plugin ecosystem's **permanently-excluded** set — the abandoned
plugins (alongside Agent Steve and Solar Power) that `minecraft-plugin-ecosystem` treats as out
of the active lifecycle, receiving no release or updater work and whose dependencies, metadata,
licensing, and ownership must **not** be treated as current standards. The router is explicit
that no skill lifts that exclusion on its own judgment: reactivation "is not something any skill
in this suite makes on its own" and requires a deliberate owner decision.

The owner has made that decision: GeoBrot is deliberately reactivated and re-enters the
lifecycle gates, beginning with this gate-1 plan. The motivation is that its Mandelbrot terrain
generator, once fixed and tuned, is worth reviving as an **additional** creative-world option
for `play.xpfarm.org` (never a replacement for the existing superflat world), with a future,
still-deferred path to integrate into the active WorldCRUD plugin.

## Consequences

- GeoBrot is no longer excluded; it is classified **experimental** (its generator currently
  produces no terrain at all — see the diagnosis in `.scratch/geobrot-terrain/`), running the
  pipeline with gates 7b (matrix), 9 (release), and 10 (updater) intentionally withheld until
  the terrain is fixed and runtime-proven.
- As an active-track plugin it must now meet current ecosystem standards — the standards
  migration (Java 25 / Paper 26.1.2 / `api-version '26.1'`, group `org.xpfarm`) and the
  relicense to AGPL ([ADR 0002](0002-relicense-to-agpl.md)) are direct downstream obligations of
  this decision.
- This reactivation covers **GeoBrot only**. It sets no precedent for Agent Steve or Solar
  Power, which remain excluded.
