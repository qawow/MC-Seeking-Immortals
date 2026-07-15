# 功法 / 术法分类修正 v123

> 无代码。P0 硬伤 + P1 分类同步。

## P0
| 项 | 处理 |
|----|------|
| 重复 tech id | `inverse_star_veil` misc→`inverse_star_veil_trace`；`golden_armor_talisman_cast` misc→`golden_armor_talisman_forge_cast` |
| 悬空 requires_method | 别名映射 + 补功法 `five_elements_root_art` / `huadao_slash_art` |
| 境界枚举 | `VOID_REFINING`/`SPIRIT_SEVERING` → `VOID_REFINEMENT`（与 progression 对齐） |
| 坤吴印诀 | 显示名改为 **昆吾印诀** |

## P1 path 规范
合并：`tao→dao`，`dual_cultivation/dual→dual`，`covert/stealth→movement`，`beast_*→beast`，五行 path→`elemental`，`defense→body`，`blood/poison→demonic`，`spirit→divine_sense`，炼丹/炼器/鉴宝→`craft_*`（无战斗 school）。

表：`data/method_path_taxonomy_v123.json`

## 互链
- 长春：仅低阶 elemental/部分 recovery；高阶五行改挂五行根基诀
- 玄阴：仅 xuan_yin 文件；魔术改挂血煞等
- 鬼符改挂天符；玄阴魂链改挂玄阴
- fashi 文件 school 统一 `fashi`
- secret_arts：`school=secret_arts`，原流派进 `parent_school`

## 未强搬
misc 中明显五行/剑/遁等只打 `suggested_school_v123` / `misc_pending_move:*`，避免大挪文件；下轮可物理迁移。

## 文件
- `cultivation_methods.json`（已改）
- `techniques/*.json` + `index.json`
- `technique_method_bridge_v123.json`
- `classification_patch_v123.json`
