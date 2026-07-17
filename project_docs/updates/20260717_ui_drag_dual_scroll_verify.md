# 2026-07-17 图节点拖拽与列表/详情双滚动验证

## 变更分类
测试 + 小幅可测辅助抽取。任务红线 **不修改** `mod_version` / `PROTOCOL_VERSION`。

## 验证重点

### 1. MethodTree 图节点拖拽
- `clampGraphNodePosition`：节点 top-left 夹在 graph 矩形内
- `offsetFromGrid`：绝对坐标还原为相对默认网格 slot 的 freeform offset
- `graphHitContains`：命中区右/下边 inclusive
- 运行时 `mouseDragged` / `mouseReleased` 仍写 `layoutOffsets` 并发送 `MethodLayoutActionPacket`

### 2. MethodTree 列表 + 详情双滚动
- 列表：`scrollListBy` 按行
- 详情：`scrollDetailBy` 按像素步长 `LINE`
- 两套函数独立
- 宽屏 list 左 / detail 右不重叠

### 3. TechniqueEdit 拖拽绑定 + 已学列表滚动
- `shouldBindOnRelease(slot, draggingId)`
- `maxLearnedScroll` / `scrollLearnedBy`
- 宽屏 slotPane / learnedPane 左右分离

## 代码改动
- `MethodTreeScreen`：拖拽/双滚动 package-visible 辅助
- `TechniqueEditScreen`：已学列表滚动与释放绑定辅助
- 新增 `DragDualScrollTest`（7 cases）

## 备份
`.bak/20260717_232803_ui_drag_dual_scroll_verify/`

## 版本
- mod_version 0.2.2
- protocol 24

## 构建结果
- 聚焦：`DragDualScrollTest` + `ScreenLayoutTest` 通过
- 全量：`bash ./gradlew build --no-daemon -PaiSkipVersionBumpCheck=true` BUILD SUCCESSFUL in 1m2s
