# 2026-06-25 凡人流设定实现大纲

## 变更分类

Docs-only。

## 内容

- 新增 `project_docs/fanren_setting_implementation_outline.md`。
- 将用户提供的凡人流设定资料整理为后续可执行路线：
  - MVP 集成与基础闭环。
  - 宗门与新手任务线。
  - 炼丹系统升级。
  - 炼器与法宝系统。
  - 阵法与符箓。
  - 妖兽、灵宠与材料。
  - 秘境与副本。
  - 经济、坊市与拍卖会。
  - 高阶境界与天劫。
- 更新 `project_docs/ai_handoff.md`，提示后续涉及凡人流扩展时必须读取该大纲。
- 更新 `project_docs/step_progress.md`，记录本次文档沉淀。

## 版本与协议

- 未修改代码、资源、数据包、构建逻辑或 gameplay/config。
- `mod_version` 不变，当前以 `gradle.properties` 的 `0.1.53` 为准。
- 未修改网络包字段、顺序或通道行为，`ModNetwork.PROTOCOL_VERSION` 不变。

## 构建

未运行 `./gradlew build`。本次仅为 Markdown 文档变更。

## 回滚

既有文档备份路径：`.bak/20260625_014100_fanren_outline_docs/`。

