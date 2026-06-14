# Phase 0: 项目结构调研报告

**调研日期**: 2026-06-13  
**版本**: 0.1.47  
**平台**: Minecraft 1.20.1 + Forge 47.2.0 + Java 17  
**代码规模**: 84 个 Java 源文件，17 个包

---

## 1. Mod 主类

**文件**: `src/main/java/com/xunxian/seekingimmortals/SeekingImmortalsMod.java`

- **Mod ID**: `seeking_immortals`
- **职责**:
  - 在 mod 事件总线上注册物品、方块、实体
  - 注册创造标签页
  - 注册网络通道

---

## 2. 包结构（17 个包）

```
com.xunxian.seekingimmortals/
├── block/                    # 2 个方块类
├── client/                   # 9 个客户端类（屏幕、覆盖层、渲染器）
├── combat/                   # 战斗属性/计算器
├── command/                  # 命令系统
├── compat/                   # Mod 兼容性检测
├── cultivation/              # 核心修炼系统（15+ 类）
├── entity/                   # 1 个实体（CushionSeatEntity）
├── event/                    # 服务端/通用事件处理器
├── item/                     # 14 个物品类 + 2 个子目录
│   ├── material/            # 材料系统
│   └── pill/                # 丹药品质系统
├── network/                  # 7 个网络包
├── registry/                 # ModItems、ModBlocks、ModEntities、ModCreativeTabs
├── skill/                    # 技能系统（枚举+效果）
│   └── effect/spell/        # 法术实现
└── spiritual/                # 灵气系统
```

---

## 3. 注册系统

### 3.1 物品注册

**文件**: `src/main/java/com/xunxian/seekingimmortals/registry/ModItems.java` (165 行)

- 使用 `DeferredRegister<Item>` 模式
- 已注册 **127+ 物品**:
  - 灵石：中性 + 五行（金木水火土）× 4 品级（下/中/上/极）
  - 丹药：凝气丹、筑基丹、破境丹、疗伤丹等
  - 符箓：火符、护甲符、速度符
  - 功法卷轴：50+ 种（青元剑诀、六道真经、妖族功法等）
  - 材料：19 种（灵草、兽材、矿物、特殊材料）
  - 工具：灵根测试石、灵力探测器、地脉罗盘
- 辅助方法：
  - `registerTechniqueManual()` — 批量注册功法卷轴
  - `registerSpiritStone()` — 批量注册灵石
  - `registerMaterial()` — 批量注册材料

### 3.2 方块注册

**文件**: `src/main/java/com/xunxian/seekingimmortals/registry/ModBlocks.java` (32 行)

- **3 个方块**:
  - `SPIRIT_ORE` — 灵矿
  - `MEDITATION_CUSHION` — 打坐蒲团
  - `SPIRIT_GATHERING_ARRAY` — 聚灵阵

### 3.3 实体注册

**文件**: `src/main/java/com/xunxian/seekingimmortals/registry/ModEntities.java` (27 行)

- **1 个实体**:
  - `CUSHION_SEAT` — 打坐蒲团的隐形辅助实体（用于坐下）

### 3.4 创造标签页

**文件**: `src/main/java/com/xunxian/seekingimmortals/registry/ModCreativeTabs.java`

- 单一创造标签页：`寻仙问道`
- 包含所有 mod 物品

---

## 4. 事件系统

**文件**: `src/main/java/com/xunxian/seekingimmortals/event/ModEvents.java`

### 服务端 Tick 事件

- 修炼经验与灵力增长
- 打坐逻辑与中断检测（移动/饥饿/怪物接近）
- 灵石吸收（被动加成与主动吸收）
- 负面状态效果（重伤/心魔/核心破碎/境界跌落疤痕）
- 年龄与寿元追踪

### 其他事件

- **Capability 附加**: `AttachCapabilitiesEvent<Entity>` — 为玩家附加修炼数据
- **玩家克隆**: `PlayerEvent.Clone` — 重生/维度切换时复制数据
- **战斗钩子**: `LivingHurtEvent` — 处理修真战斗伤害
- **登录事件**: 同步数据、赠送引导书
- **村民交易**: 灵石兑换（每日限额）
- **命令注册**: `RegisterCommandsEvent`

---

## 5. 现有 Capability 系统

### 5.1 核心 Capability

**Provider 文件**: `src/main/java/com/xunxian/seekingimmortals/cultivation/CultivationProvider.java`

- 标准 Forge Capability 提供器
- 实现 `INBTSerializable<CompoundTag>` 持久化
- 通过 `AttachCapabilitiesEvent` 自动附加到玩家

**Helper 文件**: `src/main/java/com/xunxian/seekingimmortals/cultivation/CultivationHelper.java`

- 安全的 Capability 访问封装

### 5.2 数据类

**文件**: `src/main/java/com/xunxian/seekingimmortals/cultivation/PlayerCultivation.java` (400+ 行)

#### 当前字段（与 MVP 对比）

**✅ 六大核心属性（已实现）**:
- `cultivationExp` (int) — MVP: `cultivation` (long) 
- `spiritualPower` / `maxSpiritualPower` (int) — MVP: `mana` / `manaMax` (int)
- `divineConsciousness` (int) — MVP: `divSense` (int)
- `bodyRefinement` (int) — MVP: `bodyRef` (int)
- `qiDeviationRisk` (int 0-100) — MVP: `qiDevRisk` (float 0-100)
- `tribulationResistance` (int 0-100) — MVP: `tribRes` (float 0-100)

**❌ 缺失字段**:
- MVP 要求显式 `cultivationMax` (long)，当前通过 `getCurrentStageCapExp()` 计算

**📦 扩展字段（超出 MVP 范围）**:
- 境界/阶段：`realm`, `stage`
- 灵根系统：`spiritualRoot`, `spiritualRootAttributes`, `spiritualRootPurity`, `spiritualRootTested`
- 特殊体质：`specialPhysique`
- 负面状态：`severeInjury`, `heartDemonLevel`, `shatteredCore`, `realmFallScars`
- 功法/技能：`learnedTechniques`, `techniqueSlots[7]`, `techniqueCooldownUntilTicks`, `skills` map
- 突破系统：`failedBreakthroughs`, `breakthroughAssisted`, `breakthroughPillBonus`
- 寿元系统：`lifespanYears`, `ageYears`

---

## 6. 境界与灵根系统

### 6.1 境界系统

**文件**: `src/main/java/com/xunxian/seekingimmortals/cultivation/Realm.java` (42 行)

- **10 个境界**（与 MVP 对齐）:
  - 炼气期 (QI_REFINING)
  - 筑基期 (FOUNDATION_ESTABLISHMENT)
  - 结丹期 (CORE_FORMATION)
  - 元婴期 (NASCENT_SOUL)
  - 化神期 (SOUL_TRANSFORMATION)
  - 炼虚期 (VOID_REFINEMENT)
  - 合体期 (UNITY)
  - 大乘期 (MAHAYANA)
  - 渡劫期 (TRIBULATION)
  - 真仙 (TRUE_IMMORTAL)

**文件**: `src/main/java/com/xunxian/seekingimmortals/cultivation/RealmStage.java` (37 行)

- **16 个阶段**:
  - `LAYER_1` ~ `LAYER_13` — 炼气期 1~13 层
  - `EARLY`, `MIDDLE`, `LATE` — 筑基期及后期境界的早/中/后期

**⚠️ 缺口**: MVP 要求筑基 4 阶（早/中/后/圆满），当前只有 3 阶。

### 6.2 灵根系统

**文件**: `src/main/java/com/xunxian/seekingimmortals/cultivation/SpiritualRoot.java` (119 行)

- **六种类型**（完全对齐 MVP）:
  - 天灵根 (HEAVENLY) — 修炼速度 5.0×
  - 异灵根 (HIDDEN) — 修炼速度 3.0×
  - 双灵根 (DUAL) — 修炼速度 1.8×
  - 三灵根 (TRIPLE) — 修炼速度 1.2×
  - 伪灵根 (FALSE_ROOT) — 修炼速度 1.0×
  - 杂灵根 (MIXED) — 修炼速度 0.8×

**文件**: `src/main/java/com/xunxian/seekingimmortals/cultivation/SpiritualRootAttribute.java` (70 行)

- **13 种属性**:
  - 五行：金、木、水、火、土
  - 变异：风、雷、冰、暗
  - 隐藏：隐雷、隐暗
  - 特殊：无、仙

---

## 7. 网络同步系统

**文件**: `src/main/java/com/xunxian/seekingimmortals/network/ModNetwork.java`

- **协议版本**: 4
- **已注册数据包**:
  - `SetMeditatingPacket` — 设置打坐状态
  - `SyncLearnedTechniquesPacket` — 同步已学功法
  - `SyncCultivationDataPacket` — 同步修炼数据（六大属性、境界、灵根等）
  - `ReleaseTechniquePacket` — 释放功法
  - `SetTechniqueSlotPacket` — 设置功法槽位
  - `AttemptBreakthroughPacket` — 尝试突破

**客户端镜像**:
- `src/main/java/com/xunxian/seekingimmortals/client/ClientCultivationData.java` — 修炼数据快照
- `src/main/java/com/xunxian/seekingimmortals/client/ClientTechniqueData.java` — 功法数据快照

---

## 8. GUI 系统

**客户端类** (位于 `client/` 包):

1. **CultivationStatsScreen** — 修仙属性面板（5 个分区）
2. **TechniqueSkillBarOverlay** — 7 个功法槽 HUD
3. **BreathingHudOverlay** — 打坐呼吸 HUD 与进度条
4. **TechniqueEditScreen** — 拖拽式功法槽位编辑器
5. **ImmortalUiSkin** — 共享 UI 绘制辅助类

**按键绑定** (注册于 `ClientEvents`):
- 默认打坐键: `V`
- 默认突破键: 未绑定
- 7 个功法释放键: 默认未绑定
- 功法编辑键: 默认未绑定

---

## 9. BlockEntity 现状

**❌ 状态**: 未找到任何 BlockEntity 实现

**影响**:
- 无法实现复杂方块逻辑（炼丹炉 GUI、聚灵阵存储）
- 当前 `SPIRIT_GATHERING_ARRAY` 只是简单方块，无 GUI 或数据存储
- MVP Phase 7 炼丹炉需要全新 BlockEntity + Container + Screen 架构

---

## 10. Phase 1 实现 CultivationData Capability 时可能需要修改的文件

### 10.1 核心数据类（必改）

1. **`src/main/java/com/xunxian/seekingimmortals/cultivation/PlayerCultivation.java`**
   - 对齐字段命名：`cultivationExp` → `cultivation` (考虑 long 类型)
   - 添加显式 `cultivationMax` 字段或文档化计算方法
   - 类型对齐：`qiDeviationRisk` (int → float), `tribulationResistance` (int → float)

### 10.2 Capability 提供器（可能改）

2. **`src/main/java/com/xunxian/seekingimmortals/cultivation/CultivationProvider.java`**
   - 如果新增字段或更改序列化格式，需更新 NBT 读写逻辑

### 10.3 网络同步（必改）

3. **`src/main/java/com/xunxian/seekingimmortals/network/SyncCultivationDataPacket.java`**
   - 如果字段类型变化（int → long/float），需更新 encode/decode
   - 协议版本需从 4 升到 5

4. **`src/main/java/com/xunxian/seekingimmortals/network/ModNetwork.java`**
   - 升级协议版本号

5. **`src/main/java/com/xunxian/seekingimmortals/client/ClientCultivationData.java`**
   - 更新客户端快照字段类型

### 10.4 事件处理器（可能改）

6. **`src/main/java/com/xunxian/seekingimmortals/event/ModEvents.java`**
   - 如果修炼增长公式使用新字段，需调整服务端 Tick 逻辑

### 10.5 GUI 显示（可能改）

7. **`src/main/java/com/xunxian/seekingimmortals/client/CultivationStatsScreen.java`**
   - 如果新增显示字段或重命名，需更新 UI 标签

8. **`src/main/java/com/xunxian/seekingimmortals/command/SeekingImmortalsCommand.java`**
   - 如果命令输出使用新字段名，需更新

---

## 11. MVP 对齐度总结

### ✅ 已实现（与 MVP 对齐）

- 六大核心属性存储（字段名/类型需细化）
- 10 境界 + 多阶段系统
- 六种灵根 + 13 属性完整实现
- 127+ 物品注册
- 网络同步框架（协议版本 4）
- 修仙属性面板与 HUD
- 打坐系统（基于蒲团）
- 战斗属性派生
- 突破流程与成功率加成（刚实现）

### ❌ 缺失（MVP 要求但未实现）

- 独立打坐 GUI（当前依赖蒲团方块）
- NPC 系统（墨老先生、对话树、任务追踪）
- 炼丹炉（BlockEntity + Container + GUI + 配方）
- 灵根测试交互流程
- 神秘玉净瓶（MVP Phase 5）
- 任务系统基础架构

### ⚠️ 需要细化

- 筑基阶段缺"圆满"（当前只有早/中/后）
- 修为字段命名与类型（`cultivationExp` int → `cultivation` long）
- 走火风险类型（int → float）

---

## 12. 技术债务与风险

### 架构风险

1. **BlockEntity 空白** — MVP Phase 7 炼丹炉需从零搭建
2. **NPC 实体缺失** — Phase 8/9 需自定义 NPC 实体 + 对话树
3. **打坐与方块耦合** — MVP 要求随时可用的打坐 GUI

### 范围风险

- 约 50% 功能缺口（MVP Phase 1-10）
- 复杂特性交叉依赖（筑基困局需 NPC + 任务 + 炼丹炉 + 玉净瓶）

---

## 13. 建议下一步

### Phase 1 启动前准备

1. **字段对齐** — 优先处理字段命名和类型：
   - `cultivationExp` → `cultivation` (考虑 long 类型)
   - 添加 `cultivationMax` 或文档化计算方法
   - `qiDeviationRisk` / `tribulationResistance` (int → float)

2. **境界阶段补充** — 添加 `RealmStage.PEAK` 用于筑基圆满

3. **创建备份** — 每个 Phase 前创建 `.bak/<timestamp>/` 备份

4. **协议版本规划** — 字段类型变化时升级网络协议版本

### Phase 2-3 准备

- 设计独立打坐 GUI 架构（与蒲团分离）
- 验证/完善突破处理器逻辑
- 实现 4 级走火入魔效果（70-79%/80-89%/90-99%/100%）

### Phase 7-8 准备

- 学习 Forge BlockEntity + Container 模式
- 设计 NPC 实体基类
- 设计 JSON 对话树格式

---

**调研状态**: ✅ Phase 0 完成  
**架构冲突**: 无阻塞性冲突  
**建议路径**: 先字段对齐，再进入 Phase 1 功能开发
