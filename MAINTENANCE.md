# 寻仙问道维护指南

这份文件用于让下一位维护者快速恢复上下文。它不替代源码、`AGENTS.md` 或项目状态文档；当内容冲突时，按下方“真相优先级”判断。

## 当前快照

| 项目 | 当前值 |
|---|---|
| 日期 | 2026-07-14 |
| 模组版本 | `0.1.486` |
| 网络协议 | `17` |
| Minecraft / Forge | `1.20.1` / `47.2.0` |
| Java | 17 |
| 最近验证 | `gradlew build` 成功，产物 `build/libs/seeking_immortals-0.1.486.jar` |
| 最近阶段 | Wave486 review hardening |

不要手工维护这张表而忘记真实版本。每次接手先读取 `gradle.properties` 和 `ModNetwork.PROTOCOL_VERSION`。

## 接手后的前十分钟

1. 运行 `git status --short`，确认工作区是否有他人未提交改动。
2. 阅读 `AGENTS.md`，遵守备份、版本、文档和构建门禁。
3. 阅读 `project_docs/ai_handoff.md` 顶部的 CURRENT TRUTH。
4. 阅读 `project_docs/step_progress.md` 最新阶段。
5. 按任务选择阅读 `items.md`、`pending_requests.md`、`features.md` 和 `missing_and_placeholders.md`。
6. 从当前源码和 `src/main/resources` 验证假设，不从 `build/`、`run/`、`.bak/` 或历史输出反推实现。
7. 修改前列出文件并建立 `.bak/<timestamp>/` 备份。
8. 保留工作区内不属于本任务的改动，不要重置或覆盖。
9. 完成后运行与风险相称的定向测试，再运行完整 `build`。
10. 更新项目文档和 `project_docs/updates/`，最后再提交 Git。

## 真相优先级

1. `gradle.properties`、当前 Java 源码和当前资源。
2. `src/main/java/com/xunxian/seekingimmortals/network/ModNetwork.java` 中的协议。
3. `project_docs/ai_handoff.md` 顶部。
4. `project_docs/step_progress.md` 顶部。
5. 其它项目文档和历史更新记录。
6. 构建产物、备份、运行目录和生成输出仅用于验证或追溯。

`AGENTS.md` 中可能存在静态版本示例；版本号始终以 `gradle.properties` 为准。

## 固定工作流

### 1. 变更分类

| 变更 | `mod_version` | 网络协议 |
|---|---:|---:|
| 仅 Markdown、注释、忽略规则 | 不变 | 不变 |
| Java、资源、数据包、配置、构建逻辑、运行行为 | patch +1 | 通常不变 |
| 网络字段/顺序/类型/编码/兼容行为 | patch +1 | +1 |

添加新包但不改变既有字段，也需要审查注册顺序和旧客户端兼容性。预检的“network package changed”是提示，不等同于必须提升协议；最终判断要写入更新记录。

### 2. 备份

备份目录使用 `.bak/<timestamp>/`，保留相对路径。只备份将被修改且已经存在的文件；新文件无需备份。

### 3. 实现

- 选择现有服务、目录和注册模式，不创建平行体系。
- 保持 gameplay authority 在服务端。
- 客户端只发送意图，服务端重新解析目录、成本、权限和玩家状态。
- 不从 common/server 初始化路径加载 `client` 类。
- 玩家可见文本使用 `Component.translatable`，中英文语言键同时补齐。

### 4. 验证

```powershell
# 定向测试示例
.\gradlew.bat --no-daemon --max-workers=1 test --tests com.xunxian.seekingimmortals.resources.ResourceJsonParseTest

# 最终门禁
.\gradlew.bat --no-daemon --max-workers=1 build
```

完整构建必须成功或在文档中记录精确阻塞原因。不要用 `-PaiSkipVersionBumpCheck=true` 绕过正常版本门禁。

### 5. 文档与 Git

- 更新 `ai_handoff.md` 和 `step_progress.md` 的顶部最新状态。
- 按影响更新 `items.md`、`features.md`、`pending_requests.md`、`missing_and_placeholders.md`。
- 在 `project_docs/updates/` 写版本记录，包含备份、版本、协议决定、测试和风险。
- 提交主题写结果，正文写主要更新和验证，不使用只有“update”或“fix”的空描述。
- 不提交 `.bak/`、`build/`、`run/`、`output/` 或临时补丁脚本。

## 关键模块

| 领域 | 主要入口 | 维护重点 |
|---|---|---|
| 模组入口 | `SeekingImmortalsMod` | 注册只在正确总线和物理侧执行 |
| 玩家修炼 | `cultivation/PlayerCultivation`、`CultivationProvider` | 序列化、克隆、同步、数值上限 |
| 术法释放 | `network/ReleaseTechniquePacket`、`TechniqueGateService` | 先门禁，再副作用/消耗/冷却 |
| 功法手册 | `ManualCatalogService`、`TechniqueManualItem` | 消耗型进度必须跨死亡保留 |
| 文本任务 | `TextQuestChainService`、`QuestTrackerActionPacket` | C2S 不可信，起始/阶段成本服务端校验 |
| 宗门 | `SectDefinitionService`、`SectContributionService` | 宗门别名、权限、贡献与商店一致性 |
| 拍卖 | `AuctionSoftService`、`AuctionHouseSavedData` | 共享竞价、托管、退款、首次入口 |
| 商路 | `ChronicleTradeSoftService` | 启程费与任务阶段费用联合预检 |
| 召唤物 | `SummonHonestMvpService`、`SummonedServitorEntity`、`ServitorRegistrySavedData` | 全局上限、卸载区块、延迟命令 |
| 世界包 | `WorldpackGameplayService`、`WorldpackSavedData` | 地区、维度、门票、事件和持久化 |
| 客户端 UI | `client/` | 小屏布局、按钮遮挡、仅客户端加载 |
| 网络 | `network/ModNetwork` | 注册顺序、encode/decode/handle 同步修改 |

## 状态所有权

理解状态放在哪里，比找到调用点更重要：

- 修炼核心、境界、术法槽、冷却和任务主状态：玩家 Cultivation Capability。
- 手册、已学功法和功法层数：玩家 persistent data，并在 `PlayerEvent.Clone` 中显式复制。
- 文本任务轻量状态、分支和奖励账本：玩家 persistent data。
- 功法树布局：玩家 persistent data，服务端清洗目录 ID 和包上限后同步。
- 拍卖最高价和离线退款：服务器 Overworld SavedData。
- 召唤物所有权、延迟姿态和解散状态：服务器 `ServitorRegistrySavedData`。
- 世界事件、区域和部分结构状态：对应 worldpack/formation SavedData。

Forge 死亡克隆不会自动保留所有顶层 persistent data。任何消耗型永久进度都必须明确验证死亡继承路径。

## 网络安全检查表

处理 C2S 包时至少检查：

- 字符串长度、集合数量、索引边界和枚举值。
- ID 是否存在于服务器目录或注册表。
- 玩家是否真的学会、拥有、解锁或位于正确区域。
- 境界、宗门、声望、冷却和成本是否由服务器计算。
- 所有检查通过前，不扣物品、不写 NBT、不加风险、不触发奖励。
- 操作成功后只同步最终服务端状态。

若修改包字段、顺序、类型或 encode/decode 格式，必须同步修改双方并提升协议版本。

## 新增可见内容检查表

新增物品、方块或实体时检查：

- `registry/` 注册。
- 创造模式标签页。
- `zh_cn.json` 与 `en_us.json`。
- 模型、blockstate、贴图或 GeckoLib 资源。
- 配方、战利品、标签、世界生成或数据目录。
- 服务端行为与客户端渲染的物理侧隔离。
- 资源 JSON 解析测试和完整构建。

不要删除标记为占位的资源，除非替换内容和引用已经一起完成。

## 测试选择

| 修改领域 | 优先测试 |
|---|---|
| JSON、语言、数据包 | `ResourceJsonParseTest`、相关 catalog 测试 |
| FTB Quests | `FtbQuestSnbtTest` |
| 世界/维度/传送阵 | `Worldpack*Test`、`PortalArrayStructureTest` |
| 任务/商路/拍卖 | 对应 service 测试 |
| UI 布局 | `ScreenLayoutTest`，并做实际客户端烟测 |
| 玩家持久化 | NBT round-trip、clone 测试和死亡烟测 |
| 网络 | 编解码边界、恶意输入和登录同步 |

测试通过不等于客户端视觉正确。Screen、HUD、粒子、实体渲染和多人竞态需要真人烟测。

## 0.1.454-0.1.486 维护摘要

- 任务系统从轻量阶段跟踪推进到材料成本、分支锁、奖励账本、NPC 门禁和 FTB 桥接。
- 召唤系统加入真实实体、姿态、修理、契约、秘境战壳和全局 SavedData 上限。
- 法宝与炼器加入品阶门禁、失败回收、本命成长和完整度行为。
- 宗门扩展到多套可玩定义、专属任务、商店、对话、据点与别名收敛。
- 功法系统加入目录学习、手册解锁、层数培养、客户端同步和可持久化节点树。
- 商路、编年史、势力冲突、拍卖和宗门战获得更完整的服务端权威路径。
- Wave486 集中修复死亡进度、首次拍卖、输入边界、任务前置、交易原子性、阵营计分和 UI/翻译回归。

详细逐波记录位于 `project_docs/updates/`。

## 当前剩余风险

- 高保真美术、完整自定义秘境地形和工作室级 GeckoLib 模型仍需人工制作。
- FTB Quests 仍有展示/桥接性质内容，尚非所有节点都与模组状态双向绑定。
- 未加载区块不会被召唤系统强制加载；延迟命令依赖区块后续载入。
- PvP、自定义伤害、护甲/吸收交互仍应在扩展战斗前专项复核。
- 发布前需要真人验证死亡继承、跨维度召唤、同时拍卖、宗门战、GUI 缩放和可选/必需依赖组合。

## 下次交接模板

完成一轮维护后，在更新记录中回答：

```text
目标：
变更分类：
修改文件：
备份路径：
mod_version：
协议决定：
定向测试：
完整 build：
构建产物：
剩余风险：
下一步：
```

保持交接短而准确。详细实现应留在源码、测试和版本更新记录中，不要把整段代码复制进交接文档。
