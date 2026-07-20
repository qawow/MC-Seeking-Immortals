# 方案三《洞府石刻》Cave Inscription

## 气质
散修洞府的石壁：暖灰岩底、凿痕文字、青苔与油灯暖光点缀。中性、耐看、
"苦修"味最浓——最贴合凡人流"一介散修凿洞而居"的叙事底色。

灵感语汇：石窟、凿痕、苔痕上阶绿、油灯、丹房石台。

## 四场景

| 场景 | 中文 | 岩底 | 凿痕字 | 强调 |
|---|---|---|---|---|
| QUIET_STUDY | 静窟 | 冷灰岩 `#2A2E2C` | 石灰白 `#D8DCD4` | 苔青 `#6A9070` |
| FIELD_NOTES（默认） | 途岩 | 暖灰岩 `#302E28` | 沙白 `#DCD8CC` | 土黄 `#A08850` |
| LEDGER_HALL | 库窟 | 深暖岩 `#342C24` | 灯下白 `#E4D8C4` | 灯焰橙 `#C08040` |
| OMEN_RED | 裂谷 | 赤岩 `#38221E` | 燥白 `#E6D2C6` | 岩浆红 `#C05030` |

> 中明度深底（~#2E2C28）：比方案一/二亮一档，白天黑夜都不刺眼，是五案中
> 最"安全"的选择。

## 色板展开
用方案二的 `expand_palette.py` 规则展开，三元组如上表。中明度深底下
对比断言取中间档：

```java
assertTrue(panelLum >= 100 && panelLum <= 400, "mid-tone stone panel");
assertTrue(inkLum > 500, "bright chiseled text");
assertTrue(inkLum - panelLum > 220, "contrast");
```

## 章法（差异点）
- 边框：单线 + **崩口**——每隔 8-12px 随机缺 1px（凿边感，用固定 seed 确定性绘制）。
- 标题条：**阴刻**效果——上 1px 暗线下 1px 亮线夹深底（凹槽错觉）。
- 行选中：苔青色薄洗 + 行首一点"苔斑"（2×2 accent 点）。
- 计量条：仿石槽，槽内填充带颗粒；灯焰橙做 LEDGER 强调时加 1px 暖光晕。
- 印章元素改为**刻符**：方框内一个凿痕十字。

## 纹理生成片段
```python
def stone(base, crack, moss, seed=0):
    rng = random.Random(seed)
    img = Image.new("RGBA", (32, 32), base + (255,))
    d = ImageDraw.Draw(img)
    for _ in range(14):  # 凿痕短线（斜向）
        x, y = rng.randrange(32), rng.randrange(32)
        l = rng.randrange(2, 6)
        d.line([(x, y), (x + l, y + rng.choice((-1, 0, 1)))], fill=crack + (34,))
    for _ in range(5):   # 苔斑
        x, y = rng.randrange(32), rng.randrange(32)
        d.ellipse([x-1, y-1, x+1, y+1], fill=moss + (26,))
    return img
```

## 优缺点
- ✅ 五案中最中性耐看，久用不腻；明度适中，白天/夜晚/资源包适应性最好。
- ✅ "散修凿洞苦修"气质与凡人流叙事最贴。
- ⚠️ 特色相对内敛，第一眼惊艳度不如方案一/五；靠凿痕/苔斑细节撑质感。
