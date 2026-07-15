# 多方块结构 v135

> 无代码。

## 本卷两件事

### A. 联机交互时序
文件：`multiblock_multiplayer_sequences_v135.json`

| 时序 id | 内容 |
|---------|------|
| seq_repair_race | 抢修（双通道、打断、修复台加速） |
| seq_array_contest | 夺阵（占点、拔旗、护宗倍率） |
| seq_build_coop | 协作建造（蓝图幽灵、共交材料） |
| seq_overhaul_siege | 大修与围攻打断 |
| seq_fuel_steal | 灵石槽供能争夺 |
| seq_teleport_interrupt | 传送引导打断与反噬 |
| seq_flag_tug | 拔旗/护旗 |

全局：交互锁 / 引导锁 / 争夺锁 / 建造锁；归属与 AOI 同步提示。  
各结构 `structure.multiplayer_sequence_ids` 已挂接。

### B. 材料目录对齐 + 参考价
- 从多方块收集 **239+** 材料 id  
- `materials_catalog.json`：**新增约 240**，更新约 **5**（补 `price_spirit_stone_low`、`structure_build` use）  
- 明细：`multiblock_material_prices_v135.json`  
- 每结构 materials 行含单价/行价；并有 `estimated_build_cost_spirit_stone_low`  
- 兑换：碎=0.1 / 下=1 / 中=10 / 上=100 / 仙=1000  

### C. 新增争夺配套 +4
| 结构 | 用途 |
|------|------|
| 宣战战旗桩 | 和平区开战窗 |
| 攻阵灵能撞车 | 双人破阵 |
| 战地抢修帷帐 | 野外抢修加速 |
| 争夺点方尖碑 | 通用据点 |

## 数量
- 结构总数：**86**
- 材料价目条数：**245**
- 目录材料总数：**461**

## 文件
- block_items_catalog.json（schema 7）
- multiblock_multiplayer_sequences_v135.json
- multiblock_material_prices_v135.json
- multiblock_structure_index_v135.json
- materials_catalog.json
