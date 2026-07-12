# 各模块扩充 v48

> 汇编 **§13** 秘境四要素 · **§9** 势力专精 · 通用 archetype 命名

## 突兀族 ↔ 慕兰世仇

- `wutu_mulan_feud.json`：袭营、反猎、调停商路
- 地区 **`wutu_border.json`**，`mulan` 挂 `feud_ref`
- 任务链 **`wutu_mulan_feud_line`**
- 与 `mulan_tianlan_war` 边境阶段可联动

## 坠魔谷

- `secret_realms.json` + `secret_realm_template` 四要素
- 开放：封印削弱 + 元婴 + `node_fallen_demon_rift`
- 环境：魔气、心魔、裂隙
- 链 **`fallen_demon_expedition`**

## 大晋商路

- `trade_routes.json`：**`dajin_wanbao_spine`**
- 地区卡 **`dajin.json`**（炼器/拍卖/世家政治）
- 链 **`dajin_wanbao_route`**

## 阴罗殿

- `yin_luo_hall.json` + `yin_underworld_cluster` v2
- 功法 **阴罗鬼诀**（鬼修路径前置）
- 链 **`yin_luo_ghost_sect`**

## 打包

**`seeking_immortals_lore_v48.zip`**

## 下一步

- 昆吾山秘境四要素
- 阴罗殿术法 `techniques/ghost.json` 门控补全
- MC 1.20.1 ReloadListener 版本对齐