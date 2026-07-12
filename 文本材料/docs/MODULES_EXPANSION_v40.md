# 各模块扩充 v40

> 汇编 §9 魔道专精标签 · §9.7 天渊城 · §18 技能树 · 五界飞升**结构**

## 魔道六宗

| 宗门 | 专精标签 | 功法 |
|------|----------|------|
| 鬼灵门 | 摄魂/炼尸 | 鬼灵摄魂诀 + 玄阴 |
| 合欢宗 | 双修媚术 | 合欢媚心诀 |
| 天魔宗 | 魔体血祭 | 天魔炼体诀 |
| 青罗宗 | 毒虫 | 青罗毒经 |
| 万狐宗 | 幻形 | 万狐幻形诀 |
| 血巫教 | 血咒 | 血巫咒经 |

- `demonic_six_sects.json` + 任务链 **`demonic_six_path`**
- `techniques/demonic.json` v2：分宗门 `requires_method`
- 地区 **`tiannan_north_waste`**
- 正道悬赏反噬（黄枫等 `karma`）

## 飞升 / 灵界

| 文件 | 内容 |
|------|------|
| `ascension_flow.json` | 化神巅峰→天渊→风元大陆→大乘飞仙（DLC） |
| `dimensions_catalog.json` v2 | 天渊 **2×** 灵气、走私线 `P3_smuggle_ascension` |
| `region_cards/tianyuan.json` | 功勋币、入伍 hook |
| `region_cards/spirit_fengyuan.json` | 灵界风元、上升线 |
| `spirit_realm_rise` | 挂 `ascension_ref` |

## 技能树 +7 类

鬼灵/合欢/天魔 + **天渊戍边**（功勋向）

## 汇编合规

- 保留魔道/正道阵营与**专精标签**，不绑主角法宝名  
- 飞升用境界 + 天劫成功 + 单向门，不用精确飞升成功率  

## 规模

宗门 **~28**、任务链 **16**、魔道术 **~8**、功法 **~35**

## 下一步

- 阴司冥河与 `yin_underworld_cluster` 深化
- 妖族七妖王 / 蛮荒线
- 打包 v40 zip