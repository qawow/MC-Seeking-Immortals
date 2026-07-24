# 原著设定提取 v148 — 原文描述与视觉描述

> 来源：《凡人修仙传》《凡人修仙传仙界篇》全本（约 35MB / 4795 章）单次全文扫描。
> 方法：以既有 data/ 实体词典 + 境界/地理/势力词表（2229 词）构建 Aho-Corasick 自动机，
> 单次扫描全文，抽取每个实体的首现章节、出现频次，以及含视觉关键词（光/芒/霞/焰/色/形…）的原文句。
> 性质：原文引文素材，供入包前人工校订；非逐件数值。schema v55。

## 统计

- 命中实体：**445** 个
- 视觉描述原文句：**848** 条

| 类别 | 文件 | 实体数 | 视觉句 |
|---|---|---|---|
| 法宝 | `novel_extract_artifacts_v148.json` | 66 | 115 |
| 法术 | `novel_extract_techniques_v148.json` | 98 | 172 |
| 材料 | `novel_extract_materials_v148.json` | 121 | 164 |
| 异兽 | `novel_extract_beasts_v148.json` | 62 | 126 |
| 设定 | `novel_extract_settings_v148.json` | 98 | 271 |

## 字段说明

每条实体：`display`(中文名) · `source_books`(出现于哪部) · `first_appearance`(首现书+章) ·
`mention_count`(全文出现次数) · `chapter_hits`(出现章节数) · `visual_descriptions`(视觉原文句) ·
`setting_descriptions`(设定原文句)。设定类中 `term:true` 为泛称术语（如法宝/灵石/洞府），非具体实体。

## 高频实体示例

- **法宝**：青竹蜂云剑(927)、虚天鼎(420)、灵兽袋(253)、符宝(207)、风雷翅(187)
- **法术**：炼神术(450)、元磁神光(231)、大衍诀(202)、大五行幻世(201)、大五行幻世诀(198)
- **材料**：噬金虫(1037)、阵旗(403)、紫金(401)、灵果(204)、蒲团(183)
- **异兽**：冰凤(405)、啼魂兽(267)、银翅夜叉(265)、元刹(255)、螟虫之母(247)

## 使用建议

1. `visual_descriptions` 可直接作为物品/法术 tooltip 与贴图美术参考（色彩、光效、形态）。
2. 与既有 `artifact_catalog_v92` / `materials_compendium_v92` / `techniques/*` 按 `display` 名对齐，
   把原文视觉描述补入对应条目的 `description`/`setting.lore`。
3. 泛称术语（term:true）用于世界观文案，勿当作具体物品入包。