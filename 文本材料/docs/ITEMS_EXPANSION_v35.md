# 物品扩充 v35（炼器强宗 · 合欢宗 · 区域索引）

## 汇编 §9 / §18

- **炼器强宗**（与傀儡巷区分）：天南炼器坊 — 淬器灵油、中品灵铁锭、注灵针、炼器锤、低/中炼器手册
- **魔道双修宗**：合欢宗贡献楼 + 既有魔市互补
- **区域卡**：`items_by_region.json` schema v3，挂载 v29–v34 丹药/材料/秘境掉落/坊市 id

## 炼器向材料

| id | 用途 |
|----|------|
| refinement_quench_oil | 淬炼收尾 |
| spirit_iron_ingot_mid | 中阶炼器坯料 |
| artifact_spirit_injection_needle | 注灵工序 |

## 区域索引字段

- `merchant_shops`：区域可进入的专精坊市 id 列表
- `secret_realm_drops`：灵界等区域关联宝箱表 id（风元卡聚合多秘境）

## 测试

`tests/test_canon_v35_region_shops.py`