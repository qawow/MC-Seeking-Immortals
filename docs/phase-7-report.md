# Phase 7 Report - 基础 HUD

> Date: 2026-06-19
> Scope: Phase 7 only
> Build: `.\gradlew.bat --no-daemon --max-workers=1 build` BUILD SUCCESSFUL in 26s
> Phase boundary: Did not implement Phase 8 or any later system.

## Implemented

- 新增 `CultivationHudOverlay`：常驻右上角修仙 HUD，仅在客户端同步后、无屏幕、未隐藏 GUI 时渲染。
- 显示内容：
  - 境界名称（`realm + stage`）。
  - 修为：占位进度条 + 当前修为数值文本。
  - 灵力条：`mana / manaMax` 比例条 + 数值文本。
  - 神识范围 `divSense`。
  - 走火风险 `qiDevRisk`，按阈值变色：>=70% 红色并在面板下方加“! zouhuo risk high !”警示；>=50% 黄色；否则绿色。
- 技能冷却：复用现有 `TechniqueSkillBarOverlay`（左侧 7 槽 + tooltip 显示冷却倒计时），不重做技能系统。
- 注册 overlay 到 `ClientEvents.registerGuiOverlays`，名为 `cultivation_hud`。
- 纯客户端渲染，使用 `ForgeGui` overlay 事件，无服务端调用客户端类；对 `ClientCultivationData.isSynced()` 做空值防护，避免 NullPointerException。

## Known Limitation

修为进度条目前为占位条。`SyncCultivationDataPacket` 的快照未包含 `cultivationMax`（当前阶段上限），为避免在 Phase 7 改动网络包结构（会触发协议版本 bump，超出本阶段范围），HUD 显示“当前修为数值 + 非零占位条”。精确阶段内进度需后续给同步包补 `cultivationMax` 字段。

## Validation

```powershell
.\gradlew.bat --no-daemon --max-workers=1 build
```
Result: BUILD SUCCESSFUL in 26s（compileJava / test / build 通过）。

## Non-Goals

- 未实现 Phase 8 或后续阶段。
- 未重做技能系统、炼丹 GUI、任务 GUI、世界地图 UI。
- 未做无关重构。