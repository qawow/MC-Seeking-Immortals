## 0.2.46 丹药语义与后续交接

本批未新增占位模型、纹理、物品/方块/实体/网络 id；新增兼容的 capability 替死布尔值、一个独立死亡事件消费者、65 组双语 tooltip 和目录合并运行时。114 条加载后的丹药效果均可执行且不再保留 `generic_cultivation`，境界上下限/目标、固定灵力、效果标签、元素、风险、流派与时长已进入消费端。寿元标记时序与替死最低优先级已有自动化契约。定向 13 项与普通全量构建 681 项通过，协议保持 25。

仍未完成：若干丹药目前共享类别级实现，尚未逐条还原设计说明中的独特代价、概率、阵营/地域条件和长期后果；替死、雷劫、寿元、灵兽成长及跨模组死亡取消顺序仍需客户端、专服和多人实机烟测。`detox_minor_pill`、`talisman_ink_bottle`、`spirit_sand_pouch`、`yin_coffin_nail`、`wind_feather_raft_blueprint` 仍需独立闭环。功法依旧固定 9 层，尚未消费 `total_layers/realm_band/prerequisites`；术法目录的 `damage_base/effect_key/tags/setting.target/setting.range` 尚未全面结构化执行；多方块结构仍需逐个核对形成、持续有效性、消耗、失败回滚、权限和实机行为。符箓事务、法宝倍率、储物持续授权、持久交付 outbox 与真实客户端/专服/多人烟测继续保留。

## 0.2.43 批量物品、功法与术法遗留

本批未新增占位模型、纹理、实体、方块、网络包或持久 schema；复用现有目录、储物菜单、投射物、状态和双语 tooltip。已完成批量物品类型分派、36 个目录消耗品、9/18/27 格便携储物、精确丹药 ID、炼丹方介质、材料法宝隔离，以及避雷/天劫/种植器若干事务修复。普通构建通过 676 项测试，协议保持 25。

明确未完成：`talisman_ink_bottle` 与 `spirit_sand_pouch` 仍是材料载体；`yin_coffin_nail` 没有鬼道消费闭环；`wind_feather_raft_blueprint` 无法转换为可旅行票；大量丹药仍以 `generic_cultivation` 冒充独立效果，宠物成长、魔气、雷劫、寿元债、忘尘和定颜等键缺真实消费者；术法的 `damage_base/effect_key/tags/target/range` 多数未进入运行时；功法总层数仍固定 9，与 13 层资源冲突；符箓效果失败时尚无完整退款事务；法宝 `powerScale` 未贯穿实际伤害、范围和状态；储物菜单持续授权 TOCTOU、满背包/掉落失败 outbox、灵田产物交付失败回滚仍待处理。另需真实客户端、专服和多人烟测。

## 0.2.39 拍卖原子扣款遗留

本批仅收口拍卖出价余额不足时的部分吞款：服务端按对象身份去重并快照后再扣除，负数或余额不足不修改任何栈，足额精确跨格扣除。未新增目录或持久 schema。仍未实现拍卖 winner/refund 持久 outbox、崩溃恢复与幂等交付；离线退款当前仍先删账再发货，中奖物仍先交付再标 settled。`auction_ancient_artifact_shard` 等无法解析奖励仍可能模糊回退错误物品。QuestTracker 的未启动推进、current-step 精确 hook、同分支声望一次性和不可伪造 NPC 会话也未在本批处理。0.2.38 中间普通构建通过 667 项；0.2.39 定向 6 项及最终普通构建通过，全量 669 项无失败、错误或跳过，协议保持 25。

## 0.2.37 菜单现场权限遗留

本批未新增注册物品、方块、实体类型、菜单类型、模型、纹理或目录条目；新增一个服务端菜单授权上下文、三个 C2S `long accessToken` 字段、兼容缺省的 NPC 对话 `SourceEntityId` 和一条双语失效提示。无实体对话使用玩家坐标锚点，命名宗门对话与大厅入口复用同一他宗门禁。自动化覆盖令牌往返、当前菜单、原始锚点、shop/sect 目标、宗门成员 focus、命名村民、变更命令 permission 2、31 条网络方向、市场单店和宗门不重开；0.2.37 普通 `./gradlew build` 已通过，665 项测试无失败、错误或跳过。仍需真实客户端/专服/多人烟测 NPC 行走/卸载、8 格边界、跨维瞬间、Esc 延迟包、连续重开、双击购买和协议 24 拒绝。菜单内同一合法会话的重复请求仍由各业务自身承担幂等性。Max 审计确认未完成 P0/P1：QuestTracker 未启动/当前 step/分支/NPC 状态机；拍卖余额不足部分吞款、共享/个人 escrow、奖励先发后 settled、离线退款先删后交付；同区旅行声望重放；鉴定失败扣费；Method 总层数/境界/前置。持久交付 outbox、拍卖 venue/请求幂等、Curios 飞行归属和实机烟测继续列为后续。

## 0.2.34 秘境奖励与捕捉权限遗留

本批未新增注册物品、方块、实体类型、模型、纹理、菜单或网络包；新增兼容的秘境 SessionId/encounter claim SavedData 字段和一个复用原版箱子的 NBT 奖励托管服务。自动化覆盖会话顺序、旧 SessionId 稳定迁移、跨 session 防重放、击杀/任务 hook 提交顺序、空原版库存托管、捕捉生态白名单、残血候选优先与 35% 门槛；全量 651 项测试和最终普通 `./gradlew build` 已通过。仍需真实客户端/专服/多人烟测同维相邻秘境、另一玩家及其投射物/侍灵、重启中途会话、箱子双击/双手、漏斗/活塞/爆炸、满背包世界掉落和断线时序。0.2.34 前已生成且没有新绑定的旧试炼/Boss 实体允许清理但不发新奖励；旧普通外层箱和旧 Boss 缓存无法可靠识别并自动迁移，管理员应在升级存档中清理遗留秘境壳。统一持久交付 outbox、任务 hook 当前步骤、拍卖/商店事务、远程菜单、鉴定消耗与 Curios 飞行门禁仍是高风险后续。

## 0.2.32 玩家死亡 Clone 与护送事务遗留

本批未新增占位物品、模型、纹理、实体类型、菜单、网络包或不兼容 capability schema；新增一个集中 Clone policy、极端走火 post-drop 提交状态及宗门进度根内的护送重试字段。自动化覆盖 58 个永久根键、2 个动态前缀、临时键排除、NBT 深拷贝/错误类型/一次消费、死亡/End Clone、post-cancellation 提交/回滚、可变 keepInventory 隔离、完成态护送、重试与 registry tombstone；全量 643 项测试和正式 `./gradlew build --no-daemon` 已通过。仍需真实客户端/专服烟测其他模组取消或改写掉落事件、死亡界面改规则时原版另一半库存行为、End 返回事件顺序、护送跨维/区块卸载/随从满额及满背包掉落实体。交付 helper 的世界实体生成失败仍是 at-most-once 风险；任务 hook 当前步骤、秘境/Boss owner-session Schema V2、拍卖/商店原子性、远程菜单、Boss 捕捉、鉴定消耗和 Curios 飞行门禁继续列为高风险后续。完整暂停交接见 `project_docs/updates/20260719_0.2.32_handoff_unfinished.md`。

## 0.2.31 炼器配方权限遗留

本批未新增占位物品、模型、纹理、方块、菜单、schema 或网络包；删除 24 个工作台近似配方，新增 11 个 catalog 对齐的 custom serializer，修正 7 个旧方并删除 1 个重复方。旧世界已解锁的工作台配方会随数据包重载失效，但既有物品不回收。当前只注册 G1/G2/G3 炼器炉，新增的 G4-G6 serializer 不会在低阶炉执行；真正的高阶炉方块、结构、菜单/反馈和 JEI 分类仍未实现。真实客户端仍需烟测三阶炉结构、数据包重载、配方书撤销、材料选择与失败残骸。

## 0.2.29 命令与任务账本权限遗留

本批未新增占位物品、模型、纹理、实体、菜单、数据 schema 或网络字段；复用现有目录、玩家持久数据、权限等级和阵法部署入口。自动化覆盖高风险命令节点、`learn:` 包、普通玩家学习按钮、未知 Boss、秘境目录 Boss、见闻重复幂等/首次失败不认领、同日日常、采集扣物和阵法凭证。仍需真实客户端/专服烟测管理员学习按钮、命令建议树、宗门任务跨日和秘境 Boss 生成。下一批优先移除 vanilla `refine_*.json` 炼器旁路；秘境/Boss owner-session Schema V2、任务 hook 当前步骤、拍卖事务、远程菜单、Boss 捕捉、死亡 clone、鉴定与 Curios 飞行门禁仍未完成。

## 0.2.27 储物镯权限遗留

本批未新增占位物品、模型、纹理、菜单类型、数据 schema 或网络包；复用现有储物镯、菜单、Forge item handler 和法宝门禁。自动化覆盖实例绑定、SWAP 早退、Shift-click 写回、16 种嵌套判定以及 owner/完整性顺序；仍需真实客户端和专服烟测拖拽、数字键、F、Q、断线、死亡、跨维及其他模组 capability 容器。旧存档中已嵌套的容器允许取出但不能重新放入，避免升级时直接销毁内容。普通 Boss/phase/mission/beast/natal 命令、秘境 owner/session 奖励重放、Boss 捕捉、走火保留物 clone、鉴定消耗与 Curios 飞行门禁仍是后续高风险项。

## 0.2.26 飞升护送与库存事务遗留

本批未新增占位物品、模型、纹理、实体类型、维度、数据 schema 或网络包；新增的交付服务复用 Forge 主背包交付 helper，护送复用现有侍灵实体与双语消息。自动化覆盖飞升提交顺序/验收、护送单向依赖/实体生成顺序及全源码复制模式；仍需真实客户端、专服和多人烟测 Forge 取消传送、满背包余量实体、护送跨维/死亡/卸载及创造模式上交。秘境与 Boss 的 session/永久奖励标记仍位于死亡易失玩家 NBT，是下一批 P0；任务/NPC 调试权限、任务 hook/clone、工作站旁路、拍卖原子性、储物嵌套和法宝/灵兽归属继续保留在审计队列。

## 0.2.23 秘境与旅行权限遗留

本批未新增占位物品、模型、纹理、实体、维度、数据 schema 或网络包。自动化覆盖入口权限、精确 NBT 费用回滚、专属/绑定失败语义、返程免费、只读同步、authored 节点路由、传送验收与召唤物构造安全；仍需真实客户端、专服和多人烟测。已知 P2 是取消传送前仍可能铺设目标平台，以及费用品只在副手时预检/预留槽范围不一致而误拒绝。max 审计新增的飞升取消丢库存、护送完成递归、任务/NPC 调试命令、任务 hook 重放、死亡克隆持久化、奖励部分入包复制、贡献购买、秘境试炼归属、储物嵌套与制造旁路已进入后续修复批次。

## 0.2.21 修炼权限与存档红线遗留

本批未新增占位物品、模型、纹理、实体、维度、数据 schema 或网络包。`GOLDEN_IMMORTAL`、`TAIYI`、`DAO_ANCESTOR` 等高于当前 `TRUE_IMMORTAL` 枚举的语料仍属于未来境界内容，但现在明确 fail-closed，不再降级成炼气门槛。审计确认的交易原子性、工作台/生产命令旁路、功法层数与前置、网络方向/百科节流、状态 amplifier/周期伤害归属、法宝 `powerScale` 与完整伤害管线仍需后续批次处理；真实客户端、专服和多人烟测不在自动化构建范围内。

## 0.2.20 状态伤害与权限深化遗留

本批未新增占位物品、模型、纹理、实体、维度、数据 schema 或网络包。M10 已从 vanilla 状态占位迁到带来源的 M14，M02 已接入血祭、天魔狂化、敛魂符和剑合一四个明确生产者；其余毒、幻术、控制与 resolver 元数据仍需逐项接入。M15 协同装备资格已对 capability、境界、主人、认主和完整性失败关闭，但 mapped/generic 主动技的 `powerScale` 伤害统一、`DamagePipelineHooks` 全管线整合、周期状态击杀归属、状态 amplifier 规则、Boss/状态/PvP 数值和真实客户端/专服/多人烟测仍为明确后续。

## 0.2.18 权限审查修复遗留

本批未新增占位物品、模型、纹理、实体、维度、数据 schema 或网络包。13 条审查问题已由自动化覆盖并通过全量构建；仍需真实客户端、专服和多人环境验证付费界门、飞升失败回滚、离线秘境过期重连、对话失败重试、第三方维度飞行与多来源伤害倍率叠加。M02/M10 真实状态施加端和 M15 `DamagePipelineHooks` 注册仍为明确后续。

## 0.2.1 前端 UI 修复遗留

未新增占位物品、模型、纹理或 GUI 资产；对话继续复用现有原创立绘与 journal skin。自动化已覆盖响应式布局、包容量/恶意计数、任务选择、对话 allowlist/距离和输入步进。仍需真实客户端验证 120×90 等效极端 GUI 缩放、Esc/连续点击、NPC 走远、商店最终停留、重生/跨维同步，以及 1,894 条图鉴滚动体验。

## 0.2.0 十七模块统一里程碑遗留

自动化构建与语料索引已闭合，但尚未完成真实客户端、专服和多人烟测。已知功能深度风险包括：M04 部分炼丹配方缺少可靠定向公式入口；M05 动态价格/跨币种换算需实机回归；M07/M13 飞舟船坞结构定义未统一；M13 飞升备份恢复仍需防复制审计；M14 的 outgoingDamageMul、blocksTechnique、hidesRealm 消费端已接线，但 M02/M10 真实状态施加端及 M07/M15 伤害钩子仍待后续；M15 器灵觉醒/祭炼成长链和装备扫描需继续深化。高保真结构、专用妖兽实体与美术资产仍是明确遗留。

## 0.1.493 leyline structures note

Added custom StructureType `seeking_immortals:leyline_vein` and four biome-scoped structure entries. Structure body reuses existing spirit_ore / low_spirit_iron_ore / yin_essence_ore / leyline_surface_marker / spirit_gathering_array blocks (no new textures). Hash aura remains authority; structures are physical presentation. Remaining: studio art, denser multi-chunk vein corridors, human live-smoke.

## 0.1.492 authority polish note

No new placeholder textures/models/entities. Protocol remains 18. Dual-cast reuses existing technique release authority. Remaining deferred: studio art, dedicated beast entities, multi-biome leyline structures, human live-smoke re-sign.

## 0.1.491 depth note

Added SyncAuctionLadderPacket/SkillTreeActionPacket (protocol 18). Leyline surface marker reuses spirit_ore texture as presentation placeholder. Beast ecology uses SummonedServitor proxies from spawn_tables. Remaining: dedicated marker art, full dialogue data files, human live-smoke re-sign.

## 0.1.490 full systems note

No new placeholder textures, models, item ids, blocks, entities, or packet fields were introduced. Hall GUIs reuse ImmortalUiSkin + existing Shop/Sect/Auction authority packets. Beast ecology densify reuses vanilla wolf/fox + SummonedServitor/contract path. Remaining: studio GeckoLib bestiary entities, multi-biome leyline structures, full dialogue node graphs, human live-smoke re-sign.

## 0.1.488 CustomTaskEvent note

No new placeholder textures, models, item ids, blocks, entities, or packet fields were introduced. Custom tasks reuse FTB built-in type + task tags; authority checks reuse SectWarService and ReputationService. Live FTB client auto-submit for custom tags remains a human verification item.

## 0.1.487 Multi-army war + FTB dimension note

No new placeholder textures, models, item ids, blocks, entities, or packet fields were introduced. Sect war reuses SummonedServitor + TrialCombatShellService. FTB dimension tasks use built-in FTB Quests type. Live multiplayer war density and FTB client auto-submit remain human verification items.

## 0.1.486 Review hardening note

No new placeholder assets, item ids, blocks, entities, packet fields, packet registrations, or data schemas were introduced. Servitor ownership now uses server SavedData so unloaded entities remain capped and receive deferred stance/dismiss commands when reloaded; chunks are not force-loaded. Live multiplayer death/auction/war/servitor smoke testing remains a human verification item beyond the passing automated build.

## 0.1.387 Text-material wave7 resource note

New formation/gate blocks reuse spirit_gathering_array texture placeholders. Ticket/reputation enforcement for ascension/sect gates remains deferred per open decisions.

## 0.1.386 Text-material wave6 resource note

refinement_forge reuses spirit_gathering_array texture. Remaining 79 techniques were honesty-mapped: summon/command as self-buff MVP, talisman_consume as projectile without item consume, ultimate/secret_art as area/self effects. Deeper fidelity remains deferred.

## 0.1.385 Text-material wave5 resource note

Formation cores reuse spirit_gathering_array texture. Remaining text techniques are largely ultimate/secret_art/talisman_consume/summon needing policy decisions in text_material_open_decisions.md.

## 0.1.384 Text-material wave4 resource note

thunder_tribulation_altar reuses spirit_gathering_array texture. Puppet/summon spells use honest self-buff/control MVP without entities. High-realm secret arts mapped to existing bases.

## 0.1.383 Text-material wave3 resource note

blood_sacrifice_altar reuses spirit_gathering_array texture as placeholder. Talisman spells do not yet consume physical talisman items (see open decisions). Summon techniques still deferred honestly.

## 0.1.382 Text-material wave2 resource note

sect_gate_array reuses spirit_gathering_array texture as placeholder. Spell registrations reuse existing bases only. Open decisions for summon/talisman/ticket gates recorded in project_docs/text_material_open_decisions.md.

## 0.1.381 Text-material wave1 resource note

No dedicated new PNG art was authored. `teleport_array_pedestal` reuses the existing spirit-gathering array texture as a temporary placeholder model. Spell slice reuses existing DustParticle/CultivationFireball/SwordTechnique/Formation/Recovery/SelfBuff bases only. Remaining placeholder debt: dedicated pedestal/array art, full 346 technique coverage, placeable formation zones, ritual multiblocks, GeckoLib entities, and live client smoke tests.

## 0.1.380 Mod-only refinement recipe batch resource note

No new placeholder textures, models, item ids, blocks, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, worldpack files, artifact/refinement catalogs, spell files, or runtime data loaders were added. This batch adds nine vanilla shaped recipes that reuse existing mod carriers only and intentionally skip recipes that still map materials to minecraft leather/feather stand-ins. A proper refinement forge/custom serializer, realm and forge-grade checks, manual unlocks, success/failure mechanics, JEI presentation, source-accurate acquisition, and live recipe-book smoke checks remain deferred.

## 0.1.379 Black-Gold Shield refinement recipe resource note

No new placeholder textures, models, item ids, blocks, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, worldpack files, artifact/refinement catalogs, spell files, or runtime data loaders were added. The recipe slice reuses the existing Black-Gold Shield artifact carrier, Spirit Iron, Kunwu Copper, and vanilla shaped crafting as a temporary refinement path. A proper refinement forge/custom serializer, realm and forge-grade checks, manual unlocks, success/failure mechanics, JEI presentation, source-accurate low Spirit Iron / Kunwu Copper acquisition, and live recipe-book smoke checks remain deferred.

## 0.1.378 FTB default seeding crash-fix resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB chapter files, worldpack files, artifact/refinement catalogs, spell files, or runtime data loaders were added. The fix only changes how existing bundled FTB defaults are copied when missing, avoiding unsupported `COPY_ATTRIBUTES` metadata copying from a jar resource stream. Remaining FTB placeholder debt is unchanged: real rewards, costs, branch locks, quest icons, advancement/custom tasks, Seeking Immortals quest-state sync, and live FTB client smoke checks.

## 0.1.376 Moon Shadow Disk refinement recipe resource note

No new placeholder textures, models, item ids, blocks, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, worldpack files, artifact/refinement catalogs, spell files, or runtime data loaders were added. The recipe slice reuses the existing Moon Shadow Disk artifact carrier, Yin Stone, Spirit Iron, and vanilla shaped crafting as a temporary refinement path. A proper refinement forge/custom serializer, realm and forge-grade checks, manual unlocks, success/failure mechanics, JEI presentation, source-accurate Yin Essence Ore / low Spirit Iron acquisition, and live recipe-book smoke checks remain deferred.

## 0.1.374 Formation spell resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, artifact/refinement maps, worldpack data files, or runtime data loaders were added. The formation spell slice intentionally reuses code-side `DustParticleOptions`, existing vanilla sound/effect APIs, and the current server-authoritative technique release path for 13 formation ids. Dedicated formation spell icons, custom particle texture assets, manual art, strict method/source/region/reputation gate presentation, source-accurate acquisition, persistent placeable formation state, PvP tuning, and live-world visual smoke checks remain deferred.

## 0.1.373/0.1.374 FTB Dajin / Kunwu item-task bridge placeholder note

The five upgraded Dajin / Kunwu FTB tasks are intentionally non-consuming inventory checks only. They make existing Immortal Jade, Blank Jade Slip, Cold Jade, Spirit Gathering Array, and Demon-Suppress Talisman Blank carriers visible to Wanbao auction collateral, Kunwu intelligence recording, cold-tide preparation, outer-array routing, and demon-sealing script presentation, but they do not grant rewards, consume costs, lock branches, write `QuestProgress`, sync Seeking Immortals quest state, trigger NPC/server events, validate advancement/custom conditions, prove auction access, prove Kunwu permit state, prove formation skill, trigger puppet-king encounters, add quest icons, or prove live client behavior. Remaining placeholders include wider item-task coverage, advancement/custom tasks, server-authoritative reward bridges, branch locks, auction/Kunwu validation, encounter wiring, quest icons, and live FTB client smoke checks.

## 0.1.375 Void Cold Jade Pendant refinement recipe resource note

No new placeholder textures, models, item ids, blocks, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, worldpack files, artifact/refinement catalogs, spell files, or runtime data loaders were added. The recipe slice reuses the existing Void Palace Cold Jade Pendant artifact carrier, Cold Jade material, and vanilla shaped crafting as a temporary refinement path. A proper refinement forge/custom serializer, Core Formation realm and forge-grade checks, quest/manual unlocks, success/failure mechanics, JEI presentation, source-accurate Void Palace Cold Jade and Hundred-Year Ice acquisition, and live recipe-book smoke checks remain deferred.

## 0.1.372 Evil Illusion Mirror refinement recipe resource note

No new placeholder textures, models, item ids, blocks, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, worldpack files, artifact/refinement catalogs, spell files, or runtime data loaders were added. The recipe slice reuses the existing Evil Illusion Mirror artifact carrier, Cold Jade, Cloud Mushroom, Soul-Gathering Stone, and vanilla shaped crafting as a temporary refinement path. A proper refinement forge/custom serializer, realm and forge-grade checks, manual unlocks, success/failure mechanics, JEI presentation, source-accurate Hundred-Year Ice / Demon Corruption Fungus / Soul-Gathering Stone acquisition, and live recipe-book smoke checks remain deferred.

## 0.1.368 FTB Fallen Demon / Yin item-task bridge placeholder note

The three upgraded Fallen Demon / Yin Underworld FTB tasks are intentionally non-consuming inventory checks only. They make existing Yin Stone, Soul-Gathering Stone, and Soul Fragment carriers visible to Yin Luo Hall initiation, soul-anchor stabilization, and Nether Core formation, but they do not grant rewards, consume costs, lock branches, write `QuestProgress`, sync Seeking Immortals quest state, trigger NPC/server events, validate advancement/custom conditions, prove Yin Luo reputation, prove ghost-path stage state, trigger Nether River encounters, add quest icons, or prove live client behavior. Remaining placeholders include wider item-task coverage, advancement/custom tasks, server-authoritative reward bridges, branch locks, Yin/ghost validation, boss/encounter wiring, quest icons, and live FTB client smoke checks.

## 0.1.371 Talisman Soul Charm refinement recipe resource note

No new placeholder textures, models, item ids, blocks, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, worldpack files, artifact/refinement catalogs, spell files, or runtime data loaders were added. The recipe slice reuses the existing Soul-Calming Talisman Treasure artifact carrier, Mortal Talisman Paper, Soul-Gathering Stone, and vanilla shaped crafting as a temporary refinement path. A proper refinement forge/custom serializer, realm and forge-grade checks, manual unlocks, success/failure mechanics, JEI presentation, source-accurate talisman paper grade and soul-stone acquisition, and live recipe-book smoke checks remain deferred.

## 0.1.370 Talisman Soul Charm refinement recipe resource note

No new placeholder textures, models, item ids, blocks, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, worldpack files, artifact/refinement catalogs, spell files, or runtime data loaders were added. The recipe slice reuses the existing Soul-Calming Talisman Treasure artifact carrier, Mortal Talisman Paper, Soul-Gathering Stone, and vanilla shaped crafting as a temporary refinement path. A proper refinement forge/custom serializer, realm and forge-grade checks, manual unlocks, success/failure mechanics, JEI presentation, source-accurate talisman-paper and soul-gathering acquisition, and live recipe-book smoke checks remain deferred.

## 0.1.366 Confucian spell resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, artifact/refinement maps, worldpack data files, or runtime data loaders were added. The Confucian spell slice intentionally reuses code-side `DustParticleOptions`, vanilla sound/effect APIs, and the current server-authoritative technique release path for five Confucian ids. Dedicated Confucian spell icons, custom particle texture assets, manual art, strict Confucian-method/source gate presentation, source-accurate acquisition, persistent ink-sea/word-seal state, PvP tuning, and live-world visual smoke checks remain deferred.

## 0.1.367 Bedrock Shield refinement recipe resource note

No new placeholder textures, models, item ids, blocks, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, worldpack files, artifact/refinement catalogs, spell files, or runtime data loaders were added. The recipe slice reuses the existing Bedrock Shield artifact carrier, Kunwu Copper, Diyuan Pressure Moss, and vanilla shaped crafting as a temporary refinement path. A proper refinement forge/custom serializer, realm and forge-grade checks, manual unlocks, success/failure mechanics, JEI presentation, source-accurate Earth Spine Root / Diyuan Pressure Moss acquisition, and live recipe-book smoke checks remain deferred.

## 0.1.365 Snake Pearl refinement recipe resource note

No new placeholder textures, models, item ids, blocks, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, worldpack files, artifact/refinement catalogs, spell files, or runtime data loaders were added. The recipe slice reuses the existing Snake Pearl artifact carrier, Beast Core, True Dragon Blood, and vanilla shaped crafting as a temporary refinement path. A proper refinement forge/custom serializer, realm and forge-grade checks, manual unlocks, success/failure mechanics, JEI presentation, source-accurate demon-core-fragment and beast-blood-vial acquisition, and live recipe-book smoke checks remain deferred.

## 0.1.364 Qingning Mirror refinement recipe resource note

No new placeholder textures, models, item ids, blocks, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, worldpack files, artifact/refinement catalogs, spell files, or runtime data loaders were added. The recipe slice reuses the existing Qingning Mirror artifact carrier, Cold Jade, Kunwu Copper, and vanilla shaped crafting as a temporary refinement path. A proper refinement forge/custom serializer, realm and forge-grade checks, manual unlocks, success/failure mechanics, JEI presentation, source-accurate acquisition, and live recipe-book smoke checks remain deferred.

## 0.1.364 Demon Rift event-gated portal resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, artifact/refinement catalogs, spell files, or runtime data loaders were added. The Demon Rift gate slice reuses the existing 13x13 Spirit Gathering Array / Spirit Ore portal structure, Fallen Demon Valley worldpack data, `ancient_demon_seal_breach` daily event, and default Demon Rift anchor. Dedicated portal art, Demon Rift terrain/structures/hazards, source-accurate loot/encounters, route/quest UI, and live in-game smoke checks remain deferred.

## 0.1.361 Flying Needle Set refinement recipe resource note

No new placeholder textures, models, item ids, blocks, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, worldpack files, artifact/refinement catalogs, spell files, or runtime data loaders were added. The recipe slice reuses the existing Flying Needle Case artifact carrier, Spirit Iron, Spirit Silk, and vanilla shaped crafting as a temporary refinement path. A proper refinement forge/custom serializer, realm and forge-grade checks, manual unlocks, success/failure mechanics, JEI presentation, source-accurate acquisition, and live recipe-book smoke checks remain deferred.

## 0.1.359 FTB Mulan/Tianlan/Demonic verification note

No new placeholder textures, models, item ids, blocks, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB semantics, worldpack files, artifact/refinement catalogs, or runtime data loaders were added. This pass only verifies the existing Mulan/Tianlan/Demonic non-consuming FTB inventory checks and current Buddhist spell compile reconciliation under `mod_version=0.1.359`. The known placeholder debt remains: real rewards, costs, branch locks, faction/beast-contract/demonic-karma validation, boss/encounter wiring, quest icons, Seeking Immortals quest-state sync, and live FTB client smoke checks.

## 0.1.359 Current-tree build recheck resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, worldpack files, artifact/refinement catalogs, spell files, or runtime data loaders were added by this verification pass. The current-tree build now passes under `mod_version=0.1.359`, including the inherited 13x13 realm gate slice and Mulan/Tianlan/Demonic FTB item-task bridge. Remaining placeholder/risk debt is unchanged: live-smoke the larger portal travel flow, audit inherited network-package diffs before release if packet formats changed, add dedicated portal/quest presentation, and replace FTB inventory checks with authoritative task/reward/state bridges.

## 0.1.359 Buddhist spell resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, artifact/refinement maps, worldpack data files, or runtime data loaders were added. The Buddhist spell slice intentionally reuses code-side `DustParticleOptions`, vanilla sound/effect APIs, and the current server-authoritative technique release path for six Buddhist ids. Dedicated Buddhist spell icons, custom particle texture assets, manual art, strict Buddhist-method/source gate presentation, source-accurate acquisition, persistent sarira shield state, PvP tuning, and live-world visual smoke checks remain deferred.

## 0.1.359 13x13 realm gate resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, worldpack files, artifact/refinement maps, spell data, or runtime data loaders were added. The portal slice reuses the existing Spirit Gathering Array and Spirit Ore resources plus the current worldpack default-anchor generator. Dedicated portal art, higher-tier frame materials, live in-game smoke checks, route/quest presentation, event-gated Demon Rift entry enforcement, and terrain/structure dressing remain deferred.

## 0.1.356 Storage Bracelet refinement recipe resource note

No new placeholder textures, models, item ids, blocks, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, worldpack files, artifact/refinement catalogs, or runtime data loaders were added. The recipe slice reuses the existing low Storage Bracelet artifact carrier, Void Crystal, Kunwu Copper, and vanilla shaped crafting as a temporary refinement path. A proper refinement forge/custom serializer, realm and forge-grade checks, manual unlocks, success/failure mechanics, JEI presentation, source-accurate acquisition, and live recipe-book smoke checks remain deferred.

## 0.1.353 FTB Fallen Demon / Yin item-task bridge placeholder note

The five upgraded Fallen Demon / Yin Underworld FTB tasks are intentionally non-consuming inventory checks only. They make existing Fire Talisman, Void Crystal, Demon-Suppress Talisman Blank, Soul Fragment, and Yin-Body Protection Charm carriers visible to Fallen Demon preparation, spatial-rift stabilization, demon-rift reinforcement, soul-banner clue routing, and Nether River fog progression, but they do not grant rewards, consume costs, lock branches, write `QuestProgress`, sync Seeking Immortals quest state, trigger NPC/server events, validate advancement/custom conditions, prove Fallen Demon seal-state work, prove ghost-path stage work, validate Nether River hazards, spawn encounters, or prove live client behavior. Remaining placeholders include wider item-task coverage, advancement/custom tasks, server-authoritative reward bridges, branch locks, seal/ghost/hazard validation, boss/encounter wiring, quest icons, and live FTB client smoke checks.
## 0.1.352 Spirit-Gathering Bead refinement recipe resource note

No new placeholder textures, models, item ids, blocks, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, worldpack files, artifact/refinement catalogs, or runtime data loaders were added. The recipe slice reuses the existing Spirit-Gathering Bead artifact carrier, Soul-Gathering Stone, Spirit Stone Shard, and vanilla shaped crafting as a temporary refinement path. A proper refinement forge/custom serializer, realm and forge-grade checks, manual unlocks, success/failure mechanics, JEI presentation, source-accurate acquisition, and live recipe-book smoke checks remain deferred.

## 0.1.351 Cloud Boots refinement recipe resource note

No new placeholder textures, models, item ids, blocks, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, worldpack files, artifact/refinement catalogs, or runtime data loaders were added. The recipe slice reuses the existing Cloud-Treading Boots artifact carrier, Spirit Silk, Spirit Iron, and vanilla shaped crafting as a temporary refinement path. A proper refinement forge/custom serializer, realm and forge-grade checks, manual unlocks, success/failure mechanics, JEI presentation, source-accurate acquisition, and live recipe-book smoke checks remain deferred.

## 0.1.350 FTB Star Palace / Inverse item-task bridge placeholder note

The six upgraded Star Palace / Inverse Star FTB tasks are intentionally non-consuming inventory checks only. They make existing Fire Talisman, Star Palace Tax Receipt, Immortal Jade, and Beast Core carriers visible to Star Palace enforcement/commerce, Void auction, Void lead purchase, abyss-rift routing, and Void Palace cycle-gate progression, but they do not grant rewards, consume costs, lock branches, write `QuestProgress`, sync Seeking Immortals quest state, trigger NPC/server events, validate advancement/custom conditions, prove reputation or patrol-heat work, validate Void Palace keys/cycles, spawn encounters, or prove live client behavior. Remaining placeholders include wider item-task coverage, advancement/custom tasks, server-authoritative reward bridges, branch locks, reputation validation, key/cycle validation, quest icons, and live FTB client smoke checks.

## 0.1.350 Illusion spell resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, artifact/refinement maps, worldpack data files, or runtime data loaders were added. The illusion slice intentionally reuses code-side `DustParticleOptions`, vanilla sounds/effects, and the current server-authoritative technique release path for thirteen illusion ids. Dedicated illusion spell icons, custom particle texture assets, manual art, stricter prerequisite/source gates, source-accurate Yanyue/Wanhu acquisition, PvP tuning, richer AI confusion behavior, and live-world visual smoke checks remain deferred.

## 0.1.348 Artifact support activation resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, worldpack data files, artifact/refinement JSON files, or runtime data loaders were added. This is a code-only activation bridge for existing support artifact catalog carriers, using vanilla particles/sounds/effects and the current `ArtifactActivationService` cost/cooldown/integrity path. Dedicated support-artifact visuals, persistent vehicle entities, capture containers, real refinement forge resources, spirit-liquid growth resources, acquisition placement, JEI presentation, and live-world smoke checks remain deferred.

## 0.1.347 FTB Spirit Realm service item-task bridge placeholder note

The five upgraded Spirit Realm service FTB tasks are intentionally non-consuming inventory checks only. They make existing Beast Core, Alliance Merit Token, Fengyuan Clan Ginseng, and Mortal Talisman Paper carriers visible to Tianyuan, Fengyuan clan, and barbarian progression, but they do not grant rewards, consume costs, lock branches, write `QuestProgress`, sync Seeking Immortals quest state, trigger NPC/server events, validate advancement/custom conditions, prove craft-station or reputation work, spawn encounters, or prove live client behavior. Remaining placeholders include wider item-task coverage, advancement/custom tasks, server-authoritative reward bridges, branch locks, reputation validation, quest icons, and live FTB client smoke checks.

## 0.1.344 Recovery/protection spell resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, artifact/refinement maps, worldpack data files, or runtime data loaders were added. The recovery/protection slice intentionally reuses code-side `DustParticleOptions`, vanilla sounds/effects, and the current server-authoritative technique release path for eight recovery ids. Dedicated recovery spell icons, custom particle texture assets, manual art, stricter prerequisite/source gates, source-accurate acquisition, persistent ward resources, PvP tuning, and live-world visual smoke checks remain deferred.

## 0.1.343 Artifact utility activation resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, worldpack data files, artifact/refinement JSON files, or runtime data loaders were added. This is a code-only activation bridge for existing artifact catalog carriers, using vanilla particles/sounds/effects and the current `ArtifactActivationService` cost/cooldown/integrity path. Dedicated magnet/world/formation/Great Shift visuals, persistent formation placement, real world-domain spaces, source-accurate acquisition placement, JEI presentation, and live-world smoke checks remain deferred.

## 0.1.342 FTB Chaotic Sea/Void item-task bridge placeholder note

The five upgraded Chaotic Sea, Inverse Star, and Void Palace FTB tasks are intentionally non-consuming inventory checks only. They make existing Raw Sea Pearl, Cold Jade, Beast Core, and Void Crystal carriers visible to FTB progression, but they do not grant rewards, consume costs, lock branches, write `QuestProgress`, sync Seeking Immortals quest state, trigger NPC/server events, validate advancement/custom conditions, prove craft-station or reputation work, spawn encounters, or prove live client behavior. Remaining placeholders include wider item-task coverage, advancement/custom tasks, server-authoritative reward bridges, branch locks, reputation validation, quest icons, and live FTB client smoke checks.

## 0.1.339 FTB Tiannan/Dajin/Mulan item-task bridge placeholder note

The seven upgraded Tiannan, Dajin/Kunwu, and Mulan FTB tasks are intentionally non-consuming inventory checks only. They make existing Puppet Core Blank, Ironwood, Mortal Talisman Paper, Fasting Pill paper formula, Kunwu Copper, and War Contribution Token carriers visible to FTB progression, but they do not grant rewards, consume costs, lock branches, write `QuestProgress`, sync Seeking Immortals quest state, trigger NPC/server events, validate advancement/custom conditions, prove craft-station work, spawn encounters, or prove live client behavior. Remaining placeholders include wider item-task coverage, advancement/custom tasks, server-authoritative reward bridges, branch locks, craft-station and reputation validation, quest icons, and live FTB client smoke checks.

## 0.1.340 Artifact family activation resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, worldpack data files, artifact/refinement JSON files, or runtime data loaders were added. This is a code-only activation bridge for existing artifact catalog carriers, using vanilla particles/sounds and the current `ArtifactActivationService` cost/cooldown/integrity path. Dedicated ruler/mirror/sound/swarm visuals, real puppet/beast summon entities and contracts, natal binding resources, acquisition placement, refinement forge UI resources, JEI presentation, and live-world smoke checks remain deferred.

## 0.1.337 High-tier artifact activation resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, worldpack data files, artifact/refinement JSON files, or runtime data loaders were added. This is a code-only activation bridge for already registered high-tier artifact carriers, reusing vanilla particles/sounds and the current `ArtifactActivationService` cost/cooldown/integrity/use-count path. Dedicated artifact visuals, richer source-accurate per-artifact effects, natal binding resources, acquisition placement, refinement forge UI resources, JEI presentation, and live-world smoke checks remain deferred.

## 0.1.339 Divine Sense spell resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, worldpack data files, artifact/refinement maps, or runtime data loaders were added. The divine-sense slice intentionally reuses code-side `DustParticleOptions`, vanilla sounds/effects, and the current server-authoritative technique release path for ten神识 ids. Dedicated神识 spell icons, custom particle texture assets, manual art, stricter prerequisite/source gates, source-accurate acquisition, PvP tuning, and live-world visual smoke checks remain deferred.

## 0.1.335 Demonic/Ghost spell behavior reconcile resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, worldpack data files, artifact/refinement maps, or runtime data loaders were added. This is a code-only spell behavior correction for the existing `DemonicGhostSpell`; dedicated spell icons, custom particle textures, manual art, stricter prerequisite/source gates, source-accurate sect or ruin acquisition, PvP tuning, and live-world visual smoke checks remain deferred.

## 0.1.336 FTB Yin/Diyuan version reconcile note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB task semantics, worldpack files, spell/technique data, artifact/refinement maps, or runtime loaders were added by this version reconcile. It only moves the shared version to `0.1.336` so the FTB Yin/Diyuan item-task bridge does not share the concurrent Demonic/Ghost behavior reconcile's `0.1.335` record. The remaining placeholder debt is unchanged from the item-task bridge: real reward grants, branch locks, advancement/custom tasks, NPC triggers, quest icons, Seeking Immortals quest-state sync, and live FTB client smoke tests.

## 0.1.335 FTB Yin/Diyuan item-task bridge placeholder note

The six upgraded Fallen Demon/Yin and Spirit Realm service FTB tasks are intentionally non-consuming inventory checks only. They make existing Yin Stone, Soul-Gathering Stone, Alliance Merit Token, Diyuan Permit, and Pressure-Resist Charm carriers visible to FTB progression, but they do not grant rewards, consume costs, lock branches, write `QuestProgress`, sync Seeking Immortals quest state, trigger NPC/server events, validate advancement/custom conditions, spawn encounters, or prove live client behavior. Remaining placeholders include wider item-task coverage, advancement/custom tasks, server-authoritative reward bridges, branch locks, quest icons, and live FTB client smoke checks.

## 0.1.333 Demonic/Ghost spell resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, worldpack data files, or runtime data loaders were added. The compatibility slice intentionally uses code-side `DustParticleOptions`, vanilla sounds, vanilla mob effects, and the current server-authoritative technique release path for the six Core Formation demonic/ghost ids. Dedicated spell icons, custom particle textures, manual art, stricter prerequisite/source gates, source-accurate sect or ruin acquisition, PvP tuning, and live-world visual smoke checks remain deferred.

## 0.1.334 Current-tree version-sync resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, worldpack files, FTB files, spell/technique data, artifact/refinement maps, or runtime loaders were added by this version-sync/build verification beyond the already documented `0.1.333` output carriers and parallel Demonic/Ghost spell fix. The version gate required `mod_version=0.1.334`; remaining placeholders stay with active artifact behavior, source placement, refinement UI/resources, JEI, dedicated art, spell gate presentation, and live smoke checks.

## 0.1.333 High-tier artifact output carrier resource note

The new `xuanguang_mirror`, `xuanhuang_mirror`, `nine_dragon_cauldron_replica`, `void_refining_bell`, `talisman_treasure_demon_seal`, `natal_sword_embryo`, `four_symbols_ruler_replica`, and `three_flame_fan_replica` carriers intentionally use lightweight vanilla item models (`spyglass`, `cauldron`, `bell`, `paper`, `iron_sword`, `blaze_rod`, and `blaze_powder`) as placeholder presentation. No dedicated PNG textures, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, worldpack files, FTB files, spell/technique data, or runtime loaders were added. They unblock registered output-item identity for current high-tier refinement plans, while active treasure behavior, source-accurate drops/shops/loot, refinement-forge UI resources, JEI presentation, dedicated art, and live smoke checks remain deferred.

## 0.1.332 FTB mainline item-task bridge placeholder note

The first four upgraded mainline FTB tasks are intentionally non-consuming inventory checks only. They make existing Spirit Grass, Ling Gen Test Stone, Spirit Stone Shard, and low Fasting Pill carriers visible to FTB progression, but they do not grant rewards, consume costs, lock branches, write `QuestProgress`, sync Seeking Immortals quest state, trigger NPC/server events, validate advancement/custom conditions, or prove live client behavior. Remaining placeholders include wider item-task coverage, advancement/custom tasks, server-authoritative reward bridges, branch locks, quest icons, and live FTB client smoke checks.

## 0.1.331 Refinement gap carrier resource note

The new `xuanguang_mirror_shard`, `xuanhuang_mirror_shard`, `nine_dragon_cauldron_shard`, `void_bell_fragment`, `demon_suppress_talisman_blank`, `natal_artifact_embryo`, `eight_spirit_ruler_shard`, and `seven_flame_fan_replica` carriers intentionally use lightweight vanilla item textures (`prismarine_crystals`, `echo_shard`, `blaze_rod`, `paper`, `heart_of_the_sea`, `amethyst_shard`, and `blaze_powder`) as placeholder presentation. No dedicated PNG textures, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, worldpack files, FTB files, spell files, or runtime loaders were added. They unblock material resolution for the current artifact-refinement recipe set, while registered high-tier output carriers, source drops, shops, loot placement, refinement-forge UI resources, JEI presentation, dedicated art, and live smoke checks remain deferred.

## 0.1.329 FTB Ascension Border quest placeholder note

The new Ascension Border / endgame FTB chapter intentionally uses checkmark-only tasks. It exposes high-realm endgame, Kunwu/Fallen Demon direct expedition backfill, Yin pilgrimage, ghost-sect ban and rare lift, seven-sect outer-to-inner promotion, mortal-to-spirit ascension bridge, Spirit Realm border hazards, Diyuan core probe/crystal, Great Vehicle insight, tribulation clouds, and immortal-realm placeholder without trusting client-side costs, granting rewards, locking branches, writing sect/ghost/ascension/tribulation state, consuming vouchers, spawning encounters, or mutating cultivation capabilities. Remaining placeholders include item/advancement/custom task conditions, mutually exclusive branch locks, reward bridges, NPC triggers, icons, real boss/encounter wiring, ascension-gate validation, tribulation preparation checks, and live FTB client smoke checks.

## 0.1.328 Refinement material carrier resource note

The new `puppet_core_blank`, `thunder_bamboo`, `ice_fire_crystal`, and `void_marrow` items intentionally use lightweight vanilla item textures (`heart_of_the_sea`, `bamboo`, `amethyst_shard`, and `ender_pearl`) as placeholder presentation. No dedicated PNG textures, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, worldpack files, or runtime loaders were added. They unblock material resolution for puppet, thunder talisman, ice-fire, and void-marrow refinement requirements, while exact source drops, shops, loot placement, high-tier ancient-treasure shard carriers, output artifact carriers, refinement-forge UI resources, JEI presentation, and live smoke checks remain deferred.

## 0.1.326/0.1.327 FTB Star Palace / Inverse Star quest placeholder note

The new Star Palace / Inverse Star FTB chapter intentionally uses checkmark-only tasks. It exposes Star Palace enforcement, commerce, council mediation, internal tax voting, Inverse Star bounty/smuggle/sabotage routes, Void Palace intelligence heist, abyss outpost pressure, void-key fragment turn-in, cycle entrance, layered expedition mapping, and boss-loot ledger without trusting client-side costs, granting rewards, locking branches, writing reputation/tax/patrol/teleport/sabotage state, consuming void keys, spawning encounters, or mutating cultivation capabilities. Remaining placeholders include item/advancement/custom task conditions, mutually exclusive faction locks, Star Palace and Inverse Star reward bridges, NPC triggers, icons, patrol heat, tax vote state, teleport-array sabotage consequences, Void Palace cycle/key validation, boss/secret-realm task wiring, and live FTB client smoke checks.

## 0.1.325 Turtle/poison refinement alias resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, spell files, worldpack data files, or runtime data loaders were added. The mapping slice intentionally reuses vanilla `minecraft:scute` for text-material `turtle_shell` and vanilla `minecraft:spider_eye` for `poison_sac`, covering Giant Turtle Puppet Core and Thousand Bee Needles material checks without pretending high-tier shard/embryo resources are implemented. Exact turtle-shell plates, poison sacs, Spirit Realm turtle-shell faction rewards, venom beast drops, dedicated art, and full refinement-workstation resources remain deferred.

## 0.1.323 Xuan Yin Foundation spell resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, worldpack data files, or runtime data loaders were added. The spell slice intentionally reuses custom code-side DustParticle visuals and existing vanilla sound/effect APIs for `soul_devouring_cloud`, `yin_soul_chain`, `underworld_flame`, and `corpse_armor`. Dedicated Xuan Yin spell icons, custom particle texture assets, manual art, stricter prerequisite-method/path lock presentation, ghost/corpse summon resources, and live-world visual smoke checks remain deferred.

## 0.1.319 Yin/special refinement alias resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, spell files, worldpack data files, or runtime data loaders were added. The mapping slice intentionally reuses existing `ironwood`, `yin_stone`, `soul_fragment`, `true_dragon_blood`, `diyuan_pressure_moss`, `cloud_mushroom`, `star_meteorite`, and `cold_jade` carriers to cover ghost-wood, Yin-ore, soul-moss, beast-blood, Earth-Spine Root, demon-fungus, star-sand, and Void Palace cold-jade source ids in artifact refinement. Exact physical materials, source drops, shop placement, dedicated art, output carriers for unmapped higher artifacts, and the full refinement workstation remain deferred.

## 0.1.319 FTB Spirit Realm service quest placeholder note

The new Spirit Realm/Tianyuan/Fengyuan FTB chapter intentionally uses checkmark-only tasks. It exposes Tianyuan enlistment, demon-contract merit, beast-wave defense, convoy and diplomacy hooks, Diyuan permit/quota routing, Fengyuan human-clan guest and four specialty branches, Spirit Realm eighteen-clan pilgrimage samples, Diyuan deep pressure/core/ancient-beast progression, and barbarian demon-king tribute/token routing without trusting client-side costs, granting rewards, locking branches, writing merit/reputation/permit/token state, spawning encounters, or mutating cultivation capabilities. Remaining placeholders include item/advancement/custom task conditions, mutually exclusive faction or clan locks, merit/reputation reward bridges, NPC triggers, icons, Diyuan permit validation, barbarian king-token counts, encounter/boss wiring, and live Spirit Realm route smoke checks.

## 0.1.317 FTB Mulan/Tianlan and Demonic Six quest placeholder note

The new Mulan/Demonic FTB chapter intentionally uses checkmark-only tasks. It exposes Mulan/Tiannan/Tianlan war routing, Fashi initiation, Holy Bird and Tianhu optional branches, Wutu feud, Demonic Six north migration, named sect recruitment, and righteous-bounty backlash without trusting client-side costs, granting rewards, locking branches, writing faction/karma state, spawning encounters, or mutating cultivation capabilities. Remaining placeholders include item/advancement/custom task conditions, mutually exclusive branch locking, war-merit and demonic-karma reward bridges, NPC triggers, icons, encounter/boss wiring, and broader late-game quest-chain coverage.

## 0.1.315 FTB Fallen Demon/Yin quest placeholder note

The new Fallen Demon/Yin FTB chapter intentionally uses checkmark-only tasks. It exposes Fallen Demon Valley seal weakening, ancient demon projection, Yinming escape, Yin Luo Hall, Nether River ferry/fog/soul-shoal, ghost cultivation stages, and the Nether River guardian without trusting client-side costs, granting rewards, spawning bosses, changing faction/ghost state, or mutating cultivation capabilities. Remaining placeholders include item/advancement/custom task conditions, branch locking, real reward bridges, NPC triggers, icons, boss/secret-realm task wiring, and broader quest-chain coverage.

## 0.1.314 Existing-material refinement alias resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, worldpack data files, or runtime data loaders were added. The mapping slice intentionally reuses existing `phoenix_feather`, `true_dragon_blood`, `dragon_scale`, `spirit_beast_bone`, and `void_crystal` carriers to cover fire-feather, true-spirit-blood, dragon-scale, bulk beast-bone, and space-crystal-fragment source ids in artifact refinement. Exact material fragments/blocks, high-tier source drops, shop placement, dedicated art, output artifact carriers, and the full refinement workstation remain deferred.

## 0.1.312 Artifact material alias resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, worldpack data files, or runtime data loaders were added. The mapping slice intentionally reuses existing `beast_core` and `talisman_paper_mortal` carriers to cover low/high demon-core and generic talisman-paper source ids in artifact refinement while exact tiered demon cores, paper grades, source drops, shops, and dedicated art remain deferred.

## 0.1.310 Gold Seam Stone refinement material resource note

The new `gold_seam_stone` item intentionally uses a lightweight model that points at the vanilla raw-gold texture; no dedicated PNG, block/ore resource, loot table, worldgen, shop stock, GUI asset, packet, entity, capability, or runtime data loader was added. It unblocks material resolution for metal-heavy artifact refinement paths such as Gold Demon Chain, Gold Light Brick, Vajra Shield, thunder talismans, and natal sword embryos when the remaining source materials are already mapped. Source-accurate Dajin/Kunwu mining, region loot, dedicated art, shop placement, JEI presentation, and full refinement-workstation resources remain deferred.

## 0.1.309 Named Foundation elemental spell resource note

No new placeholder textures, item models, blocks, recipes, loot tables, GUI assets, packets, entity registrations, capabilities, shop data, worldpack data files, or runtime data loaders were added. This spell slice reuses the existing custom `CultivationFireballEntity`, its renderer, and DustParticle patterns for three projectile variants, and adds a code-only DustParticle beam for 落云灵焰. Dedicated spell icons, projectile textures, manual art, richer emitter curves, prerequisite-gate UI, and live-world visual smoke checks remain deferred.

## 0.1.308 Kunwu Copper refinement material resource note

The new `kunwu_copper` item intentionally uses a lightweight model that points at the vanilla copper ingot texture; no dedicated PNG, block/ore resource, loot table, worldgen, shop stock, GUI asset, packet, entity, capability, or runtime data loader was added. It unblocks material resolution for Kunwu-heavy artifact refinement paths such as Qingning Mirror, Silver Giant Sword, Black Gold Shield, Hunyuan replica, and related Foundation/Core Formation plans when the remaining source materials are already mapped. Source-accurate Kunwu mining, region loot, dedicated art, shop placement, JEI presentation, and full refinement-workstation resources remain deferred.

## 0.1.305 Ironwood and Qingye refinement resource note

The new `ironwood` item intentionally uses a lightweight model that points at the vanilla stripped dark oak log texture; no dedicated PNG, block/log resource, loot table, worldgen, shop stock, GUI asset, packet, entity, capability, or runtime data loader was added. The old Qingye fan bamboo stand-in is replaced in the shipped recipe by the real `seeking_immortals:ironwood` carrier, but survival acquisition, exact `ironwood_log` / `ironwood_heart` variants, Extreme West/Thousand Bamboo node placement, dedicated art, JEI presentation, and full refinement-workstation resources remain deferred.

## 0.1.304 Formation array Patchouli guide resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, worldpack data files, or runtime data loaders were added. The guide slice reuses existing Patchouli book resources, the current `spirit_gathering_array` icon/block/recipe, Wuyue Hall `technique_manual_formation` shop hook, and shipped formation-related technique hooks. Dedicated array disk/flag items, deployment UI/resources, persistent zone visuals, blueprint loot, formation-break effects, JEI recipe polish, and live visual smoke checks remain deferred.

## 0.1.305 Yin body protection charm resource note

The new `yin_body_protection_charm` intentionally reuses the existing Armor Talisman texture/model style as placeholder art and reuses already registered carriers (`talisman_paper_mortal`, `yin_stone`, `soul_fragment`) for its first recipe. No new dedicated textures, blocks, loot tables, GUI assets, packets, entities, shop data, alchemy data, worldpack data files, dimension resources, capabilities, or runtime data loaders were added. Dedicated 避阴符 art, mid-tier `yin_protect_talisman`, `ghost_hide_talisman`, Yin Luo Hall shop data, underworld loot placement, route quest assets, and live visual smoke checks remain deferred.

## 0.1.299 Beast/puppet Patchouli guide resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, worldpack data files, or runtime data loaders were added. The guide slice reuses existing Patchouli book resources, the current `spirit_beast_bridle` icon, shipped Yuling/Extreme West/Thousand Bamboo hooks, existing beast/puppet material and manual carriers, and current cultivation technique data. Dedicated beast models/animations, puppet models, contract UI assets, assembly-station resources, spawn/loot resources, quest assets, and live visual smoke checks remain deferred.

## 0.1.297 Yin underworld ambient hazard resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, alchemy data, worldpack data files, dimension resources, or runtime data loaders were added. The runtime slice reuses existing Yinming/Nether River pocket dimensions, worldpack region/secret-realm/daily-event state, vanilla effects, and current cultivation sync. Dedicated Yin miasma visuals/audio, Yin-protection mitigation items, ghost NPC/entity resources, underworld terrain/structure resources, route UI assets, and live visual smoke checks remain deferred.

## 0.1.298 Technique learning requirements guide resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, worldpack data files, or runtime data loaders were added. The guide slice reuses existing Patchouli book resources, the current `technique_manual_common` icon, shipped technique/manual resources, and current server-authoritative manual/release paths. Dedicated locked-gate icons, learning UI assets, source NPC resources, quest resources, and stricter prerequisite display assets remain deferred.

## 0.1.298 Dark/light projectile resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entity registrations, shop data, alchemy data, worldpack data files, or runtime data loaders were added. The spell slice reuses the existing custom `CultivationFireballEntity` renderer, SynchedEntityData element id, and DustParticle trail/impact pattern. Dedicated dark/light projectile textures, spell icons, manual art, richer emitter curves, source-accurate prerequisite presentation, and live-world visual smoke checks remain deferred.

## 0.1.295 Elemental area spell guide resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, worldpack data files, or runtime data loaders were added. The guide slice reuses existing Patchouli book resources, the current `spirit_charm` icon, shipped technique data, and the `ElementalAreaSpell` runtime. Dedicated spell icons, custom particle assets, manual art, stricter prerequisite display assets, and live visual smoke checks remain deferred.

## 0.1.293 Patchouli sect contribution rank-gate guide resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, entities, player-data fields, shop loaders, or runtime data loaders were added. The guide slice reuses the existing Patchouli book path, the sect-secret Foundation Pill formula icon, current contribution-shop data, and current localized purchase feedback. Dedicated shop-lock icons/text, sect NPC resources, quest resources, stricter quota displays, and source-accurate stock placement remain deferred.

## 0.1.291 Low storage bracelet runtime resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, worldpack data files, or runtime data loaders were added. The storage slice reuses the existing `storage_bracelet_low` artifact carrier/model/localization, existing artifact JSON, vanilla `ItemStack` NBT serialization, current `PlayerCultivation` realm state, and Patchouli guide path. Graphical storage screens/menus, Curios storage-slot policy, refinement workstation outputs, source-accurate loot/shop/NPC/quest placement, dedicated bracelet art, and live inventory smoke checks remain deferred.

## 0.1.290 9x9 realm gate resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, entities, player-data fields, worldpack data files, or runtime data loaders were added. The larger portal slice reuses the existing Spirit Gathering Array block, Spirit Ore block/model/texture, current worldpack teleport service, and vanilla particles/sounds. Dedicated portal/ferry art, high-tier frame materials, route UI, FTB/quest presentation, Yin underworld terrain dressing, ghost NPC resources, and live travel smoke checks remain deferred.
## 0.1.289 Contribution shop rank-gate resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, entities, player-data fields, shop loaders, or runtime data loaders were added. The rank-gate slice reuses existing `QuestProgress` sect stages, `SectContributionService`, current shop JSON, and current localization resources. Deferred surfaces remain: realm/reputation/monthly/per-player gates, visual lock text in the sect UI, source-accurate NPC/quest stock placement, and stricter alchemy/sect privilege semantics.

## 0.1.284/0.1.285 Artifact shop acquisition resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, entities, or runtime data loaders were added. The shop slice reuses the existing exact `flying_sword_low` and stackable `artifact_repair_kit` item carriers, existing placeholder models, and existing `ShopService` stock fields, complementing the current artifact integrity/repair-kit runtime. Dedicated artifact art, repair/refinement workstation resources, rank-gate presentation, NPC/quest placement, storage UI/resources, Wanbao auction/appraisal resources, and live economy balance checks remain deferred.

## 0.1.283 Artifact repair/integrity resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, entities, shop data, container/menu classes, capabilities, worldpack data files, or runtime data loaders were added. The repair slice reuses exact `ArtifactCatalogItem` carriers, existing artifact JSON, vanilla NBT on item stacks, current cultivation sync, and existing placeholder artifact models. True storage bracelet inventory, refinement workstation resources, repair/refinement visuals, Wanbao auction/appraisal assets, source-accurate placement resources, dedicated artifact art, and live in-game smoke checks remain deferred.

## 0.1.275 P0/P1 artifact exact carrier resource note

The new P0/P1 artifact carrier models intentionally reuse existing/vanilla placeholder models: swords use vanilla sword models, defense/bracelet/pendant items use vanilla ingot/nugget/gem-style models, movement/sail/bridle/whip items use vanilla elytra/saddle/lead-style models, and talisman treasures use vanilla paper/gold-style models. No dedicated textures, recipes, loot tables, GUI assets, packets, entities, shop data, worldpack data files, Curios slot behavior, artifact activation runtime, refinement workstation resources, auction UI assets, or runtime loaders were added. Dedicated artifact art, exact category visuals, equipment-slot UI, refinement outputs, talisman-treasure resources, Wanbao auction/appraisal resources, and source-accurate placement resources remain deferred.

## 0.1.277 Pressure Resist Pill runtime resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, entities, shop data, alchemy data, worldpack data files, or runtime data loaders were added. The runtime slice reuses the existing Pressure-Resist Pill item/model/localization, existing alchemy recipe/formula chain, current `CatalogPillItem` framework, vanilla potion effects, and active secret-realm state. Dedicated pressure visuals, pressure-resist charm behavior/resources, Diyuan moss gathering resources, NPC/loot/quest resources, FTB Quests chapter routing, and deeper pressure-wave balancing remain deferred.

## 0.1.277 Custom elemental projectile resource note

No new placeholder item/block textures, item models, recipes, loot tables, GUI assets, packets, shop data, alchemy data, worldpack data files, or runtime data loaders were added. The spell slice removes the previous rendered-item fireball look by drawing a custom glowing projectile core in code and uses colored spiritual-dust particles for FIRE/WATER/METAL trails and impacts. Dedicated custom particle types, projectile textures, spell icons, manual art, live-world visual smoke checks, and richer Ars/Iron's-style emitter curves remain deferred.

## 0.1.273 Yin underworld pocket dimensions resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, entities, shop data, alchemy data, or runtime data loaders were added. The Yin underworld dimension slice ships only data-pack dimension/dimension_type resources and reuses the existing Spirit Gathering Array / Spirit Ore framed portal platform generator for default arrivals. Dedicated Yinming/Nether River terrain assets, structures, mobs, ghost NPC art, Yin Luo shop data, route UI, quest resources, Patchouli entries, and in-game visual smoke checks remain deferred.

## 0.1.267 Spirit Realm portal corner-frame resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, entities, shop data, alchemy data, worldpack data files, or runtime data loaders were added. The framed portal slice reuses the existing Spirit Gathering Array block, existing Spirit Ore block/model/texture, existing Alliance Merit Token fee behavior, vanilla particles/sounds, and the current worldpack teleport service. Dedicated portal art, high-tier frame materials, route UI, quest resources, Tianyuan merit reward-source resources, FTB Quests chapter routing, and in-game visual smoke checks remain deferred.

## 0.1.267 Basic spell effects current-tree resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, entities, shop data, alchemy data, worldpack data files, or runtime data loaders were added by this current-tree reconciliation. The spell-effect work reuses existing projectile entities, vanilla particles/sounds, current localized message resources, and the existing `SkillEffect` release path. Dedicated elemental projectiles/particles, spell icons, manual art, quest resources, and source-accurate acquisition presentation remain deferred.

## 0.1.265 Basic spell effects resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, entities, shop data, alchemy data, worldpack data files, or runtime data loaders were added. The spell-effect slice reuses existing projectile entities, vanilla particles/sounds, current localized message resources, and the existing `SkillEffect` release path. Dedicated elemental projectiles/particles, spell icons, manual art, quest resources, and source-accurate acquisition presentation remain deferred.

## 0.1.269/0.1.271 P0 artifact carrier resource note

The new P0 artifact carrier models intentionally reuse existing/vanilla placeholder textures and were reverified on the current shared `0.1.271` tree: Cloud-Treading Boots use vanilla leather boots, Spirit-Gathering Bead uses vanilla ender pearl, and Artifact Repair Kit uses vanilla iron ingot. No dedicated textures, recipes, loot tables, GUI assets, packets, entities, shop data, worldpack data files, or runtime loaders were added. Dedicated artifact art, exact movement/anti-illusion/repair visuals, refinement workstation resources, auction UI assets, and source-accurate placement resources remain deferred.

## 0.1.267 Artifact catalog command resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, entities, shop data, alchemy data, worldpack data files, or runtime loaders were added. The command slice reuses the shipped `data/seeking_immortals/artifacts/` JSON resources and current command framework only. Dedicated artifact art, refinement workstation resources, auction UI assets, loot/shop placement resources, and category-specific item models remain deferred.

## 0.1.261 Pressure Resist Pill formula resource note

No new placeholder textures, blocks, recipes, loot tables, GUI assets, packets, entities, worldpack data, pressure runtime mechanics, account systems, or runtime data loaders were added. The new formula item reuses the existing sect-secret formula texture/model convention and the existing `AlchemyFormulaItem` path, while the acquisition hook reuses `danxia_valley_contribution_hall`. Dedicated formula art, Diyuan-specific acquisition resources, pressure mitigation visuals/effects, NPC resources, FTB Quests chapter routing, and source-accurate loot/shop placement remain deferred.

## 0.1.259 Spirit Realm large portal array resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, entities, shop data, alchemy data, worldpack data files, or runtime data loaders were added. The enlarged portal slice reuses the existing Spirit Gathering Array block, existing Alliance Merit Token item/model/localization, vanilla particles/sounds, and current worldpack teleport service. Dedicated portal art, visible frame blocks, route UI, quest resources, Tianyuan merit earning/exchange resources, FTB Quests chapter routing, and in-game visual smoke checks remain deferred.

## 0.1.257 Spirit Realm Condense Pill formula resource note

No new placeholder textures, blocks, recipes, loot tables, GUI assets, packets, entities, worldpack data, account systems, or runtime data loaders were added. The new formula item reuses the existing sect-secret formula texture/model convention and the existing `AlchemyFormulaItem` path, while the acquisition hook reuses `danxia_valley_contribution_hall`. Dedicated formula art, Spirit Fengyuan-specific acquisition resources, NPC resources, FTB Quests chapter routing, and source-accurate loot/shop placement remain deferred.

## 0.1.254 Alliance Merit Token item resource note

The new Alliance Merit Token intentionally reuses the existing paper formula texture through its item model; no dedicated token art was added. No blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, worldpack data, entities, account systems, exchange services, or runtime data loaders were added. Dedicated token art, Tianyuan merit reward sources, merit accounting, exchange stock/services, NPC resources, and FTB Quests chapter routing remain deferred.

## 0.1.257 Spirit Realm default portal platform resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, entities, shop data, alchemy data, worldpack data files, or runtime data loaders were added. The destination-platform pass reuses the existing Spirit Gathering Array block, existing default dimension anchor data, and current worldpack teleport service. Dedicated portal art, richer generated gateway structures, route UI, quest resources, FTB Quests chapter routing, and in-game visual smoke checks remain deferred.

## 0.1.251 Spirit Realm Condense Pill alchemy resource note

No new placeholder textures, models, items, blocks, loot tables, GUI assets, packets, entities, shop data, worldpack data, or runtime data loaders were added. The new alchemy recipe reuses the existing Spirit Realm Condense Pill item/model/localization from 0.1.250, the Fengyuan Clan Ginseng material carrier from 0.1.248, and the existing data-driven alchemy recipe loader. Dedicated pill art, formula/manual item acquisition, Spirit Fengyuan gathering resources, pill consumption/effect resources, NPC/shop/loot resources, and FTB Quests chapter routing remain deferred.

## 0.1.247 Pressure Resist Pill alchemy resource note

No new placeholder textures, models, items, blocks, loot tables, GUI assets, packets, entities, shop data, worldpack data, or runtime data loaders were added. The new alchemy recipe reuses the existing Pressure Resist Pill item/model/localization from 0.1.244, the Diyuan Pressure Moss material carrier from 0.1.243, and the existing data-driven alchemy recipe loader. Dedicated pill art, formula/manual item acquisition, Diyuan moss gathering resources, charm crafting resources, pressure/no-fly mitigation visuals, NPC/shop/loot resources, and FTB Quests chapter routing remain deferred.

## 0.1.248 Diyuan no-fly current-tree verification resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, entities, shop data, alchemy data, worldpack data files, or runtime data loaders were added by this verification. It preserves the existing worldpack `no_fly` tag behavior, the zh_cn/en_us pressure-suppression message keys, and all parallel 0.1.247/0.1.248 resource work. Dedicated pressure visuals, pressure-resist pill/charm effect resources, layer hazards, NPC resources, quest resources, and FTB Quests chapter routing remain deferred.

## 0.1.252 Spirit Realm DimensionType resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, entities, shop data, alchemy data, worldpack data files, or runtime data loaders were added. The dimension-type slice adds only shipped data-pack DimensionType JSON for `seeking_immortals:tianyuan` and `seeking_immortals:spirit_fengyuan`, and updates the two existing dimension JSON files to reference them. Dedicated portal/dimension art, generated gateway structures, destination platform structures, route UI, quest resources, FTB Quests chapter routing, and in-game visual smoke checks remain deferred.

## 0.1.246 Diyuan no-fly runtime resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, entities, shop data, alchemy data, worldpack data files, or runtime data loaders were added. The runtime rule reuses the existing worldpack `no_fly` tag, `PlayerCultivation` secret-realm state, `FlyingAuthority`, and current flying spell active keys. Only zh_cn/en_us pressure-suppression message keys were added. Dedicated pressure visuals, pressure-resist pill/charm effect resources, layer hazards, NPC resources, quest resources, and FTB Quests chapter routing remain deferred.

## 0.1.245 Diyuan Permit entry consumption resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, entities, shop data, alchemy data, worldpack data files, or runtime data loaders were added. The entry rule reuses the existing `seeking_immortals:diyuan_permit` item/model/localization, existing worldpack secret-realm ticket consumption path, existing anchors/cooldowns, and current Patchouli Tianyuan guide entries. Dedicated permit art, Tianyuan merit exchange UI, permit vendor/reward resources, Diyuan quest resources, pressure/no-fly visuals/effects, NPC resources, and FTB Quests chapter routing remain deferred.

## 0.1.247 Spirit Gathering Array portal structure resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, entities, shop data, alchemy data, worldpack data files, or runtime data loaders were added. The portal structure pass reuses the existing Spirit Gathering Array block, existing worldpack portal route, vanilla particles/sounds, and zh_cn/en_us language resources. Dedicated portal block/art, generated gateway structures, destination-platform structures, route UI, quest resources, FTB Quests chapter routing, and in-game visual smoke checks remain deferred.

## 0.1.243 Wind Feather Raft final verification resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, entities, shop data, alchemy data, worldpack data files, or runtime data loaders were added by the final `0.1.243` verification. It preserves the existing Wind Feather Raft Ticket item/model/localization, adds or preserves the dedicated missing travel-ticket lang prompt, and keeps dedicated raft route UI, route NPC resources, ticket vendor/exchange resources, route particles/sounds, quest resources, FTB Quests chapter routing, and dedicated art deferred.

## 0.1.242 Wind Feather Raft route version recheck resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, entities, shop data, alchemy data, worldpack data files, or runtime data loaders were added by the `0.1.242` version/build reconciliation. It preserves the existing Wind Feather Raft Ticket item/model/localization and server-side ticket-consumption route; dedicated raft route UI, route NPC resources, ticket vendor/exchange resources, route particles/sounds, quest resources, FTB Quests chapter routing, and dedicated art remain deferred.

## 0.1.242 Wind Feather Raft ticket travel reconciliation note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, entities, shop data, alchemy data, worldpack data files, or runtime loaders were added. The current route behavior reuses the existing Wind Feather Raft Ticket item/model/localization, existing worldpack anchors, existing inventory ticket consumption, existing portal-array fallback, and Patchouli Tianyuan guide pages. Dedicated raft NPCs, vendor/exchange assets, fee/duration/risk presentation, route particles/sounds, quest resources, FTB Quests chapter routing, and route-specific art remain deferred.

## 0.1.241 Wind Feather Raft ticket travel resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, entities, shop data, alchemy data, or runtime data loaders were added. The travel rule reuses the existing Wind Feather Raft Ticket item/model/localization, current worldpack travel anchors, the existing portal-array path, and Patchouli Tianyuan entries. Dedicated raft route UI, route NPC resources, ticket vendor/exchange resources, route particles/sounds, broader route art, quest resources, and FTB Quests chapter routing remain deferred.

## 0.1.238 Wind Feather Raft ticket item resource note

The new Wind Feather Raft ticket item intentionally reuses the existing paper formula texture through its item model; no dedicated ticket art was added. No blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, worldpack data, alchemy data, entities, or runtime data loaders were added. Dedicated raft-ticket art, Tianyuan merit exchange UI, route-gate resources, NPC resources, ticket-consumption feedback, and FTB Quests chapter routing remain deferred.

## 0.1.239 Wind Feather Raft ticket Patchouli guide sync resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, entities, shop data, worldpack data, alchemy data, or runtime data loaders were added beyond the build-verified 0.1.238 ticket item carrier. The guide sync reuses existing Patchouli Tianyuan entries plus the paper-texture `seeking_immortals:wind_feather_raft_ticket` model. Dedicated ticket art, Tianyuan merit exchange UI, Wind Feather Raft route UI, ticket-consumption resources, route NPC resources, quest resources, and FTB Quests chapter routing remain deferred.

## 0.1.237 Diyuan Permit Patchouli guide sync resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, entities, shop data, worldpack data, alchemy data, or runtime data loaders were added. The guide sync reuses the existing Patchouli Tianyuan City entries and the build-verified `seeking_immortals:diyuan_permit` item/model from 0.1.236. Dedicated permit art, Tianyuan merit exchange UI, entry-consumption resources, pressure/no-fly visuals, NPC resources, quest resources, and FTB Quests chapter routing remain deferred.

## 0.1.233 Star Palace island-market tax pricing resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, entities, shop protocol files, worldpack data files, alchemy data, or runtime data loaders were added. The tax-pricing slice reuses the existing tax receipt item/model, PlayerCultivation/QuestProgress paid-tax flag, `WorldpackGameplayService` market-cost modifier, and `ShopService` adjusted-cost path for `chaotic_sea_island_general` and `outer_sea_public_stall`. Dedicated paid-tax UI/status art, ferry/tax quest resources, Star Palace NPC resources, reputation presentation, and source-accurate pearl-economy reward tables remain deferred.

## 0.1.235 Spirit Realm aura profile resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, entities, shop files, worldpack data files, alchemy data, or runtime data loaders were added. The aura-profile slice reuses the existing custom dimension JSON resources, Spirit Gathering Array multiblock portal prerequisite, aura detector/HUD sync paths, and current cultivation/meditation aura formulas. Dedicated portal art, activation particles/sounds, custom DimensionType resources, permit/ticket UI, generated gateway structures, and in-game runtime smoke checks remain deferred.

## 0.1.231 Star Palace tax receipt use resource note

No new placeholder textures, models, blocks, recipes, loot tables, GUI assets, packets, entities, shop protocol files, worldpack data, alchemy data, or runtime data loaders were added. The receipt use slice reuses the existing paper-formula-based receipt model, current item stack, existing PlayerCultivation/QuestProgress NBT persistence, and translatable tooltips/messages. Dedicated receipt art, tax-paid status UI, ferry/tax quest resources, NPC resources, and FTB Quests chapter routing remain deferred.

## 0.1.230 Star Palace tax receipt shop-stock resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, worldpack data, alchemy data, entities, or runtime data loaders were added. The shop-stock follow-up reuses the existing Star Palace tax receipt item/model from 0.1.229, the current Chaotic Sea island general shop data, low-grade metal spirit-stone currency, and ShopService presentation. Dedicated receipt art, consumption effects, tax-paid UI, ferry/tax quest resources, NPC resources, and FTB Quests chapter routing remain deferred.

## 0.1.230 Star Palace tax receipt shop reconciliation resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, entities, or runtime data loaders were added by this reconciliation. The Chaotic Sea island shop entry reuses the registered `seeking_immortals:star_palace_tax_receipt`, existing metal spirit-stone currency, and current `ShopService` display/packet flow. Dedicated receipt art, receipt use effects, tax-paid state resources, ferry/tax quest presentation, NPC resources, and source-accurate reward tables remain deferred.

## 0.1.229 Star Palace tax receipt item resource note

The new Star Palace tax receipt item intentionally reuses the existing paper formula texture through its item model; no dedicated receipt art was added. No blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, worldpack data, entities, or runtime data loaders were added. Dedicated tax receipt art, shop stock presentation, consumption effects, ferry/tax quest resources, NPC resources, and FTB Quests chapter routing remain deferred.

## 0.1.227 Tianyuan merit Patchouli resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, worldpack data, entities, or runtime data loaders were added. The Tianyuan merit guide entries reuse the existing Patchouli book path, `seeking_immortals:immortal_jade` icon, shipped Tianyuan worldpack region/dimension/portal prerequisites, and current guidebook presentation. Dedicated alliance-merit UI, Diyuan permit/ticket items, pressure-resistance charm assets/effects, Tianyuan NPC resources, exchange screens, and FTB Quests chapter resources remain deferred.

## 0.1.225 Patchouli guide compatibility resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, worldpack data, entities, or runtime data loaders were added. The guide fix reuses the existing Patchouli book content and adds a temporary `en_us` fallback mirror of the current JSON category/entry tree; polished English guide translation remains deferred.

## 0.1.225 Dimension portal rules resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, entities, or runtime data loaders were added by this verification/docs pass. The portal route reuses the existing Spirit Gathering Array block as a 3x3 same-level multiblock, current worldpack region tags, current dimension JSON resources, and the existing server-authoritative travel service. Dedicated portal block/art, activation particles or sound, custom dimension type/aura-nature resources, generated gateway structures, permit/cost items, and richer destination structures remain deferred.

## 0.1.229 Outer Sea shard stock aggregate resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, entities, or runtime data loaders were added by the shard-stock slice. The Outer Sea public-stall shard entry reuses the registered `seeking_immortals:spirit_stone_shard`, existing metal spirit-stone currency, current `ShopService` display/packet flow, and Patchouli guide resources. Reverse exchange UI, pearl tax receipt shop/use resources, ferry/tax quest presentation, public-stall NPC resources, pearl processing, and source-accurate reward tables remain deferred.

## 0.1.223 Great Jin Central Patchouli guide resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, worldpack data, entities, or runtime data loaders were added. The Great Jin Central guide entry reuses the existing Patchouli book path, `seeking_immortals:immortal_jade` icon, shipped `great_jin_central` worldpack region data, shipped `great_jin_auction_week` daily-event data, and current command/GUI data flow. Dedicated Wanbao auction UI, invitations, lot resources, ancient-artifact appraisal assets, clan reputation UI, cross-region array resources, refinement-hall service UI, NPC resources, and FTB Quests chapter resources remain deferred.

## 0.1.222 Human clan league Patchouli guide resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, worldpack data, entities, or runtime data loaders were added. The human-clan league guide entry reuses the existing Patchouli book path, `seeking_immortals:spirit_grass` icon, shipped `spirit_fengyuan` worldpack region data, and current command/GUI data flow. Dedicated clan NPC resources, clan reputation UI, alliance or marriage gate resources, specialty shop assets, family quest resources, Dajin clan-feud presentation, reward tables, and FTB Quests chapter resources remain deferred.

## 0.1.220 Star Palace City merit-hall Patchouli guide resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, worldpack data, entities, or runtime data loaders were added. The Star Palace City guide entry reuses the existing Patchouli book path, `seeking_immortals:essence_condensing_pill` icon, shipped `star_palace_city` worldpack region data, current `WorldpackDataService` command/GUI flow, existing `ShopService` contribution-shop data, Essence Condensing Pill/formula carriers, Soul Gathering formula carrier, and Calming Pill/formula carriers. Dedicated teleport-permit items, patrol seal/order items, Void Palace map fragments, auction UI/resources, patrol-board resources, Star Palace NPCs, reputation UI, sea-event rewards, and FTB Quests chapter resources remain deferred.

## 0.1.219 Spirit Fengyuan Patchouli guide resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, worldpack data, entities, or runtime data loaders were added. The Fengyuan guide entry reuses the existing Patchouli book path, `seeking_immortals:spirit_grass` icon, shipped `spirit_fengyuan` worldpack region data, related secret-realm hooks, and current command/GUI data flow. Dedicated clan NPC resources, clan reputation UI, specialty shop assets, Treasure Fair UI, border travel visuals, subregion gates, human-clan quest resources, and FTB Quests chapter resources remain deferred.

## 0.1.218 Yuling spirit-beast Patchouli guide resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, worldpack data, entities, or runtime data loaders were added. The guide entry reuses the existing Patchouli book path, `seeking_immortals:beast_core` icon, shipped Yuling contribution-shop data, current `ShopService` contribution flow, Spirit Grass carrier, and Beast Core carrier. Dedicated spirit-beast nurture pill art/effects, feed resources, contract UI, beast entities, GeckoLib animations, source drop tables, and FTB Quests chapter resources remain deferred.

## 0.1.217 Tianyuan City Patchouli resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, worldpack data, entities, or runtime data loaders were added. The Tianyuan City guide entry reuses the existing Patchouli book path, `seeking_immortals:immortal_jade` icon, shipped `tianyuan` worldpack region data, and current command/GUI data flow. Dedicated Tianyuan city art, NPC resources, merit exchange UI, spirit-realm gate visuals, Wind Feather Raft assets, Diyuan permit items, guard reputation UI, siege reward tables, and FTB Quests chapter resources remain deferred.

## 0.1.216 Star Palace patrol supply Patchouli resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, worldpack data, entities, or runtime data loaders were added. The Star Palace guide entry reuses the existing Patchouli book path, Armor Talisman icon, metal spirit-stone currency, shipped patrol-supply data, Armor Talisman carrier, and Fire Talisman carrier. Dedicated anti-demon visuals/effects, Bu Tian Pill assets/effects, Star Palace reputation UI, patrol-board resources, quota presentation, and exact reward tables remain deferred.

## 0.1.215 Barbarian seven kings Patchouli guide resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, worldpack data, entities, or runtime data loaders were added. The guide entry reuses the existing Patchouli book path, `seeking_immortals:immortal_jade` icon, shipped Barbarian Wasteland secret-realm hooks, and current worldpack command/GUI data flow. Dedicated demon-king art/entities, GeckoLib animations, territory structures, king-token resources, tribute-trade UI, council-audience resources, and source-accurate reward tables remain deferred.

## 0.1.213 Inverse Star black market Patchouli resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, worldpack data, entities, or runtime data loaders were added. The Inverse Star guide entry reuses the existing Patchouli book path, Beast Core icon, metal spirit-stone currency, shipped black-market data, Calming Pill carrier, and Calming Pill jade formula carrier. Dedicated Demon Heart Pill assets/effects, contraband/cipher/contact items, island deed resources, black-market NPC resources, reputation UI, risk visuals, and source-accurate reward tables remain deferred.

## 0.1.214 Mulan-Tianlan Patchouli guide resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, worldpack data, entities, or runtime data loaders were added. The guide entry reuses the existing Patchouli book path and `seeking_immortals:fire_talisman` icon as a temporary war-alert visual. Dedicated war art, fashi-array visuals, holy-beast/GeckoLib presentation, side-specific quest UI, FTB Quests chapter resources, reputation screens, and reward tables remain deferred.

## 0.1.212 Mulan-Tianlan war phase resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, Patchouli entries, entities, or runtime data loaders were added. The four war-phase hooks reuse the existing worldpack daily-event schema, current `mulan`/`tianlan` regions, current command/GUI display flow, and existing effect tokens. Real battlefield entities, GeckoLib-capable holy-beast/fashi presentation, side-specific quest UI, war merit/reputation resources, and source-accurate reward tables remain deferred.

## 0.1.211 Outer Sea public stall Patchouli resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, worldpack data, entities, or runtime data loaders were added. The Outer Sea guide entry reuses the existing Patchouli book path, Spirit Grass icon, metal spirit-stone currency, and shipped shop data. Registered shard/pearl assets, tax receipts, public-stall NPC resources, ferry/tax quest UI, stock visibility gates, and source-accurate pearl-economy resources remain deferred.

## 0.1.210 Tiannan demonic market Patchouli resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, worldpack data, entities, or runtime data loaders were added. The demonic-market guide entry reuses the existing Patchouli book path, Cultivation Pill icon, metal spirit-stone currency, and shipped shop data. Dedicated Dual Harmony Pill, Demonic Yang-gathering Pill, demonic blood coral assets, demonic reputation UI, risk visuals, Hehuan routing, and source-accurate market/NPC resources remain deferred.

## 0.1.209 Qingxin Patchouli final verification resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, worldpack data, entities, or runtime data loaders were added by the final verification pass. The current Qingxin guide entry still reuses the existing Patchouli book, Qingxin Pill icon/model/texture, and data-driven alchemy recipe; dedicated Qingxin art, heart-demon visuals, quality variants, and stricter acquisition resources remain deferred.

## 0.1.209 Qingxin Pill Patchouli build-verification resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, worldpack data, entities, or runtime data loaders were added by the final verification. The Qingxin guide entry reuses the existing Patchouli book path, Qingxin Pill item, paper formula, and shipped alchemy recipe. Dedicated Qingxin item art, formula art, independent heart-demon visuals/effects, source-accurate formula distribution, JEI/Patchouli recipe-link polish, and quality-specific variants remain deferred.

## 0.1.208 Tiannan demonic dual market build-verification resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, alchemy data, entities, Patchouli entries, or runtime data loaders were added by the verification pass. The current demonic-dual market hook still reuses existing Cultivation Pill and metal spirit-stone assets through `ShopService`; dedicated Dual Harmony Pill, demonic Yang-gathering pill, demonic blood coral, demonic reputation UI, risk visuals, Hehuan routing, and source-accurate market/NPC resources remain deferred.

## 0.1.207 Tiannan demonic dual market resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, alchemy data, entities, Patchouli entries, or runtime data loaders were added. The Tiannan demonic-dual market backfill reuses existing Cultivation Pill and metal spirit-stone assets through current shop data. Dedicated Dual Harmony Pill, demonic Yang-gathering pill, demonic blood coral, demonic reputation UI, risk visuals, and source-accurate reward/shop resources remain deferred.

## 0.1.206 Region-card daily-event exact-id resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, entities, or runtime data loaders were added. The exact region-card daily-event backfill reuses the existing worldpack daily-event schema, current regions, current effect tokens, and command/GUI display flow. Real NPCs, encounter entities, spatial-rift visuals, demon-king intrusion assets, patrol mission UI, clan reputation screens, and source-accurate reward tables remain deferred.

## 0.1.203 Qixuan Village stall Patchouli resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, worldpack data, alchemy data, or runtime data loaders were added. The new Patchouli entry reuses the existing guide book, Mortal Martial Manual icon, Qixuan stall shop data, and current item carriers. Dedicated guide art, quest-page routing through FTB Quests, village NPC presentation, and real mundane medicine/herb bundle assets remain deferred.

## 0.1.204 Outer Sea public stall + Qixuan Patchouli aggregate resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, worldpack data, alchemy data, or runtime data loaders were added by the aggregate. The Outer Sea public stall shop reuses existing Spirit Grass, low-grade metal spirit-stone currency, and the current `ShopService` display/packet flow; the parallel Qixuan Patchouli entry reuses the mandatory Patchouli guide path. Dedicated pearl/shard items, public stall art/NPC assets, ferry/tax quest UI, tax receipt resources, and exact pearl-market reward resources remain deferred.

## 0.1.202 Qixuan Village stall resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, worldpack data, alchemy data, or runtime data loaders were added. The Qixuan stall shop reuses existing Spirit Grass, low-quality Healing Pill, Mortal Martial Manual, low-grade metal spirit-stone currency, and current `ShopService` display/packet flow. Dedicated village-stall art, mundane medicine/herb bundle items, Seven Mysteries quest UI, NPC placement assets, and stricter stock-gate presentation remain deferred.

## 0.1.201 spirit-rain meditation multiplier resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, shop data, alchemy data, worldpack JSON, packets, regions, secret realms, or runtime data loaders were added. This slice reuses existing worldpack daily-event ids and player cultivation state to apply meditation cultivation multipliers. Dedicated rain visuals, event icons/descriptions for cultivation-exp multipliers, multi-day event UI, reward tables, and event-specific ambience remain deferred.

## 0.1.201 Spirit rain bonus resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, shop data, alchemy data, regions, secret realms, or runtime data loaders were added. The spirit-rain pass reuses existing worldpack daily-event data, server effect math, client text rendering, and language files only. Dedicated rain visuals, particles, weather ambience, Patchouli art/text, and stricter meditation-exp presentation remain deferred.

## 0.1.201 Qinglan Huangfeng stock resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop loaders, alchemy runtime data, worldpack data, or runtime data loaders were added. The Qinglan contribution backfill reuses existing Foundation Building Pill, alchemy formula, and Flying Sword item assets and behavior. Exact Huangfeng rank/monthly gates, source-specific formula identities, dedicated low-tier flying-sword progression, and stricter sect access UI remain deferred.

## 0.1.197 version-sync resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, worldpack data, regions, secret realms, or runtime data loaders were added by this recheck. Resource debt remains the same as the verified 0.1.196 aggregate and 0.1.195 Chaotic Sea anti-demon market slice.

## 0.1.198 Inverse Star demon-heart resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, worldpack data, alchemy runtime data, regions, secret realms, or runtime data loaders were added. The Inverse Star `demon_heart_pill` and `recipe_demon_heart` market backfill reuses the existing Calming Pill item, model, texture, behavior, and Calming Pill jade formula carrier as the closest demon-qi/heart-stabilizing equivalents. Dedicated Demon Heart Pill resources/effects, demon-qi resistance visuals, black-market access UI, contraband/cipher goods, and exact reward/risk tables remain deferred.

## 0.1.196 Barbarian king territories resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, regions, daily events, or runtime data loaders were added. The Barbarian King territory backfill reuses the existing worldpack secret-realm schema, `barbarian_wasteland` region, `immortal_jade` ticket item, and current command/GUI display flow. Real demon-king visuals/entities, GeckoLib animations, territory structures, token items, tribute-trade UI, quest presentation, and dedicated loot tables remain deferred.

## 0.1.196 Remaining daily-random exact-id resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, regions, secret realms, or runtime data loaders were added. The remaining daily-random exact-id aggregate reuses the existing worldpack daily-event schema, existing regions, and current effect tokens. Real spirit-rain visuals/effects, merchant NPC assets, rogue encounter entities, sect recruitment UI, demon-qi hazard visuals, beast migration spawns, and dedicated reward tables remain deferred.

## 0.1.195 Chaotic Sea island anti-demon resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, worldpack data, alchemy data, regions, secret realms, or runtime data loaders were added. The Chaotic Sea island `anti_demon_talisman` market backfill reuses the existing `fire_talisman` item, model, texture, and behavior as the closest demon-suppressing attack carrier. Dedicated anti-demon visuals/effects, sea-route demon encounter assets, Star Palace reputation UI, patrol-board resources, Bu Tian Pill assets/effects, and exact reward tables remain deferred.

## 0.1.197 Daily-random exact-id worldpack resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, regions, secret realms, or runtime data loaders were added. The exact daily-event id backfill reuses the existing worldpack daily-event schema, existing regions, current effect tokens, and current command/GUI display flow. Real spirit-rain visuals/effect math, travelling merchants, bandit entities, sect recruitment screens, demon-qi hazards, beast migration spawn tables, and dedicated reward tables remain deferred.

## 0.1.194 Star Palace patrol anti-demon resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, worldpack data, alchemy data, regions, secret realms, or runtime data loaders were added. The Star Palace patrol `anti_demon_talisman` market backfill reuses the existing `fire_talisman` item, model, texture, and behavior as the closest demon-suppressing attack carrier. Dedicated anti-demon visuals/effects, Bu Tian Pill assets/effects, Star Palace reputation UI, patrol-board resources, and exact patrol reward tables remain deferred.

## 0.1.193 Tianlan scout-clash version-sync resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, regions, secret realms, or runtime data loaders were added by this version-sync verification. Resource debt remains the same as the 0.1.192 Tianlan scout-clash data hook.

## 0.1.194 Star Palace anti-demon patrol-supply resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, worldpack data, alchemy data, regions, secret realms, or runtime data loaders were added. The anti-demon patrol-supply backfill reuses the existing Fire Talisman item, model, texture, and behavior as a partial anti-demon equivalent. Dedicated anti-demon visuals/effects, Star Palace patrol UI, reputation gates, Bu Tian Pill resources, and exact patrol reward tables remain deferred.

## 0.1.192 Tianlan scout-clash event resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, regions, secret realms, or runtime data loaders were added. The Tianlan scout-clash hook reuses the existing worldpack daily-event schema, existing `tianlan` region, and current `trade_risk_up` plus `rare_loot_hint` effect tokens. Real scout NPC/entity assets, Tianlan/Mulan war-cycle UI, reputation resources, holy-beast trial routing, and dedicated reward tables remain deferred.

## 0.1.193 Dajin auction_notice exact-id event resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, regions, secret realms, or runtime data loaders were added. The exact `auction_notice` backfill reuses the existing worldpack daily-event schema, existing `dajin` region, and current `secret_realm_ticket_hint` plus `rare_loot_hint` effect tokens. Real auction screens, Wanbao NPCs, invitation/ticket items, Bu Tian Pill and high-herb lot resources, clan reputation UI, and reward tables remain deferred.

## 0.1.191 Kunwu ancient-ruins event resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, regions, secret realms, or runtime data loaders were added. The Kunwu ancient-ruins event reuses the existing worldpack daily-event schema, existing `kunwu` region, and current `secret_realm_ticket_hint` plus `rare_loot_hint` effect tokens. Real Kunwu intel quest UI, permit/map-fragment items, puppet encounter assets, seal-research rewards, and dedicated loot tables remain deferred.

## 0.1.190 Pirate raid exact-id event resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, regions, secret realms, or runtime data loaders were added. The exact `pirate_raid` event reuses the existing worldpack daily-event schema, existing `chaotic_sea` region, and current `trade_risk_up` effect token. Real pirate entities, ship/ferry visuals, combat encounters, reputation UI, patrol mission assets, and reward tables remain deferred.

## 0.1.189 Fengyuan treasure-fair exact-id event resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, regions, secret realms, or runtime data loaders were added. The exact `treasure_fair_rumor` event reuses the existing worldpack daily-event schema, existing `spirit_fengyuan` region, and current `secret_realm_ticket_hint` plus `rare_loot_hint` effect tokens. Real Treasure Fair visuals, invitation items, auction/bidding screens, NPC services, reward tables, and reputation gates remain deferred.

## 0.1.190 Pirate raid exact-id event resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, regions, secret realms, or runtime data loaders were added. The exact `pirate_raid` backfill reuses the existing worldpack daily-event schema, existing `chaotic_sea` region, and current `trade_risk_up` effect token. Real pirate NPC/entity assets, boat/ferry visuals, island-defense quest UI, dedicated loot tables, and reputation resources remain deferred; future richer work should reuse the existing prerequisite stack instead of adding one-off systems.

## 0.1.187 version-sync resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, worldpack data, alchemy runtime data, gameplay logic, packets, shop loaders, or runtime data loaders were added by this version-sync pass. The current workspace preserves the 0.1.186 Qinglan/Danxia seed-pack data and its reuse of the existing `spirit_grass` item asset.

## 0.1.189 Fengyuan treasure-fair exact-id resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, regions, secret realms, or runtime data loaders were added. The exact `treasure_fair_rumor` backfill reuses the existing worldpack daily-event schema, existing `spirit_fengyuan` region, and current `secret_realm_ticket_hint` plus `rare_loot_hint` effect tokens. Real treasure-fair UI, auction NPCs, invitation items, bidding rules, market lots, artifact-appraisal visuals, and reward tables remain deferred.

## 0.1.187 Yin corruption exact-event resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, regions, secret realms, or runtime data loaders were added. The exact `yin_corruption_warning` backfill reuses the existing worldpack daily-event schema, existing `yinming` region, and current `trade_risk_up` effect token. Real yin-corruption visuals, ambient damage/mitigation, Yin-protection items, ghost-path encounter assets, warning UI, and reward tables remain deferred.

## 0.1.186 Huangfeng/Danxia seed-pack resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, worldpack data, alchemy runtime data, gameplay logic, packets, shop loaders, or runtime data loaders were added. The Huangfeng/Danxia seed-pack backfill reuses the existing `spirit_grass` item asset and behavior in Qinglan and Danxia contribution halls. Dedicated seed-pack art/items, garden blocks, planting/growth UI, source-accurate herb composition, and contribution-shop gate resources remain deferred.

## 0.1.185 Tiannan + Luoyun shop aggregate resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, worldpack data, alchemy runtime data, gameplay logic, packets, shop loaders, or runtime data loaders were added by this aggregate. Tiannan exact source-id entries reuse existing Spirit Gathering Pill and Fasting Pill assets, and the Luoyun seed-pack backfill reuses the existing `spirit_grass` item asset and behavior. Dedicated source-specific pill behavior, seed-pack art/items, garden blocks, growth UI, and source-accurate herb composition remain deferred.

## 0.1.184 Tiannan exact pill-id market resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop loaders, worldpack data, or runtime data loaders were added. The Tiannan exact source-id entries reuse existing `spirit_gathering_pill` and `fasting_pill_low` item assets and behavior. Dedicated Spirit Condense/Bigu tuning, source-specific recipes, gate UI, and any alias cleanup remain deferred.

## 0.1.183 Star Palace patrol supply resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, worldpack data, gameplay logic, packets, or runtime data loaders were added. The patrol-supply shop reuses the existing `armor_talisman` item asset and behavior as the current `body_guard_talisman` equivalent, with title localization only. Anti-demon talisman assets/effects, Bu Tian Pill itemization, patrol-board UI, reputation gates, and exact patrol reward resources remain deferred.

## 0.1.182 Danxia + Star Palace shop aggregate resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop loaders, worldpack data, or runtime data loaders were added by this version reconciliation. The aggregate reuses already registered `alchemy_lid_mid`, `dan_fire_mid`, and `alchemy_formula_soul_gathering_pill_jade` assets and behavior. Exact alchemy-component visuals, fire-control hazards, Yanghun Pill behavior, access gates, and dedicated UI/quest surfaces remain deferred.

## 0.1.181 Star Palace Yanghun formula resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, alchemy runtime data, worldpack data, or runtime data loaders were added. The Star Palace Yanghun formula backfill reuses existing `alchemy_formula_soul_gathering_pill_jade` item assets and behavior. Exact Yanghun formula identity, dedicated Yanghun Pill soul-injury behavior, Star Palace access gates, and dedicated UI/quest surfaces remain deferred.

## 0.1.181 Danxia alchemy tools resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop loaders, or runtime data loaders were added by this reconciliation. The Danxia alchemy lid and Dan Fire contribution backfill reuses already registered `alchemy_lid_mid` and `dan_fire_mid` item assets and behavior. Exact alchemy-component mismatch visuals, fire-control hazards, crafting sources, and stricter sect access gates remain deferred.

## 0.1.180 Multiregion daily-event resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, regions, secret realms, or runtime data loaders were added. The multiregion daily-event hooks reuse the existing worldpack daily-event schema, existing regions, and current effect tokens. Real talisman rewards, duel entities, ghost hazards, regional reputation effects, warning UI, and reward tables remain deferred.

## 0.1.180 Tianyuan void-rift sighting resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, regions, secret realms, or runtime data loaders were added. The Tianyuan void-rift hook reuses the existing worldpack daily-event schema, existing `tianyuan` region, and current effect tokens. Real spatial-rift visuals, hazard math, patrol quests, encounter entities, realm-gated warnings, and reward tables remain deferred.

## 0.1.178 Star Palace Sea Calm formula resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, alchemy runtime data, worldpack data, or runtime data loaders were added. The Star Palace Sea Calm formula backfill reuses existing `alchemy_formula_calming_pill_jade` item assets and behavior. Exact Sea Calm paper formula identity, voyage-hazard mechanics, Star Palace access gates, and dedicated UI/quest surfaces remain deferred.

## 0.1.178 Dajin demon-qi + Star Palace Sea Calm aggregate resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, alchemy data, regions, secret realms, or runtime data loaders were added by this aggregate. The Dajin demon-qi hook reuses the existing worldpack daily-event schema, existing `dajin` region, and current effect tokens. The Star Palace Sea Calm formula backfill reuses the existing `alchemy_formula_calming_pill_jade` asset. Real demon-qi visuals, hazard math, Dajin/Fallen Demon Valley quest routing, purge resources, Sea Calm voyage systems, Star Palace gates, encounters, and reward tables remain deferred.

## 0.1.176 Worldpack event/UI aggregate resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI PNG assets, packets, shop data, alchemy data, regions, secret realms, or runtime data loaders were added. The Great Jin auction event reuses the existing worldpack daily-event schema and `great_jin_central` region. The UI effect-description pass reuses native text rendering and localization keys only; real auction screens, bidding assets, event icons, and richer daily-effect visuals remain deferred.

## 0.1.174 Diyuan pressure daily-event resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, regions, secret realms, or runtime data loaders were added. The Diyuan pressure event reuses the existing worldpack daily-event schema, existing `spirit_fengyuan` region, existing `diyuan` secret-realm context, and current effect tokens. Real pressure visuals, hazards, permit items, dungeon layers, reward tables, and dedicated entities remain deferred.

## 0.1.173 High-realm daily-event resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, regions, secret realms, or runtime data loaders were added. The high-realm event backfill reuses the existing worldpack daily-event schema and current effect tokens. Real encounter entities, hazards, NPCs, realm-gated mechanics, UI explanations, and reward tables remain deferred.

## 0.1.172 Mulan worldpack resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, secret realms, or runtime data loaders were added. The Mulan backfill reuses the existing worldpack region and daily-event schemas plus current effect tokens. Real Mulan anchors, Fashi services, holy-bird altar visuals, beast-taming resources, war-cycle encounters, and reward tables remain deferred.

## 0.1.171 Extreme West worldpack resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, daily events, secret realms, or runtime data loaders were added. The Extreme West backfill reuses the existing worldpack region schema and command/GUI display flow. Real Extreme West structures, Thousand Bamboo services, ironwood/resource nodes, puppet-core crafting, puppet tower routing UX, mechanism encounters, and reward tables remain deferred.

## 0.1.170 Great Jin Central worldpack resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, daily events, secret realms, or runtime data loaders were added. The Great Jin Central backfill reuses the existing worldpack region schema and command/GUI display flow. Real central-city structures, Wanbao auction UI, refinement-hall services, cross-region arrays, Kunwu copper/resource nodes, ancient-artifact appraisal, clan reputation, and high-tier market gates remain deferred.

## 0.1.169 Spirit event hooks resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, regions, secret realms, or runtime data loaders were added. The spirit-event hook backfill reuses the existing worldpack daily-event schema and current effect tokens. Real ghost-hunt risk, Spirit Realm storm hazards, ancient-ruin quest routing, realm gates, encounter spawns, and reward tables remain deferred.

## 0.1.168 Kunwu Mountain worldpack resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, or runtime data loaders were added. The Kunwu Mountain backfill reuses the existing worldpack region, secret-realm, and daily-event schemas plus current effect tokens. True Kunwu anchor placement, opening-cycle UI, permit/map-fragment items, cold-snap movement effects, puppet encounters, seal research hooks, and source-accurate reward tables remain deferred.

## 0.1.132 aggregate resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The aggregate build verifies the trade-route daily-event backfill and the Cangming body-guard shop-data backfill using only existing data schemas, regions, effect tokens, and registered armor_talisman resources. Route travel systems, ferry/boat visuals, smuggling/reputation mechanics, and broader Star Palace patrol goods remain deferred.## 0.1.132 Cangming patrol supply body-guard resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The Cangming patrol-supply body-guard backfill reuses the existing armor_talisman item, model, texture, and behavior. The import is intentionally partial: exact Star Palace patrol reputation gates, monthly limits, anti-demon talisman behavior, Bu Tian Pill, and dedicated patrol rewards remain deferred. Static JSON validation passed, and the aggregate 0.1.132 final Gradle build verified the merged trade-route + Cangming workspace.
## 0.1.131 worldpack trade-route resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop files, or runtime data loaders were added. The trade-route backfill reuses the existing worldpack daily-event schema, existing region ids, and existing effect tokens. The import is intentionally partial: physical travel tickets, route fees, ferry/boat behavior, smuggling reputation, convoy rewards, and route-specific encounter tables remain deferred.## 0.1.130 Danxia body-guard talisman resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The Danxia body-guard talisman backfill reuses the existing armor_talisman item, model, texture, and behavior. The import is intentionally partial: exact talisman paper/ink crafting, shield-value tuning, stock gates, and crate/manual systems remain deferred.
## 0.1.129 worldpack secret-realm resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop files, or runtime data loaders were added. The Nether River Land backfill reuses existing worldpack secret-realm schema, the existing nether_river region, and the existing water_spirit_stone_mid ticket item. The Thousand Bamboo correction reuses the existing extreme_west_thousand_bamboo region. Ferry behavior, Yin-stone currency, ghost-path drops, puppet loot tables, and dungeon structure placement remain deferred.
## 0.1.128 worldpack faction-conflict resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop files, or runtime data loaders were added. The faction-conflict backfill reuses existing worldpack daily-event schema and effect tokens. The import is intentionally partial: reputation shifts, faction-war entities, blockade services, smuggling discounts, patrol/tax encounters, and reward tables remain deferred. Focused worldpack/resource validation and final Gradle build passed under `mod_version=0.1.128`.
## 0.1.127 Cangming condensation resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The Cangming contribution backfill reuses existing `essence_condensing_pill` and `alchemy_formula_essence_condensing_pill_jade` assets and behavior. The import is intentionally partial: Star Palace reputation gates, realm gates, monthly limits, merit-account identity, and broader high-tier contribution rules remain deferred.
## 0.1.127 Huanglong pill market resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The Huanglong Pill market backfill reuses the existing cultivation_pill asset and behavior. The import is intentionally partial: exact Huanglong formula, unique effect tuning, and stricter realm/source gates remain deferred.

## 0.1.126 Tiannan refinement forge build-verified resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, or runtime loaders were added. The final build passed under `mod_version=0.1.126`; the shop reuses existing `spirit_iron` and `metal_spirit_stone` assets.
## 0.1.124 Tiannan refinement forge resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The Tiannan refinement forge backfill reuses the existing `spirit_iron` asset and behavior for both text-material iron entries. The import is intentionally partial: exact low/mid ingot tiering, refinement manuals, quench oil, forge hammer behavior, artifact injection tools, and full refinement crafting remain deferred.
## 0.1.126 Tiannan refinement forge reconciliation note

No resource debt changed from the 0.1.124 draft: no new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, or runtime loaders were added. The final version label is 0.1.126 because local ai-preflight state had already used 0.1.124.
## 0.1.126 Yuling beast-soul essence resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The Yuling Pavilion beast-soul essence backfill reuses the existing beast_core material asset and behavior. The import is intentionally partial: exact beast-soul essence identity, beast source drops, spirit-beast contracts, puppet use, and nurture/feed items remain deferred.

## 0.1.124 Tiannan refinement forge resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay systems, packets, or runtime data loaders were added. The new shop reuses existing `spirit_iron` and `metal_spirit_stone` assets and behavior. The import is intentionally partial: exact low/mid spirit-iron ingot identity, refinement manuals, quench oil, forge tools, and artifact-refining mechanics remain deferred.
## 0.1.123 chaotic sea material market resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The Chaotic Sea material backfill reuses existing `dragon_scale` and `spirit_iron` assets and behavior. The import is intentionally partial: exact jiao-scale drops, water-artifact behavior, deep-sea cold-iron identity, and broader Chaotic Sea shop systems remain deferred.
## 0.1.122 Qinglan furnace reconciliation resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. This finalizes the Qinglan furnace backfill at 0.1.122 and reuses the existing registered alchemy_furnace_tier_2 block item surface. The temporary 0.1.120 Qinglan note remains historical because the parallel Danxia batch was finalized separately as 0.1.121.

## 0.1.120 Qinglan furnace text-material resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The contribution-hall backfill reuses the existing registered `alchemy_furnace_tier_2` block item, model, texture, loot, and creative-tab coverage. Higher alchemy equipment and gated shop semantics remain deferred.

## 0.1.111 worldpack daily-event resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, or data systems were added. The new daily events reuse existing worldpack data fields and existing display-only effect tokens. Remaining risk is content tuning/manual regression rather than missing resources.

## 0.1.110 spirit-stone reference note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, or data systems were added. This pass only replaces the final active recipe/Patchouli seeking_immortals:spirit_stone item references with the registered seeking_immortals:metal_spirit_stone; generic spirit_stone language keys remain shared tooltip/message keys for the five-element spirit-stone item class.

## 0.1.106 custom fire talisman resource note

No new placeholder textures, models, GUI assets, items, blocks, recipes, loot tables, or tags were added. Fire Talisman now reuses the existing custom fireball entity and renderer from 0.1.105. Remaining original-effect debt is thunder-focused: `ThunderStrikeSpell` still uses real vanilla lightning, and `TribulationService` still uses visual-only vanilla lightning.
## 0.1.105 custom fireball resource note

No new placeholder textures, models, GUI assets, items, blocks, recipes, loot tables, or tags were added. `CultivationFireballRenderer` uses the existing mod fire-element spirit-stone item model as the projectile core and relies on flame/smoke particles for spell feedback. Remaining original-effect debt: `FireTalismanItem` still uses vanilla `SmallFireball`, `ThunderStrikeSpell` still uses real vanilla lightning, and `TribulationService` still uses visual-only vanilla lightning.
## 0.1.104 worldpack data resource note

No placeholder textures, models, GUI assets, items, blocks, recipes, loot tables, or tags were added. The expanded worldpack data uses existing runtime schemas and existing registered ticket items. The main remaining worldpack gap is content depth: OP anchor placement, manual travel/realm regression, daily-event balancing, and future generated realm/dungeon implementations.

## 0.1.102 preservation verification resource/protocol note

No placeholder textures, models, GUI assets, items, blocks, recipes, loot tables, or data resources were added or removed. Per the latest user instruction, completed Phase 10/market/worldpack packet-GUI code was preserved; the current packet surface remains protocol 9 and the review-fix audit/build passed without source deletion.
## 0.1.102 Worldpack/market resource and placeholder note

No new placeholder textures, models, GUI PNG assets, items, blocks, recipes, loot tables, or tags were added for the worldpack/market wiring request. `ShopScreen` and `WorldpackScreen` use native `GuiGraphics`/`ImmortalUiSkin` drawing. The `text-materials/data` setting catalogs remain reference and validation inputs only; runtime loaders continue to use canonical shipped `src/main/resources/data/...` JSON. Clear Void, Forget Dust, and Appearance Fixing pill species stay disabled until their future pet, memory, and appearance systems exist. This note supersedes older protocol-8 cleanup notes below.
## 0.1.102 Phase 10 protocol/resource note

No new placeholder textures, models, GUI assets, items, blocks, recipes, loot tables, or data resources were added in this final sync. Phase 10 outposts continue to use vanilla block jigsaw structures and the existing sect steward renderer; richer sect NPC and structure art remain later polish.

## 0.1.101 UI visual fix current resource/protocol note

- No placeholder textures, models, GUI assets, items, blocks, recipes, loot tables, or data resources were added.
- This request did not change packet fields, packet order, packet encoding, or packet registrations; source check shows ModNetwork.PROTOCOL_VERSION remains 8.

## 0.1.101 Phase 10 resource/build note

  No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, or data resources were added in this closure.
  Phase 10 outposts remain vanilla block jigsaw structures using the existing sect steward renderer; richer sect NPC/structure art remains later polish.

## 0.1.101 cultivation stats visual fix resource/protocol note

  No new placeholder textures, models, GUI assets, items, blocks, recipes, loot tables, or data resources were added.
  The stats screen visual fix uses native fills and existing UI colors only; it does not add PNG/UI texture debt.
  oodNetwork.PROTOCOL_VERSION remains 8; stale market/worldpack packet backed GUI classes remain outside the active source tree.

## 0.1.101 review fix resource/protocol note

  No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, creative tab entries, or data resources were added.
  Current source does not retain the stale market/worldpack packet GUI classes, client data caches, screen classes, packet registrations, or packet tests from the transient 0.1.96 0.1.99 notes.
  Patchouli optional dependency hardening is code side only: direct Patchouli API access is isolated in `compat.patchouli.PatchouliGuideBridge` and guarded by `oodCompat.PATCHOULI_LOADED`.
  The existing Phase 10 outpost resources remain vanilla block jigsaw structures with the existing sect steward renderer; richer custom NPC/structure art remains later polish.

> 0.1.94 resource note: No PNG/UI texture/resource placeholders were added or removed. The stats screen restyle uses native GuiGraphics.fill and existing ImmortalUiSkin helpers only; existing texture placeholder status is unchanged.
## 0.1.88 texture art final status

  Active model texture references under ssets/seeking_immortals/models resolve to existing PNG files.
  Active model JSON files currently have 0 references to *_placeholder.png or 	echnique_manual_placeholder.
  Unused placeholder PNG files remain only as compatibility/historical fallback assets and are not current model targets.
  Remaining art debt is optional polish: manual in game visual checks, richer Patchouli illustrations, dedicated sect NPC visuals, animated skill effects, and possible later cleanup of unused placeholder files.

## 0.1.87 texture art status

  Active model texture references under `assets/seeking_immortals/models` now resolve to existing PNG files.
  No active model JSON currently points at `*_placeholder.png`.
  The former major placeholder/reuse groups now have dedicated original pixel art resources: technique manuals, spirit stones, alchemy lids/fires/furnace tiers, formula carriers, pill quality families, quest evidence, utility tools, skill icons, and selected blocks.
  Unused `*_placeholder.png` files are retained as compatibility/historical fallback resources for now; they are no longer active model targets.
# 缺失与占位内容清�?
## 文档维护规则

后续新增或调整方块、物品、配方、掉落、Patchouli 指南、模型、贴图、交互机制时，必须同步更新：

  `project_docs/items.md`
  `project_docs/pending_requests.md`
  对应版本更新日志：`project_docs/updates/年月日_版本�?md`

## 当前仍使用占位符的内�?
  `technique_manual_*` 系列功法/术法传承物品仍有 60 个使�?`*_placeholder.png` 占位贴图，详见下方完整清单�?  技能栏目前仍是基础占位表现�?.1.34 已接入绑定槽快捷键释放�? 秒默认冷却同步和 tooltip 冷却秒数，但仍缺正式技能图标、冷却遮�?动效 UI 与完整技能效果结算�?  五行灵石本次使用程序生成�?16x16 区分色占位贴图，后续应替换为正式美术资源�?  飞剑/飞行法宝（`flying_sword`、`flying_artifact`）已�?16x16 程序占位贴图接入，后续应替换为正式美术，并补飞行粒子/音效/HUD�?  部分 Patchouli 条目仍是玩法说明型文本，尚未补齐完整图文、配方联动和进阶引导�?
## `technique_manual_*` 系列缺失正式贴图清单

> 以下条目已有可加载的占位 PNG，游戏内不会紫黑丢贴图；但都缺少正式美术贴图，需要后续替换对�?`*_placeholder.png` 或调整模型指向正式纹理�?
| 序号 | 物品 ID | 显示�?| 当前占位贴图 | 模型文件 |
|     |     |     |     |     |
| 1 | `technique_manual_ancient_demon` | 古魔传承卷轴 | `technique_manual_ancient_demon_placeholder.png` | `technique_manual_ancient_demon.json` |
| 2 | `technique_manual_ancient_demon_secret` | 古魔秘术传承卷轴 | `technique_manual_ancient_demon_secret_placeholder.png` | `technique_manual_ancient_demon_secret.json` |
| 3 | `technique_manual_ancient_demonic_skill` | 上古魔功传承卷轴 | `technique_manual_ancient_demonic_skill_placeholder.png` | `technique_manual_ancient_demonic_skill.json` |
| 4 | `technique_manual_ancient_secret_art` | 上古秘术传承卷轴 | `technique_manual_ancient_secret_art_placeholder.png` | `technique_manual_ancient_secret_art.json` |
| 5 | `technique_manual_ancient_sword_sect` | 古剑门传承卷�?| `technique_manual_ancient_sword_sect_placeholder.png` | `technique_manual_ancient_sword_sect.json` |
| 6 | `technique_manual_azure_origin_sword_art` | 青元剑诀传承卷轴 | `technique_manual_azure_origin_sword_art_placeholder.png` | `technique_manual_azure_origin_sword_art.json` |
| 7 | `technique_manual_azure_origin_sword_derivative` | 青元剑诀衍生传承卷轴 | `technique_manual_azure_origin_sword_derivative_placeholder.png` | `technique_manual_azure_origin_sword_derivative.json` |
| 8 | `technique_manual_azure_origin_sword_spirit_realm` | 青元剑诀·灵界篇传承卷�?| `technique_manual_azure_origin_sword_spirit_realm_placeholder.png` | `technique_manual_azure_origin_sword_spirit_realm.json` |
| 9 | `technique_manual_azure_origin_sword_spirit_realm_pre` | 青元剑诀·灵界篇前置传承卷�?| `technique_manual_azure_origin_sword_spirit_realm_pre_placeholder.png` | `technique_manual_azure_origin_sword_spirit_realm_pre.json` |
| 10 | `technique_manual_azure_origin_sword_support` | 青元剑诀辅助传承卷轴 | `technique_manual_azure_origin_sword_support_placeholder.png` | `technique_manual_azure_origin_sword_support.json` |
| 11 | `technique_manual_azure_origin_sword_support_skill` | 青元剑诀辅助功法传承卷轴 | `technique_manual_azure_origin_sword_support_skill_placeholder.png` | `technique_manual_azure_origin_sword_support_skill.json` |
| 12 | `technique_manual_azure_sea_true_lord_skill` | 碧海真君成名功法传承卷轴 | `technique_manual_azure_sea_true_lord_skill_placeholder.png` | `technique_manual_azure_sea_true_lord_skill.json` |
| 13 | `technique_manual_black_wind_flag_spirit` | 黑风旗器灵传承卷�?| `technique_manual_black_wind_flag_spirit_placeholder.png` | `technique_manual_black_wind_flag_spirit.json` |
| 14 | `technique_manual_brahma_sacred_fragment` | 梵圣真片传承卷轴 | `technique_manual_brahma_sacred_fragment_placeholder.png` | `technique_manual_brahma_sacred_fragment.json` |
| 15 | `technique_manual_buddhist` | 佛门传承卷轴 | `technique_manual_buddhist_placeholder.png` | `technique_manual_buddhist.json` |
| 16 | `technique_manual_chaotic_star_sea_demonic` | 乱星海魔修传承卷�?| `technique_manual_chaotic_star_sea_demonic_placeholder.png` | `technique_manual_chaotic_star_sea_demonic.json` |
| 17 | `technique_manual_chaotic_star_sea` | 乱星海传承卷�?| `technique_manual_chaotic_star_sea_placeholder.png` | `technique_manual_chaotic_star_sea.json` |
| 18 | `technique_manual_common_low` | 通用低阶传承卷轴 | `technique_manual_common_low_placeholder.png` | `technique_manual_common_low.json` |
| 19 | `technique_manual_common` | 通用传承卷轴 | `technique_manual_common_placeholder.png` | `technique_manual_common.json` |
| 20 | `technique_manual_common_tricks` | 通用小技巧传承卷�?| `technique_manual_common_tricks_placeholder.png` | `technique_manual_common_tricks.json` |
| 21 | `technique_manual_demon_domain_body_refining` | 魔域顶级炼体功传承卷�?| `technique_manual_demon_domain_body_refining_placeholder.png` | `technique_manual_demon_domain_body_refining.json` |
| 22 | `technique_manual_demon_race_secret` | 魔族秘传传承卷轴 | `technique_manual_demon_race_secret_placeholder.png` | `technique_manual_demon_race_secret.json` |
| 23 | `technique_manual_demonic` | 魔道传承卷轴 | `technique_manual_demonic_placeholder.png` | `technique_manual_demonic.json` |
| 24 | `technique_manual_evergreen_appendix` | 长春功附载传承卷�?| `technique_manual_evergreen_appendix_placeholder.png` | `technique_manual_evergreen_appendix.json` |
| 25 | `technique_manual_five_elements_escape` | 五行遁术传承卷轴 | `technique_manual_five_elements_escape_placeholder.png` | `technique_manual_five_elements_escape.json` |
| 26 | `technique_manual_formation` | 阵法类传承卷�?| `technique_manual_formation_placeholder.png` | `technique_manual_formation.json` |
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
| 37 | `technique_manual_kunpeng_red_cloud_created` | 鲲鹏族红云老祖所创传承卷�?| `technique_manual_kunpeng_red_cloud_created_placeholder.png` | `technique_manual_kunpeng_red_cloud_created.json` |
| 38 | `technique_manual_little_pole_palace` | 小极宫传承卷�?| `technique_manual_little_pole_palace_placeholder.png` | `technique_manual_little_pole_palace.json` |
| 39 | `technique_manual_lost_true_immortal_art` | 上古失传真仙术传承卷�?| `technique_manual_lost_true_immortal_art_placeholder.png` | `technique_manual_lost_true_immortal_art.json` |
| 40 | `technique_manual_mortal_martial` | 世俗武林传承卷轴 | `technique_manual_mortal_martial_placeholder.png` | `technique_manual_mortal_martial.json` |
| 41 | `technique_manual_mystic_herder_nascent_appendix` | 玄牧化婴附属传承卷轴 | `technique_manual_mystic_herder_nascent_appendix_placeholder.png` | `technique_manual_mystic_herder_nascent_appendix.json` |
| 42 | `technique_manual_mystic_yin_appendix` | 玄阴经附属传承卷�?| `technique_manual_mystic_yin_appendix_placeholder.png` | `technique_manual_mystic_yin_appendix.json` |
| 43 | `technique_manual_nangong_wan_main` | 南宫婉主修传承卷�?| `technique_manual_nangong_wan_main_placeholder.png` | `technique_manual_nangong_wan_main.json` |
| 44 | `technique_manual_nascent_soul_common` | 元婴修士通用传承卷轴 | `technique_manual_nascent_soul_common_placeholder.png` | `technique_manual_nascent_soul_common.json` |
| 45 | `technique_manual_nascent_soul_late_plus` | 元婴后期以上传承卷轴 | `technique_manual_nascent_soul_late_plus_placeholder.png` | `technique_manual_nascent_soul_late_plus.json` |
| 46 | `technique_manual_orthodox` | 正道传承卷轴 | `technique_manual_orthodox_placeholder.png` | `technique_manual_orthodox.json` |
| 47 | `technique_manual_purple_luo_mystic_skill` | 紫罗玄功传承卷轴 | `technique_manual_purple_luo_mystic_skill_placeholder.png` | `technique_manual_purple_luo_mystic_skill.json` |
| 48 | `technique_manual_self_created` | 自创传承卷轴 | `technique_manual_self_created_placeholder.png` | `technique_manual_self_created.json` |
| 49 | `technique_manual_seven_mysteries_sect` | 七玄门传承卷�?| `technique_manual_seven_mysteries_sect_placeholder.png` | `technique_manual_seven_mysteries_sect.json` |
| 50 | `technique_manual_six_paths_sage_created` | 六道极圣所创传承卷�?| `technique_manual_six_paths_sage_created_placeholder.png` | `technique_manual_six_paths_sage_created.json` |
| 51 | `technique_manual_spirit_taming_basic` | 御灵宗基础传承卷轴 | `technique_manual_spirit_taming_basic_placeholder.png` | `technique_manual_spirit_taming_basic.json` |
| 52 | `technique_manual_spirit_taming_sect` | 御灵宗传承卷�?| `technique_manual_spirit_taming_sect_placeholder.png` | `technique_manual_spirit_taming_sect.json` |
| 53 | `technique_manual_supreme_demonic` | 魔道无上传承卷轴 | `technique_manual_supreme_demonic_placeholder.png` | `technique_manual_supreme_demonic.json` |
| 54 | `technique_manual_thousand_bamboo_heritage` | 千竹教传承传承卷�?| `technique_manual_thousand_bamboo_heritage_placeholder.png` | `technique_manual_thousand_bamboo_heritage.json` |
| 55 | `technique_manual_thousand_illusion_sect` | 千幻宗传承卷�?| `technique_manual_thousand_illusion_sect_placeholder.png` | `technique_manual_thousand_illusion_sect.json` |
| 56 | `technique_manual_top_demonic` | 魔道顶阶传承卷轴 | `technique_manual_top_demonic_placeholder.png` | `technique_manual_top_demonic.json` |
| 57 | `technique_manual_true_word_sect_heritage` | 真言门传承传承卷�?| `technique_manual_true_word_sect_heritage_placeholder.png` | `technique_manual_true_word_sect_heritage.json` |
| 58 | `technique_manual_yao_bird_cultivator` | 妖族禽修传承卷轴 | `technique_manual_yao_bird_cultivator_placeholder.png` | `technique_manual_yao_bird_cultivator.json` |
| 59 | `technique_manual_yao` | 妖族传承卷轴 | `technique_manual_yao_placeholder.png` | `technique_manual_yao.json` |
| 60 | `technique_manual_yuancha_saint_ancestor` | 元刹圣祖传承卷轴 | `technique_manual_yuancha_saint_ancestor_placeholder.png` | `technique_manual_yuancha_saint_ancestor.json` |

## 当前缺失或待补齐内容

  手动突破流程已接入破境丹/药力、成功晋阶、失败回退与走火风险；成功率已接入丹药、灵�?灵眼、功法品质和执念加成。真实“地灵之眼”方�?结构、更多品质丹药注�?配方、功�?JSON `quality` 全量标注、按境界材料表、闭�?环境加成、金丹品质、天�?五衰/斩三尸仍待设计�?  六大核心属性已进入 Capability/NBT、同步和基础展示；神识探测、肉身生命加成、走火随机事件、渡劫伤害抵抗仍待接入具体玩法公式�?  五行灵石的自然生成、矿脉差异、怪物/秘境掉落规则待设计�?  变异/隐藏灵根与五行灵石增幅的映射规则待设计，例如雷、冰、风、暗、隐雷、隐暗、仙灵根�?  新增方块/物品后的模型、贴图、语言、创造栏、配方、掉落、指南与文档需要保持同步�?
## 后续要求

新增方块或物品时，至少检查并同步：注册代码、语言文件、模型文件、贴图文件或占位说明、合成配方、掉落表、创造模式物品栏、Patchouli 指南、`items.md`、`pending_requests.md` 与版本更新日志�?
## 灵气系统占位/后续扩展�?.1.22�?
  灵脉第一版为隐藏区块算法，不生成可见灵脉地形、洞府遗迹或专属矿脉�?  寻脉罗盘第一版使用文字方向和距离提示，不做客户端动态指�?模型旋转�?  秘境/仙府 10x 灵气已预留维度名判定，但尚未实装独立秘境/仙府维度与危险事件�?  下界炼体、末地法则感悟当前仅作为灵气性质标记，尚未接入独立炼体经验、法则感悟或专属突破公式�?  测灵盘、寻脉罗盘、聚灵阵当前使用程序生成 16x16 占位贴图，后续应替换为正式美术资源�?
## 0.1.27 UI 复审备注

  本次未新增贴图资源；技能栏图标仍为基于技�?ID 的原�?UI 占位色块，后续需要替换为正式技能图标�?  修仙页、技能栏、吐�?HUD 已统一�?Forge Screen/Overlay + 原生 UI 绘制工具，后续重点是正式图标、冷却与交互�?
## 0.1.28 UI 复审备注

  本次仍未新增贴图资源；技能栏图标继续使用基于技�?ID 的占位色块�?  已修复修仙页面板在极窄屏/�?GUI 缩放下的宽度与状态条越界风险，但小屏、宽屏、多 GUI Scale、JEI/XEI 同屏仍需游戏内人工验证�?  最终样式不在代码中硬定，后续应按用户选择继续优化原生分页、外置主题资源或独立 Screen 布局�?
## 0.1.29 UI 复审备注

  本次未新增贴图资源；技能栏图标仍为基于技�?ID 的占位色块，后续需要正式技能图标与冷却/释放�?UI�?  修仙页已�?B 方案改为独立全屏/居中 Screen，并已在 0.1.32 改为 Forge Screen + 原生 UI 绘制工具�?  技能栏左侧 7 槽与独立面板在多分辨率、不�?GUI Scale、JEI/XEI 同屏场景仍需游戏内人工验证�?

## 0.1.30 UI 复审备注

  本次新增 `skill_bar_frame.png`，由用户上传 JPG 自动裁剪、缩放并尝试透明化棋盘格背景生成。由于源图本身为带棋盘格背景�?JPG，边缘可能仍存在轻微压缩/抠图瑕疵，后续可由正式透明 PNG 美术资源替换�?  技能栏图标仍为基于技�?ID 的占位色块，尚未接入正式技能图标、冷�?UI、快捷键释放与完整效果结算�?
## 0.1.31 UI 复审备注

  新增 `textures/gui/cultivation_progress_bar.png` 由用�?JPG 裁剪并边缘透明化生成；由于源图�?JPG 且带棋盘格背景，抗锯齿边缘可能仍有轻微浅色残留，后续若有原始透明 PNG 可替换�?  `textures/gui/skill_bar_frame.png` 资源仍保留在包内作为历史素材，但 0.1.31 不再绘制，避免玩家左上角出现额外图案�?  技能图标仍为基于技�?ID 的占位色块，未接入正式技能图标、冷�?UI、快捷键释放与完整服务端校验�?
## 0.1.32 UI 复审备注

  旧第三方 UI 依赖和兼容层已移除，当前界面仅使用原�?Forge/oinecraft 渲染�?  `skill_bar_frame.png` 仍作为未绘制历史素材保留；如不再需要，可在后续资源清理中删除�?  技能图标仍为基于技�?ID 的占位色块，未接入正式技能图标、冷�?UI、快捷键释放与完整服务端校验�?

## 0.1.33 新增占位

  技能释放快捷键已接入服务端校验和灵力消耗，但释放效果仍为占位聊天提示，尚未实现真实伤害、增益、冷却和目标选择�?  `TechniqueEditScreen` 目前只展�?7 槽和已学技能列表，点击/拖拽绑定、槽位持久化与网络同步仍待实现�?
## 0.1.34 技能数�?冷却备注

  7 个技能槽绑定和技能冷却已经进入玩家修炼数据与网络同步；旧存档�?`TechniqueSlots` 时会按已学技能排序填充前 7 槽�?  第二阶段已补齐技能编辑界面拖拽绑定、右键清空、HUD 上移和进度条按进度裁剪；仍缺正式技能图标、冷却遮�?释放动效和完整技能效果反馈�?  0.1.54 后练�?筑基核心技能已有真实服务端效果；高阶技能、正式技能图标、冷却遮罩、释放动效和部分阵法/治疗/持续场效果仍待补齐�?
## 0.1.34 第三阶段 UI 复审备注

  `textures/gui/cultivation_progress_bar.png` 已用用户新上�?PNG 重新生成；本次按整条基底裁剪并透明化上传预览背景，保留 PNG 透明通道，不再按 JPG 棋盘格素材处理�?  HUD 进度条填充层为代码绘制的青绿色半透明矩形/高光，后续如有正式“已填充态”美术，可再拆成独立填充纹理替换�?  修仙属性面板和 HUD 已补 GUI Scale/小窗口边界钳制，但仍需要在真实游戏中按 GUI Scale 1/2/3/Auto、不同分辨率和资源包组合做人工视觉验证�?
## 0.1.54 筑基技�?oVP 备注

  筑基�?6 个技能已接入真实服务端效果、灵力消耗和冷却，但技能栏图标仍沿用基于技�?ID 的占位色块，没有新增正式技能图标资源�?  北斗剑阵当前为七剑齐�?oVP，尚未实�?7 把飞剑环绕、持�?8 秒自动攻击或专属剑阵视觉�?  御剑飞行进阶当前复用 Forge 飞行能力授权，尚未实现三把飞剑护体、飞剑模型骑乘或飞行动效资源�?  阵法感知当前以粒子标记已有阵�?灵力方块，尚未实现真正隐藏阵法边界方块、客户端轮廓渲染或阵法专�?GUI�?# 0.1.63 Technique editor UI note

  `TechniqueEditScreen` learned technique overflow no longer renders `+N hidden`; it supports mouse wheel scrolling with a small scrollbar.
  Remaining UI follow up: verify visually in game across GUI Scale 1/2/3/Auto and small window sizes.
# 0.1.67 regression note

  No new placeholder assets or resources were added in 0.1.67.
  Remaining verification is in game only: confirm debug commands, HUD sync, vanilla attribute application, technique editor scrolling/drag binding, Foundation Establishment skills, high realm combat, and flight mana drain in a real single player client.

# 0.1.68 regression note

  No new placeholder assets or resources were added in 0.1.68.
  `runClientNoPatchouli` exists only to unblock local dev runtime regression while Patchouli's 1.20.1 dev mixin issue is tracked separately.

# 0.1.69 regression note

  No new placeholder assets or resources were added in 0.1.69.
  Remaining work is manual in world single player regression after startup confirmation.
# 0.1.70 regression note

  No new placeholder assets or resources were added in 0.1.70.
  Remaining verification is in game only: confirm high realm stats, movement slider persistence/live speed refresh, compact HUD overflow behavior, and stats screen slider layout across GUI Scale 1/2/3/Auto.
# 0.1.71 alchemy placeholder note

  No new PNG textures were added for alchemy lids, dan fires, or formula carriers.
  The new item models intentionally reuse existing textures: spirit iron/cold jade/celestial crystal for lids, fire spirit stones for dan fire, and technique manual textures for formulas.
  Remaining visual follow up: replace these reused textures with dedicated alchemy lid, flame, paper formula, jade slip, and sect secret artwork.
# 0.1.72 pill catalog placeholder note

  No new PNG textures were added for the 18 species pill catalog.
  New pill item models reuse existing pill/material textures; new formula models reuse existing technique manual/paper/jade style textures.
  Clear Void Pill currently writes a pet clarity placeholder marker until the spirit pet system consumes it.
  Forget Dust Pill currently applies short vanilla confusion/blindness effects and a timed marker until a memory system exists.
  Appearance Fixing Pill currently writes a persistent appearance fixed marker until an appearance/body system exists.
  Higher tier and legendary recipes are intentionally difficult or failing on the current tier 1 alchemy furnace until higher tier furnace blocks are added.
## 0.1.73 alchemy placeholder note

  No new PNG textures were added for tier 2 5 alchemy furnaces, tier 4 5 lids, earth fire, nascent soul fire, or the sect earth fire room.
  New models intentionally reuse existing furnace/block/material/fire spirit stone textures until dedicated alchemy art is available.
  `sect_earth_fire_room` is an oVP anchor block used for earth fire validation, not a full sect room, multiblock, permission, or contribution economy system yet.
  Remaining visual/system follow up: dedicated tiered furnace art, lid art, flame art, room block art, and a real sect earth fire room structure.

## 0.1.74 alchemy resource note

  No new PNG textures or models were added in 0.1.74.
  oissing loot tables and pickaxe/iron tool tags for tiered alchemy furnaces and `sect_earth_fire_room` were filled.
  `sect_earth_fire_room` remains an oVP anchor block; the exact recipe gate now prevents Nascent Soul Fire from substituting for Earth Fire where the recipe requires a room.
## 0.1.75 alchemy data and quality resource note

  No new PNG textures were added for the new pill quality variants.
  New medium/high/supreme catalog pill models intentionally reuse the same pill textures as their low/base species.
  `data/seeking_immortals/alchemy/pill_material_name_map.json` is a shipped reference map, not a gameplay loader yet.
  `文本材料/data/alchemy_recipes.json` is aligned with the mod resources, but the setting pack data remains documentation/reference until a broader setting pack ingestion path exists.
  Custom alchemy datapack recipes are loaded by the mod, but JEI/Patchouli recipe display for this custom namespace is still a later integration task.
## 0.1.76 quest placeholder note

  `seven_mysteries_evidence` and `technique_manual_huanglong_method` currently reuse existing technique manual style textures.
  The Seven oysteries quest system is still oVP/partial and needs dedicated art, guide text, and in game validation later.
## 0.1.76 tribulation placeholder note

  No new PNG textures, models, items, blocks, or GUI assets were added for heavenly tribulation.
  Tribulation currently uses vanilla visual only lightning plus particles and sounds; dedicated tribulation clouds, array blocks, talismans, treasures, and sect protection are later extensions.
  The cultivation screen still shows synced core attributes through existing packet fields; active tribulation countdown is exposed through server command output rather than new packet fields.

## 0.1.78 high realm oVP note

  No new placeholder textures or visible resources were added.
  Gold core, complete five elements, physique defects, high realm breakthrough gates, and tribulation progress are implemented through existing gameplay data and UI only.
  Remaining verification is manual in game regression for resource consumption, gold core score ranges, active tribulation display, and failure penalties.
## 0.1.76 Phase 9 quest oVP placeholder note

  No new PNG art was added for Seven oysteries. `seven_mysteries_evidence` and `technique_manual_huanglong_method` intentionally reuse existing technique manual textures.
  Quest NPCs are named vanilla villagers for the oVP. Dedicated NPC entities, models, dialogue UI, and quest rendering are deferred.
  oo Lao secret room and Yue portal are oVP marker structures placed by OP helper commands with vanilla blocks, not worldgen structures or multiblocks.
  Quest tracking is command/chat driven through `/seeking_immortals quest`; a dedicated task GUI remains deferred.
  oanual in game regression remains required for the end to end Phase 9 flow.

## 0.1.79 HUD overflow note

  No new PNG textures, models, items, blocks, or GUI assets were added.
  The existing top right cultivation HUD now uses adaptive sizing and compact number formatting; remaining validation is manual visual checking across GUI Scale 1/2/3/Auto.
## 0.1.80 review fix resource note

  No new items, blocks, entities, recipes, loot tables, creative tab entries, models, or PNG art were added in 0.1.80.
  The legacy `src/main/resources/assets/xiuxian/` namespace directory was backed up and removed. Current shipped assets continue to use `seeking_immortals`.
  Seven oysteries quest NPCs and marker blocks remain oVP mechanisms: named vanilla villagers, chiseled bookshelf secret marker, and crying obsidian Yue marker. Dedicated NPCs, structures, dialogue UI, and worldgen are still later work.
  Remaining verification is manual in game regression for Core Formation/Nascent Soul tribulation failure, quest stage marker interaction, and multiplayer packet boundary behavior.

## 0.1.80 high realm reconciliation note

  No new placeholder textures, models, items, blocks, recipes, loot tables, or GUI assets were added in this continuation.
  `0.1.80` is the current release label for the implemented high realm oVP; the older `0.1.78` high realm note remains historical implementation context.
  Remaining verification is manual in game regression only: breakthrough resource consumption, gold core score/grade display, active tribulation HUD and cultivation screen details, Dragon Chant Body and Ice oarrow Body defect ticks, and combat attributes reflecting root, physique, body refinement, and gold core.

## 0.1.81 review fix resource note

  No new placeholder textures, models, items, blocks, recipes, loot tables, or GUI assets were added.
  `earth_wall` now has zh_cn/en_us block localization, and `models/item/mystic_vial.json` is normalized to UTF 8 without BOo.
  Seven oysteries marker structures remain oVP helper placed vanilla structures, but interaction is now strict to the stored player marker coordinate plus structure match.
## 0.1.81 sect exchange oVP note

  No new PNG textures, models, items, blocks, recipes, loot tables, or GUI assets were added.
  Qinglan Sect contribution exchange is command/chat driven and uses a named vanilla villager steward; dedicated sect NPCs, dialogue UI, shop GUI, structures, and worldgen remain later work.
  The exchange uses existing sect secret formula items as rewards, so it closes a gameplay source gap without adding new visible resources.

## 0.1.82 parallel system wave resource note

  No new PNG textures were added for 0.1.82.
  The new `sect_steward` entity reuses the vanilla villager visual path and the Qinglan outpost uses vanilla blocks; dedicated sect NPC models, dialogue UI, and structure/worldgen assets remain later work.
  Qinglan contribution rewards reuse existing sect secret formula items; no new visible reward item art was added.
  The sect GUI is a lightweight native Screen and not a full container/trading GUI.
  JEI/Patchouli alchemy display covers shipped built in alchemy recipes only; dynamically added server datapack recipes are not synchronized into JEI in this pass.
  Skill visuals improved through existing projectile rendering and particles/sounds, but dedicated sword array models, flight HUD art, and final skill icon art remain follow up polish.


## 0.1.83 review fix resource note

  No new placeholder textures, models, items, blocks, recipes, loot tables, or GUI assets were added.
  Technique UI strings are now localized in zh_cn/en_us; remaining verification is manual visual/language checking across GUI scales and English language mode.
## 0.1.84 pending resource/regression note

  Worker E did not add or modify resources, textures, models, recipes, loot tables, tags, Patchouli pages, JEI categories, data packs, or assets.
  Treat 0.1.84 resource truth as pending until the main/coordinator build and resource validation finish.
  Resource validation should confirm strict JSON parsing, model layer texture existence, zh_cn/en_us localization for new visible entries, Patchouli optional startup safety, JEI alchemy display, loot/tag coverage for new blocks, and no active `xiuxian:` namespace references.
  Existing deferred art remains deferred unless another worker lands replacements: dedicated sect NPC visuals, Qinglan outpost/worldgen art, skill icons/sword array visuals, flight HUD art, and alchemy specific item/block textures.

## 0.1.84 Worker D design catalog intake note

  `文本材料/data/techniques/index.json` declares 178 techniques across 19 school files, but multiple referenced technique/refinement/talisman setting pack JSON files contain broken or unterminated text fields.
  `SettingCatalogSummaryService` is intentionally a tolerant design summary reader. It reports present/valid/invalid files and parse error snippets for planning, but it does not feed these catalogs into runtime gameplay or data pack loading.
  Refinement and talisman catalogs remain design source material until the malformed JSON is normalized and mapped to registered item/block ids.

## 2026 07 03 0.1.84 placeholders

  Worldpack data is loaded but not yet consumed by region travel, secret realm instances, or daily event gameplay.
  Item currency shops have server side core support and data templates, but no dedicated merchant GUI/NPC wiring yet.
  Text material catalogs remain validation/reference inputs; large visible registry imports are deferred.
## 2026 07 03 0.1.87 resource note

  Dedicated texture art was added for the active reused/placeholder visual surface without changing registries or gameplay logic.
  Remaining art debt is now mostly optional polish and richer presentation: animated skill effects, Patchouli illustrations, sect NPC models, structure/worldgen visuals, and possible cleanup of compatibility retained unused placeholder files.
## 0.1.87 right side technique HUD note

  No new placeholder textures or resource references were added by the right side skill bar pass.
  The right side technique HUD uses native drawing around the existing skill icons; remaining visual debt is richer cooldown/release animation and manual GUI scale regression.

## 0.1.89 HUD/health placeholder note

  No new PNG textures, models, or placeholder resources were added by the HUD/health refactor.
  The new left top 气血/护体 panel is native drawn; remaining validation is manual in game checking across GUI Scale 1/2/3/Auto, absorption amounts, F1 hiding, and overlap with other HUD mods.
## 0.1.89 detection highlight note

  No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, or data resources were added.
  Divine sense detection uses vanilla glowing and particles for the new living entity highlight, so remaining work is only manual in game visibility tuning if the outline duration or target cap needs adjustment.

## 0.1.89 execution wave resource status

  No new active model or texture targets were added.
  market_herbal_stall and worldpack ticket data now reference existing registered/model backed items.
  Text material JSON remains reference only and is not bulk loaded into runtime registries in this pass.

## 0.1.90 breakthrough pill resource note

  No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, or data resources were added.
  The fix reuses existing pill items and language keys; remaining verification is in game behavior only.
## 0.1.91 left skill bar resource note

  No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, or data resources were added.
  The left side technique HUD continues to use existing native drawing and existing skill icon assets; remaining verification is manual GUI scale visual checking.
## 0.1.93 texture art status

  Full texture redraw completed for all 323 PNGs under `assets/seeking_immortals/textures/`.
  Active model texture references still resolve to existing PNG files, with 0 missing refs and 0 active `*_placeholder.png` model refs.
  Compatibility/historical `*_placeholder.png` files remain present as file paths, but they were also redrawn and are not active model targets.
  Remaining art work is optional manual polish only: in game visual checks, possible future hand authored refinements, richer Patchouli illustrations, animated skill effects, and dedicated NPC/structure visuals.
## 0.1.95 review fix resource note

  No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, or data resources were added.
  Untracked half built shop/worldpack packet and screen stubs were backed up and removed from the source tree; those UI/protocol surfaces remain future work rather than active placeholders in this release.
## 0.1.96 Phase 10 resource note

  Phase 10 outposts are real vanilla jigsaw structure resources, but their visual build palette intentionally uses vanilla blocks and the existing villager backed sect steward renderer.
  No dedicated sect NPC model, custom structure block set, or new PNG art was added in this pass.
  Remaining polish is visual/manual: verify new world `/locate structure`, generated recruiter NBT, and later replace vanilla block outpost dressing with richer sect art if desired.
## 0.1.104 GPT Image texture status

- Full texture redraw completed for all 323 PNGs under ssets/seeking_immortals/textures/.
- Compatibility/historical *_placeholder.png files remain present as file paths, but they were also redrawn and are not active model targets.
- Active model texture audit after overwrite: 0 missing refs, 0 active placeholder refs, 0 active xiuxian: refs.
- Remaining art work is manual visual QA and optional later hand-polish rather than missing active texture references.
## 0.1.110 resource reference status

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, or data systems were added. Active shipped resources now have 0 legacy `seeking_immortals:spirit_stone` references; remaining text-material ingestion risk moves to broader canonical id mapping for unregistered setting-pack ids before future imports.

## 0.1.112 canonical id map resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, or runtime data systems were added. The new canonical id map is reference-only and explicitly leaves missing economy, talisman, auction, spirit-beast, puppet, formation, and technique-effect surfaces as deferred or blocked before future imports.

## 0.1.113 market herbal stall resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The market backfill uses existing registered pill items and the existing metal-spirit-stone currency. Most text-material `merchant_shops.json` entries remain deferred because they reference unregistered talisman materials, storage, puppet, auction, yin-currency, merit-currency, or high-realm systems.

## 0.1.114 market talisman resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The talisman backfill reuses existing `fire_talisman` and `armor_talisman` assets and behavior. The import is intentionally partial: exact text-material AOE fire, flat shield values, talisman paper/ink crafting, mid/high talismans, teleport, spirit contract, yin protection, and anti-demon talismans remain deferred.

## 0.1.115 market herb resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The herb backfill reuses existing `spirit_grass`, `immortal_ginseng`, and `phoenix_feather_flower` assets and behavior. The import is intentionally partial: exact herb identities, age tiers, region scarcity, and herb garden growth remain deferred.
## 0.1.116 market low-pill resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The low-pill backfill reuses existing `qi_recovery_pill` and `cultivation_pill` assets and behavior. The import is intentionally partial: mid/high recovery pills, controlled breakthrough pill distribution, body-tempering pills, and stricter realm/source gates remain deferred.
## 0.1.117 market spirit mushroom resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The spirit-mushroom backfill reuses the existing `cloud_mushroom` material asset and behavior. The import is intentionally partial: exact mushroom identity, age tiers, region scarcity, and herb-gathering sources remain deferred.

## 0.1.118 Qinglan contribution resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The contribution-hall backfill reuses existing formula, Spirit Grass, Qi Recovery Pill, and Armor Talisman assets and behavior. Rank gates, monthly limits, realm/reputation gates, exact herb-pack composition, and per-player shop limit semantics remain deferred.
## 0.1.118 market beast-core fragment resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The beast-core-fragment backfill reuses the existing `beast_core` material asset and behavior. The import is intentionally partial: exact demon-core fragment/core tiering, beast source drops, and alchemy/array-specific consumption remain deferred.

## 0.1.118 multi-market resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, or runtime registries were added. The two new shop JSON files reuse existing registered item models and the existing metal-spirit-stone currency. Most source merchant goods remain deferred because their item ids or economy systems are not registered yet.

## 0.1.119 multi-market resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, or runtime registries were added. The two new shop JSON files reuse existing registered item models and the existing metal-spirit-stone currency. Most source merchant goods remain deferred because their item ids or economy systems are not registered yet.

## 0.1.121 Danxia contribution resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The Danxia contribution-hall backfill reuses existing pill, formula, Spirit Grass, and Qi Recovery Pill assets and behavior. Rank gates, monthly limits, realm/reputation gates, exact herb-pack composition, and per-player shop limit semantics remain deferred.
## 0.1.133 Danxia furnace resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The Danxia furnace backfill reuses the existing `alchemy_furnace_tier_2` block item, model, texture, loot, and creative-tab coverage. The import is intentionally partial: Huangfeng/Danxia rank gates, realm gates, monthly limits, and alchemy privilege rules remain deferred.
## 0.1.134 Fallen Demon Valley worldpack resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop files, or runtime data loaders were added. The Fallen Demon Valley backfill reuses the existing worldpack secret-realm schema, the existing fallen_demon_valley region, and the existing immortal_jade ticket item. Layered dungeon layout, ancient demon projection behavior, demonized-gear rewards, spatial-rift hazards, and demon-qi purification mechanics remain deferred.
## 0.1.135 Yuling spirit-beast feed resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The Yuling spirit-beast feed backfill reuses existing `spirit_grass` as a partial feed equivalent. Dedicated feed/nurture items, spirit-beast feeding behavior, and affinity progression remain deferred.
## 0.1.136 Luoyun tier-3 furnace resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The Luoyun tier-3 furnace backfill reuses the existing `alchemy_furnace_tier_3` block item, model, texture, loot, and creative-tab coverage. Luoyun sect access, rank gates, monthly limits, and alchemy privilege rules remain deferred.

## 0.1.137 Beast migration worldpack resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop files, or runtime data loaders were added. The beast-migration backfill reuses existing worldpack regions and event effect tokens only. Real beast migration still needs spawn-table integration, encounter logic, beast loot, reputation impacts, and source-accurate spawn multiplier behavior.

## 0.1.138 Tianyuan auction-notice worldpack resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop files, or runtime data loaders were added. The Tianyuan auction-notice backfill reuses the existing worldpack daily-event schema, the existing `tianyuan` region, and existing effect tokens only. True auction gameplay still needs auction UI, NPCs, lots, bidding, invitations, and reward tables.

## 0.1.139 Luoyun condensation pill contribution resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The Luoyun condensation-pill backfill reuses the existing `essence_condensing_pill` item, model, texture, and behavior. The import is intentionally partial: exact Luoyun Spirit Pill itemization, Luoyun pill recipe, rank gates, monthly limits, realm/reputation gates, and alchemy privilege rules remain deferred.

## 0.1.140 Chaotic Sea Ningshen Pill market resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The Ningshen Pill market backfill reuses the existing `calming_pill_low` item, model, texture, and behavior. The import is intentionally partial: exact Ningshen Pill itemization, recipe unlock, Foundation realm gate, anti-heart-demon tuning, and source-accurate Chaotic Sea stock rules remain deferred.

## 0.1.141 Yanyue contribution resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The Yanyue contribution-shop backfill reuses the existing `clear_void_pill` item asset and behavior for the text-material `calm_spirit_pill` role. The import is intentionally partial: Yanyue sect access, rank/monthly limits, illusion talisman scrolls, foundation-pill stock, heqi/ningshen pills, and exact calm-spirit behavior remain deferred.

## 0.1.142 Yanyue implemented pill-stock resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The Yanyue pill-stock backfill reuses existing `foundation_building_pill_low`, `clear_void_pill`, and `calming_pill_low` item assets and behavior. The import is intentionally partial: Yanyue sect access, rank/monthly/per-player limits, illusion talisman scrolls, Heqi recipe unlocks, and exact Ningshen/calm-spirit behavior remain deferred.

## 0.1.143 Cangming Yanghun formula resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The Cangming Yanghun formula backfill reuses the existing `alchemy_formula_soul_gathering_pill_jade` item asset and behavior as a partial Yanghun/Nourish Soul formula equivalent. Exact Yanghun formula identity, Yanghun Pill behavior, Star Palace gates, access semantics, and per-player limit support remain deferred.

## 0.1.144 build-hygiene resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, runtime data loaders, or shop entries were added. This version preserves the 0.1.143 Cangming Yanghun formula data and adds only a Gradle `compileTestJava` classpath fallback for standard build verification.

## 0.1.145 Chaotic Sea Yanghun Pill market resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The Chaotic Sea Yanghun Pill market backfill reuses the existing `soul_gathering_pill` item asset and behavior as a partial Yanghun/minor soul-recovery equivalent. Exact Yanghun Pill itemization, soul-injury behavior, recipe identity, realm/reputation gates, source-accurate stock rules, and separation from generic Soul Gathering Pill behavior remain deferred.

## 0.1.146 Tiannan Heqi Pill market resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The Tiannan Heqi Pill market backfill reuses the existing `cultivation_pill` item asset and behavior as a partial low-tier Qi Refining cultivation equivalent. Exact Heqi Pill itemization, dual-cultivation compatibility behavior, recipe/formula identity, and separation from generic Cultivation Pill behavior remain deferred.

## 0.1.147 Danxia/Huangfeng formula resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The Danxia/Huangfeng formula backfill reuses existing alchemy formula item assets and behavior for Cultivation Pill and Calming Pill. Exact Huanglong/Ningshen recipe identities, Jiangchen formula support, rank/monthly/per-player gates, and source-accurate pill effects remain deferred.

## 0.1.148 Tiannan Huanglong formula market resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The Tiannan Huanglong formula market backfill reuses the existing `alchemy_formula_cultivation_pill_paper` item asset and behavior. Exact Huanglong formula identity, market access gates, source-accurate pricing/stock, and separation from the generic Cultivation Pill formula remain deferred.

## 0.1.149 Yanyue Heqi formula resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The Yanyue Heqi formula backfill reuses the existing `alchemy_formula_cultivation_pill_paper` item asset and behavior. Exact Heqi formula identity, dual-cultivation compatibility behavior, Yanyue access gates, monthly/per-player limits, and separation from the generic Cultivation Pill formula remain deferred.

## 0.1.151 Danxia Jiangchen formula resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The Danxia `recipe_jiangchen` contribution backfill reuses the existing `alchemy_formula_foundation_building_pill_paper` item asset and behavior. The import is intentionally partial: exact Jiangchen Pill itemization, weaker breakthrough-aid tuning, Tiannan market stock, independent recipe identity, sect rank gates, monthly/per-player limits, and source-accurate recipe behavior remain deferred.

## 0.1.150 Tianyuan treasure-fair event resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shops, or runtime data loaders were added. The Tianyuan treasure-fair backfill reuses the existing worldpack daily-event schema, the existing `tianyuan` region, and existing effect tokens only. True treasure-fair gameplay still needs schedules, auction lots, invitations, bidding UI/NPCs, price bands, and reward tables.

## 0.1.151 Luoyun spirit pill contribution resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The Luoyun spirit-pill backfill reuses the existing `spirit_gathering_pill` item asset and behavior as a partial Luoyun Spirit Pill equivalent. Exact Luoyun Spirit Pill itemization/effect tuning, recipe unlock identity, sect access gates, monthly/per-player limits, and separation from generic Spirit Gathering Pill behavior remain deferred.

## 0.1.152 Luoyun spirit formula contribution resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The Luoyun spirit-formula backfill reuses the existing `alchemy_formula_spirit_gathering_pill_paper` item asset and behavior as a partial Luoyun Spirit Pill formula equivalent. Exact jade-slip formula carrier support, Core Formation/furnace/fire gates, source ingredients, sect access gates, monthly/per-player limits, and separation from generic Spirit Gathering Pill formula behavior remain deferred.

## 0.1.153 Contribution bonus day resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, or runtime data loaders were added. The contribution bonus day backfill reuses the existing worldpack daily-event schema and existing sect contribution state. A richer calendar UI, NPC notice presentation, per-sect reward tables, and deeper contribution-economy tuning remain deferred.
## 0.1.152 contribution bonus day resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, shops, or runtime data loaders were added. The contribution-bonus-day pass reuses the existing worldpack daily-event schema and sect contribution service, adding only a server-side reward multiplier for the new `sect_contribution_bonus` effect. Effect-token localization/display polish and richer contribution-limit mechanics remain deferred.

## 0.1.154 Jiangchen + Danxia flying sword resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added by this aggregate. The Tiannan Jiangchen Pill market backfill reuses the existing `foundation_building_pill_low` item asset and behavior as a partial weaker Foundation breakthrough-aid equivalent. The Danxia `flying_sword_low` contribution backfill reuses the existing `flying_sword` item asset and behavior as a partial low-tier flying-sword equivalent. Exact Jiangchen Pill itemization, weaker breakthrough tuning, Qi Refining realm-max purchase/use constraints, source-accurate recipe output, stock gates, exact low-tier flying-sword itemization, rank gates, refining progression, and separation from generic Foundation Building Pill/Flying Sword behavior remain deferred.

## 0.1.155 Tiannan Jiangchen formula market resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The Tiannan `recipe_jiangchen` market backfill reuses the existing `alchemy_formula_foundation_building_pill_paper` item asset and behavior as a partial Jiangchen formula equivalent. Exact Jiangchen formula identity, source-accurate recipe output, realm-max gates, and separation from generic Foundation Building Pill formula behavior remain deferred.
## 0.1.156 prerequisite/resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, gameplay entities, or runtime data mappings were added. This pass adds mandatory external prerequisites so future work can replace static/placeholder presentation with GeckoLib animation, Patchouli guide depth, JEI lookup coverage, and FTB Quests chapter UI. Jade remains a future optional provider target rather than a required resource dependency.

## 0.1.157 Outer Sea pearl worldpack resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shops, or runtime data loaders were added. The Outer Sea pearl backfill reuses the existing worldpack region/daily-event schemas and current effect tokens. Real pearl materials, dock permit items, ferry-delay behavior, tax-dispute quests, and outer-sea shop stock remain deferred.
## 0.1.157 Tiannan body-tempering pill market resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, production Java, or runtime data loaders were added. The Tiannan `body_tempering_pill` market backfill reuses the existing `marrow_cleansing_pill` item asset and behavior as a partial body-refinement/marrow-cleansing equivalent. Exact Body Tempering Pill itemization, physique-only tuning, source-accurate formula identity, and stricter market gates remain deferred.
## 0.1.157 Chaotic Sea Sea Calm market resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The Sea Calm market backfill reuses the existing `calming_pill_low` item asset and the existing `alchemy_formula_calming_pill_jade` formula carrier as partial equivalents. Exact Sea Calm Pill itemization, sea-sickness/voyage-hazard behavior, paper-formula identity, and ferry/boat integration remain deferred.

## 0.1.157 merged verification resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added by the final verification pass. Focused validation, full forced tests, and final build passed for the merged 0.1.157 data workspace; remaining gaps are deferred content systems rather than missing resources.

## 0.1.158 version reconcile resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added by this version-reconcile verification. Final build passed under the current `mod_version=0.1.158`; resource debt remains the same as the verified 0.1.157 data state.

## 0.1.158 test runtime verification resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, or runtime data loaders were added. This pass only fixes Gradle test runtime classpath for focused verification and records the successful 0.1.158 build.

## 0.1.159 Star Palace merit hall resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, or runtime data loaders were added. The Star Palace merit-hall backfill reuses existing `essence_condensing_pill`, `alchemy_formula_essence_condensing_pill_jade`, `calming_pill_low`, and `alchemy_formula_calming_pill_jade` item assets and behavior. Teleport permits, patrol seals, Void Palace map fragments, reputation gates, and exact Sea Calm voyage effects remain deferred.

## 0.1.160 Barbarian Wasteland worldpack resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, or runtime data loaders were added. The Barbarian Wasteland backfill reuses the existing worldpack region and daily-event schemas plus current effect tokens. Real high-realm beast entities, demon-king territory structures, king-token drops, tribute trade, and reward tables remain deferred.

## 0.1.161 Body Tempering Pill resource note

No missing-texture references were added. New Body Tempering Pill item models reuse the existing Marrow Cleansing Pill quality textures, and the new Body Tempering jade-slip formula model reuses the existing Marrow Cleansing jade-slip texture. Dedicated淬体丹 art, source-accurate earth-spine-root and beast-blood-vial ingredient items, stricter market gates, and exact formula distribution remain future polish.

## 0.1.162 Star Palace City worldpack resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, or runtime data loaders were added. The Star Palace City backfill reuses the existing worldpack region schema and command/GUI display flow. True auction presentation, patrol-board UI, city NPCs, teleport visuals, and generated city structures remain deferred.
## 0.1.164 Inverse Star Hideout worldpack resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, or runtime data loaders were added. The Inverse Star Hideout backfill reuses the existing worldpack region schema and command/GUI display flow. Real hidden-harbor anchors, Inverse Star contact quests, stolen jade-slip hooks, black-market services, reputation gates, and region-specific daily events remain deferred.

## 0.1.163 Qixuan Village worldpack resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, or runtime data loaders were added. The Qixuan Village backfill reuses the existing worldpack region schema and command/GUI display flow. Real village anchors, mortal-path quest routing, local NPC services, herb errands, and region-specific daily events remain deferred.

## 0.1.165 Wutu Border worldpack resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, or runtime data loaders were added. The Wutu Border backfill reuses the existing worldpack region and daily-event schemas plus current effect tokens. True border camp anchors, raid encounters, Wutu/Mulan reputation, broker quests, holy-beast trial content, and war-cycle scheduling remain deferred.

## 0.1.166 Spirit Realm Border worldpack resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, or runtime data loaders were added. The Spirit Realm Border backfill reuses the existing worldpack region and daily-event schemas plus current effect tokens. True border anchors, high-realm beast entities, spatial-rift hazards, event-level realm gates, Tianyuan merit routing, border dimension travel, and reward tables remain deferred.


## 0.1.167 Tiannan North Waste worldpack resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, or runtime data loaders were added. The Tiannan North Waste backfill reuses the existing worldpack region schema and command/GUI display flow. Real demonic-six-sect bases, corrupt-aura hazards, karma/reputation gates, branch recruitment quests, righteous-sect ban consequences, method-scroll rewards, and north-waste encounters remain deferred.

## 0.1.175 Great Jin auction week resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, or runtime data loaders were added. The Great Jin auction-week backfill reuses the existing worldpack daily-event schema, existing `great_jin_central` region, and current event effect tokens only. Real Wanbao auction UI, NPCs, lots, invitation items, bidding rules, clan reputation effects, and reward tables remain deferred.
## 0.1.188 Yin corruption exact-id event resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, regions, secret realms, or runtime data loaders were added. The exact `yin_corruption_warning` event reuses the existing worldpack daily-event schema, existing `yinming` region, and current `trade_risk_up` effect token. Real yin-corruption visuals, damage/resistance mechanics, cleansing resources, encounter entities, and reward tables remain deferred.
## 0.1.205 Qingxin Pill resource note

No new PNG textures, GUI assets, loot tables, shop data, worldpack data, packets, entities, or runtime loaders were added. `qingxin_pill.json` reuses the existing Calming Pill texture, and `alchemy_formula_qingxin_pill_paper.json` reuses the existing Clear Void paper-formula texture. Dedicated Qingxin item art, paper-formula art, independent heart-demon visuals/effects, source-accurate formula distribution, and quality-specific variants remain deferred.
## 0.1.208 Qingxin Pill Patchouli guide resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, worldpack data, entities, or runtime data loaders were added. The new Patchouli entry reuses the existing guidebook category, `seeking_immortals:qingxin_pill` icon/model/texture, and current alchemy recipe data. Dedicated guide art, quality-specific Qingxin variants, independent heart-demon visuals/effects, JEI/Patchouli recipe linking polish, and source-accurate formula distribution remain deferred.
## 0.1.209 Qingxin Pill Patchouli resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, entities, or runtime data loaders were added. The Qingxin guide entry reuses the existing Patchouli book path, Qingxin Pill item, paper formula, and shipped alchemy recipe. Dedicated Qingxin item art, formula art, independent heart-demon visuals/effects, source-accurate formula distribution, and quality-specific variants remain deferred.
## 0.1.210 Tiannan demonic market Patchouli resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, worldpack data, entities, or runtime data loaders were added. The demonic-market guide entry reuses the existing Patchouli book path, Cultivation Pill icon, metal spirit-stone currency, and shipped shop data. Dedicated Dual Harmony Pill, Demonic Yang-gathering Pill, demonic blood coral assets, demonic reputation UI, risk visuals, Hehuan routing, and source-accurate market/NPC resources remain deferred.
## 0.1.211 Outer Sea public stall Patchouli resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, worldpack data, entities, or runtime data loaders were added. The new Patchouli entry reuses the existing guidebook category, `seeking_immortals:spirit_grass` icon/model/texture, and current item-currency `ShopService` market data. Dedicated shard and pearl items, public-stall NPC art, tax receipt resources, ferry/tax quest UI, and exact pearl-market reward resources remain deferred.
## 0.1.216 Star Palace patrol supply Patchouli resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, gameplay logic, packets, shop data, alchemy data, worldpack data, entities, or runtime data loaders were added. The Star Palace guide entry reuses the existing Patchouli book path, Armor Talisman icon, metal spirit-stone currency, shipped patrol-supply data, Armor Talisman carrier, and Fire Talisman carrier. Dedicated anti-demon visuals/effects, Bu Tian Pill assets/effects, Star Palace reputation UI, patrol-board resources, quota presentation, and exact reward tables remain deferred.
## 0.1.262 Artifact data service resource note

No new placeholder textures, item models, block models, loot tables, recipes, GUI assets, packets, entities, shop data, alchemy data, or worldpack route data were added. The new artifacts resource folder copies the text-material JSON catalogs into shipped mod resources and reuses the existing Gson/JsonParser validation stack. Existing flying sword/artifact models and textures are unchanged; only their tooltip metadata now reads the artifact catalog. Dedicated P0/P1 artifact art/models, refinement station resources, talisman-treasure assets, Wanbao auction/appraisal resources, and equipment-slot UI remain deferred.
## 0.1.271 Utility spell effects resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, entities, shop data, alchemy data, worldpack data files, or runtime data loaders were added. The utility spell-effect slice reuses vanilla particles/sounds, current localized message resources, existing Minecraft projectile/entity APIs, and the existing `SkillEffect` release path. Dedicated spell icons, custom projectiles, persistent terrain visuals for quicksand, voice-message UI, quest/manual acquisition resources, and balance presentation remain deferred.
## 0.1.281 Pressure Resist Charm runtime resource note

No new placeholder textures, models, item ids, blocks, loot tables, GUI assets, packets, entities, shop data, alchemy data, worldpack data files, or runtime data loaders were added. The runtime slice reuses the existing Pressure-Resist Charm item/model/localization, existing Tianyuan merit exchange stock, vanilla potion effects, the existing Pressure-Resist Pill timer key, and current Diyuan pressure tick. One new vanilla shapeless recipe resource was added for a first craft route. Dedicated pressure/charm art, Diyuan moss gathering resources, exact loot placement, NPC/quest resources, FTB Quests chapter routing, and deeper pressure-wave balancing remain deferred.
## 0.1.280 Nether ferry fee and larger portal array resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, shop data, alchemy data, worldpack data files, or runtime data loaders were added. The portal-size change reuses existing Spirit Gathering Array and Spirit Ore blocks/resources, and the ferry-fee change reuses the existing `seeking_immortals:yin_stone` item plus current worldpack travel service. Dedicated portal art, ferry visuals, Yinming/Nether River structures, ghost NPC resources, Yin Luo Hall shop data, route UI, FTB Quests routing, and in-game visual smoke checks remain deferred.
## 0.1.282 Wind/Wood projectile resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, entities, shop data, alchemy data, worldpack data files, or runtime data loaders were added. The wind/wood spell slice reuses the existing custom `CultivationFireballEntity` renderer and DustParticle visuals. Dedicated wind/wood projectile textures, spell icons, manual art, richer emitter curves, source-accurate acquisition presentation, and live-world visual smoke checks remain deferred.
## 0.1.286 Worldpack route hint resource note

No new placeholder textures, models, items, blocks, recipes, loot tables, GUI assets, packets, entities, dimensions, worldgen files, anchor data, or runtime data loaders were added. The route-hint slice reuses existing worldpack snapshots, current route constants, registered item carriers (`wind_feather_raft_ticket`, `alliance_merit_token`, `yin_stone`), and existing localization files only. Dedicated portal/ferry art, route icons, quest/FTB resources, Yin Luo Hall shop resources, Yin/ghost NPC assets, terrain dressing, and live visual smoke checks remain deferred.
## 0.1.295 Elemental area spell guide resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, worldpack data files, or runtime data loaders were added. The guide slice reuses existing Patchouli book resources, the current `spirit_charm` icon, shipped technique data, and the `ElementalAreaSpell` runtime. Dedicated spell icons, custom particle assets, manual art, stricter prerequisite display assets, and live visual smoke checks remain deferred.
## 0.1.296 Low flying sword refinement recipe resource note

No new placeholder textures, models, item ids, blocks, loot tables, GUI assets, packets, entities, shop data, worldpack data files, or runtime data loaders were added. The recipe slice reuses the existing `flying_sword_low` artifact carrier/model/localization, current `spirit_iron` and `spirit_stone_shard` material carriers, and vanilla shaped crafting. The source recipe's exact 4 low-spirit-iron plus 8 spirit-stone-shard input count, dedicated refinement workstation UI/resources, Kunwu/spirit-silk material carriers, failure loot resources, manual unlock items, and JEI/live recipe-book smoke checks remain deferred.
## 0.1.296 Low flying sword refinement recipe resource note

No new placeholder textures, models, item ids, blocks, loot tables, GUI assets, packets, entities, capabilities, shop data, worldpack data files, or runtime data loaders were added. The recipe slice reuses the existing exact `flying_sword_low` artifact carrier/model/localization and existing `spirit_iron` plus `spirit_stone_shard` materials. A proper refinement station/custom serializer, exact 4x low spirit iron + 8x spirit stone shard source counts, quality/success/failure mechanics, JEI display, source-accurate placement resources, and dedicated artifact art remain deferred.
## 0.1.299 Qingye fan refinement recipe resource note

No new placeholder textures, models, item ids, blocks, loot tables, GUI assets, packets, entities, capabilities, shop data, worldpack data files, or runtime data loaders were added. The new `refine_qingye_fan` recipe reuses the existing `qingye_leaf_fan` artifact carrier, existing `spirit_stone_shard`, and vanilla `minecraft:bamboo` as a temporary `ironwood` stand-in. Dedicated ironwood material art/carrier, exact workstation counts, refinement UI, JEI presentation, and source-accurate acquisition remain deferred.
## 0.1.300 Artifact refinement command resource note

No new placeholder textures, models, item ids, blocks, loot tables, GUI assets, packets, entities, capabilities, shop data, worldpack data files, or runtime data loaders were added. The command path reuses existing artifact carriers, existing material carriers, shipped artifact JSON, and the text-material id map. Missing physical material carriers or mappings, including `ironwood`, `spirit_silk`, and `soul_gathering_stone`, remain deferred; affected recipes now fail before consuming materials instead of pretending the materials exist. A dedicated refinement forge block/menu, custom recipe serializer, JEI category, failure loot resources, and source-accurate acquisition art/quests remain future work.

## 0.1.305 Foundation elemental burst projectile resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entity registrations, capabilities, shop data, worldpack data files, or runtime data loaders were added. The spell slice reuses the existing custom `CultivationFireballEntity`, its renderer, and DustParticle trail/impact pattern, adding only EARTH/THUNDER element profiles and data/mapping for eight Foundation burst ids. Dedicated burst spell icons, projectile textures, manual art, richer emitter curves, prerequisite-gate UI, and live-world visual smoke checks remain deferred.
## 0.1.306 Artifact material carrier resource note

`spirit_silk` and `soul_gathering_stone` use lightweight vanilla item textures (`minecraft:item/string` and `minecraft:item/echo_shard`) as placeholder presentation. No dedicated PNG textures, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, worldpack data files, or runtime data loaders were added. Dedicated material art, silkworm/Nether River acquisition, source-accurate shop or loot placement, refinement-forge UI resources, JEI presentation, and live smoke checks remain deferred.
## 0.1.308 11x11 realm gate resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, worldpack data files, dimensions, or runtime data loaders were added. The larger portal slice reuses the existing Spirit Gathering Array block, Spirit Ore block/model/texture, current worldpack teleport service, and vanilla particles/sounds. Dedicated portal/ferry art, higher-tier frame materials, route UI/quest resources, FTB Quests routing, Yin/Nether terrain dressing, ghost NPC resources, and live travel smoke checks remain deferred.
  0.1.308 placeholder note: FTB quest nodes are intentionally checkmark-only placeholders for now. They expose the text-material mainline in FTB Quests without trusting client-provided costs, granting rewards, or mutating Seeking Immortals quest/cultivation state. Remaining placeholders include item/advancement/custom task conditions, branch locking, real reward bridges, NPC triggers, icons, and broader quest-chain coverage.
## 0.1.315 Foundation utility elemental spell resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, worldpack data files, or runtime data loaders were added. The spell slice intentionally reuses custom code-side DustParticle shield/vine/mirror visuals and existing vanilla sound/effect APIs. Dedicated spell icons, custom particle types/textures, manual art, persistent water-mirror reflection state, stricter locked-gate UI, and live-world visual smoke checks remain deferred.
## 0.1.316 Foundation utility and FTB current-tree recheck resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, worldpack data files, or runtime data loaders were added by the `0.1.316` reconciliation build. Deferred resource debt remains the same as the `0.1.315` Foundation utility spell and FTB chapter notes: dedicated spell icons/particles/textures, quest icons/tasks/rewards, locked-gate UI, and live visual smoke checks.

## 0.1.319 Secret elemental spell resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, worldpack data files, or runtime data loaders were added. The spell slice reuses custom code-side DustParticle visuals and existing vanilla sound/effect APIs for `life_fire`, `lieyan_true_fire_secret`, and `five_element_fusion_burst`. Dedicated high-tier spell icons, custom particle texture assets, manual art, stricter prerequisite-method/root lock presentation, and live-world visual smoke checks remain deferred.

## 0.1.320 Secret elemental and refinement alias coordination resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, worldpack data files, or runtime data loaders were added by this coordination pass. Deferred resource debt remains the same as the secret elemental spell and refinement alias slices: dedicated spell icons/particle assets, exact material art/carriers/acquisition, refinement forge resources, JEI presentation, and live smoke checks.
## 0.1.323 FTB Tiannan seven-sects and craft branch placeholder note

The new Tiannan seven-sects FTB chapter intentionally uses checkmark-only tasks. It exposes Huadao blade trials, Giant Sword Gate relic restoration, Thousand Bamboo puppet apprenticeship/tower/Dayan pages, Yuling beast-puppet and spirit-beast contracts, Yanyue illusion lessons/seven-sect tournament/inner secrets, Tianfu talisman grade progression, and craft-master alchemy/puppet/formation/refinement convergence without trusting client-side costs, granting rewards, locking branches, writing sect/reputation/skill/craft state, spawning encounters, or mutating cultivation capabilities. Remaining placeholders include item/advancement/custom task conditions, mutually exclusive sect or demonic branch locks, reward bridges, NPC triggers, icons, craft-station validation, puppet/beast entity wiring, talisman recipe tasks, and live FTB client smoke checks.
## 0.1.327 Foundation sword technique resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, worldpack data files, or runtime data loaders were added. The sword spell slice reuses code-side DustParticle sword traces, existing vanilla sounds/effects, and the already registered mod-owned `SwordProjectileEntity`. Dedicated sword spell icons, custom particle texture assets, manual art, stricter Qingyuan/equipped-artifact lock presentation, source-accurate sect/manual acquisition, richer persistent sword-domain/array behavior, PvP balance passes, and live-world visual smoke checks remain deferred.

## 0.1.329 Sword expansion spell resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, artifact/refinement maps, worldpack data files, or runtime data loaders were added by the sword expansion verification. The six added sword effects reuse code-side DustParticle sword traces, current vanilla sound/effect APIs, and the existing server-authoritative technique release path. Dedicated sword spell icons, custom particle texture assets, manual art, stricter prerequisite/equipped-artifact lock presentation, source-accurate sect/manual acquisition, persistent multi-tick sword-array/domain resources, PvP balance passes, and live-world visual smoke checks remain deferred.
## 0.1.345 FTB Ascension Border item-task bridge placeholder note

The five upgraded Ascension Border FTB tasks are intentionally non-consuming inventory checks only. They make existing Alliance Merit Token, Wind Feather Raft Ticket, Beast Core, Diyuan Permit, and Pressure-Resist Charm carriers visible to FTB progression, but they do not grant rewards, consume costs, lock branches, write `QuestProgress`, sync Seeking Immortals quest state, trigger NPC/server events, validate advancement/custom conditions, prove reputation/craft-station work, spawn encounters, or prove live client behavior. Remaining placeholders include wider item-task coverage, advancement/custom tasks, server-authoritative reward bridges, branch locks, reputation validation, boss/encounter wiring, quest icons, and live FTB client smoke checks.
## 0.1.354 Dao spell resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, artifact/refinement maps, worldpack data files, or runtime data loaders were added. The Dao spell slice intentionally reuses code-side `DustParticleOptions`, vanilla sounds/effects, and the current server-authoritative technique release path for seven Daoist ids. Dedicated Dao spell icons, custom particle texture assets, manual art, strict Taixu/source gate presentation, source-accurate acquisition, PvP tuning, and live-world visual smoke checks remain deferred.
## 0.1.355 Yellow Umbrella refinement recipe resource note

No new placeholder textures, models, item ids, blocks, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, worldpack files, artifact/refinement catalogs, or runtime data loaders were added. The recipe slice reuses the existing Yellow Umbrella artifact carrier, Spirit Silk, Beast Core, Spirit Iron, and vanilla shaped crafting as a temporary refinement path. A proper refinement forge/custom serializer, exact 5+2+3 source-count handling, realm and forge-grade checks, manual unlocks, success/failure mechanics, JEI presentation, source-accurate acquisition, and live recipe-book smoke checks remain deferred.
## 0.1.357 FTB Mulan/Tianlan/Demonic item-task bridge placeholder note

The five upgraded Mulan/Tianlan/Demonic FTB tasks are intentionally non-consuming inventory checks only. They make existing Beast Core, Soul Fragment, and Demonic Blood Coral carriers visible to FTB progression for beast-taming, beast-soul training, Guiling Gate contact, Tianmo blood rite, and North Waste demonic-core routing, but they do not grant rewards, consume costs, lock branches, write `QuestProgress`, sync Seeking Immortals quest state, validate Mulan/Tianlan faction state, validate beast contracts, validate demonic karma, trigger NPC/server events, spawn encounters, or prove live client behavior. Remaining placeholders include wider item-task coverage, advancement/custom tasks, server-authoritative reward bridges, branch locks, faction/karma validation, boss/encounter wiring, quest icons, and live FTB client smoke checks.
## 0.1.358 13x13 realm gate resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, worldpack files, artifact/refinement maps, spell data, or runtime data loaders were added. The portal slice reuses the existing Spirit Gathering Array and Spirit Ore resources plus the current worldpack default-anchor generator. Dedicated portal art, higher-tier frame materials, live in-game smoke checks, route/quest presentation, event-gated Demon Rift entry enforcement, and terrain/structure dressing remain deferred.
## 0.1.360 Artifact failure-loot resource note

No new placeholder textures, models, item ids, blocks, recipes, loot tables, GUI assets, packets, entities, capabilities, shop data, FTB files, worldpack files, artifact/refinement catalog files, or runtime data-loader files were added. The command failure-salvage slice reuses the already shipped `refinement_failure_loot.json`, existing item carriers, and one temporary `scrap_spirit_iron` id-map alias to Spirit Iron. Dedicated scrap itemization, failure explosion/mismatch rules, workstation UI resources, custom serializer resources, JEI presentation, and live smoke checks remain deferred.
## 0.1.364 FTB Tiannan seven-sects item-task bridge placeholder note

The five upgraded Tiannan seven-sects FTB tasks are intentionally non-consuming inventory checks only. They make existing Spirit Iron, Beast Core, and Fire Talisman carriers visible to FTB progression for Huadao forge work, Giant Sword relic restoration, Yuling beast-puppet binding, spirit-beast contract routing, and Tianfu low talisman certification, but they do not grant rewards, consume costs, lock branches, write `QuestProgress`, sync Seeking Immortals quest state, validate sect/craft-state, validate beast contracts, validate talisman crafting, trigger NPC/server events, spawn encounters, or prove live client behavior. Remaining placeholders include wider item-task coverage, advancement/custom tasks, server-authoritative reward bridges, branch locks, craft-state/reputation validation, boss/encounter wiring, quest icons, and live FTB client smoke checks.
