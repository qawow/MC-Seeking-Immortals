# 2026-07-17 GUI 共享基础设施试点

## 变更分类
代码重构（client GUI 基础设施 + 两个试点屏幕迁移）。按任务红线 **不修改** `mod_version` / `PROTOCOL_VERSION`。

## 新建
- `client/UiRect.java` — 共享矩形几何
- `client/ScrollableListPanel.java` — 滚动视口 + scissor + thin scrollbar + 可见行按钮重建
- `client/TabBar.java` — ImmortalButton 冒充 tab 的统一组件（多数派模式）
- `client/AbstractJournalScreen.java` — 纯 Screen 标准 journal 流水线基类
- `client/AbstractJournalContainerScreen.java` — AbstractContainerScreen journal 流水线基类

## 试点迁移（唯一允许改动的现有 Screen）
- `AlchemyStatusScreen` → `AbstractJournalScreen` + `ScrollableListPanel`
- `AlchemyFurnaceScreen` → `AbstractJournalContainerScreen`

## 约束遵守
- 未改其他 Screen/Overlay/Menu
- 未删除 ImmortalUiSkin legacy 色板
- 未改 `mod_version` / protocol
- 视觉与交互与迁移前对齐（布局常量、标题居中、滚动步长 18、内容 inset 7/6、关闭按钮位置保持）

## 备份
`.bak/20260717_172250_ui_shared_infra/`

## 验证
`./gradlew build --no-daemon -PaiSkipVersionBumpCheck=true`

## 构建结果
`bash ./gradlew build --no-daemon -PaiSkipVersionBumpCheck=true`
- BUILD SUCCESSFUL in 1m 41s
- 11 actionable tasks
- 仅 JEI deprecation 警告，无编译错误

## 版本与协议
- `mod_version` 保持 `0.2.2`（任务红线）
- `ModNetwork.PROTOCOL_VERSION` 保持 `24`（无 packet 变化）
