# 维度与界层设计（P0）

> 数据：`data/dimensions_catalog.json` · 飞升链：`spirit_realm_interface.json`

## 五界 → 模组维度

| 汇编五界 | 模组 `dimension_id` | 可玩 |
|----------|---------------------|------|
| 人界 | `seeking_immortals:mortal_world` | ✅ 主世界 |
| 灵界 | `seeking_immortals:tianyuan` + `seeking_immortals:spirit_fengyuan` | ✅ |
| 阴司之界 | `seeking_immortals:yin_ming_pocket` | 秘境式 |
| 上古魔界 | `seeking_immortals:demon_rift` | 事件副本 |
| 仙界 | `seeking_immortals:immortal_realm` | 占位 |

## 入境门槛（结构向，§18）

| 目标维度 | 门槛类型 |
|----------|----------|
| 天渊城 | 化神巅峰渡劫飞升（单向） |
| 风元大陆 | 炼虚 + 天渊功勋门费 / 空间裂缝（高风险） |
| 冥河界域 | 筑基+ 或 鬼修道途 |
| 魔界裂隙 | 元婴+ 世界事件 / 坠魔谷联动 |

## 旅行矩阵

- 人界 → 天渊：**仅飞升**
- 天渊 → 人界：**分身下界**（炼虚+，弱克隆）
- 天渊 → 风元：**管制传送阵**（功勋 500）
- 秘境：**instanced**，父维度默认人界，灵界秘境父级为风元

## Forge 实现顺序

1. `mortal_world`（Overworld 规则覆盖）
2. `tianyuan`（飞升落点 + 2× 修炼区）
3. `spirit_fengyuan`（炼虚后内容）

## 关联

- 地区卡：`region_cards/tianyuan.json`、`spirit_fengyuan.json`
- 鬼修：`ghost_cultivation_path.json`
- 秘境模板：`secret_realm_template.json`（四要素不变）