# 缺失与占位内容清单

## 文档维护规则

后续新增或调整方块、物品、配方、掉落、Patchouli 指南、模型、贴图、交互机制时，必须同步更新：

- `project_docs/items.md`
- `project_docs/pending_requests.md`
- 对应版本更新日志：`project_docs/updates/年月日_版本号.md`

## 当前仍使用占位符的内容

- `technique_manual_*` 系列功法/术法传承物品仍有 60 个使用 `*_placeholder.png` 占位贴图，详见下方完整清单。
- 技能栏目前仍是基础占位表现；0.1.34 已接入绑定槽快捷键释放、5 秒默认冷却同步和 tooltip 冷却秒数，但仍缺正式技能图标、冷却遮罩/动效 UI 与完整技能效果结算。
- 五行灵石本次使用程序生成的 16x16 区分色占位贴图，后续应替换为正式美术资源。
- 飞剑/飞行法宝（`flying_sword`、`flying_artifact`）已用 16x16 程序占位贴图接入，后续应替换为正式美术，并补飞行粒子/音效/HUD。
- 部分 Patchouli 条目仍是玩法说明型文本，尚未补齐完整图文、配方联动和进阶引导。

## `technique_manual_*` 系列缺失正式贴图清单

> 以下条目已有可加载的占位 PNG，游戏内不会紫黑丢贴图；但都缺少正式美术贴图，需要后续替换对应 `*_placeholder.png` 或调整模型指向正式纹理。

| 序号 | 物品 ID | 显示名 | 当前占位贴图 | 模型文件 |
| --- | --- | --- | --- | --- |
| 1 | `technique_manual_ancient_demon` | 古魔传承卷轴 | `technique_manual_ancient_demon_placeholder.png` | `technique_manual_ancient_demon.json` |
| 2 | `technique_manual_ancient_demon_secret` | 古魔秘术传承卷轴 | `technique_manual_ancient_demon_secret_placeholder.png` | `technique_manual_ancient_demon_secret.json` |
| 3 | `technique_manual_ancient_demonic_skill` | 上古魔功传承卷轴 | `technique_manual_ancient_demonic_skill_placeholder.png` | `technique_manual_ancient_demonic_skill.json` |
| 4 | `technique_manual_ancient_secret_art` | 上古秘术传承卷轴 | `technique_manual_ancient_secret_art_placeholder.png` | `technique_manual_ancient_secret_art.json` |
| 5 | `technique_manual_ancient_sword_sect` | 古剑门传承卷轴 | `technique_manual_ancient_sword_sect_placeholder.png` | `technique_manual_ancient_sword_sect.json` |
| 6 | `technique_manual_azure_origin_sword_art` | 青元剑诀传承卷轴 | `technique_manual_azure_origin_sword_art_placeholder.png` | `technique_manual_azure_origin_sword_art.json` |
| 7 | `technique_manual_azure_origin_sword_derivative` | 青元剑诀衍生传承卷轴 | `technique_manual_azure_origin_sword_derivative_placeholder.png` | `technique_manual_azure_origin_sword_derivative.json` |
| 8 | `technique_manual_azure_origin_sword_spirit_realm` | 青元剑诀·灵界篇传承卷轴 | `technique_manual_azure_origin_sword_spirit_realm_placeholder.png` | `technique_manual_azure_origin_sword_spirit_realm.json` |
| 9 | `technique_manual_azure_origin_sword_spirit_realm_pre` | 青元剑诀·灵界篇前置传承卷轴 | `technique_manual_azure_origin_sword_spirit_realm_pre_placeholder.png` | `technique_manual_azure_origin_sword_spirit_realm_pre.json` |
| 10 | `technique_manual_azure_origin_sword_support` | 青元剑诀辅助传承卷轴 | `technique_manual_azure_origin_sword_support_placeholder.png` | `technique_manual_azure_origin_sword_support.json` |
| 11 | `technique_manual_azure_origin_sword_support_skill` | 青元剑诀辅助功法传承卷轴 | `technique_manual_azure_origin_sword_support_skill_placeholder.png` | `technique_manual_azure_origin_sword_support_skill.json` |
| 12 | `technique_manual_azure_sea_true_lord_skill` | 碧海真君成名功法传承卷轴 | `technique_manual_azure_sea_true_lord_skill_placeholder.png` | `technique_manual_azure_sea_true_lord_skill.json` |
| 13 | `technique_manual_black_wind_flag_spirit` | 黑风旗器灵传承卷轴 | `technique_manual_black_wind_flag_spirit_placeholder.png` | `technique_manual_black_wind_flag_spirit.json` |
| 14 | `technique_manual_brahma_sacred_fragment` | 梵圣真片传承卷轴 | `technique_manual_brahma_sacred_fragment_placeholder.png` | `technique_manual_brahma_sacred_fragment.json` |
| 15 | `technique_manual_buddhist` | 佛门传承卷轴 | `technique_manual_buddhist_placeholder.png` | `technique_manual_buddhist.json` |
| 16 | `technique_manual_chaotic_star_sea_demonic` | 乱星海魔修传承卷轴 | `technique_manual_chaotic_star_sea_demonic_placeholder.png` | `technique_manual_chaotic_star_sea_demonic.json` |
| 17 | `technique_manual_chaotic_star_sea` | 乱星海传承卷轴 | `technique_manual_chaotic_star_sea_placeholder.png` | `technique_manual_chaotic_star_sea.json` |
| 18 | `technique_manual_common_low` | 通用低阶传承卷轴 | `technique_manual_common_low_placeholder.png` | `technique_manual_common_low.json` |
| 19 | `technique_manual_common` | 通用传承卷轴 | `technique_manual_common_placeholder.png` | `technique_manual_common.json` |
| 20 | `technique_manual_common_tricks` | 通用小技巧传承卷轴 | `technique_manual_common_tricks_placeholder.png` | `technique_manual_common_tricks.json` |
| 21 | `technique_manual_demon_domain_body_refining` | 魔域顶级炼体功传承卷轴 | `technique_manual_demon_domain_body_refining_placeholder.png` | `technique_manual_demon_domain_body_refining.json` |
| 22 | `technique_manual_demon_race_secret` | 魔族秘传传承卷轴 | `technique_manual_demon_race_secret_placeholder.png` | `technique_manual_demon_race_secret.json` |
| 23 | `technique_manual_demonic` | 魔道传承卷轴 | `technique_manual_demonic_placeholder.png` | `technique_manual_demonic.json` |
| 24 | `technique_manual_evergreen_appendix` | 长春功附载传承卷轴 | `technique_manual_evergreen_appendix_placeholder.png` | `technique_manual_evergreen_appendix.json` |
| 25 | `technique_manual_five_elements_escape` | 五行遁术传承卷轴 | `technique_manual_five_elements_escape_placeholder.png` | `technique_manual_five_elements_escape.json` |
| 26 | `technique_manual_formation` | 阵法类传承卷轴 | `technique_manual_formation_placeholder.png` | `technique_manual_formation.json` |
| 27 | `technique_manual_ghost` | 鬼道传承卷轴 | `technique_manual_ghost_placeholder.png` | `technique_manual_ghost.json` |
| 28 | `technique_manual_gold_magnetic_spirit_wood` | 金磁灵木传承卷轴 | `technique_manual_gold_magnetic_spirit_wood_placeholder.png` | `technique_manual_gold_magnetic_spirit_wood.json` |
| 29 | `technique_manual_gray_immortal_heritage` | 灰仙传承传承卷轴 | `technique_manual_gray_immortal_heritage_placeholder.png` | `technique_manual_gray_immortal_heritage.json` |
| 30 | `technique_manual_great_development_formula` | 大衍诀传承卷轴 | `technique_manual_great_development_formula_placeholder.png` | `technique_manual_great_development_formula.json` |
| 31 | `technique_manual_great_development_master` | 大衍神君传承卷轴 | `technique_manual_great_development_master_placeholder.png` | `technique_manual_great_development_master.json` |
| 32 | `technique_manual_great_jin` | 大晋传承卷轴 | `technique_manual_great_jin_placeholder.png` | `technique_manual_great_jin.json` |
| 33 | `technique_manual_han_li_self_created` | 韩立自创传承卷轴 | `technique_manual_han_li_self_created_placeholder.png` | `technique_manual_han_li_self_created.json` |
| 34 | `technique_manual_heavenly_lan_temple` | 天澜圣殿传承卷轴 | `technique_manual_heavenly_lan_temple_placeholder.png` | `technique_manual_heavenly_lan_temple.json` |
| 35 | `technique_manual_immortal_realm_skill` | 仙界功法传承卷轴 | `technique_manual_immortal_realm_skill_placeholder.png` | `technique_manual_immortal_realm_skill.json` |
| 36 | `technique_manual_immortal_thunder_origin` | 仙界雷法本源传承卷轴 | `technique_manual_immortal_thunder_origin_placeholder.png` | `technique_manual_immortal_thunder_origin.json` |
| 37 | `technique_manual_kunpeng_red_cloud_created` | 鲲鹏族红云老祖所创传承卷轴 | `technique_manual_kunpeng_red_cloud_created_placeholder.png` | `technique_manual_kunpeng_red_cloud_created.json` |
| 38 | `technique_manual_little_pole_palace` | 小极宫传承卷轴 | `technique_manual_little_pole_palace_placeholder.png` | `technique_manual_little_pole_palace.json` |
| 39 | `technique_manual_lost_true_immortal_art` | 上古失传真仙术传承卷轴 | `technique_manual_lost_true_immortal_art_placeholder.png` | `technique_manual_lost_true_immortal_art.json` |
| 40 | `technique_manual_mortal_martial` | 世俗武林传承卷轴 | `technique_manual_mortal_martial_placeholder.png` | `technique_manual_mortal_martial.json` |
| 41 | `technique_manual_mystic_herder_nascent_appendix` | 玄牧化婴附属传承卷轴 | `technique_manual_mystic_herder_nascent_appendix_placeholder.png` | `technique_manual_mystic_herder_nascent_appendix.json` |
| 42 | `technique_manual_mystic_yin_appendix` | 玄阴经附属传承卷轴 | `technique_manual_mystic_yin_appendix_placeholder.png` | `technique_manual_mystic_yin_appendix.json` |
| 43 | `technique_manual_nangong_wan_main` | 南宫婉主修传承卷轴 | `technique_manual_nangong_wan_main_placeholder.png` | `technique_manual_nangong_wan_main.json` |
| 44 | `technique_manual_nascent_soul_common` | 元婴修士通用传承卷轴 | `technique_manual_nascent_soul_common_placeholder.png` | `technique_manual_nascent_soul_common.json` |
| 45 | `technique_manual_nascent_soul_late_plus` | 元婴后期以上传承卷轴 | `technique_manual_nascent_soul_late_plus_placeholder.png` | `technique_manual_nascent_soul_late_plus.json` |
| 46 | `technique_manual_orthodox` | 正道传承卷轴 | `technique_manual_orthodox_placeholder.png` | `technique_manual_orthodox.json` |
| 47 | `technique_manual_purple_luo_mystic_skill` | 紫罗玄功传承卷轴 | `technique_manual_purple_luo_mystic_skill_placeholder.png` | `technique_manual_purple_luo_mystic_skill.json` |
| 48 | `technique_manual_self_created` | 自创传承卷轴 | `technique_manual_self_created_placeholder.png` | `technique_manual_self_created.json` |
| 49 | `technique_manual_seven_mysteries_sect` | 七玄门传承卷轴 | `technique_manual_seven_mysteries_sect_placeholder.png` | `technique_manual_seven_mysteries_sect.json` |
| 50 | `technique_manual_six_paths_sage_created` | 六道极圣所创传承卷轴 | `technique_manual_six_paths_sage_created_placeholder.png` | `technique_manual_six_paths_sage_created.json` |
| 51 | `technique_manual_spirit_taming_basic` | 御灵宗基础传承卷轴 | `technique_manual_spirit_taming_basic_placeholder.png` | `technique_manual_spirit_taming_basic.json` |
| 52 | `technique_manual_spirit_taming_sect` | 御灵宗传承卷轴 | `technique_manual_spirit_taming_sect_placeholder.png` | `technique_manual_spirit_taming_sect.json` |
| 53 | `technique_manual_supreme_demonic` | 魔道无上传承卷轴 | `technique_manual_supreme_demonic_placeholder.png` | `technique_manual_supreme_demonic.json` |
| 54 | `technique_manual_thousand_bamboo_heritage` | 千竹教传承传承卷轴 | `technique_manual_thousand_bamboo_heritage_placeholder.png` | `technique_manual_thousand_bamboo_heritage.json` |
| 55 | `technique_manual_thousand_illusion_sect` | 千幻宗传承卷轴 | `technique_manual_thousand_illusion_sect_placeholder.png` | `technique_manual_thousand_illusion_sect.json` |
| 56 | `technique_manual_top_demonic` | 魔道顶阶传承卷轴 | `technique_manual_top_demonic_placeholder.png` | `technique_manual_top_demonic.json` |
| 57 | `technique_manual_true_word_sect_heritage` | 真言门传承传承卷轴 | `technique_manual_true_word_sect_heritage_placeholder.png` | `technique_manual_true_word_sect_heritage.json` |
| 58 | `technique_manual_yao_bird_cultivator` | 妖族禽修传承卷轴 | `technique_manual_yao_bird_cultivator_placeholder.png` | `technique_manual_yao_bird_cultivator.json` |
| 59 | `technique_manual_yao` | 妖族传承卷轴 | `technique_manual_yao_placeholder.png` | `technique_manual_yao.json` |
| 60 | `technique_manual_yuancha_saint_ancestor` | 元刹圣祖传承卷轴 | `technique_manual_yuancha_saint_ancestor_placeholder.png` | `technique_manual_yuancha_saint_ancestor.json` |

## 当前缺失或待补齐内容

- 手动突破流程已接入破境丹/药力、成功晋阶、失败回退与走火风险；成功率已接入丹药、灵脉/灵眼、功法品质和执念加成。真实“地灵之眼”方块/结构、更多品质丹药注册/配方、功法 JSON `quality` 全量标注、按境界材料表、闭关/环境加成、金丹品质、天劫/五衰/斩三尸仍待设计。
- 六大核心属性已进入 Capability/NBT、同步和基础展示；神识探测、肉身生命加成、走火随机事件、渡劫伤害抵抗仍待接入具体玩法公式。
- 五行灵石的自然生成、矿脉差异、怪物/秘境掉落规则待设计。
- 变异/隐藏灵根与五行灵石增幅的映射规则待设计，例如雷、冰、风、暗、隐雷、隐暗、仙灵根。
- 新增方块/物品后的模型、贴图、语言、创造栏、配方、掉落、指南与文档需要保持同步。

## 后续要求

新增方块或物品时，至少检查并同步：注册代码、语言文件、模型文件、贴图文件或占位说明、合成配方、掉落表、创造模式物品栏、Patchouli 指南、`items.md`、`pending_requests.md` 与版本更新日志。

## 灵气系统占位/后续扩展（0.1.22）

- 灵脉第一版为隐藏区块算法，不生成可见灵脉地形、洞府遗迹或专属矿脉。
- 寻脉罗盘第一版使用文字方向和距离提示，不做客户端动态指针/模型旋转。
- 秘境/仙府 10x 灵气已预留维度名判定，但尚未实装独立秘境/仙府维度与危险事件。
- 下界炼体、末地法则感悟当前仅作为灵气性质标记，尚未接入独立炼体经验、法则感悟或专属突破公式。
- 测灵盘、寻脉罗盘、聚灵阵当前使用程序生成 16x16 占位贴图，后续应替换为正式美术资源。

## 0.1.27 UI 复审备注

- 本次未新增贴图资源；技能栏图标仍为基于技能 ID 的原生 UI 占位色块，后续需要替换为正式技能图标。
- 修仙页、技能栏、吐纳 HUD 已统一为 Forge Screen/Overlay + 原生 UI 绘制工具，后续重点是正式图标、冷却与交互。

## 0.1.28 UI 复审备注

- 本次仍未新增贴图资源；技能栏图标继续使用基于技能 ID 的占位色块。
- 已修复修仙页面板在极窄屏/高 GUI 缩放下的宽度与状态条越界风险，但小屏、宽屏、多 GUI Scale、JEI/XEI 同屏仍需游戏内人工验证。
- 最终样式不在代码中硬定，后续应按用户选择继续优化原生分页、外置主题资源或独立 Screen 布局。

## 0.1.29 UI 复审备注

- 本次未新增贴图资源；技能栏图标仍为基于技能 ID 的占位色块，后续需要正式技能图标与冷却/释放态 UI。
- 修仙页已按 B 方案改为独立全屏/居中 Screen，并已在 0.1.32 改为 Forge Screen + 原生 UI 绘制工具。
- 技能栏左侧 7 槽与独立面板在多分辨率、不同 GUI Scale、JEI/XEI 同屏场景仍需游戏内人工验证。


## 0.1.30 UI 复审备注

- 本次新增 `skill_bar_frame.png`，由用户上传 JPG 自动裁剪、缩放并尝试透明化棋盘格背景生成。由于源图本身为带棋盘格背景的 JPG，边缘可能仍存在轻微压缩/抠图瑕疵，后续可由正式透明 PNG 美术资源替换。
- 技能栏图标仍为基于技能 ID 的占位色块，尚未接入正式技能图标、冷却 UI、快捷键释放与完整效果结算。

## 0.1.31 UI 复审备注

- 新增 `textures/gui/cultivation_progress_bar.png` 由用户 JPG 裁剪并边缘透明化生成；由于源图是 JPG 且带棋盘格背景，抗锯齿边缘可能仍有轻微浅色残留，后续若有原始透明 PNG 可替换。
- `textures/gui/skill_bar_frame.png` 资源仍保留在包内作为历史素材，但 0.1.31 不再绘制，避免玩家左上角出现额外图案。
- 技能图标仍为基于技能 ID 的占位色块，未接入正式技能图标、冷却 UI、快捷键释放与完整服务端校验。

## 0.1.32 UI 复审备注

- 旧第三方 UI 依赖和兼容层已移除，当前界面仅使用原生 Forge/Minecraft 渲染。
- `skill_bar_frame.png` 仍作为未绘制历史素材保留；如不再需要，可在后续资源清理中删除。
- 技能图标仍为基于技能 ID 的占位色块，未接入正式技能图标、冷却 UI、快捷键释放与完整服务端校验。


## 0.1.33 新增占位

- 技能释放快捷键已接入服务端校验和灵力消耗，但释放效果仍为占位聊天提示，尚未实现真实伤害、增益、冷却和目标选择。
- `TechniqueEditScreen` 目前只展示 7 槽和已学技能列表，点击/拖拽绑定、槽位持久化与网络同步仍待实现。

## 0.1.34 技能数据/冷却备注

- 7 个技能槽绑定和技能冷却已经进入玩家修炼数据与网络同步；旧存档无 `TechniqueSlots` 时会按已学技能排序填充前 7 槽。
- 第二阶段已补齐技能编辑界面拖拽绑定、右键清空、HUD 上移和进度条按进度裁剪；仍缺正式技能图标、冷却遮罩/释放动效和完整技能效果反馈。
- 技能释放仍是占位成功提示，没有接入真实伤害、治疗、位移、阵法等效果结算。

## 0.1.34 第三阶段 UI 复审备注

- `textures/gui/cultivation_progress_bar.png` 已用用户新上传 PNG 重新生成；本次按整条基底裁剪并透明化上传预览背景，保留 PNG 透明通道，不再按 JPG 棋盘格素材处理。
- HUD 进度条填充层为代码绘制的青绿色半透明矩形/高光，后续如有正式“已填充态”美术，可再拆成独立填充纹理替换。
- 修仙属性面板和 HUD 已补 GUI Scale/小窗口边界钳制，但仍需要在真实游戏中按 GUI Scale 1/2/3/Auto、不同分辨率和资源包组合做人工视觉验证。
