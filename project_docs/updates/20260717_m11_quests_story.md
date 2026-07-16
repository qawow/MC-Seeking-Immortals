# 2026-07-17 M11 任务与主线落地

## 摘要

落地 M11 任务体系全量数据与运行时：62 任务链 schema18 索引/enrich、35 条 QUEST_LINES_FULL_v147 章节化服务、主线章节推进器（境界/区域/声望/秘境通关门槛）、任务钩子接线（对话/M06 每日/M09 通关/击杀/采集/炼制）、主线奖励与唯一剧情道具幂等账本、韩立时间线/编年史记录服务、FTB 章节↔原生链映射维护、任务追踪包容量扩到 72 行以覆盖 62 链并发追踪。

## 变更类

- 代码 + 语料发布资源 + 文档
- **不升** `mod_version`（任务红线，保持 0.1.507）
- **不升** `ModNetwork.PROTOCOL_VERSION`（保持 21；`SyncQuestTrackerPacket` 字段/顺序未改，仅编码上限常量提升）

## 语料发布（text_material / catalog）

- 发布/刷新：`quest_chains.json`、`quest_chains_playable_v141.json`、`main_story_chapters.json`、`main_story_quest_map_v145.json`、`main_quest_rewards_v101.json`、`quest_lines_full_descriptions_v147.json`、`hanli_timeline_items_v100.json`、`timeline_guide_v115.json`、`chronicle_events.json`、faction quest networks、endgame tables 等
- 新建/刷新索引：`quest_chains_index.json`（schema 19 + step_hooks/alchemy/skill_tree）、`quest_lines_full_index.json`、`main_story_quest_map_index.json`、`main_quest_rewards_index.json`、`hanli_timeline_index.json`、`quest_chains_playable_index.json`、`dialogue_effect_quest_links_index.json`、`chronicle_events_index.json`

## Java

### 新增

- `quest/QuestLineService` — 35 线 + 章节索引 + 交叉引用校验
- `quest/QuestRewardService` — main_quest_rewards_v101 + unique 全局幂等账本
- `quest/TimelineChronicleService` — 韩立时间线阶段 + 编年史记录
- `quest/QuestHookRuntime` — DialogueNodeReachedEvent / LivingDeath / craft / pickup / M06 daily / M09 clear
- `quest/FtbQuestBridgeService` — 9 FTB 章 ↔ 62 原生链映射（FTB 缺席时纯查询不崩）
- `quest/M11QuestCorpusTest`

### 增强

- `ExtendedCatalogService.QuestChain` 增 `stepHooks/alchemyLoopRef/skillTreeRef`；`StoryChapter` 增 `questChainRefs`
- `MainStorySoftService` 数据驱动 chain refs + 解锁门槛
- `TextQuestChainService` 终章调用 `QuestRewardService`；tracker 活跃上限 64
- `SyncQuestTrackerPacket.MAX_LINES` 32→72；`QuestTrackerActionPacket` 对齐
- `DialogueActionExecutor` offer/turnin 真正结算任务
- `SeekingImmortalsMod` 注册 `QuestHookRuntime`；`ModEvents` 秘境击杀后通知 M11

## 备份

`.bak/20260717_040504_m11_quests_story/`

## 验证

```bash
bash ./gradlew.unix --no-daemon test --tests 'com.xunxian.seekingimmortals.quest.*' -PaiSkipVersionBumpCheck=true
bash ./gradlew.unix --no-daemon build -PaiSkipVersionBumpCheck=true
```

结果：BUILD SUCCESSFUL

## 验收对照

- 62 链 + 35 线 + 主线图交叉引用可解析（`M11QuestCorpusTest`）
- 唯一剧情道具全局幂等（`QuestRewardService.UNIQUE_LEDGER` + authority ledger）
- FTB 映射查询不依赖 FTB 运行时；原生通道独立
- 协议未变（仍 21）
