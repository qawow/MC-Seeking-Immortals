# 各模块扩充 v49

> 汇编 **§13** 四要素 · **§18** 功法→法术→神通 · 通用昆吾/阴罗命名

## 昆吾山

| 要素 | 设定 |
|------|------|
| 开放 | `cycle_kunwu_open` + 大晋许可/图碎片 + 元婴+ |
| 环境 | 极寒、禁制、傀儡、金灵压 |
| 分层 | 外阵→矿脉→傀儡殿→封印峰 |
| 掉落 | 层箱、傀儡核、封印碎片、昆吾矿 |

- `secret_realms.json` v3、`kunwu.json` 地区卡
- 任务链 **`kunwu_mountain_expedition`**

## 阴罗殿术法 `ghost.json`

- 新门派文件 **5** 术：`yin_soul_devour`、`soul_banner_wave`、`yin_luo_ghost_cloak`、`ghost_king_avatar`（secret）、`nether_ghost_walk`（冥河诀）
- 门控：`path_required` + `requires_method`（阴罗鬼诀 / 冥河鬼修诀）
- `ghost_talisman` 挂阴罗鬼诀
- 技能树 **`yin_luo_ghost`**
- `techniques/index.json` v5，总数刷新

## MC 1.20.1 ReloadListener

- `LorePackReloadListener` 实现 **`PreparableReloadListener`**（避免 `SimplePreparableReloadListener(null,null)`）
- `data_manifest.minecraft_target`: `1.20.1-forge-47`

## 打包

**`seeking_immortals_lore_v49.zip`**

## 下一步

- 天渊城 / 灵界边境（汇编 9.7）
- 鬼修与正道宗门 `sect_ban` 任务后果
- 全秘境 `secret_realm_template` 补全率检查