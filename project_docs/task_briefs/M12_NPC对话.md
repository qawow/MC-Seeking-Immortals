# M12 NPC 与对话

- 分支：`task/m12-npc-dialogue` ｜ worktree：`D:/codex/mc-mod-worktrees/m12-npc-dialogue`
- 前置模块：M00、M06（region_id，NPC 分布）、M05（商店框架，商人复用）
- 优先级：P1（M11 任务的对话前置）

## 目标

落地具名 NPC（named_npcs_v116）与对话模板体系：对话树运行时、NPC 实体（执事/商人）AI 与分布，向任务系统（M11）提供稳定的对话推进 API。

## 功能点清单（可勾选）

- [ ] 对话模板：`npc_dialogue_templates` + `npc_dialogues_v117` → 对话树数据驱动加载（现 `OpenDialogueScreen`/`DialogueAction` 包 + TextQuestDialogue 树运行时扩量）。
- [ ] 具名 NPC：`named_npcs_v116` → NPC 注册表（id、区域、势力、身份、商店引用、对话树引用）。
- [ ] NPC 奖励：`named_npc_loot_rewards_v97` → 对话/好感触发的奖励发放（id 走 M03，幂等标记同 M11 红线）。
- [ ] 实体行为：`SectSteward`、`MarketTrader` AI 扩展（驻点、作息、按 region 分布刷新；宗门业务逻辑归 M08，商店货架归 M05/M08）。
- [ ] 对话推进 API：`onDialogueNodeReached(npcId, nodeId, player)` 事件发布 + `startDialogue(npcId, treeId)` 服务接口（M11 消费）。
- [ ] 对话内嵌动作协议：对话节点触发"打开商店/交付任务物/传送"的动作枚举，服务端校验后执行。
- [ ] 好感/关系度（如语料含）持久化入玩家或 NPC SavedData。

## 涉及文件

**语料侧（本模块拥有）**
- `文本材料/data/npc_dialogue_templates*`、`npc_dialogues_v117*`、`named_npcs_v116*`、`named_npc_loot_rewards_v97*`

**模组侧（本模块拥有）**
- `OpenDialogueScreen`/`DialogueAction` 包数据面与对话屏幕数据扩量
- TextQuestDialogue 对话树运行时类
- `entity/` 中 `SectSteward`、`MarketTrader` 的 AI/刷新逻辑（业务委托见接口节）
- NPC 注册表服务（新建）与相关 SavedData

## 依赖前置库

- Forge 实体 AI goal 体系；网络包（对话交互）；GeckoLib 仅在需要 NPC 动画时评估（现仅检测标志）。

## 接口交叉（谁提供 → 谁消费）

- **M12 → M11**：对话推进事件与 startDialogue 服务。
- **M12 → M05**：商人 NPC 调用商店框架开店。
- **M12 → M08**：宗门执事对话入口（宗门业务回调 M08 服务）。
- **M12 ← M06**：region_id（NPC 分布）。
- **M12 ← M03**：奖励物品 id。

## 验收与红线

- 每个 named NPC 的对话树引用、商店引用、区域引用全部可解析（校验测试）。
- 对话动作（给物/传送/开店）全部服务端校验；客户端只发节点选择意图。
- NPC 奖励幂等：同一奖励节点每玩家只发一次。

## 工作流提醒

按 README 通用工作流。`DialogueAction` 包字段变更 → 双升 mod_version + PROTOCOL_VERSION。
