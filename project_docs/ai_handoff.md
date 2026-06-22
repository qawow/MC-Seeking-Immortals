# AI 交接文档：寻仙问道 seeking_immortals

> 本文档用于 AI / 代理 / 后续开发者快速接手当前 Minecraft Forge 修仙模组项目。
> 每次接手项目时，必须先读取本文件和 `step_progress.md`，再开始执行任务。

## 1. 项目概况

- **项目名称**：寻仙问道
- **Mod ID**：`seeking_immortals`
- **项目类型**：Minecraft Java Edition Forge 模组
- **目标版本**：Minecraft `1.20.1` + Forge `47.2.0`
- **当前版本**：`0.1.51`
- **当前工作目录**：`D:\codex\mc-mod`
- **当前重点**：0.1.48 已加入 Codex/Claude Code 固定 AI 工作流与 Gradle preflight 版本门禁；0.1.47 已实装六大核心属性、手动突破 + 成功率加成、走火入魔分级、飞剑/法宝飞行、炼丹系统、神秘小瓶、练气期 9 技能、基础 HUD 与资源接入验证。当前 Phase 9 MVP 集成测试（详见 `docs/task-board.md`）。

## 2. 技术栈与构建

- Java：17
- Minecraft：1.20.1
- Forge：47.2.0
- Gradle Wrapper：项目内 `./gradlew`
- 必需依赖：Curios
- 可选依赖：Patchouli、JEI
- UI：仅使用原生 `GuiGraphics`、`ResourceLocation`、`fill`、`blit`、`drawString`、Forge Overlay 与原版 `Screen`。

常用命令：

```bash
cd D:/codex/mc-mod
./gradlew build
```

## 3. 强制工作规则

1. 每次开始新任务前必须读取 `project_docs/ai_handoff.md` 与 `project_docs/step_progress.md`。
2. 如涉及新增/修改物品或系统，还需读取 `project_docs/items.md`、`project_docs/pending_requests.md`。
3. 每完成小步、阶段、阻塞排查或构建验证，都要更新 `project_docs/step_progress.md`。
4. 每完成版本迭代，同步更新 `items.md`、`pending_requests.md`、`features.md`、`missing_and_placeholders.md` 与 `updates/YYYYMMDD_版本号.md`。
5. 修改已有代码优先使用 `safe_edit` 或先做显式备份；当前工作区通常不是 Git 仓库。
6. 代码修改后必须执行 `./gradlew build`。
7. 代码、资源、数据包、构建逻辑或 gameplay/config 变更必须先将 `gradle.properties` 的 `mod_version` 按 `0.1.X` 递增；`./gradlew build` 会运行 `scripts/preflight.ps1` 检查该门禁。
8. 修改网络 packet 字段、字段顺序、编码/解码格式或不兼容通道行为时，必须同时 bump `ModNetwork.PROTOCOL_VERSION`。
9. 客户端 UI 类必须放在 `client` 包并通过 `Dist.CLIENT` 事件隔离，避免服务端加载客户端类。
10. 不要引入新的第三方 UI 框架，除非用户明确要求。

## 4. 当前版本状态：0.1.48

`0.1.48` 将固定 AI 工作流写入 `AGENTS.md` 与 `CLAUDE.md`，并在 Gradle `build` 前接入 `scripts/preflight.ps1`，用于检查代码/资源/构建变更是否同步 bump `mod_version`；网络包目录变更会提示确认是否需要 bump `ModNetwork.PROTOCOL_VERSION`。

`0.1.47` 在 0.1.34 原生 UI 基础上完成六大核心属性存储/同步/展示、手动突破流程与成功率加成（丹药/灵脉灵眼/功法品质/执念）、走火入魔四级判定、飞剑/法宝飞行闭环（Curios `artifact` 槽）、炼丹系统、神秘小瓶、练气期 9 技能、基础 HUD 与资源接入验证。详细功能见 `project_docs/features.md`，待办见 `project_docs/pending_requests.md`，进度对齐见 `docs/task-board.md`。

`0.1.34`（历史）在 0.1.33 原生 UI 基础上完成第一阶段技能槽持久化、绑定网络与冷却：

- 移除 Gradle 中旧 UI 库仓库与依赖。
- 移除 `mods.toml` 中旧 UI 库 mandatory 依赖声明。
- 删除旧兼容皮肤类命名，新增 `ImmortalUiSkin`。
- `ImmortalUiSkin` 只使用 `GuiGraphics.fill`、`blit`、原版文字绘制与 `ResourceLocation`。
- 左侧 7 槽技能栏继续使用 Forge Overlay，靠左显示，保留编号、占位图标和 tooltip。
- 打坐吐纳 HUD 继续使用 Forge Overlay，使用 `cultivation_progress_bar.png` 作为 5 秒结算进度条。
- 修仙面板继续为 B 方案独立 `Screen`，保留基础、灵根、功法、负面状态四分页。
- 背包入口按钮只负责打开独立修仙面板，不在背包 Screen 上叠画完整面板。
- HUD 在任意 Screen 打开时隐藏。
- `./gradlew build` 应作为最终验证标准。

## 5. 关键代码位置

### 客户端 UI

- `src/main/java/com/xunxian/seekingimmortals/client/TechniqueSkillBarOverlay.java`：左侧 7 槽技能栏 Overlay。
- `src/main/java/com/xunxian/seekingimmortals/client/BreathingHudOverlay.java`：打坐吐纳 HUD 与修炼进度条。
- `src/main/java/com/xunxian/seekingimmortals/client/CultivationStatsScreen.java`：B 方案独立全屏/居中修仙面板，单页分区总览。
- `src/main/java/com/xunxian/seekingimmortals/client/ImmortalUiSkin.java`：原生 UI 绘制工具类。
- `src/main/java/com/xunxian/seekingimmortals/client/ClientEvents.java`：客户端事件、打坐/技能释放/技能编辑按键、背包按钮入口与 Overlay 注册。
- `src/main/java/com/xunxian/seekingimmortals/client/ClientCultivationData.java`：客户端修炼数据快照。
- `src/main/java/com/xunxian/seekingimmortals/client/ClientTechniqueData.java`：客户端已学技能列表、排序槽位与 tooltip 数据。
- `src/main/java/com/xunxian/seekingimmortals/client/TechniqueEditScreen.java`：原生技能编辑界面占位实现。

### 服务端/通用系统

- `src/main/java/com/xunxian/seekingimmortals/cultivation/PlayerCultivation.java`
- `src/main/java/com/xunxian/seekingimmortals/network/SyncCultivationDataPacket.java`
- `src/main/java/com/xunxian/seekingimmortals/network/SyncLearnedTechniquesPacket.java`
- `src/main/java/com/xunxian/seekingimmortals/network/ReleaseTechniquePacket.java`

## 6. 下一步建议

1. 游戏内验证客户端 UI 坐标、tooltip、屏幕缩放和服务端进入安全性。
2. 继续实现技能点击/拖拽绑定、真实冷却、真实效果结算和可配置槽位保存。
3. 替换技能占位图标为正式资源。
4. 继续完善闭关、风险、威胁等级等吐纳 HUD 信息。

## 7. 明确不要做的事

- 不要规划或实现“心魔击杀玩家后削弱自我数值”的机制。
- 不要把客户端类从通用服务端路径直接引用。
- 不要重新引入第三方 UI 框架或旧兼容层。
- 不要跳过 `items.md`、`pending_requests.md`、`step_progress.md` 的维护。
