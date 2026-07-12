# JEI compat notes / TODO

Current state:

- JEI is optional in `mods.toml` and remains a development/runtime optional dependency.
- The mod already ships a JEI alchemy category for built-in alchemy recipes.
- The JEI alchemy view is limited to shipped/client-known recipes; datapack-added or server-only recipe changes are not dynamically synced into JEI in this pass.

Remaining TODO:

- Add dynamic recipe sync if server datapack alchemy recipes should appear in JEI after `/reload`.
- Add transfer/catalyst helpers for the alchemy furnace once the UX is ready.
- Keep direct JEI API use isolated under compatibility code so the mod still starts without JEI installed.
