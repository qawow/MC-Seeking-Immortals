# NPC 与灵界秘境设计

## NPC

`npc_dialogue_templates.json` 提供 **11 种原型**：

- 宗门：炼丹长老、贡献执事  
- 乱星海：征税吏、黑市掮客  
- 前线：慕兰法士长、天渊校尉  
- 灵界：风元拍卖师  
- 通用：药贩、傀儡匠人、秘境引路人  

每条含 `shop_ref` / `quest_offers` / `reputation_gate` 等模组键。

## 灵界秘境（§13 四要素）

已在 `secret_realm_template.json` 补全：

| 秘境 | 境界 | 要点 |
|---|---|---|
| 地渊 | 炼虚+ | 五层、古兽、空间碎片 |
| 广寒界 | 化神+ | 月华、周期碎片 |
| 魔金山 | 炼虚+ | 魔气潮汐 |
| 小修罗界 | 合体+ | 杀意试炼 |
| 九仙山 | 大乘+ | 九峰闭关 |

`secret_realms.json` 新增 **地渊**、**九仙山秘境** 条目。

## 任务挂钩

- `diyuan_scout` / `guanghan_moon_hunt` / `asura_trial`（见 `quest_hooks.json`）

## 合规

引路人台词用通用描述；掉落物为原创 id，非原著专属法宝名。