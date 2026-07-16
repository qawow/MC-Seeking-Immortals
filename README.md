# 寻仙问道 / Seeking Immortals

`seeking_immortals` 是面向 Minecraft Java Edition 1.20.1 的 Forge 修仙模组，以原创凡人流成长为核心，覆盖修炼、功法、炼丹炼器、宗门、任务、拍卖、秘境、阵法和多人服务端玩法。

当前项目是可构建、可游玩的深度 MVP。核心系统已经形成服务端权威闭环，但美术、叙事表现和部分大型世界内容仍在持续完善。

## 项目状态

| 项目 | 当前值 |
|---|---|
| 模组版本 | `0.1.486` |
| 网络协议 | `17` |
| Mod ID | `seeking_immortals` |
| Minecraft | `1.20.1` |
| Forge | `47.2.0` |
| Java | `17` |
| 构建系统 | ForgeGradle 6.x / Gradle Wrapper |
| 许可证 | All Rights Reserved |

版本和协议的最终真相分别是 [`gradle.properties`](gradle.properties) 与 [`ModNetwork.java`](src/main/java/com/xunxian/seekingimmortals/network/ModNetwork.java)。不要从旧构建产物或历史文档推断当前版本。

## 安装

客户端和服务端必须安装相同版本的模组与依赖。当前 `mods.toml` 将以下依赖声明为必需：

| 依赖 | 最低/开发版本 |
|---|---|
| Forge | `47.2.0` |
| Curios | `5.9.1+1.20.1` |
| Patchouli | `1.20.1-84-FORGE` |
| JEI | `15.20.0.106` |
| GeckoLib | `4.8.4` |
| Architectury API | `9.1.12` |
| FTB Library | `2001.2.9` |
| FTB Teams | `2001.3.0` |
| FTB Quests | `2001.4.22` |

安装步骤：

1. 安装 Minecraft 1.20.1 和 Forge 47.2.0。
2. 将本模组及上述依赖放入实例或服务器的 `mods/` 目录。
3. 确保客户端、服务端使用同一模组版本和协议。
4. 首次进入世界前备份存档；开发版本不承诺向后兼容所有实验数据。

当前构建产物位于 `build/libs/seeking_immortals-0.1.486.jar`。

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
| 切换打坐 | `V` |
| 打开打坐界面 | `B` |
| 打开任务追踪 | `J` |
| 打开修仙属性 | 未绑定，也可从背包中的“修仙”按钮进入 |
| 技能编辑 | 未绑定 |
| 突破 | 未绑定 |
| 七个技能槽释放 | 未绑定 |

所有按键均可在 Minecraft 控制设置中调整。

## 命令入口

根命令为：

```text
/seeking_immortals
```

常用入口：

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
/seeking_immortals war
/seeking_immortals live_smoke
```

管理和调试命令需要 OP 权限。命令树变化较快，当前实现以 [`SeekingImmortalsCommand.java`](src/main/java/com/xunxian/seekingimmortals/command/SeekingImmortalsCommand.java) 为准。

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

## 0.1.486 更新摘要

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
