# 物品扩充 v28（2026-07-03）

> 阵法九龙神火罩、黄枫谷符匣、汇编镜/傀儡部件、Java DeferredRegister 生成。

## 阵法

- `formation_catalog`：**nine_dragon_flame_barrier**（与 `nine_dragon_flame_array_disk` 对齐）
- `test_formation_formation_items`：阵盘 `formation_id` 必在 catalog

## 宗门贡献

- 黄枫谷：**低/中阶符箓匣**、**护体符**（贡献点 + 月限）

## 汇编 11.1 / 11.7

- 法宝向材料：**邪幻镜碎片**、**玄黄镜碎片**（炼符加成，非古宝本体）
- 傀儡：**巨猿傀儡图**、**巨龟甲胚**

## Forge 代码生成

- `generate_deferred_register_java.py` → `SeekingImmortalsItems.java`（前 **80** 项 stub，全量见 JSON）

## 规模

| 指标 | v28 |
|------|-----|
| artifacts | **35** |
| puppet_parts | **10** |
| forge_registry | **241** |
| Java stub | **80** `RegistryObject` |
| worldpack | **356** 文件 |

## v29 候选

- 镜碎片写入 `loot_tables` / 秘境
- DeferredRegister 按 category 分文件
- `formation_catalog` ↔ Patchouli 阵法卷