# 方案四《符箓黄纸》Talisman Paper

## 气质
符修的案头：明黄符纸做底，朱砂符线做框，浓墨字。比方案〇更浓烈、更
"术法"——朱砂不再只是印章，而是贯穿边框与强调的主线。与符箓/阵法
玩法呼应最强。

灵感语汇：黄裱纸、朱砂符线、敕令、云篆、镇符。

## 四场景

| 场景 | 中文 | 符纸 | 墨字 | 符线/强调 |
|---|---|---|---|---|
| QUIET_STUDY | 静符 | 淡黄纸 `#EEE6C4` | 青墨 `#2A3028` | 靛符 `#3A5A78` |
| FIELD_NOTES（默认） | 行符 | 明黄纸 `#F0E4B8` | 浓墨 `#302A20` | 朱砂 `#A83828` |
| LEDGER_HALL | 财符 | 深黄纸 `#E8D8A0` | 褐墨 `#342C1C` | 赤金 `#B07828` |
| OMEN_RED | 敕令 | 焦黄纸 `#ECDCB0` | 焦墨 `#362418` | 浓朱 `#C03020` |

> 浅暖色系，与方案〇同为"浅底深字"，**InkPaletteTest 现行断言直接可用**，
> 是五案中试装最零成本的一套。

## 色板展开
`expand_palette.py` 三元组如上表。额外规则：
- `row/rowHovered/rowSelected` 用符线色的 12%/24%/38% 洗层（比基线更显色）。
- `dividerGlow` 直接用符线色 55 alpha——分隔线就是一道细符线。

## 章法（差异点）
- 边框：**双层符线**——外 1px 朱砂 + 内 1px 淡朱，四角向内画小
  "符角勾"（3px L 形回勾）。
- 标题条：仿"敕令头"——左端一枚小方印（4×4 朱底 + 1px 留白十字）。
- 行首标记用小朱点替代玉签竖条。
- 计量条：符线色描边；DANGER 状态时边框变浓朱双线（呼应 OMEN）。
- OMEN 场景整体加密符线元素：面板四边中点各一个 2×2 朱点（镇符钉）。

## 纹理生成片段
```python
def talisman_paper(base, fiber, vermilion, seed=0):
    rng = random.Random(seed)
    img = Image.new("RGBA", (32, 32), base + (255,))
    d = ImageDraw.Draw(img)
    for _ in range(22):  # 纸纤维（沿用基线）
        y = rng.randrange(32); x = rng.randrange(32); l = rng.randrange(4, 12)
        d.line([(x, y), (x + l, y)], fill=fiber + (46,))
    for _ in range(3):   # 极稀朱砂飞白
        x, y = rng.randrange(32), rng.randrange(32)
        d.point((x, y), fill=vermilion + (30,))
    return img
```

## 优缺点
- ✅ 试装零成本（浅色断言直接复用）；朱砂主题与符箓/阵法/丹符玩法联动感最强。
- ✅ 黄纸底在 MC 原版棕色系 GUI 生态里非常协调。
- ⚠️ 黄底大面积使用略"暖过头"，静修场景靠靛符线降温；
  朱砂多处使用后，OMEN 场景的"危"感区分度要靠浓度而非色相，需微调把握。
