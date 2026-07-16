# 2026-07-16 M02 功法与术法落地

## 变更类

代码 + 发布资源（text_material / catalog / lang）。`mod_version` 按任务红线保持 `0.1.506` 不升；网络包字段兼容扩展同步包容量与 PROTOCOL 至 `21`。

## 接入范围决策（747 语料）

**正式接入运行时：作者侧 747 部 technique 全量发布。**

| 集合 | 处理 |
|---|---|
| `文本材料/data/techniques/*`（747） | 复制进 `src/main/resources/data/seeking_immortals/text_material/techniques/`，由 `TechniqueDataManager` + `ClientTechniqueData` 加载 |
| 历史 346 发布快照 | 被 747 覆盖，不再保留为运行时权威 |
| jar `cultivation/*.json`（477 境界桶） | 保留兼容；id 冲突时 text_material 语料优先 |
| `cultivation_methods` 136 | 发布 + catalog index 升至 136；`TextMaterialCatalogService` 优先读 text_material |
| `skill_trees` 90 + layer schema | 发布并由 `SkillTreeCatalogService` 消费 |
| `manual_conflict_matrix_v100` | 发布并由 `ManualConflictMatrixService` 在 `learnMethod` 路径硬门禁 D/F |
| `method_layer_technique_matrix_v130` | 发布；学法/升层时 `MethodLayerTechniqueService` 解锁对应术法 |
| `novel_cultivation_*` | 已在 text_material 中刷新；作为设定/校验语料，不单独再开加载器（已合并进 747/136） |

**暂缓：** 不为 747 条各自新增 `SkillType` 枚举常量；改用 40 种 `effect.type` → 可复用 `SkillEffect` 的抽象解析器。无 SkillType 映射的术法走 `AbstractTechniqueEffectResolver` + 虚拟 skill，保证“每条已加载 technique 的 effect 可解析”。

## 实现要点

1. `TechniqueDataManager`：加载 classpath 语料 20 校 + 旧 cultivation 6 文件；`TechniqueEntry` 扩展 `requiresMethod/tier/effectType/effectElement/cooldownTicks`。
2. `ClientTechniqueData`：同步扩量 builtin 摘要。
3. `AbstractTechniqueEffectResolver`：40 abstract type 映射；懒加载避免单元测试触碰 MC registry。
4. `ReleaseTechniquePacket`：效果解析走 resolver；无 SkillType 亦可施放（仍服务端校验槽位/已学/冷却/灵力/门禁）。
5. `TechniqueGateService`：全量 text_material `requires_method`；神通/禁术高阶强制境界。
6. `ManualCatalogService`：学习前冲突矩阵；学法/升层授予层数矩阵术法。
7. `SyncLearnedTechniquesPacket`：`MAX_LEARNED/COOLDOWNS` 256→768。
8. `ModNetwork.PROTOCOL_VERSION` 20→21（同步包容量上限变化）。
9. 验收测试 `M02TechniqueCorpusTest`：加载 ≥747、effect 全解析、方法 136/树 90/矩阵存在。

## 验证

- `./gradlew --no-daemon test`（含 M02 聚焦 + 全量）通过。
- `./gradlew --no-daemon build -PaiSkipVersionBumpCheck=true` **BUILD SUCCESSFUL**。
- 跳过版本门禁原因：任务红线明确“不要修改 mod_version”；preflight 对 shippable 变更默认要求 bump，故显式 skip 并在此记录。

## 版本与协议

- `mod_version`：保持 `0.1.506`（任务约束）。
- `PROTOCOL_VERSION`：`20` → `21`（learned techniques 同步包上限变更）。

## 备份

`.bak/20260716_164708_m02_techniques/`

## 等待 M03

- 术法 `learn_requirements.items_required` / 飞剑装备门槛等物品实体校验：等待 M03 提供稳定物品 id 解析接口后再硬门禁。
- 冲突矩阵中叙事向标签（如“五行杂修”“普通疗伤丹依赖”）无 method id 时仍软忽略，不臆造物品/状态 id。
