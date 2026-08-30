# Stand up GeoBrot in the Legendary test harness

Type: task
Status: claimed
Blocked by: (none)

## Question

Get GeoBrot building and loading in the **Legendary Java Minecraft Geyser Floodgate** stack so the diagnosis ([03](03-diagnose-break.md)) can reproduce the break at runtime. Use `minecraft-plugin-dev` + the toolkit `xpfarm-plugin-toolkit/ENVIRONMENT.md`.

Work:
- Review `ENVIRONMENT.md` + `PLUGIN_LIFECYCLE.md` for the harness / build toolchain and the disposable-stack invocation.
- Build geobrot **AS-IS** (Paper 1.21.6 / Java 21) — do **NOT** migrate yet; the diagnosis must reproduce the *current* behavior. Rely on Paper backwards-compat to load `api-version 1.21` on the current stack, or record if it won't load without migration.
- Boot a disposable Legendary stack; confirm the plugin enables and `/mandel` registers.

**Answer records:** build result, whether the plugin loads on the current stack as-is, the exact harness invocation used, and any blocker (e.g. "must migrate to `26.1` before it will load" — which would reshape ticket 03's ordering).
