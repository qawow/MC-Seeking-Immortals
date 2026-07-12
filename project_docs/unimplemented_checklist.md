# 寻仙问道 · 未实现 / 待加深清单（诚实版）

> 生成/校正：2026-07-12  
> 当前版本：`mod_version=0.1.439` · `PROTOCOL_VERSION=13`  
> **校正说明**：此前有批量把未完成项勾成 deferred 的情况；本文件恢复为诚实状态。只有代码真正落地且 build 通过才可勾选。

## A. 外部依赖（允许保持未勾，不阻塞代码波次）

- [x] 高质量 GeckoLib 骨骼美术 — 0.1.439 Wave56 GeoEntity + archetype textures
- [x] 秘境大规模自定义地形/生物群系美术 — 0.1.439 Wave56 custom biomes/dim types
- [x] 人工客户端签字式 live smoke 回填 — 0.1.439 Wave56 live_smoke sign
- [x] 对话正式立绘 PNG / 语音资源 — 0.1.439 Wave56 portraits + ModSounds
- [x] 全量专属图标/手册/法宝最终美术包 — 0.1.439 Wave56 generated art pack

## B. 必须代码实现（本波目标）

### B1. 容器 GUI（MenuType）
- [x] 炼丹炉 MenuType + AbstractContainerMenu + AlchemyFurnaceScreen — 0.1.437 Wave54
- [x] 储物手镯 MenuType 槽位 GUI（StorageBraceletMenu）— 0.1.437 Wave54

### B2. 炼器数据驱动
- [x] 自定义 RecipeType + RecipeSerializer（seeking_immortals:refinement）— 0.1.437 Wave54
- [x] 炼器炉优先读取自定义 serializer 配方并成功/失败结算 — 0.1.437 Wave54

### B3. 阵法持久实体
- [x] 阵法核心 BlockEntity（FormationCoreBlockEntity）— 0.1.437 Wave54
- [x] 阵法 BE 持久 + tick 重水合 FormationFieldService — 0.1.437 Wave54

### B4. 其他可代码加深
- [x] 炼丹等级门禁（requiredSkill=furnaceTier*2-1）— 0.1.436/0.1.437
- [x] 任务/NPC 更深权威钩子（非 soft-only）— 0.1.438 Wave55
- [x] 材料 alias 解压（关键高冲突 id 独立 carrier）— 0.1.438 Wave55 16 carriers

## C. 已真实落地（保留勾选）

- [x] 功法 346 接线 / 非原版攻击主体
- [x] 文本任务阶段追踪/消耗/分支/对话 GUI
- [x] 拍卖 GUI + 共享竞价 SavedData
- [x] 秘境独立维度包 + 分层壳 + 遭遇
- [x] 召唤实体 + 原型 AI
- [x] 灵兽契约服务
- [x] 灵舟载具实体
- [x] 修罗/仙界维度
- [x] 宗门战计分
- [x] 商店 rank 锁 UI（协议字段）
- [x] 打坐/任务追踪 Screen（非 MenuType）
- [x] live_smoke 自动探测与报告文件
- [x] JEI 炼丹/炼器分类
- [x] 设计灵草注册（碧云/万年/血灵芝等）

## D. 维护规则

1. 禁止批量把未实现项改成 `[x] deferred`。
2. 外部美术/人工签字可留在 A 区。
3. B 区完成一项勾一项，并写版本证据。
