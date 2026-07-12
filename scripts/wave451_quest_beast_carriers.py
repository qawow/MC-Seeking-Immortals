#!/usr/bin/env python3
"""0.1.451: bulk-register remaining physical quest/beast/reward item carriers + soft aliases."""
from __future__ import annotations

import hashlib
import json
import re
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(".")
ASSETS = ROOT / "src/main/resources/assets/seeking_immortals"
BULK = ASSETS / "catalog_bulk_items.json"
ITEM_MODELS = ASSETS / "models/item"
ITEM_TEX = ASSETS / "textures/item"
ZH = ASSETS / "lang/zh_cn.json"
EN = ASSETS / "lang/en_us.json"
IDMAP = ROOT / "src/main/resources/data/seeking_immortals/reference/text_material_id_map.json"
TEXT = ROOT / "文本材料"
JAVA = ROOT / "src/main/java/com/xunxian/seekingimmortals"

# Known rename aliases -> already registered carriers
SOFT_ALIASES = {
    "talisman_fire_burst": "fire_burst_talisman",
    "low_grade_spirit_stone": "low_spirit_stone",
    "spatial_crystal": "space_crystal",
    "alliance_merit": "alliance_merit_token",
    "venom_sac": "venom_sac_refined",
    "beast_hide_low": "beast_hide",
    "beast_bone": "spirit_beast_bone",
    "thunder_horn": "thunder_dragon_horn",
    "true_dragon_blood_trace": "true_dragon_blood",
    "spirit_beast_bridle_high": "spirit_beast_bridle",
    "talisman_blank_high": "demon_suppress_talisman_blank",
    "yellow_palm_fan": "qingye_leaf_fan",
}

# Deferred non-item effects
EXCLUDE = {
    "fail",
    "explosion",
    "beast_tribulation_bonus",
    "demon_qi_penalty",
    "five_elements_mountain_array",
    "nether_river_shield_zone",
}

# Extra physical ids from OTH classification
EXTRA_PHYSICAL = {
    "chitin",
    "feather_fire",
    "feather_wind",
    "scale_water",
    "peacock_true_fire",
    "pearl_raw_bulk",
    "refine_material_mid",
    "refinement_material_high",
    "talisman_recipe",
    "talisman_recipe_mid",
    "formation_flag_low",
    "low_artifact_blank",
}

SOFT_EXACT = {
    "contribution",
    "contribution_minor",
    "contribution_small",
    "contribution_star",
    "contribution_rank_inner",
    "pill_quality_roll",
    "pill_quality_rank_up_chance_tag",
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
}

REAL_TOKENS = (
    "_token",
    "_fragment",
    "_pill",
    "_talisman",
    "_manual",
    "_scroll",
    "_core",
    "_bone",
    "_scale",
    "_feather",
    "_hide",
    "_fur",
    "_horn",
    "_shell",
    "_gall",
    "_pelt",
    "_pearl",
    "_crystal",
    "_blood",
    "_meat",
    "_sac",
    "_fiber",
    "_resin",
    "_flag",
    "_blank",
    "_paper",
    "_bundle",
    "_shard",
    "_map",
    "_permit",
    "_license",
    "_voucher",
    "_credit",
    "_charm",
    "_bead",
    "_bridle",
    "_contract",
    "_blueprint",
    "_plate",
    "_seed",
    "_bait",
    "_fang",
    "_wing",
    "_mane",
    "_chitin",
    "_marrow",
    "_silk",
    "_dust",
    "_sand",
    "_essence",
    "_trace",
    "_chain",
    "_cloak",
    "_fan",
    "_page",
    "_cipher",
    "_pass",
    "_tag",
    "_lot",
    "_spoil",
    "_trophy",
    "_pact",
    "_truce",
    "_waiver",
    "_vip",
    "_larva",
    "_tail",
    "_skin",
    "_claw",
    "_recipe",
    "_art",
    "_scout",
    "_campaign",
    "_contact",
    "_survival",
    "_disciple",
    "_quest",
    "_identify",
    "_suppress",
    "_reinforcement",
    "_teleport",
    "_gate",
    "_intel",
    "_ancestor",
    "_body",
    "_wind",
    "_ghost",
    "_soul",
    "_azure",
    "_intent",
)


def load_registered() -> set[str]:
    reg: set[str] = set()
    for p in JAVA.rglob("*.java"):
        t = p.read_text(encoding="utf-8", errors="ignore")
        reg.update(re.findall(r'\.register\("([a-z0-9_]+)"', t))
        reg.update(re.findall(r'register\w+\("([a-z0-9_]+)"', t))
    cpt = JAVA / "item/pill/CatalogPillType.java"
    enum_to_id = dict(re.findall(r'([A-Z0-9_]+)\("([a-z0-9_]+)"', cpt.read_text(encoding="utf-8")))
    reg.update(enum_to_id.values())
    mi = (JAVA / "registry/ModItems.java").read_text(encoding="utf-8")
    for m in re.finditer(r'registerCatalogPill\(CatalogPillType\.([A-Z0-9_]+)(?:,\s*"([a-z0-9_]+)")?', mi):
        base = enum_to_id[m.group(1)]
        reg.add(base if not m.group(2) else f"{base}_{m.group(2)}")
    bulk = json.loads(BULK.read_text(encoding="utf-8"))
    reg.update(o["id"] for o in bulk["items"])
    return reg


def covered(i: str, reg: set[str], aliases: dict[str, str]) -> bool:
    i = i.split(":")[-1]
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


def is_soft(i: str) -> bool:
    if i in SOFT_EXACT or i in EXCLUDE:
        return True
    if i.startswith("rep_"):
        return True
    if any(x in i for x in ("_hint", "_unlock", "_overview", "_summary", "_governance", "_politics", "_changelog", "_access", "_quota", "_chance", "_buff", "_discount", "_deal", "_rank", "_points", "_rep", "_merit", "contribution_", "reputation")):
        return True
    if i.startswith(("recipe_", "craft_", "assemble_", "upgrade_", "refine_")) and i not in EXTRA_PHYSICAL:
        # refine_material_mid is physical-ish and in EXTRA
        if not i.startswith("refine_material") and not i.startswith("refinement_material"):
            return True
    return False


def looks_physical(i: str) -> bool:
    if is_soft(i):
        return False
    if i in EXTRA_PHYSICAL:
        return True
    return any(t in i for t in REAL_TOKENS)


def collect_missing(reg: set[str], aliases: dict[str, str]) -> list[str]:
    item_fields: set[str] = set()
    keys = {
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
    }
    for p in TEXT.rglob("*.json"):
        try:
            obj = json.loads(p.read_text(encoding="utf-8"))
        except Exception:
            continue

        def walk(o):
            if isinstance(o, dict):
                for k, v in o.items():
                    if k in keys and isinstance(v, str):
                        s = v.split(":")[-1]
                        if re.fullmatch(r"[a-z][a-z0-9_]{1,80}", s):
                            item_fields.add(s)
                    elif k in (
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
                    ) and isinstance(v, list):
                        for e in v:
                            if isinstance(e, dict):
                                for ik in ("id", "item", "output", "result", "product"):
                                    if ik in e and isinstance(e[ik], str):
                                        s = e[ik].split(":")[-1]
                                        if re.fullmatch(r"[a-z][a-z0-9_]{1,80}", s):
                                            item_fields.add(s)
                                walk(e)
                            elif isinstance(e, str) and re.fullmatch(r"[a-z][a-z0-9_]{1,80}", e):
                                item_fields.add(e)
                    elif isinstance(v, (dict, list)):
                        walk(v)
            elif isinstance(o, list):
                for e in o:
                    walk(e)

        walk(obj)

    out = []
    for i in sorted(item_fields):
        if covered(i, reg, aliases):
            continue
        if i in SOFT_ALIASES:
            continue
        if looks_physical(i):
            out.append(i)
    # always include extras if missing
    for i in EXTRA_PHYSICAL:
        if not covered(i, reg, aliases) and i not in out and i not in SOFT_ALIASES:
            out.append(i)
    return sorted(set(out))


def cat_for(rid: str) -> tuple[str, str]:
    if any(x in rid for x in ("_pill", "_dan", "_elixir", "_ointment")):
        return "pill", "rare"
    if any(x in rid for x in ("_talisman", "_charm", "_seal")):
        return "talisman", "uncommon"
    if any(x in rid for x in ("_manual", "_scroll", "_page", "_recipe", "_blueprint", "_cipher", "_art_")):
        return "manual", "rare"
    if any(x in rid for x in ("_token", "_permit", "_license", "_voucher", "_credit", "_ticket", "_pass", "_tag", "_vip", "_waiver", "_pact", "_truce")):
        return "currency", "uncommon"
    if any(x in rid for x in ("_fan", "_sword", "_blade", "_armor", "_boots", "_ring", "_umbrella", "_mirror", "_disk", "_bracelet", "_pendant", "_shield", "_whip", "_net", "_shovel", "_ruler", "_brick", "_chain", "_bell", "_bowl", "_needle")):
        return "artifact", "rare"
    if any(x in rid for x in ("_flag", "_blank", "_core", "_fragment", "_shard")):
        return "material", "rare"
    return "material", "uncommon"


def zh_name(rid: str) -> str:
    # Prefer readable Chinese-ish placeholders using common mappings; fall back to id.
    special = {
        "formation_flag_low": "下品阵旗",
        "low_artifact_blank": "下品法宝胚",
        "alchemy_manual_low": "下品丹经",
        "beast_taming_manual": "御兽手册",
        "beast_taming_pill_low": "下品驯兽丹",
        "appearance_lock_pill": "定颜丹",
        "marrow_drain_pill": "抽髓丹",
        "qingxu_pill": "清虚丹",
        "anti_fashi_talisman": "克伐尸符",
        "illusion_suppress_talisman": "破幻符",
        "talisman_paper_bundle": "符纸捆",
        "talisman_recipe": "符箓配方",
        "talisman_recipe_mid": "中品符箓配方",
        "talisman_recipe_high_bundle": "上品符箓配方捆",
        "dayan_art_scroll": "大衍术卷",
        "manual_qingyuan_sword_inner": "青元剑诀内篇",
        "ancient_blood": "上古精血",
        "ancient_pine_resin": "古松脂",
        "ape_bone": "猿骨",
        "bear_gall": "熊胆",
        "beast_fur_low": "下品兽毛",
        "beast_fur_mid": "中品兽毛",
        "beast_horn_fire": "火角",
        "beast_meat": "兽肉",
        "beast_soul": "兽魂",
        "beetle_shell": "甲虫壳",
        "carp_scale": "鲤鳞",
        "chitin": "甲壳质",
        "corpse_marrow": "尸髓",
        "demon_core_peak": "巅峰妖核",
        "dragon_scale_earth": "土龙鳞",
        "feather_fire": "火羽",
        "feather_wind": "风羽",
        "fox_tail": "狐尾",
        "frost_wing": "霜翅",
        "gold_fur": "金毛",
        "ice_fang": "冰牙",
        "jiao_blood": "蛟血",
        "jiao_horn": "蛟角",
        "mirage_pearl": "蜃珠",
        "moon_corpse_core": "月尸核",
        "mulan_spirit_feather": "木兰灵羽",
        "peacock_true_fire": "孔雀真火",
        "qilin_blood": "麒麟血",
        "scale_water": "水鳞",
        "serpent_skin": "蛇皮",
        "silver_wing": "银翅",
        "sky_tiger_wing": "天虎翼",
        "snake_king_gall": "蛇王胆",
        "spirit_dust": "灵尘",
        "spirit_sand": "灵沙",
        "suanni_mane": "狻猊鬃",
        "thunder_demon_core": "雷妖核",
        "tiger_bone": "虎骨",
        "tiger_king_bone": "虎王骨",
        "turtle_king_shell": "龟王甲",
        "vine_fiber": "藤丝",
        "water_pearl": "水珠",
        "wind_core": "风核",
        "wind_feather_bait": "风羽饵",
        "wind_sky_crystal": "风天晶",
        "wood_array_seed": "木阵种",
        "wood_essence": "木精",
        "wutu_wolf_pelt": "巫屠狼皮",
        "yin_silk": "阴丝",
        "gold_devour_larva": "噬金虫幼体",
        "fox_mist_pearl": "狐雾珠",
        "hunt_trophy_fang": "猎获獠牙",
        "illusion_cloak_shard": "幻衣碎片",
        "illusion_core": "幻核",
        "illusion_true_essence": "幻真精",
        "soul_calm_bead": "定魂珠",
        "soul_banner_fragment": "魂幡碎片",
        "demon_suppress_chain": "镇魔链",
        "defense_array_plate": "护阵盘",
        "earth_spirit_core": "地灵核",
        "diyuan_core_crystal": "地渊晶核",
        "fashi_soul_crystal": "伐尸魂晶",
        "fashi_art_fragment": "伐尸术残篇",
        "ghost_cultivation_fragment": "鬼修残篇",
        "guiling_soul_art_fragment": "鬼灵炼魂残篇",
        "qixuan_azure_pearl_art_fragment": "七玄青珠术残篇",
        "ascension_map_fragment": "飞升图残片",
        "kunwu_map_fragment": "昆吾图残片",
        "barbarian_map": "蛮荒地图",
        "fengyuan_map": "凤原地图",
        "artifact_identify_scroll": "鉴宝卷",
        "illusion_scroll": "幻术卷",
        "shape_shift_scroll": "化形卷",
        "void_palace_intel_scroll": "虚天殿情报卷",
        "formation_scroll_mid": "中品阵法卷",
        "dayan_art_scroll": "大衍术卷",
        "silver_giant_sword_blueprint": "银巨剑图纸",
        "sky_tiger_beast_contract": "天虎灵兽契",
        "spirit_pet_contract": "灵宠契",
        "beast_contract_token": "御兽令",
        "beast_puppet_core": "兽傀核",
        "beast_core_t4": "四阶妖核",
        "beast_king_token": "兽王令",
        "outer_disciple_token": "外门令牌",
        "mortal_quest_token": "凡尘任务令",
        "bounty_token": "悬赏令",
        "bounty_tag": "悬赏签",
        "array_break_token": "破阵令",
        "auction_lot_token": "拍卖标的令",
        "barbarian_survival_token": "蛮荒生存令",
        "barbarian_trade_pact": "蛮荒商约",
        "black_market_credit": "黑市信誉券",
        "black_market_vip": "黑市贵宾令",
        "demonic_karma_token": "魔业令",
        "island_permit": "岛主通行令",
        "island_teleport_token": "海岛传送令",
        "island_tax_rate_tag": "岛税签",
        "mulan_pass_or_truce_token": "木兰通关/休战令",
        "truce_token": "休战令",
        "seal_reinforcement_token": "封印加固令",
        "sect_token": "宗门令牌",
        "sea_pass": "出海令",
        "spirit_pill_voucher": "灵丹券",
        "spirit_realm_gate_voucher": "灵界关隘券",
        "tax_waiver_token": "免税令",
        "teleport_array_license": "传送阵执照",
        "war_campaign_token": "战事令",
        "war_scout_token": "斥候令",
        "yuling_contact_token": "御灵联络令",
        "barbarian_king_token_fox": "狐王令",
        "barbarian_king_token_pine": "松王令",
        "barbarian_king_token_roc": "鹏王令",
        "barbarian_king_token_snake": "蛇王令",
        "barbarian_king_token_tiger": "虎王令",
        "barbarian_king_token_turtle": "龟王令",
        "mulan_spoil": "木兰战利",
        "mulan_wind_spirit_art_page": "木兰风灵术页",
        "tianmo_body_art_page": "天魔炼体术页",
        "yin_luo_ghost_art_page": "阴罗鬼术页",
        "huadao_blade_intent_page": "化刀刀意页",
        "inverse_star_art_page": "逆星术页",
        "inverse_star_cipher": "逆星密文",
        "dajin_clan_ancestor_art_page": "大金世家祖传术页",
        "demonic_manual_low": "下品魔功手册",
        "realm_entry_blood": "秘境入门血契",
        "holy_feather_black_market": "圣羽黑市货",
        "poison_sac_bulk": "毒囊捆",
        "pearl_raw_bulk": "生珠捆",
        "refine_material_mid": "中品炼器材",
        "refinement_material_high": "上品炼器材",
        "talisman_skill_xp_tag": "符箓熟练签",
    }
    if rid in special:
        return special[rid]
    return rid


def make_icon(name: str, cat: str, size: int = 64) -> Image.Image:
    h = hashlib.md5(name.encode()).hexdigest()
    base = (80 + int(h[0:2], 16) % 160, 80 + int(h[2:4], 16) % 160, 80 + int(h[4:6], 16) % 160, 255)
    im = Image.new("RGBA", (size, size), (base[0] // 2, base[1] // 2, base[2] // 2, 255))
    d = ImageDraw.Draw(im)
    d.rectangle([1, 1, size - 2, size - 2], outline=(255, 240, 200, 220), width=2)
    if cat == "pill":
        d.ellipse([size * 0.22, size * 0.22, size * 0.78, size * 0.78], fill=base, outline=(255, 255, 255, 200))
    elif cat == "talisman":
        d.rectangle([size * 0.3, size * 0.15, size * 0.7, size * 0.85], fill=(250, 240, 210, 255), outline=(120, 40, 40, 255))
    elif cat == "manual":
        d.rectangle([size * 0.25, size * 0.15, size * 0.75, size * 0.85], fill=(240, 230, 200, 255), outline=(90, 50, 20, 255))
    elif cat == "currency":
        d.ellipse([size * 0.2, size * 0.2, size * 0.8, size * 0.8], fill=base, outline=(255, 255, 255, 200))
        d.ellipse([size * 0.38, size * 0.38, size * 0.62, size * 0.62], fill=(20, 20, 20, 180))
    else:
        d.rounded_rectangle([size * 0.2, size * 0.2, size * 0.8, size * 0.8], radius=8, fill=base, outline=(255, 255, 255, 180))
    try:
        font = ImageFont.load_default()
    except Exception:
        font = None
    label = "".join(ch for ch in name if ch.isalnum())[:2].upper() or "SI"
    d.text((size * 0.36, size * 0.4), label[:2], fill=(255, 255, 255, 230), font=font)
    return im


def title_en(rid: str) -> str:
    return " ".join(w.capitalize() for w in rid.split("_"))


def upsert_idmap(idmap: dict, rid: str, note: str, source_files: list[str], category: str) -> None:
    by = {e.get("source_id"): e for e in idmap["entries"] if isinstance(e, dict)}
    if rid in by:
        e = by[rid]
        e["canonical_type"] = "item"
        e["canonical_id"] = f"seeking_immortals:{rid}"
        e["status"] = "implemented"
        e["note"] = note
    else:
        idmap["entries"].append(
            {
                "source_category": category,
                "source_id": rid,
                "source_files": source_files,
                "canonical_type": "item",
                "canonical_id": f"seeking_immortals:{rid}",
                "status": "implemented",
                "note": note,
            }
        )


def main() -> None:
    reg = load_registered()
    idmap = json.loads(IDMAP.read_text(encoding="utf-8"))
    aliases: dict[str, str] = {}
    for e in idmap["entries"]:
        sid, cid = e.get("source_id"), e.get("canonical_id") or ""
        if isinstance(sid, str) and isinstance(cid, str) and cid.startswith("seeking_immortals:"):
            aliases[sid] = cid.split(":", 1)[1]

    # apply soft aliases first
    alias_n = 0
    for src, dst in SOFT_ALIASES.items():
        if dst not in reg and not covered(dst, reg, aliases):
            raise SystemExit(f"alias target missing: {src}->{dst}")
        upsert_idmap(
            idmap,
            src,
            f"Wave 0.1.451 alias {src} -> {dst}",
            ["quest_hooks.json", "beast_bestiary.json", "craft_daily_loops.json", "items_by_region.json"],
            "material",
        )
        # force canonical to target
        for e in idmap["entries"]:
            if e.get("source_id") == src:
                e["canonical_id"] = f"seeking_immortals:{dst}"
                e["status"] = "implemented"
        aliases[src] = dst
        alias_n += 1

    missing = collect_missing(reg, aliases)
    print("to add", len(missing))

    bulk = json.loads(BULK.read_text(encoding="utf-8"))
    existing = {it["id"] for it in bulk.get("items", [])}
    zh = json.loads(ZH.read_text(encoding="utf-8"))
    en = json.loads(EN.read_text(encoding="utf-8"))

    added = []
    tex_n = 0
    for rid in missing:
        if rid in existing or rid in reg:
            upsert_idmap(idmap, rid, "Wave 0.1.451 already present verified.", ["quest_beast_reward_sources"], "material")
            continue
        cat, rar = cat_for(rid)
        bulk["items"].append(
            {
                "id": rid,
                "category": cat,
                "rarity": rar,
                "description": f"Catalog carrier for {rid}",
            }
        )
        existing.add(rid)
        reg.add(rid)
        added.append(rid)

        png = ITEM_TEX / f"{rid}.png"
        if not png.exists():
            png.parent.mkdir(parents=True, exist_ok=True)
            make_icon(rid, cat).save(png, format="PNG")
            tex_n += 1
        (ITEM_MODELS / f"{rid}.json").write_text(
            json.dumps(
                {
                    "parent": "minecraft:item/generated",
                    "textures": {"layer0": f"seeking_immortals:item/{rid}"},
                },
                indent=2,
            )
            + "\n",
            encoding="utf-8",
        )
        zh[f"item.seeking_immortals.{rid}"] = zh_name(rid)
        en[f"item.seeking_immortals.{rid}"] = title_en(rid)
        upsert_idmap(
            idmap,
            rid,
            "Wave 0.1.451 remaining quest/beast/reward physical carrier.",
            ["quest_hooks.json", "beast_bestiary.json", "barbarian_demon_kings.json", "craft_daily_loops.json"],
            cat if cat != "equipment" else "material",
        )

    # keep deferred effects marked deferred/future_loader
    for rid in EXCLUDE:
        for e in idmap["entries"]:
            if e.get("source_id") == rid:
                e["status"] = "deferred"
                e["canonical_type"] = "future_loader"
                e["canonical_id"] = rid
                e["note"] = "Non-item effect/result token; no physical carrier by design (0.1.451)."

    BULK.write_text(json.dumps(bulk, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    ZH.write_text(json.dumps(zh, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    EN.write_text(json.dumps(en, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    IDMAP.write_text(json.dumps(idmap, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    gp = Path("gradle.properties")
    raw = gp.read_text(encoding="utf-8")
    if "mod_version=0.1.450" in raw:
        gp.write_text(raw.replace("mod_version=0.1.450", "mod_version=0.1.451"), encoding="utf-8")
    elif "mod_version=0.1.451" not in raw:
        raise SystemExit("unexpected mod_version")

    print("aliases", alias_n)
    print("added", len(added))
    print("bulk total", len(bulk["items"]))
    print("textures", tex_n)
    print("version", [ln for ln in gp.read_text(encoding="utf-8").splitlines() if "mod_version" in ln][0])
    print("sample", added[:20])


if __name__ == "__main__":
    main()
