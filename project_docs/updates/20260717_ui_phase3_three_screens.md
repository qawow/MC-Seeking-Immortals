# 2026-07-17 GUI Phase3 三屏迁移（Worldpack / Market / Auction）

## 变更分类
代码重构（client GUI）。任务红线 **不修改** `mod_version` / `PROTOCOL_VERSION`。

## 迁移屏幕
1. `WorldpackScreen` → `AbstractJournalScreen` + `ScrollableListPanel` + `TabBar`
   - 保留 snapshot revision / actionState 重建按钮
   - 保留 `currentDailyEventRemainingTicks` / `currentRealmCooldownTicks` 按接收时间递减
   - 保留 `isPauseScreen() == false`
   - 双 Tab（区域/秘境）与行内 travel/enter 按钮逻辑不变
2. `MarketHallScreen` → `AbstractJournalContainerScreen` + `ScrollableListPanel`
   - **客户端分页** `PAGE_SIZE=6` 保留，不与拍卖统一
3. `AuctionHallScreen` → `AbstractJournalContainerScreen` + `ScrollableListPanel`
   - **服务端翻页** + 页内滚动保留，不与坊市统一

布局常量与 `ScreenLayoutTest` 公开 API（`calculateLayout` / `Layout` / `HallLayout` / `MarketLayout` / `Rect` 字段）保持兼容。

## 备份
`.bak/20260717_181023_ui_phase3_three_screens/`

## 版本
- mod_version 0.2.2（任务红线不 bump）
- protocol 24

## 构建结果
`bash ./gradlew build --no-daemon -PaiSkipVersionBumpCheck=true`
- BUILD SUCCESSFUL in 1m 9s
- 11 actionable tasks (8 executed, 3 up-to-date)
