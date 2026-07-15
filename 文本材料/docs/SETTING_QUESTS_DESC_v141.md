# 设定与描述完善 v141（贴原著）

> 无代码。

## 1. 任务链可玩化
`quest_chains_playable_v141.json`：**20** 条  

每条含：realm_span、prereq、steps（do/place/reward/novel_anchor）、finale、可选分支。  

覆盖：血色配额与试炼、墨蛟权重、星宫/逆星、天渊/风元、真言/幻世、轮回/天庭/黑风、虚天筹备、昆吾线索、坠魔令与灵烛果、阴阳/培婴、千竹与大衍。

`tiannan_faction_quests.json` 已挂天南相关引用。  
`dialogue_effect_quest_links_v140` 指向本可玩表。

## 2. 描述精修
| 类 | 内容 |
|----|------|
| 功法 | 长春、青元、大衍、太虚、天魔体、五行幻世、时间法则等关键描述 |
| 术法 | elemental 图标术精修 + 其余薄描述轻量模板去「空洞生成句」 |
| 关键物 | `key_item_descriptions_v141.json` 试炼令/坠魔令/灵烛果/虚天残图与鼎/阴芝马/培婴/大衍残篇/昆吾线索/太阴/飞升符 |
| 秘境 | secret_realms blurb_v141 |
| 区域 | region_cards 摘要贴原著重写 |

## 3. 原著原则（维持）
unique 不保底 · 一生一次造化 · 飞升单向 · 事件权重可选 · 完整大衍不商店直售

## 仍缺
- 全部 747 术法逐条原著精修（本卷仅关键+去空洞）
- 广寒全文战
- 任务奖励精确数值表可再拆
