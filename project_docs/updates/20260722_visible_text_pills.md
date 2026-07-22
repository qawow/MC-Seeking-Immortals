# 2026-07-22：玩家可见文本与丹药去重（0.2.137）

## 范围

- 审计目录物品、工站、炼丹、法宝、捕获、灵兽、任务和技能树的玩家可见文字，避免原始英文、版本标记、snake_case 或内部 registry id 泄漏。
- 重点检查鉴宝卷、大乘护劫丹方、宗门令牌等批量目录载体在缺少语言键时的 tooltip 回退。
- 对回灵丹、凝气丹、破境丹及同类丹药按运行时效果核对重复关系。

## 结论与实现

- `CatalogItemDescriptionService`、`BaseMaterialItem`、炼丹/工站/法宝/功法显示 helper 现在在缺少安全本地化文本时使用中文未知项或通用用途说明，不直接显示目录 JSON 原文。
- 动态消息使用已注册的本地化名称；未知站点、状态、效果和功法门槛不回显原始 id。阴罗鬼氅的显示文本也已统一。
- 目录索引中的历史价值带与小说批次改用“物品目录·价值带一/二/三/四/五”“小说物品索引·完整/全量”等实际中文分类名，生成脚本同步更新。
- 旧 `qi_recovery_pill`、`cultivation_pill`、`breakthrough_pill` 注册物品、Java 类、配方、模型和纹理已删除。旧字符串只保留在 `AlchemyFormulaKnowledge` 的兼容读取路径。
- 旧 `jiangying_pill` 仅保留兼容别名解析到 `jiangchen_pill`，不再有正常注册、配方、模型、纹理或产出。
- `spirit_recovery_pill`（恢复 50% 灵力）、`cultivate_speed_pill`（限时修炼效率）、`jiangchen_pill`（炼气期筑基辅助）分别对应不同效果；正式筑基丹和高品质回灵丹仍保留为独立阶级/品质。
- 普通玩家使用百科术语查询时只看到安全中文主名/别名；内部 id 仍可作为查询输入，但不再回显。炼丹炉查找与 NBT 读取会把旧丹方 id 规范化到现行配方。

## 验证与版本

- 新增丹药与术语契约测试，断言旧三个 id 不再有运行时效果条目、三个替代效果互不相同、术语命令不回显 id；旧丹方索引兼容路径也受测试约束。
- `python3 scripts/list_eng_displays.py` 报告 0 条；`python3 scripts/m16_lang_audit.py` 报告 `zh=5459 en=5459`；全资源 JSON 解析通过；定向测试通过；最终 `./gradlew build --no-daemon` 为 `BUILD SUCCESSFUL in 1m 8s`。
- `mod_version=0.2.137`；没有修改网络包字段、顺序、类型或频道行为，`ModNetwork.PROTOCOL_VERSION` 保持 `26`。
- 备份：`.bak/20260722_0.2.136_visible_text_pills/`、`.bak/20260722_0.2.136_followup/`、`.bak/20260722_0.2.137_finalize/`、`.bak/20260722_0.2.137_glossary_pill_contract/` 与 `.bak/20260722_0.2.137_compat_safety/`。

## 遗留风险

旧存档中已经写入的已删除物品栈可能由 Forge 显示为缺失物品；兼容 NBT 只覆盖丹方知识，不会凭空恢复已删除 registry。管理员调试命令仍保留受权限保护的机器 id 输出；普通玩家命令与百科路径不回显内部标识。
