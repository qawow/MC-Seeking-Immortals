# Existing Implementation Inventory

**生成日期**: 2026-06-14  
**版本**: 0.1.47  
**基于**: Phase 0 项目调研 + Phase 1 实现完成

---

## Summary

当前项目已实现**核心修仙系统的基础架构**，包括：

- ✅ **完整的玩家修仙数据系统**：Capability + NBT 持久化
- ✅ **境界与灵根系统**：10 境界 + 13 层练气 + 4 阶筑基 + 6 种灵根
- ✅ **修炼与突破系统**：打坐修炼、突破流程、走火入魔 4 级判定
- ✅ **灵力与战斗系统**：灵力消耗、战斗属性派生、PvP 钩子
- ✅ **物品与注册系统**：127+ 物品（灵石、丹药、功法卷轴、符箓、材料）
- ✅ **客户端 UI/HUD**：修仙面板、技能槽 HUD、打坐 HUD、技能编辑器
- ✅ **网络同步**：6 个数据包，协议版本 4
- ✅ **灵气/灵脉系统**：灵气浓度计算、灵脉探测、聚灵阵方块
- ✅ **技能系统框架**：枚举型技能 + 功法卷轴数据驱动

**缺失/未实现**：

- ❌ **神秘小瓶系统** (MVP Phase 5)
- ❌ **炼丹系统** (BlockEntity + GUI + Recipe，MVP Phase 7)
- ❌ **NPC 与对话系统** (MVP Phase 8)
- ❌ **任务系统** (MVP Phase 9-10)
- ❌ **完整技能释放** (仅框架，无实体/弹射物实现)

---

## Implemented

### 1. 玩家数据 / Capability

**状态**: ✅ **Implemented**

**相关文件**:
- `src/main/java/com/xunxian/seekingimmortals/cultivation/PlayerCultivation.java` (400+ 行)
- `src/main/java/com/xunxian/seekingimmortals/cultivation/CultivationProvider.java`
- `src/main/java/com/xunxian/seekingimmortals/cultivation/CultivationHelper.java`

**关键字段** (58+ 个):
- 六大核心属性：`spiritualPower`, `divineConsciousness`, `bodyRefinement`, `qiDeviationRisk`, `tribulationResistance`, `cultivationExp`
- 境界/阶段：`realm` (Realm 枚举), `stage` (RealmStage 枚举)
- 灵根系统：`spiritualRoot`, `spiritualRootAttributes` (List), `spiritualRootPurity`, `spiritualRootTested`
- 特殊体质：`specialPhysique`
- 负面状态：`severeInjury`, `heartDemonLevel`, `shatteredCore`, `realmFallScars`
- 功法/技能：`learnedTechniques` (List<String>), `techniqueSlots[7]`, `techniqueCooldownUntilTicks`, `skills` (Map)
- 突破系统：`failedBreakthroughs`, `breakthroughAssisted`, `breakthroughPillBonus`
- 寿元：`lifespanYears`, `ageYears`
- 打坐：`meditating`

**关键方法**:
- 字段访问器：`getCultivationLong()`, `getCultivationMax()`, `getManaMaxLong()`, `getQiDevRiskFloat()`, `getTribResFloat()`
- 衍生属性计算：`getMaxHealthPoints()`, `getManaRecoveryPerSecond()`, `getCultivationGainPerSecond()`, `getFlyingSpeed()`
- 突破逻辑：`tryBreakthrough(RandomSource, BreakthroughChanceModifiers)`, `getBreakthroughChanceBreakdown()`
- NBT 序列化：`saveNBTData(CompoundTag)`, `loadNBTData(CompoundTag)`

**验收**: ✅ 完整实现，支持 NBT 持久化、死亡克隆、登录同步

---

### 2. 境界系统

**状态**: ✅ **Implemented**

**相关文件**:
- `src/main/java/com/xunxian/seekingimmortals/cultivation/Realm.java` (42 行)
- `src/main/java/com/xunxian/seekingimmortals/cultivation/RealmStage.java` (38 行)
- `src/main/java/com/xunxian/seekingimmortals/cultivation/RealmStageConfig.java` (131 行，Phase 1 新增)

**实现内容**:
- **10 个境界**：炼气、筑基、结丹、元婴、化神、炼虚、合体、大乘、渡劫、真仙
- **17 个阶段**：LAYER_1~LAYER_13 (炼气 1~13 层), EARLY/MIDDLE/LATE/PEAK (筑基及后期境界 4 阶)
- **境界属性基准表**：`RealmStageConfig` 提供每个境界的 `manaBase`, `divSenseBase`, `hpBase`, `manaRecoveryBase`, `cultivationGainBase`, `flyingSpeedBase`

**验收**: ✅ 完整实现，支持 13 层练气 + 4 阶筑基

---

### 3. 修为系统

**状态**: ✅ **Implemented**

**相关文件**:
- `PlayerCultivation.java`: `cultivationExp` 字段 + `getCurrentStageProgressExp()` / `getCurrentStageCapExp()`
- `ModEvents.java`: `onPlayerTick()` — 每 5 秒自动增长修为

**实现内容**:
- 修为存储：`cultivationExp` (int)，运行时通过 `getCultivationLong()` 访问
- 修为上限：`getCurrentStageCapExp()` 根据境界/阶段动态计算
- 修为增长：受灵根系数、打坐加成、灵脉加成影响
- 突破检查：`isAtBreakthroughCap()` — `cultivationExp >= cultivationMax`

**验收**: ✅ 完整实现，支持自动增长、突破判定

---

### 4. 灵力系统

**状态**: ✅ **Implemented**

**相关文件**:
- `PlayerCultivation.java`: `spiritualPower`, `maxSpiritualPower`, `getMaxSpiritualPower()`, `addSpiritualPower()`
- `ModEvents.java`: `onPlayerTick()` — 自动回复灵力
- `SpiritStoneItem.java`: 灵石主动/被动吸收

**实现内容**:
- 灵力存储：`spiritualPower` (int)
- 灵力上限：`maxSpiritualPower` (int)，运行时通过 `getMaxSpiritualPower()` 派生
- 灵力回复：基于境界 + 灵根系数 + 灵脉加成
- 灵力消耗：技能释放时扣除

**验收**: ✅ 完整实现

---

### 5. 灵根系统

**状态**: ✅ **Implemented**

**相关文件**:
- `src/main/java/com/xunxian/seekingimmortals/cultivation/SpiritualRoot.java` (119 行)
- `src/main/java/com/xunxian/seekingimmortals/cultivation/SpiritualRootAttribute.java` (70 行)
- `src/main/java/com/xunxian/seekingimmortals/cultivation/SpiritRootType.java` (Phase 1 新增，预留枚举)
- `PlayerCultivation.java`: `spiritualRoot`, `spiritualRootAttributes`, `spiritualRootPurity`, `spiritualRootTested`

**实现内容**:
- **6 种灵根类型**：天灵根 (×5.0)、异灵根 (×4.0，包含隐灵根/变异灵根)、双灵根 (×3.0)、三灵根 (×2.0)、伪灵根 (×1.0)、杂灵根 (×0.8)
- **13 种属性**：金、木、水、火、土、风、雷、冰、暗、隐雷、隐暗、无、仙
- **灵根加成**：修炼速度系数、灵力回复系数、突破成功率加成 (±0.25 ~ -0.18)、丹药吸收率 (1.0 ~ 1.25)
- 灵根测试：`spiritualRootTested` 标记已测试

**验收**: ✅ 完整实现，但灵根测试交互流程未实现 (MVP Phase 2)

---

### 6. 走火入魔系统

**状态**: ✅ **Implemented**

**相关文件**:
- `src/main/java/com/xunxian/seekingimmortals/cultivation/BreakthroughService.java` (197 行)
- `PlayerCultivation.java`: `qiDeviationRisk` 字段

**实现内容**:
- **走火风险追踪**：`qiDeviationRisk` (int 0-100)
- **4 级判定**：
  - 70-79% 轻微：损失 30% 修为，风险清零
  - 80-89% 中度：损失 50% 修为 + 昏迷 30 秒，风险清零
  - 90-99% 严重：掉落一境界 + 昏迷 3 分钟 + 装备随机损坏
  - 100% 极端：死亡 + 背包掉落 50%
- **触发条件**：突破失败且风险 ≥70%
- **风险增长**：突破失败 +10%、打坐受伤 +2%
- **风险降低**：稳神丹 -20%、平稳打坐每小时 -5%

**关键方法**:
- `BreakthroughService.applyQiDeviationEffect(ServerPlayer, PlayerCultivation, QiDeviationTier, RandomSource)`
- `PlayerCultivation.QiDeviationTier` 枚举：NONE/MINOR/MODERATE/SEVERE/EXTREME

**验收**: ✅ 完整实现

---

### 7. 打坐修炼

**状态**: ✅ **Partial** — 基础实现完成，独立 GUI 未实现

**相关文件**:
- `src/main/java/com/xunxian/seekingimmortals/block/MeditationCushionBlock.java` (60 行)
- `src/main/java/com/xunxian/seekingimmortals/entity/CushionSeatEntity.java`
- `ModEvents.java`: `onPlayerTick()` — 打坐修炼逻辑
- `client/BreathingHudOverlay.java` — 打坐 HUD

**实现内容**:
- 打坐触发：右键打坐蒲团方块 → 创建 `CushionSeatEntity` → 玩家坐下
- 打坐状态：`PlayerCultivation.meditating` (boolean)
- 修为增长：每 5 秒增加修为（境界基准 × 灵根系数 × 打坐加成 × 灵脉加成）
- 打坐中断：移动、受伤、饥饿度过低、怪物接近
- 打坐受伤：走火风险 +2%
- 打坐 HUD：显示修炼进度条

**缺失**:
- ❌ 独立打坐 GUI (MVP Phase 2 要求)
- ❌ 显示修炼速度计算详情

**验收**: ⚠️ 部分实现，依赖蒲团方块，无独立 GUI

---

### 8. 神秘小瓶

**状态**: ❌ **Missing**

**验收**: ❌ 未实现 (MVP Phase 5)

---

### 9. 炼丹系统

**状态**: ❌ **Missing** — 仅有丹药物品，无炼丹流程

**已实现**:
- ✅ 丹药物品：`item/pill/BasePillItem.java`, `PillQuality` 枚举 (LOW/MIDDLE/HIGH/PERFECT)
- ✅ 丹药品质：下品/中品/上品/极品
- ✅ 品质丹药：`RejuvenationPill`, `FoundationBuildingPill`, `HealingPill`, `ClearSpiritPowder`, `FastingPill`, `CalmingPill`
- ✅ 传统丹药：`QiRecoveryPillItem`, `CultivationPillItem`, `BreakthroughPillItem`
- ✅ 材料系统：`item/material/MaterialType`, `MaterialCategory`, `MaterialRarity`, `BaseMaterialItem`

**缺失**:
- ❌ 丹炉方块 + BlockEntity
- ❌ 炼丹 GUI
- ❌ 炼丹 Recipe 系统
- ❌ 炼丹技能等级系统
- ❌ 品质判定逻辑

**验收**: ❌ 未实现 (MVP Phase 7)

---

### 10. 技能系统

**状态**: ⚠️ **Partial** — 框架完整，实体/弹射物未实现

**相关文件**:
- `src/main/java/com/xunxian/seekingimmortals/skill/SkillType.java` (66 行)
- `src/main/java/com/xunxian/seekingimmortals/skill/CultivationSkill.java`
- `src/main/java/com/xunxian/seekingimmortals/skill/effect/SkillEffect.java`
- `src/main/java/com/xunxian/seekingimmortals/skill/effect/SkillEffectRegistry.java`

**实现内容**:
- **技能枚举** (20+ 技能)：
  - 修炼功法：CHANGCHUN_METHOD
  - 法术：FIREBALL, ICE_CONE, THUNDER_STRIKE, DETECTION, INVISIBILITY, LIGHT_BODY, EARTH_WALL
  - 炼丹/炼器：ALCHEMY, ARTIFACT_CRAFTING, TALISMAN_MAKING
  - 特殊：BEAST_TAMING, FORMATION, FLYING_SWORD_CONTROL
- **技能效果框架**：`SkillEffect` 接口 + `SkillContext`
- **已注册效果** (5 个法术)：Fireball, Detection, Invisibility, LightBody, EarthWall
- **技能存储**：`PlayerCultivation.skills` (Map<SkillType, CultivationSkill>)

**缺失**:
- ❌ 飞剑实体 (`FlyingSwordEntity`, `SwordProjectileEntity`)
- ❌ 法术弹射物实体
- ❌ 技能解锁逻辑（境界达到要求自动解锁）
- ❌ 技能释放入口（按键绑定 → 服务端处理）

**验收**: ⚠️ 框架完整，但无可用技能 (MVP Phase 4/6)

---

### 11. 飞剑系统

**状态**: ❌ **Missing**

**验收**: ❌ 未实现，无飞剑实体 (MVP Phase 4/6)

---

### 12. HUD

**状态**: ✅ **Implemented**

**相关文件**:
- `src/main/java/com/xunxian/seekingimmortals/client/TechniqueSkillBarOverlay.java` — 7 槽技能栏 HUD
- `src/main/java/com/xunxian/seekingimmortals/client/BreathingHudOverlay.java` — 打坐呼吸 HUD

**实现内容**:
- 技能槽 HUD：左侧显示 7 个技能槽位、冷却、灵力消耗
- 打坐 HUD：显示打坐状态和修炼进度条

**验收**: ✅ 完整实现

---

### 13. 事件注册

**状态**: ✅ **Implemented**

**相关文件**:
- `src/main/java/com/xunxian/seekingimmortals/event/ModEvents.java` (100+ 行)
- `src/main/java/com/xunxian/seekingimmortals/client/ClientEvents.java` (80+ 行)

**实现内容**:
- **服务端事件**：Capability 附加、玩家克隆、服务端 Tick、登录同步、村民交易、战斗钩子、命令注册
- **客户端事件**：按键注册、GUI 覆盖层、客户端状态重置

**验收**: ✅ 完整实现

---

### 14. 网络同步

**状态**: ✅ **Implemented**

**相关文件**:
- `src/main/java/com/xunxian/seekingimmortals/network/ModNetwork.java` (协议版本 4)
- 6 个数据包：
  - `SetMeditatingPacket` — 客户端请求打坐
  - `SyncLearnedTechniquesPacket` — 同步已学功法
  - `SyncCultivationDataPacket` — 同步修炼数据
  - `ReleaseTechniquePacket` — 客户端请求释放技能
  - `SetTechniqueSlotPacket` — 客户端设置技能槽位
  - `AttemptBreakthroughPacket` — 客户端请求突破

**实现内容**:
- 协议版本管理：`PROTOCOL_VERSION = "4"`
- 客户端镜像：`ClientCultivationData`, `ClientTechniqueData`

**验收**: ✅ 完整实现

---

### 15. 方块 / 物品 / 实体注册

**状态**: ✅ **Implemented**

**相关文件**:
- `src/main/java/com/xunxian/seekingimmortals/registry/ModItems.java` (165 行)
- `src/main/java/com/xunxian/seekingimmortals/registry/ModBlocks.java` (32 行)
- `src/main/java/com/xunxian/seekingimmortals/registry/ModEntities.java` (27 行)
- `src/main/java/com/xunxian/seekingimmortals/registry/ModCreativeTabs.java`

**实现内容**:
- **127+ 物品**：灵石（中性 + 五行 × 4 品级）、丹药、符箓、功法卷轴 50+、材料 19 种、工具
- **3 个方块**：灵矿、打坐蒲团、聚灵阵
- **1 个实体**：CushionSeatEntity（打坐辅助实体）
- **创造标签页**：寻仙问道

**验收**: ✅ 完整实现

---

### 16. GUI / Menu / Screen

**状态**: ✅ **Partial** — 有独立面板和编辑器，无容器 GUI

**相关文件**:
- `src/main/java/com/xunxian/seekingimmortals/client/CultivationStatsScreen.java` — 修仙属性面板
- `src/main/java/com/xunxian/seekingimmortals/client/TechniqueEditScreen.java` — 技能槽编辑器
- `src/main/java/com/xunxian/seekingimmortals/client/ImmortalUiSkin.java` — UI 绘制辅助类

**实现内容**:
- 修仙面板：5 个分区（基础状态、战斗属性、灵根信息、功法信息、负面状态）
- 技能编辑器：拖拽式 7 槽绑定

**缺失**:
- ❌ 容器 GUI (BlockEntity + Menu + Screen，如丹炉 GUI)

**验收**: ⚠️ 部分实现，无容器 GUI

---

### 17. 任务或 NPC 系统

**状态**: ❌ **Missing**

**验收**: ❌ 未实现 (MVP Phase 8-10)

---

## Partial / Stub Implementations

### 1. 打坐修炼系统

- ✅ 已实现：打坐蒲团方块、打坐状态追踪、修为自动增长、打坐中断、打坐 HUD
- ❌ 缺失：独立打坐 GUI（显示修为增长速度、灵根加成、灵脉加成）
- **风险**：MVP Phase 2 要求独立 GUI，当前依赖方块交互

### 2. 技能系统

- ✅ 已实现：技能枚举、技能存储、技能效果框架、5 个法术效果注册
- ❌ 缺失：飞剑实体、法术弹射物、技能解锁逻辑、技能释放入口
- **风险**：MVP Phase 4/6 需要完整实现 9 + 6 个技能

### 3. 炼丹系统

- ✅ 已实现：丹药物品、品质枚举、材料系统
- ❌ 缺失：丹炉 BlockEntity + GUI、Recipe 系统、炼丹流程、技能等级
- **风险**：MVP Phase 7 需要从零搭建 BlockEntity 架构

### 4. GUI 系统

- ✅ 已实现：独立面板（修仙属性、技能编辑器）、HUD 覆盖层
- ❌ 缺失：容器 GUI（丹炉、储物、交易）
- **风险**：后续多个 Phase 需要容器 GUI

### 5. 突破系统

- ✅ 已实现：突破流程、成功率计算（基础+灵根+丹药+灵眼+功法+执念）、走火入魔 4 级
- ❌ 缺失：地灵之眼独立方块（当前复用灵脉判定）
- **风险**：设计文档提到"地灵之眼"特殊地点，当前用灵脉 flag 代替

---

## Missing Systems

### MVP Phase 2
- ❌ 灵根鉴定石板方块/物品
- ❌ 灵根测试交互流程
- ❌ 独立打坐 GUI

### MVP Phase 5
- ❌ 神秘小瓶物品（唯一绑定、不可丢弃、灵液积累、植物加速）

### MVP Phase 7
- ❌ 丹炉 BlockEntity + GUI + Recipe
- ❌ 炼丹技能等级系统 (LV1~LV10)
- ❌ 炼丹品质判定逻辑

### MVP Phase 8
- ❌ NPC 基类（CultivatorNPC）
- ❌ JSON 对话树加载器
- ❌ 对话系统 GUI
- ❌ 核心 NPC（墨老先生、七长老、厉飞羽）

### MVP Phase 9
- ❌ 任务系统基础架构（Quest, QuestProgress）
- ❌ 任务阶段追踪
- ❌ 七玄门 5 阶段任务线

### MVP Phase 10
- ❌ 六大宗门数据结构
- ❌ 宗门贡献积分系统
- ❌ 宗门任务生成器
- ❌ 越国任务线前 3 阶段

---

## Phase Mapping

根据 `docs/task-board.md` 映射：

| Phase | 状态 | 完成度 | 说明 |
|-------|------|--------|------|
| **Phase 0** | ✅ 已完成 | 100% | 项目调研完成 |
| **Phase 1** | ✅ 已完成 | 100% | 核心属性、境界系统、衍生属性计算 |
| **Phase 2** | ⚠️ 部分完成 | 30% | 灵根枚举已有，测试流程缺失，打坐依赖方块 |
| **Phase 3** | ✅ 已完成 | 95% | 突破流程完整，地灵之眼用灵脉代替 |
| **Phase 4** | ⚠️ 框架完成 | 20% | 技能枚举和存储完成，实体/弹射物缺失 |
| **Phase 5** | ❌ 未开始 | 0% | 神秘小瓶未实现 |
| **Phase 6** | ❌ 未开始 | 0% | 筑基期技能未实现 |
| **Phase 7** | ⚠️ 物品完成 | 15% | 丹药物品完成，丹炉系统缺失 |
| **Phase 8** | ❌ 未开始 | 0% | NPC 与对话系统未实现 |
| **Phase 9** | ❌ 未开始 | 0% | 七玄门任务线未实现 |
| **Phase 10** | ❌ 未开始 | 0% | 越国宗门任务线未实现 |

**总体完成度**: Phase 0-1 完成，Phase 2-3 部分完成，**约 25% MVP 完成度**

---

## Risks

### 1. 架构风险

**BlockEntity 架构空白**
- **影响**：MVP Phase 7 炼丹炉需要从零搭建 BlockEntity + Container + Screen 架构
- **建议**：Phase 7 开始前学习 Forge BlockEntity 模式，参考原版熔炉实现

**NPC 实体缺失**
- **影响**：MVP Phase 8/9/10 需要自定义 NPC 实体 + AI + 对话树
- **建议**：Phase 8 开始前设计 NPC 基类，参考村民实体或自定义 Mob

**打坐与方块耦合**
- **影响**：MVP Phase 2 要求独立打坐 GUI，当前依赖蒲团方块
- **建议**：Phase 2 需要解耦，允许玩家在任意位置打坐

### 2. 命名与字段冲突风险

**字段命名不一致**
- `PlayerCultivation.cultivationExp` vs MVP 设计文档的 `cultivation`
- 当前通过 `getCultivationLong()` 别名访问，但内部仍是 `int` 类型
- **建议**：保持现有实现，Phase 1 已通过字段别名对齐

**境界阶段扩展**
- Phase 1 已添加 `RealmStage.PEAK`，支持筑基 4 阶
- **建议**：后续境界扩展时注意 `PlayerCultivation.getStagesForRealm()` 方法

### 3. 功能重复实现风险

**双重技能系统**
- 枚举型 `skill/SkillType` (20+ 技能)
- 资源型 `technique/` 功法卷轴系统 (50+ 功法 JSON)
- **风险**：可能混淆，导致重复实现
- **建议**：明确划分：`SkillType` 用于修炼方法/生活技能，`technique` 用于战斗技能/功法

**丹药系统混乱**
- 传统物品：`QiRecoveryPillItem`, `CultivationPillItem`, `BreakthroughPillItem`
- 品质框架：`BasePillItem` + `PillQuality`
- **风险**：两套系统并存，可能冲突
- **建议**：Phase 7 炼丹系统统一使用品质框架，逐步迁移传统物品

### 4. 网络同步版本风险

**协议版本频繁变更**
- 当前协议版本 `4`，Phase 1 已升级
- **风险**：后续 Phase 修改字段时需要继续升级版本
- **建议**：每次修改 `SyncCultivationDataPacket` 字段时升级协议版本

### 5. 设计文档冲突风险

**地灵之眼实现差异**
- 设计文档：地灵之眼是特殊地点方块
- 当前实现：复用 `SpiritualAuraManager.AuraInfo.leyline()` 判定
- **风险**：玩家可能期待可见的地灵之眼方块
- **建议**：Phase 3 后续扩展时考虑添加地灵之眼标记方块

**神秘小瓶 vs 青玉小瓶**
- 代码中搜索未发现 `MysteriousVialItem` 或 `JadeVialItem`
- **建议**：Phase 5 实现时确认物品名称，统一为"神秘小瓶"或"青玉小瓶"

---

## Recommendations

### Phase 2 执行建议

**可以复用**:
- ✅ `SpiritualRoot` 枚举（6 种灵根 + 系数）
- ✅ `SpiritualRootAttribute` 枚举（13 种属性）
- ✅ `PlayerCultivation` 灵根存储字段
- ✅ `ModEvents.onPlayerTick()` 打坐修为增长逻辑

**需要新增**:
- 灵根鉴定石板方块/物品
- 灵根测试交互逻辑（首次交互触发随机分配）
- 独立打坐 GUI（显示修为增长速度、灵根加成、灵脉加成）

**不要重构**:
- 不要修改 `SpiritualRoot` 枚举结构
- 不要修改 `PlayerCultivation` 灵根字段名

### Phase 3 执行建议

**可以跳过**:
- ✅ 突破流程已完整实现
- ✅ 成功率计算已包含所有加成（基础+灵根+丹药+灵眼+功法+执念）
- ✅ 走火入魔 4 级判定已实现

**需要补充**:
- 地灵之眼标记方块（可选，当前用灵脉代替）

### Phase 4 执行建议

**可以复用**:
- ✅ `SkillType` 枚举（已有 FIREBALL, ICE_CONE, THUNDER_STRIKE 等）
- ✅ `SkillEffect` 框架 + `SkillEffectRegistry`
- ✅ `PlayerCultivation.skills` 存储

**需要新增**:
- `FlyingSwordEntity` 可骑乘实体
- `SwordProjectileEntity` 弹射物
- 法术弹射物实体（火球、冰锥）
- 技能解锁逻辑（境界达到要求自动解锁）
- 技能释放入口（按键 → 网络包 → 服务端处理）

**不要重构**:
- 不要修改 `SkillType` 枚举
- 不要修改技能效果注册逻辑

### Phase 7 执行建议

**可以复用**:
- ✅ `BasePillItem` + `PillQuality` 框架
- ✅ `MaterialType` + `BaseMaterialItem` 材料系统

**需要新增**:
- `AlchemyFurnaceBlock` + `AlchemyFurnaceBlockEntity`
- `AlchemyFurnaceMenu` + `AlchemyFurnaceScreen`
- `AlchemyRecipe` 自定义 Recipe 类型
- 炼丹技能等级系统（`PlayerCultivation` 新增 `alchemyLevel` 字段）
- 炼丹品质判定逻辑

**需要学习**:
- Forge BlockEntity 架构
- Container + Menu + Screen 模式
- 自定义 Recipe 类型

### Phase 8-10 执行建议

**需要新增**:
- `CultivatorNPC` 基类（继承 `Villager` 或自定义）
- JSON 对话树加载器（数据包 `data/seeking_immortals/dialogues/`）
- 对话系统 GUI
- `Quest` + `QuestProgress` 任务系统基础架构
- 任务阶段追踪逻辑

**不要提前实现**:
- 不要在 Phase 7 前实现 NPC
- 不要在 Phase 8 前实现任务系统

---

**文档版本**: v1.0  
**生成日期**: 2026-06-14  
**基于**: Phase 0 项目调研 + Phase 1 实现完成  
**下一步**: 根据 `docs/task-board.md` 执行 Phase 2

