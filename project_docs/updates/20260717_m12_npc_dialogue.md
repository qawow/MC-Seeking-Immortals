# 2026-07-17 M12 NPC 与对话落地

## 摘要

落地 M12 具名 NPC 注册表、对话模板/分支树运行时、内嵌动作执行器、奖励幂等、好感持久化，以及 SectSteward/MarketTrader 驻点作息与 region 刷新入口。向 M11 提供 `NpcDialogueApi` + `DialogueNodeReachedEvent`。

## 变更类

- 代码 + 语料发布资源 + 文档
- **不升** `mod_version`（任务红线，保持 0.1.506）
- **不升** `ModNetwork.PROTOCOL_VERSION`（保持 21；`DialogueActionPacket` 字段顺序/类型未改，仅服务端路由扩展）

## 新增 / 主要文件

### 语料发布（text_material）

- `named_npcs_v116.json`、`named_npc_seeds_v137.json`
- `npc_dialogue_templates.json`、`npc_dialogue_templates_v138.json`
- `npc_dialogue_branches_v139.json`、`npc_dialogues_v117.json`
- `named_npc_loot_rewards_v97.json`、`npc_vendor_roster_v96.json`
- `dialogue_effect_quest_links_v140.json`

### Java（`npc/`）

- `NamedNpcRegistry` — 168+ 具名 NPC（id/region/faction/role/shop/tree）
- `DialogueTemplateService` — archetype 九类 lines + bindings
- `DialogueBranchService` — v139 分支树 + condition ops
- `NpcDialogueApi` — `startDialogue` / `selectNext` / session
- `DialogueNodeReachedEvent` — Forge 事件（M11 消费）
- `DialogueActionExecutor` — open_shop/grant_item/teleport/… 服务端校验
- `NamedNpcRewardService` — v97 奖励 + 每玩家幂等 claimed 标记
- `NpcFavorService` / `NpcDialogueFlags` — 玩家 persistent NBT
- `NpcSpawnService` — 执事/商人生成 + region ensure

### 实体 / 网络 / 命令

- `SectStewardEntity` / `MarketTraderEntity`：驻点、作息、namedNpcId/shop/tree NBT、对话入口
- `ModEvents`：交互优先走 M12 对话，再回落 M08 宗门厅 / M05 商店
- `DialogueActionPacket`：兼容 text-quest chain 与 M12 tree/npc session（字段不变）
- `/seeking_immortals npc …` 调试命令

### 测试

- `npc/NamedNpcRegistryTest`
- `npc/DialogueBranchServiceTest`
- `npc/DialogueTemplateServiceTest`
- `npc/NamedNpcRewardServiceTest`
- `npc/DialogueActionExecutorTest`

## 备份

`.bak/20260717_030507_m12_npc_dialogue/`

## 验证

```bash
bash ./gradlew --no-daemon test --tests 'com.xunxian.seekingimmortals.npc.*' -PaiSkipVersionBumpCheck=true
bash ./gradlew --no-daemon build -PaiSkipVersionBumpCheck=true
```

结果：BUILD SUCCESSFUL

## M11 / M09 接口注意

- M11 应订阅 `DialogueNodeReachedEvent`，并用 `NpcDialogueApi.startDialogue(player, npcId, treeId)` 开场。
- `turnin_quests` / `offer_quest` 当前写 soft flag + favor，真正任务结算留给 M11。
- `enter_instance` 优先调 `SecretRealmDimensionService`，失败则标记 flag（M09 可消费）。
- 商店 id 语料别名（`blood_forbidden_quota` 等）在 `DialogueActionExecutor` 映射到 M05 已有 shop id。
