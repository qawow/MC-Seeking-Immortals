# 物品贴图风格规范（凡人修仙向 · MC 物品）

## 硬性规则（丹药优先）

1. **一物一图**：每个 `item_id` 单独一次 `generate_image`，禁止四宫格拼批（旧任务 `66ddc2f7` 等仅作废弃参考，不用于正式入库）。
2. **透明底**：prompt 必须含 `fully transparent background`；入库加 `--transparent` 抠深色残留。
3. **MC 尺寸**：主文件 `{item_id}.png` 为 **16×16**；可选 `{item_id}_32.png` 预览。

## 视觉基调

- 中式修仙、凡人冷色：青玉、赭石、弱金光，少艳俗仙侠
- 主体占画面约 70%，轮廓简单（缩 16px 可读）
- 无文字、无水印、无地面投影

## 丹药 prompt 模板

```
Single Chinese xianxia game item icon: one <英文名/功效> (<中文名>), <颜色形态描述>.
ONLY ONE object, centered, Fanren Xiuxian style, fully transparent background,
no backdrop no floor no cast shadow, simple silhouette for Minecraft 16px item texture, no text
```

队列：`data/asset_texture_pill_queue.json`（47 丹药，已入库 `jiangchen_pill` 等除外）

## 入库命令

```bash
python3 scripts/ingest_pill_from_task.py --task-id <任务ID> --item-id condensation_pill
```

## 目录

- `assets/textures/item/<item_id>.png`
- `forge_scaffold/src/main/resources/assets/seeking_immortals/textures/item/`