# Reconnect GeoBrot to git + GitHub

Type: task
Status: claimed
Blocked by: (none)

## Question

GeoBrot has **no local `.git`** and does not appear in the GitHub inventory, yet the README claims `github.com/carmelosantana/geobrot`. Put the repo on a clean git footing so later release/CI/updater milestones have somewhere to push.

Work:
- `git init` (default branch `main`); commit identity `Carmelo Santana <me@carmelosantana.com>` (global default — do NOT override).
- Confirm whether `carmelosantana/geobrot` exists on GitHub (`gh` / powerbank `Infrastructure/GitHub.md`). If it exists, reconcile local ↔ remote; if not, decide create-now vs. create-at-first-release.
- Set the SSH remote to `carmelosantana` ownership — never the obsolete `herobrinesystems` identity anywhere.
- Make the initial commit. Do **not** push until the owner approves (outward-facing action).

AFK where possible; GitHub repo creation / first push is outward-facing and needs approval (and possibly `gh` auth — the `engineering:github` MCP is currently unauthenticated in this session).

**Answer records:** git status, whether the remote exists, the remote URL, and what (if anything) still needs owner approval or auth.
