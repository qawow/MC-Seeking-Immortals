# 各模块扩充 v38

> 汇编 §18 技能树 · §9 宗门专精标签 · 七玄/化刀/巨剑天南小派

## 掩月幻术

- `illusion.json` v2：筑基+ 挂 **掩月幻心诀**；+月纱障、掩月幻阵（secret）
- 技能树 **`illusion_yanyue`**
- 任务：掩月阵试（v37）

## 七玄门凡俗线

| 环节 | 内容 |
|------|------|
| 任务链 | `qixuan_mortal_path`（山村→采药→入门→衰落遗册） |
| 功法 | 七玄凡诀、青珠诀（残传） |
| 年表 | `K10_qixuan_decline` |
| 地区 | `region_cards/qixuan_village.json` |
| 商店 | 七玄村货摊 |

结构向原著「小派衰落、凡人起步」，不绑具体剧情 NPC 名。

## 化刀坞 / 巨剑门

| 链 | 终奖 |
|----|------|
| `huadao_blade_path` | 化刀刀意残页 |
| `giant_sword_gate_path` | 银色巨剑图谱 + 巨剑诀（可选血色禁地线索） |

技能树：`blade_huadao`、`blade_giant_sword`

## 其它

- `puppet.json` v2：大衍术门控
- `movement` / `recovery` v2：`tier`
- 任务链 **12**、任务钩 **~62**、术法 **~195**

## 汇编提醒（§18）

保留境界三段、法宝十一级等**结构**；数值与成功率表仅作模组参考。

## 下一步

- 千竹教傀儡线、御灵宗隐秘关联
- 大晋世家 `dajin_clan_ancestor_art` 任务
- 打包 v38 zip