# 方案二《青铜鼎纹》Bronze Tripod

## 气质
上古鼎彝：深锈青做底，错金细线做框，文字如鼎内铭文的暖白。强调色是
铜锈青绿与错金。厚重、庙堂、上古传承感——和宗门大殿、坊市宝库、
炼器玩法气质最合。

灵感语汇：饕餮纹、云雷纹、错金银、铭文、彝器、绿锈。

## 四场景

| 场景 | 中文 | 鼎底 | 铭文/文字 | 强调 |
|---|---|---|---|---|
| QUIET_STUDY | 丹鼎 | 深锈青 `#16201C` | 暖铭白 `#E0E0D0` | 锈青 `#5A9078` |
| FIELD_NOTES（默认） | 行彝 | 深褐青 `#1A1C16` | 米铭白 `#E2DEC8` | 铜绿 `#6E9060` |
| LEDGER_HALL | 宝库 | 深棕 `#201814` | 暖金白 `#ECDEC0` | 错金 `#C89848` |
| OMEN_RED | 血祭 | 锈黑红 `#1E1210` | 灰红白 `#E8D4C8` | 祭红 `#B44030` |

## 四场景色板要点（46-token 生成规则）

以「深底 ~#161C18 系 / 亮字 ~#E0DEC8 系 / 场景强调色」三元组按下式展开：

```
border      = FF + 强调色
borderDim   = 88 + 强调色暗 30%
voidFill    = E8 + 底色暗 40%
panel       = F2 + 底色
inner       = EE + 底色亮 8%
header      = F0 + 底色暗 15%
row         = 1E + 强调色       rowHovered = 3A + 强调色
rowSelected = 55 + 强调色亮 15%  rowDisabled = 12 + 中灰
control     = F0 + 底色亮 12%    controlHovered = +20%  controlDisabled = DD 底色暗
tabSelected = F0 + 底色亮 25%
accent      = FF 强调色          accentText = FF 强调色亮 35%
paper       = FF 文字色          paperMuted = FF 文字色暗 35%
spirit=FF6E9C94  cinnabar=FFB44030  cinnabarBright=FFDC6848  warning=FFC89848（跨场景恒定）
bar/node/divider/scrollbar/cultivation/hud* 组按基线同比例平移
```

具体数值试装时由脚本一键展开（见下方 palette 展开脚本）。

## 章法（差异点）
- 边框：**回纹角饰**——四角画 3×3 的 L 形回纹（云雷纹最小单元），边线 1px 错金色。
- 标题条：仿铭文带，底色更深一档，文字用 accentText（错金）。
- 分隔线：双细线夹 1px 空隙（仿范线）。
- 计量条：填充加锈斑颗粒（α 噪声）；满值时两端亮起错金点。
- 印章元素改为**饕餮衔环**简形（圆环 + 上方两点目纹）。

## 纹理生成片段
```python
def bronze(base, patina, gold, seed=0):
    rng = random.Random(seed)
    img = Image.new("RGBA", (32, 32), base + (255,))
    d = ImageDraw.Draw(img)
    for _ in range(40):  # 锈斑
        x, y = rng.randrange(32), rng.randrange(32)
        r = rng.randrange(1, 3)
        d.ellipse([x-r, y-r, x+r, y+r], fill=patina + (rng.choice((20, 30)),))
    for _ in range(6):   # 错金细屑
        x, y = rng.randrange(32), rng.randrange(32)
        d.point((x, y), fill=gold + (48,))
    return img
```

## palette 展开脚本思路
在 `scripts/` 放 `expand_palette.py`：输入（底色, 文字色, 强调色）三元组 ×4 场景，
按上面的规则表输出 46-token Java 常量块，直接粘进 `InkScene.java`。
（方案一/三/四/五同样适用此脚本，试装成本进一步降到"填 12 个色值"。）

## InkPaletteTest 断言
深色方案，同方案一的反转断言。

## 优缺点
- ✅ 最厚重的"古物感"；炼器/坊市/宗门场景氛围极佳；错金强调醒目而不艳。
- ✅ 回纹/饕餮元素都是程序可画的简形，无需美术。
- ⚠️ 整体偏暗偏绿，长时间阅读密集文字略压抑——建议正文字色取暖白而非冷白。
- ⚠️ 与"凡人散修"的寒酸气质略有出入（更像大宗门/皇朝），看剧情侧重取舍。
