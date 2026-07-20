# 方案五《水墨山水》Ink-Wash Landscape

## 气质
青绿山水手卷：极淡的天青纸底上，远山淡影做面板底纹，石青石绿点染强调。
五案中最"文人画"、最接近国风大作的一套——代价是纹理最讲究，需要
多层晕染而非简单噪点。

灵感语汇：千里江山、青绿设色、留白、远山如黛、烟波。

## 四场景

| 场景 | 中文 | 天青底 | 墨字 | 设色强调 |
|---|---|---|---|---|
| QUIET_STUDY | 卧游 | 淡天青 `#E0EAE6` | 黛墨 `#28322E` | 石绿 `#4A8868` |
| FIELD_NOTES（默认） | 远行 | 暖绢白 `#EAE8DA` | 常墨 `#302E26` | 石青 `#3E6A88` |
| LEDGER_HALL | 市桥 | 秋绢 `#E8DFC2` | 褐墨 `#342E20` | 赭石 `#A87848` |
| OMEN_RED | 惊涛 | 灰绢 `#E6DED6` | 焦墨 `#342822` | 绛红 `#A83C30` |

浅底深字，InkPaletteTest 现行断言可直接复用。

## 色板展开
`expand_palette.py` 三元组如上表。额外规则：
- `voidFill`（卷外）用更深的青灰 `#C8 1A2420` 系——像卷轴之外的裱边。
- `paperSheen` 抬到 `0x30FFFFFF`：绢面比纸面更亮泽。
- `cultivationFill` 用石绿→石青的场景对应色（修为条如青山层染）。

## 章法（差异点，五案中最讲究）
- 面板底部 1/4 高度画**远山剪影带**：两层山形折线（后层 α12、前层 α20，
  场景强调色），程序生成固定 seed。
- 边框：无硬线——用**裱边**替代：面板外扩 2px 的深青灰带（voidFill 色），
  内侧 1px 绢光。整体像一幅装裱的册页。
- 标题：左上角**题跋区**（竖排感用短横线模拟界格），右上角一枚小朱印。
- 分隔："烟波线"——1px 主线 + 下方错位 1px α30 副线（水纹错觉）。
- 计量条：如"层峦渐染"，填充色从 60% 到 100% 做两段阶梯（非渐变，保持像素感）。

## 纹理生成片段
```python
def silk_wash(base, wash, seed=0):
    rng = random.Random(seed)
    img = Image.new("RGBA", (32, 32), base + (255,))
    d = ImageDraw.Draw(img)
    # 绢纹（正交细网）
    for i in range(0, 32, 4):
        d.line([(i, 0), (i, 31)], fill=wash + (10,))
        d.line([(0, i), (31, i)], fill=wash + (8,))
    # 两三团淡晕
    for _ in range(3):
        x, y = rng.randrange(32), rng.randrange(32)
        r = rng.randrange(4, 9)
        d.ellipse([x-r, y-r, x+r, y+r], fill=wash + (12,))
    return img

def mountain_band(width, height, color, seed=1):
    """远山剪影带：供 InkBrush 侧参考的两层折线算法（也可离线烘焙成 PNG）。"""
    rng = random.Random(seed)
    pts = [(0, height)]
    x = 0
    while x < width:
        x += rng.randrange(6, 14)
        pts.append((min(x, width), rng.randrange(height // 3, height - 2)))
    pts.append((width, height))
    return pts  # fill polygon with alpha 12/20 twice, second layer seed+1
```

## 实现提示
- 远山带建议实现为 `InkBrush.drawMountainBand(graphics, x, y, w, h, seed)`
  程序绘制（确定性 seed = 面板宽高 hash），不必出 PNG。
- 若嫌工作量大，可先"半装"：只换色板 + 绢纹纹理，远山带/裱边二期再加——
  半装成本与方案四相同。

## 优缺点
- ✅ 观感上限最高，最有"作品感"；青绿设色与修仙"山水洞天"意象天然契合。
- ✅ 浅底断言零改动。
- ⚠️ 章法元素（远山带/裱边/题跋）是五案中唯一需要新增 InkBrush 绘制函数的，
  完整落地约为其他方案 2 倍工作量。
- ⚠️ 大量淡色叠淡色，低分辨率/高 GUI scale 下细节容易糊，需实机验证。
