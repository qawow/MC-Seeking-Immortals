---
trigger: always_on
---
# 寻仙问道 Mod - 强制开发规则

## 项目基本信息

- **Mod ID**: `seeking_immortals`
- **Minecraft**: 1.20.1 / **Forge**: 47.2.0 / **Java**: 17
- **构建**: `./gradlew build`（每次代码修改后必须执行）
- **版本格式**: `gradle.properties` 中的 `mod_version`，使用 `0.1.X` 格式
- **工作区通常不是 Git 仓库**，修改代码前必须做显式备份（`.bak/<timestamp>/` 或 `backups/<timestamp>/`）

## 强制工作流程

### 每次开始任务前

1. 必须读取 `project_docs/ai_handoff.md` 和 `project_docs/step_progress.md`
2. 如果涉及物品、系统或版本修改，还需读取：
   - `project_docs/items.md`
   - `project_docs/pending_requests.md`
   - `project_docs/features.md`
   - `project_docs/missing_and_placeholders.md`

### 每完成一个小步

- 更新 `project_docs/step_progress.md`（记录状态、时间、备注）

### 每完成版本迭代

- 同步更新以下文件：
  - `project_docs/items.md`
  - `project_docs/pending_requests.md`
  - `project_docs/features.md`
  - `project_docs/missing_and_placeholders.md`
  - `project_docs/updates/YYYYMMDD_版本号.md`（新建版本更新日志）

### 代码修改后

- 必须执行 `./gradlew build` 并记录结果
- 构建失败必须修复后才能继续下一步

### 新增方块/物品时

至少检查并同步：
- 注册代码（`registry/` 包）
- 中文语言文件 `assets/seeking_immortals/lang/zh_cn.json`
- 英文语言文件 `assets/seeking_immortals/lang/en_us.json`
- 物品模型 `assets/seeking_immortals/models/item/`
- 贴图 `assets/seeking_immortals/textures/`
- 创造模式物品栏 `ModCreativeTabs`
- 合成配方/掉落表（如适用）
- Patchouli 指南（如适用）
- 上述所有文档文件

## Git 推送规则

- **每 5 个小版本必须 git 推送一次**（例如 0.1.45 → 0.1.50 时推送）
- 推送前先执行 `./gradlew build` 确保构建通过
- 推送前检查 `step_progress.md` 和文档是否已同步到最新版本
- 推送命令：`git add -A; git commit -m "v0.1.X: <简述本批版本变更>"; git push`
- 如果当前不是 Git 仓库，提醒用户先初始化或切换到 Git 工作区

## 明确禁止

- **不要**规划或实现"心魔击杀玩家后削弱自我数值"的机制
- **不要**把客户端类从通用服务端路径直接引用（客户端代码必须在 `client` 包并通过 `Dist.CLIENT` 隔离）
- **不要**引入新的第三方 UI 框架，除非用户明确要求
- **不要**跳过文档维护（items.md、pending_requests.md、step_progress.md）
- **不要**在代码中硬编码数值而不做注释说明用途
- **不要**信任客户端发送的数据——服务端必须校验所有资源消耗、冷却、技能状态

## 架构要点

- `PlayerCultivation` 是核心玩家数据类（Capability 模式），所有修炼数据统一存储
- 技能系统分两层：枚举/代码层（`SkillType` + `SkillEffect`）和资源驱动层（cultivation JSON + `TechniqueManualItem`）
- 网络包：客户端 → 服务端只发送意图（槽位、按键），服务端校验一切
- 修改网络包字段时必须同时更新 encode/decode/handle 并递增 `ModNetwork.PROTOCOL_VERSION`
- UI 只使用原生 Forge `GuiGraphics`、`Screen`、`Overlay`，不使用第三方 UI 库
