# 0.2.135 UI、结构与术法反馈完成记录

- 日期：2026-07-21
- 状态：实现与自动验证完成，实机签字待后续执行
- 版本：`mod_version=0.2.135`
- 网络：`ModNetwork.PROTOCOL_VERSION=26`，未修改数据包字段、顺序、类型或频道行为

## UI 与打坐

- 已删除独立 `MeditationScreen`、打坐快捷键及 `BreathingHudOverlay`。
- 蒲团右键进入打坐、移动取消打坐的服务端玩法保持不变。
- 唯一的打坐收益、周期与效率因子并入修仙属性「道基」页；不再重复展示灵根、体质、灵力或修为储备。
- 修仙入口、属性页、功法树、生活技能树、技能编辑及技能栏的布局与显示完成响应式收口；极小窗口会隐藏不适合呈现的搜索框并清除不可见过滤。
- 属性、流派、境界、功法与技能统一走本地化显示，避免向玩家暴露英文代码、原始 id 或内部占位来源。
- 技能编辑提供可见搜索框、无结果状态、鼠标滚轮和左键拖动滚动，并区分滚动手势与技能拖拽绑定；右键清空槽位保持不变。

## 功法学习与技能树

- 普通玩家不再能从修仙属性或功法树直接学习未入门功法。
- 功法必须由现有秘籍、卷轴、宗门授予或其他服务端权威来源学习；管理员 `learn:` 兼容入口保留。
- 功法树只向已入门功法提供「精进」，继续显示动态层数、前置、境界门槛、解锁术法与当前进度。
- 修炼分页、未入门/已入门提示及双语文案已与上述规则对齐。

## 自然结构与玩家搭建结构

- 新增自然生成结构 `ancient_cultivator_cave` 与 `spirit_beast_den`，包含 structure、structure_set、群系标签和专属宝箱战利品资源。
- 两种结构的 piece seed 会持久化，跨区块加载时保持确定布局；灵兽巢穴植被只放入空气，不覆盖宝箱或阵基。
- 玩家搭建侧扩充聚灵阵、长距传送阵、炼器炉、杀阵/幻阵枢纽及渡劫台等专用几何路径；未实现的作者 validator 显式失败关闭，不再用任意灵矿环冒充。
- 单核心结构绑定精确注册方块；阵法场域持久化精确核心 id，核心被拆除或替换后在周期检查或重载时失效，旧记录保持兼容。
- 作者数据与实现几何已对齐：阵法枢纽半径 `1`，渡劫台为 `9x9x6`、半径 `4`；三品炼器炉实体为真实 `3x3x3`，炉顶三格烟道作为额外净空。
- `single_core` 只接受白名单注册方块，`yin_essence_ore_block` 显式映射实际方块 `yin_essence_ore`，未知 id 失败关闭。

## 术法表现与命中

- 十五元素族通用 VFX 扩展到施法、路径、命中、光环、扫描、beam 与 cone 表现，并复用现有粒子和声音资源。
- `beam` 与 `cone` 不再降级为普通投射物，使用各自真实的服务器命中几何和视觉形状。
- 飞剑初阶/进阶、剑阵、投射飞剑补充轨迹与命中反馈；逐实体重复施法声音和高频轨迹广播已收敛。
- 主动神识扩展、探测、隐身、轻身、土遁、阵法感知、自身增益和土墙等原弱反馈术法补充可见/可听反馈；土墙未成功放置时不再误报成功。
- 通用解析优先消费作者 type、element、tags、damage、range 与 effect key，避免旧泛型别名覆盖真实 beam/cone 等语义。

## 自动化与待签字项

- 新增或扩充 `ScreenLayoutTest`、`DragDualScrollTest`、`LangParityTest`、`StructureResourceContractTest`、`M02TechniqueCorpusTest` 与 `TechniqueVfxPaletteTest` 等契约覆盖。
- 新增 JSON 已用严格解析器通过；UI/目录/结构/术法/权限/JSON 定向测试全部通过。
- 首次全量构建只因新增 beam/cone 将施法者归属伤害点从 47 增至 49、旧测试基线未同步而失败；更新精确计数后最终 `./gradlew build` `BUILD SUCCESSFUL in 1m 12s`，全量 857 项测试、0 failures/errors/skipped，`aiPreflight` 记录 `mod_version=0.2.135`。
- 仍需游戏内检查极小/超宽窗口、搜索焦点和拖拽手势；使用 `/locate` 检查两种自然结构的频率、跨区块外观及战利品；验证阵法核心拆换/重载失效；观察单机与多人场景下的粒子、声音和命中密度。

## 回滚路径

- UI：`.bak/20260721-ui/`、`.bak/20260721-ui-followup/`、`.bak/20260721-ui-final/`
- 结构：`.bak/20260721-structures/`、`.bak/20260721-structure-followup/`
- 术法：`.bak/20260721-spells/`、`.bak/20260721-spell-followup/`
- 集成：`.bak/20260721-integration/`
- 本次文档：`.bak/20260721-docs-02135/`
- 最终收口：`.bak/20260721-refinement-geometry/`、`.bak/20260722-structure-catalog-fix/`、`.bak/20260722-ui-compact-drag/`、`.bak/20260722-damage-authority-test/`、`.bak/20260722-docs-build-success/`
