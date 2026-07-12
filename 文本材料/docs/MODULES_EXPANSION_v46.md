# 各模块扩充 v46

> 汇编 **§9.7** 星宫派系 · 凡人/修士分层 · **§18** 结构非概率

## 凡人采珠船队

| 日常事件 | 效果 |
|----------|------|
| `pearl_diving_mortals` | 外海摊补 `pearl_raw` |
| `pearl_storm_shelter` | 避港、税涨标签 |
| `pearl_tax_dispute` | 星宫 vs 船队声望抉择 |
| `cultivator_pearl_snatch` | 护送任务 `protect_mortal_fleet` |

- `economy_price_bands.json`：`pearl_raw` 量级 5–15 下品灵石
- 与 `mortal_pearl_fleet`（`chaotic_sea_factions` 中立势力）一致

## 星宫双圣 / 派系内斗

| 派系 | 标签 |
|------|------|
| 执法派 | 巡海强硬、逆星悬赏加成 |
| 贸易派 | 岛税、拍卖便利 |
| 长老会中立 | 调停 |

- `star_palace_internal_factions.json`
- 任务链 **`star_palace_internal_politics`**（执法/贸易**互斥**分支 → 长老会 → 岛税表决）
- 不写具体圣者名，用职务 archetype

## Forge 加载

- `SeekingImmortalsMod.java`
- `LorePackReloadListener` → `AddReloadListenerEvent` → `LorePackLoader.loadAll`
- `mods.toml` 桩

## 打包

**`seeking_immortals_lore_v46.zip`**

## 下一步

- 修正 `SimplePreparableReloadListener` 构造以匹配目标 MC 版本
- 慕兰 / 天澜 势力任务与 `mulan_war` 深化
- 全 JSON schema_version 一致性扫描脚本