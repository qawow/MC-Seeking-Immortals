# 2026-06-25 前端 / 客户端重构交接文档

## 变更分类

Docs-only。

## 内容

- 新增 `project_docs/frontend_refactor_handoff.md`。
- 将当前客户端/前端代码从整体项目中单独拆分列出，覆盖：
  - `client/` 包内 Screen、Overlay、HUD、输入事件、客户端数据镜像和 renderer。
  - GUI 贴图、语言文件、模型/贴图等前端资源。
  - `SyncCultivationDataPacket`、`SyncLearnedTechniquesPacket`、`ReleaseTechniquePacket`、`SetTechniqueSlotPacket`、`SetMeditatingPacket`、`AttemptBreakthroughPacket` 等 UI 边界 packet。
  - 当前 UI 流程、客户端同步数据流、客户端意图到服务端校验的数据流。
  - 交给其他 AI 重构时的任务切分、验证清单和禁止跨越的服务端权威边界。
- 更新 `project_docs/ai_handoff.md`，增加前端 / 客户端重构必读入口。
- 更新 `project_docs/step_progress.md`，记录本次文档沉淀。

## 版本与协议

- 未修改 Java、资源、数据包、构建逻辑或 gameplay/config。
- `mod_version` 不变，当前以 `gradle.properties` 的 `0.1.54` 为准。
- 未修改 packet 字段、顺序、编码/解码或通道行为，`ModNetwork.PROTOCOL_VERSION` 维持 `5`。

## 构建

未运行 `./gradlew build`。本次仅 Markdown 文档变更。

## 回滚

既有文档备份路径：`.bak/20260625_021500_frontend_refactor_docs/`。
