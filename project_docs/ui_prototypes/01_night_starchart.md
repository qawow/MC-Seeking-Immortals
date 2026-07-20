# 方案一《玄夜星图》Night Star-Chart

## 气质
观星台的夜：近黑的玄青夜幕做底，银白细线如星轨勾勒边框，文字是星光的
冷白。强调色是"周天星斗"的银蓝与一点北辰金。适合夜间长时间游玩，
氛围最接近"仰观天象、推演命数"的修仙意象。

灵感语汇：星图、浑天仪、周天列宿、紫微垣、银河。

## 四场景

| 场景 | 中文 | 夜幕底 | 星线/文字 | 强调 |
|---|---|---|---|---|
| QUIET_STUDY | 观星台 | 玄青 `#101820` | 星白 `#D8E4EC` | 银蓝 `#6FA8C8` |
| FIELD_NOTES（默认） | 夜行图 | 墨蓝 `#12161E` | 月白 `#DCE0D8` | 淡银 `#9AB8C0` |
| LEDGER_HALL | 星市 | 暗紫褐 `#1A141E` | 暖星白 `#E4D8C8` | 北辰金 `#C8A050` |
| OMEN_RED | 荧惑 | 暗赤黑 `#1E1012` | 灰红白 `#E8D0C8` | 荧惑红 `#C84838` |

> 荧惑守心 = 凶兆，天文语义正好对应 OMEN 场景。

## 四场景色板（46-token，按 UiClimate.Palette 字段顺序）

### QUIET_STUDY 观星台
```
border=FF6FA8C8 borderDim=88486878 voidFill=E8080C10 panel=F2101820 inner=EE141E28 header=F00C1218
row=1E6FA8C8 rowHovered=3A6FA8C8 rowSelected=558FC0D8 rowDisabled=12304048
control=F0182430 controlHovered=F0203040 controlDisabled=DD0E141C tabSelected=F0284050
accent=FF6FA8C8 accentText=FFA8CCE0 paper=FFD8E4EC paperMuted=FF8098A8
spirit=FF7AB8C0 cinnabar=FFB04838 cinnabarBright=FFDC7050 warning=FFC8A050
barBacking=FF0C1218 barHighlight=336FA8C8 iconInset=FF283848 sealInset=FF203040
nodeEmpty=FF243440 nodeLocked=FF38505E dividerGlow=556FA8C8 scrollbarTrack=660C141C
cultivationFill=CC6FA8C8 cultivationHighlight=EE9CCCE4 paperSheen=1AFFFFFF paperWeight=44060A0E rimInner=6688B8D0
hudBorder=C86FA8C8 hudBacking=D8101820 hudInner=D0141E28 hudEdge=88486878
hudSlotFilled=C0182430 hudSlotEmpty=5E101820
hudSkillBorder=906FA8C8 hudSkillBacking=52101820 hudSkillInner=44141E28 hudSkillSlotFilled=8C182430 hudSkillSlotEmpty=38101820
material=JADE
```

### FIELD_NOTES 夜行图（默认）
```
border=FF9AB8C0 borderDim=88607880 voidFill=E80A0C10 panel=F212161E inner=EE181C26 header=F00E1016
row=1E9AB8C0 rowHovered=3A9AB8C0 rowSelected=55B0C8D0 rowDisabled=12303840
control=F01A202C controlHovered=F0242C3A controlDisabled=DD10141A tabSelected=F02A3648
accent=FF9AB8C0 accentText=FFC4D8DC paper=FFDCE0D8 paperMuted=FF8C98A0
spirit=FF7AB8C0 cinnabar=FFB04838 cinnabarBright=FFDC7050 warning=FFC8A050
barBacking=FF0E1016 barHighlight=339AB8C0 iconInset=FF2A3440 sealInset=FF222C38
nodeEmpty=FF262E38 nodeLocked=FF3A4854 dividerGlow=559AB8C0 scrollbarTrack=660C0E14
cultivationFill=CC9AB8C0 cultivationHighlight=EEC0D8DC paperSheen=1AFFFFFF paperWeight=44060810 rimInner=66A8C4CC
hud*=同上按 panel/inner 色相平移
material=BAMBOO
```

### LEDGER_HALL 星市
```
border=FFC8A050 borderDim=88907840 voidFill=E80E0A10 panel=F21A141E inner=EE201A26 header=F0120E16
row=1EC8A050 rowHovered=3AC8A050 rowSelected=55D8B868 rowDisabled=12403828
control=F0241E2C controlHovered=F0302838 controlDisabled=DD140F1A tabSelected=F03A3048
accent=FFC8A050 accentText=FFE0C890 paper=FFE4D8C8 paperMuted=FF9C8C80
（其余按同法平移；material=LACQUER）
```

### OMEN_RED 荧惑
```
border=FFC84838 borderDim=88883028 voidFill=E8100A0A panel=F21E1012 inner=EE241618 header=F0140A0C
accent=FFC84838 accentText=FFF0B8A0 paper=FFE8D0C8 paperMuted=FFA08880
（其余按同法平移；material=SEAL）
```

## 章法（与基线的差异点）
- 边框改为**双线星轨**：外 1px 亮银线 + 内 1px 暗线，四角放小星点（2×2 亮像素）。
- 分隔线用**虚线星链**（1px 亮 2px 空循环）替代实墨线。
- 计量条填充加**星闪高光**：每 16px 一个 1px 亮点。
- 面板背景纹理：稀疏星点噪声（黑底上 α≤40 的白点，密度 ~2%）。
- 印章元素改为**星官罗盘纹**（圆环 + 十字刻度）。

## 纹理生成片段（合入 generate 脚本）
```python
def starfield(base, star, density=20, seed=0):
    rng = random.Random(seed)
    img = Image.new("RGBA", (32, 32), base + (255,))
    d = ImageDraw.Draw(img)
    for _ in range(density):
        x, y = rng.randrange(32), rng.randrange(32)
        a = rng.choice((28, 40, 56))
        d.point((x, y), fill=star + (a,))
        if a == 56:  # 亮星带十字微光
            for dx, dy in ((1,0),(-1,0),(0,1),(0,-1)):
                d.point((x+dx, y+dy), fill=star + (18,))
    return img
# 四张: night_deep(16,24,32) night_field(18,22,30) night_market(26,20,30) night_omen(30,16,18)
```

## InkPaletteTest 断言替换（深色方案必改）
```java
// 深色方案：panel 是深夜幕、paper 是亮星白 —— 亮度关系与浅色基线相反
assertTrue(panelLum < 300, scene + " panel should be dark night");
assertTrue(inkLum > 480, scene + " text should be bright starlight");
assertTrue(inkLum - panelLum > 250, scene + " needs strong contrast");
```

## 优缺点
- ✅ 夜间游玩最舒适；与洞府/秘境深色环境融合最好；星象意象贴合推演/占卜文案。
- ✅ 与原《四材问道录》深色时代的 token 亮度关系一致，老截图对比违和感最小。
- ⚠️ 白天雪原等亮环境下面板对比强烈；描金/星金在小字号下需要抬高明度。
