# 寻仙问道 / Seeking Immortals

`seeking_immortals` 是 Minecraft **Java 1.20.1** + **Forge 47.2.0** 的原创凡人流修仙模组。

| 项 | 值 |
|---|---|
| 当前版本 | `0.1.440`（`gradle.properties`） |
| 网络协议 | `ModNetwork.PROTOCOL_VERSION = "13"` |
| Java | 17 |
| 主题 | 灵力、境界、灵根、寿元、功法/术法、灵石、丹药、法宝、符箓、阵法、宗门、秘境、拍卖与修仙 UI |

> 文档以当前源码/资源为准。历史 `docs/phase-*.md`、旧 README 段落仅作追溯，不覆盖 `gradle.properties` 与 `project_docs/ai_handoff.md`。

---

## 快速开始

```powershell
# 构建（输出 build/libs/seeking_immortals-0.1.440.jar）
.\gradlew.bat --no-daemon --max-workers=1 build

# 开发客户端 / 服务端
.\gradlew.bat runClient
.\gradlew.bat runServer
```

常规约定：

- 改代码/资源后必须 `build` 通过；`build` 会先跑 `scripts/preflight.ps1` 做版本门禁。
- 改现有文件前备份到 `.bak/<timestamp>/`。
- 代码/资源/构建逻辑变更需 bump `mod_version`（`0.1.X`）。
- 改网络包字段/顺序/兼容性时同时 bump `ModNetwork.PROTOCOL_VERSION`。

---

## 依赖

| 依赖 | 状态 |
|---|---|
| Curios | 必需（法宝槽等） |
| GeckoLib | 必需（召唤物骨骼渲染） |
| Architectury / FTB Library / FTB Teams / FTB Quests | 已声明集成 |
| Patchouli | 可选（修仙指南书） |
| JEI | 可选（炼丹/炼器分类） |

可选依赖不得从 common/server 初始化路径无条件引用。客户端 UI 放在 `client` 包，经 `Dist.CLIENT` 隔离。

---

## 当前玩法概览

### 1. 修炼核心

- **Capability 玩家修仙数据**：灵力/灵力上限、修为/阶段上限、神识、肉身、走火风险、寿元/年龄。
- **境界**：炼气 → 筑基 → 金丹 → 元婴 → 化神 … 直至高阶（含设计映射与突破流程）。
- **灵根**：伪/杂/三系/双系/异/天等，影响修炼速度、灵力回复、突破与丹药吸收。
- **打坐吐纳**：静止打坐增长修为；受伤打断并抬高走火风险；受灵气浓度、功法、灵石等修正。
- **突破**：材料检查、成功率（灵根/丹药/执念等）、成功升境或失败回退/走火检定。
- **负面状态**：重伤、心魔、碎丹、跌境伤疤等。

### 2. 功法 / 术法 / 战斗

- **约 346 条文本功法/术法**已接线到释放路径（服务端权威）。
- **7 槽技能栏** + 技能编辑界面；客户端只发意图包，消耗/冷却/已学状态服务端校验。
- 自定义弹射物/区域术法为主（尽量不用原版火球/闪电作攻击主体）。
- **召唤物** `SummonedServitor`：真实实体 + 原型 AI（兽/傀/鬼/通用），GeckoLib 渲染。
- 战斗属性面板：攻防、暴击、闪避等由境界与修仙数据推导。

### 3. 炼丹 / 炼器 / 材料

- **炼丹炉**：MenuType 容器 GUI、配方、进度、成功率/废丹/爆炉、炼丹技能门禁。
- **品质丹药框架** + 凝气/筑基/稳神/回灵等核心丹。
- **炼器**：自定义 `seeking_immortals:refinement` RecipeType/Serializer；炼器炉/多块结构；命令规划与结算。
- 材料体系：灵草、妖兽材料、矿石、特殊材料；高冲突 source-id 已部分解压为独立 carrier。
- **JEI**：炼丹/炼器分类（JEI 存在时）。

### 4. 世界与空间

- 灵矿 worldgen、聚灵阵、鉴定石板、传送阵/宗门门/血禁门/飞升门等多块结构。
- **秘境独立维度包** + 自定义 biome/dimension_type（如迷雾洞、血禁、虚空殿、堕魔等）。
- 天元/凤原、阴冥/冥河、修罗/仙界等维度与旅行规则（门票、阵门、事件门等）。
- 灵气浓度：维度/群系/灵脉哈希/附近阵法共同计算。

### 5. 任务 / 宗门 / 经济

- **七玄门五阶段 MVP**：命名 NPC、阶段旗标、分支、密室/越国门、奖励。
- **文本任务 62 链**：阶段追踪、材料消耗、分支、对话 GUI、钩子接取、命名村民权威入口。
- **宗门**：加入/贡献商店/rank 锁/日任务生成/执事实体/据点骨架。
- **商店网络** + 贡献/货币购买；拍卖 GUI + 共享竞价 SavedData。
- 声望、势力冲突索引、宗门战计分。

### 6. 其它系统

- 神秘小瓶（绑定、24h 充能、植物加速）。
- 灵石吸收、储物手镯 MenuType GUI、本命绑定、捕获罐。
- 灵舟载具、灵兽契约服务、阵法场持久/重水合。
- 修仙 HUD、打坐 HUD、任务追踪 Screen、对话立绘/语音资源。
- Patchouli 指南书；`live_smoke` 自动探测与签字报告。

---

## 默认按键 / UI

| 操作 | 默认 |
|---|---|
| 打坐 | `V` |
| 打坐详情屏 | `B` |
| 任务追踪 | `J` |
| 修仙面板 | 原版背包中的「修仙」按钮 / 相关按键 |
| 技能编辑 / 7 槽释放 | 默认可重绑（多为未绑定） |

创造模式标签页：**寻仙问道**。

---

## 命令一览

根命令：

```text
/seeking_immortals
```

`OP` = 权限等级 ≥ 2。

### 基础

```text
/seeking_immortals lingli|qi          # 灵力/修为
/seeking_immortals realm              # 境界
/seeking_immortals root               # 灵根
/seeking_immortals breakthrough       # 突破
```

### 法宝

```text
/seeking_immortals artifact
/seeking_immortals artifact p0|list|files
/seeking_immortals artifact info <id>
/seeking_immortals artifact recipe <id>
/seeking_immortals artifact refine <id>
/seeking_immortals artifact plan <id>
/seeking_immortals artifact natal
/seeking_immortals artifact natal bind|grow
```

### 任务

```text
# 七玄门
/seeking_immortals quest
/seeking_immortals quest start|check
/seeking_immortals quest choose report|silent|blackmail
/seeking_immortals quest reset|advance              # OP
/seeking_immortals quest spawn_mo_lao|spawn_steward # OP
/seeking_immortals quest place_secret_room|place_yue_portal|give_evidence|trigger_attack  # OP

# 文本任务 62 链
/seeking_immortals quest text [list]
/seeking_immortals quest text start|advance|status|cost <id>
/seeking_immortals quest text branch <id> <choice>
/seeking_immortals quest text talk <id> [choice]
/seeking_immortals quest text gui <id>
/seeking_immortals quest text hooks
/seeking_immortals quest text hooks <id>
/seeking_immortals quest text hooks accept <id>
/seeking_immortals quest text spawn_npc <id>        # OP
/seeking_immortals quest text interact <npc>
/seeking_immortals quest text story [list]
/seeking_immortals quest text story complete <id>
```

### 市场 / 世界包 / 宗门

```text
/seeking_immortals market [open [shopId]|list [shopId]]
/seeking_immortals market buy <entry>
/seeking_immortals market buy <shopId> <entry>
/seeking_immortals market spawn_trader              # OP

/seeking_immortals worldpack [open]
/seeking_immortals worldpack travel <region>
/seeking_immortals worldpack enter <realm>
/seeking_immortals worldpack return
/seeking_immortals worldpack set_anchor <anchor>    # OP
/seeking_immortals worldpack regions|realms|events

/seeking_immortals sect [status|open|join|candidates|advance|shop]
/seeking_immortals sect apply <sectId>
/seeking_immortals sect buy <entry>
/seeking_immortals sect donate spirit_grass
/seeking_immortals sect spawn_steward [sectId]      # OP
/seeking_immortals sect place_outpost [sectId]      # OP
```

### 目录 / 拍卖 / 灵兽 / 声望等

```text
/seeking_immortals catalog [summary]
/seeking_immortals catalog manual|flight|summon <id>
/seeking_immortals catalog methods|realms|quests|sects|bands|chapters|manifest|lore|factions
/seeking_immortals catalog auction [list|open]
/seeking_immortals catalog auction interest|bid|settle <id>
/seeking_immortals catalog auction <id>
/seeking_immortals catalog spatial [travel] <id>
/seeking_immortals catalog reputation [list]
/seeking_immortals catalog reputation discount <shopId>
/seeking_immortals catalog reputation get <faction>
/seeking_immortals catalog reputation add <faction> <delta>   # OP
/seeking_immortals catalog conflicts|bulk|refine|formations|chronicle|trade [id]
/seeking_immortals catalog beast [list]
/seeking_immortals catalog beast contract|feed|summon <id>
/seeking_immortals catalog has <id>
```

### 其它系统

```text
/seeking_immortals boss <id>
/seeking_immortals phase [mark <id>]
/seeking_immortals mission [gen]
/seeking_immortals war [status]
/seeking_immortals war start <factionA> <factionB> [minutes]  # OP
/seeking_immortals war stop                                   # OP
/seeking_immortals live_smoke [run]
/seeking_immortals live_smoke sign [note]
```

### OP 调试

```text
/seeking_immortals affliction severe_injury|heart_demon|realm_fall|shattered_core
/seeking_immortals debug set_cultivation <amount>
/seeking_immortals debug set_core_attrs <divSense> <bodyRef> <qiDevRisk> <tribRes>
/seeking_immortals debug start_tribulation <target_realm>
/seeking_immortals debug add_contribution <amount>
/seeking_immortals debug fill_mana
/seeking_immortals debug unlock_skills
```

### 常用示例

```text
/seeking_immortals qi
/seeking_immortals breakthrough
/seeking_immortals quest text start huangfeng_cultivation_path
/seeking_immortals quest text gui huangfeng_cultivation_path
/seeking_immortals market open
/seeking_immortals worldpack enter mist_cave_trial
/seeking_immortals sect open
/seeking_immortals catalog auction open
/seeking_immortals live_smoke
```

---

## 架构速览

```text
src/main/java/com/xunxian/seekingimmortals/
  SeekingImmortalsMod.java     # 入口注册
  cultivation/                 # 修炼数据、境界、突破、飞行权限
  skill/ + technique data      # 技能效果与资源功法
  alchemy/ + recipe/           # 炼丹/炼器
  artifact/                    # 法宝激活、炼器规划、储物
  quest/ + sect/ + shop/       # 任务、宗门、商店
  worldpack/ + structure/      # 区域秘境、阵法、多块
  client/                      # HUD/Screen/渲染
  network/                     # 协议包
  registry/                    # 物品方块实体菜单配方音效
```

资源：

- 资产：`src/main/resources/assets/seeking_immortals/`
- 数据：`src/main/resources/data/seeking_immortals/`（recipes、worldgen、dimension、text_material、artifacts、shops…）

---

## 文档入口

真相优先级：

1. `gradle.properties`（版本）
2. 当前源码 / 资源
3. `project_docs/ai_handoff.md`
4. `project_docs/step_progress.md`
5. `project_docs/unimplemented_checklist.md`

其它常用文档：

- `project_docs/features.md` — 功能记录
- `project_docs/items.md` — 物品
- `project_docs/pending_requests.md` — 长期待办
- `project_docs/missing_and_placeholders.md` — 占位与缺口
- `docs/task-board.md` / `docs/mvp-scope.md` — 早期路线与范围
- `CLAUDE.md` / `AGENTS.md` — AI/协作约定

---

## 已知局限（诚实说明）

当前是**可构建、可游玩的深 MVP / 内容骨架**，不是最终凡人流全作：

- 文本 62 链为轻量权威（阶段/消耗/分支/钩子），不是完整叙事引擎。
- 大量贴图/立绘为程序生成或占位，非最终商业美术。
- 召唤/灵兽/傀儡有实体与契约 MVP，深度养成仍可扩展。
- 秘境有维度与 biome 壳，大规模手作地形/结构仍可加深。
- FTB 任务多为展示/桥接，并非全量双向权威同步。
- 发布前建议再做一轮真实客户端回归；历史 network 包 diff 若涉及兼容需审计协议。

最近修复示例：`0.1.440` 修复创建世界时 `add_yin_essence_ore` biome_modifier 非法 biomes 数组导致的 registry 崩溃。

---

## 工作约定

- 以当前源码和资源为准，不以 `build/`、`.gradle/`、`run/`、`.bak/` 为实现真相。
- 新增可见物品/方块时同步：注册、创造栏、中英语言、模型、贴图、配方/掉落/文档。
- 网络包假设客户端不可信：消耗、冷却、境界、槽位、已学状态均服务端校验。
- UI 使用原生 Forge/Minecraft Screen、Menu、Overlay；不重新引入旧第三方 UI 框架。
- 一次任务尽量小范围、可回滚；先备份再改，改完再 build。
