import json
import re
import shutil
from pathlib import Path

ROWS = json.loads(Path("_tmp_eng_displays.json").read_text(encoding="utf-8"))
ZH = json.loads(Path("src/main/resources/assets/seeking_immortals/lang/zh_cn.json").read_text(encoding="utf-8"))

# Prefer existing item/block Chinese names.
NAME = {}
for k, v in ZH.items():
    if (k.startswith("item.seeking_immortals.") or k.startswith("block.seeking_immortals.")) and isinstance(v, str):
        if re.search(r"[一-鿿]", v):
            NAME[k.split(".")[-1]] = v

# Explicit translations for residual catalog index displays.
EXTRA = {
    # chronicle / events
    "E_mortal_cap_enforcement": "事件·凡俗上限执法",
    "L3_void_realm_cultivation_norm": "纪事·虚境修炼规范",
    # elements / roots
    "metal": "金",
    "wood": "木",
    "water": "水",
    "fire": "火",
    "earth": "土",
    "wind": "风",
    "thunder": "雷",
    "ice": "冰",
    "yang": "阳",
    "yin": "阴",
    # economy bands / catalog expansions
    "v13": "物品目录·价值带一",
    "v14": "物品目录·价值带二",
    "v15": "物品目录·价值带三",
    "v16": "物品目录·价值带四",
    "v21": "物品目录·价值带五",
    "v57_novel_complete": "小说物品索引·完整",
    "v58_novel_all": "小说物品索引·全量",
    "artifact_low": "法宝·低阶",
    "artifact_mid": "法宝·中阶",
    "artifact_high": "法宝·高阶",
    "artifact_top_tier": "法宝·顶级",
    "artifact_fine_threshold": "法宝精炼阈值",
    "talisman_low": "符箓·低阶",
    "talisman_mid": "符箓·中阶",
    "talisman_high": "符箓·高阶",
    "herb_common": "常见灵草",
    "forge_grade": "炼器炉阶",
    "manual_unlock": "功法解锁",
    "manual_refine_low": "低阶炼器手册",
    "material_quality": "材料品质",
    "refinement_skill": "炼器技艺",
    "pill_qi_refining": "丹药·炼气",
    "pill_foundation": "丹药·筑基",
    "pill_quality_audit": "丹药品阶核验",
    "qi_refining_gear": "炼气装备",
    "sect_stipend_qi_outer": "外门炼气津贴",
    "thousand_year_herb_vs_fine_artifact": "千年灵药对照精良法宝",
    "top_artifact_fair_low_equiv": "顶级法宝会·低等值对照",
    "mid_artifact_fair": "中阶法宝会",
    "low_artifact_fair": "低阶法宝会",
    "auction_legendary": "拍卖·传说级",
    "auction_deity_pill": "拍卖·仙丹",
    "foundation_pill_auction": "筑基丹拍卖",
    # refinement fail tables
    "refinement_fail_default": "炼器失败·默认",
    "refinement_fail_low": "炼器失败·低阶",
    "refinement_fail_mid": "炼器失败·中阶",
    "refinement_fail_high": "炼器失败·高阶",
    "refinement_fail_ancient": "炼器失败·上古",
    "refinement_fail_puppet": "炼器失败·傀儡",
    "refinement_fail_spirit": "炼器失败·灵宝",
    # realms / regions
    "core_formation": "结丹",
    "foundation_breakthrough": "筑基突破",
    "nascent_soul": "元婴",
    "mortal_life": "凡俗生涯",
    "realm_control": "境界压制",
    "diyuan": "地渊",
    "feiling": "飞灵",
    "void_palace": "虚空殿",
    "huangfeng_valley": "黄枫谷",
    "kunwu_mountain": "坤吾山",
    "fallen_demon_valley": "堕魔谷",
    "blood_forbidden": "血禁",
    "yinming_pocket": "阴冥秘境",
    "nether_river_land": "冥河之地",
    "tianyuan_city": "天渊城",
    "yanyue_sect": "掩月宗",
    "lingfu_sect": "灵符宗",
    "ghost_spirit_gate": "鬼灵门",
    "thousand_bamboo_puppet_tower": "千竹傀儡楼",
    "guanghan_realm": "广寒境",
    "jiuxian_seclusion": "九仙闭关",
    "minor_asura_realm": "小修罗界",
    "wild_ancient_tomb": "荒野古墓",
    "demon_gold_mountain": "魔金山",
    "shuiyue": "水月",
    "jinhai": "金海",
    "jinyi": "锦衣",
    "mayi": "蚂蚁",
    "huzhu": "狐族",
    "shezu": "蛇族",
    "shiren": "尸人",
    "shuyao": "树妖",
    "muyao": "木妖",
    "mozu": "魔族",
    "guizu": "鬼族",
    "yaksha": "夜叉",
    "langzu": "狼族",
    "lingzu": "灵族",
    "tianhu": "天狐",
    "tianpeng": "天鹏",
    "yin_qi_aura": "阴气灵场",
    # quest hooks / faction quests
    "alchemy_furnace_access": "炼丹炉使用权限",
    "ancient_cultivator_ruins": "上古修士遗迹",
    "ancient_ruin_explore": "上古遗迹探索",
    "ancient_treasure": "上古秘宝",
    "ancient_wood_core_donate": "古木核心捐献",
    "array_disk_commission": "阵盘委托",
    "artifact_appraisal_quest": "法宝鉴定任务",
    "ascension_gate_rumor": "飞升门传闻",
    "auction_invite_dajin": "大晋拍卖邀请",
    "barbarian_beast_tide_survive": "蛮荒兽潮求生",
    "barbarian_council_audience": "蛮王议事觐见",
    "barbarian_king_token_hunt": "蛮王令搜寻",
    "barbarian_tribute_run": "蛮部进贡护送",
    "clan_feud_mediation_optional": "宗族仇隙调停（可选）",
    "clan_guest_register": "宗族客卿登记",
    "clan_raid_defense_ning": "宁氏宗族防守战",
    "demon_beast_siege_defense": "妖兽围城防御",
    "demon_qi_purge_side": "清剿魔气支线",
    "diyuan_core_probe": "地渊核心探查",
    "diyuan_permit_apply": "地渊通行令申请",
    "failed_hide_talisman": "失败的隐匿符",
    "feud_truce_broker": "仇隙停战斡旋",
    "fengyuan_clan_intro": "风元宗族引见",
    "formation_trial_mo": "阵法试炼·墨",
    "ghost_path_active": "鬼道启程",
    "herb_tribute_gu": "古氏灵草进贡",
    "illusion_pearl_trade": "幻珠交易",
    "internal_tax_vote": "内务税赋表决",
    "inverse_star_ambush_event": "逆星伏击事件",
    "inverse_star_cipher_trial": "逆星密文试炼",
    "inverse_star_contact_rumor": "逆星接头传闻",
    "inverse_star_contraband": "逆星禁运物资",
    "inverse_star_oath": "逆星盟誓",
    "king_territory_intrusion": "王域侵入",
    "kunwu_cold_snap_survive": "坤吾寒潮求生",
    "kunwu_map_fragment_turnin": "坤吾地图碎片上交",
    "kunwu_puppet_king": "坤吾傀儡王",
    "kunwu_rumor": "坤吾传闻",
    "mulan_counter_hunt": "木兰反击猎杀",
    "peak_talisman_three_thunder": "巅峰符箓·三雷",
    "protect_mortal_fleet": "护送凡俗船队",
    "recipe_scroll_random_low": "低阶配方残卷随机",
    "refinement_apprentice_yu": "炼器学徒·玉",
    "route_tiannan_to_mulan_smuggle": "天南至木兰走私线",
    "route_tianyuan_merit_convoy": "天渊功勋押运",
    "seal_weak_event": "封印松动事件",
    "soul_banner_quest": "魂幡任务",
    "spirit_fengyuan_border_patrol": "灵界风元边境巡逻",
    "spirit_ore_escort": "灵石矿护送",
    "star_palace_branch_commerce": "星宫分支·商事",
    "star_palace_branch_enforcement": "星宫分支·执法",
    "star_palace_enforcement_raid": "星宫执法突袭",
    "star_palace_patrol_dodge": "星宫巡海规避",
    "star_palace_spy_expose_optional": "星宫细作曝光（可选）",
    "talisman_basic_shop": "基础符箓铺",
    "talisman_blank_order": "空白符纸订购",
    "talisman_inscription_lesson": "符箓铭刻课业",
    "talisman_low_shop": "低阶符箓铺",
    "talisman_mid_master": "中阶符师考核",
    "tianyuan_merit_enlist": "天渊功勋入籍",
    "void_key_fragment_rumor": "虚空钥匙碎片传闻",
    "void_palace_intel_sell": "虚空殿情报出售",
    "wild_land_rumor": "荒野传闻",
    "wind_feather_craft_order": "风羽器物订单",
    "wutu_raid_mulan_camp": "巫屠突袭木兰营",
    "wutu_scout_contact": "巫屠斥候接触",
    "yin_luo_initiation": "阴罗殿入门",
    "asura_trial_chest": "修罗试炼箱",
    "blood_forbidden_chest": "血禁宝箱",
    "demon_gold_chest": "魔金宝箱",
    "diyuan_core_crystal_boss": "地渊核心晶主",
    "diyuan_layer_chest": "地渊层箱",
    "fallen_demon_chest": "堕魔宝箱",
    "guanghan_chest": "广寒宝箱",
    "jiuxian_peak_chest": "九仙峰宝箱",
    "kunwu_layer_chest": "坤吾层箱",
    "nether_river_chest": "冥河宝箱",
    "tianyuan_patrol_chest": "天渊巡逻箱",
    "void_palace_layer_chest": "虚空殿层箱",
    "inverse_star_smuggler_loot": "逆星走私缴获",
    "wanbao_auction_lot_pool": "万宝拍卖拍品池",
    "mulan_border": "木兰边境",
    "ghost_sect_ban_rules": "鬼道宗门禁令",
    "dajin_righteous_demon_skirmish": "大晋正魔边境冲突",
    # file-like manifest ids
    "artifact_faction_specialty.json": "法宝·宗门专精表",
    "artifact_realm_drops.json": "法宝·境界掉落表",
    "artifact_taxonomy_111.json": "法宝分类表 111",
    "ascension_flow.json": "飞升流程表",
    "barbarian_demon_kings.json": "蛮荒魔王表",
    "chaotic_sea_factions.json": "乱星海势力表",
    "demonic_six_sects.json": "魔道六宗表",
    "dimensions_catalog.json": "维度目录",
    "ghost_cultivation_path.json": "鬼修路径表",
    "ghost_sect_ban_rules.json": "鬼道宗门禁令表",
    "human_clan_league.json": "人族宗盟表",
    "human_clan_quest_network.json": "人族宗族任务网",
    "inverse_star_quest_network.json": "逆星任务网",
    "moditems_artifacts_draft.json": "模组法宝草案",
    "mulan_tianlan_war.json": "木兰天岚战表",
    "novel_cultivation_master_index.json": "小说修炼总索引",
    "novel_cultivation_waves.json": "小说修炼波次",
    "novel_items_master_index.json": "小说物品总索引",
    "novel_items_waves.json": "小说物品波次",
    "patchouli_static_entries.json": "手册静态条目",
    "quest_chains.json": "任务链表",
    "quest_hooks.json": "任务钩子表",
    "schema_validation_report.json": "结构校验报告",
    "secret_realm_template.json": "秘境模板",
    "skill_trees.json": "技能树表",
    "spatial_nodes_catalog.json": "空间节点目录",
    "spirit_realm_clan_quests.json": "灵界宗族任务",
    "spirit_roots.json": "灵根表",
    "star_palace_internal_factions.json": "星宫内务势力",
    "techniques/ghost.json": "术法·鬼道",
    "tianyuan_city.json": "天渊城数据",
    "wanbao_auction_artifacts.json": "万宝拍卖法宝表",
    "wutu_mulan_feud.json": "巫屠木兰世仇",
    "yin_luo_hall.json": "阴罗殿数据",
    "yin_underworld_cluster.json": "阴冥集群",
    "region_cards/barbarian_wasteland.json": "地区卡·蛮荒",
    "region_cards/dajin.json": "地区卡·大晋",
    "region_cards/kunwu.json": "地区卡·坤吾",
    "region_cards/nether_river.json": "地区卡·冥河",
    "region_cards/spirit_fengyuan.json": "地区卡·灵界风元",
    "region_cards/spirit_realm_border.json": "地区卡·灵界边境",
    "region_cards/tianlan.json": "地区卡·天岚",
    "region_cards/tiannan_north_waste.json": "地区卡·天南北荒",
    "region_cards/tianyuan.json": "地区卡·天渊",
    "region_cards/wutu_border.json": "地区卡·巫屠边境",
    "region_cards/yinming.json": "地区卡·阴冥",
}

# Phrase helpers for leftovers.
TOKEN_MAP = {
    "quest": "任务",
    "hook": "钩子",
    "route": "商路",
    "auction": "拍卖",
    "artifact": "法宝",
    "talisman": "符箓",
    "formation": "阵法",
    "refine": "炼器",
    "refinement": "炼器",
    "alchemy": "炼丹",
    "pill": "丹药",
    "manual": "功法",
    "sect": "宗门",
    "clan": "宗族",
    "beast": "灵兽",
    "demon": "魔",
    "ghost": "鬼",
    "spirit": "灵",
    "realm": "境界",
    "chest": "宝箱",
    "loot": "掉落",
    "boss": "首领",
    "patrol": "巡逻",
    "border": "边境",
    "war": "战事",
    "raid": "突袭",
    "trade": "交易",
    "shop": "商店",
    "token": "令牌",
    "permit": "许可",
    "map": "地图",
    "fragment": "碎片",
    "rumor": "传闻",
    "event": "事件",
    "trial": "试炼",
    "optional": "可选",
    "low": "低阶",
    "mid": "中阶",
    "high": "高阶",
    "fail": "失败",
    "default": "默认",
    "ancient": "上古",
    "void": "虚空",
    "palace": "殿",
    "mountain": "山",
    "river": "河",
    "sea": "海",
    "city": "城",
    "gate": "门",
    "hall": "殿",
    "path": "路线",
    "network": "网络",
    "index": "索引",
    "json": "数据",
    "report": "报告",
    "template": "模板",
    "catalog": "目录",
    "master": "总",
    "wave": "波次",
    "static": "静态",
    "entry": "条目",
    "entries": "条目",
    "skill": "技能",
    "tree": "树",
    "trees": "树",
    "technique": "术法",
    "techniques": "术法",
    "dimension": "维度",
    "dimensions": "维度",
    "region": "地区",
    "card": "卡",
    "cards": "卡",
    "secret": "秘境",
    "spawn": "刷新",
    "table": "表",
    "tables": "表",
    "band": "带",
    "value": "价值",
    "economy": "经济",
    "reference": "参考",
    "magnitude": "量级",
    "stone": "石",
    "root": "根",
    "roots": "根",
    "ban": "禁令",
    "rule": "规则",
    "rules": "规则",
    "border": "边境",
    "skirmish": "冲突",
    "righteous": "正道",
    "demonic": "魔道",
    "dajin": "大晋",
    "mulan": "木兰",
    "tianlan": "天岚",
    "wutu": "巫屠",
    "tianyuan": "天渊",
    "tiannan": "天南",
    "kunwu": "坤吾",
    "fengyuan": "风元",
    "yinming": "阴冥",
    "diyuan": "地渊",
    "asura": "修罗",
    "nether": "冥",
    "fallen": "堕",
    "star": "星",
    "palace": "宫",
    "inverse": "逆",
    "chaotic": "乱",
    "barbarian": "蛮",
    "human": "人族",
    "league": "盟",
    "feud": "世仇",
    "war": "战",
    "campaign": "战役",
    "expedition": "远征",
    "initiation": "入门",
    "enlist": "入籍",
    "convoy": "押运",
    "smuggle": "走私",
    "intel": "情报",
    "sell": "出售",
    "buy": "购买",
    "order": "订单",
    "lesson": "课业",
    "shop": "铺",
    "basic": "基础",
    "blank": "空白",
    "inscription": "铭刻",
    "protect": "护送",
    "mortal": "凡俗",
    "fleet": "船队",
    "survive": "求生",
    "defense": "防御",
    "siege": "围城",
    "purge": "清剿",
    "side": "支线",
    "contact": "接触",
    "scout": "斥候",
    "camp": "营",
    "audience": "觐见",
    "council": "议事",
    "hunt": "猎杀",
    "counter": "反击",
    "tribute": "进贡",
    "run": "护送",
    "register": "登记",
    "guest": "客卿",
    "mediation": "调停",
    "truce": "停战",
    "broker": "斡旋",
    "vote": "表决",
    "tax": "税赋",
    "internal": "内务",
    "branch": "分支",
    "commerce": "商事",
    "enforcement": "执法",
    "raid": "突袭",
    "patrol": "巡逻",
    "dodge": "规避",
    "spy": "细作",
    "expose": "曝光",
    "cipher": "密文",
    "trial": "试炼",
    "ambush": "伏击",
    "contraband": "禁运",
    "oath": "盟誓",
    "rumor": "传闻",
    "invite": "邀请",
    "access": "权限",
    "furnace": "炉",
    "explore": "探索",
    "ruins": "遗迹",
    "cultivator": "修士",
    "treasure": "秘宝",
    "donate": "捐献",
    "core": "核心",
    "wood": "木",
    "ancient": "古",
    "commission": "委托",
    "disk": "盘",
    "array": "阵",
    "appraisal": "鉴定",
    "gate": "门",
    "ascension": "飞升",
    "probe": "探查",
    "apply": "申请",
    "permit": "通行令",
    "layer": "层",
    "crystal": "晶",
    "king": "王",
    "puppet": "傀儡",
    "fragment": "碎片",
    "turnin": "上交",
    "cold": "寒",
    "snap": "潮",
    "territory": "域",
    "intrusion": "侵入",
    "intro": "引见",
    "active": "启程",
    "path": "道",
    "trade": "交易",
    "pearl": "珠",
    "illusion": "幻",
    "three": "三",
    "thunder": "雷",
    "peak": "巅峰",
    "random": "随机",
    "scroll": "卷",
    "recipe": "配方",
    "apprentice": "学徒",
    "yu": "玉",
    "mo": "墨",
    "gu": "古",
    "ning": "宁",
    "feather": "羽",
    "craft": "器物",
    "land": "地",
    "wild": "荒野",
    "key": "钥匙",
    "seclusion": "闭关",
    "pocket": "秘境",
    "aura": "灵场",
    "qi": "气",
    "banner": "幡",
    "soul": "魂",
    "weak": "松动",
    "seal": "封印",
    "event": "事件",
    "hide": "隐匿",
    "failed": "失败的",
    "talisman": "符",
    "quality": "品质",
    "audit": "核验",
    "foundation": "筑基",
    "refining": "炼气",
    "gear": "装备",
    "stipend": "津贴",
    "outer": "外门",
    "fine": "精良",
    "threshold": "阈值",
    "top": "顶级",
    "tier": "阶",
    "fair": "会",
    "equiv": "等值",
    "legendary": "传说",
    "deity": "仙",
    "magnitude": "量级",
    "reference": "参考",
    "master": "总",
    "waves": "波次",
    "complete": "完成",
    "all": "全量",
    "draft": "草案",
    "moditems": "模组物品",
    "taxonomy": "分类",
    "specialty": "专精",
    "faction": "势力",
    "drops": "掉落",
    "flow": "流程",
    "kings": "王",
    "factions": "势力",
    "six": "六",
    "sects": "宗",
    "cultivation": "修炼",
    "items": "物品",
    "novel": "小说",
    "patchouli": "手册",
    "static": "静态",
    "chains": "链",
    "hooks": "钩子",
    "schema": "结构",
    "validation": "校验",
    "secret": "秘境",
    "spatial": "空间",
    "nodes": "节点",
    "skill": "技能",
    "roots": "根",
    "internal": "内务",
    "star": "星",
    "palace": "宫",
    "wanbao": "万宝",
    "lot": "拍品",
    "pool": "池",
    "smuggler": "走私",
    "north": "北",
    "waste": "荒",
    "wasteland": "荒原",
    "underworld": "冥界",
    "cluster": "集群",
    "luo": "罗",
    "hall": "殿",
    "initiation": "入门",
}


def humanize(s: str) -> str:
    s = s.strip()
    if not s:
        return s
    if s in EXTRA:
        return EXTRA[s]
    if s in NAME:
        return NAME[s]
    # file-like
    if s.endswith(".json") or "/" in s:
        base = s
        if base in EXTRA:
            return EXTRA[base]
        # strip path/ext and tokenize
        bare = base.replace(".json", "").replace("region_cards/", "地区卡·")
        parts = re.split(r"[/_.\-]+", bare)
        zh_parts = []
        for p in parts:
            if not p:
                continue
            if p in EXTRA:
                zh_parts.append(EXTRA[p])
            elif p in NAME:
                zh_parts.append(NAME[p])
            elif p in TOKEN_MAP:
                zh_parts.append(TOKEN_MAP[p])
            elif re.fullmatch(r"v\d+", p):
                zh_parts.append("版本" + p[1:])
            else:
                zh_parts.append(p)
        out = "·".join(zh_parts)
        return out if re.search(r"[一-鿿]", out) else "索引·" + bare
    # recipe_
    if s.startswith("recipe_"):
        rest = s[7:]
        if rest in NAME:
            return "配方·" + NAME[rest]
        return "配方·" + humanize(rest)
    # tokenize snake
    parts = re.split(r"[_\-.]+", s)
    zh_parts = []
    for p in parts:
        if not p:
            continue
        if p in EXTRA:
            zh_parts.append(EXTRA[p])
        elif p in NAME:
            zh_parts.append(NAME[p])
        elif p in TOKEN_MAP:
            zh_parts.append(TOKEN_MAP[p])
        elif re.fullmatch(r"v\d+", p):
            zh_parts.append("版本" + p[1:])
        elif re.fullmatch(r"\d+", p):
            zh_parts.append(p)
        else:
            zh_parts.append(p)
    out = "".join(zh_parts) if all(re.search(r"[一-鿿0-9]", x or "") for x in zh_parts) else "·".join(zh_parts)
    if not re.search(r"[一-鿿]", out):
        out = "条目·" + s
    return out


# Build final map for all residual ids.
FINAL = {}
for row in ROWS:
    i = row["id"]
    FINAL[i] = humanize(i)

# Apply to files.
bak_root = Path(".bak/20260715_170000_catalog_display_zh")
bak_root.mkdir(parents=True, exist_ok=True)

changed = 0
files_touched = 0
still = []
for p in Path("src/main/resources/data/seeking_immortals/catalog").glob("*_index.json"):
    original = p.read_text(encoding="utf-8")
    data = json.loads(original)
    touched = False

    def walk(o):
        global changed, touched
        if isinstance(o, dict):
            if "id" in o and "display" in o and isinstance(o["display"], str):
                disp = o["display"]
                i = str(o.get("id", ""))
                if re.search(r"[A-Za-z]", disp) and not re.search(r"[一-鿿]", disp):
                    new = FINAL.get(i) or humanize(i)
                    if re.search(r"[一-鿿]", new):
                        o["display"] = new
                        changed += 1
                        touched = True
                    else:
                        still.append((p.name, i, disp, new))
            for v in o.values():
                walk(v)
        elif isinstance(o, list):
            for v in o:
                walk(v)

    walk(data)
    if touched:
        b = bak_root / p
        b.parent.mkdir(parents=True, exist_ok=True)
        if not b.exists():
            b.write_text(original, encoding="utf-8")
        p.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        files_touched += 1

# recount
left = []
for p in Path("src/main/resources/data/seeking_immortals/catalog").glob("*_index.json"):
    data = json.loads(p.read_text(encoding="utf-8"))

    def walk(o):
        if isinstance(o, dict):
            if "id" in o and "display" in o and isinstance(o["display"], str):
                disp = o["display"]
                if re.search(r"[A-Za-z]", disp) and not re.search(r"[一-鿿]", disp):
                    left.append((p.name, o.get("id"), disp))
            for v in o.values():
                walk(v)
        elif isinstance(o, list):
            for v in o:
                walk(v)

    walk(data)

print("changed", changed, "files", files_touched)
print("remaining", len(left))
for x in left[:30]:
    print(x)
Path("_tmp_display_map.json").write_text(json.dumps(FINAL, ensure_ascii=False, indent=2), encoding="utf-8")
print("map size", len(FINAL))
