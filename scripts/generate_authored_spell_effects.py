#!/usr/bin/env python3
"""Compile every authored spell description into a distinct runtime effect plan.

This is intentionally independent from the historical 344-profile generator.
It reads the complete technique corpus, every incremental novel extraction, the
raw v118-v122 look cards, and every Java source file.  The result retains the
author prose while projecting it into bounded renderer and gameplay semantics.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
from collections import Counter
from pathlib import Path
from typing import Any, Iterable


ROOT = Path(__file__).resolve().parents[1]
MATERIAL_DIR = ROOT / "文本材料" / "data"
TECHNIQUE_DIR = MATERIAL_DIR / "techniques"
JAVA_DIR = ROOT / "src/main/java/com/xunxian/seekingimmortals"
OUTPUT = ROOT / "src/main/resources/data/seeking_immortals/visual/authored_spell_effects.json"

VISUAL_LAYERS = tuple(MATERIAL_DIR / f"item_descriptions_v{version}.json" for version in range(118, 123))
RUNTIME_FAMILIES = (
    "FIRE", "WATER", "METAL", "WOOD", "EARTH", "WIND", "ICE", "THUNDER",
    "LIGHT", "DARK", "SOUL", "BLOOD", "VOID", "ILLUSION", "NEUTRAL",
)
RUNTIME_MOTIFS = (
    "GENERIC", "PROJECTILE", "BLADE", "SHIELD", "DOMAIN", "TELEPORT", "SUMMON",
    "WALL", "CHAIN", "CHANNEL", "RAIN", "HEAL", "CLEANSE", "SEAL", "FORMATION",
    "BUDDHIST", "CONFUCIAN", "DAO", "GHOST", "TALISMAN", "ILLUSION", "MARTIAL",
)

FAMILY_KEYWORDS = {
    "THUNDER": ("雷", "电", "霆", "霹雳", "紫电"),
    "VOID": ("虚空", "空间", "空间裂", "界面", "裂隙", "挪移"),
    "BLOOD": ("精血", "血云", "血光", "血焰", "血影", "血色", "血魂", "煞"),
    "SOUL": ("神魂", "魂魄", "元婴", "神念", "神识", "鬼影", "鬼物", "阴魂", "魂"),
    "ILLUSION": ("幻境", "幻象", "幻影", "梦境", "迷幻", "蜃"),
    "DARK": ("魔气", "阴气", "黑气", "黑光", "漆黑", "幽暗", "玄阴", "灰黑"),
    "LIGHT": ("佛光", "圣光", "乳白", "白光", "金身", "功德", "光明"),
    "ICE": ("玄冰", "冰", "霜", "寒", "雪", "冻结"),
    "FIRE": ("火焰", "火光", "火球", "火龙", "烈焰", "炎", "焰", "熔岩"),
    "WATER": ("水幕", "水浪", "海潮", "波涛", "雨", "水", "海", "潮", "浪"),
    "WOOD": ("藤蔓", "草木", "灵木", "青木", "木", "藤", "花", "林"),
    "METAL": ("剑光", "剑虹", "飞剑", "金刃", "庚金", "刀光", "金色", "金鳞", "剑", "刀"),
    "EARTH": ("山岳", "岩石", "巨石", "土石", "地刺", "大地", "尘土", "土", "山"),
    "WIND": ("狂风", "旋风", "风刃", "指风", "云气", "白云", "风", "云"),
}

MOTIF_KEYWORDS = {
    "BUDDHIST": ("佛", "金刚", "梵", "禅", "罗汉"),
    "CONFUCIAN": ("儒", "浩然", "文气", "墨", "经卷"),
    "TALISMAN": ("符箓", "符纸", "符箭", "符文", "符阵"),
    "FORMATION": ("剑阵", "法阵", "阵法", "阵图", "禁制", "阵"),
    "TELEPORT": ("瞬移", "挪移", "遁", "凭空出现", "消失不见", "融入虚空"),
    "SUMMON": ("召唤", "法相", "化身", "傀儡", "鬼影", "真灵", "灵兽", "骷髅"),
    "SHIELD": ("护罩", "光幕", "护盾", "屏障", "护体", "甲胄", "铠甲"),
    "WALL": ("冰墙", "土墙", "风墙", "石壁", "壁垒", "牢笼"),
    "CHAIN": ("锁链", "连锁", "电网", "丝线", "光丝", "藤蔓", "缠绕"),
    "RAIN": ("暴雨", "箭雨", "剑雨", "火雨", "雷雨", "漫天", "密密麻麻"),
    "HEAL": ("疗伤", "治愈", "恢复", "回春", "再生"),
    "CLEANSE": ("净化", "驱散", "解毒", "祛除"),
    "SEAL": ("封印", "镇压", "禁锢", "定身", "束缚"),
    "ILLUSION": ("幻境", "幻象", "幻影", "梦境", "迷幻"),
    "GHOST": ("鬼爪", "鬼影", "鬼物", "阴魂", "骷髅", "尸"),
    "BLADE": ("剑光", "剑虹", "飞剑", "刀光", "剑影", "斩", "刃"),
    "PROJECTILE": ("飞射", "射出", "箭", "弹", "珠", "针", "锥", "光球"),
    "CHANNEL": ("持续", "灌注", "催动", "侵入", "牵引", "吸取"),
    "DOMAIN": ("领域", "天象", "世界", "云团", "血云", "漩涡", "覆盖"),
    "MARTIAL": ("拳影", "掌影", "拳", "掌", "爪", "肉身", "鳞片", "吼"),
    "DAO": ("道法", "法则", "真言", "仙术", "玄天"),
}

SCHOOL_MOTIFS = {
    "body": "MARTIAL",
    "buddhist": "BUDDHIST",
    "confucian": "CONFUCIAN",
    "dao": "DAO",
    "demon_path": "GHOST",
    "demonic": "GHOST",
    "divine_sense": "CHANNEL",
    "formation": "FORMATION",
    "ghost": "GHOST",
    "illusion": "ILLUSION",
    "movement": "TELEPORT",
    "puppet": "SUMMON",
    "recovery": "HEAL",
    "sword": "BLADE",
    "talisman": "TALISMAN",
    "xuan_yin": "GHOST",
}

FAMILY_STYLE = {
    "FIRE": ("fire_ember", "none", "赤金火焰"),
    "WATER": ("water_mist", "none", "澄蓝水光"),
    "METAL": ("metal_spark", "sword_thin", "银金锋芒"),
    "WOOD": ("wood_pollen", "none", "青绿生机"),
    "EARTH": ("earth_dust", "heavy_weapon", "土黄岩尘"),
    "WIND": ("qi_soft", "movement_wind", "淡青风痕"),
    "ICE": ("water_mist", "none", "青白寒雾"),
    "THUNDER": ("thunder_arc", "thunder_jagged", "紫金雷光"),
    "LIGHT": ("heal_motes", "none", "乳白金光"),
    "DARK": ("yin_smoke", "soul_afterimage", "幽紫黑烟"),
    "SOUL": ("soul_wisps", "soul_afterimage", "幽蓝魂光"),
    "BLOOD": ("blood_mist", "blood_ribbon", "暗红血雾"),
    "VOID": ("space_glitch", "none", "幽紫虚空裂光"),
    "ILLUSION": ("soul_wisps", "soul_afterimage", "幻紫重影"),
    "NEUTRAL": ("qi_soft", "none", "青白灵光"),
}

EFFECT_TYPES = {
    "projectile", "beam", "cone", "chain", "aoe", "aoe_control", "aoe_dot", "field",
    "domain", "wall", "trap", "buff_zone", "debuff", "dot", "drain", "control",
    "buff_self", "buff", "shield", "transform", "heal", "heal_spirit", "cleanse",
    "movement", "dash", "escape", "teleport_short", "melee", "strike", "ultimate",
    "secret_art", "soul_attack", "summon", "summon_field", "talisman_consume", "utility",
    "utility_combat", "scout", "scan", "inspect", "command", "craft_gate",
}

OPERATIONS = {
    "ATTACK", "DEFEND", "RESTORE", "RESTORE_SPIRIT", "CLEANSE", "MOVE", "SUMMON",
    "COMMAND", "DETECT", "CONCEAL", "TRANSFORM", "TERRAIN", "SEAL", "DRAIN", "CRAFT",
    "CULTIVATE", "COMMUNICATE",
}

DELIVERIES = {
    "PROJECTILE", "BEAM", "CONE", "CHAIN", "AREA", "FIELD", "SELF", "MOVEMENT",
    "SUMMON", "COMMAND", "TARGETED", "CONTACT",
}


def read_json(path: Path) -> dict[str, Any]:
    payload = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(payload, dict):
        raise ValueError(f"JSON object expected: {path}")
    return payload


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def relative(path: Path) -> str:
    return str(path.relative_to(ROOT)).replace("\\", "/")


def clean(value: Any) -> str:
    return "" if value is None else str(value).strip()


def norm(value: Any) -> str:
    return re.sub(r"_+", "_", clean(value).lower().replace("-", "_").replace(" ", "_"))


def flatten(value: Any) -> list[str]:
    if value is None:
        return []
    if isinstance(value, dict):
        result: list[str] = []
        for key in sorted(value):
            result.extend(flatten(value[key]))
        return result
    if isinstance(value, list):
        result: list[str] = []
        for item in value:
            result.extend(flatten(item))
        return result
    text = clean(value)
    return [text] if text else []


def unique(values: Iterable[str]) -> list[str]:
    result: list[str] = []
    seen: set[str] = set()
    for value in values:
        text = clean(value)
        if text and text not in seen:
            seen.add(text)
            result.append(text)
    return result


def score_keywords(text: str, table: dict[str, tuple[str, ...]], fallback: str) -> str:
    scores: dict[str, int] = {}
    for kind, keywords in table.items():
        score = sum(text.count(keyword) * max(1, len(keyword)) for keyword in keywords)
        if score:
            scores[kind] = score
    if not scores:
        return fallback
    order = {key: index for index, key in enumerate(table)}
    return max(scores, key=lambda key: (scores[key], -order[key]))


def infer_family(explicit: str, text: str) -> str:
    direct = norm(explicit)
    aliases = {
        "fire": "FIRE", "water": "WATER", "metal": "METAL", "wood": "WOOD",
        "earth": "EARTH", "wind": "WIND", "ice": "ICE", "thunder": "THUNDER",
        "lightning": "THUNDER", "light": "LIGHT", "yang": "LIGHT", "dark": "DARK",
        "yin": "DARK", "soul": "SOUL", "ghost": "SOUL", "blood": "BLOOD",
        "demonic": "BLOOD", "void": "VOID", "illusion": "ILLUSION", "neutral": "NEUTRAL",
        "火": "FIRE", "水": "WATER", "金": "METAL", "木": "WOOD", "土": "EARTH",
        "风": "WIND", "冰": "ICE", "雷": "THUNDER", "光": "LIGHT", "暗": "DARK",
        "魂": "SOUL", "血": "BLOOD", "空间": "VOID", "幻": "ILLUSION",
    }
    for token, family in aliases.items():
        if token in direct:
            return family
    return score_keywords(text, FAMILY_KEYWORDS, "NEUTRAL")


def infer_motif(effect_type: str, text: str, school: str = "") -> str:
    school_motif = SCHOOL_MOTIFS.get(norm(school))
    if school_motif:
        if effect_type in {"heal", "heal_spirit", "cleanse"}:
            return "HEAL" if effect_type != "cleanse" else "CLEANSE"
        if effect_type == "shield":
            return "SHIELD"
        return school_motif
    motif = score_keywords(text, MOTIF_KEYWORDS, "GENERIC")
    if motif != "GENERIC":
        return motif
    return {
        "projectile": "PROJECTILE", "beam": "PROJECTILE", "cone": "PROJECTILE",
        "chain": "CHAIN", "field": "FORMATION", "domain": "DOMAIN", "wall": "WALL",
        "buff_zone": "DOMAIN", "shield": "SHIELD", "heal": "HEAL", "cleanse": "CLEANSE",
        "movement": "TELEPORT", "dash": "TELEPORT", "escape": "TELEPORT",
        "teleport_short": "TELEPORT", "summon": "SUMMON", "summon_field": "SUMMON",
        "talisman_consume": "TALISMAN", "melee": "MARTIAL", "strike": "MARTIAL",
        "control": "SEAL", "trap": "SEAL", "soul_attack": "GHOST",
    }.get(effect_type, "GENERIC")


def infer_effect_type(explicit: str, motif: str, text: str, name: str = "") -> str:
    value = norm(explicit)
    if value in EFFECT_TYPES:
        return value
    name_attack = any(token in name for token in (
        "斩", "杀", "灭", "轰", "击", "刺", "刃", "剑", "爪", "掌", "拳", "矛", "箭", "针",
        "火球", "火花", "雷球", "洞穿", "破", "碎", "战", "猎杀", "对轰", "镇压", "碾压", "风柱",
    ))

    def combat_type() -> str:
        motif_type = {
            "GHOST": "soul_attack", "MARTIAL": "melee", "FORMATION": "field",
            "DOMAIN": "domain", "WALL": "wall", "CHAIN": "chain", "RAIN": "aoe",
        }.get(motif)
        if motif_type:
            return motif_type
        projectile_language = motif in {"PROJECTILE", "BLADE"} or any(token in text for token in (
            "飞射", "激射", "射出", "射向", "弹", "箭", "针", "锥", "矛", "刺", "飞芒", "火星",
        ))
        return "projectile" if projectile_language else "utility_combat"

    if not name_attack and any(token in name for token in ("传音", "传讯", "密语", "传声", "通讯", "传话")):
        return "utility"
    if not name_attack and any(token in name for token in ("敛气", "隐匿", "匿身", "隐形", "潜行", "易容", "变容")):
        return "utility"
    if not name_attack and any(token in name for token in ("天眼", "灵眼", "探查", "探测", "侦测", "鉴定", "追踪", "搜魂")):
        return "scan"
    if any(token in name for token in ("炼器", "炼丹", "锻造", "制符", "稳炉")):
        return "craft_gate"
    if any(token in name for token in ("流沙", "化土为沙", "冻土")):
        return "utility"
    if any(token in name for token in ("吸灵", "吸法", "吸血", "噬灵", "吞元", "吞魂")):
        return "drain"
    if any(token in name for token in ("变身", "变化", "化形", "化身", "真身")):
        return "transform"
    if not name_attack and any(token in name for token in ("升空", "飞行", "瞬移", "挪移", "遁术", "遁法", "步法", "逃遁")):
        return "movement"
    if not name_attack and any(token in name for token in ("召唤", "唤灵", "唤兽", "豆兵", "傀儡术", "撒豆成兵")):
        return "summon"
    if not name_attack and any(token in name for token in ("护体", "护身", "灵甲", "护盾", "护罩", "金刚体")):
        return "shield"
    if not name_attack and any(token in name for token in (
            "封印", "禁制", "禁神", "定身", "镇封", "束缚", "迷魂", "控神", "摄神", "梦魇")):
        return "control"
    if name_attack:
        return combat_type()
    if any(token in text for token in ("传音", "传讯", "密语", "传声", "通讯", "传话")):
        return "utility"
    if any(token in text for token in ("敛气", "隐匿", "匿身", "隐形", "潜行", "遮蔽气息", "掩饰修为", "易容", "变容")):
        return "utility"
    if any(token in text for token in ("探查", "探测", "侦测", "鉴定", "辨影", "识破", "追踪", "观骨", "搜魂", "天眼术", "灵眼术")):
        return "scan"
    if any(token in text for token in ("炼器", "炼丹", "锻造", "制符", "稳炉", "炼制法宝")):
        return "craft_gate"
    if any(token in text for token in ("流沙", "化土为沙", "凝结成冰", "地形", "开山", "裂地", "冻土")):
        return "utility"
    if any(token in text for token in ("吸取灵力", "吸取法力", "吞噬法力", "抽取精血", "吸血", "噬灵")):
        return "drain"
    if any(token in text for token in ("变身", "变化之术", "化形", "变化形态", "显露真身")):
        return "transform"
    if any(token in text for token in ("瞬移", "挪移", "飞遁", "逃遁", "腾空", "升空", "御风飞行")):
        return "movement"
    if any(token in text for token in ("召唤", "唤出", "凝出傀儡", "撒豆成兵", "召来灵兽")):
        return "summon"
    if any(token in text for token in ("疗伤", "治愈", "恢复伤势", "回春")):
        return "heal"
    if any(token in text for token in ("净化", "驱散", "解毒", "祛除")):
        return "cleanse"
    if motif == "SHIELD":
        return "shield"
    if motif == "TELEPORT":
        return "teleport_short"
    if motif == "SUMMON":
        return "summon"
    if motif == "WALL":
        return "wall"
    if motif in {"CHAIN", "SEAL"}:
        return "control"
    if motif == "DOMAIN":
        return "domain"
    if motif == "FORMATION":
        return "field"
    if motif in {"BLADE", "PROJECTILE", "TALISMAN"}:
        if any(token in text for token in ("光柱", "光束", "剑光", "剑虹", "射线")):
            return "beam"
        return "projectile"
    if motif in {"MARTIAL", "GHOST"}:
        return "melee" if motif == "MARTIAL" else "soul_attack"
    if any(token in text for token in ("攻敌", "攻击", "突袭", "轰击", "击伤", "斩杀", "杀伤",
                                        "刺向", "射向", "扑向", "轰向", "砸向", "迎头击")):
        return combat_type()
    if motif == "RAIN" or any(token in text for token in ("漫天", "大片", "四周", "周围", "暴雨般")):
        return "aoe"
    if any(token in text for token in ("侵入", "反噬", "诅咒", "削弱", "麻痹")):
        return "debuff"
    return "utility_combat" if any(token in text for token in ("击", "爆", "轰", "杀", "伤")) else "utility"


def target_for(effect_type: str, text: str) -> str:
    if effect_type in {"buff_self", "buff", "shield", "transform", "heal", "heal_spirit", "cleanse",
                       "movement", "dash", "escape", "teleport_short", "scan", "inspect"}:
        return "self"
    if effect_type in {"aoe", "aoe_control", "aoe_dot", "field", "domain", "wall", "trap",
                       "buff_zone", "summon_field"} or any(token in text for token in ("四周", "周围", "漫天")):
        return "area"
    return "single"


def operation_for(effect_type: str, text: str, name: str = "") -> str:
    direct = {
        "heal": "RESTORE", "heal_spirit": "RESTORE_SPIRIT", "cleanse": "CLEANSE",
        "movement": "MOVE", "dash": "MOVE", "escape": "MOVE", "teleport_short": "MOVE",
        "summon": "SUMMON", "summon_field": "SUMMON", "command": "COMMAND",
        "buff_self": "DEFEND", "buff": "DEFEND", "shield": "DEFEND", "buff_zone": "DEFEND",
        "transform": "TRANSFORM", "scan": "DETECT", "scout": "DETECT", "inspect": "DETECT",
        "drain": "DRAIN", "craft_gate": "CRAFT",
        "control": "SEAL", "debuff": "SEAL", "dot": "SEAL", "trap": "SEAL",
    }
    if effect_type in direct:
        return direct[effect_type]
    if effect_type != "utility":
        return "ATTACK"
    name_operations = (
        ("COMMUNICATE", ("传音", "传讯", "密语", "传声", "通讯", "传话")),
        ("CONCEAL", ("敛气", "隐匿", "匿身", "隐形", "潜行", "遮蔽", "易容", "变容")),
        ("TERRAIN", ("流沙", "化土为沙", "冻土")),
        ("SEAL", ("封印", "禁制", "禁神", "定身", "镇封", "束缚", "迷魂", "控神", "摄神", "梦魇")),
        ("DEFEND", ("护体", "护身", "灵甲", "护盾", "护罩")),
    )
    for operation, keywords in name_operations:
        if any(keyword in name for keyword in keywords):
            return operation
    keyword_operations = (
        ("COMMUNICATE", ("传音", "传讯", "密语", "传声", "通讯", "传话")),
        ("CONCEAL", ("敛气", "隐匿", "匿身", "隐形", "潜行", "遮蔽", "掩饰修为", "易容", "变容")),
        ("DETECT", ("天眼", "灵目", "探查", "探测", "侦测", "鉴定", "辨影", "识破", "感应", "追踪", "观骨", "搜魂")),
        ("TERRAIN", ("流沙", "化土为沙", "凝结成冰", "冰冻", "地形", "开山", "裂地", "冻土")),
        ("CRAFT", ("炼器", "炼丹", "锻造", "制符", "炼制", "稳炉", "炉火")),
        ("SEAL", ("誓言", "契约", "禁制", "权限", "封印", "锁真元", "迷魂", "震慑")),
        ("DEFEND", ("护", "灵甲", "护体", "防御", "抗", "解蔽", "除奴痕")),
    )
    for operation, keywords in keyword_operations:
        if any(keyword in text for keyword in keywords):
            return operation
    return "CULTIVATE"


def delivery_for(effect_type: str, target: str) -> str:
    if effect_type in {"projectile", "talisman_consume"}:
        return "PROJECTILE"
    if effect_type == "beam":
        return "BEAM"
    if effect_type == "cone":
        return "CONE"
    if effect_type == "chain":
        return "CHAIN"
    if effect_type in {"field", "domain", "wall", "trap", "buff_zone", "summon_field"}:
        return "FIELD"
    if effect_type in {"movement", "dash", "escape", "teleport_short"}:
        return "MOVEMENT"
    if effect_type == "summon":
        return "SUMMON"
    if effect_type == "command":
        return "COMMAND"
    if target == "area":
        return "AREA"
    if target == "self":
        return "SELF"
    return "CONTACT" if effect_type in {"melee", "strike"} else "TARGETED"


def summon_archetype_for(text: str) -> str:
    if any(token in text for token in ("傀儡", "机关", "甲士", "力士", "豆兵", "人偶")):
        return "puppet"
    if any(token in text for token in ("鬼", "魂", "尸", "骷髅", "元婴", "阴兵", "魔子")):
        return "ghost"
    if any(token in text for token in ("灵兽", "妖兽", "火鸟", "飞禽", "蛟", "龙", "蛇", "狼", "虎", "猿", "猴")):
        return "beast"
    return "generic"


def terrain_mode_for(text: str) -> str:
    for mode, keywords in (
            ("sand", ("流沙", "沙漠", "化土为沙")),
            ("ice", ("冰冻", "凝冰", "冰川", "冻土", "寒冰")),
            ("fire", ("火海", "熔岩", "地火", "焚烧")),
            ("vine", ("藤蔓", "草木", "树林", "灵木")),
            ("rock", ("土墙", "石壁", "山岳", "岩石", "地刺")),
            ("water", ("水域", "海浪", "洪水", "水幕")),
            ("wind", ("飓风", "风墙", "龙卷")),
    ):
        if any(keyword in text for keyword in keywords):
            return mode
    return "none"


def mechanics_for(effect_type: str, target: str, shape: str, scale: int, text: str,
                  operation: str = "") -> dict[str, Any]:
    operation = operation or operation_for(effect_type, text)
    delivery = delivery_for(effect_type, target)
    projectile_count = 1
    if shape in {"projectile_swarm", "sword_rain", "falling_barrage", "fist_barrage"}:
        projectile_count = min(12, 4 + scale * 2)
    elif shape in {"serpent_dragon", "giant_claw"}:
        projectile_count = min(5, 1 + scale)
    max_targets = 1
    if delivery in {"AREA", "FIELD", "CONE"}:
        max_targets = min(32, 8 + scale * 6)
    elif delivery == "CHAIN":
        max_targets = min(8, 3 + scale)
    duration_ticks = min(400, 80 + scale * 50)
    if delivery == "FIELD" or operation in {"DEFEND", "TRANSFORM", "CONCEAL", "CULTIVATE"}:
        duration_ticks = min(600, 140 + scale * 70)
    result = {
        "operation": operation,
        "delivery": delivery,
        "duration_ticks": duration_ticks,
        "projectile_count": projectile_count,
        "max_targets": max_targets,
        "summon_archetype": summon_archetype_for(text),
        "terrain_mode": terrain_mode_for(text),
    }
    if operation not in OPERATIONS or delivery not in DELIVERIES:
        raise ValueError(f"unsupported mechanics projection: {operation}/{delivery}")
    return result


def scale_for(text: str) -> int:
    if any(token in text for token in ("千丈", "万丈", "遮天", "天地", "整座", "山岳般", "城池般")):
        return 4
    if any(token in text for token in ("百丈", "房屋般", "巨大", "硕大", "漫天", "狂风暴雨")):
        return 3
    if any(token in text for token in ("数十丈", "大片", "密密麻麻", "无数", "漩涡", "云团")):
        return 2
    if any(token in text for token in ("数道", "一团", "一片", "丈许")):
        return 1
    return 0


def shape_for(motif: str, text: str, operation: str) -> str:
    if operation == "COMMUNICATE":
        return "sound_wave"
    if operation == "CONCEAL":
        return "mist_veil"
    if operation == "DETECT":
        return "eye_gaze"
    if motif == "FORMATION" and any(token in text for token in ("法阵", "阵法", "符文", "阵图")):
        return "rune_orbit"
    candidates = (
        ("giant_claw", ("巨爪", "鬼爪", "火焰鬼爪")),
        ("spatial_rift", ("空间裂缝", "空间裂口", "虚空裂缝", "撕裂空间", "撕裂虚空", "破碎虚空", "遁空")),
        ("ice_prison", ("冰牢", "冰狱", "冰封", "冰墙", "冰峰", "玄冰牢", "寒冰囚")),
        ("blood_sea", ("血海", "血河", "血池", "血浪", "血潮")),
        ("tree_avatar", ("巨树", "古树", "树影", "树根", "藤蔓", "木灵法相")),
        ("flame_bird", ("火鸟", "炎鸟", "朱雀", "凤凰", "火凤", "金乌")),
        ("beast_phantom", ("兽影", "虎影", "巨猿", "魔猿", "麒麟", "玄武", "白虎")),
        ("insect_swarm", ("虫云", "虫群", "蜂群", "蚁群", "飞蝎", "噬金虫")),
        ("lightning_storm", ("雷海", "雷云", "千雷", "雷柱", "雷暴", "雷雨", "雷霆万钧", "轰顶")),
        ("tidal_wave", ("巨浪", "海浪", "浪潮", "海潮", "洪水", "水幕", "水墙", "海啸")),
        ("mountain_meteor", ("山岳", "山峰", "巨山", "山影", "陨石", "流星", "坠星", "泰山压")),
        ("giant_hand", ("巨手", "大手", "巨掌", "佛掌", "血掌", "金色手掌", "擎天手")),
        ("eye_gaze", ("法目", "灵目", "天眼", "灵眼", "竖目", "眼眸", "瞳孔", "目光", "双目")),
        ("sound_wave", ("梵音", "禅音", "魔啸", "长啸", "怒吼", "咆哮", "钟鸣", "铃声", "传音", "尖鸣")),
        ("lotus_mandala", ("莲花", "金莲", "青莲", "血莲", "莲台", "莲瓣")),
        ("mirror_disc", ("宝镜", "古镜", "镜光", "镜面", "水镜", "明镜", "镜影")),
        ("scripture_glyph", ("经文", "书卷", "典籍", "墨字", "金文", "古字", "文字", "符字", "真言")),
        ("magnetic_field", ("元磁", "磁力", "磁光", "磁场", "两极")),
        ("wheel_disc", ("宝轮", "法轮", "光轮", "圆盘", "轮盘", "日轮", "月轮")),
        ("wing_fan", ("双翼", "羽翼", "翅膀", "羽翅", "风雷翅", "羽扇")),
        ("spear_spike", ("金针", "银针", "灵针", "飞针", "长矛", "冰矛", "冰锥", "地刺", "骨刺")),
        ("orb_projectile", ("光球", "火球", "雷球", "水球", "圆球", "灵珠", "宝珠", "圆珠", "珠光")),
        ("mist_veil", ("雾气", "迷雾", "云雾", "烟雾", "雾幕", "烟幕", "霞光", "隐踪", "隐匿")),
        ("fist_barrage", ("拳影", "掌影")),
        ("sword_rain", ("剑雨", "剑阵", "密密麻麻剑")),
        ("serpent_dragon", ("火龙", "水龙", "火蛇", "蛟龙", "巨蟒", "毒蛇般")),
        ("cloud_vortex", ("漩涡", "云团", "血云", "黑云")),
        ("rune_orbit", ("符文", "符箓", "符印")),
        ("chain_net", ("锁链", "电网", "光丝", "丝线")),
        ("beam_lance", ("光柱", "光束", "剑虹", "剑光")),
        ("projectile_swarm", ("飞射", "箭雨", "无数", "密密麻麻")),
        ("body_aura", ("鳞片", "金身", "铠甲", "护体")),
        ("spirit_avatar", ("法相", "鬼影", "骷髅", "化身")),
        ("ground_field", ("地面", "大地", "阵法", "领域")),
    )
    for shape, keywords in candidates:
        if any(keyword in text for keyword in keywords):
            return shape
    return {
        "PROJECTILE": "single_projectile", "BLADE": "blade_arc", "SHIELD": "body_shell",
        "DOMAIN": "sphere_field", "TELEPORT": "afterimage_path", "SUMMON": "summon_gate",
        "WALL": "barrier_plane", "CHAIN": "chain_links", "CHANNEL": "channel_stream",
        "RAIN": "falling_barrage", "HEAL": "rising_motes", "CLEANSE": "cleansing_ring",
        "SEAL": "seal_cage", "FORMATION": "array_rings", "GHOST": "spirit_avatar",
        "TALISMAN": "burning_talisman", "ILLUSION": "layered_afterimages",
        "MARTIAL": "impact_arcs",
    }.get(motif, "aura_burst")


def frame_name(index: int, count: int, text: str, telegraphed: bool) -> str:
    if any(token in text for token in ("溃散", "消散", "消失", "熄灭", "收回", "散去")):
        return "回收"
    if any(token in text for token in ("爆裂", "爆开", "击中", "洞穿", "破碎", "崩溃", "砸", "轰")):
        return "命中"
    if any(token in text for token in ("飞射", "射出", "喷涌", "抓下", "轰击", "冲出", "扑向")):
        return "出击"
    if any(token in text for token in ("凝聚", "汇聚", "形成", "浮现", "化为", "变成", "出现")):
        return "成型"
    if index == 0:
        return "大招预兆" if telegraphed else "起手"
    return "命中" if index == count - 1 else "出击"


def make_frames(visuals: list[str], telegraphed: bool) -> list[dict[str, Any]]:
    sources = visuals[:8] or ["作者资料未提供独立视觉引文，按设定语义生成起手与命中阶段。"]
    result: list[dict[str, Any]] = []
    cursor = 0
    for index, source in enumerate(sources):
        duration = max(3, min(10, 3 + len(source) // 24))
        result.append({
            "name": frame_name(index, len(sources), source, telegraphed),
            "frame": f"{cursor}-{cursor + duration - 1}",
            "vis": source,
        })
        cursor += duration
    if result[-1]["name"] not in {"命中", "回收"}:
        result.append({"name": "回收", "frame": f"{cursor}-{cursor + 3}", "vis": "余光、残焰或灵气按原路径收束消散。"})
    return result


def status_for(family: str, effect_type: str) -> tuple[str, str]:
    if effect_type in {"heal", "heal_spirit", "cleanse"}:
        return "regeneration", "absorption"
    if effect_type in {"shield", "buff", "buff_self", "transform"}:
        return "resistance", "damage_boost"
    return {
        "FIRE": ("burning", "weakness"), "WATER": ("slowness", "weakness"),
        "METAL": ("armor_break", "bleeding"), "WOOD": ("rooted", "poison"),
        "EARTH": ("slowness", "weakness"), "WIND": ("levitation", "slowness"),
        "ICE": ("frozen", "slowness"), "THUNDER": ("stunned", "weakness"),
        "LIGHT": ("glowing", "weakness"), "DARK": ("blindness", "wither"),
        "SOUL": ("confusion", "weakness"), "BLOOD": ("wither", "poison"),
        "VOID": ("levitation", "blindness"), "ILLUSION": ("confusion", "blindness"),
        "NEUTRAL": ("weakness", "slowness"),
    }[family]


def numeric_plan(effect_type: str, scale: int, explicit_cost: int, explicit_damage: float,
                 explicit_cooldown: int) -> dict[str, Any]:
    damaging = effect_type in {
        "projectile", "beam", "cone", "chain", "aoe", "aoe_control", "aoe_dot", "field",
        "domain", "wall", "trap", "debuff", "dot", "drain", "control", "melee", "strike",
        "ultimate", "secret_art", "soul_attack", "talisman_consume", "utility_combat",
    }
    damage = explicit_damage if explicit_damage > 0 else ((10.0 + scale * 7.0) if damaging else 0.0)
    radius = round(min(8.0, 0.9 + scale * 1.25 + (1.5 if effect_type in {"aoe", "field", "domain"} else 0.0)), 2)
    range_value = min(48.0, 14.0 + scale * 7.0)
    return {
        "cost": max(1, explicit_cost or (8 + scale * 9)),
        "cooldown_ticks": max(20, explicit_cooldown or (50 + scale * 35)),
        "damage_base": round(damage, 2),
        "range": round(range_value, 2),
        "radius": radius,
        "intensity": min(48, 14 + scale * 7),
    }


def collect_visual_rows() -> dict[str, list[dict[str, Any]]]:
    result: dict[str, list[dict[str, Any]]] = {}
    for version, path in zip(range(118, 123), VISUAL_LAYERS):
        for row in read_json(path).get("items", []):
            if not isinstance(row, dict):
                continue
            catalog_id = norm(row.get("catalog_id"))
            row_type = norm(row.get("type"))
            if catalog_id and ("tech" in row_type or "technique" in row_type):
                result.setdefault(catalog_id, []).append({**row, "_version": version, "_path": relative(path)})
    return result


def visual_texts(rows: list[dict[str, Any]]) -> list[str]:
    values: list[str] = []
    for row in rows:
        for key in ("appearance", "cast_vfx", "impact_vfx", "telegraph", "description"):
            values.extend(flatten(row.get(key)))
        for frame in row.get("frames", []):
            if isinstance(frame, dict):
                values.extend(flatten(frame.get("vis")))
    return unique(values)


def make_profile(entry: dict[str, Any], namespace: str, source_path: Path,
                 visuals: list[str], visual_kind: str, rows: list[dict[str, Any]] | None = None) -> dict[str, Any]:
    profile_id = norm(entry.get("id"))
    display = clean(entry.get("display") or entry.get("name"))
    setting = clean(entry.get("setting") or entry.get("description") or entry.get("summary"))
    settings = unique(flatten(entry.get("setting_descriptions")))
    effect = entry.get("effect", {}) if isinstance(entry.get("effect"), dict) else {}
    tags = unique(flatten(entry.get("tags")) + flatten(effect.get("tags")))
    explicit_type = clean(effect.get("type") or entry.get("effect_type") or entry.get("type"))
    explicit_element = clean(effect.get("element") or entry.get("element") or entry.get("attribute"))
    school = clean(entry.get("school") or source_path.stem)
    semantic_blob = " ".join(unique([display, clean(entry.get("term")), school, setting] + tags))
    blob = " ".join(unique([semantic_blob] + visuals + settings))
    family = infer_family(explicit_element, blob)
    provisional_motif = infer_motif(norm(explicit_type), semantic_blob, school)
    effect_blob = " ".join(unique([semantic_blob] + visuals))
    effect_type = infer_effect_type(explicit_type, provisional_motif, effect_blob,
                                    " ".join(unique([display, clean(entry.get("term"))])))
    motif = infer_motif(effect_type, blob, school)
    scale = scale_for(blob)
    operation = operation_for(effect_type, semantic_blob,
                              " ".join(unique([display, clean(entry.get("term"))])))
    target = target_for(effect_type, " ".join(unique([semantic_blob] + visuals)))
    if operation in {"DETECT", "CONCEAL", "CRAFT", "CULTIVATE"}:
        target = "self"
    shape_text = " ".join(unique([display, clean(entry.get("term"))] + visuals))
    if not visuals:
        shape_text = " ".join(unique([shape_text, setting]))
    shape = shape_for(motif, shape_text, operation)
    mechanics = mechanics_for(effect_type, target, shape, scale, semantic_blob, operation)
    numeric = numeric_plan(
        effect_type, scale,
        int(entry.get("spirit_cost_base", entry.get("cost", 0)) or 0),
        float(effect.get("damage_base", entry.get("damage_base", 0.0)) or 0.0),
        int(entry.get("cooldown_ticks", 0) or 0),
    )
    particle, trail, color = FAMILY_STYLE[family]
    if motif == "BLADE" and trail == "none":
        trail = "sword_thin"
    elif motif == "TALISMAN":
        trail = "talisman_ash"
    elif motif == "TELEPORT":
        trail = "movement_wind"
    telegraphed = effect_type in {"ultimate", "secret_art"} or scale >= 3 \
        or any(token in blob for token in ("掐诀", "念咒", "蓄势", "法诀", "恐怖威能", "长前摇"))
    primary_status, secondary_status = status_for(family, effect_type)
    frame_sources = visuals or unique([setting] + settings)
    frames = make_frames(frame_sources, telegraphed)
    source_versions = sorted({int(row["_version"]) for row in rows or []})
    signature_input = json.dumps({
        "id": profile_id,
        "visuals": frame_sources,
        "family": family,
        "motif": motif,
        "effect_type": effect_type,
        "shape": shape,
        "numeric": numeric,
        "mechanics": mechanics,
    }, ensure_ascii=False, sort_keys=True).encode("utf-8")
    return {
        "id": profile_id,
        "namespace": namespace,
        "qualified_id": f"{namespace}:{profile_id}",
        "display": display,
        "term": clean(entry.get("term")),
        "school": school,
        "source": clean(entry.get("source") or entry.get("source_books") or source_path.name),
        "source_file": relative(source_path),
        "source_version": int(entry.get("extract_version", 0) or 0),
        "visual_source_kind": visual_kind,
        "visual_descriptions": frame_sources,
        "setting_descriptions": settings,
        "setting": setting,
        "family": family,
        "motif": motif,
        "effect_type": effect_type,
        "target": target,
        "particle": particle,
        "trail": trail,
        "shape": shape,
        "color": color,
        "telegraphed": telegraphed,
        "radius": numeric["radius"],
        "intensity": numeric["intensity"],
        "scale_tier": scale,
        "visual_signature": hashlib.sha256(signature_input).hexdigest(),
        "frames": frames,
        "mechanics": mechanics,
        "functional": {
            "type": effect_type,
            "element": family.lower(),
            "target": target,
            "cost": numeric["cost"],
            "cooldown_ticks": numeric["cooldown_ticks"],
            "damage_base": numeric["damage_base"],
            "range": numeric["range"],
            "radius": numeric["radius"],
            "primary_status": primary_status,
            "secondary_status": secondary_status,
        },
        "tags": tags,
        "sources": {
            "primary": relative(source_path),
            **({"visual_layers": ",".join(f"v{version}" for version in source_versions)} if source_versions else {}),
        },
    }


def load_corpus(visual_rows: dict[str, list[dict[str, Any]]]) -> tuple[list[dict[str, Any]], list[Path]]:
    profiles: list[dict[str, Any]] = []
    paths = sorted(TECHNIQUE_DIR.glob("*.json"))
    for path in paths:
        payload = read_json(path)
        for entry in payload.get("techniques", []):
            if not isinstance(entry, dict) or not norm(entry.get("id")):
                continue
            rows = visual_rows.get(norm(entry.get("id")), [])
            descriptions = visual_texts(rows)
            kind = "authored_visual_stack" if descriptions else "technique_description"
            profiles.append(make_profile(entry, "corpus", path, descriptions, kind, rows))
    return profiles, paths


def version_of(path: Path) -> int:
    match = re.search(r"_v(\d+)\.json$", path.name)
    return int(match.group(1)) if match else -1


def tracked_novel_technique_paths() -> list[Path]:
    """Use committed inputs in a Git checkout so concurrent scratch files cannot stale builds."""
    try:
        result = subprocess.run(
            ["git", "-c", "core.quotePath=false", "ls-files", "--cached", "--",
             "文本材料/data/novel_curated_techniques_v*.json"],
            cwd=ROOT, check=True, capture_output=True, text=True,
        )
    except (OSError, subprocess.CalledProcessError):
        result = None
    if result is not None:
        tracked = [ROOT / line.strip() for line in result.stdout.splitlines() if line.strip()]
        if tracked:
            return sorted((path for path in tracked if path.is_file()), key=version_of)
    return sorted(MATERIAL_DIR.glob("novel_curated_techniques_v*.json"), key=version_of)


def load_novel() -> tuple[list[dict[str, Any]], list[Path]]:
    profiles: list[dict[str, Any]] = []
    paths = tracked_novel_technique_paths()
    for path in paths:
        payload = read_json(path)
        extract_version = int(payload.get("extract_version", version_of(path)))
        for raw in payload.get("entries", []):
            if not isinstance(raw, dict) or not norm(raw.get("id")):
                continue
            entry = {**raw, "extract_version": extract_version, "source": payload.get("source", path.name)}
            descriptions = unique(flatten(raw.get("visual_descriptions")))
            kind = "direct_novel_quote" if descriptions else "novel_setting_fallback"
            profiles.append(make_profile(entry, "novel", path, descriptions, kind))
    return profiles, paths


def java_audit() -> dict[str, Any]:
    paths = sorted(JAVA_DIR.rglob("*.java"))
    aggregate = hashlib.sha256()
    related: list[str] = []
    registrations = 0
    spell_classes = 0
    tokens = ("Technique", "SkillEffect", "Spell", "Vfx", "VisualEvent", "Particle", "Trail")
    for path in paths:
        data = path.read_bytes()
        rel = relative(path)
        aggregate.update(rel.encode("utf-8"))
        aggregate.update(b"\0")
        aggregate.update(data)
        text = data.decode("utf-8")
        if any(token in text for token in tokens):
            related.append(rel)
        registrations += len(re.findall(r"register\s*\(\s*SkillType\.", text))
        if re.search(r"\bclass\s+\w*(?:Spell|Effect)\b", text):
            spell_classes += 1
    return {
        "file_count": len(paths),
        "aggregate_sha256": aggregate.hexdigest(),
        "spell_related_file_count": len(related),
        "spell_effect_class_count": spell_classes,
        "skill_effect_registry_registration_count": registrations,
        "spell_related_files": related,
    }


def compile_catalog() -> dict[str, Any]:
    rows = collect_visual_rows()
    corpus, corpus_paths = load_corpus(rows)
    novel, novel_paths = load_novel()
    profiles = corpus + novel
    ids = [profile["id"] for profile in profiles]
    qualified = [profile["qualified_id"] for profile in profiles]
    signatures = [profile["visual_signature"] for profile in profiles]
    if len(ids) != len(set(ids)):
        duplicates = sorted(key for key, count in Counter(ids).items() if count > 1)
        raise ValueError(f"cross-corpus spell id collision: {duplicates[:12]}")
    if len(qualified) != len(set(qualified)) or len(signatures) != len(set(signatures)):
        raise ValueError("qualified IDs and visual signatures must be unique")
    for profile in profiles:
        if profile["family"] not in RUNTIME_FAMILIES or profile["motif"] not in RUNTIME_MOTIFS:
            raise ValueError(f"renderer enum closure failed for {profile['id']}")
        if profile["effect_type"] not in EFFECT_TYPES or not profile["frames"]:
            raise ValueError(f"functional/timeline closure failed for {profile['id']}")
    profiles.sort(key=lambda profile: (profile["namespace"], profile["id"]))
    source_paths = list(dict.fromkeys(corpus_paths + novel_paths + list(VISUAL_LAYERS)))
    source_hashes = {relative(path): digest(path) for path in source_paths}
    direct_novel = sum(profile["visual_source_kind"] == "direct_novel_quote" for profile in novel)
    return {
        "schema_version": 1,
        "description": "全量文本材料法术视觉与功能语义计划；不依赖旧 344 条模板输出",
        "source_hashes": source_hashes,
        "java_source_audit": java_audit(),
        "counts": {
            "corpus": len(corpus),
            "novel": len(novel),
            "total": len(profiles),
            "novel_direct_visual": direct_novel,
            "novel_setting_fallback": len(novel) - direct_novel,
            "novel_visual_quotes": sum(len(profile["visual_descriptions"]) for profile in novel
                                       if profile["visual_source_kind"] == "direct_novel_quote"),
            "unique_visual_signatures": len(set(signatures)),
        },
        "constraints": {
            "old_profile_reuse": False,
            "preserve_author_prose": True,
            "max_frames_per_spell": 9,
            "max_radius": 8.0,
            "max_range": 48.0,
            "max_intensity": 48,
        },
        "profiles": profiles,
    }


def encoded(catalog: dict[str, Any]) -> str:
    return json.dumps(catalog, ensure_ascii=False, indent=2) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="fail when committed output is stale")
    args = parser.parse_args()
    compiled = compile_catalog()
    content = encoded(compiled)
    if args.check:
        if not OUTPUT.exists() or OUTPUT.read_text(encoding="utf-8") != content:
            print(f"stale generated file: {OUTPUT.relative_to(ROOT)}")
            return 1
        counts = compiled["counts"]
        print("authored spell effects are current: "
              f"{counts['total']} profiles ({counts['corpus']} corpus + {counts['novel']} novel)")
        return 0
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(content, encoding="utf-8")
    counts = compiled["counts"]
    print(f"wrote {OUTPUT.relative_to(ROOT)} ({counts['total']} profiles)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
