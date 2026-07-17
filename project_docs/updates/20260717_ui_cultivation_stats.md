# 2026-07-17 CultivationStatsScreen 迁移

## 变更分类
代码重构（client GUI）。任务红线 **不修改** `mod_version` / `PROTOCOL_VERSION`。

## 子部分迁移对照（逐项核对）

### 1. 壳体 Shell
- 前：`extends Screen`，自管 `renderBackground → renderCultivationJournal → super.render`
- 后：`extends AbstractJournalScreen`
  - `journalChrome()` → panel + header + content 矩形
  - `renderJournalTitle` 空实现（双行标题仍由 `drawHeader` 负责，避免与 seal/subtitle 冲突）
  - `renderJournalContent` → 原 `renderCultivationJournal`
- chrome 负责 layered panel / title bar / content innerFrame
- `drawJournalFrame` 只补 profile 内框 + tab 轨 + footer 轨
- `isPauseScreen()==false` 保留

### 2. Tab（替换全项目最后一处真 drawTab）
- 前：`JournalTabButton` + `ImmortalUiSkin.drawTab` 每帧判 selected
- 后：`TabBar<StatsTab>` + ImmortalButton primary/secondary
- `selectTab` 后 `rebuildActionWidgets()` 重建 Tab 视觉（TabBar 不会每帧重绘选中态）
- 默认仍 `FOUNDATION`；切 Tab 重置滚动 + slider 可见性

### 3. 滚动页
- 前：`scrollOffset` + 手写 scissor + `drawScrollBar`
- 后：`ScrollableListPanel`（`setScrollStep(20)`，`setContentHeight` 双遍测高）
- `contentRevision`（STUDY 同步变化）仍触发 `listPanel.resetScroll()`
- `clampScroll` 委托 `ScrollableListPanel.clampScroll`（API 兼容测试）

### 4. 三页内容（未改业务）
- FOUNDATION：境界修为 / 灵根吐纳（双栏）
- COMBAT：战力 / 破境 / 伤势（双栏）+ MovementSpeedSlider 仅本页可见
- STUDY：功法玉简 / 术法卡片 / 生活百艺 / 异术旁门

### 5. Footer 跳转与动作（行为锁死）
- 突破 → `AttemptBreakthroughPacket` 不变
- 功法 → `setScreen(new MethodTreeScreen(this))` 不变
- 技能 → `setScreen(new LifeSkillTreeScreen(this))` 不变
- 关闭 → `onClose`；`returnToInventory` 时回 `InventoryScreen` 不变

### 6. MovementSpeedSlider
- 类体完整保留：quantize / keyboard / pending ack / SetMovementSpeedScalePacket
- `updateSliderVisibility`：`activeTab==COMBAT && showsMovementSlider`

### 7. 背包注入 / 快捷键
- **未改** `ClientEvents`：背包按钮 `new CultivationStatsScreen(player, true)`；快捷键 `false`

### 8. 公开 API 保留
`calculateLayout` / `PanelLayout` / 内嵌 `UiRect` / `StatsTab` / `clampScroll` /
`returnsToInventory` / `closeButtonLabel` / movement helpers / `statusBarHighlightColor`

## 备份
`.bak/20260717_212213_ui_cultivation_stats/`

## 版本
- mod_version 0.2.2
- protocol 24

## 构建结果
- 聚焦：`ScreenLayoutTest` + `CultivationStatsInteractionTest` 通过
- 全量：`bash ./gradlew build --no-daemon -PaiSkipVersionBumpCheck=true` BUILD SUCCESSFUL in 1m20s
