# 任务简报总索引（文本材料 v147 → 模组落地拆解）

生成日期：2026-07-15。基线：`main` 分支，mod_version `0.1.504`，`ModNetwork.PROTOCOL_VERSION = 19`，语料批次 v147（schema v55）已入库。

本目录将 `文本材料/`（设定语料全集）拆成 **17 份互不重叠的任务简报（M00–M16）**，每份对应一个 git worktree。拆分原则：

1. **文件所有权唯一**：每个语料文件、每个 Java 包/类只归属一个模块；其他模块只能通过声明的接口消费。
2. **接口显式声明**：跨模块交叉一律写成"谁提供 → 谁消费"，禁止直接读别人拥有的文件新增字段。
3. **红线内嵌**：DESIGNER_HANDBOOK §5 的设计红线写进相关简报的验收标准。

## 模块总表

| 模块 | 简报 | 分支 | worktree 目录 |
| --- | --- | --- | --- |
| M00 数据管线与校验 | [M00_数据管线.md](M00_数据管线.md) | `task/m00-data-pipeline` | `D:/codex/mc-mod-worktrees/m00-data-pipeline` |
| M01 境界与修炼基础 | [M01_境界修炼.md](M01_境界修炼.md) | `task/m01-realm-progression` | `D:/codex/mc-mod-worktrees/m01-realm-progression` |
| M02 功法与术法 | [M02_功法术法.md](M02_功法术法.md) | `task/m02-techniques` | `D:/codex/mc-mod-worktrees/m02-techniques` |
| M03 物品目录与批量注册 | [M03_物品目录.md](M03_物品目录.md) | `task/m03-item-catalog` | `D:/codex/mc-mod-worktrees/m03-item-catalog` |
| M04 炼制与生产 | [M04_炼制生产.md](M04_炼制生产.md) | `task/m04-craft-production` | `D:/codex/mc-mod-worktrees/m04-craft-production` |
| M05 经济与交易 | [M05_经济交易.md](M05_经济交易.md) | `task/m05-economy-trade` | `D:/codex/mc-mod-worktrees/m05-economy-trade` |
| M06 区域与世界事件 | [M06_区域事件.md](M06_区域事件.md) | `task/m06-regions-events` | `D:/codex/mc-mod-worktrees/m06-regions-events` |
| M07 阵法与多方块 | [M07_阵法多方块.md](M07_阵法多方块.md) | `task/m07-formations-multiblock` | `D:/codex/mc-mod-worktrees/m07-formations-multiblock` |
| M08 宗门与势力 | [M08_宗门势力.md](M08_宗门势力.md) | `task/m08-sects-factions` | `D:/codex/mc-mod-worktrees/m08-sects-factions` |
| M09 秘境与副本 | [M09_秘境副本.md](M09_秘境副本.md) | `task/m09-secret-realms` | `D:/codex/mc-mod-worktrees/m09-secret-realms` |
| M10 妖兽与生态 | [M10_妖兽生态.md](M10_妖兽生态.md) | `task/m10-beasts-ecology` | `D:/codex/mc-mod-worktrees/m10-beasts-ecology` |
| M11 任务与主线 | [M11_任务主线.md](M11_任务主线.md) | `task/m11-quests-story` | `D:/codex/mc-mod-worktrees/m11-quests-story` |
| M12 NPC 与对话 | [M12_NPC对话.md](M12_NPC对话.md) | `task/m12-npc-dialogue` | `D:/codex/mc-mod-worktrees/m12-npc-dialogue` |
| M13 维度与飞升 | [M13_维度飞升.md](M13_维度飞升.md) | `task/m13-dimensions-ascension` | `D:/codex/mc-mod-worktrees/m13-dimensions-ascension` |
| M14 战斗与状态 | [M14_战斗状态.md](M14_战斗状态.md) | `task/m14-combat-status` | `D:/codex/mc-mod-worktrees/m14-combat-status` |
| M15 法宝与器灵 | [M15_法宝器灵.md](M15_法宝器灵.md) | `task/m15-artifacts` | `D:/codex/mc-mod-worktrees/m15-artifacts` |
| M16 图鉴与百科 | [M16_图鉴百科.md](M16_图鉴百科.md) | `task/m16-lore-books` | `D:/codex/mc-mod-worktrees/m16-lore-books` |

## 接口关系图（提供方 → 消费方）

```
M00 数据管线      → 全部模块（语料同步、catalog 索引再生成、校验基线）
M01 境界修炼      → M02/M04/M05/M08/M10/M11/M13/M14/M15（境界/灵根/体质/寿元门槛 API）
M02 功法术法      → M10/M15（SkillEffectRegistry 效果 id）；消费 M01 门槛、M14 状态注册表
M03 物品目录      → M04/M05/M06/M08/M09/M10/M11/M15/M16（物品 id 权威 + 别名解析）
M04 炼制生产      → M05（产出物价格锚点）；消费 M03 物品 id、M07 多方块工作站判定
M05 经济交易      → M08（贡献商店框架）、M12（商人 NPC 复用商店 UI）；消费 M03/M01
M06 区域事件      → M07/M08/M09/M10/M11/M13（region_id 注册表、每日事件调度、灵气/地脉钩子）
M07 阵法多方块    → M04（炼制工作站）、M08（宗门阵法枢纽）、M13（传送阵结构判定）
M08 宗门势力      → M11（声望/阵营状态供任务门槛）、M05（贡献商店货架）；消费 M01/M03/M06/M12
M09 秘境副本      → M11（秘境通关事件供任务钩子）；消费 M06/M13/M10/M03
M10 妖兽生态      → M09（Boss 实体）、M06（区域刷怪表数据）；消费 M01/M03/M14、M02 效果 id
M11 任务主线      → M13（任务完成 flag 供飞升前置）、M06/M09（任务钩子注册）；消费 M12/M08/M01/M03
M12 NPC对话       → M11（对话 API）、M05（NPC 商人框架）、M08（宗门执事交互）；消费 M06 region_id
M13 维度飞升      → M06/M09（维度 id 注册表）；消费 M01 飞升门槛、M07 传送阵、M11 任务 flag
M14 战斗状态      → M02/M10/M15（统一状态 id 注册表、伤害管线钩子）；消费 M01 境界推导属性
M15 法宝器灵      → M14（法宝装备属性钩子）；消费 M03/M01/M14/M02
M16 图鉴百科      → 无下游（只读展示层）；消费全部模块的最终数据
```

## 合并顺序（对齐 EXPANSION_ROADMAP 优先级）

- **第 1 批（P0 基座）**：M00 → M01 → M02、M03（并行）
- **第 2 批（P0 生产/战斗）**：M04、M14（并行，依赖 M03/M01）
- **第 3 批（P1 骨架）**：M07、M05 →（P0-C）M06 →（P0-E）M08
- **第 4 批（P1 内容）**：M10、M12（并行）→ M09、M11
- **第 5 批（P2 收口）**：M13、M15 → M16（最后，只读展示）

跨批次接口先以"接口桩 + TODO 注释"占位，禁止绕过接口直接实现下游功能。

## 全局已知阻塞（M00 首项）

当前工作树下语料校验测试红：**预期 346 部功法、实际发现 747 部**，且存在 `body.json.json` 索引解析问题（`文本材料/data/techniques/index.json` 项与文件名拼接产生双后缀）。提交基线（committed baseline）构建为绿（383/383）。任何模块动 `src/` 前，先确认 M00 是否已修复该基线，或在本 worktree 内仅验证与本模块相关的聚焦测试并在报告中注明。

## 所有 worktree 通用工作流（CLAUDE.md 摘要）

1. 动手前读 `project_docs/ai_handoff.md`、`project_docs/step_progress.md`，涉及物品/系统/版本/规划再读 items/pending_requests/features/missing_and_placeholders。
2. 修改任何既有文件前备份到 `.bak/<时间戳>/`（保留相对路径）。
3. 代码/资源/数据包改动 → `mod_version` 升一个 patch（`0.1.X`）；改包字段/顺序/类型 → 同时升 `ModNetwork.PROTOCOL_VERSION`；纯文档不升。
4. 改完跑 `./gradlew build`，失败必须修复或以精确失败信息记录阻塞。
5. 完成后更新 project_docs 并在 `project_docs/updates/` 加更新说明。
6. 只暂存本任务文件；中文提交主题+正文；本地 commit 即终点，**严禁 push / PR / 任何远程操作**。
