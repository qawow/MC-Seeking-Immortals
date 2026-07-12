# 技能树三层：功法 → 法术 → 神通

主 schema：`data/skill_tree_layer_schema.json`  
树表：`data/skill_trees.json`（每树含 `layer_nodes` + `learn_requirements.spell/secret`）

| 层 | 作用 | 数据 |
|----|------|------|
| **功法** | 职业/主线方向、灵根门槛 | `cultivation_methods.json` |
| **法术** | 主动技能，须先习功法 | `techniques/*` `tier=spell` |
| **神通** | 高阶秘术，常见结丹+ | `tier=secret` / `secret_arts.json` |

解锁顺序：**功法 → 任意法术节点 → 神通**（`realm_min` 为门槛，非成功率）。

四大热门职业（阵/丹/器/符）见 `craft_daily_loops` 与宗门 `sect_specialty_map`，可挂副职或支线树。