# 大量扩充 v56

> 汇编 **§9.7 乱星海/逆星盟** · **§18 功法→法术→神通** · **§13** 虚天情报结构

## 逆星盟任务网

| 任务链 | 内容 |
|--------|------|
| `inverse_star_recruit` | 联络→暗号→入盟誓 |
| `inverse_star_smuggle_arc` | 走私包→躲执法→卖虚天情报 |
| `inverse_star_void_heist` | 虚天钥传闻→黑市→可选反间谍 |
| `chaotic_sea_civil_war` | 星宫巡逻→逆星伏击→可选中立 |

- `inverse_star_quest_network.json`
- `chaotic_sea_factions` v5 挂 `quest_network_ref`、声望 tier

## 魔道六宗门控审计

- `demonic_six_sects` v2：**skill_tree_gate_audit**（功法↔技能树）
- 补 **5** 棵道途树：合欢/天魔/青罗/万狐/血巫（`requires_method_all_spells` + `demonic_karma`）
- 鬼灵门沿用 **ghost_xuan_yin**

## validate_all

- `scripts/validate_all.py`：全 `data/**/*.json` 解析 + manifest 路径检查
- 输出 `schema_validation_report.json`

## Patchouli

- `inverse_star_alliance`、`demonic_six_overview`

## 打包

**`seeking_immortals_lore_v56.zip`**

## 下一步

- 星宫内部派系 `star_palace_internal_factions` 任务分支
- 虚天殿周期与逆星/星宫双线结局标签
- Forge 运行时加载 `inverse_star_quest_network`