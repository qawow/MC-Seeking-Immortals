# 2026-07-17 GUI SectHallScreen 迁移

## 变更分类
代码重构（client GUI）。任务红线 **不修改** `mod_version` / `PROTOCOL_VERSION`。

## 迁移
`SectHallScreen` → `AbstractJournalContainerScreen` + `TabBar` + `ScrollableListPanel`

### 钩子拆分
- chrome：`renderJournalChrome` / `renderJournalTitle`（layout 驱动 panel，非固定 leftPos）
- body：`renderJournalBody` → summary + content frame + 模式分发
- TabBar 四 Tab，默认 `MISSION`；非会员隐藏 Tab
- 滚动列表：仅候选人 + SHOP 使用 `ScrollableListPanel`
- 静态 Tab：DIALOGUE / MISSION / PROGRESS 仍为文案 + footer 按钮

### 保留
- `calculateLayout` / `Layout` / `Rect` 公开契约（ScreenLayoutTest）
- 所有 SectActionPacket 动作路径
- `SectScreen`（legacy）未改

## 备份
`.bak/20260717_211305_ui_sect_hall/`

## 版本
- mod_version 0.2.2
- protocol 24

## 构建结果
`bash ./gradlew build --no-daemon -PaiSkipVersionBumpCheck=true`
- BUILD SUCCESSFUL in 1m 27s
- 11 actionable tasks (8 executed, 3 up-to-date)
