# 多方块结构 v134

> 无代码。

## 本卷重点
1. **精修**原 auto 建造表（制符台、傀儡台、灵草圃、引流管、冰鉴、灵火鼎、核心炼台、契坛、阵旗、聚灵井、血祭/炼尸、双修室、逆星暗港、真言台、轮回案、拍卖台、灵石库、藏经架、贡献碑、飞舟泊、灌溉、丹柜、法宝架、矿与秘境碑等）为**具体材料+阶段**  
2. **全部 82 个结构**写入 `structure.operational_states`：  
   - 状态：完好 → 受损 → 濒毁 → 瘫痪（矿：未开采/采空）  
   - 转移：受击/过载/炸炉/拔旗/修理/大修/升级  
   - 交互：use / inspect / repair / upgrade / dismantle / recharge…  
   - 耐久 max_hp、损伤源、修理费用提示、升级路径  

## 新增配套结构 +8
| id | 用途 |
|----|------|
| 结构修复台 | 加速修理状态机 |
| 结构蓝图台 | 查看 build_stages |
| 阵法维稳碑 | 降大阵过载损伤 |
| 丹炉防爆小阵 | 炸炉少掉档 |
| 认主石碑 | 批量归属 |
| 灵兽化形池 | 化形仪式 |
| 渡劫台 | 渡劫/丹劫位点 |
| 内门任务牌 | 宗门任务 |

## 数量
- 结构总数：**82**
- 有状态机：**82**
- 有建造阶段：**82**
- 有材料：**82**
- 手工建造（含本卷精修）：约 **61**
- 仍 auto_pending：**0**

## 文件
- `block_items_catalog.json`（schema 6）
- `multiblock_operational_states_v134.json`（总则）
- `multiblock_structure_index_v134.json`


续：`MULTIBLOCK_STRUCTURE_EXPANSION_v135.md`
