# 2026-07-17 AbstractLoreScreen 抽取

## 变更分类
代码重构（client GUI lore 族）。任务红线 **不修改** `mod_version` / `PROTOCOL_VERSION`。

## 新建
- `client/AbstractLoreScreen.java` extends `AbstractJournalScreen`
  - 共同壳体：420×260 layered panel + 左对齐 title strip
  - `sendLoreAction` / `addRefreshAndClose` 统一 packet 刷新与关闭
  - `computeListDetailChrome`：Bestiary/Chronicle 共享窄屏 chrome 压缩与 list+detail 几何
  - `renderWrappedDetail`：详情区 scissor 换行

## 迁移
- `BestiaryScreen` / `ChronicleScreen` / `LoreCompendiumScreen` → AbstractLoreScreen
- Tab/filter 改用 `TabBar`（primary=选中）
- Compendium → bestiary/chronicle 仍发 `LoreScreenActionPacket`
- 公开 `Layout`/`Rect`/`calculateLayout`/`actionForTab`/`findSelectedIndex` 保持，ScreenLayoutTest 不改断言语义

## 备份
`.bak/20260717_181200_ui_abstract_lore/`

## 版本
- mod_version 0.2.2
- protocol 24

## 构建结果
`bash ./gradlew build --no-daemon -PaiSkipVersionBumpCheck=true`
- BUILD SUCCESSFUL in 1m 6s
- ScreenLayoutTest 全绿（窄屏断言保留）
