# 全模块继续扩充 v69

对齐汇编 **结构优先**（古宝/妖兽十三级/灵石四级/宗门专精），并补 **阴司、大晋佛修** 线。

## 新增与批量补全

| 类别 | 内容 |
|------|------|
| **阴司** `yin_underworld_cluster.json` | 阴冥、冥河区域 `setting` + 鬼道进入条件；集群整体说明 |
| **佛修** | +功法 **金刚诀**；+势力 **大晋佛寺一脉**（写入 `cultivation_methods` / `sects`） |
| **宗门秘传** | 青元内门篇、落云结婴辅方、天符高阶符方 → `sect_contribution_shop` |
| **灵界势力** | +云梦山灵族、人族联军；`faction_species` 全条补全 |
| **编年史** | +大晋佛修北传 |
| **索引/规则表**（约 20+ 文件） | 古宝索引、蛮荒魔王、灵石阶梯、妖兽/法宝阶位图、符材、阵材、傀儡部件、协同、炼器索引、货币等 → 统一 `setting` + `learn_requirements` |
| **样例法术** `techniques_sample.json` | 指向主表 `cultivation_methods` 的注记 |

## 脚本与包

`novel_world_expansion_waves_v69.json` · `expand_world_modules_v69.py` · `seeking_immortals_world_v69.zip`

## 说明

`novel_*_waves.json` 种子文件本身可不写 `setting`（由合并脚本写入主表）；主玩法 JSON 在 v69 后覆盖更完整。