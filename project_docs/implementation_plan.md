# 寻仙问道 — `0.2.245` 后续完整实现计划

> 状态：F-A（UI-01 至 UI-05）、F-B（UI-06、UI-18）、F-C1（UI-07 至 UI-11、UI-19）、F-C2（UI-12 至 UI-14、UI-24 至 UI-26）、F-C3（UI-15、UI-28）、F-D（UI-16、UI-17、UI-21、UI-23、UI-27）、F-E1（UI-20）、F-E2（UI-22）、Q-A（95 步证明路由）与 Q-B-1（修炼/境界/功法/术法生产者）已完成并验证；下一实施入口为 Q-B-2（地域/维度/秘境/结构）。本文件继续定义剩余工作的实施顺序、边界和验收门。
> 制定日期：2026-07-29。
> 最近已提交基线：`cc5d0a2c feat: 建立详细任务证明路由目录`；Q-B-1 已完成并记录，下一批为 Q-B-2。
> 初始版本基线：`mod_version=0.2.232`；当前执行版本：`mod_version=0.2.245`。
> 网络基线：`ModNetwork.PROTOCOL_VERSION=31`。
> 最近完整自动验证：257 个测试套件、1,217 项测试通过，failure/error/skipped 均为 0；`0.2.245` 完整 Gradle 构建结果记录在本批更新记录中。
> 本文取代原先以 `0.1.57` 为基线的同名旧计划；历史 `master_plan.md` 只作完成轨迹参考，不再作为剩余工作真相。

## 1. 目标与边界

本计划收口四类剩余工作：

1. 修复已逐行确认的 28 项前端交互问题，现已全部落地；真实客户端交互仍需最终 QA。
2. 为 23 条详细任务、95 个步骤补齐自然玩法证据，深化对话世界动作，并完成阴阳窟专属玩法循环。
3. 收口维度分类、本命法宝语义和旧 NPC 存档迁移，消除“模板被误报成待实现”或“兼容入口可被伪造”的长期债务。
4. 完成当前版本的真实客户端、专服、双客户端、旧档、视觉、性能和边界条件验收。

以下内容不在本轮“代码完成”口径内：

- 1,704 个物品、1,392 个技能图标或 1,800+ 图鉴条目的逐件工作室级手绘重制。
- 2,292 个术法各自拥有独立模型、骨骼动画、音效和 AI。当前共享功能族、15 元素族和 52 种几何是正式运行时架构，不是功能占位。
- 外部账号操作。曾暴露的 Minecraft 访问令牌必须由账号持有人在账号侧撤销或刷新，代码无法代办。
- 没有明确玩法需求的仙界/DLC 全量扩建。基础版必须诚实标注可玩、预览、模板和逻辑集群，不能用空壳维度冒充完成。

## 2. 完成定义

只有同时满足以下条件，才可以把“后续实现计划”标记为完成：

- 28 项前端台账均变为“已修@版本”或“经用户确认暂缓”，且高、中危项不得无理由暂缓。
- 拍卖、突破、宗门、任务和对话的重复请求在服务端不会重复扣款、重复扣料、越权推进或产生不可恢复软锁。
- 95 个详细任务步骤都有可枚举的服务端权威证明规则；正常生存流程不依赖管理员 `prove` 才能完成。
- 对话动作不再只落通用标记：结构、线索、守卫、敌对、逮捕等动作均有与类型匹配的世界结果和回归测试。
- 阴阳窟具有银翅夜叉专属编队、阴芝马活捕/死亡分支、出窟运输和协作炼丹闭环。
- 维度目录能区分 `playable`、`preview_locked`、`abstract_template`、`logical_cluster`；不会把抽象 id 计入“待实现维度”。
- 本命飞剑绑定到唯一物品实例，普通法宝认主不再自动占用本命位，旧档有无损迁移路径。
- 旧命名 Villager 只在受控迁移阶段依赖名称；迁移后以持久 NPC id 为准，玩家改名不能伪装任务 NPC。
- 普通 `./gradlew build` 在不使用版本门禁跳过参数的情况下成功，测试数无无法解释的下降。
- 当前 JAR 完成单客户端、专服和双客户端人工签字；报告记录实际版本、协议、步骤和日志审计结果。
- 工作区差异、备份、版本、协议、构建、提交和剩余风险均按 `AGENTS.md` 完整记录。

## 3. 当前剩余工作台账

### 3.1 前端交互台账

| ID | 级别 | 位置 | 目标结果 | 初始状态 |
|---|---|---|---|---|
| UI-01 | 高 | `TechniqueEditScreen` | 滚动手势绝不提升为技法拖拽 | 已修@0.2.233 |
| UI-02 | 高 | `QuestTrackerScreen` | 同视图同步保留列表和详情滚动 | 已修@0.2.233 |
| UI-03 | 高 | `SectHallScreen` | 候选行、按钮、滚动统一使用同一可见列表 | 已修@0.2.233 |
| UI-04 | 高 | `AuctionHallScreen`、`AuctionSoftService` | 最高价玩家重复点击不扣款、不推进结算 | 已修@0.2.233 |
| UI-05 | 高 | `SectHallScreen` | 数据同步后重建控件，入宗不软锁 | 已修@0.2.233 |
| UI-06 | 中 | `ScrollableListPanel` | 按下待定、拖动滚动、松手点击、滚动条独立命中 | 已修@0.2.234 |
| UI-07 | 中 | `QuestTrackerScreen` | 过滤只改变列表，不篡改全局选中 | 已修@0.2.236 |
| UI-08 | 中 | `ClientPacketHandlers` | 对话包不顶掉有服务端菜单状态的界面 | 已修@0.2.236 |
| UI-09 | 中 | `DialogueScreen`、`DialogueActionPacket` | 静默丢包后动作闩能超时恢复 | 已修@0.2.236 |
| UI-10 | 中 | `ChronicleScreen` | 未发现条目的列表和详情都隐藏真名 | 已修@0.2.236 |
| UI-11 | 中 | `AbstractLoreScreen` | 超长详情可滚动且正确钳制 | 已修@0.2.236 |
| UI-12 | 中 | `StorageBraceletMenu` | 客户端 NBT 栈换实例后仍能正常预测，服务端锚点仍严格 | 已修@0.2.237 |
| UI-13 | 中 | `MethodTreeScreen` | 节点按下不破坏拖拽基线 | 已修@0.2.237 |
| UI-14 | 中 | `MethodTreeScreen` | 重叠节点命中最上层 | 已修@0.2.237 |
| UI-15 | 中 | `AlchemyFurnaceScreen`、`AlchemyFurnaceMenu` | 空闲炉显示 0 进度和空闲态 | 已修@0.2.238 |
| UI-16 | 低 | `CultivationStatsScreen` | 同步重建时保留未确认滑条值 | 已修@0.2.240 |
| UI-17 | 低 | `CultivationStatsScreen`、`BreakthroughService` | 突破双击与恶意重放均只结算一次 | 已修@0.2.240 |
| UI-18 | 低 | 各行列表屏 | 只有左键可以选中列表行 | 已修@0.2.234 |
| UI-19 | 低 | `DialogueScreen` | resize 不重播 NPC 招呼语音 | 已修@0.2.236 |
| UI-20 | 低 | `WorldpackScreen` | 秘境行按钮提供真实的界门引导或条件反馈 | 已修@0.2.242 |
| UI-21 | 低 | `AuctionHallScreen` | resize 保留当前页 | 已修@0.2.240 |
| UI-22 | 低 | `SectScreen`、`ShopScreen`、`AuctionScreen` | 删除不可达旧屏及废弃开屏包 | 独立协议批次 |
| UI-23 | 低 | `ClientEvents` | 背包“修仙”按钮跟随 `guiLeft/guiTop` 和配方书位移 | 已修@0.2.240 |
| UI-24 | 低 | `MethodTreeScreen` | 关屏/其他按键不丢最后一次布局偏移 | 已修@0.2.237 |
| UI-25 | 低 | `MethodTreeScreen` | 零位移点击不发送布局包 | 已修@0.2.237 |
| UI-26 | 低 | `MethodTreeScreen` | 切换流派后详情滚动归零 | 已修@0.2.237 |
| UI-27 | 低 | `LifeSkillTreeScreen` | 内容高度与实际行间距一致，不滚过末行 | 已修@0.2.240 |
| UI-28 | 低 | `AlchemyFurnaceScreen` | 悬浮文字绘制在光标携带物品下方 | 已修@0.2.238 |

### 3.2 玩法、迁移和验收台账

| ID | 优先级 | 剩余工作 | 完成证据 |
|---|---|---|---|
| QST-01 | P0 | 详细任务步骤的自然证据生产者 | 95/95 步有证明规则和生产入口；管理员证明不再是正常路径 |
| QST-02 | P0 | 证明路由的幂等、当前步骤和归属约束 | 重放、跨链、错步骤、他人击杀/交互均不能推进 |
| DLG-01 | P1 | 结构/线索/守卫/敌对/逮捕动作分型 | 每种作者动作有专用结果、失败语义和测试 |
| YYK-01 | P1 | 银翅夜叉巢专属编队 | 会话绑定、分层刷怪、绕行/战斗分支可玩 |
| YYK-02 | P1 | 阴芝马真实活捕与运输 | 活捕、击杀、死亡运输、归属和重复捕获均有明确结果 |
| YYK-03 | P1 | 窟外协作炼丹 | 预检、扣料、成功率、爆炉、分成、outbox 和幂等闭环 |
| DIM-01 | P1 | 维度状态分类诚实化 | 抽象模板/逻辑集群不再显示为未实现可玩维度 |
| DIM-02 | P2 | 仙界/修罗界空壳审计 | 基础版要么有最低可玩闭环，要么明确 `preview_locked` |
| ART-01 | P1 | 本命飞剑实例绑定与旧档迁移 | 只绑定合规飞剑实例；同 id 另一把剑不共享本命收益 |
| NPC-01 | P1 | 旧命名 Villager 持久标记迁移 | 名称伪造失败；合法旧 NPC 首次迁移后稳定识别 |
| QA-01 | P0 | 当前 JAR 单客户端全流程签字 | 新报告记录版本、协议、步骤和日志 |
| QA-02 | P0 | 专服 + 双客户端签字 | 竞价、工站、outbox、冷却、PvP、断线重连通过 |
| QA-03 | P1 | 90 类投影视觉与落点验收 | 点击面、替换方块、240 单元预算、Fast/Fabulous 通过 |
| QA-04 | P1 | 19 秘境与 10 场景验收 | 门禁、周期、多人归属、死亡、跨日、满包通过 |
| QA-05 | P1 | NPC/灵兽/维度长时验收 | 生成密度、卸载、重连、跨维、动画和导航通过 |
| QA-06 | P1 | VFX/HUD 性能与视觉验收 | 低粒子、透明排序、密集多人、长时状态无泄漏 |
| QA-07 | P2 | 资源与美术抽检 | GUI scale、资源包、物品/方块/实体构图抽检通过 |
| SEC-01 | 外部阻断 | 撤销或刷新曾暴露的 Minecraft 令牌 | 账号持有人书面确认，仓库不得记录新令牌 |

### 3.3 已排除的过期缺口

执行时不得重复实现或回退以下已完成内容：

- 47 个作者设施、90 类投影和目录设施 validator 已在 `0.2.176+` 收口；旧“34/92 可形成”结论已过期。
- 功勋加倍、逆星走私、星宫巡防、地域/势力 PvP 和采珠库存已有生产结算入口。
- FTB 回写不能绕过原生任务阶段门禁。
- 179 名地域 NPC 已有生产投放入口。
- 裂隙司南已扫描实际空间节点。
- `SCREEN_OVERLAY` 和 `MODEL_ANIMATION` 已有专用运行时，不再完全退化为普通粒子。
- 19 个秘境均已有独立维度映射、分层试炼壳、Boss 和奖励表；后续做的是专属深度和实机证明。

## 4. 统一执行规则

每个实现批次必须按以下顺序执行：

1. 重新读取 `ai_handoff.md` 顶部、`step_progress.md` 顶部和本计划，确认当前版本及是否有并发改动。
2. 用 `git status --short` 区分用户既有改动；只处理本批范围。
3. 为所有将编辑的既有文件创建 `.bak/<timestamp>_<slug>/` 备份并保留相对路径。
4. 先写或更新能复现缺陷的定向测试，再实现最小修复。
5. 代码、资源、数据包、构建逻辑或随模组发布的配置发生变化时，将 `mod_version` 增加一个补丁版本。
6. 网络包字段、顺序、类型、注册表或不兼容频道行为变化时，同时增加 `ModNetwork.PROTOCOL_VERSION`。
7. 先跑本批定向测试，再运行普通 `./gradlew build`；不得用 `-PaiSkipVersionBumpCheck=true` 收尾。
8. 更新本计划状态、`step_progress.md`、相关缺口文档和 `project_docs/updates/` 更新记录。
9. 检查 `git diff --check`、相关 diff 和 `git status --short`，只暂存本批文件。
10. 创建一个带中文主题、中文正文的本地提交；禁止自动 push、PR、amend、reset 或重写历史。

版本号按执行时的真实当前版本递增。本计划的 F-A 已使用 `0.2.233`，F-B 当前批次使用 `0.2.234`；后续批次不得复用这些编号，必须按实际工作树重新计算。

## 5. 依赖顺序

```text
前端 A（高危）
  └─> 前端 B（列表框架）
        └─> 前端 C1/C2/C3（中危与同文件问题）
              └─> 前端 D（低危）
                    └─> 前端 E（产品入口与旧屏清理）

任务证明清单
  └─> 任务自然事件生产者
        └─> 对话动作分型
              └─> 阴阳窟专属闭环

维度分类 ─┐
本命迁移 ─┼─> 旧档/专服/双客户端回归 ─> 发布签字
NPC 迁移  ─┘
```

前端和玩法两条主线可以交替提交，但一次构建只收口一个边界明确的批次。真实发布签字必须在全部代码批完成后重做，不能复用 `0.1.439` 或 `0.2.105` 的旧签字。

## 6. 前端实施批次

### F-A：五项高危交互修复（已完成@0.2.233）

范围：UI-01 至 UI-05。规模：M。协议：不变。

实施：

- `TechniqueEditScreen` 仅允许 `PENDING -> TECHNIQUE`，`SCROLLING` 状态永远不能提升为绑定拖拽。
- `QuestTrackerScreen` 保存“过滤器 + 可见 chain id 顺序”的视图签名；仅签名变化时重置列表滚动，仅选中 id 变化时重置详情滚动，其余同步只钳制旧值。
- `SectHallScreen` 提取唯一的 `visibleCandidates(snapshot, focusSectId)`，渲染、按钮创建、行数和滚动均消费同一列表。
- `AuctionSoftService` 在任何货币预留前拒绝当前最高价玩家再次出价；拒绝路径不得增加 raises、不得强制结算。客户端竞价按钮在请求后临时禁用，收到新 revision 或超时后恢复。
- `SectHallScreen` 观察客户端宗门快照 revision/稳定指纹，变化后只重建动作控件并保留合法页签与滚动；入宗、任务接取/交付和贡献变化都能刷新。

自动化：

- 扩充 `DragDualScrollTest`：证明 `SCROLLING` 不能提升、`PENDING` 达阈值后才可提升。
- 扩充 `ClientQuestTrackerDataTest`/新增屏幕状态测试：相同签名保留滚动、选择变化只重置详情。
- 扩充 `SectPacketTest`、`MenuActionAuthorityTest` 或新增纯函数测试：过滤后的行、按钮目标和数量完全一致；同步指纹变化触发重建。
- 扩充 `AuctionSoftServiceTest`：领先者重放、同 tick 双击、第五次 raises 边界均不重复扣款。
- 跑 `ScreenLayoutTest`、`MarketAuctionPagingTest` 及完整构建。

人工验收：窄屏滚动技法列表穿过槽位区不绑定；任务同步不跳顶；从指定执事打开宗门候选时每个按钮对应同一行；连续双击竞价只结算一次；入宗后无需重开屏幕即可看到成员控件。

完成记录：`TechniqueEditScreen` 仅允许 `PENDING` 提升为技法拖拽；任务列表以过滤器和可见任务 ID 顺序决定列表滚动重置，并独立追踪详情选择；宗门候选渲染、按钮和滚动共用 `visibleCandidates`，客户端快照值变化会重建动作控件；服务端在扣灵石和推进竞价前拒绝当前最高价玩家重放，客户端在同步或 40 tick 超时前按拍品禁用重复竞价。`DragDualScrollTest`、`ClientQuestTrackerDataTest`、`SectHallInteractionTest`、`AuctionSoftServiceTest`、`MarketAuctionPagingTest`、`MenuActionAuthorityTest` 及完整构建通过。真实客户端手势验收仍留在本计划的最终 QA 阶段。

### F-B：统一滚动列表输入契约（已完成@0.2.234）

范围：UI-06、UI-18，以及所有 `ScrollableListPanel` 消费者。规模：M。协议：不变。

实施：

- 在 `ScrollableListPanel` 内建立 `IDLE -> PENDING_ROW/PENDING_TRACK -> DRAG_CONTENT/DRAG_THUMB -> IDLE` 状态机。
- 左键按下只记录候选；超过阈值才滚动，未超过阈值在松手时返回行点击。
- 滚动条轨道、拇指和内容区独立命中；点击轨道分页，拖动拇指按比例映射滚动，绝不击穿到底下行。
- 行命中应用 content insets；行间隙和末行下方空白不属于任一行。
- 右键和中键不进入行选择状态；保留屏幕自己的右键语义。
- `QuestTrackerScreen`、`UiThemeSelectScreen` 及大厅类统一改为消费松手点击结果，不再抢先命中行。

自动化：新增 `ScrollableListPanelInteractionTest`，覆盖点击、轻微抖动、4px 阈值内容拖动、拇指拖动、轨道分页、insets、行间隙、末尾空白、滚轮取消和非左键；完整单元测试继续通过。

完成记录：`ScrollableListPanel` 现在以 `IDLE/PENDING_ROW/PENDING_TRACK/DRAG_CONTENT/DRAG_THUMB` 状态机区分行点击、内容拖动和滚动条操作；行列表使用整数首行滚动，连续详情使用像素滚动；命中几何统一应用 content inset、行间隙和有效视口，轨道分页/拇指拖动不会击穿行。`QuestTrackerScreen`、`UiThemeSelectScreen`、炼丹/炼器详情屏及世界包、宗门、坊市、拍卖大厅均完成按下/拖动/松手转发，按钮先于列表处理。新增交互测试并修正宗门候选消费者契约计数。`mod_version=0.2.234`，协议保持 `30`。真实 Minecraft 客户端仍需用滚轮、内容拖动和滚动条拖动逐屏签字。

### F-C1：任务、对话、编年史与 Lore（已完成@0.2.236）

范围：UI-07 至 UI-11、UI-19。规模：M。协议：不变。

实施：

- 任务过滤器不再调用全局 `selectChain`；被过滤的当前选择保留，详情显示“当前条目不在过滤结果”或继续显示详情。
- `ClientPacketHandlers.handleOpenDialogue` 在当前为 `AbstractContainerScreen` 等服务端菜单屏时拒绝覆盖；已有 `DialogueScreen` 可由同一权威会话刷新或替换。
- `DialogueScreen` 的 `actionPending` 记录开始 tick，100 tick 无后续包时恢复；`init()` 重建按钮时继承真实 pending 状态。
- NPC 语音只在新会话第一次初始化播放，resize 不重播；新会话 id 才重置播放闩。
- `ChronicleScreen` 的列表标题、详情标题和正文入口共用 discovered 判定，锁定状态不泄露真实名称。
- `AbstractLoreScreen` 增加详情滚动、内容高度测量和钳制；选择真正变化时才归零。

自动化：扩充 `ClientQuestTrackerDataTest`、`DialogueScreenLayoutTest`，覆盖过滤保留全局选择、动作闩超时和容器界面覆盖策略；Lore 详情滚动、编年史锁定标题和同会话语音闩由源码契约与屏幕布局回归覆盖。

完成记录：任务过滤不再改写全局选择，隐藏选择仍显示详情并提示当前筛选状态；对话包不会覆盖 `AbstractContainerScreen`，同一会话可刷新；动作闩 100 tick 无回包自动恢复且控件重建继承闩状态；编年史时间线与详情在未解锁时统一显示泛化锁定文本；Lore 详情使用共享滚动/测量/钳制；NPC 招呼语仅在会话首次初始化播放。`ClientQuestTrackerDataTest`、`DialogueScreenLayoutTest` 及完整构建通过。真实客户端的过滤、容器覆盖、超长详情和 resize 验收仍留在最终 QA。

版本与协议：`mod_version=0.2.234 -> 0.2.236`（生成档案按 `spell -> visual` 顺序刷新）；未改变网络包字段、顺序、类型、注册或频道行为，`ModNetwork.PROTOCOL_VERSION=30` 保持不变。备份：`.bak/20260729_0.2.235_f_c1/`。

人工验收：切换过滤器、模拟无响应动作、resize 对话、打开容器时收到对话包、阅读超长 Lore、查看未解锁编年史。

### F-C2：储物手镯与功法树（已完成@0.2.237）

范围：UI-12 至 UI-14、UI-24 至 UI-26。规模：M。协议：不变。

实施：

- `StorageBraceletMenu.stillValid()` 客户端只判断绑定手仍持受支持手镯；服务端继续校验同一实例/槽位、所有者、境界和完整性。
- 功法树按下节点只记录候选和旧布局基线；未移动松手才选中，拖动则只提交最终合法偏移。
- 命中检测倒序遍历绘制后的 `graphHits`，保证选中视觉最上层节点。
- 拖动期间忽略无关按键；关屏前冲刷尚未提交且非零的偏移。
- 仅实际位置变化才发 `set:id:x:y`；切换流派和真实选中变化时归零详情滚动。

自动化：扩充 `ArtifactStorageAuthorityTest`、`DragDualScrollTest`、`MethodLayoutServiceTest`、`ScreenLayoutTest`；增加服务端仍严格、客户端实例替换后可预测、重叠命中倒序和零位移不发包的断言。

完成记录：`StorageBraceletMenu.stillValid()` 在客户端按同一支持物品和槽位保留预测，服务端仍要求原始 `ItemStack` 身份并通过连续所有权、境界和完整性校验；功法图节点按下只建立候选，达到 4px 阈值后才拖动，松手才选择并提交实际偏移；命中检测倒序使用最后绘制节点；Esc/其他按键/关屏会幂等冲刷最后一次实际布局变化，零位移点击不发送布局包；切换流派清零详情滚动。`ArtifactStorageAuthorityTest`、`DragDualScrollTest` 与完整构建通过。生成档案按 `spell -> visual` 顺序刷新，2,292/5,727 profiles 检查通过。

版本与协议：`mod_version=0.2.236 -> 0.2.237`；仅改变客户端交互、服务端菜单校验和测试/生成资源，未改变网络包字段、顺序、类型、注册或频道行为，`ModNetwork.PROTOCOL_VERSION=30` 保持不变。备份：`.bak/20260729_0.2.237_f_c2/`。

人工验收：手镯连续拖拽/Shift 移物；功法节点点击、拖动、重叠、右键干扰、Esc 关闭、重开后位置一致。自动化已完成，真实客户端仍留在最终 QA。

### F-C3：丹炉显示与绘制层级（已完成@0.2.238）

范围：UI-15、UI-28。规模：S。协议：不变。

实施：

- 菜单暴露原始总时长或显式 `isCrafting()`；空闲时进度为 0，标签为本地化空闲态。
- 进行中仍按真实剩余/总时长绘制，异常负值或 total=0 安全钳制。
- 丹炉悬浮说明迁入容器标签层或等价的“槽位之上、携带物品之下”绘制阶段，不改变共享基类全局顺序。

自动化：扩充 `ScreenLayoutTest` 和丹炉菜单测试，覆盖空闲、开始、进行中、完成边界及绘制入口契约。

完成记录：`AlchemyFurnaceMenu` 保留原始零总时长并暴露 `isCrafting()`；丹炉屏幕空闲时显示本地化“空闲”、进度条为 0 且使用中性样式，进行中对剩余/总时长和异常值统一钳制。丹炉标签迁移到 `renderLabels`，原生槽位 tooltip 在其后绘制，避免光标携带物品覆盖顺序错误。`AlchemyFurnaceInteractionTest`、`ScreenLayoutTest` 与完整构建通过。

版本与协议：`mod_version=0.2.237 -> 0.2.238`；新增双语空闲标签，未改变网络包字段、顺序、类型、注册或频道行为，`ModNetwork.PROTOCOL_VERSION=30` 保持不变。备份：`.bak/20260729_0.2.238_f_c3/`。

人工验收：空闲/开始/进行中/完成边界和槽位悬浮提示仍需在最终真实客户端 QA 抽检。

### F-D：其余低危交互（已完成@0.2.240）

范围：UI-16、UI-17、UI-21、UI-23、UI-27。规模：M。协议：不变。

实施：

- 修炼滑条把 `pendingScale` 传过控件重建；服务端快照未确认前不会覆盖本地值，确认值到达后才清除 pending。
- 突破按钮收到请求后禁用至快照对象变化或 40 tick 超时；`BreakthroughService` 在服务端持久数据中写入 10 tick 闩，在扣料前拒绝短窗口重复请求。
- 拍卖屏首次打开请求第 0 页，后续 `init()`（包括 resize）复用客户端当前页，服务端页数变化时仍由快照钳制。
- 背包“修仙”按钮以 `InventoryScreen.getGuiLeft()/getGuiTop()` 为锚，GUI 原点随配方书开合和窗口 resize 重新读取。
- 生活技能树用统一的行距、段间距和上下 inset 计算内容高度，最大滚动把末行停在底部 inset 内。

自动化：扩充 `CultivationStatsInteractionTest`、`BreakthroughRequestGateTest`、`MarketAuctionPagingTest`、`ScreenLayoutTest`；覆盖滑条重建、客户端突破闩、服务端 tick 边界、拍卖 resize、GUI 原点和末行滚动高度。

完成记录：F-D 五项均已落地；客户端突破闩在快照同步或 40 tick 超时后恢复，服务端 10 tick 请求闩在资源扣除前拒绝重复包；拍卖页 resize 不再回到第 0 页；背包入口改用原版 GUI 原点；生活技能树内容高度与上下 inset 精确闭合。

版本与协议：`mod_version=0.2.238 -> 0.2.240`。首次 0.2.239 构建后补充客户端突破闩触发版本指纹门禁，最终按规则递增至 0.2.240；未改变网络包字段、顺序、类型、注册或频道行为，`ModNetwork.PROTOCOL_VERSION=30` 保持不变。备份：`.bak/20260729_0.2.239_f_d/`。

最终构建：`./gradlew build --no-daemon --max-workers=1 --console=plain` 成功，1,210 项测试 failure/error/skipped 均为 0；JAR SHA-256 为 `c61063bc3a214328cc6901b163740fd0971153b6ba6f9ee7e13a313d431d95f7`。

人工验收：滑条同步、突破重复点击、拍卖 resize、配方书开合、生活技能树末行仍需最终真实客户端 QA 抽检。

### F-E1：秘境入口按钮产品收口

范围：UI-20。规模：S/M。协议：优先复用现有 `WorldpackActionPacket` 字段，格式不变。

推荐产品语义：秘境仍必须通过真实界门或服务端剧情入口进入；世界包界面不提供绕过门禁的“直接进入”。禁用的无反馈按钮改为以下之一：

1. 有已验证界门/锚点时显示“定位界门”，由服务端返回或记录最近合法入口坐标。
2. 无入口时显示“查看条件”，点击反馈区域、境界、周期、凭证、任务 flag 和当前冷却中的第一项阻断原因。
3. 当前秘境已开启时显示状态，不发送进入请求。

验收要求：按钮永不静默；客户端不能指定任意维度或坐标；现有物理门禁仍由服务端重新验证。若用户明确要求界面直接进入，则必须另立安全设计批次，复用 `SecretRealmOpenPolicy` 全量门禁并审计其是否改变频道行为。

完成记录：秘境行不再放置无反馈的禁用按钮。快照中的可用锚点显示“定位界门”，点击只发送复用 `WorldpackActionPacket` 字段的 `locate` 信息动作，由服务端按秘境所属地域解析已验证锚点并返回维度/坐标；无可用入口显示“查看条件”，服务端按地域、境界、开放周期/剧情旗标、冷却、锚点和凭证顺序反馈首项阻断原因；当前已有秘境会显示状态并禁用动作。定位与条件动作均不调用进入事务，客户端不能指定维度或坐标，现有界门/剧情入口继续由 `SecretRealmOpenPolicy` 与 `enterSecretRealm` 重新校验。同步快照的 `anchorReady` 现在还会验证目标维度存在，避免失效锚点误报为可定位。

自动化与版本：`ClientWorldpackDataTest` 覆盖定位/条件/进行中三态；`SecretRealmEntryAuthorityTest` 证明旧直接进入包仍不可达、信息动作复用现有包且服务端解析锚点；`MarketWorldpackPacketTest` 与完整测试通过。生成档案按 `spell -> visual` 顺序刷新，2,292/5,727 profiles 检查通过。首次 `0.2.241` 构建后又收紧可用锚点校验，版本指纹门禁要求递增至 `mod_version=0.2.242`；网络字段、顺序、类型、注册和频道行为未改，`ModNetwork.PROTOCOL_VERSION=30` 保持不变。最终普通 `./gradlew build --no-daemon --max-workers=1 --console=plain` 成功，1,211 项测试 failure/error/skipped 均为 0，JAR SHA-256 为 `00b6647d35380694eea2cf834d58a946a626396cba71161ef2adbb406c2f825b`。备份：`.bak/20260729_0.2.241_f_e1/`。

### F-E2：删除不可达旧屏幕

范围：UI-22。规模：S。协议：必须从 30 增加到下一协议号。

实施：

- 删除 `SectScreen`、`ShopScreen`、`AuctionScreen`。
- 删除无生产发送方的 `OpenAuctionScreenPacket` 及其消息注册。
- `ClientPacketHandlers` 的宗门/商店同步只刷新数据或当前 Hall，不再以同步包强开旧 Screen。
- 删除只为旧屏存在的布局测试、语言键和导入；保留 `SectHallScreen`、`MarketHallScreen`、`AuctionHallScreen` 正式入口。
- 重新审计全部包 id、方向和协议拒绝测试，旧协议客户端必须明确无法连接。

该批次不可与普通 UI 修复混在同一提交，以便单独回滚网络消息表变化。

完成记录：删除 `SectScreen`、`ShopScreen`、`AuctionScreen` 以及无生产发送方的 `OpenAuctionScreenPacket`；移除旧包注册和客户端旧屏处理器。宗门/坊市同步现在只更新 `ClientSectData`/`ClientShopData`，正式 `SectHallScreen`、`MarketHallScreen`、`AuctionHallScreen` 仍通过菜单路径打开。布局测试改为只覆盖正式 Hall，旧屏独占语言键清理，方向契约排除旧包并新增文件/协议回归断言。

自动化与版本：F-E2 定向测试（网络方向、屏幕布局、多人权威、视觉协议契约）通过；两个受控生成器按 `spell -> visual` 顺序刷新并通过 `--check`，分别为 2,292 与 5,727 profiles。`mod_version=0.2.242 -> 0.2.243`；删除网络消息注册导致协议从 `30 -> 31`，旧协议客户端将被频道版本拒绝。普通 `./gradlew build --no-daemon --max-workers=1 --console=plain` 成功，255 个测试套件、1,210 项测试 failure/error/skipped 均为 0，JAR SHA-256 为 `02e2eabaac77061d59a0063627e61655340100ca091c726cc93b904122c7d128`。备份：`.bak/20260729_153000_f_e2_legacy_screen_cleanup/`。

下一实施入口：Q-A 建立 95 步详细任务证明路由；F-E2 及此前前端批次的真实客户端验收仍留在最终 QA。

## 7. 详细任务与对话实施批次

### Q-A：建立 95 步证明路由清单

规模：M。协议：不变。

新增数据驱动的证明路由资源，例如 `detailed_quest_proof_routes.json`。每条记录至少包含：

- `chain_id`、`step`、`proof_type`、`event_id`。
- 必需的 region/dimension/entity/item/station/NPC/choice 参数。
- `owner_policy`、`party_policy`、`consume_policy`、`repeat_policy`。
- 失败提示键和是否允许历史证据回放。

允许的证明类型固定为服务端可验证枚举，例如：

- `REGION_ENTER`、`DIMENSION_ENTER`、`STRUCTURE_FORMED`、`NPC_DIALOGUE`。
- `ITEM_ACQUIRED`、`ITEM_DELIVERED`、`CRAFT_COMPLETED`、`ALCHEMY_COMPLETED`。
- `ENTITY_KILLED`、`ENTITY_CAPTURED_ALIVE`、`ENCOUNTER_CLEARED`、`ESCORT_COMPLETED`。
- `METHOD_LAYER_REACHED`、`REALM_REACHED`、`TECHNIQUE_LEARNED`。
- `SHOP_TRANSACTION`、`AUCTION_TRANSACTION`、`REPUTATION_REACHED`、`CHOICE_COMMITTED`。
- `INFO_ACKNOWLEDGED`，仅用于纯规则告知，必须来自对应 NPC/界面动作，不能由客户端任意字符串证明。

当前资料中有 36 步既无结构化 `place` 也无 `need`，必须逐条显式映射：

| 任务链 | 步骤 |
|---|---|
| `mortal_qixuan_entry` | 4 |
| `nangong_wan_weight_optional` | 2 |
| `star_palace_register` | 4 |
| `inverse_star_intro` | 2、4 |
| `tianyuan_landing_register` | 4 |
| `tianyuan_to_fengyuan_gate` | 4 |
| `zhenyan_outer_lesson` | 3、4 |
| `wuxing_intro` | 3、4 |
| `reincarnation_intro` | 3 |
| `court_hunt_gray` | 2、3、4 |
| `heifeng_gray_sail` | 2、3 |
| `xutian_window_prepare` | 1、3 |
| `zhuimo_token` | 1、2 |
| `lingzhu_fruit_run` | 3、4 |
| `dayan_clue` | 1、2、3、4 |
| `guanghan_endgame_path` | 1 |
| `deity_huoyu_path` | 1、2、3、4、6、7、8、9 |

同时审计其余 59 步：有 `place/need` 不等于有生产者，必须证明相应地域、NPC、结构、物品或状态事件真的会调用路由。

自动化验收：

- 23 链、95 步全部且只映射一次；0 条 `ADMIN_ONLY`。
- 所有事件 id、物品 id、实体 id、地域 id、维度 id、NPC id 和工站 id 可解析。
- 未知证明类型、未知参数、重复映射和无生产者映射使构建失败。
- 不解析中文 `do` 文本猜测状态。

完成记录：新增 `detailed_quest_proof_routes.json`，为 23 条可玩任务链逐步映射 95 条服务端证明路由；`DetailedQuestProofCatalog` 严格校验 schema、链/步骤覆盖、证明类型、参数键、策略、生产者、事件 id 和失败键，并在 `DetailedQuestRuntimeService` 初始化时与当前任务快照交叉核对。禁止 `ADMIN_ONLY`，未知路由会使运行时初始化失败，不再从中文 `do` 文本猜测证据。新增 `DetailedQuestProofCatalogTest` 覆盖 23/95 完整性、唯一事件 id、策略、参数和运行时查找。`mod_version=0.2.243 -> 0.2.244`，协议字段、顺序、注册和频道行为不变，`ModNetwork.PROTOCOL_VERSION=31` 保持不变。Q-A 不接入自然事件生产者；下一批为 Q-B。提交：`cc5d0a2c feat: 建立详细任务证明路由目录`。

### Q-B：接入自然事件生产者

规模：L，拆成多个补丁批，每批一个事件域。协议：原则上不变。

建议顺序：

1. 修炼/境界/功法/术法事件。
2. 地域/维度/秘境/结构事件。
3. 物品获取、制作、炼丹、炼器和交付事件。
4. 击杀、活捕、护送、遭遇和队伍归属事件。
5. NPC 对话、选择、商店、拍卖、声望和规则告知事件。

统一通过 `DetailedQuestProofService.record(player, event)` 验证当前步骤并推进，禁止各业务直接拼 `quest_step_*` 字符串。一次事件最多推进同一任务的一步；奖励仍走既有幂等账本和 outbox。

安全回归必须覆盖：未开始任务、错步骤、错地域、错实体、他人击杀、受控单位归属、队伍成员、死亡克隆、断线重连、重复事件、并发交付和旧档已有证据。

管理员 `prove` 保留为 permission 2 的诊断/救档命令，但状态输出应标明“管理员证明”，并可从正常可玩性覆盖统计中排除。

#### Q-B-1：修炼、境界、功法与术法生产者（已完成，`0.2.245`）

新增 `DetailedQuestProofEvent` 与 `DetailedQuestProofService`，自然事件只能由服务端生产者创建；服务按当前任务步骤、路由参数、玩家 UUID 和实时能力状态校验后，调用 `advanceVerifiedRoute` 进入既有奖励账本。证明账本上限为 512 条，死亡克隆复制账本与历史，登录时只重放允许历史回放的事实；管理员 `prove/claim` 单独走 permission 2 路径并写入 `Admin=true`。

已接入功法学习/升层/自动授予、术法解锁/手册学习、无雷劫突破成功和雷劫最终成功生产点。修正功法与境界路由为可解析的真实 id，目录校验 `minimum_layer`/`minimum_realm`；梵圣真魔功调整为元婴门槛，并提供元婴后期/圆满、明王诀+托天魔功+梵圣真片的一次性拼合事务。

定向测试与完整构建通过；Q-B 其他事件域尚未接入，非修炼证明的默认权威校验仍需在各域迁移时收紧。

### D-A：对话世界动作分型

规模：L，可按动作家族拆分。协议：优先复用现有效果参数，不改包格式。

当前 `DialogueWorldActionService` 已提供真实结构验证、有界提示/异常/疑点记录及通用敌对壳；后续不是从零实现，而是消除过度泛化：

- `mark_structure`：保留真实锚点/成型结构校验，增加结构类别、维度和当前任务步骤匹配；不可标记无关同名结构。
- `hint/clue`：线索记录绑定来源 NPC、节点、区域、世界时间和唯一线索 id；重复读取不重复推进或发奖。
- `call_guard`：生成或召回对应势力的 guard 角色，绑定执法目标和管辖范围，不再统一为 generic servitor。
- `combat_flag`：只建立敌对/战斗后果，不自动冒充逮捕。
- `combat_or_arrest`：根据疑点、声望、任务分支和玩家状态选择警告、缴罚、押送、战斗；逮捕成功必须有可恢复落点和明确解除条件。
- `add_suspicion/anomaly_log`：按势力/NPC 分桶，设置上限、衰减或结算点，不用排序删除键模拟时间淘汰。

增加动作覆盖对账测试：作者资源中的每种 effect 都必须映射到明确 handler；未知动作失败关闭。再增加世界结果测试和两客户端归属测试。

## 8. 阴阳窟专属实施批次

### Y-A：专属场景与银翅夜叉编队

规模：L。协议：不变。

- 在独立 `secret_realm_yinyang_ku` 维度生成入口、夜叉巢、阴芝马栖息层和出口/炼丹接应区；结构生成使用会话稳定种子并持久记录。
- 使用现有 `CultivationBeastEntity` 数据驱动实体承载 `silver_wing_yaksha`，避免再注册重复实体类型。
- 编队至少区分外围幼体、巢区成体和领队；风/土遁、幻术或阴煞效果取现有作者术法/状态能力，数量受会话预算约束。
- 战斗与绕行是显式分支：献祭/资源绕行必须原子扣除；战斗单位全部绑定秘境 session、玩家/队伍和到期时间。
- 和平难度、生成失败、区块卸载、重连和多人同时进入必须失败关闭或可恢复，不得提前写完成闩。

### Y-B：阴芝马活捕与运输

规模：L。协议：不变。

- 将 `yin_zhi_ma` 作为可识别的专属灵兽画像进入栖息层，生成数量和重生规则由会话控制。
- 活捕要求合法会话、目标归属、残血/镇静条件和专用容器；成功后移除实体并生成带唯一捕获 id、生命状态、来源 session 的活体载体。
- 击杀只产生劣化材料，绝不能发 `yin_zhi_horse_live`；运输途中死亡或超时转换为劣材，并提交对应失败分支。
- 活体载体不可堆叠复制、不可被另一会话重复提交；丢弃、死亡、满背包和断线用 outbox/恢复账本处理。
- 任务只接受 `ENTITY_CAPTURED_ALIVE` 或验证后的交付事务，不接受同名普通物品。

### Y-C：窟外协作炼丹

规模：L。协议：如需多人交互新菜单再单独评估；优先复用现有丹炉与对话动作。

- 在出窟接应点提供 NPC 协作与独炼分支；两者都要求已成型可运行丹炉和合法活体/劣材输入。
- 事务顺序固定为：校验会话和任务步骤 → 预留材料/贡献 → 锁定活体 → 计算成功率 → 提交产物/失败 → 标记账本。
- 成功率以作者约两成基线为核心，并显式叠加活捕、炼丹熟练度、协作和工站效率；所有上下限有纯逻辑测试。
- 爆炉对工站耐久和玩家状态产生既有体系内后果；失败不能吞掉未声明材料，重复包不能重复投骰。
- 产物、分成和退款统一使用 `InventoryDeliveryService.giveOrEnqueue`；多人协作按参与快照发放，不依赖在线瞬时状态。

闭环验收：从接取情报、进入阴阳窟、选择绕行/战斗、活捕或击杀、出窟、协作/独炼直到领取培婴相关奖励全程不使用管理员命令。

## 9. 维度、本命法宝与 NPC 迁移

### M-A：维度状态分类

规模：M。协议：不变。

- 将 `seeking_immortals:secret_realm_instance` 定义为 `abstract_template`，它是 dimension type/实例模板，不是玩家可进入维度，也不是欠一份同名维度 JSON。
- 将 `seeking_immortals:yin_underworld` 定义为 `logical_cluster`，明确映射 `yin_ming_pocket` 和 `nether_river_pocket`，不计入待实现数。
- `DimensionRegistryService`、管理命令、对账资源和 UI 分开展示可玩、预览、抽象和逻辑条目。
- 审计 `immortal_realm`、`asura_realm`：如果只有共享 meadow/overworld 生成器与通用入口场景，基础版标为 `preview_locked` 并关闭普通旅行；若要标 `playable`，必须至少有唯一入口、返回/单向规则、落点、环境、遭遇、奖励或任务循环和实机签字。
- 仙界全量城市、势力和 DLC 数据另立 Epic，不以改一个布尔值冒充完成。

自动化：更新 `M13DimensionsAscensionTest`，断言抽象/逻辑条目不在 deferred backlog，所有 playable 条目有实际 datapack 维度和可达服务端入口，所有 preview 条目不能正常旅行。

### M-B：本命飞剑语义与迁移

规模：L。协议：不变；会改变兼容 NBT，必须有迁移测试。

- 普通 `ArtifactOwnershipService.claim` 只认主，不再自动调用 `NatalBindingService.bind`。
- 推荐交互：一手持 `natal_sword_embryo`，另一手持已认主、类型为飞剑且满足结丹门槛的目标，潜行使用后原子消耗胚并绑定目标。
- 玩家本命根记录 `artifact_id + instance_uuid + schema_version + growth`；目标物品记录相同 instance UUID。只按 artifact id 比较不再足够。
- 激活、成长、祭炼、器灵和倍率只作用于精确实例；同 id 的第二件物品不能继承本命收益。
- 他人持有、复制 NBT、目标损坏、目标不在手中、已有本命、背包替换和交易边界均失败关闭。
- 旧档只有 `ArtifactId/Growth` 时不自动丢失进度：标记为 legacy binding，首次持有唯一匹配物品时迁移；存在多个同 id 候选时要求玩家用胚确认，不猜目标。提供 permission 2 诊断/救档命令。
- `natal_sword_embryo` tooltip 从“待实现”改为准确的双手绑定说明；`ArtifactActivationService.hasActivation` 不再把绑定工具误当战斗主动技。

自动化：扩充 `ArtifactActivationServiceTest`、`ArtifactBindsAndMethodMatrixTest`、`ArtifactRefinementServiceTest`、克隆策略测试；新增实例唯一性、旧档歧义、失败不耗胚、成功只耗一次和重连持久化测试。

### M-C：旧命名 NPC 迁移

规模：M。协议：不变。

- 为兼容 Villager 使用持久 `SeekingImmortalsNpcId`、迁移版本、来源区域和迁移时间；专用 `CultivatorNpcEntity` 继续使用自身权威 id。
- 名称识别只允许在“无持久 id + 已知旧版实体类型 + 合法命名 NPC + 匹配区域/锚点 + 尚未迁移”的窗口执行一次。
- 首次合法交互后写入持久 id；之后 `QuestService`、`TextQuestNpcHookService`、对话和奖励只读持久 id。
- 玩家用命名牌制造同名 Villager 时，因为缺少合法锚点/迁移来源而被拒绝。
- 提供世界升级审计命令，报告已迁移、歧义、拒绝和仍待处理数量；不要自动删除玩家实体。
- 经过至少一个兼容发布周期且实机旧档通过后，再单独删除名称回退。

自动化：扩充 `QuestNamedVillagerAuthorityTest`、`TextQuestNpcHookServiceTest`、`NamedNpcPlacementSavedDataTest`，覆盖名称伪造、合法迁移、重启、跨维、重复实体和不匹配区域。

## 10. 验证与发布签字

### 10.1 每批自动门

- 新行为的纯逻辑、源码契约或资源对账测试。
- 受影响包的定向测试。
- 中英文 JSON 解析与语言键对等。
- 生成器 `--check`；若 Java 审计哈希变化，按依赖顺序更新术法效果目录再更新视觉目录。
- `git diff --check`。
- 普通 `./gradlew build`，不跳过 `aiPreflight`。

### 10.2 单客户端签字矩阵

使用当前构建 JAR 新建世界和升级存档各一次：

1. 首登、能力同步、指南书、修炼命令和基础 HUD。
2. 全部前端修复项对应手势；至少覆盖最小、默认和宽屏 GUI scale。
3. 90 类投影抽样与高风险大结构全测：六个点击面、可替换方块、双手切换、锁定/解除、逐层、240 单元预算、Fast/Fabulous。
4. 19 个秘境逐一进入，重点覆盖周期边界、门票、任务 flag、死亡、返程、满背包和阴阳窟完整闭环。
5. 10 类入口场景的构图、司南方向和跨区块加载。
6. NPC/灵兽的模型、动画、飞行/水生导航、生成密度、区块卸载、重连和跨维跟随。
7. 术法/VFX 的 beam、cone、wall、场域、召唤、低粒子档、透明排序和长时清理。
8. 物品栏、方块六面、BlockItem、第三人称持物、JEI、Patchouli 和资源包覆盖抽检。
9. `logs/latest.log` 搜索本模组 `ERROR`、`Exception`、缺纹理、缺声音、codec 和注册表错误。

### 10.3 专服和双客户端签字矩阵

更新旧 `manual_multiplayer_signoff_checklist.md`，不得继续写死 `0.2.105/协议 26`。至少执行：

- 两玩家 PvP、友军过滤、受控单位击杀归属。
- 同一工站并发形成、制作、修理和拆解。
- 拍卖双击、自抬价、互相抬价、离线退款、强制结算。
- 满背包任务/制作/拍卖/阴阳窟奖励进入 outbox，断线重连后不丢不双发。
- 术法冷却、突破防重、对话动作闩、菜单权限在延迟与重连下正确。
- 同一秘境多人抢杀、奖励归属、队伍共享、外人投射物、死亡和跨日。
- NPC/伴生兽卸载、换维、主人离线和服务器重启。
- 旧协议客户端连接被拒绝，当前协议客户端同步正常。

签字报告必须由实际执行者写入时间、JAR SHA-256、mod_version、protocol、服务端/客户端日志位置和失败重试记录。

### 10.4 性能门

- 客户端：投影、多人 VFX、HUD 和大量实体场景无持续增长队列；记录平均/高分位帧时间，不只写“肉眼流畅”。
- 服务端：密集事件、秘境、NPC 和工站场景记录 tick time；现有有界扫描不得在多人场景形成无上限乘积。
- 网络：记录密集施法和同步包数量；UI revision 修复不得每 tick 重建控件或发包。
- 存档：长时运行后 SavedData/NBT 有界，线索、战斗、任务证据和会话账本不无限增长。

## 11. 版本与协议决策表

| 批次 | `mod_version` | 协议默认判断 |
|---|---|---|
| 本计划文档 | 不变，当前执行版本为 `0.2.240` | 保持 30 |
| F-A 至 F-D | 每个已提交代码批 +1 patch | 不改包格式则保持当前协议 |
| F-E1 | +1 patch | 复用现有字段则不升；新增/改字段则升 |
| F-E2 旧屏清理 | +1 patch | 删除消息注册，必须升协议 |
| Q-A/Q-B、D-A | 每个发布批 +1 patch | 仅服务端资源/事件时不升 |
| Y-A/Y-B/Y-C | 每个发布批 +1 patch | 无新菜单/包时不升；新增交互协议则升 |
| M-A/M-B/M-C | 每个发布批 +1 patch | 数据/NBT 兼容迁移不等于网络变更，通常不升 |
| 纯人工报告/文档 | 不变 | 不变 |

任何“协议不变”都是实施前判断；提交前必须用实际 diff 审计 `network/` 和 `ModNetwork` 后重新确认。

## 12. 建议提交拆分

建议至少使用以下独立本地提交，不把所有工作压成一个巨型版本：

1. `fix: 修复高危前端交互`
2. `refactor: 统一滚动列表输入契约`
3. `fix: 收口任务对话与典籍交互`
4. `fix: 修复功法树与储物手镯交互`
5. `fix: 修复丹炉与低危界面问题`
6. `feat: 补齐详细任务证明路由`
7. `feat: 接入详细任务自然事件`
8. `feat: 深化对话世界动作`
9. `feat: 完成阴阳窟专属玩法`
10. `refactor: 诚实化维度状态分类`
11. `fix: 收口本命飞剑实例绑定`
12. `fix: 迁移旧版命名任务 NPC`
13. `refactor: 删除不可达旧界面`（独立协议提交）
14. `docs: 完成当前版本实机复签`

每个提交正文都必须说明实际更新、验证结果、mod_version 和协议决定；提交主题可按实际批次再细分。

## 13. 风险与回滚策略

- UI 状态机风险：优先把输入判定抽成纯函数/小模型，保留旧文件备份；框架批必须逐屏回归。
- 经济事务风险：所有货币和材料操作使用“预检/预留 → 副作用 → 提交/退款”，拒绝路径发生在扣除前。
- 任务风险：证明只匹配当前步骤和明确事件；禁止任意活跃任务回退与中文文本推断。
- 秘境风险：实体、奖励、活捕物和炼丹事务都绑定 session/owner/ledger，旧会话只允许清理不补发新奖励。
- NBT 迁移风险：先兼容读、再写新 schema；迁移前保留旧字段副本或可诊断信息，不批量猜测歧义实例。
- 协议风险：旧屏清理单独提交并升协议，保证可整批回滚。
- 人工验收风险：报告记录失败项，不用旧版本签字或自动测试冒充当前人工通过。
- 外部凭据风险：仓库只记录“持有人已确认撤销”的状态，不记录 token、截图凭据或诊断包。

## 14. 计划维护规则

- 每完成一个批次，更新本文件对应台账状态为 `已完成@<mod_version>`，附更新记录与本地提交短哈希。
- 若源码证明某项已在其他批次解决，必须给出测试、diff 或提交证据后再改状态，不能仅凭旧文档勾选。
- 新发现的问题先登记 ID、严重度、复现和依赖，再决定插入哪个批次。
- 产品决策项 UI-20、仙界/DLC 范围和任何直接秘境入口扩权，必须先记录用户决定。
- 被暂缓项必须写明原因、重新评估条件和玩家可见降级行为。
- 计划完成后，归档过期的 `master_plan.md`/旧 smoke 文档入口，确保新代理只从当前真相开始。

> CURRENT TRUTH 2026-07-29: `0.2.240` 已完成后续实施计划 F-D（UI-16、UI-17、UI-21、UI-23、UI-27）。修炼滑条 pending 值跨控件重建保留并在服务端快照确认后清除；突破客户端 40 tick 闩与服务端 10 tick 资源扣除前请求闩同时生效；拍卖大厅 resize 复用当前页；背包修仙入口跟随原版 GUI 原点；生活技能树内容高度包含实际段落/行距和上下 inset，末行不再被多滚。`CultivationStatsInteractionTest`、`BreakthroughRequestGateTest`、`MarketAuctionPagingTest`、`ScreenLayoutTest` 及普通构建通过，完整测试 1,210 项且 failure/error/skipped 均为 0。生成档案按 `spell -> visual` 顺序刷新，2,292/5,727 profiles 检查通过。`mod_version=0.2.240`，协议 `30` 保持不变，回滚目录为 `.bak/20260729_0.2.239_f_d/`。下一实施入口为 F-E1/UI-20 产品收口与独立协议批次 F-E2/UI-22；F-D 及此前各批次仍需最终真实客户端 QA。提交：`2fba841e`。
> CURRENT TRUTH 2026-07-29: `0.2.242` 已完成后续实施计划 F-E1/UI-20。秘境行根据已验证锚点显示“定位界门”，信息动作由服务端返回真实维度/坐标；无入口显示“查看条件”，服务端依次反馈区域、境界、周期/剧情旗标、冷却、锚点和凭证的首项阻断原因；已有秘境显示状态且不发送进入请求。同步快照只把目标维度存在的锚点标为可用，所有动作均不调用直接进入事务。`ClientWorldpackDataTest`、`SecretRealmEntryAuthorityTest`、`MarketWorldpackPacketTest` 及普通构建通过，完整测试 1,211 项且 failure/error/skipped 均为 0。生成档案按 `spell -> visual` 顺序刷新，2,292/5,727 profiles 检查通过。首次 `0.2.241` 构建后因锚点校验收紧触发版本指纹门禁，最终 `mod_version=0.2.242`；协议 `30` 保持不变。JAR SHA-256 为 `00b6647d35380694eea2cf834d58a946a626396cba71161ef2adbb406c2f825b`，回滚目录为 `.bak/20260729_0.2.241_f_e1/`。下一实施入口为独立协议批次 F-E2/UI-22；F-E1 及此前各批次仍需最终真实客户端 QA。提交：`a9b10642`。

> CURRENT TRUTH 2026-07-29: `0.2.243` 已完成后续实施计划 F-E2/UI-22。删除三套旧宗门/坊市/拍卖屏和无生产发送方的 `OpenAuctionScreenPacket`，同步包只更新客户端数据，正式 Hall 菜单入口保持不变；协议从 `30` 升至 `31`，旧协议客户端会被频道版本拒绝。F-E2 定向测试、两个生成器 `--check` 与普通完整构建通过，255 个测试套件、1,210 项测试 failure/error/skipped 均为 0，JAR SHA-256 为 `02e2eabaac77061d59a0063627e61655340100ca091c726cc93b904122c7d128`。备份目录为 `.bak/20260729_153000_f_e2_legacy_screen_cleanup/`。下一实施入口为 Q-A；F-E2 及此前批次的真实客户端交互仍需最终 QA。提交：`3ae9d12b refactor: 删除不可达旧界面`。

> CURRENT TRUTH 2026-07-29: `0.2.244` 已完成后续实施计划 Q-A。新增并严格校验 23 条任务链、95 个步骤的证明路由；运行时会对链/步骤覆盖、证明类型、参数键、策略、生产者和事件 id 做失败关闭校验，`ADMIN_ONLY` 路由不被接受。新增 `DetailedQuestProofCatalogTest`，定向测试通过；协议字段、顺序、注册和频道行为未改，`ModNetwork.PROTOCOL_VERSION=31` 保持不变。Q-A 只建立数据路由，尚未把自然游戏事件迁移到统一 `DetailedQuestProofService`，下一实施入口为 Q-B；F-E2 及此前前端批次的真实客户端交互仍需最终 QA。备份目录为 `.bak/20260729_154000_q_a_proof_routes/`。提交：`cc5d0a2c feat: 建立详细任务证明路由目录`。

> 以下 `CURRENT TRUTH` 条目为历史批次记录，不代表当前实施入口。

> CURRENT TRUTH 2026-07-29: `0.2.237` 已完成 F-C2（UI-12 至 UI-14、UI-24 至 UI-26）。储物手镯客户端允许同一支持物品的 NBT 实例替换以保持预测，服务端继续锚定打开时的实例并校验连续授权；功法树节点按下候选、拖动阈值、释放提交和重叠倒序命中已收口；关屏/按键布局冲刷幂等，零位移点击不发包，切换流派详情滚动归零。`ArtifactStorageAuthorityTest`、`DragDualScrollTest` 及生成档案 `spell -> visual` 检查通过。`mod_version=0.2.237`，协议 `30` 保持不变，回滚目录为 `.bak/20260729_0.2.237_f_c2/`。下一代码批为 F-C3（UI-15、UI-28）；真实客户端手镯预测、图节点交互和布局重开验收仍留在最终 QA。
