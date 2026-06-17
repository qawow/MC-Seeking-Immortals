# Item Resource Validation Report

**最近更新**: 2026-06-17（贴图正式应用完成）
**Mod**: 寻仙问道 (Seeking Immortals) — Minecraft 1.20.1 Forge 47.2.0
**数据来源**: `ModItems.java`（注册）× `models/item/*.json` × `textures/item/*.png` × `textures/block/*.png` × `lang/*.json` × `generated_art/raw/*.png`
**构建状态**: `BUILD SUCCESSFUL`

> 上一版（2026-06-17 上午）盘点的是“待接入”状态；本次更新反映贴图已正式应用后的实际接入情况。

---

## 1. Summary（当前状态）

| 项目 | 当前值 | 上一版（接入前） |
|------|--------|-----------------|
| Java 注册物品总数（ModItems） | 121 | 121 |
| `models/item/*.json` 数量 | 121 | 99 |
| `textures/item/*.png` 数量 | 178 | 87 |
| `textures/block/*.png` 数量 | 3 | 2 |
| 已注册但缺 model JSON 的物品 | **0** | 26 |
| model JSON layer0 指向不存在贴图 | **0** | 11 项（7 有效 + 4 孤儿） |
| 已注册但完全缺贴图的物品 | **0** | 34 |
| 仍使用 placeholder 贴图的注册物品 | **1**（`technique_manual_azure_origin_sword_derivative`，raw 无正式图） | 60+ |
| 中文 lang 缺失名字 | 0 | 0 |
| 英文 lang 缺失名字 | **0** | 26 |
| 英文 lang 总键数 | 260 | — |
| 中文 lang 总键数 | 270 | 270 |
| `generated_art/raw/` 中无对应注册项的别名/备用素材 | 7 | 7 |

**结论：121 个注册物品现已全部具备 model + 贴图，layer0 全部可解析，中英文 lang 齐全。**

---

## 2. 已注册但缺贴图（PNG 不存在）

**无。** 本次接入已补齐全部 34 个原缺图物品的贴图与（需要的）模型。

---

## 3. 已注册但缺 model JSON

**无。** 本次为 26 个原缺模型的物品新建了标准 `item/generated` 模型。

---

## 4. model JSON 的 layer0 指向不存在贴图

**无。** 全部 121 个 model 的 layer0（或 block parent 引用）均已可解析为存在的 PNG。

> 已清理：上一版遗留的 4 个基础灵石孤儿 model JSON（`spirit_stone.json` / `spirit_stone_mid.json` / `spirit_stone_high.json` / `spirit_stone_superior.json`）已删除，不再计入。

---

## 5. lang 一致性

- **中文 zh_cn.json**: 121 个注册物品全覆盖（0 缺失），共 270 键。
- **英文 en_us.json**: 本次补齐 26 个缺失英文名，现 0 缺失，共 260 键。

本次新增的英文条目（26 个）：

```
rejuvenation_pill_low, foundation_building_pill_low, healing_pill_low,
clear_spirit_powder_low, fasting_pill_low, calming_pill_low,
spirit_grass, cloud_mushroom, phoenix_feather_flower, dragon_blood_grass,
immortal_ginseng, beast_core, spirit_beast_bone, dragon_scale,
phoenix_feather, true_dragon_blood, spirit_iron, cold_jade, star_meteorite,
celestial_crystal, chaos_gold, soul_fragment, void_crystal, time_sand,
primordial_essence, immortal_jade, ling_gen_test_stone
```

---

## 6. 功法卷轴接入情况

60 个功法卷轴原本全部指向 `*_placeholder` 占位贴图。本次：

- **59 个**：已复制 `generated_art/raw/technique_manual_<id>.png` 到 `textures/item/technique_manual_<id>.png`，并将对应 model JSON 的 layer0 从 `..._placeholder` 改为正式名。卷轴现在显示独立正式贴图。
- **1 个** `technique_manual_azure_origin_sword_derivative`：raw 中无正式贴图，**保留 placeholder**（model 仍指向 `technique_manual_azure_origin_sword_derivative_placeholder.png`，文件存在，游戏内可正常显示占位图）。

---

## 7. 本次接入操作清单（已完成）

均为资源文件操作，**未修改任何 Java 代码**。

### 复制贴图（model 已存在，7 个）

`qi_recovery_pill`、`cultivation_pill`、`breakthrough_pill`、`spirit_charm`、`fire_talisman`、`armor_talisman`、`speed_talisman`

### 复制贴图 + 新建 model JSON（26 个）

丹药 6：`rejuvenation_pill_low`、`foundation_building_pill_low`、`healing_pill_low`、`clear_spirit_powder_low`、`fasting_pill_low`、`calming_pill_low`
材料·灵草 5：`spirit_grass`、`cloud_mushroom`、`phoenix_feather_flower`、`dragon_blood_grass`、`immortal_ginseng`
材料·妖兽 5：`beast_core`、`spirit_beast_bone`、`dragon_scale`、`phoenix_feather`、`true_dragon_blood`
材料·矿物 5：`spirit_iron`、`cold_jade`、`star_meteorite`、`celestial_crystal`、`chaos_gold`
材料·特殊 4：`soul_fragment`、`void_crystal`、`time_sand`、`primordial_essence`
货币 1：`immortal_jade`

新建模型统一采用：
```json
{
  "parent": "minecraft:item/generated",
  "textures": { "layer0": "seeking_immortals:item/<item_id>" }
}
```

### 方块贴图（1 个）

`generated_art/raw/spirit_ore.png` → `textures/block/spirit_ore.png`
（`spirit_ore` 的 item 模型 parent 到 `seeking_immortals:block/spirit_ore`。）

### 卷轴升级（59 个）

复制 raw 正式图到 `textures/item/`（去 `_placeholder`），model JSON 的 layer0 改为正式名。

### 英文 lang 补齐（26 个）

见第 5 节。

### 清理孤儿文件（4 个，已完成）

删除已从代码移除的基础灵石 model JSON：
`spirit_stone.json`、`spirit_stone_mid.json`、`spirit_stone_high.json`、`spirit_stone_superior.json`

---

## 8. 备份

本次接入前已备份待修改资源至：
`.bak/20260617_180434_apply_art/`
（含 `textures_item/`、`textures_block/`、`models_item/`、`en_us.json`、`zh_cn.json`）

如需回滚，恢复该备份目录对应文件即可。

---

## 9. 已完全接入的物品（121 个全部就绪）

- **五行灵石（20）**：金/木/水/火/土 × 4 品级
- **货币（1）**：`immortal_jade`
- **丹药（9）**：3 传统丹药 + 6 下品丹药
- **符箓/法器（7）**：`spirit_charm`、`flying_sword`、`flying_artifact`、`fire_talisman`、`armor_talisman`、`speed_talisman`、`ling_gen_test_stone`
- **工具（2）**：`spirit_detector`、`leyline_compass`
- **方块（3）**：`spirit_ore`、`meditation_cushion`、`spirit_gathering_array`
- **材料（19）**：灵草 5 + 妖兽 5 + 矿物 5 + 特殊 4
- **功法卷轴（60）**：59 正式贴图 + 1 占位

---

## 10. raw 中无对应注册项的素材（别名/备用，共 7 个，未接入）

| raw 文件 | 实际对应注册 id | 处理 |
|----------|---------------|------|
| `spirit_recovery_pill.png` | `qi_recovery_pill` | 别名，未使用（同图已存在正式名） |
| `qi_condensing_pill.png` | `cultivation_pill` | 别名，未使用 |
| `foundation_establishment_pill.png` | `breakthrough_pill`（可选） | 别名，未使用 |
| `mind_stabilizing_pill.png` | `calming_pill_low` | 别名，未使用 |
| `mystic_vial.png` | — | 备用素材，无对应物品 |
| `spirit_liquid.png` | — | 备用素材，无对应物品 |
| `waste_pill.png` | — | 备用素材（废丹），无对应物品 |

---

## 11. 遗留与后续建议

1. **`technique_manual_azure_origin_sword_derivative`**：仍用 placeholder，需补一张正式卷轴贴图（命名 `technique_manual_azure_origin_sword_derivative.png`，放入 `textures/item/`，并把 model layer0 改为正式名）。
2. **丹药命名别名**：raw 里 `breakthrough_pill.png` 与 `foundation_establishment_pill.png` 均对应破境丹，本次已用与注册 id 一致的 `breakthrough_pill.png`，别名文件保留在 raw 备用。
3. **形态复核建议**：`clear_spirit_powder_low`（清灵散，应为粉末/小瓶）、`true_dragon_blood`（真龙血，应小瓶液体）的 raw 贴图若与预期形态不符，可人工复核替换。
4. **备用素材** `mystic_vial`/`spirit_liquid`/`waste_pill`：暂无对应物品，保留备用；若后续开放玉瓶/废丹系统可启用。

---

**报告结束**
