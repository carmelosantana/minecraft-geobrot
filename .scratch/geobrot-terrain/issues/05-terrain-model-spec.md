# Decide the vertical terrain model (height, stone floor, relief, geode tiers)

Type: grilling
Status: open
Blocked by: 03

## Question

With the break diagnosed, **lock the exact numbers** for the tuned terrain — a decision to hand to the `/sdd` tuning chip. Charting constraints: faithful centered fractal; surface band **~Y160–170**; **raised solid floor** (no bedrock→surface plinth); **gentle** buildable relief; **natural top, geode underside**.

Decide:
- **Solid floor Y** (where fill starts) and total **island thickness**.
- **In-set surface target Y** and the **escaped-point → height** mapping, so relief is gentle (a few blocks of amplitude, not today's 0–64 spread).
- **Surface skin** depth (grass/dirt) and the **geode-tier depths/materials** underneath (map to config `deep` / `medium-deep` / `medium` / `shallow`).
- **Spawn-safety** implications of the chosen floor (the current `findSpawnLocation` scans top-down for solid+2-air).

**Answer records:** the concrete vertical model (floor, thickness, surface Y, amplitude, material bands by depth) ready to implement. Graduates the "geode-depth tiering detail" fog patch on the map.
