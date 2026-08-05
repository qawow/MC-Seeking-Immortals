# 2026-08-06 命令参考与文档交接

## 变更类别

本批为 docs-only：没有修改 Java、资源、数据包、构建逻辑、`gradle.properties` 或网络协议。并行工作的 `CLAUDE.md` 与 `project_docs/frontend_interaction_audit_0.2.198.md` 保持未触碰、未纳入提交。

## 文档更新

- 新增 `project_docs/command_reference.md`，以 `SeekingImmortalsCommand.java` 为源，记录普通查询、权限 2 作弊/调试、异常状态、任务/NPC/商店/目录/战争/宗门管理、`live_smoke` 单机与多人签字命令。
- `ai_handoff.md` 补充 0.2.268 当前快照和并行协作边界。
- `pending_requests.md`、`missing_and_placeholders.md` 补充供给链 55→0、下一步数据驱动化、约 646 个 bulk 孤儿、经济参数和 QA 签字风险。
- `step_progress.md` 在 0.2.268 条目中登记命令文档交接。

## 版本、验证与回滚

- `mod_version` 保持 `0.2.268`；`ModNetwork.PROTOCOL_VERSION` 保持 `31`。
- 文档-only 不重复运行 Gradle 构建；最近代码基线的完整构建为 271 套件 / 1,327 项全通过，JAR SHA-256 为 `7e5b87d251bdf621de8cb2130f2c989b44d3cd5a5853a56f19fdd026629799d8`。
- 本批既有文档备份：`backups/20260806074930_docs_command_reference/`。
- 收尾应执行 `git diff --check`，只暂存本批文档，不得暂存并行工作的文件。
