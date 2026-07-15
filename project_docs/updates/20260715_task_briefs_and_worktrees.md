# 20260715 文本材料 v147 拆解为 17 份任务简报并建立对应 worktree

## 变更类别

docs-only（项目文档 + 语料批次入库；无代码/资源/构建逻辑改动，不升 mod_version，协议保持 19）。

## 更新内容

1. **语料批次入库**：提交既有 `文本材料/` v147 批次（40 修改 + 325 新增，共 365 文件），作为独立审计提交（遵循"先单独提交已完成的早期批次"策略）。
2. **任务简报**：新增 `project_docs/task_briefs/`——`README.md`（模块总表、接口关系图、合并顺序、通用工作流）+ 17 份简报 `M00_数据管线.md` … `M16_图鉴百科.md`。每份含：目标、可勾选功能点清单、涉及文件（语料侧+模组侧，所有权唯一）、依赖前置库、接口交叉（谁提供→谁消费）、验收与红线（DESIGNER_HANDBOOK §5 红线内嵌）、工作流提醒。
3. **worktree**：为每个模块建立 `task/mXX-<slug>` 分支与 `D:/codex/mc-mod-worktrees/<mXX-slug>` worktree（共 17 个），全部基于包含语料与简报的最新 main HEAD。

## 拆分要点

- 每个语料文件/Java 包只归属一个模块；跨模块只走声明接口。
- 合并顺序对齐 EXPANSION_ROADMAP：P0 = M00→M01→M02/M03 → M04/M14；P0-C = M06；P0-E = M08；P1 = M07/M05/M10/M12/M09/M11；P2 = M13/M15/M16。
- region_cards（23 张，现零 Java 消费者）由 M06 建立首个运行时消费者。

## 已知阻塞（转交 M00 首项）

当前工作树语料校验测试红：预期 346 部功法、实际发现 747 部；`文本材料/data/techniques/index.json` 条目与文件名拼接产生 `body.json.json` 双后缀。提交基线构建为绿（383/383，见 step_progress 第 449 条）。本批次为 docs-only 未运行完整构建；修复责任已写入 `M00_数据管线.md` 功能点第 1、2 项。

## 版本与协议

- mod_version：保持 `0.1.504`（docs-only 不升版）。
- ModNetwork.PROTOCOL_VERSION：保持 `19`。

## 验证

- `git -c core.quotepath=false status --porcelain` 确认 365 个脏条目全部位于 `文本材料/`，无代码/资源混入。
- 简报文件所有权经交叉检查无重叠；接口图与 README 合并顺序一致。
