# 寻仙问道 / Seeking Immortals

`seeking_immortals` 是面向 Minecraft Java Edition 1.20.1 的 Forge 修仙模组，以原创凡人流成长为核心，覆盖修炼、功法、炼丹炼器、宗门、任务、拍卖、秘境、阵法和多人服务端玩法。

当前项目是可构建、可游玩的深度 MVP，当前版本为 `0.2.269`。核心系统已经形成服务端权威闭环，但美术、叙事表现和部分大型世界内容仍在持续完善。

## 项目状态

| 项目 | 当前值 |
|---|---|
| 模组版本 | `0.2.269` |
| 网络协议 | `31` |
| Mod ID | `seeking_immortals` |
| Minecraft | `1.20.1` |
| Forge | `47.2.0` |
| Java | `17` |
| 构建系统 | ForgeGradle 6.x / Gradle Wrapper |
| 许可证 | All Rights Reserved |

版本和协议的最终真相分别是 [`gradle.properties`](gradle.properties) 与 [`ModNetwork.java`](src/main/java/com/xunxian/seekingimmortals/network/ModNetwork.java)。不要从旧构建产物或历史文档推断当前版本。

## 安装

客户端和服务端必须安装相同版本的模组与依赖。Curios、GeckoLib 和 Lodestone 是必需依赖；Patchouli、JEI、Architectury API 与 FTB 系列为可选兼容依赖。具体门槛以当前 `mods.toml` 为准：

| 依赖 | 状态 | 最低版本 |
|---|---|---|
| Forge | 必需 | `47.2.0` |
| Curios | 必需 | `5.0.0` |
| Patchouli | 可选 | `1.20.1-84` |
| JEI | 可选 | `15.0.0` |
| GeckoLib | 必需 | `4.8.4` |
| Architectury API | 可选 | `9.1.12` |
| FTB Library | 可选 | `2001.2.9` |
| FTB Teams | 可选 | `2001.3.0` |
| FTB Quests | 可选 | `2001.4.22` |
| Lodestone | 必需 | `1.20.1-1.6.4.1` |

安装步骤：

1. 安装 Minecraft 1.20.1 和 Forge 47.2.0。
2. 将本模组及上述依赖放入实例或服务器的 `mods/` 目录。
3. 确保客户端、服务端使用同一模组版本和协议。
4. 首次进入世界前备份存档；开发版本不承诺向后兼容所有实验数据。

当前构建产物位于 `build/libs/seeking_immortals-0.2.269.jar`。`build/` 目录和 JAR 不纳入 Git 提交。

## 开发构建

Windows PowerShell：

```bash
# 完整验证并构建 JAR
./gradlew --no-daemon --max-workers=1 build

# 开发客户端 / 专用服务端
./gradlew runClient
./gradlew runServer

# 数据生成
./gradlew runData
```

Linux/macOS 使用对应的 `./gradlew` 命令。完整构建会先执行 `scripts/preflight.sh`，检查版本递增和网络变更提示，然后编译、运行测试并重混淆 JAR。

## 玩法概览

### 修炼与角色成长

- 十阶境界路线，从炼气逐步成长至真仙。
- 灵力、修为、神识、肉身、寿元、灵根、特殊体质和走火入魔风险。
- 打坐吐纳、灵气浓度、灵脉、聚灵阵、突破材料与天劫流程。
- 重伤、心魔、碎丹、跌境伤疤等长期负面状态。

### 功法、术法与战斗

- 资源驱动的功法/术法目录与服务端学习门禁。
- 七槽技能栏、技能编辑、冷却、灵力消耗和熟练度成长。
- 功法树、层数培养、可拖拽节点布局及服务端持久化。
- 自定义弹射物、剑术、五行、魔道、鬼道、佛门、儒门、阵法与召唤效果。
- 灵兽、傀儡、鬼物和通用侍从实体；跨维度所有权和全局数量上限。

### 生产、物品与法宝

- 炼丹炉 GUI、配方、品质、成功率、废丹和爆炉。
- 炼器炉、多块结构、自定义炼器配方与失败回收。
- 灵石、仙玉、丹药、符箓、灵草、妖兽材料、矿物和批量目录物品。
- 法宝激活、本命绑定、成长、完整度、储物与捕获罐流程。

### 世界、宗门与多人内容

- 灵矿世界生成、阵法场、空间节点、传送阵和多种门类结构。
- 独立秘境/维度、区域旅行、门票、事件门禁、环境危险和分层奖励。
- 多宗门加入、晋升、贡献商店、任务、对话、据点和声望体系。
- 62 条文本任务链、主线桥接、FTB Quests 章节、阶段消耗和分支奖励。
- 共享拍卖 SavedData、离线退款、商路、势力冲突和宗门战计分。

## 默认按键

| 操作 | 默认按键 |
|---|---|
| 开始/结束打坐 | 使用冥想蒲团；无独立按键 |
| 打开任务追踪 | `J` |
| 打开修仙属性 | 未绑定，也可从背包中的“修仙”按钮进入 |
| 打开图鉴/编年 | 未绑定 |
| 技能编辑 | 未绑定 |
| 突破 | 未绑定 |
| 七个技能槽释放 | 未绑定 |

所有按键均可在 Minecraft 控制设置中调整。

## 命令入口

根命令为：

```text
/seeking_immortals
```

命令注册树以 [`SeekingImmortalsCommand.java`](src/main/java/com/xunxian/seekingimmortals/command/SeekingImmortalsCommand.java) 为唯一真相；完整参数、权限和副作用说明见 [`project_docs/command_reference.md`](project_docs/command_reference.md)。游戏聊天栏保留前导 `/`，服务器控制台执行时去掉 `/`。`<...>` 表示必填参数，`[...]` 表示可选参数，目录中的 `id` 必须使用内部标识。

普通查询和玩法入口：

```text
/seeking_immortals qi
/seeking_immortals realm
/seeking_immortals root
/seeking_immortals breakthrough
/seeking_immortals quest
/seeking_immortals sect
/seeking_immortals market
/seeking_immortals worldpack
/seeking_immortals catalog
/seeking_immortals artifact
/seeking_immortals phase
/seeking_immortals war
/seeking_immortals lore
/seeking_immortals live_smoke
```

常用查询还包括：

```text
/seeking_immortals lingli
/seeking_immortals qi
/seeking_immortals npc list [region]
/seeking_immortals npc info <id>
/seeking_immortals npc favor <id>
/seeking_immortals market list [shopId]
/seeking_immortals worldpack travel <region>
/seeking_immortals worldpack return
/seeking_immortals worldpack regions
/seeking_immortals worldpack realms
/seeking_immortals region [here|list]
/seeking_immortals region items <region>
/seeking_immortals region routes <from> <to>
/seeking_immortals catalog summary
/seeking_immortals catalog methods [list|studied]
/seeking_immortals catalog dimensions [list|get <id>|travel <route>]
/seeking_immortals catalog reputation [list|discount <shopId>|get <faction>]
/seeking_immortals catalog bulk <name>
/seeking_immortals catalog station inspect <id>
/seeking_immortals catalog talisman [list]
/seeking_immortals catalog puppet [list]
/seeking_immortals catalog beast [list]
/seeking_immortals artifact [p0|list|files|info <id>|recipe <id>|plan <id>]
/seeking_immortals lore [hub|compendium|bestiary|chronicle|summary]
/seeking_immortals lore glossary [query]
/seeking_immortals lore numeric
/seeking_immortals lore visual
/seeking_immortals lore lang
/seeking_immortals lore patchouli
```

没有权限 2 门槛不等于只读：`breakthrough`、任务分支选择、区域旅行/返回、日常事件领取、飞升尝试/确认/取消、工站成型/维修/改造、商路启程、灵兽喂养/召唤和侍从姿态操作都可能消耗材料、改变持久进度或写入世界状态。普通生存验证应在不使用管理员命令的专用存档中完成。

### 权限 2 调试和作弊

以下命令在源码中要求 `permission 2`，通常需要 OP。它们会直接修改玩家状态、世界状态或持久账本，不得用于证明正常生存流程可达。

修为、核心属性和异常状态：

```text
/seeking_immortals debug set_cultivation <amount>
/seeking_immortals debug set_core_attrs <divSense> <bodyRef> <qiDevRisk> <tribRes>
/seeking_immortals debug start_tribulation <target_realm>
/seeking_immortals debug add_contribution <amount>
/seeking_immortals debug fill_mana
/seeking_immortals debug unlock_skills

/seeking_immortals affliction severe_injury
/seeking_immortals affliction heart_demon
/seeking_immortals affliction realm_fall
/seeking_immortals affliction shattered_core
```

`set_cultivation` 的 `amount` 不得小于 0，`add_contribution` 必须为正整数。`target_realm` 使用 `MORTAL`、`QI_REFINING`、`FOUNDATION`、`CORE_FORMATION`、`NASCENT_SOUL`、`DEITY_TRANSFORMATION`、`VOID_REFINEMENT`、`BODY_INTEGRATION`、`GREAT_VEHICLE`、`TRIBULATION_LAND` 或 `TRUE_IMMORTAL` 等内部标识。

其他参数范围：`divSense` 与 `bodyRef >= 0`，`qiDevRisk` 为 `0..100`，`tribRes` 为 `0..90`，`limit` 为 `1..16`，`step` 为 `1..95`，`grade` 为 `1..3`，`delta` 为 `-1000..1000`；宗门战时长为 `1..120` 分钟，省略时默认为 10 分钟。权限 2 只控制命令入口，不会跳过服务端的目录、境界、区域、成本或状态校验。

管理员/测试变更：

```text
/seeking_immortals boss <id>
/seeking_immortals phase mark <id>
/seeking_immortals phase enter <id>
/seeking_immortals mission [gen]
/seeking_immortals war start <factionA> <factionB> [minutes]
/seeking_immortals war start <factionA> <factionB> <factionC> [minutes]
/seeking_immortals war stop

/seeking_immortals quest reset
/seeking_immortals quest advance
/seeking_immortals quest give_evidence
/seeking_immortals quest trigger_attack
/seeking_immortals quest spawn_mo_lao
/seeking_immortals quest spawn_steward
/seeking_immortals quest place_secret_room
/seeking_immortals quest place_yue_portal
/seeking_immortals quest text start <id>
/seeking_immortals quest text advance <id>
/seeking_immortals quest text branch <id> <choice>
/seeking_immortals quest text talk <id> [choice]
/seeking_immortals quest text gui <id>
/seeking_immortals quest text hooks accept <id>
/seeking_immortals quest text playable claim <id>
/seeking_immortals quest text playable start <id>
/seeking_immortals quest text playable prove <id> <step>
/seeking_immortals quest text spawn_npc <id>
/seeking_immortals quest text interact <npc>
/seeking_immortals quest text story start <id>
/seeking_immortals quest text story complete <id>

/seeking_immortals npc audit
/seeking_immortals npc talk <id> [tree]
/seeking_immortals npc act <choice>
/seeking_immortals npc spawn <id>
/seeking_immortals npc ensure_region <region> [limit]
/seeking_immortals market open [shopId]
/seeking_immortals market buy <entryOrShop> [entry]
/seeking_immortals market spawn_trader
/seeking_immortals market spawn_banker
/seeking_immortals worldpack enter <realm>
/seeking_immortals worldpack set_anchor <anchor>
/seeking_immortals worldpack daily_events enable
/seeking_immortals worldpack daily_events disable
/seeking_immortals worldpack daily_events roll

/seeking_immortals catalog manual <id>
/seeking_immortals catalog methods learn <id>
/seeking_immortals catalog reputation add <faction> <delta>
/seeking_immortals catalog spatial travel <id>
/seeking_immortals catalog ascension restore
/seeking_immortals catalog refine craft <id> [grade]
/seeking_immortals catalog formations deploy <id>
/seeking_immortals catalog station dismantle <id>
/seeking_immortals catalog talisman craft <id>
/seeking_immortals catalog puppet craft <id>
/seeking_immortals catalog chronicle discover <id>
/seeking_immortals catalog beast contract <id>
/seeking_immortals catalog auction open
/seeking_immortals catalog auction interest <id>
/seeking_immortals catalog auction bid <id>
/seeking_immortals catalog auction settle <id>

/seeking_immortals artifact refine <id>
/seeking_immortals artifact natal bind
/seeking_immortals artifact natal grow
/seeking_immortals artifact natal diagnose
/seeking_immortals sect open
/seeking_immortals sect join
/seeking_immortals sect advance
/seeking_immortals sect apply <sectId>
/seeking_immortals sect buy <entry>
/seeking_immortals sect donate spirit_grass
/seeking_immortals sect spawn_steward [sectId]
/seeking_immortals sect place_outpost [sectId]
```

### 实机烟测签字

`live_smoke` 在注册层没有权限 2 门槛；`run` 会生成/更新烟测清单，`sign` 会写入签字记录，只能在真实客户端或多人服流程完成后使用：

```text
/seeking_immortals live_smoke run
/seeking_immortals live_smoke sign [note]
/seeking_immortals live_smoke mp run
/seeking_immortals live_smoke mp sign [note]
```

调试命令后的结果不能作为 QA-01/QA-02 签字。命令源码和完整参数树见 [`SeekingImmortalsCommand.java`](src/main/java/com/xunxian/seekingimmortals/command/SeekingImmortalsCommand.java)。

## 代码结构

```text
src/main/java/com/xunxian/seekingimmortals/
  cultivation/   玩家修炼 Capability、境界、突破、灵根与契约
  skill/         技能定义、施法门禁与效果实现
  item/          丹药、手册、法宝载体、符箓和工具
  artifact/      法宝激活、捕获、炼器与本命绑定
  quest/         主线、文本任务、FTB 奖励桥接和 NPC 钩子
  sect/          宗门、贡献、定义和宗门战
  catalog/       文本材料目录、拍卖、商路、功法布局和召唤服务
  worldpack/     区域、秘境、SavedData、危险与世界玩法
  client/        Screen、HUD、渲染和客户端同步镜像
  network/       C2S/S2C 网络包与协议注册
  registry/      物品、方块、实体、菜单、配方和创造栏注册
```

资源目录：

- `src/main/resources/assets/seeking_immortals/`：语言、模型、贴图、声音和 GUI。
- `src/main/resources/data/seeking_immortals/`：配方、战利品、世界生成、维度、宗门、商店和文本材料。
- `src/main/resources/seeking_immortals/ftbquests/`：随模组发布的 FTB Quests 默认内容。

## 维护规则

开始修改前先阅读 [`MAINTENANCE.md`](MAINTENANCE.md)、[`AGENTS.md`](AGENTS.md)、[`project_docs/ai_handoff.md`](project_docs/ai_handoff.md) 和 [`project_docs/step_progress.md`](project_docs/step_progress.md)。

核心约束：

- 当前源码和资源优先于历史文档、`build/`、`run/`、备份和生成目录。
- 修改现有文件前，在 `.bak/<timestamp>/` 中按相对路径备份。
- 代码、资源、数据包、构建逻辑或运行行为变更时递增 `mod_version`。
- 网络包字段、顺序、类型、编码或兼容行为变化时同时递增协议版本。
- 所有 C2S 请求都视为不可信；消耗、境界、权限、槽位、冷却和目录 ID 必须由服务端校验。
- 新增可见内容时同步处理注册、创造栏、中英语言、模型、贴图、配方/掉落和文档。
- 代码或资源变更完成后必须运行完整 `build`。

## 0.2.269 当前更新摘要

- `single_core` 站点的 10 个核心方块已注册并接入主线结构证明，13 个单核心站点不再因缺少核心映射而永久卡死；`structure_blueprint_table` 仍保留为手持投影工具，不转为方块。
- 拍卖请柬已接入非管理员入口，并以玩家持久闩限制一次性引荐声望；拍卖场馆和区域门禁仍由服务端校验。
- 符箓与法宝供给链已补齐：已发布配方的无来源原料从 55 条降为 0 条。
- 新增五级造纸阶梯、三种符墨和符墨瓶配方，补充 9 家商店货源及 8 个首领掉落。
- 制符改为数据驱动：`TalismanCraftService` 读取 `talisman_recipes.json`，此前无入口的 19 种符箓变为可造，并按作者语料兑现产量、每配方专属符墨和境界门槛。
- 新增资源供给链与语料一致性回归测试；最近完整构建为 272 个测试套件、1,332 项测试全部通过。
- 当前剩余风险：3 条作者 stub 配方无材料因而对应成品仍无获取途径；制符成功率未按符纸品阶加成；造纸/制墨配方暂未执行 `talisman_table` 工站门槛；约 646 个数据驱动 bulk 物品、价格/掉率和真人 QA 仍待处理。

本节只描述已提交的 `0.2.269` 发布基线；未提交的工作不属于当前版本。

## 公开仓库脱敏规则

- 不提交 `.env`、`local.properties`、`secrets.properties`、日志、运行存档、备份目录、诊断压缩包、访问令牌、API 密钥、私钥或服务器凭据。
- `.gitignore` 已覆盖本地配置、`run/`、`build/`、`logs/`、`backups/` 和常见凭据文件；提交前仍须检查未跟踪文件和已暂存差异，不能只依赖忽略规则。
- README、日志、烟测报告和更新记录中的本地绝对路径、账号令牌、服务器地址、私人联系方式和原始命令输出应替换为泛化示例；路径只保留仓库相对路径。
- 发布前至少执行 `git status --short`、`git diff --check`，并对工作树和即将推送的提交做高信号密钥/私钥/令牌扫描。不要把 `build/`、`run/`、`.bak/`、`backups/` 或临时诊断文件加入提交。
- 本规则是内容级脱敏，不自动改写既有 Git 历史、提交作者元数据或远端已有对象。若需要历史级匿名化，必须先做独立备份、审查引用和远端影响，再单独批准历史重写；普通发布不得用强制推送替代这一步。

## 历史更新摘要（0.1.486 基线）

- 方法树布局升级至协议 17，并加入目录 ID、长度和数量边界。
- 手册、已学功法和功法层数在死亡克隆后继续保留。
- 修复首次万宝拍卖出价、任务起始前置校验和商路扣费原子性。
- 召唤物改用服务器全局登记，未加载区块内的实体仍计入上限并接收延迟命令。
- 宗门战战壳按实际击杀者阵营计分；补齐战场消息翻译。
- 修复无效手册授予、被拒施法副作用和方法树底部文字遮挡。
- 完整 Gradle 构建和自动化测试已通过。

更细的版本演进见 [`project_docs/updates/`](project_docs/updates/) 与 [`project_docs/ai_handoff.md`](project_docs/ai_handoff.md)。

## 已知边界

- 当前仍有占位或程序生成美术，不能视为最终发布品质。
- 文本任务具备服务端进度、消耗、分支和奖励权威，但不是完整叙事引擎。
- 部分秘境以可玩结构和规则为主，仍需要更丰富的手作地形、建筑和遭遇。
- 未加载区块不会被召唤系统强制加载；延迟命令会在实体区块重新载入后应用。
- 发布前仍需真人客户端和多人服烟测，重点覆盖死亡继承、跨维度召唤、同时竞拍和宗门战。

## 许可证

项目元数据声明为 **All Rights Reserved**。未经权利人明确许可，不得重新分发、再授权或用于商业发布。
