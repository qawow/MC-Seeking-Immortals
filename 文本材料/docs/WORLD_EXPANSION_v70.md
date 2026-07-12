# 全模块继续扩充 v70

对齐汇编 **慕兰大战结构**、**天劫小/大/仙**、**秘境三段式**，并 **合并 `novel_*_waves` 种子入主表**。

## 种子合并（入主表）

| 种子文件 | 目标 |
|----------|------|
| `novel_cultivation_waves.json` | `cultivation_methods.json`（去重追加 + 种子自补 setting） |
| `novel_cultivation_content_waves.json` | `data/techniques/*.json` + `method_content` → `cultivation_methods` |
| `novel_items_waves.json` | `pills_catalog.json` |
| `novel_beast_puppet_waves.json` | `beast_bestiary.json` |
| `novel_world_expansion_waves_v64.json` | `secret_realms.json` |

所有 `novel_*waves*.json` 内列表条目补 `setting`（种子可独立阅读）。

## 战役与地理

- **慕兰大战** `mulan_tianlan_war.json`：边境摩擦、法士大阵、圣禽介入、天南反攻 — 原著向 lore + 参战门槛  
- **突兀—慕兰世仇** `wutu_mulan_feud.json`：三方角色与战役链接  
- **天劫** `tribulation_rules.json`：小劫/化神/炼虚等类型 lore  
- **生态** `worldgen_biomes.json`：天南林、乱星海岛、蛮荒  
- **阴罗殿** `yin_luo_hall.json`：声望阶梯  

## 剩余缺 setting 文件

兽典阶表、傀儡阶、符宝模板、炼器失败掉落、patchouli/moditems/贴图批次等已批量补注记。

## 脚本与包

`novel_world_expansion_waves_v70.json` · `expand_world_modules_v70.py` · `seeking_immortals_world_v70.zip`