# 寻仙问道 / Seeking Immortals

`seeking_immortals` 是 Minecraft Java Edition 1.20.1 + Forge 47.2.0 的原创凡人流修仙模组。

当前工作区版本：`0.1.48`  
当前阶段：Phase 9 MVP 集成测试准备中  
网络协议：`ModNetwork.PROTOCOL_VERSION = "4"`  
Java：17

## 当前状态

0.1.47 已完成的主要系统：

- 修炼 Capability、灵力/修为/神识/肉身/走火风险等核心属性。
- 炼气 1-13 层、筑基等境界阶段与手动突破流程。
- 灵根、属性、特殊体质、寿元与负面状态基础。
- 打坐吐纳、灵气浓度、测灵盘、寻脉罗盘与聚灵阵。
- 练气期 9 个技能、7 槽技能栏、技能编辑界面与服务端释放校验。
- 飞剑/法宝飞行基础闭环。
- 炼丹 MVP、丹炉、废丹/爆炉、基础丹药效果。
- 神秘小瓶、离线充能、植物加速。
- 基础 HUD、修仙属性面板、资源与物品接入验证。

当前最重要的待办不是继续扩展大系统，而是先按代码审查清单修复高危问题，并完成 Phase 9 集成测试。

## 构建与运行

```powershell
.\gradlew.bat --no-daemon --max-workers=1 build
.\gradlew.bat runClient
.\gradlew.bat runServer
.\gradlew.bat runData
```

常规要求：

- 修改代码后必须运行 `.\gradlew.bat --no-daemon --max-workers=1 build`。
- 修改现有文件前先备份到 `.bak/<timestamp>/`。
- 代码/资源/构建逻辑变更必须同步 bump `gradle.properties` 的 `mod_version`；`build` 会先运行 `scripts/preflight.ps1` 做门禁检查。
- 涉及网络包字段顺序/类型变更时，必须 bump `ModNetwork.PROTOCOL_VERSION` 并同步文档。

## 依赖

- 必需：Curios
- 可选：Patchouli、JEI
- 检测预留：Jade、GeckoLib

可选依赖不得从 common/server 初始化路径无条件引用。客户端 UI 类必须放在 `client` 包并通过 `Dist.CLIENT` 事件隔离。

## 文档入口

当前真相优先级：

1. `gradle.properties`
2. `docs/task-board.md`
3. `project_docs/ai_handoff.md`
4. `project_docs/step_progress.md`

关键执行文档：

- `docs/code-review-fix-checklist.md`：代码审查修复落地清单。
- `docs/code-review-fix-alternatives.md`：每个高/中危问题的双方案与推荐。
- `docs/code-review-report.md`：代码审查问题来源。
- `project_docs/features.md`：当前已有功能。
- `project_docs/pending_requests.md`：长期待办。
- `project_docs/missing_and_placeholders.md`：缺失与占位内容。
- `project_docs/items.md`：当前物品与资源说明。

历史/证据类文档包括 `docs/phase-*.md`、`docs/gap-audit-report.md`、`docs/existing-implementation.md`、`docs/implementation-roadmap.md`。这些文档用于追溯，不应覆盖当前 `task-board.md` 的状态判断。

## 下一步建议

先处理代码审查第一批剩余项：

- H11：飞行死亡/换维/重生清理与 grant 灵力门。
- H3：MysticVial 催熟目标传入真实 level/pos，避免 NPE。
- H4/H5：技能释放失败或 effect 缺失时不扣灵力、不进冷却。
- H12：炼丹炉 server tick 守卫上提。

完成后再进入 Phase 9 MVP 集成测试，验证主流程、资源显示、关键玩法链路和已知修复。

## 工作约定

- 以当前源码和资源为准，不以 `build/`、`.gradle/`、`run/`、`.bak/` 等生成或历史目录为实现真相。
- 新增可见物品/方块时同步注册、创造栏、语言、模型、贴图、配方/掉落和文档。
- 网络包必须假设客户端不可信，所有消耗、冷却、境界、槽位、已学状态都在服务端校验。
- 不重新引入旧第三方 UI 框架；当前 UI 使用原生 Forge/Minecraft Screen 与 Overlay。
