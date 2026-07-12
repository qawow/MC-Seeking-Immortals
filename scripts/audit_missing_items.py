#!/usr/bin/env python3
"""Accurate missing-item audit for seeking_immortals."""
from __future__ import annotations

import json
import re
from collections import Counter
from pathlib import Path

ROOT = Path(".")


def load_reg() -> set[str]:
    mi = (ROOT / "src/main/java/com/xunxian/seekingimmortals/registry/ModItems.java").read_text(
        encoding="utf-8"
    )
    reg = set(
        re.findall(
            r'register(?:Artifact|Material|SpiritStone|TechniqueManual|CatalogPill|AlchemyFormula|AlchemyLid|DanFire)?\(\s*"([a-z0-9_]+)"',
            mi,
        )
    )
    reg |= set(re.findall(r'ITEMS\.register\(\s*"([a-z0-9_]+)"', mi))
    cpt = ROOT / "src/main/java/com/xunxian/seekingimmortals/item/pill/CatalogPillType.java"
    if cpt.exists():
        t = cpt.read_text(encoding="utf-8")
        for m in re.finditer(r'([A-Z0-9_]+)\("([a-z0-9_]+)"', t):
            base = m.group(2)
            reg.add(base)
            for q in ("low", "mid", "high", "perfect", "supreme"):
                reg.add(f"{base}_{q}")
    return reg


def main() -> None:
    reg = load_reg()
    models = {
        p.stem
        for p in (ROOT / "src/main/resources/assets/seeking_immortals/models/item").glob("*.json")
    }
    print("reg", len(reg), "models", len(models))
    for x in [
        "metal_spirit_stone",
        "soul_gathering_pill",
        "body_tempering_pill",
        "alchemy_lid_mid",
        "dan_fire_mid",
        "spirit_realm_condense_pill",
        "pressure_resist_pill",
        "beast_blood_vial",
        "star_sand",
    ]:
        print(x, "reg", x in reg, "model", x in models)

    idmap = json.loads(
        (
            ROOT / "src/main/resources/data/seeking_immortals/reference/text_material_id_map.json"
        ).read_text(encoding="utf-8")
    )
    source_to_canon = {}
    for e in idmap["entries"]:
        if str(e.get("status", "")).startswith("implemented") and e.get("canonical_type") == "item":
            cid = e.get("canonical_id") or ""
            if cid.startswith("seeking_immortals:"):
                source_to_canon[e["source_id"]] = cid.split(":", 1)[1]
            elif cid.startswith("minecraft:"):
                source_to_canon[e["source_id"]] = cid

    def resolve(i: str) -> bool:
        i = i.split(":")[-1]
        if i in reg or i in models:
            return True
        c = source_to_canon.get(i)
        if not c:
            return False
        if c.startswith("minecraft:"):
            return True
        return c in reg or c in models

    # refinement
    recipes = json.loads(
        (
            ROOT / "src/main/resources/data/seeking_immortals/artifacts/refinement_recipes.json"
        ).read_text(encoding="utf-8")
    )
    arts = {r["artifact_id"].split(":")[-1] for r in recipes["recipes"] if r.get("artifact_id")}
    print("ref arts missing", sorted(a for a in arts if not resolve(a)))
    mats = Counter()
    for r in recipes["recipes"]:
        for m in r.get("materials") or []:
            mid = (m.get("id") or "").split(":")[-1]
            if mid:
                mats[mid] += 1
    print("ref mats unresolved", [(m, c) for m, c in mats.items() if not resolve(m)])

    # compressed aliases still used by refinement
    print("refinement compressed aliases still active:")
    for e in idmap["entries"]:
        if not str(e.get("status", "")).startswith("implemented"):
            continue
        sid = e.get("source_id")
        cid = e.get("canonical_id") or ""
        if not cid.startswith("seeking_immortals:"):
            continue
        last = cid.split(":")[-1]
        if sid and sid != last and sid in mats:
            print(" ", sid, "x" + str(mats[sid]), "->", cid, e.get("status"), e.get("source_category"))

    # recipes/
    rec_hard = set()
    for p in (ROOT / "src/main/resources/data/seeking_immortals/recipes").rglob("*.json"):
        try:
            d = json.loads(p.read_text(encoding="utf-8"))
        except Exception:
            continue
        for m in re.findall(r"seeking_immortals:([a-z0-9_]+)", json.dumps(d)):
            if m in ("crafting_shaped", "crafting_shapeless", "refinement"):
                continue
            if not resolve(m):
                rec_hard.add(m)
    print("recipe hard", sorted(rec_hard))

    # shops / merchant
    shop_hard = set()
    for p in (ROOT / "src/main/resources/data/seeking_immortals").rglob("*.json"):
        path = str(p).replace("\\", "/").lower()
        if "shop" not in path and "merchant" not in path:
            continue
        try:
            d = json.loads(p.read_text(encoding="utf-8"))
        except Exception:
            continue
        for m in re.findall(r"seeking_immortals:([a-z0-9_]+)", json.dumps(d)):
            if m in ("item", "sect_contribution"):
                continue
            if not resolve(m):
                shop_hard.add(m)
    print("shop hard", sorted(shop_hard))

    # true implemented canonical missing (not just source alias)
    canon_miss = []
    for e in idmap["entries"]:
        cid = e.get("canonical_id") or ""
        if not cid.startswith("seeking_immortals:"):
            continue
        if not str(e.get("status", "")).startswith("implemented"):
            continue
        iid = cid.split(":", 1)[1]
        if not resolve(iid):
            canon_miss.append((e.get("source_id"), cid, e.get("source_category"), e.get("status")))
    print("true canon miss", len(canon_miss))
    for x in canon_miss[:40]:
        print(" ", x)

    # partial compressed non-refinement high value
    print("other partial compressed (sample):")
    n = 0
    for e in idmap["entries"]:
        if "partial" not in str(e.get("status", "")):
            continue
        sid = e.get("source_id")
        cid = e.get("canonical_id") or ""
        if not cid.startswith("seeking_immortals:"):
            continue
        last = cid.split(":")[-1]
        if sid != last:
            print(" ", sid, "->", cid, e.get("source_category"))
            n += 1
            if n >= 40:
                break

    # vanilla left
    van = [
        (e.get("source_id"), e.get("canonical_id"))
        for e in idmap["entries"]
        if isinstance(e.get("canonical_id"), str) and e["canonical_id"].startswith("minecraft:")
    ]
    print("vanilla canonical left", van)

    # manuals catalog hard
    for rel in (
        "src/main/resources/data/seeking_immortals/catalog/manuals_catalog.json",
        "src/main/resources/data/seeking_immortals/text_material/manuals_catalog.json",
    ):
        p = ROOT / rel
        if not p.exists():
            continue
        d = json.loads(p.read_text(encoding="utf-8"))
        entries = d.get("manuals") or d.get("entries") or d.get("items") or d
        if isinstance(entries, dict):
            vals = []
            for k, v in entries.items():
                if isinstance(v, dict):
                    vv = dict(v)
                    vv.setdefault("id", k)
                    vals.append(vv)
                else:
                    vals.append({"id": k})
            entries = vals
        hard = []
        soft = []
        for e in entries if isinstance(entries, list) else []:
            if not isinstance(e, dict):
                continue
            i = (e.get("id") or e.get("manual_id") or "").split(":")[-1]
            if not i:
                continue
            if resolve(i):
                continue
            if i in source_to_canon:
                soft.append((i, source_to_canon[i]))
            else:
                hard.append(i)
        print("manuals", p.name, "hard", hard, "soft", soft)


if __name__ == "__main__":
    main()
