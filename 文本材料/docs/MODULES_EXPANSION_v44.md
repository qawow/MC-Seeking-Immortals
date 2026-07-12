# 各模块扩充 v44

> 汇编 **§9.7** 乱星海 · **§18** 功法→法术门控 · 黑市结构（非精确劫掠概率）

## 海盗黑市

| 项 | 内容 |
|----|------|
| 商店 | `pirate_black_market_outer_sea` |
| 准入 | 逆星盟声望 / 贿赂 / 赃物销赃 |
| 货品 | 赃物包、低阶魔核、盗玉简、非法传送许可、虚天图碎片 |
| 风险 | 星宫抄查（事件标签，非固定概率表） |
| 势力 | `pirate_scatter` 挂 `black_market_shop` |

## 星宫镇海诀门控

- 功法：**星宫镇海诀** → 阵法 + 剑阵标签
- 新术：**镇海锁阵**、**巡海信标**、**天星剑阵**（secret）
- `formation.json` v3、`sword.json` 补 `requires_method`

## 逆星秘典

- **暗港遁步**（movement）+ 逆星秘行技能树
- 与既有 `inverse_star_art`、乱星海政治线一致

## NeoForge

- `docs/NEOFORGE_DATA_LOADER.md`：manifest 加载顺序与门控规则
- 新包 **`seeking_immortals_lore_v44.zip`**

## 下一步

- 补天丹配额与星宫声望 tier 对接 `economy_contribution_exchange`
- 外海坊市 `outer_sea_market` 地区卡
- 实装 `LorePackLoader` Java 类（forge_scaffold）