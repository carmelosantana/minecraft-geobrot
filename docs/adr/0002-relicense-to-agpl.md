---
status: accepted
date: 2026-08-30
---

# Relicense GeoBrot from CC BY-NC 4.0 to AGPL-3.0-or-later

GeoBrot's abandoned-era licensing was **CC BY-NC 4.0** — a Creative Commons *content* licence
with a **non-commercial** restriction, which is both an ill fit for source code and,
critically, incompatible with the ecosystem. The active xpfarm.org plugins and the updater are
all **AGPL-3.0-or-later**, and the concrete near-term goal — porting GeoBrot's Mandelbrot
generator into the AGPL-licensed WorldCRUD plugin's `WorldGenerators` registry — is **not
possible** while GeoBrot carries a `-NC` licence: an AGPL work cannot absorb non-commercial
code, and `play.xpfarm.org` is a running service the `-NC` clause casts doubt over regardless.

We relicense the project to **AGPL-3.0-or-later**, matching every other active plugin in the
suite. GeoBrot's history has a single owner/author (Carmelo Santana), so there are no
third-party copyright holders whose consent would be required to change the licence.

## Consequences

- The physical relicense — adding an AGPL-3.0-or-later `LICENSE` file, aligning the Maven
  `<licenses>` metadata, and applying AGPL headers — is executed at the **scaffold/metadata
  gate (gate 3)**, not here; this ADR records the *decision* and its rationale. The gate-1
  checklist tracks it as an open §3 item.
- The relicense is a **prerequisite** for the deferred WorldCRUD code-merge milestone; that
  port stays fogged until this is in place.
- CC BY-NC 4.0 is retired for this project and must not be reintroduced or cited as GeoBrot's
  licence in code, metadata, or docs.
