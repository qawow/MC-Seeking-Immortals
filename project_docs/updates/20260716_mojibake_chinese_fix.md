# 2026-07-16 中文乱码修复

## 变更类型

- Docs + 已入库代码/注释修复收尾（docs-only 本批剩余部分）。
- `mod_version`：保持 `0.1.506`（代码乱码修复已在该版本提交）。
- 网络协议：未修改 packet 字段/顺序/编码；`ModNetwork.PROTOCOL_VERSION` 保持 `20`。

## 完成内容

1. **典型 mojibake（UTF-8 中文被错误解码）**
   - `gradle.properties`：`mod_name=寻仙问道`，`mod_description=原创凡人流修仙玩法模组：灵力、境界、灵石、丹药、法宝、符箓与灵石矿。`
   - `PlayerCultivation.java` / `ModEvents.java`：走火、灵力、灵根、飞行等相关中文注释恢复。
   - `docs/phase-5-report.md`、`project_docs/updates/20260628_0.1.60.md`：整篇中文恢复。
2. **交接文档**
   - `project_docs/ai_handoff.md`：保留顶部 CURRENT TRUTH 历史；损坏中文主体替换为可读交接正文。
3. **路径损坏**
   - `文本材料/` 在 `missing_and_placeholders.md`、`pending_requests.md`、`step_progress.md` 中的损坏串已恢复。

## 未处理 / 延期

- `project_docs/features.md`、`step_progress.md` 等历史区大量 `U+FFFD`（U+FFFD）截断：多为不可逆丢字，未整文件重写。
- `review_diff.txt`：本地审计 diff，含大量历史乱码，不进游戏，暂不处理。

## 备份

- `.bak/20260716_161026/`

## 验证

- 关键路径扫描：`gradle.properties`、Java 源、`phase-5-report`、`0.1.60` 更新说明无典型 mojibake。
- 代码侧修复已由 `64aa70fb` / `ac9bb29f` 入库；本文件记录 docs 收尾。
