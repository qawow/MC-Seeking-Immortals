# 0.2.154 原生任务追踪与 FTB 文本收口

## 变更类别

代码、资源、数据生成器、测试与项目文档。网络数据包字段、顺序、类型、注册和频道行为未改变。

## 完成内容

- 原生任务追踪器同步 62 条任务链，提供全部、可接、受限、进行和完成筛选，以及接取、推进和分支按钮。
- 任务详情补齐双语任务标题、描述、下一目标、绑定任务角色、接取要求、材料条件、中段里程碑、终章奖励、分支奖励和已领取状态。
- `requires`（鬼修路线）、`race_required`（慕兰法士身份）和 `parent_chain` 进入服务端接取门禁；`karma_required`、阶段分支、编年事件和场景约束因当前资料未提供完整权威运行时规则，明确标为资料条件而不伪造硬门禁。
- FTB 九章保持 215 个节点、62 条链和 241 个阶段映射。所有物品检测为非消耗，FTB `rewards` 为空，原生账本继续唯一发奖方。
- 44 个无原生标签节点中，41 个通过显式展示映射关联正确任务链，3 个使用“章节剧情节点”副标题并明确不计入原生任务链；修复了内部代码 ID、英文和章节首链奖励错配。
- 奖励 token 支持 `id*count` 与 `id:count`，异常大数量限制为 4096，避免整数溢出和异常批量投递。

## 验证

- `python3 scripts/generate_ftb_quest_projection.py --check`：9 章、215 节点、62 链、241 阶段一致。
- 定向 `QuestPresentationServiceTest`、`TextQuestChainServiceTest`、`FtbQuestSnbtTest`、`FtbQuestDefaultsTest`、客户端数据/布局测试通过。
- 最终普通 `./gradlew build --no-daemon --max-workers=1`：`BUILD SUCCESSFUL`（1 分 17 秒）；`aiPreflight` 记录 `mod_version=0.2.154`，全量测试与打包通过。

## 版本、协议与回滚

- `mod_version=0.2.154`。
- `ModNetwork.PROTOCOL_VERSION=29` 保持不变。
- 回滚目录：`.bak/20260723_quest_tracker_followup/`、`.bak/20260723_quest_tracker_authored_requirements/`、`.bak/20260723_quest_tracker_manifest/`、`.bak/20260723_quest_tracker_version_docs/`、`.bak/20260723_quest_tracker_narrative_reconcile/`。

## 剩余风险

仍需真实客户端在极小窗口、不同 GUI 缩放和英文环境检查长文本折行；FTB/Patchouli 流程需要可选依赖实机浏览。魔道因果、阶段分支、编年前置及队伍/周期/环境约束仍是资料展示，待对应权威系统定义后再升级为硬门禁。
