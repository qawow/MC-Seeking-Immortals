# 各模块扩充 v43

> 汇编 **§9.7** 星宫/逆星盟 · **§13** 虚天殿四要素 · §18 结构优先

## 乱星海势力与节点

| 势力 | 节点 | 专精 |
|------|------|------|
| 星宫 | `node_star_palace`（天星城阵枢） | 镇海、拍卖监管、巡海 |
| 逆星盟 | `node_inverse_star_hideout`（暗港） | 走私、秘传、策应海盗 |
| 散修海盗 | — | 黑市劫掠 |

- `chaotic_sea_factions.json` v2
- 地区卡：`star_palace_city`、`inverse_star_hideout`
- 空间节点 +6（含虚天殿周期门、天南边境、掩月/清虚阵）
- 冲突事件：巡海遇袭、虚天殿周期开启

## 虚天殿秘境

`secret_realm_template.json#void_palace` 四要素 + `cycle_void_palace`  
掉落：层箱、寒玉挂坠、虚天钥碎片（通用名，非主角专属古宝绑定）

## 任务钩 +3

星宫泊船许可、逆星走私、虚天钥残片

## 打包与索引

- **`seeking_immortals_lore_v43.zip`**：`data/` + `docs/`
- `data_manifest.json` v2：校验字段 + 增补文件列表
- `item_id_index.json` 挂 `manifest_ref`

## 节点总数

见 `spatial_nodes_catalog.json`（v2，含乱星海完整链）

## 下一步

- 海盗黑市 `pirate_scatter` 商店表
- 星宫功法 `star_palace_art` 术法门控
- NeoForge 侧读取 `data_manifest` 加载器