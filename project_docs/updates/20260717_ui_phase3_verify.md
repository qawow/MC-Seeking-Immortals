# 2026-07-17 GUI Phase3 分页/revision 验证

## 变更分类
测试 + 小幅可测辅助抽取（client GUI）。任务红线 **不修改** `mod_version` / `PROTOCOL_VERSION`。

## 验证重点
1. **Market 客户端分页** `PAGE_SIZE=6`：maxPage/clamp/pageStart/pageEnd/pageItemCount
2. **Auction 服务端分页**：snapshot.page/maxPage 驱动 previous/next；页内 contentHeight 仅看当前页 lot 数
3. **两种分页模型互不混用**：Market maxPage(25)=4 vs Auction pageSize=8 时 maxPage=3
4. **Worldpack revision 重建后按钮状态**：
   - `actionState` 在冷却就绪 / 进入秘境 时变化
   - `canTravelRegion` / `canEnterRealm` 按 anchor/current/cooldown/activeRealm 判定
5. **单机暂停倒计时不漂移**：`remainingTicks` 纯墙钟（receivedAtNanos vs nowNanos），与 screen tick / 暂停无关

## 代码改动
- `WorldpackScreen`：抽出 `actionState` / `canTravelRegion` / `canEnterRealm`（package-visible）
- `MarketHallScreen`：抽出 `pageSize/maxPage/clampPage/pageStart/pageEnd/pageItemCount`
- `AuctionHallScreen`：抽出 `canPagePrevious/canPageNext/previousPage/nextPage`

## 测试
- 扩展 `ClientWorldpackDataTest`
- 新增 `MarketAuctionPagingTest`
- 聚焦测试：`ClientWorldpackDataTest` + `MarketAuctionPagingTest` + `ScreenLayoutTest` 通过

## 备份
`.bak/20260717_203453_ui_phase3_verify/`

## 版本
- mod_version 0.2.2
- protocol 24

## 构建结果
`bash ./gradlew build --no-daemon -PaiSkipVersionBumpCheck=true`
- BUILD SUCCESSFUL in 1m 25s
- 聚焦测试 ClientWorldpackDataTest + MarketAuctionPagingTest + ScreenLayoutTest 先通过
- 11 actionable tasks (6 executed, 5 up-to-date)
