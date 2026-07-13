# 寻仙问道 · 未实现 / 待加深清单（诚实版）

> 生成/校正：2026-07-12
> 当前版本：`mod_version=0.1.486` · `PROTOCOL_VERSION=17`
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


## E. 2026-07-13 任务板 1→5 加深证据

- [x] Wave1 任务权威 0.1.457 — tracker UI/catalog rewards/main-story
- [x] Wave2 召唤深度 0.1.458 — stance/dismiss/repair/combat growth/jar seal
- [x] Wave3 法宝炼器 0.1.459 — grade select/salvage/natal/integrity
- [x] Wave4 秘境实例 0.1.460 — mid patrol/hazards/layered loot/boss cache
- [x] Wave5 文档/smoke 收口 0.1.460 — open decisions + checklist sync

## F. 2026-07-13 Wave466 加深证据

- [x] TechniqueGateService 学/放境界门禁 0.1.466
- [x] Soft→权威命令接线（story/summon/talisman/puppet）0.1.466
- [x] 拍卖超价退款 + 冲突真实声望推进 0.1.466
- [x] 黄枫/掩月/星宫专属任务商店对话 0.1.466

## G. 2026-07-13 Wave467 加深证据

- [x] TechniqueGate method + rep 0.1.467
- [x] 拍卖离线超价退款 ledger + login claim 0.1.467
- [x] 鬼灵/天魔/血巫/万狐专属任务商店对话 0.1.467

## H. 2026-07-13 Wave468 加深证据

- [x] TechniqueGate region/dimension cast affinity 0.1.468
- [x] 清虚/千竹/灵兽山/逆星/巨剑专属任务商店对话 0.1.468

## I. 2026-07-13 Wave469 加深证据

- [x] 合欢/落云/天岚/七玄/木兰法师/大晋佛寺专属任务商店对话 0.1.469

## J. 2026-07-13 Wave470 加深证据

- [x] 化道武/魔焰门/千幻/青岚/青萝/天阙/天煞专属任务商店对话 0.1.470
- [x] 全 playable 宗门去通用草芝参模板壳（药园向黄枫/落云除外）0.1.470

## K. 2026-07-14 Wave471 加深证据

- [x] 秘境击杀门：中层巡逻解锁中层箱 0.1.471
- [x] 核心守护者解锁核心箱 + 稀有掉落 0.1.471
- [x] Boss 宝箱击杀后生成（不再进门预放）0.1.471

## L. 2026-07-14 Wave472 加深证据

- [x] 召唤 BEAST 契约门禁 + 灵石碎片消耗 0.1.472
- [x] 秘境停留周期 hazard tick 0.1.472

## M. 2026-07-14 Wave473 加深证据

- [x] 功法 method 权威学习 NBT + learnMethod API 0.1.473
- [x] TechniqueGate 识别 learned methods 0.1.473
- [x] /catalog methods learn|studied 命令 0.1.473

## N. 2026-07-14 Wave474 加深证据

- [x] 手册 study unlocks 自动授予 learned methods 0.1.474
- [x] 宗门外门晋升授予入门功法 0.1.474

## O. 2026-07-14 Wave475 加深证据

- [x] 术法手册 source→method 自动授予 0.1.475
- [x] 宗门入门功法 id 对齐 methods index 0.1.475

## P. 2026-07-14 Wave476 加深证据

- [x] CatalogManualItem study 映射授予 methods 0.1.476
- [x] CultivationStatsScreen 功法目录提示 0.1.476

## Q. 2026-07-14 Wave477 加深证据

- [x] SyncLearnedMethodsPacket + protocol 14 0.1.477
- [x] ClientMethodData + 修仙 UI 已学功法展示 0.1.477
- [x] 登录/授予自动同步 learned methods 0.1.477

## R. 2026-07-14 Wave478 加深证据

- [x] MethodActionPacket learn/sync + protocol 15 0.1.478
- [x] MethodTreeScreen 交互式功法目录/修习 0.1.478
- [x] CultivationStatsScreen 功法入口按钮 0.1.478

## S. 2026-07-14 Wave479 加深证据

- [x] 编年全覆盖映射 + 首次发现奖励/声望 0.1.479
- [x] 商路商会税收折扣 + 完成结算利润 0.1.479
- [x] 冲突接取开启 SectWar + 选边计分 0.1.479

## T. 2026-07-14 Wave480 加深证据

- [x] SummonedServitor 敌对 trial 模式（无主人、猎玩家）0.1.480
- [x] TrialCombatShellService 分种映射 + 生成 0.1.480
- [x] 秘境巡逻/守护/首领改用分种战斗壳 0.1.480

## U. 2026-07-14 Wave481 加深证据

- [x] SpiritCharm ICurioItem 完整 Curios 佩戴/回灵 0.1.481
- [x] 功法层数 1-9 权威精进 + 消耗 0.1.481
- [x] SyncLearnedMethodsPacket 层数同步 protocol 16 0.1.481
- [x] MethodTreeScreen 精进 UI 0.1.481

## V. 2026-07-14 Wave482 加深证据

- [x] FTB 打包物品任务 consume_items=true（66）0.1.482
- [x] FTB data.snbt default_consume_items=true 0.1.482
- [x] 主线 FTB 物品任务 item rewards 0.1.482
- [x] FtbQuestSnbtTest 消耗权威断言 0.1.482

## W. 2026-07-14 Wave483 加深证据

- [x] MethodTreeScreen 层数节点连线 1-9 0.1.483
- [x] MethodTreeScreen 流派相邻节点图 0.1.483
- [x] FTB 全部 66 物品任务 item rewards 0.1.483
- [x] FtbQuestSnbtTest 全量 rewards 覆盖 0.1.483

## X. 2026-07-14 Wave484 加深证据

- [x] MethodTreeScreen 可点选多节点流派网格图 0.1.484
- [x] FTB 战斗节点 kill 任务（6）0.1.484
- [x] FtbQuestSnbtTest 支持 kill 类型 0.1.484

## Y. 2026-07-14 Wave485 加深证据

- [x] MethodTreeScreen 自由拖拽节点布局 + 复位 0.1.485
- [x] SectWar 战场 AI 压力壳脉冲 + 击杀计分 0.1.485
- [x] FTB advancement 任务（8）0.1.485
- [x] FtbQuestSnbtTest 支持 advancement 类型 0.1.485
