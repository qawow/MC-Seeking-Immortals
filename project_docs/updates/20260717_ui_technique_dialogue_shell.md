# 2026-07-17 TechniqueEdit + Dialogue 渲染外壳迁移

## 变更分类
代码重构（client GUI 渲染外壳）。任务红线 **不修改** `mod_version` / `PROTOCOL_VERSION`。

## 仅修改
- `TechniqueEditScreen` → `AbstractJournalScreen`
  - 拖拽 hitbox / mouseDragged / mouseReleased / SetTechniqueSlotPacket 原样
  - 拖拽 ghost 经 `renderAfterWidgets` 画在 widgets 之上
- `DialogueScreen` → `AbstractJournalScreen`
  - **仅** render chrome/content 迁移；`beginAction` / `closeSent` / `send` / choice 按钮 / `onClose` ACTION_CLOSE 未改
  - 立绘 blit + prompt scissor 保留
  - 自定义 title strip（含 refresh/close 高度）覆盖 `renderJournalChrome`

## 未触碰
- `DialogueActionExecutor*` / `NpcDialogueApi` / packet handle
- `DialogueActionExecutorTest` / `DialogueBranchServiceTest` 源文件
- 其它 Screen

## 版本
- mod_version 0.2.2
- protocol 24

## 构建结果
`bash ./gradlew build --no-daemon -PaiSkipVersionBumpCheck=true`
- BUILD SUCCESSFUL in 1m 15s
- DialogueActionExecutorTest / DialogueBranchServiceTest 等全绿
- 工作树仅 DialogueScreen + TechniqueEditScreen 渲染外壳 diff；npc/network/test 无变更
