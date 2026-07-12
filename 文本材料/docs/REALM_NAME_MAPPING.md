# 境界名对照表（模组 ↔ 设定汇编）

> **P0**：UI、Patchouli、任务文案应逐步统一；代码内部可同时保留 `Realm` 枚举别名。

| 模组当前（seeking_immortals） | 汇编/通用 | 英文（可选） | 小阶段 |
|---|---|---|---|
| 引气境 | 炼气期 | Qi Refining | 1～13 层（待实装） |
| 聚气境 | 筑基期 | Foundation Establishment | 初/中/后/圆满 |
| 凝元境 | 结丹期 | Core Formation | 初/中/后/圆满 |
| *（未实装）* | 元婴期 | Nascent Soul | 初/中/后/圆满 |
| *（未实装）* | 化神期 | Spirit Transformation | 初/中/后/圆满 |
| *（未实装）* | 炼虚期 | Void Refinement | 初/中/后/圆满 |
| *（未实装）* | 合体期 | Body Integration | 初/中/后/圆满 |
| *（未实装）* | 大乘期 | Mahayana | 初/中/后/圆满 |
| *（未实装）* | 渡劫 | Tribulation | 大乘巅峰状态 |

## 迁移建议

1. **短期**：Patchouli 显示「引气境（炼气）」双名。  
2. **中期**：配置 `realm_display_mode: dual | classic | mod`。  
3. **长期**：枚举改名时做存档 NBT 迁移表。

## JSON 字段

术法/物品统一用汇编键：`realm_min`: `QI_REFINING` | `FOUNDATION` | `CORE_FORMATION` …  
模组 Cap 映射：`引气境` → `QI_REFINING`。

## 与人界上限

化神**初期**为人界软上限（见 `CHRONICLE.md` D3）；模组实装化神前，凝元境圆满后应触发「飞升/跨界」引导任务。