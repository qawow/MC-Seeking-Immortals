# 全模块继续扩充 v65

承接 v64，补**炼器·商会·正魔/海域冲突·编年史·魔道六宗·商路·刷怪**等「经济—战争—探索」闭环。

## 扩充一览

| 文件 | 内容 |
|------|------|
| `refinement_recipes.json` | **73** 条配方全部 `learn_requirements.craft` + `setting`（关键配方有详述 patch） |
| `merchant_shops.json` | +5 商铺（万宝楼、乱星海黑市、大晋灵石庄、坤吴阵材、阴罗鬼市）；全店 `setting` / `trade` 限制 |
| `faction_conflict_events.json` | +4 冲突（正魔边境、慕兰入侵、星宫清剿、夺矿）；全条 `setting` |
| `chronicle_events.json` | **55** 条编年史补 `setting` + `discover`；坤吴/大衍/七派/魔道北迁等详述 |
| `demonic_six_sects.json` | 六宗全部 `setting` + 入门 `learn_requirements`（叛宗/魔道因果） |
| `trade_routes.json` | +3 商路（天南—乱星海、大晋蛮荒、天南—大晋陆路） |
| `spawn_tables.json` | +4 刷怪表（血色禁地、浅海、坤吴傀儡殿、慕兰风骑） |
| `quest_hooks.json` | +4（魔道招揽、万宝寄售、蛮荒护送、阴罗鬼契） |

## 种子与脚本

- `data/novel_world_expansion_waves_v65.json`
- `python3 scripts/expand_world_modules_v65.py`
- 包：`seeking_immortals_world_v65.zip`

## 与汇编对应

- **结构优先**：炼器=配方+境界+地火/炉；商会=灵石阶梯+贡献；魔道=因果与正派禁入  
- **不硬编精确成功率**：`success_base` 等仅在有处保留，新增以 `note` 说明为主