#!/usr/bin/env python3
"""Strict full item gap reaudit after 0.1.451."""
from __future__ import annotations

import json
import re
from pathlib import Path

root = Path(r"D:/codex/mc-mod")
java = root / "src/main/java/com/xunxian/seekingimmortals"
assets = root / "src/main/resources/assets/seeking_immortals"
data = root / "src/main/resources/data/seeking_immortals"
text = root / "文本材料"

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

aliases: dict[str, str] = {}
idmap = json.loads((data / "reference/text_material_id_map.json").read_text(encoding="utf-8"))
for e in idmap["entries"]:
    sid = e.get("source_id")
    cid = e.get("canonical_id") or ""
    if isinstance(sid, str) and isinstance(cid, str) and cid.startswith("seeking_immortals:"):
        aliases[sid] = cid.split(":", 1)[1]
    elif isinstance(sid, str) and isinstance(cid, str) and cid.startswith("minecraft:"):
        aliases[sid] = cid


def covered(i: str) -> bool:
    i = (i or "").split(":")[-1]
    if not i:
        return True
    if i in reg:
        return True
    t = aliases.get(i)
    if t:
        if t.startswith("minecraft:") or t in reg:
            return True
        for s in ("_low", "_mid", "_high", "_supreme"):
            if f"{t}{s}" in reg:
                return True
    for s in ("_low", "_mid", "_high", "_supreme"):
        if f"{i}{s}" in reg:
            return True
    return False


NON = {
    "fail",
    "explosion",
    "beast_tribulation_bonus",
    "demon_qi_penalty",
    "five_elements_mountain_array",
    "nether_river_shield_zone",
    "herb",
    "pill",
    "specialty_item",
    "beast_material",
    "beast_material_bulk",
    "yin_material",
    "quality_pills",
    "merchants",
    "peak",
    "random_low_artifact",
    "contribution",
    "contribution_minor",
    "contribution_small",
    "contribution_star",
    "contribution_rank_inner",
    "pill_quality_roll",
    "legendary_roll",
    "array_merit",
    "forge_merit",
    "patrol_merit",
    "patrol_merit_bonus",
    "clan_merit",
    "clan_merit_goods",
    "clan_reputation",
    "war_merit",
    "war_merit_huangfeng",
    "merit_points",
    "merit_points_bulk",
    "merit_points_small",
    "tianyuan_contribution",
    "mulan_rep",
    "star_palace_rep",
    "inverse_rep",
    "reputation",
    "reputation_dajin",
    "dual_clan_rep",
    "dual_rep_neutral",
    "karma_ambiguous",
    "ascension_insight",
    "holy_bird_blessing_buff",
    "treasure_sense_buff",
    "spirit_realm_gate_fee_discount",
    "tax_reduction_deal",
    "temporary_sect_access",
    "kunwu_mountain_access",
    "foundation_pill_quota",
    "smuggle_route",
    "compliance_note",
    "expansion_changelog",
    "mortal_realm_cap",
    "alchemy_furnace_slot",
    "auction_slot",
    "inner_manual_slot",
    "nether_core_form_unlock",
    "soul_anchor_rite_unlock",
    "yin_body_condense_unlock",
    "yin_soul_burst_unlock",
    "wutu_side_quest_unlock",
    "nether_river_land",
    "thunder_tribulation",
    "spirit_beast",
    "fire_feather_spawn",
    "alchemy_flow",
    "ascension_flow",
    "beast_tiers",
    "boss_drops",
    "economy_tiers",
    "puppet_flow",
    "refinement_flow",
    "talisman_flow",
    "qianzhu_puppet_loop",
    "mulan_tianlan_war",
    "inverse_star_black_market",
    "wanbao_auction",
    "spirit_roots_catalog",
    "diyuan_layer_chest",
    "kunwu_layer_chest",
    "node_fallen_demon_rift_open",
    "changchun_gong",
    "recipe_craft_body_guard",
    "recipe_nascent_assist",
    "barbarian_seven_kings",
    "chaotic_sea_factions",
    "dajin_clan_politics",
    "dajin_kunwu",
    "demonic_six_overview",
    "diyuan_depth",
    "ghost_path_summary",
    "human_clan_league",
    "refinement_artifacts",
    "spatial_nodes",
    "spirit_eighteen_clans",
    "spirit_fengyuan",
    "spirit_realm_tribulation",
    "star_palace_governance",
    "tianlan_temple",
    "tianyuan_city",
    "tianyuan_garrison",
    "yin_underworld_cluster",
    "yuling_beast_puppet",
    "loot_blood_jiao",
    "loot_void_palace_lord",
    "pirate_black_market_outer_sea",
    "wanbao_auction_house_tiannan",
}
NON_PREFIX = (
    "recipe_",
    "craft_",
    "assemble_",
    "upgrade_",
    "quest_",
    "hook_",
    "event_",
    "chapter_",
    "node_",
    "sect_",
    "faction_",
    "realm_",
    "stage_",
    "skill_",
    "spell_",
    "technique_",
    "npc_",
    "dialogue_",
    "mission_",
    "story_",
    "wave_",
    "phase_",
    "zone_",
    "trial_",
    "index_",
    "dim_",
    "biome_",
    "region_",
    "path_",
    "method_",
    "daily_",
    "economy_",
    "price_",
    "band_",
    "boss_",
    "encounter_",
    "loot_table",
    "shop_",
    "merchant_",
    "chain_",
    "rep_",
)
NON_SUFFIX = (
    "_hall",
    "_shop",
    "_market",
    "_stall",
    "_pavilion",
    "_auction",
    "_exchange",
    "_bank",
    "_vendor",
    "_lane",
    "_supply",
    "_general",
    "_trade",
    "_forge",
    "_quest",
    "_event",
    "_chapter",
    "_faction",
    "_sect",
    "_region",
    "_dimension",
    "_biome",
    "_technique",
    "_spell",
    "_skill",
    "_method",
    "_path",
    "_dialogue",
    "_mission",
    "_index",
    "_map",
    "_schema",
    "_rules",
    "_catalog",
    "_list",
    "_table",
    "_graph",
    "_network",
    "_flow",
    "_system",
    "_compendium",
    "_guidance",
    "_magnitudes",
    "_bands",
    "_tiers",
    "_templates",
    "_bindings",
    "_manifest",
    "_registry",
    "_line",
    "_optional",
    "_ending",
    "_audience",
    "_survive",
    "_contact",
    "_defense",
    "_run",
    "_clash",
    "_escort",
    "_blockade",
    "_war",
    "_loop",
    "_outpost",
    "_chance",
    "_hint",
    "_unlock",
    "_overview",
    "_summary",
    "_governance",
    "_politics",
    "_changelog",
    "_access",
    "_quota",
    "_buff",
    "_discount",
    "_deal",
    "_rank",
    "_points",
    "_rep",
    "_merit",
    "_contribution",
    "_roll",
    "_insight",
    "_slot",
)


def is_non_item(i: str) -> bool:
    if i in NON:
        return True
    if any(i.startswith(p) for p in NON_PREFIX):
        return True
    if any(i.endswith(s) for s in NON_SUFFIX):
        return True
    if any(x in i for x in ("contribution_", "reputation", "_rep_", "_merit", "_hint", "_unlock", "_chance", "_buff", "_discount", "_quota")):
        return True
    return False


def walk_ids(obj, ids: set[str]) -> None:
    if isinstance(obj, dict):
        for k, v in obj.items():
            if k in {
                "id",
                "item",
                "item_id",
                "output",
                "result",
                "product",
                "material_id",
                "artifact_id",
                "pill_id",
                "manual_id",
                "talisman_id",
                "puppet_id",
                "vehicle_id",
                "part_id",
                "herb_id",
                "carrier_id",
                "reward_item",
                "cost_item",
                "fuel",
                "paper_item_id",
            } and isinstance(v, str):
                s = v.split(":")[-1]
                if re.fullmatch(r"[a-z][a-z0-9_]{1,80}", s):
                    ids.add(s)
            elif isinstance(v, (dict, list)):
                walk_ids(v, ids)
    elif isinstance(obj, list):
        for e in obj:
            walk_ids(e, ids)


KEY = [
    "artifacts_catalog.json",
    "materials_catalog.json",
    "consumables_catalog.json",
    "manuals_catalog.json",
    "formation_items_catalog.json",
    "formation_catalog.json",
    "block_items_catalog.json",
    "currency_items.json",
    "flight_vehicles.json",
    "puppet_definitions.json",
    "puppet_parts_catalog.json",
    "refine_manual_index.json",
    "refinement_system.json",
    "spirit_herbs_catalog.json",
    "talisman_catalog.json",
    "talisman_materials_catalog.json",
    "talisman_recipes.json",
    "alchemy_recipes.json",
    "merchant_shops.json",
    "novel_items_waves.json",
    "pills_catalog.json",
    "boss_loot_tables.json",
    "loot_tables.json",
    "puppet_craft_recipes.json",
    "craft_daily_loops.json",
    "quest_hooks.json",
    "beast_bestiary.json",
    "barbarian_demon_kings.json",
    "daily_quest_templates.json",
    "daily_random_events.json",
    "economy_contribution_exchange.json",
    "sect_contribution_shop.json",
    "items_by_region.json",
    "item_synergy.json",
    "trade_routes.json",
    "mulan_tianlan_war.json",
]

print("registered", len(reg), "bulk", len(bulk["items"]))
hard: list[str] = []
for name in KEY:
    paths = list(text.rglob(name)) + list(data.rglob(name))
    if not paths:
        print(name, "NOT_FOUND")
        continue
    ids: set[str] = set()
    for p in paths:
        try:
            obj = json.loads(p.read_text(encoding="utf-8"))
        except Exception:
            continue
        walk_ids(obj, ids)
    miss = sorted(i for i in ids if not covered(i) and not is_non_item(i))
    if miss:
        print(f"{name}: missing {len(miss)} -> {miss[:40]}")
        hard.extend(miss)
    else:
        print(f"{name}: ok ids={len(ids)}")

# all text item fields
item_fields: set[str] = set()
for p in text.rglob("*.json"):
    try:
        obj = json.loads(p.read_text(encoding="utf-8"))
    except Exception:
        continue

    def walk2(o):
        if isinstance(o, dict):
            for k, v in o.items():
                if k in {
                    "item",
                    "item_id",
                    "output",
                    "result",
                    "product",
                    "reward_item",
                    "cost_item",
                    "material_id",
                    "talisman_id",
                    "manual_id",
                    "pill_id",
                    "fuel",
                    "paper_item_id",
                } and isinstance(v, str):
                    s = v.split(":")[-1]
                    if re.fullmatch(r"[a-z][a-z0-9_]{1,80}", s):
                        item_fields.add(s)
                elif k in {
                    "items",
                    "materials",
                    "rewards",
                    "drops",
                    "entries",
                    "stock",
                    "goods",
                    "ingredients",
                    "outputs",
                    "papers",
                    "inks",
                    "parts",
                    "definitions",
                    "talismans",
                    "pills",
                    "artifacts",
                    "manuals",
                    "herbs",
                    "pools",
                } and isinstance(v, list):
                    for e in v:
                        if isinstance(e, dict):
                            for ik in ("id", "item", "output", "result", "product"):
                                if ik in e and isinstance(e[ik], str):
                                    s = e[ik].split(":")[-1]
                                    if re.fullmatch(r"[a-z][a-z0-9_]{1,80}", s):
                                        item_fields.add(s)
                            walk2(e)
                        elif isinstance(e, str) and re.fullmatch(r"[a-z][a-z0-9_]{1,80}", e):
                            item_fields.add(e)
                elif isinstance(v, (dict, list)):
                    walk2(v)
        elif isinstance(o, list):
            for e in o:
                walk2(e)

    walk2(obj)

miss_all = sorted(i for i in item_fields if not covered(i) and not is_non_item(i))
print("ALL text item-field hard missing", len(miss_all))
for i in miss_all:
    print(" ", i)
print("UNIQUE hard", sorted(set(hard + miss_all)))

refs: set[str] = set()
for sub in ("shops", "alchemy", "recipes", "loot_tables", "artifacts"):
    d = data / sub
    if d.exists():
        for p in d.rglob("*.json"):
            refs.update(re.findall(r"seeking_immortals:([a-z0-9_]+)", p.read_text(encoding="utf-8", errors="ignore")))
print("shipped refs missing", sorted(i for i in refs if not covered(i)))

print("id_map non-impl item-like:")
for e in idmap["entries"]:
    st = str(e.get("status") or "")
    ct = str(e.get("canonical_type") or "")
    if ct in ("item", "future_item") and not st.startswith("implemented"):
        print(" ", e.get("source_id"), st, ct, e.get("canonical_id"))

models = {p.stem for p in (assets / "models/item").glob("*.json")}
miss_model = []
for e in idmap["entries"]:
    st = str(e.get("status") or "")
    ct = e.get("canonical_type")
    cid = e.get("canonical_id") or ""
    if st.startswith("implemented") and ct == "item" and isinstance(cid, str) and cid.startswith("seeking_immortals:"):
        rid = cid.split(":", 1)[1]
        if rid not in models and rid in reg:
            miss_model.append(rid)
print("impl missing model among registered", len(miss_model), miss_model[:20])
print([ln for ln in (root / "gradle.properties").read_text(encoding="utf-8", errors="ignore").splitlines() if "mod_version" in ln][0])
