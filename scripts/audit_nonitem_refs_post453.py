#!/usr/bin/env python3
from __future__ import annotations

import json
import re
from collections import defaultdict
from pathlib import Path

root = Path(__file__).resolve().parents[1]
java = root / "src/main/java/com/xunxian/seekingimmortals"
assets = root / "src/main/resources/assets/seeking_immortals"
data = root / "src/main/resources/data/seeking_immortals"

reg: set[str] = set()
for p in java.rglob("*.java"):
    t = p.read_text(encoding="utf-8", errors="ignore")
    reg.update(re.findall(r'\.register\("([a-z0-9_]+)"', t))
    reg.update(re.findall(r'register\w+\("([a-z0-9_]+)"', t))
cpt = (java / "item/pill/CatalogPillType.java").read_text(encoding="utf-8")
enum_to_id = dict(re.findall(r'([A-Z0-9_]+)\("([a-z0-9_]+)"', cpt))
reg.update(enum_to_id.values())
mi = (java / "registry/ModItems.java").read_text(encoding="utf-8")
for m in re.finditer(r'registerCatalogPill\(CatalogPillType\.([A-Z0-9_]+)(?:,\s*"([a-z0-9_]+)")?', mi):
    base = enum_to_id[m.group(1)]
    reg.add(base if not m.group(2) else f"{base}_{m.group(2)}")
bulk = json.loads((assets / "catalog_bulk_items.json").read_text(encoding="utf-8"))
reg.update(o["id"] for o in bulk["items"])
mb = (java / "registry/ModBlocks.java").read_text(encoding="utf-8")
blocks = set(re.findall(r'BLOCKS\.register\("([a-z0-9_]+)"', mb))
reg.update(blocks)

ent: set[str] = set()
for p in (java / "registry").glob("*.java"):
    t = p.read_text(encoding="utf-8", errors="ignore")
    if "EntityType" in t or "ENTITIES" in t:
        ent.update(re.findall(r'\.register\("([a-z0-9_]+)"', t))
print("entities", sorted(ent))

aliases: dict[str, str] = {}
for e in json.loads((data / "reference/text_material_id_map.json").read_text(encoding="utf-8"))["entries"]:
    sid, cid = e.get("source_id"), e.get("canonical_id") or ""
    if isinstance(sid, str) and isinstance(cid, str) and cid.startswith("seeking_immortals:"):
        aliases[sid] = cid.split(":", 1)[1]
    elif isinstance(sid, str) and isinstance(cid, str) and cid.startswith("minecraft:"):
        aliases[sid] = cid
reg.add("refinement")


def covered(i: str) -> bool:
    i = (i or "").split(":")[-1]
    if i in reg or i in ent:
        return True
    t = aliases.get(i)
    if t and (t.startswith("minecraft:") or t in reg or t in ent):
        return True
    for s in ("_low", "_mid", "_high", "_supreme"):
        if f"{i}{s}" in reg:
            return True
    return False


miss_ctx: dict[str, set[str]] = defaultdict(set)
for p in data.rglob("*.json"):
    t = p.read_text(encoding="utf-8", errors="ignore")
    for m in re.findall(r"seeking_immortals:([a-z0-9_]+)", t):
        if not covered(m):
            miss_ctx[m].add(str(p.relative_to(root)).replace("\\", "/"))

print("missing refs", len(miss_ctx))
for m, files in sorted(miss_ctx.items()):
    f0 = sorted(files)[0]
    kinds = []
    joined = " ".join(files)
    if "dimension" in joined:
        kinds.append("dimension")
    if "biome" in joined:
        kinds.append("biome")
    if "structure" in joined:
        kinds.append("structure")
    if "tags/" in joined:
        kinds.append("tag")
    if "worldpack" in joined or "worldgen" in joined:
        kinds.append("world")
    if "loot" in joined:
        kinds.append("loot")
    if "entity" in joined or "spawn" in joined:
        kinds.append("entity")
    if "sound" in joined:
        kinds.append("sound")
    print(f"{m}\t{','.join(kinds) or '?'}\t{f0}")

models = {p.stem for p in (assets / "models/item").glob("*.json")}
tex = {p.stem for p in (assets / "textures/item").glob("*.png")}
orphans = sorted(tex - models)
print("orphan item textures", len(orphans))
print("orphan tex not reg", [o for o in orphans if o not in reg][:40], "count", len([o for o in orphans if o not in reg]))
SKIP = {
    "cultivation_fireball",
    "cushion_seat",
    "earth_wall",
    "formation_core",
    "market_trader",
    "refinement",
    "sect_steward",
    "seeking_immortals_tab",
    "spirit_boat",
    "storage_bracelet",
    "summoned_servitor",
    "sword_projectile",
}
no_model = [i for i in sorted(reg) if i not in models and i not in SKIP and i not in blocks]
print("reg no item model", len(no_model), no_model[:20])

# sounds?
sounds = assets / "sounds.json"
if sounds.exists():
    print("sounds.json exists")
else:
    print("sounds.json missing")
