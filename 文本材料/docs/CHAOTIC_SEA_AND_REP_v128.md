# 乱星海精表 + 开局/声望互斥 v128

> 无代码。

## A. 乱星海势力

| 势力 | 定位 | 术法约 | 敌对 |
|------|------|--------|------|
| 星宫 | 乱星海秩序/巡海/注册 | 19 | inverse_star_alliance |
| 逆星盟 | 割据/走私/反星宫 | 13 | star_palace |
| 妙音门 | 音律摄神；名义长老线 | 6 | — |
| 乱星海中立航行 | 通用御浪 | 3 | — |

数据：`chaotic_sea_factions_packs_v128.json`

### 星宫 vs 逆星
- 图：war weight -80
- 声望硬互斥：不可同时友好+

### 妙音门
- 音律摄神；异于合欢
- 可与星宫贸易向

### 元磁
- 化神 + 星宫尊崇/双圣旗

## B. 开局名单切换

| 选择 | 宗门 |
|------|------|
| 主名单 | huangfeng_valley, yanyue_sect, spirit_beast_mountain, qingxu_gate, huadao_wu, tianque_fort, giant_sword_gate |
| 备选 | huangfeng_valley, yanyue_sect, tianfu_gate, tianque_fort, tianlan_temple, lingyun_sect, qingyan_sect |
| 交集 | huangfeng_valley, tianque_fort, yanyue_sect |
| 仅主 | giant_sword_gate, huadao_wu, qingxu_gate, spirit_beast_mountain |
| 仅备 | lingyun_sect, qingyan_sect, tianfu_gate, tianlan_temple |

- 开局必须二选一
- 中期可转名单：保留功法，可能跳槽惩罚
- 落云仍后期解锁、不占七派

## C. 声望互斥（摘要）

| 规则 | 类型 |
|------|------|
| zhengdao_vs_modao | hard_mutex_high：不可同时 ≥ friendly(30)。若一侧≥honored，另一侧强制≤un… |
| star_vs_inverse | hard_mutex_high：不可同时 ≥ friendly。星宫注册后逆星任务降星宫；反之亦然。… |
| jingu_yuan_phase | content_gate：金鼓原阶段：正道扩张线与魔道扩张线任务互斥，只能推进一条主线分支… |
| mulan_wanted | soft_penalty：慕兰声望≥exalted 且未完成通商和谈时，天南正道可能通缉（rep_zhen… |
| yanyue_vs_hehuan | soft_rivalry：掩月与合欢同源竞合：合欢任务↑魔道时掩月声望小幅↓… |
| huangfeng_vs_guiling | war_hostility：鬼灵门/魔道血战线大幅降黄枫与正道… |
| list_switch_primary_alt | opening_choice：开局二选一；中期可用「转宗/游历」消耗剧情点切换可见宗门商店，但不重置已学功法… |
| luoyun_late | late_unlock：落云不占七派；结丹+且未与正道彻底决裂可入… |
| yuanci_gate | reward_gate：元磁神光功法：需化神境界 + (星宫≥honored 或双圣剧情旗)… |

全文：`opening_list_and_reputation_rules_v128.json`
