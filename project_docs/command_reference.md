# `/seeking_immortals` 命令参考

本文件按 `src/main/java/com/xunxian/seekingimmortals/command/SeekingImmortalsCommand.java` 的注册树整理，当前对应 `mod_version=0.2.268`、`ModNetwork.PROTOCOL_VERSION=31`。源码是唯一真相；新增或删除命令时必须同步本文件。

## 使用约定

- 游戏内聊天栏使用前导 `/`；服务器控制台执行时去掉 `/`。大多数命令通过 `getPlayerOrException()` 作用于执行者，不能把控制台当作目标玩家。
- 源码显式 `.requires(source -> source.hasPermission(2))` 的命令标为“权限 2”。它们是管理员、调试或测试入口，不是普通生存流程。
- `<...>` 表示必填参数，`[...]` 表示可选参数；`id`、`region`、`realm`、`faction` 等值必须使用源码/数据目录中的内部标识。
- 命令即使能执行，也不代表对应内容已通过正常获取路径。权限 2 修改可能写入玩家 NBT、任务/声望账本、世界 SavedData、实体或工站状态；请在专用测试世界使用，并预留回滚存档。

## 普通查询与玩法入口

这些入口在命令注册层没有权限 2 门槛，但仍会经过服务端规则；其中带有“尝试、旅行、交付、制作”等词的命令可能消耗资源或写入进度。

```text
/seeking_immortals lingli
/seeking_immortals qi                         # lingli 别名
/seeking_immortals realm
/seeking_immortals root
/seeking_immortals breakthrough
/seeking_immortals quest
/seeking_immortals quest start
/seeking_immortals quest check
/seeking_immortals quest choose report|silent|blackmail
/seeking_immortals npc
/seeking_immortals npc list [region]
/seeking_immortals npc info <id>
/seeking_immortals npc favor <id>
/seeking_immortals market
/seeking_immortals market list [shopId]
/seeking_immortals worldpack [open]
/seeking_immortals worldpack travel <region>
/seeking_immortals worldpack return
/seeking_immortals worldpack regions
/seeking_immortals worldpack realms
/seeking_immortals worldpack events
/seeking_immortals worldpack daily_events [status|claim]
/seeking_immortals region [here|list]
/seeking_immortals region items <region>
/seeking_immortals region routes <from> <to>
/seeking_immortals phase
/seeking_immortals war [status]
/seeking_immortals sect [status|candidates|shop]
/seeking_immortals lore [hub|compendium|bestiary|chronicle|summary]
/seeking_immortals lore glossary [query]
/seeking_immortals lore numeric
/seeking_immortals lore visual
/seeking_immortals lore lang
/seeking_immortals lore patchouli
```

常用目录查询/预览：

```text
/seeking_immortals catalog [summary]
/seeking_immortals catalog flight <id>
/seeking_immortals catalog methods [list|studied]
/seeking_immortals catalog realms|quests|sects|bands|chapters|manifest|lore|factions
/seeking_immortals catalog auction [list|<id>]
/seeking_immortals catalog spatial [<id>]
/seeking_immortals catalog dimensions [list|get <id>|travel <route>]
/seeking_immortals catalog ascension [status|attempt|confirm|cancel]
/seeking_immortals catalog reputation [list|discount <shopId>|get <faction>]
/seeking_immortals catalog conflicts [<id>|accept <id>|side <id> <side>]
/seeking_immortals catalog bulk [<name>]
/seeking_immortals catalog refine [<id>]
/seeking_immortals catalog formations [<id>]
/seeking_immortals catalog station inspect <id>
/seeking_immortals catalog talisman [list]
/seeking_immortals catalog puppet [list]
/seeking_immortals catalog chronicle [<id>]
/seeking_immortals catalog trade [<id>]
/seeking_immortals catalog summon [list]
/seeking_immortals catalog beast [list]
/seeking_immortals catalog has <id>
/seeking_immortals artifact [p0|list|files|info <id>|recipe <id>|plan <id>]
/seeking_immortals artifact natal
```

## 权限 2：作弊与调试

以下命令在注册树上统一要求权限 2。参数范围是源码注册的范围；服务端仍可能因境界、目标数据或当前状态拒绝执行。

### 修为、核心属性与异常状态

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

参数约束：`amount >= 0`；`qiDevRisk` 为 `0..100`；`tribRes` 为 `0..90`；`add_contribution` 为正整数；`divSense` 与 `bodyRef` 不得小于 0。`start_tribulation` 的 `target_realm` 使用境界内部标识，不要填写中文显示名；源码接受的设计 id 包括 `MORTAL`、`QI_REFINING`、`FOUNDATION`、`CORE_FORMATION`、`NASCENT_SOUL`、`DEITY_TRANSFORMATION`、`VOID_REFINEMENT`、`BODY_INTEGRATION`、`GREAT_VEHICLE`、`TRIBULATION_LAND`、`TRUE_IMMORTAL`。

### 任务、NPC、商店与世界状态

```text
/seeking_immortals quest reset
/seeking_immortals quest advance
/seeking_immortals quest spawn_mo_lao
/seeking_immortals quest spawn_steward
/seeking_immortals quest place_secret_room
/seeking_immortals quest place_yue_portal
/seeking_immortals quest give_evidence
/seeking_immortals quest trigger_attack

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
```

`quest text list/status/cost/hooks`、`quest text playable list/status` 和 `quest text story/list` 是查询入口；不要把查询结果当作已经完成证明。`npc audit` 只读审计，不会删除实体。

### 目录、法宝、Boss、战争与宗门

```text
/seeking_immortals artifact refine <id>
/seeking_immortals artifact natal bind
/seeking_immortals artifact natal grow
/seeking_immortals artifact natal diagnose

/seeking_immortals catalog manual <id>
/seeking_immortals catalog methods learn <id>
/seeking_immortals catalog auction open
/seeking_immortals catalog auction interest <id>
/seeking_immortals catalog auction bid <id>
/seeking_immortals catalog auction settle <id>
/seeking_immortals catalog spatial travel <id>
/seeking_immortals catalog ascension restore
/seeking_immortals catalog reputation add <faction> <delta>
/seeking_immortals catalog refine craft <id> [grade]
/seeking_immortals catalog formations deploy <id>
/seeking_immortals catalog station dismantle <id>
/seeking_immortals catalog talisman craft <id>
/seeking_immortals catalog puppet craft <id>
/seeking_immortals catalog chronicle discover <id>
/seeking_immortals catalog beast contract <id>

/seeking_immortals boss <id>
/seeking_immortals phase mark <id>
/seeking_immortals phase enter <id>
/seeking_immortals mission [gen]
/seeking_immortals war start <factionA> <factionB> [minutes]
/seeking_immortals war start <factionA> <factionB> <factionC> [minutes]
/seeking_immortals war stop

/seeking_immortals sect open
/seeking_immortals sect join
/seeking_immortals sect apply <sectId>
/seeking_immortals sect advance
/seeking_immortals sect buy <entry>
/seeking_immortals sect donate spirit_grass
/seeking_immortals sect spawn_steward [sectId]
/seeking_immortals sect place_outpost [sectId]
```

`war start` 未填写时长默认为 10 分钟；源码允许 `1..120` 分钟。`limit` 的范围是 `1..16`，`step` 是 `1..95`，`grade` 是 `1..3`，声望 `delta` 是 `-1000..1000`。`catalog station form/repair/overhaul`、`catalog puppet repair`、`catalog summon`、`catalog beast feed/summon` 和 `catalog trade embark` 没有命令层权限 2 门槛，但它们可能是真实玩法变更或消耗材料，不能因此当作只读命令。

## 实机烟测与签字

`live_smoke` 注册树本身没有权限 2 门槛，但签字会写入烟测报告/持久记录。只在完成真实客户端或多人流程后签字：

```text
/seeking_immortals live_smoke
/seeking_immortals live_smoke run
/seeking_immortals live_smoke sign [note]

/seeking_immortals live_smoke mp
/seeking_immortals live_smoke mp run
/seeking_immortals live_smoke mp sign [note]
```

- 无参数或 `run`：打印单机清单；`sign`：记录单机人工签字。
- `mp`/`mp run`：打印多人清单；`mp sign`：记录多人人工签字。
- `sign` 不会替代启动客户端、专服、第二客户端、重连、死亡/克隆或资源包检查；使用调试命令后得到的结果不得写入正式 QA 签字。
- QA 报告应同时记录 JAR SHA-256、`mod_version`、`ModNetwork.PROTOCOL_VERSION`、运行环境、日志路径、失败步骤和重试结果。当前批次的代码构建基线为 0.2.268，SHA-256 为 `7e5b87d251bdf621de8cb2130f2c989b44d3cd5a5853a56f19fdd026629799d8`。

## 维护规则

命令源码有新增、删除、参数改名或权限变化时，先以注册树和服务端处理函数核对，再同步本文件、`ai_handoff.md`、`step_progress.md` 与对应更新日志。文档批次不改变 `mod_version`；只有代码、资源、数据或构建逻辑变更才按 `AGENTS.md` 升补丁版本，网络包字段/顺序/类型或频道兼容行为变化才升协议。
