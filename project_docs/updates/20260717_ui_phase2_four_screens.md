# 2026-07-17 GUI Phase2 四屏迁移

## 变更分类
代码重构（client GUI）。任务红线 **不修改** `mod_version` / `PROTOCOL_VERSION`。

## ScrollableListPanel 小幅增强
为匹配 refine/quest 的 1px scissor 与 `viewport-6` 滚动可视高度：
- `setScissorInsets(left, top, right, bottom)`
- `setScrollHeightReduce(pixels)` — 滚动夹紧与 thumb 计算用 height-pad
- `setScrollbarTrackInsets(top, bottom)`

Alchemy 试点默认 inset=0，行为不变。

## 迁移屏幕
1. `RefinementPlanScreen`
2. `QuestTrackerScreen`
3. `LifeSkillTreeScreen`
4. `MeditationScreen`

均改为 `AbstractJournalScreen` + `ScrollableListPanel`，私有 scroll 字段移除；
布局算法与内容绘制保持原常量。

## 备份
`.bak/20260717_174500_ui_phase2_screens/`

## 版本
- mod_version 0.2.2
- protocol 24

## 构建结果
`bash ./gradlew build --no-daemon -PaiSkipVersionBumpCheck=true`
- BUILD SUCCESSFUL in 1m 14s
- 11 actionable tasks (8 executed, 3 up-to-date)
