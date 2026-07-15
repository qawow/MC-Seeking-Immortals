# 设定完善续 v138

> 无代码。承接 v137「建议下一刀」前三项。

## 1. 术法字段回填（techniques/*）
- 扫描约 **747** 条术法  
- 顶层补齐：`element` / `path` / `alignment` / `tags` / `source_method`  
- 规则：`effect.element` → `element_required` → 学派默认  
- 回填后 **empty element ≈ 0**  
- 报告：`technique_field_fill_report_v138.json`

## 2. 区域卡百科摘要
`region_cards_v138.json`：**11** 张  
天南、乱星海、大晋、慕兰、阴司、天渊、风元、地渊、仙界、黑风海、秘境总述  
每张含：摘要、维度、势力、旅行 id、秘境、节点、风险、原著锚点

## 3. NPC 对话模板
`npc_dialogue_templates_v138.json` + 写回 `npc_dialogue_templates.json`  
**12** 个职能 archetype（贡献执事、阵看守、星宫、逆星、天渊、真言、轮回、天庭、黑风、灰界、摊主、秘境领队）  
v137 具名 NPC 已绑 `dialogue_archetype`

## 仍缺（诚实）
- 功法主表若独立于 techniques 的字段专项  
- 更细区域百科  
- 对话分支/好感曲线全表  

## 4. 功法主表 cultivation_methods.json
- 共 136 条回填 element/school/alignment/tags
- empty_after: {}
