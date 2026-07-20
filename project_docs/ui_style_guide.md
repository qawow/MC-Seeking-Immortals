# UI 风格指南：《云笈墨卷》(InkScroll)

> 生效版本 0.2.83（2026-07-20）。本指南是前端唯一风格与接口事实源；改 UI 前先读本文与
> `/root/.claude/plans/parsed-mapping-puppy.md`（重构总计划）。

## 1. 概览与铁律

**气质**：凡人修仙传式的克制文人感——界面永远是"浅纸 + 深墨"，唯一饱和强调色是印泥朱砂；
描金细线只用于账房/高危强调。**Do**：纸纹低透明度、墨线分隔、印章元素、1px 纸光/纸压。
**Don't**：霓虹/发光特效、近白半透明高光、材质隐喻（玉/竹/漆已退役）、任何屏幕里的硬编码色值。

## 2. 场景系统（InkScene）

场景按界面语义路由（不是玩家主题开关）。`client/ui/InkScene`：

| 场景 | 中文 | 纸/墨 | 强调 | 使用界面 |
|---|---|---|---|---|
| `QUIET_STUDY` | 静室 | 冷灰绿纸·淡墨 | 青印 | 修仙面板、功法树、技能编辑、打坐、生活技能树、全部 HUD |
| `FIELD_NOTES` | 行录（默认） | 暖米纸·常墨 | 朱印 | 任务、对话、见闻/图鉴/云笈、旅行、炼丹状态 |
| `LEDGER_HALL` | 账房 | 黄旧纸·浓墨 | 描金+琥珀 | 坊市、拍卖、宗门、储物、炼器计划、全部容器屏 |
| `OMEN_RED` | 凶兆 | 枯纹纸·高对比 | 朱砂 | 突破危签按钮、走火/天劫强调（瞬时 push） |

机制：渲染线程场景栈（push/pop/with），`ImmortalUiSkin` 的 `JOURNAL_*`/`HUD_*` 静态 token
在每次 push/pop 时按 `InkScene.fromClimate(...)` 重绑。过渡期屏幕仍写 `defaultClimate()`
（`UiClimate` 已 @Deprecated 为别名）；新屏幕直接选 InkScene 对应的 climate 别名即可。

## 3. Token 参考

色板 record：`UiClimate.Palette`（46 字段），由 `InkScene` 提供四套值。语义分组：

- **纸底**：`panel`(浅纸) `inner` `header` `voidFill`(卷外) `paperSheen/paperWeight`(1px 光/压)
- **墨迹**：`border`(浓墨框) `borderDim`(淡墨) `paper`(=正文墨色！) `paperMuted`(注墨) `dividerGlow`(墨线)
- **行/控件**：`row/rowHovered/rowSelected/rowDisabled`（墨洗层），`control*`、`tabSelected`
- **印与彩**：`accent`(场景印色) `accentText` `cinnabar`(0xFF9E3226 跨场景恒定) `cinnabarBright` `warning`(琥珀 0xFF9A7020 恒定) `spirit`
- **计量**：`barBacking` `cultivationFill/Highlight` + `StatusBarStyle{CULTIVATION,SPIRIT,HEALTH,WARNING,DANGER,NEUTRAL}`
- **HUD**：`hud*` 与 `hudSkill*`（半透明浅纸牍）

跨场景常量（`static final`，不随场景变）：`HEALTH_BAR_FILL`、`HUD_SHADOW`、`HUD_COOLDOWN_OVERLAY`、
走火阈值 `QI_DEV_WARN_THRESHOLD=50` / `QI_DEV_DANGER_THRESHOLD=70`（`qiDevRiskColor`）。

**注意语义反转**（0.2.77 起）：`JOURNAL_PAPER` 是**墨色文字**，`JOURNAL_PANEL` 才是纸底。
不要按旧四材直觉用 token。

## 4. 分层架构与接口

```
client/ui/
  InkScene    场景枚举 + 四套色板 + fromClimate 过渡映射
  InkLayout   响应式布局引擎  panel(w,h,Spec) -> Panel{outer,header,body,listPane,detailPane,footer,stacked,wide}
              Spec.JOURNAL / Spec.LORE(420/260/4/280) / Spec.CONTAINER；clampScroll；tabCell
  NumberFmt   数字语系唯一实现：cjk(万/亿/兆) cjkPair percent two clamp01
client/ui/widget/
  InkRows     keyValue 行 / section 题签 / labeledRow(斑马+玉签行体) / syncWait 等待占位
  InkPaging   clampPage / maxPage / pageStart / pageEnd
client/
  ImmortalUiSkin   token 镜像 + 场景栈 + ~40 绘制 helper（drawLayeredPanel/drawTitleBar/
                   drawListRow/drawTab/drawButtonBackground/drawSemanticStatusBar/drawMeterRow/
                   drawSkillSlot/drawHudPanel/drawThinScrollbar/withScissor/drawWrappedText/
                   drawStringFit/drawTiledTexture/qiDevRiskColor/getStatusText...）
  AbstractJournalScreen / AbstractJournalContainerScreen / AbstractLoreScreen   三族基类
  ImmortalButton(secondary/primary/danger→OMEN_RED) / TabBar / ScrollableListPanel / UiRect
```

纹理：`textures/gui/ink/paper_{rice,cool,aged,dry}.png` + `seal_grain.png`，由
`scripts/generate_ink_ui_textures.py`（PIL，幂等）生成；真实美术可按名替换。
技能图标 16×16 在 `gui/skill/`（`generate_skill_icons.py`）；对话立绘 `gui/dialogue/`。

## 5. 文案与本地化

- 命名空间：`screen.seeking_immortals.<屏>.*`、`hud.seeking_immortals.<层>.*`、
  `status.seeking_immortals.affliction.*`（参数化：`心魔%s层`）。
- **零硬编码中文**：`LangParityTest` 对 Stats 屏与三 HUD 做 CJK 字面量 grep 门，并强制
  hud/status 命名空间 zh/en 键集齐平。新增键必须两语言同批落。
- 语域：问道录/玉简/坊市/吐纳/走火 等词保留为内容词；标题用手卷语系（问道·身册/行录/账房/云笈）。

## 6. 新增一个屏幕（食谱）

1. 选族：信息屏 extends `AbstractJournalScreen`；容器屏 extends `AbstractJournalContainerScreen`；
   图鉴类 extends `AbstractLoreScreen`。
2. `defaultClimate()` 返回场景（表 §2）；危签强调用 `ImmortalButton.danger`。
3. 写 `public static calculateLayout(w,h)` + public `Layout`/`Rect` record（ScreenLayoutTest 契约），
   内部委托 `InkLayout.panel(w,h,Spec.X)` 投影到自己的 record。
4. 行/题签/等待态用 `InkRows`；分页用 `InkPaging`；数字用 `NumberFmt`；滚动用 `ScrollableListPanel`。
5. 文案全部 `Component.translatable`，两语言同批加键。
6. 在 `ScreenLayoutTest` 加 panel-fit/非重叠断言；跑 `./gradlew build`。

## 7. 不可破坏接口（改前必读）

- `ClientCultivationData.Snapshot`（90 字段 record）+ 静态访问器 + `pendingMeditating` 乐观宽限。
- `ClientTechniqueData` 访问器 + `SLOT_COUNT = 7`。全部 11 个 `Client*Data` 镜像的 set/reset。
- 一切网络包（`PROTOCOL_VERSION=25`）；UI 批次禁止碰 network/。
- `ClientEvents`：按键注册（V/B/J，其余默认未绑）、登录/登出/重生 `resetClientSyncState`、
  背包"修仙"按钮注入。
- 独立全屏 Screen 模式（用户既定 B 方案）；**技能栏禁止装饰外框**（用户既定 0.1.31）。
- 测试钩子：`climateStackDepthForTest` / `forceResetClimateForTest`；token 名 `JOURNAL_*`。
- 屏幕内零硬编码 hex（一切颜色出自 skin token）。

## 8. 验证清单

- `InkPaletteTest`：纸墨对比度、无霓虹、跨场景语义色恒定。
- `UiClimateStackTest`：栈语义 + qiDev 阈值。`InkUiCoreTest`：布局/分页/数字。
- `ScreenLayoutTest`（622 行）：panel-fit/非重叠/断点。`LangParityTest`：键齐平 + CJK 门。
- runClient 烟测：23 屏全开 + 4 HUD，GUI scale 1/2/3/Auto + 小窗；`latest.log` 零 ERROR。
