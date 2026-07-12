# 法宝 / 法器设定（凡人向 · 模组数据）

## 原著结构（优先照搬）

| 模组 `tier` | 原著称呼 | 典型境界 | 说明 |
|-------------|----------|----------|------|
| `low` / `mid` | **法器** | 炼气、筑基 | 威力有限，散修与低阶宗门主力 |
| `high` | **法宝** | 结丹及以上 | 需灵力支撑；**境界不足则威能大减** |
| — | **符宝** | 多为结丹炼制 | `type: talisman_treasure`，`uses` 有限，近法宝但消耗 |
| `ancient_treasure` | **古宝** | 结丹～元婴+ | 不可批量炼制，秘境/残片重铸 |
| `spirit_treasure` | **灵宝 / 通天灵宝向** | 炼虚（灵界） | 碎片/仿制品名，避免绑定专名 |

十一级细档见 `data/artifact_eleven_tier_map.json`（`game_tier` 1–11）。

## 数据文件

- `data/artifacts_catalog.json` — 主目录（**schema v3，约 65 条**）
- `data/artifact_tier_rules.json` — 五档 + 本命法宝规则
- `data/refinement_recipes.json` — 可炼制条目
- `data/flight_vehicles.json` — 灵舟/云轿（飞行器具，非攻击法宝）

## 分类（`type`）

`flying_sword` `offense` `defense` `movement` `illusion` `soul_*` `puppet_*` `talisman_treasure` `formation_deploy` `storage` `utility` `material_artifact` `spirit_treasure` …

## 合规（对外发布）

- 主角强绑定专名 → **仿 / 碎片 / 模板**（见各条 `note` / `compliance`）
- 通用名词（筑基丹、灵石、飞剑、符箓）可直接使用
- 朱雀环、掌天瓶类 → 本模组用「朱雀环（仿）」「敛气残帛」等

## 扩充轮次

### 第一轮（+30）→ 65 条

**人界法器**：青叶、烈阳剑、冷月刀、黄丝衫、银巨剑、青索网、火雨针、风火轮、龟壳盾、血玉盾、储物镯、御兽鞭…  
**法宝/符宝**：千蜂针、火云旗、金光砖符宝、朱雀环（仿）、镇妖塔（仿）  
**古宝/灵界向**：黑月尺、落星环、青铜古钟、虚天尺碎片、八灵尺碎片、黑风旗残角、七焰扇（仿）

### 第二轮（+28）→ **93 条**

**汇编 11.1 分类补全**
- 攻击：乌金盾、玄铁环、摄魂铃、炼魂幡、降魔杵、火鸦扇、饮血钩、量天尺（法器）…
- 防御/衣：天蚕法衣、驱灵镜
- 飞行/遁：风遁帆（单人飞行法器，区别于 `flight_vehicles` 灵舟）
- 符宝：霹雳神雷符宝、金轮符宝、封魔符宝 → `data/talisman_treasure_templates.json`
- 古宝/灵宝碎片：平海戈、万森轮盘碎片；本命法宝胚
- 乱星海：星宫制式飞剑、逆星刺刃、镇海链、虚天尺碎片（见 `items_by_region`）

**新增索引**
- `data/ancient_treasure_index.json` — 成品 / 碎片 / 灵宝残片 / 仿模板
- `data/talisman_treasure_templates.json` — 符宝炼制境界、次数衰减

### 第三轮（+28）→ **121 条**

- **汇编 11.1 补全**：御兽环/兽魂铃（灵兽山）、铁猿甲、银灵镜、月影轮（掩月）、七星盘、凤羽扇、龙鳞甲…
- **符宝 +3**：火蛟矛、玄冰盾、定魂符宝
- **秘境掉落**：`artifact_realm_drops.json` + `secret_realms` loot 扩展（血色禁地/虚天殿/昆吾/坠魔谷）
- **宗门**：`artifact_faction_specialty.json`；黄枫谷/灵兽山贡献堂法宝条目
- **合规**：辟邪神兵（仿）、黄泉幡（仿）；玄光镜/九龙罩**碎片**条目

### 第四轮（+23）→ **144 条**

**严格对齐汇编 11.1 原文条目（仿/成品并存）**
- 云轿令牌、大挪移护符（仿）、混元钵（仿）、玄光镜（仿）— 与古宝/高阶本体区分
- 踏云靴/乌靴、大挪移令、引魂钟、聚魂钵、婆罗珠、邪幻镜、玄黄镜、混元钵 — 前序已录入

**新增**：玄冰剑、金刚罩/琉璃罩、天雷子（一次性符宝）、本命飞剑胚、鉴宝瞳镜、万宝楼/大晋拍卖 `wanbao_auction_artifacts.json`、Forge 首批 `forge_artifact_priority.json`

### 第五轮（+25）→ **169 条**

**攻击/防御补全（通用名+仿）**：无形针匣、玄铁飞天盾、红线遁光针（仿）、两仪环（仿）、弥天镯（仿）、四象尺（仿）、三焰扇（仿）、黑风旗（仿）…  
**汇编 11.1 特殊类**：御风车（`flight_vehicles` 绑定）、画轴芥子器、禁魔环（仿）  
**灵宝/古宝碎片**：虚天鼎碎片、魔龙刃胚、平山印（仿）、玉如意（仿）  
**炼器材料入表**：庚精镶件、太阳精石镶件、化形泥、兽骨块  
**结构化**：`artifact_taxonomy_111.json`（§11.1 四类）、`moditems_artifacts_draft.json`（22 项 Forge 草稿）

炼器配方现为 **73** 条。

## 游戏化建议

1. 装备栏区分 **法器槽 / 法宝槽 / 符宝一次性槽**  
2. `realm_min` 低于要求时：`power_scale = clamp(0.2, realm_ratio, 1.0)`  
3. 本命法宝见 `artifact_tier_rules.natal_artifact`（结丹、唯一、毁则重伤）