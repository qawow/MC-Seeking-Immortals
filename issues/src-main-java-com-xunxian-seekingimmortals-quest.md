# 审计报告：src/main/java/com/xunxian/seekingimmortals/quest

- 审计文件数：26
- 有问题文件数：26
- 问题总数：91
- 规则：每个文件一个独立代理；每个代理从目标文件映射全项目调用链和资源边界。
- 工作流：tools/audit-current-directory-files.js（AUDIT_TARGET_DIR=src/main/java/com/xunxian/seekingimmortals/quest，AUDIT_EXTENSIONS=.java）
- 核对结果：发现文件数 26 == 启动代理数 26 == 返回结果数 26，无遗漏。
- 严重度分布：high 2，medium 20，low 69；类型分布：bug 37，security 7，perf 1，smell 46。

## 发现（按严重度、再按文件路径排序）

### DetailedQuestProofEvent.java

- **high / bug**（第 206 行）：跨模块接线缺口：95 条证明路由中有 11 条（11/23 个任务链）在正常游玩中永远无法被任何服务器事件产生，任务链永久卡死（仅 op2 adminProve 可绕过）。(a) INFO_ACKNOWLEDGED：mortal_qixuan_entry:1、zhenyan_outer_lesson:4、tianyuan_to_fengyuan_gate:4、dayan_clue:4 的 choice token（mortal_qixuan_entry_step_1/true_word_exam_passed/tianyuan_return_fee/dayan_complete_rarity_rule）不在 INFO_CHOICE_SOURCES 中，而 infoAcknowledged 工厂唯一生产者是 recordDialogueNode，无法产出这些 token；(b) CHOICE_COMMITTED：bf_mid_contest/inverse_star_alignment/gh_true_immortal_pressure 不在 CHOICE_COMMITTED_SOURCES 中（secret_realm_runtime.json 里它们只是遭遇层 id，不是对话节点）；(c) SHOP_TRANSACTION：heifeng_sea_route/dajin_jin_capital_rim/ziling_exchange 不在 MARKET_SHOPS（种子列表+merchant_shops_runtime.json 54 个店）中，购买路径永远无法触发；(d) AUCTION_TRANSACTION：zhuimo_token:1 的 fallen_demon_token 不存在任何拍卖场馆 id 且无 PROOF_AUCTION_MAPPINGS 映射。最严重的是 mortal_qixuan_entry 第 1 步（教程链首步）对所有新玩家不可完成。
  证据：src/main/resources/data/seeking_immortals/text_material/detailed_quest_proof_routes.json:5/17/27/37/41/53/64/71/85/89/97、DetailedQuestProofService.java:98/107/781/796、src/main/java/com/xunxian/seekingimmortals/shop/ShopService.java:217、src/main/resources/data/seeking_immortals/catalog/merchant_shops_runtime.json、src/main/resources/data/seeking_immortals/text_material/economy_auction_bands.json

- **low / bug**（第 270 行）：secretRealmLayerEntered 的 worldRegionId 参数误传 realmId 而非实际的 regionId（.withWorld("", realmId, realmId, ...)），与同文件的 secretRealmEncounterCleared（正确传 region）不一致。后果：historyEntry 将当前区域字段持久化为 realm id（HISTORY_TAG "Region" 存的是血炼狱等境界 id 而非 bf_water_jiao 等区域 id），事件的世界上下文语义被破坏；当前仅因秘密领域校验路径不读取 currentRegionId 且 HISTORY 重放只检查非空而免于出错，任何后续消费方读取该字段都会得到错误数据，属于易碎的隐性依赖。
  证据：DetailedQuestProofEvent.java:264/185、DetailedQuestProofService.java:1175/1012

- **low / smell**（第 30 行）：类型接线 fail-open：authoritativeState 的 switch 对未覆盖的 proofType 返回 default -> true，且 Type 枚举新增常量与目录 PROOF_TYPES/EXPECTED_PARAMETER/authoritativeState 之间没有任何联动校验。未来若在目录中注册新 proof_type 但忘记补 authoritativeState 分支，该路由将完全跳过状态校验（仅剩 producer+参数精确匹配门），直接放行。在验证关键路径上应 fail-closed。
  证据：DetailedQuestProofService.java:1000、DetailedQuestProofCatalog.java:36

- **low / smell**（第 75 行）：writeLedger 的淘汰策略注释为 "oldest"，实际按 getAllKeys().sorted() 移除字典序最小键（ledger key = route.eventId + "|" + eventKey），并非最早写入；CompoundTag 键序不保证插入序，淘汰是任意的。影响：当账本达到 512 条上限时可能淘汰仍有效的区域证明条目，使 hasRegionProof（链前置条件 reached_water_zone 依赖它）与去重保护静默失效，被证明过的路由在进度回滚场景下可被重新推进。
  证据：DetailedQuestProofService.java:1292/580、DetailedQuestRuntimeService.java:452

### TextQuestDialogueTreeService.java

- **high / bug**（第 45 行）：nodeFor 的线性钳位把进行中的 stage 直接映射为节点索引，而 complete 节点是列表最后一个元素（index = nodes.size()-1 = 4）。当 stage >= 4 且 complete==false 时（例如 huangfeng_cultivation_path step_count=5 的进行中 stage 4，或 ghost_path step_count=6 的进行中 stage 4/5），idx = min(4, stage) = 4 会命中 complete 节点：对话显示『此线已完结』并只提供 talk 选项，玩家在最后一两个进行中阶段无法通过对话树的 advance 选项推进/完成该链（act/actCurrent 的 choiceIds 白名单中也没有 advance）。quest_chains_index.json 中 step_count>=5 的链共 15/62 条（huangfeng_cultivation_path、chaotic_sea_politics、ghost_path、spirit_realm_rise、mulan_war_campaign、demonic_six_path、chaotic_sea_civil_war、chain_ascension_spirit_world、blood_forbidden_campaign、void_palace_campaign、fallen_demon_campaign、kunwu_mountain_campaign、nether_river_campaign、diyuan_campaign、dajin_righteous_demon_line）全部受影响；且同一钳位使 step_count>=6 的链在 stage 3 就显示 finale 文案（未按 stepCount 缩放）。应把进行中 stage 钳位到 nodes.size()-2（finale），或按链的 stepCount 做比例映射。
  证据：TextQuestDialogueTreeService.java:45/40、TextQuestChainService.java:135/404、TextQuestDialogueService.java:43、src/main/resources/data/seeking_immortals/catalog/quest_chains_index.json:13/76/345

- **low / smell**（第 34 行）：现有测试只覆盖 stage=0（start 节点）与 complete=true（complete 节点），从未覆盖『进行中且 stage >= nodes.size()-1』的状态组合（如 huangfeng stage=4, complete=false），正是上述钳位泄漏的发生区间，导致该回归未被测试套件拦截。建议补充 in-progress 高 stage 的断言（应返回 finale 节点且保留 advance 选项）。
  证据：src/test/java/com/xunxian/seekingimmortals/quest/TextQuestDialogueTreeServiceTest.java:17/21

### DetailedQuestProofCatalog.java

- **medium / smell**（第 202 行）：硬编码 EXPECTED_CHAIN_COUNT=23 / EXPECTED_STEP_COUNT=95（31-32、202-207 行）与 validateCoverage 的 1:1 覆盖校验完全冗余：任何一条链/一步的数据变更都必须同步改本类常量，否则类静态初始化（99 行 BUILTIN）抛 IllegalStateException，表现为 ExceptionInInitializerError 整体启动崩溃。同时同一份路由文件与源文件在类初始化阶段被解析/校验两次（本类 99 行 + DetailedQuestRuntimeService 62-63 行），放大了数据-代码同步维护面。
  证据：DetailedQuestProofCatalog.java:31/32/99/205、DetailedQuestRuntimeService.java:63

- **medium / smell**（第 289 行）：跨模块契约缺口：目录已对 method（289-291 行，TextMaterialCatalogService）与 realm（293-299 行，Realm.fromDesignId）做领域目录交叉校验，但对其余参数只做语法校验，导致多条路由引用的 id 在运行时无任何产生方，对应任务链在正常游戏中永久卡死（fail-closed，无利用面）：SHOP_TRANSACTION 的 heifeng_sea_route（heifeng_gray_sail:1）、dajin_jin_capital_rim（yinyang_ku_intel:1）、ziling_exchange（deity_huoyu_path:7）不在 PROOF_SHOP_MAPPINGS 亦无同名真实商店；AUCTION_TRANSACTION 的 fallen_demon_token（zhuimo_token:1）不在 PROOF_AUCTION_MAPPINGS；ENCOUNTER_CLEARED 的 wuxing_shallow_trial（wuxing_intro:4）、gray_realm_border（court_hunt_gray:3）、heifeng_sea（heifeng_gray_sail:2）在 encounterRegionsForPhase 中无来源（DetailedQuestProofService.java:1058 注释自证「have no producer yet」）。目前被 Q-B 分期计划掩盖，目录本可在加载期 fail-fast 拦截此类漂移。
  证据：src/main/resources/data/seeking_immortals/text_material/detailed_quest_proof_routes.json:45/51/53/64/71/97、DetailedQuestProofService.java:58/80/1058/1316

- **low / smell**（第 267 行）：PROOF_TYPES（36-42 行）与 EXPECTED_PARAMETER/EXPECTED_PRODUCER（54-97 行）是手工维护的独立集合：若未来向 PROOF_TYPES 新增 proof_type 而未同步两个 Map，第 267 行 Set.of(expectedKey)（expectedKey 为 null，Set.of 拒绝 null）与第 271 行 EXPECTED_PRODUCER.get(...).equals(...) 都会抛出未捕获 NPE，且发生在静态初始化期（99 行），表现为不可诊断的初始化崩溃而非校验错误。当前 21 个类型全部被覆盖，属潜在维护风险。
  证据：DetailedQuestProofCatalog.java:36/54/76/267/271

- **low / smell**（第 441 行）：同一文件内 id 大小写校验策略不一致：validId（440-442 行）先 toLowerCase 再匹配，大写/混合大小写 id 可通过校验，与声明契约 `[a-z][a-z0-9_]*`（33 行）及报错文案「must be a lowercase id string」（334 行）不符；event_id 期望值用原始（未归一化）chainId 拼接（275-276 行），FAILURE_KEY 却大小写敏感（255 行）。全大写数据可通过全部校验并在 Route 构造器中被归一化后正常运行，实际数据全小写（95 条路由零错误），无现行影响，但校验强度弱于声明。
  证据：DetailedQuestProofCatalog.java:33/255/275/334/441

- **low / smell**（第 172 行）：误导性失败信息：readObject（398-411 行）在资源缺失与 JSON 解析失败（异常仅写 LOGGER）两种情况下都返回 null，loadAndValidate（171-173 行）一律抛「Missing detailed quest proof route resource」。当文件存在但损坏时，类初始化以错误定位的报错崩溃，排障成本高。
  证据：DetailedQuestProofCatalog.java:172/399/408

- **low / smell**（第 239 行）：死代码：239、243 行的 `"ADMIN_ONLY".equals(...)` 永远为 false——ADMIN_ONLY 既不在 OWNER_POLICIES（43 行）也不在 PARTY_POLICIES（44 行）中，`!set.contains(...)` 已保证拒绝，两处显式判断冗余；另外 12 参数兼容构造器 Route（124-130 行）在 src/main 与 src/test 全仓库无任何调用方（仅 313 行的 13 参数构造器被使用），属未使用 API，且其将 minimumLayer/minimumRealm 静默置 0/空，若未来被调用会产生与校验语义不一致的路由。
  证据：DetailedQuestProofCatalog.java:43/44/124/239/243

- **low / smell**（第 69 行）：测试与实现契约漂移：DetailedQuestProofCatalogTest 的 PARAMETER_BY_TYPE 把 TECHNIQUE_LEARNED 映射为参数键 "method"，而目录（69 行）与事件工厂（DetailedQuestProofEvent.java:108-113）均使用 "technique"。由于当前 95 条路由中没有 TECHNIQUE_LEARNED 类型，该条目从未被执行到，错误被掩盖；未来新增第一条 TECHNIQUE_LEARNED 路由时，即使数据与目录一致，测试也会因键不匹配而误报失败。
  证据：DetailedQuestProofCatalog.java:69、src/test/java/com/xunxian/seekingimmortals/quest/DetailedQuestProofCatalogTest.java:35、DetailedQuestProofEvent.java:108

### DetailedQuestProofService.java

- **medium / bug**（第 98 行）：多个路由永久无法被证明（fail-closed 但已随正式剧情链发布）：INFO_CHOICE_SOURCES 未覆盖 mortal_qixuan_entry_step_1、tianyuan_return_fee、true_word_exam_passed、dayan_complete_rarity_rule；CHOICE_COMMITTED_SOURCES 未覆盖 bf_mid_contest、inverse_star_alignment、gh_true_immortal_pressure；PROOF_SHOP_MAPPINGS 的 heifeng_sea_route/dajin_jin_capital_rim/ziling_exchange 不是任何真实市场商铺 id（不在 MARKET_SHOPS 种子及 merchant_shops_runtime.json 的 54 个 id 中）；fallen_demon_token 无拍卖场；encounterRegionsForPhase 对 wuxing_shallow_trial/gray_realm_border/heifeng_sea 无分支且 recordEncounterCleared 仅由 secret-realm 清除触发，普通遭遇无调用方。这些 token 唯一的产生途径是 recordDialogueNode（378-416 行）与 encounterRegionsForPhase，全项目 grep 无其他 producer。后果：新玩家主入门链 mortal_qixuan_entry（进入 qinglan_mountains/qixuan_village/tiannan 自动启动）第 1 步永久卡死，kunwu_clue_assemble 之外共 14/95 步软锁，对应 11 条链无法完结。测试只把这些 token 钉为 FAIL_CLOSED 清单（并未阻止发布），属于已发布内容中的永久卡关。
  证据：DetailedQuestProofService.java:98/107/80/86/1058/378、DetailedQuestDialogueProofRouteTest.java:32、detailed_quest_proof_routes.json:5、QuestHookRuntime.java:54

- **medium / bug**（第 87 行）：跨模块拍卖场馆 id 契约不匹配：路由 dajin_wanbao_auction 的证明映射只接受 wanbao_auction（或自身），但 AuctionSoftService.mergeWanbaoLots 的 defaultVenue 在所有场馆 id 都不含 'wanbao'（auction_catalog_v93.json 的 9 个场馆：tainan_xiaohui/tiannan_trade_fair/...）时回退到 venues.get(0).id()='tainan_xiaohui'，因此万宝楼/大晋拍品每次成交记录的 venue.id() 都是 'tainan_xiaohui'，'wanbao_auction' 这个 id 永远不会出现。recordAuctionTransaction 是唯一的 AUCTION_TRANSACTION 生产者，导致 kunwu_clue_assemble 第 1 步（拍卖）永久无法被证明、整条链在第 1 步软锁；而 DetailedQuestDialogueProofRouteTest 把它归类为可产出（FAIL_CLOSED_AUCTIONS 只含 fallen_demon_token），测试与实际运行时场馆解析不一致。
  证据：DetailedQuestProofService.java:87、AuctionSoftService.java:877/303、auction_catalog_v93.json:7

- **low / smell**（第 134 行）：Route 的 owner_policy/party_policy/consume_policy/repeat_policy 四个字段被目录校验（DetailedQuestProofCatalog 103-135 行）且随 mod 发布，但本服务从未读取：record() 硬编码 owner==player UUID（134-136 行）且处理链中不检查任何 policy。后果：配置了 PARTY_LEADER/PARTY_ONLY 等策略的路由实际等同 PLAYER/SOLO（组队击杀/交付按击杀者本人记账，队长任务无法被队员证明）；consume_policy=ON_SUCCESS 不会消耗；repeat_policy 全部失效（行为恒为 IDEMPOTENT）。任何依赖这些字段的后续路由作者会得到与配置不符的运行时行为。
  证据：DetailedQuestProofService.java:134/149、DetailedQuestProofCatalog.java:103

- **low / smell**（第 1293 行）：writeLedger 的淘汰策略按字典序删除第一个 key（ledger.getAllKeys().stream().sorted().findFirst()），并非最旧条目；若将来 512 上限被触达，当前步骤的 ledger 条目可能被淘汰，而 replayCurrent（600-631 行）只依赖 hasLedger 去重，会重新推进同一路由（重复发奖、跳步）。当前 95 条路由 + 95 条 admin 记录远低于 512，实际不可达，属潜在隐患。
  证据：DetailedQuestProofService.java:1293/600

### DetailedQuestRuntimeService.java

- **medium / bug**（第 518 行）：Five authored reward/prerequisite items are not registered anywhere (no ModItems/ModBulkItems entry, no item_id_aliases.json alias): zhui_mo_ling, lingzhu_fruit, yin_zhi_horse_live, peiying_dan, court_warrant_gray. rewardPreflight() therefore returns false and advanceInternal() permanently blocks with 'reward_unavailable' the steps zhuimo_token:1, court_hunt_gray:2, peiying_material_hunt:2 and :3, lingzhu_fruit_run:3. In addition the lingzhu_fruit_run prerequisite 'zhui_mo_ling' (matchesPrerequisite -> hasItem -> countItem) can never be satisfied, so that chain can never start even after the zhuimo_token cascade. grantCatalogItem() fails silently, so no item is ever granted and no ledger entry is written, making the block permanent.
  证据：DetailedQuestRuntimeService.java:514/549/656、src/main/resources/data/seeking_immortals/text_material/quest_chains_playable_v141.json（zhuimo_token step1 item=zhui_mo_ling；peiying_material_hunt step2/step3 item=yin_zhi_horse_live/peiying_dan；court_hunt_gray step2 item=court_warrant_gray；lingzhu_fruit_run prereq=zhui_mo_ling）、src/main/resources/assets/seeking_immortals/catalog_bulk_items.json（无这些条目）

- **medium / bug**（第 683 行）：'new_game' prerequisite maps to noDetailedProgress(), i.e. ROOT_TAG must be completely empty, and it is evaluated against all chains. QuestHookRuntime auto-starts several no-prerequisite chains on login/region reach (kunwu_clue_assemble, yinyang_ku_intel, xutian_window_prepare, zhuimo_token, qianzhu_tower_trial, deity_huoyu_path via REGION_TO_DETAILED_CHAINS/REALM_TO_DETAILED_CHAINS). Any player whose first started chain is not mortal_qixuan_entry (e.g. spawned or teleported into dajin/chaotic_sea/extreme_west) permanently loses access to the intro chain mortal_qixuan_entry: start() -> canStart() fails, the admin claim command also goes through start(), and no command can clear ROOT_TAG. The chain is permanently unstartable with no recovery path.
  证据：DetailedQuestRuntimeService.java:682/203、QuestHookRuntime.java:121/177/53、src/main/resources/data/seeking_immortals/text_material/quest_chains_playable_v141.json（mortal_qixuan_entry prereq=new_game）

- **low / bug**（第 332 行）：Unchecked index access chain.steps().get(progress.stage() - 1) in recordAndAdvance(), turnIn() and showCurrentStep(): progressOf() caps Stage only at 0 (Math.max(0, ...)) and never at steps.size(). A player.dat edited externally (singleplayer/NBT tools) or a future data shrink (chain steps reduced between mod versions while player Stage is preserved) throws IndexOutOfBoundsException on the server thread every time any kill/craft/pickup/dialogue hook fires. advanceInternal() is guarded at line 257, but these three call sites are not.
  证据：DetailedQuestRuntimeService.java:332/361/388/179

- **low / bug**（第 549 行）：Contract mismatch between rewardPreflight() and applyRewardField(): rewardPreflight() returns true when the 'item' field is NOT a JsonPrimitive (515-517 行, `!isJsonPrimitive()` passes), but applyRewardField() then unconditionally calls value.getAsString() on the 'item' key (549 行). A non-primitive 'item' (e.g. an array for multi-item rewards, which the preflight would explicitly accept) throws UnsupportedOperationException from within applyRewardOnce(). Because spendContribution() (277-282 行) runs before applyRewardOnce(), the player loses the contribution while the stage is not persisted (state write at 289-298 行 never runs), so the step must be repeated and re-paid. Currently no authored reward triggers it (all 'item' values are strings), but the preflight gate gives a false guarantee.
  证据：DetailedQuestRuntimeService.java:514/549/277

- **low / smell**（第 701 行）：recordEvidence()'s MAX_EVIDENCE=256 eviction (remove lexicographically first key) is unreachable: only KNOWN_EVIDENCE tokens can be stored and buildKnownEvidence() currently yields 212 tokens. If the quest data grows past 256 distinct tokens, the eviction silently deletes the alphabetically-first evidence key (e.g. 'bf_herb_ring'-type region/step tokens and chain-id tokens used by prereq gating), permanently breaking unrelated chains with no log. There is no per-token criticality protection or warning.
  证据：DetailedQuestRuntimeService.java:699/713

- **low / smell**（第 277 行）：Non-atomic transaction in advanceInternal(): spendContribution() (277 行), applyRewardOnce() (284 行) and the stage/Complete persistence (289-298 行) are three separate persistent-data writes. A server crash or listener exception between them leaves the contribution spent without advancement (retry re-spends), or the reward granted without the stage saved (retry passes the ledger check, advances, and the step reward is skipped forever). All writes are synchronous on the server thread so the window is a crash/exception-only risk, but the reward ledger gives a false impression of idempotency for the whole transaction.
  证据：DetailedQuestRuntimeService.java:277/284/289

### FtbCustomTaskHooks.java

- **medium / security**（第 157 行）：镜像分支（ready.isEmpty() → data.setProgress(1L)，行 153-159）在完成 FTB 团队共享任务进度时，既不检查 isAuthoritySafeQuest（无奖励、无消耗性任务），也不执行 singleAuthorityPlayer 单一权威校验；而同族路径——ready 分支（行 161）和 ObjectCompletedEvent.QUEST 回退写入（行 91）——都强制这两项检查。CustomTask 的 check 由 FTB 每在线成员每秒触发一次（autoSubmitOnPlayerTick==20），因此任意一名队伍成员的个人原生账本/声望满足镜像标签，即可为整个队伍完成该任务并解锁 FTB 奖励。当前捆绑 SNBT 被测试强制为无奖励（FtbQuestSnbtTest:244-245），但运行时自身未实施该防护，第三方包或未来捆绑内容一旦在镜像任务上配置 FTB 奖励，就会发生跨成员归属的团队奖励发放，绕过本文件其余部分刻意建立的权威/奖励归属边界。
  证据：FtbCustomTaskHooks.java:153-159/161/91-95、src/test/java/com/xunxian/seekingimmortals/quest/FtbQuestSnbtTest.java:244-245

- **low / bug**（第 172 行）：checkCustomTask 中 singleAuthorityPlayer 的实参 data.teamData().getOnlineMembers() 在进入方法体（及其 try/catch 与 isManagerLoaded() 防护）之前就被求值；TeamData.getOnlineMembers() 内部直接调用 FTBTeamsAPI.api().getManager()，而该调用是 Objects.requireNonNull(TeamManagerImpl.INSTANCE)，在 FTB Teams 管理器未加载时抛出 NPE。该异常会穿过 CustomTask.Check → submitTask → FTBQuestsEventHandler.playerTick，成为服务端 tick 崩溃向量，使方法内 191-194 行 'native write rejected' 的防护意图落空（防护只覆盖 getManager() 之后的查询，不覆盖调用点参数求值）。FTB 管理器为 null 的窗口极小（FTB Teams 初始化失败/服务端关闭边缘），但这是明确的防护顺序缺陷：防护应前置到调用点。
  证据：FtbCustomTaskHooks.java:171-172/191-194

### FtbDefaultPackManifest.java

- **medium / bug**（第 10 行）：FILES 清单新增文件（新章节）时，自动升级路径被自己的旧状态文件永久阻断：readState 对每个 FILES 条目强制要求 state 文件里有匹配的 sha256 键（FtbQuestDefaults.java:170-176），而 state 文件是安装器自写（writeState 只写当前 FILES 的条目）。版本升级后 FILES 增加条目 → 旧 state 缺键 → StateRead.invalid → inspect 直接返回 unsafe（:190-192）→ installKnownPack 升级分支永远不可达（:97-100 转 PRESERVED_CUSTOMIZED），每次启动都重写 pending/<REVISION>，玩家安装永远不会自动升级，只能手动删 state 文件或手工合并 pending。REVISION 与 historicalHashes 机制表明升级场景是被设计支持的，此处属于自有状态迁移的死锁。
  证据：FtbDefaultPackManifest.java:10、FtbQuestDefaults.java:170/176/191/98

- **low / smell**（第 10 行）：从 FILES 清单中移除章节文件时没有任何清理/墓碑机制：inspect 与 installKnownPack 只遍历 bundled（FILES），state 文件也只记录 FILES 条目，被移除的旧章节文件会永久留在 config/ftbquests/quests/chapters 中；FTB Quests 会继续加载该文件夹下所有 .snbt，导致已删除章节的过期任务在游戏内长期残留。安装器对用户文件的保守不删除策略（有测试固化）使安装器无法区分自己装过又被下架的章节与用户文件，版本演进中删除章节时必现内容漂移。
  证据：FtbDefaultPackManifest.java:10、FtbQuestDefaults.java:195/311、src/test/java/com/xunxian/seekingimmortals/quest/FtbQuestDefaultsTest.java:130

- **low / smell**（第 10 行）：章节清单被三处手工重复维护且只有两处有测试约束：FtbQuestBridgeService.loadBuiltin 手写 9 个 ChapterSeed，其 :88 注释引用了不存在的字段 FtbQuestDefaults.SEED_FILES（该类只定义 RESOURCE_ROOT/TARGET_ROOT/MANAGEMENT_ROOT/STATE_FILE），桥接层清单与 quest_handbook_i18n_v1.json 的 ftb_chapters 由 QuestHandbookCoverageTest 校验一致，但没有任何测试把两者与 FtbDefaultPackManifest.FILES 对齐（FtbQuestDefaultsTest 只校验 FILES↔资源↔state）。新增/删除章节时 manifest 与桥接/手册映射会静默偏离，安装器管理的文件与运行时章节投影互不同步。
  证据：FtbDefaultPackManifest.java:10、FtbQuestBridgeService.java:88/89、src/test/java/com/xunxian/seekingimmortals/resources/QuestHandbookCoverageTest.java:68、src/test/java/com/xunxian/seekingimmortals/quest/FtbQuestDefaultsTest.java:269

### FtbQuestCompatBootstrap.java

- **medium / security**（第 21 行）：The reflectively registered hook binds team-scoped custom-task progress (data.setProgress(1L)) for player-specific gates si_rep_<faction>_<min>, si_native_<chain>_<stage> (mirror) and si_war_active without the single-authority / reward-safety guards that the same file's sibling paths enforce. FTB Quests invokes task.check(data, player) per online team member on the check timer, so in a party of 2+ any single member whose own native progress or reputation satisfies the spec completes the FTB quest for the whole team, granting team-wide FTB rewards for state other members never earned. The write path (si_native_ready_* and the ObjectCompletedEvent.QUEST fallback) explicitly rejects exactly this with singleAuthorityPlayer (requires exactly one full member == one online member) and isAuthoritySafeQuest (quest must be reward-free), but checkCustomTask's non-NativeReady branch applies neither, and quests hosting mirror/rep tasks are not required to be reward-free. Cross-member reward leakage with the mod's own authority model as evidence of intended protection.
  证据：FtbCustomTaskHooks.java:149/157/171/181、FtbNativeQuestSync.java:137/147

- **low / smell**（第 21 行）：The reflectively invoked register() binds three handlers sequentially (CustomTaskEvent, ObjectCompletedEvent, then CustomClickEvent) and sets the idempotency flag only at the end. Because mods.toml declares ftbquests optional with a version range that is not hard-enforced (an optional dep present-but-out-of-range only logs a warning), a partially incompatible FTB/FTB Library API can throw LinkageError after the first or second registration; the bootstrap catches and logs it but never retries (it is invoked exactly once from the mod constructor). Result: permanently inconsistent runtime where the task-check binding is active but the native-write fallback or Patchouli click handler silently remains unregistered, with no recovery path.
  证据：FtbCustomTaskHooks.java:65/108、FtbQuestCompatBootstrap.java:24、src/main/resources/META-INF/mods.toml:76

### FtbQuestDefaults.java

- **medium / bug**（第 173 行）：State machine has no repair path for missing/invalid state: readState() requires a sha256.<file> key for EVERY file in the current manifest and returns StateRead.invalid when any key is absent or malformed. An invalid state makes inspect() return unsafe, which makes installDefaultPack() write the pending pack and return PRESERVED_CUSTOMIZED without ever rewriting the state file. Consequence: the first time a maintainer adds a new file to FtbDefaultPackManifest.FILES (the normal way to ship a new quest chapter), every existing installation that has a state file from an older revision is permanently stuck at PRESERVED_CUSTOMIZED and never auto-upgrades — the new chapter is silently never deployed and a pending pack is regenerated on every boot. Deleting the state file does not recover either: with the new file absent from disk, the legacy path returns 'Legacy managed pack is incomplete' (present != bundled.size()), so the only escape is manual copying plus state deletion. A state file written by a crash/corruption lands in the same permanent-preserve state. There is no code path that rewrites the state when the file set of a newer manifest grows.
  证据：FtbQuestDefaults.java:171/172/173/190/191/97/98/239/240/241

- **low / bug**（第 323 行）：Incomplete rollback in installKnownPack(): the catch block calls rollback(replaced), but an ExistingFile is appended to 'replaced' only AFTER moveAtomically() succeeds. If a move fails after partially modifying the destination (possible on the non-atomic fallback path Files.move(source, target, REPLACE_EXISTING), e.g. copy-based cross-device or non-ATOMIC_MOVE filesystems), the file that failed to move is missing from 'replaced' and is not restored by rollback(), leaving a mixed pack: earlier files rolled back to old content, the failed file in an undefined (possibly truncated/deleted) state, later files untouched. The pre-migration backup (backups/before-<rev>-<fp>) and the pending pack do preserve the data, and the next run detects the mismatch, so impact is manual recovery rather than data loss, but the rollback contract is incomplete.
  证据：FtbQuestDefaults.java:322/323/326/327/328/397/456/463/464

### FtbRewardBridgeService.java

- **medium / bug**（第 25 行）：防御性 fallback 分支会永久污染单向权威账本而不发放任何奖励：当 onTextQuestFinished 从「未先经过 grantAuthorityFinaleReward」的调用点进入时，markAuthorityReward 把该链标记为已发奖，而 TextQuestChainService.grantAuthorityFinaleReward 与 grantBranchFinaleBonus 在账本已置位时无条件跳过发放，hasAuthorityReward 还会把旧 FTB tag 永久迁移进账本；后果是终局物品奖励被永久吞掉，无物品、无提示、无日志、且没有任何管理命令可清除该账本条目。当前唯一调用点（TextQuestChainService.advance 第 426→429 行）总是先发放，此分支是死代码，但任何未来的 legacy FTB 完成回调（如 FtbCustomTaskHooks 风格的完成回退）一旦接入即静默触发永久拒发，属潜在地雷。
  证据：FtbRewardBridgeService.java:25/27、TextQuestChainService.java:429/881/770/903

- **low / smell**（第 23 行）：第 23 行 toLowerCase() 使用 JVM 默认 locale，而全模块其余键归一化均用 Locale.ROOT（TextQuestChainService:1317、QuestRewardService:388、FtbNativeQuestSync:248，QuestTrackerActionPacket:39 亦然）；在 tr_TR 等 locale 下含大写 I 的 chainId 会被折叠成无点 ı，生成的 legacy FTB tag 键与权威账本/迁移查询不一致，破坏去重（重复发放或永久拒发）且无编译期检查。当前唯一调用方传入的已是 Locale.ROOT 归一化后的小写 ASCII id，本分支的 toLowerCase 实际为空操作，故仅为一致性隐患。
  证据：FtbRewardBridgeService.java:23、TextQuestChainService.java:1317、QuestRewardService.java:388、FtbNativeQuestSync.java:248

- **low / smell**（第 15 行）：ROOT 标签字符串 seeking_immortals_ftb_reward_bridge 以魔法字面量重复出现于 TextQuestChainService.hasAuthorityReward 迁移检查（第 913 行）与 PlayerPersistentDataClonePolicy.DURABLE_KEYS（第 48 行，及克隆策略测试第 43 行），而权威账本键 AUTHORITY_REWARD_TAG 已被提升为共享常量（TextQuestChainService:47）；任一侧改名都会静默破坏旧 FTB tag→权威账本的迁移去重（重复发放或永久拒发），无编译期检查。
  证据：FtbRewardBridgeService.java:15、TextQuestChainService.java:913、PlayerPersistentDataClonePolicy.java:48

### MainStorySoftService.java

- **medium / bug**（第 194 行）：秘境界通关解锁门（secretRealmClear）读写的 NBT 键从未被写入：`meetsUnlock` 读取 `seeking_immortals_secret_realm_core_clear` / `seeking_immortals_secret_realm_mid_clear` 两个持久化 compound，但全仓库（src/main/java、scripts）不存在任何写入这两个键的代码，唯一其它引用是 PlayerPersistentDataClonePolicy 仅做克隆拷贝。真实通关状态实际存在 `SecretRealmProgressSavedData`（世界级 SavedData，按 realm id 记录）和会话级 `seeking_immortals_realm_session_clear`（离开秘境即被移除）。后果：chapter_2_foundation_secret（secretRealmClear="blood_forbidden"）和 chapter_5_deity_transformation（secretRealmClear="fallen_demon"）的该硬门永远不满足——经 TextQuestChainService 链末自动触发 completeQuiet（链完结后 advance 调用 maybeCompleteMainStory）时 meetsUnlock 静默返回 false，章节在生存模式永远无法标记完成，玩家无任何提示；仅 op 命令可绕过。另外 chapter_5 的门 id "fallen_demon" 与秘境界目录中的规范 id "fallen_demon_valley" 也不一致，即使有人按规范 id 写入该 tag 也无法匹配。这与第 210 行注释表达的"chapter_2 门票仅软拦截"的设计意图相矛盾（门票是软门但秘境门是硬门且已死）。
  证据：MainStorySoftService.java:194/195、persistence/PlayerPersistentDataClonePolicy.java:65/66、worldpack/SecretRealmSessionService.java:20/107、TextQuestChainService.java:792、src/main/resources/data/seeking_immortals/worldpack/secret_realms.json

- **low / bug**（第 110 行）：章节完成触发的时间线阶段联动对 chapter_1_sect 与 chapter_4_great_jin 永远不生效：completeInternal 将章节 id（如 "chapter_1_sect"、"chapter_4_great_jin"）传给 TimelineChronicleService.onChronicleDiscovered，而该桥接方法按关键词匹配（huangfeng/qi_refin/blood/foundation/tiannan/core/chaotic/star/nascent/dajin/void/deity/spirit/tianyuan），"chapter_1_sect" 与 "chapter_4_great_jin" 均不含任何关键词（"great_jin" ≠ "dajin"），因此完成这两个章节不会解锁任何时间线阶段，与其它五个章节行为不一致，属于跨模块契约失效（静默，无提示）。
  证据：MainStorySoftService.java:110、TimelineChronicleService.java:133/135

- **low / smell**（第 77 行）：isComplete 对同一完成标志做了两次不一致的键读取：第一次用 normalize()（trim+lowercase），第二次用原始 chapterId.trim()（未小写）。由于写入端统一使用 catalog 的小写章节 id（completeInternal 第 102 行 root.putBoolean(chapter.id(), true)），第二次查询几乎总是冗余失效，但它会静默容忍未来任何以非规范大小写写入的标志，掩盖键格式漂移类数据 bug；建议删除第二次读取或统一归一化。
  证据：MainStorySoftService.java:77/78/102

### QuestAuthorityCatalog.java

- **medium / bug**（第 93 行）：chaotic_sea_politics 线性链条在分支锁下无法完成：stages 3-4 要求 requires_branch=rebel（matchesBranch 映射到 BRANCH_DEMONIC），stage 5 要求 loyalist（映射 BRANCH_RIGHTEOUS）。而 chooseBranch 一旦选择非中立分支即永久锁定（TextQuestChainService.java:703），中立分支最多切换一次。因此正道路线玩家在 stage 3 被永久阻断，魔道路线玩家在 stage 5 被永久阻断，任何分支（含中立）都无法推进到 6 步终章，导致该链对非创造模式玩家 100% 不可完成，中途玩家的进度被永久浪费。数据中无其他旁路（stageGate 是 advance/canTransitionExact 的唯一出口）。
  证据：QuestAuthorityCatalog.java:92/144、src/main/resources/data/seeking_immortals/text_material/quest_chains.json:71/80、TextQuestChainService.java:703

- **medium / security**（第 61 行）：权限目录 fail-open：loadBuiltin() 任何解析/IO 异常都返回空表，且 startGate/stageGate 在 rule/stage 缺失时一律返回 OPEN。quest_chains.json 与链条索引 quest_chains_index.json 是两个独立加载的文件——若文本材料文件缺失/损坏，62 条链仍可从索引启动（ExtendedCatalogService 分别容错），但 karma_required、extends_chain（PARENT）、party_size_max、REALM、stage 的 requires_branch/requires/branch_any 全部静默失效，仅剩 learn_requirements 的部分门禁。魔道线可不满足 karma 直接开启，阶段门完全可绕过，且无日志提示（仅有 loadBuiltin 的 warn 在整表空时打印一次）。
  证据：QuestAuthorityCatalog.java:89/163/171、catalog/ExtendedCatalogService.java:114/232

- **low / bug**（第 96 行）：parse() 不读取步骤的 optional 标记，stageGate 把可选步骤上的约束当作硬性关卡：mulan_war_campaign 第 5 步 holy_bird_blessing_quest 标记为 "optional": true 且 "requires": "M4_holy_bird_mulan"，未发现该编年事件的玩家在 stage 5 被永久卡住，6 步战役无法完成——数据作者声明的可选语义被运行时强制化为必选，跨模块契约不匹配。
  证据：QuestAuthorityCatalog.java:200/204、src/main/resources/data/seeking_immortals/text_material/quest_chains.json:836/837

- **low / smell**（第 88 行）：stageGate(chainId, 1) 无任何调用者，第 1 步的关卡规则永远不会被评估：start()/advance() 在 stage<=0 时走 startGate（链级门）直接返回，advance 只对 stage+1（>=2）调 stageGate；canTransitionExact 对 targetStage==1 走 meetsStartRequirements 而非 stageGate。当前数据恰好没有第 1 步约束（requires_branch 出现在 3-5 步、requires 在 5 步、branch_any 在第 2 步），但数据作者在后续为 steps[0] 添加约束时会静默失效，属于契约隐患。
  证据：TextQuestChainService.java:294/409、QuestAuthorityCatalog.java:86

### QuestRewardService.java

- **medium / smell**（第 131 行）：grantUniqueOnce unconditionally marks the one-time global claim (UNIQUE_LEDGER + authority 'unique:' key) even when the delivered stack is a generic JADE_SLIP_BLANK proxy because the canonical unique id has no registered item. 7 of 9 unique tokens (true_soul_pill/八灵尺→eight_spirit_ruler, spirit_refine_art, void_heaven_cauldron, huiyang_synth_mark, zaohua_trial_mark, detailed_node_token) resolve to no registered item — the registry only has eight_spirit_ruler_shard/_replica and the real palm_heaven_bottle is deliberately hidden behind ItemCatalogService.UNIQUE_FORBIDDEN, so it also falls to the proxy. Consequence: all distinct story rewards arrive as indistinguishable blank jade slips, and the at-most-once ledger permanently burns the claim, so registering the real item in a future version can never deliver it to players who already completed the chain.
  证据：QuestRewardService.java:127-131/205-230、ItemCatalogService.java:33-42、ModItems.java:319、ModBulkItems.java:50-59/147-148

- **low / smell**（第 302 行）：Data-contract mismatch: loadBuiltin parses only the 'unique' arrays, but the v101 rewards file also uses the 'unique_account' schema key (e.g. '大挪移令' at main_quest_rewards_v101.json:35 and hanli_timeline_items_v100.json:31), which is silently ignored, so that unique is never granted. Additionally, step-level uniques from the full v101 file are only merged into a chain when the index row's unique list is empty (311-314 行), so any future divergence where the index lists uniques but v101 adds more step-level ones would silently drop grants.
  证据：QuestRewardService.java:297-315、src/main/resources/data/seeking_immortals/text_material/main_quest_rewards_v101.json:35、src/main/resources/data/seeking_immortals/text_material/hanli_timeline_items_v100.json:31

- **low / smell**（第 332 行）：Silent resource failure: readJson swallows missing files and all parse exceptions and returns null without any logging (only the test's chainCount()>=10 would eventually catch an empty corpus). If main_quest_rewards_index.json or main_quest_rewards_v101.json is corrupt or missing at runtime, BUILTIN.chains is empty and onTextChainFinished silently grants nothing while players complete chains — with no server-side diagnostic and no way to re-trigger the one-shot finale reward.
  证据：QuestRewardService.java:324-335/256-259

- **low / smell**（第 199 行）：Dead soft-map row: mappedRewardRows can add 'foundation_path' (when the chain id contains 'foundation' or 'blood_forbidden', e.g. 'blood_forbidden_campaign'), but 'foundation_path' is never a key in BUILTIN.chains (only the 13 index ids exist), so the branch is always skipped — a silent no-op that masks missing reward-table wiring for the foundation/blood_forbidden campaign chains.
  证据：QuestRewardService.java:191-203/256-318

### QuestService.java

- **medium / bug**（第 434 行）：teleportToYue 的落点搜索起点为主世界 y=minBuildHeight+80(=16) 且只向下搜索（while 循环仅 target.below()）。世界出生点附近地表通常在 y≈60-70，高于搜索起点，因此循环实际会沿洞穴/岩层一路下探到第一个实心方块——通常是洞穴地板或 y≈-60 的基岩层，玩家被传送到基岩内部（窒息）、虚空或地下洞穴中，而不是地表；且 FLAG_YUE_PORTAL/yueArrived 在传送前已置位、checkProgress 紧随其后把阶段推进到 STAGE_COMPLETE，落点错误无法重试，可能导致死亡与物品丢失（hardcore 下永久损失）。
  证据：QuestService.java:433/434/437/397/398

- **low / security**（第 104 行）：NPC 身份仅按可伪造的显示名匹配：handleLegacyNamedVillagerInteraction 对任意自定义名为"墨老先生"的普通村民（任意玩家用原版命名牌即可伪造）触发 handleSevenMysteriesNpc，免费执行 LingGenTestStoneItem.testPlayer(consumeUse=false)——绕过 5 次使用的测灵根石获得灵根测试，低资质玩家还会经 grantMysticVialForLowTalent 免费获得绑定的神秘小瓶（每现实 24h 1 份灵液催熟），完全绕过物品经济与任务门控。专用 NPC 生成路径是 op(权限 2)命令，但该名字字符串门禁可被任何生存玩家伪造，存在客户端可伪造的权威性缺口。
  证据：QuestService.java:104/111/114、item/LingGenTestStoneItem.java:98、event/ModEvents.java:708

- **low / bug**（第 447 行）：hasItem/consumeItems 只扫描 player.getInventory().items（36 个主背包格），不包含副手/盔甲槽：grantMysticVial（第 447 行）在玩家已把任务小瓶放入副手时判断为未持有而再发放一个绑定小瓶——两个小瓶各自按现实时间独立充能，灵液产出翻倍（物品重复发放）；completeInfightingStage 的证据物品消费（第 344-345 行）同样漏检副手，玩家把证据放副手会卡死"取得长老勾结证据"步骤。LingGenTestStoneItem.java:101 的低资质小瓶发放存在同源缺陷。
  证据：QuestService.java:447/467/475/344、item/LingGenTestStoneItem.java:101

- **low / smell**（第 188 行）：chooseBranch 在分支被拒绝时仍返回 true：第 188 行的 return true 位于 ifPresent 之外，无论阶段不是 STAGE_INFIGHTING、缺少 FLAG_EVIDENCE、已选过分支，还是玩家 capability 缺失，命令 questChoose 都返回成功码 1，向玩家/脚本报告"已执行"而状态毫无变化（仅发送一条 choice_locked 提示），返回值语义与命令退出码不一致。
  证据：QuestService.java:154/162/188、command/SeekingImmortalsCommand.java:1046

### SevenMysteriesQuest.java

- **medium / bug**（第 77 行）：目标 '逃离七玄门'(FLAG_ESCAPE_READY) 在正常流程中永远不可达：QuestService 中 FLAG_ATTACK_TRIGGERED 与 FLAG_ESCAPE_READY 总是同时设置（进入 STAGE_LEAVE 时在 completeInfightingStage 一起添加；triggerAttack 命令也同时添加），全仓库不存在只设置 ATTACK_TRIGGERED 而不同时设置 ESCAPE_READY 的代码路径。因此 objective() 第 77 行的 '逃离七玄门' 分支永远不会显示，任务状态机中设计的'天罡盟攻打→逃离'两步被合并成一次自动完成，逃离玩法步骤实际缺失。
  证据：QuestService.java:351-352/263-264、SevenMysteriesQuest.java:76-77

- **low / bug**（第 79 行）：目标 '穿过越国传送门'(FLAG_YUE_PORTAL) 不可达：FLAG_YUE_PORTAL 只在 useYuePortal 内设置（同时已传送并 setYueArrived(true)，随后立刻 checkProgress），而 completeLeaveStage 在四个条件（ATTACK/ESCAPE/YUE_PORTAL/yueArrived）满足后同一 tick 内将 stage 置为 STAGE_COMPLETE。因此玩家看到 FLAG_YUE_PORTAL 已置位的那一刻任务已经完成，第 79 行分支永不被展示；第 78 行'找到越国传送门标记'成为 STAGE_LEAVE 唯一可见目标。
  证据：QuestService.java:397-401/357-371、SevenMysteriesQuest.java:78-79

- **low / smell**（第 56 行）：objective() 大量分支因 QuestService 的自动发物/自动推进而成为死代码：FLAG_HUANGLONG_MANUAL（第 56 行目标）与 FLAG_ALCHEMY_LEARNED（第 58 行）在进入 ENTRY 阶段的同一 checkProgress do-while 内无条件自动授予（无需玩家操作）；FLAG_VIAL_GRANTED（第 64 行）在发现密室后同一调用链内自动授予；FLAG_EVIDENCE（第 70 行）在 SECRET 阶段完成时经 giveEvidence 自动设置；第 59 行'等待执事安排下一步'、第 66 行'回报执事'、第 70 行、第 76 行同理在稳态下不可达。即 12 个目标分支中 9 个在正常流程中永不会显示，显示层与状态机契约不一致，且自动授予炼丹术/功法与目标文案描述的手动步骤相矛盾。
  证据：QuestService.java:272-283/300-314/329-339/336、SevenMysteriesQuest.java:56-79

- **low / smell**（第 16 行）：FLAG_ADMITTED 是死标志：全仓库唯一写入点是 QuestService.completeRootStage（设置后无人读取），没有 NPC 对话、门派入口判定或任何服务消费该标志；resources 中亦无 'admitted' 相关接线。若意图是作为入门凭据（类似 sect 的已入门校验），则存在未接线的安全/业务缺口。
  证据：QuestService.java:291、src/main/resources/assets/seeking_immortals/lang/en_us.json:842

- **low / smell**（第 43 行）：objective()/stageName() 返回硬编码中文字符串并被作为参数传入 translatable 状态消息（QuestService.show 的 command.seeking_immortals.quest.status），而 mod 其余玩家可见文案均走 zh_cn/en_us 本地化键。en_us 玩家会直接看到中文目标，且该文件文案无法通过资源包覆盖；lang 文件中不存在对应键，构成 i18n 契约不一致。
  证据：QuestService.java:41-48、src/main/resources/assets/seeking_immortals/lang/en_us.json:533-537

### TextQuestDialogueService.java

- **medium / bug**（第 43 行）：对话树阶段映射错误：TextQuestDialogueTreeService.nodeFor 对仍在进行中的任务，当 stage >= 4 时直接钳制到最后一个节点（complete 节点），该节点文案为"此线已完结"且仅提供 talk 选项（无 advance/分支选项）。所有 stepCount >= 5 的任务链（如 huangfeng_cultivation_path 5 步、chaotic_sea_politics 6 步）在阶段 4 至 stepCount-1 期间，对话界面会显示虚假的"已完成"状态，且无法再通过对话推进阶段（只能改用任务追踪器或命令），任务链大部分后期流程的对话驱动功能失效。complete 标志仅在 complete=true 分支中被处理，钳制路径完全忽略它。（与 TextQuestDialogueTreeService 的 high 发现同根因，独立代理交叉确认。）
  证据：TextQuestDialogueTreeService.java:45/109、src/main/resources/data/seeking_immortals/text_material/quest_chains.json:12、TextQuestDialogueService.java:298

- **low / security**（第 97 行）：NPC 权威门禁不对称：act() 中 Wave55 世界 NPC 门禁（requireNearbyNpcOrWarn）覆盖 advance/branch_righteous/demonic/neutral，但遗漏了 "start" 分支——持有含 start 选项对话会话的玩家可在无绑定 NPC 的情况下仅凭会话锚点（8 格）内接取任务，而同方法内其它推进/分支操作都强制要求附近存在绑定 NPC。虽然当前阶段 0 会话只能由服务端路径（op 命令或自动 start 的 openDialogue）打开导致实际利用面很小，但该不对称性是潜在的无 NPC 远程接取向量，且与第 96 行注释声明的"advancing/branching"门禁范围不一致，未来任何新增的阶段 0 会话打开路径都会直接继承此漏洞。
  证据：TextQuestDialogueService.java:97/103/96、QuestTrackerActionPacket.java:40

### TextQuestNpcHookService.java

- **medium / bug**（第 92 行）：跨模块接线缺失：本类与 TextQuestChainService.npcFor() 使用的六个任务 NPC id（npc_mo_lao / npc_text_quest_guide / npc_mulan_envoy / npc_yinluo_steward / npc_star_palace_broker / npc_kunwu_steward）在 NamedNpcRegistry 的全部数据源（named_npcs_v116.json、named_npc_seeds_v137.json、npc_dialogue_templates_v138.json、npc_dialogues_v117.json）中均不存在（resources 中唯一命中是音效名 dialogue_npc_mo_lao 的子串）。后果：(a) 世界 NPC 放置只由 NamedNpcRegistry 驱动（NpcSettlementService.ensureRegionalRoster / NpcSpawnService.ensureRegionNpcs），因此这些专属任务 NPC 在正常游戏流程中永远不会被生成，handleNamedNpcInteraction / isNearBoundNpc 的 id 路径实际死代码，只能靠 OP 命令 textQuestSpawnNpc 手动生成；(b) 未注册 id 导致 NpcSpawnService.spawnQuestNpc 走 npc.isEmpty() 分支，applyNamedNpc 不执行、dialogueTreeId 为空，QuestNpcEntity.openFor 的 NpcDialogueApi 路径无法使用作者化对话树；(c) 对话界面发言人回退为『引路人』（TextQuestDialogueService.npcDisplay 经 NamedNpcRegistry.find 查空）。现有测试 TextQuestNpcHookServiceTest 只断言字符串绑定，未覆盖注册接线。
  证据：TextQuestNpcHookService.java:24/92/148、npc/NamedNpcRegistry.java:139/177、npc/NpcSettlementService.java:73/112、npc/NpcSpawnService.java:99、entity/QuestNpcEntity.java:64、TextQuestDialogueService.java:321、command/SeekingImmortalsCommand.java:991、src/test/java/com/xunxian/seekingimmortals/quest/TextQuestNpcHookServiceTest.java:11

- **low / security**（第 159 行）：NPC 存在性门禁可被命名牌伪造绕过：isNearBoundNpc 与 handleLegacyNamedVillagerInteraction 接受任意 vanilla 村民，只要其自定义名（生存模式玩家可用命名牌任意设置，支持中文名）等于链对应 NPC 的显示别名/id（DISPLAY_ALIASES 覆盖全部 6 个身份）。QuestTrackerActionPacket 的 advance:/branch: 与 TextQuestDialogueService.act 的 advance/branch/righteous/demonic/neutral 选择仅由该门禁把关，玩家在自家基地放一个改名村民即可随时满足『必须在专属任务 NPC 附近』的权威前置，无需真实任务 NPC。下游权威门禁（境界/区域/阵营/阶段费用/分支锁定）仍在 TextQuestChainService 内服务器端强制，故不构成越权，但 NPC 存在性门禁作为独立防线实际失效；这是有意的 legacy 兼容路径，但门禁未区分 legacy 村民与专属 NPC。
  证据：TextQuestNpcHookService.java:159/103/24、network/QuestTrackerActionPacket.java:45/54、TextQuestDialogueService.java:97

- **low / bug**（第 124 行）：openDialogue 的 autoStart 失败被吞掉：autoStart=true 且玩家尚未开始该链时，TextQuestChainService.start() 会因境界/区域/阵营等前置不满足而返回 false 并给玩家发警告，但此处忽略返回值，继续执行 talk()、发送 OpenDialogueScreenPacket 并广播 npc_interact 成功消息且返回 true。调用方把交互视为完全成功：QuestService.handleNamedNpcInteraction 返回 true 会取消原版交互事件，SeekingImmortalsCommand.textQuestInteract 上报成功(1)。玩家收到『开始失败』警告与『交互成功』消息并看到未开始状态对话（含 start 选择），状态传播与事实不符。
  证据：TextQuestNpcHookService.java:122/128、TextQuestChainService.java:147、QuestService.java:97、command/SeekingImmortalsCommand.java:1007

### TimelineChronicleService.java

- **medium / bug**（第 133 行）：onChronicleDiscovered 的关键词映射对主线章节 id 覆盖不全：完成 chapter_1_sect（炼气/黄枫谷章节）和 chapter_4_great_jin（大晋章节）永远不会解锁任何时间线阶段。chapter_1_sect 不包含炼气分支的任一关键词（huangfeng/qi_refin/blood，第135-136行）；chapter_4_great_jin 只含 "jin" 而不含元婴分支要求的关键词 "dajin"（第141行）。结果：M16 编年 UI 中 "炼气·黄枫谷" 与 "元婴·天南回归与大晋" 阶段即使完成对应主线也永久保持锁定。现有测试（M11QuestCorpusTest.timelineAndChronicleIndexesLoad）只断言数量，无法捕获该映射缺口。
  证据：TimelineChronicleService.java:133/135/141、MainStorySoftService.java:110、src/main/resources/data/seeking_immortals/catalog/main_story_quest_map_index.json:26/89

- **low / smell**（第 84 行）：unlockPhase 的模糊匹配与 hasPhase 的精确匹配不对称：unlockPhase 接受阶段显示名的任意子串（第84行 contains 匹配），但解锁状态以完整显示名存入 NBT，且匹配命中按索引顺序取第一个包含该子串的阶段（例如 id="天南" 会命中 "筑基·天南乱" 而非同样包含 "天南" 的 "元婴·天南回归与大晋"）；而 hasPhase 只按完整名称读取（第110行）。当前调用者恰好传入完整阶段名（凡人/炼气/筑基/结丹/元婴/化神），问题被掩盖，但任何未来调用者传入简称/部分名都会产生错误匹配或永久查不到已解锁状态。
  证据：TimelineChronicleService.java:84/93/110

- **low / smell**（第 93 行）：持久化键直接使用本地化显示名（如 "凡人·七玄门"）而非稳定 ID：一旦 data 中的阶段名被重命名（内容/数据包更新），玩家已解锁标记成为孤儿键——hasPhase 返回 false、UI 显示锁定、解锁消息重复弹出，而 unlockedPhaseCount（第113-125行）仍统计这些孤儿 true 键，导致 LoreCompendiumService 进度显示与实际 UI 不一致。跨版本存档存在解锁丢失风险。
  证据：TimelineChronicleService.java:93/106、lore/LoreCompendiumService.java:139、client/ChronicleScreen.java:120

- **low / smell**（第 127 行）：编年→时间线桥接（onChronicleDiscovered）的实际接线与文档及关键词表不符：Javadoc（第127行）声明"发现编年事件可解锁对应时间线阶段"，关键词表（m1/qixuan/tianyuan/star/void/nascent/dajin/blood/spirit 等）明显针对编年事件 ID（chronicle_events_index.json 中确实存在 A4_void_palace_built、K6_dajin_clans、K10_qixuan_decline 等），但全仓库唯一调用方是主线章节完成（MainStorySoftService.java:110），ChronicleTradeSoftService.discoverChronicle（第160行）从不触发它；且 m1/startsWith("m1")/tianyuan/star/void/nascent 等关键词对章节 ID（chapter_0_mortal…chapter_6_spirit_realm）永不可达。声明的功能未接线，属死代码与误导性契约。
  证据：TimelineChronicleService.java:127/133、catalog/ChronicleTradeSoftService.java:160、MainStorySoftService.java:110、src/main/resources/data/seeking_immortals/catalog/chronicle_events_index.json:1

### FtbNativeQuestSync.java

- **low / bug**（第 165 行）：运行时/解析时阶段上限与 FTB 数据包留存策略不匹配：isSatisfied（line 165）和 parseTarget（line 241）都把目标阶段绑定到目录 stepCount，而 FtbQuestDefaults 对自定义过的 FTB 包采取 PRESERVED_CUSTOMIZED（保留旧 SNBT 标签不升级）。一旦后续目录更新缩减某链 stepCount 或移除链，旧标签（如 si_native_<chain>_6）将无法解析为 Target，FtbCustomTaskHooks.parseTag 得到 Spec.Unknown，evaluate 恒 false——镜像/就绪自定义任务永远无法完成，FTB 任务永久卡死；写标签则静默 fail-closed。同步层没有任何恢复路径（无版本协商、无标签迁移），玩家只能手动删除/修复 config/ftbquests 文件。
  证据：FtbNativeQuestSync.java:165/241、FtbQuestDefaults.java:236-250/112-114

- **low / smell**（第 115 行）：跨模块契约不匹配（当前数据恰好对齐，属于脆弱契约而非现役缺陷）：validateWriteIntent（line 115）把链标签白名单限定为 FtbQuestBridgeService.registeredChainIds()（仅章节映射过的链，line 145 = chainToChapter.keySet()），而 write 标签解析 parseTarget（line 239-244）却用完整目录 TextQuestChainService.find() 校验。目录新增链但未同步桥接映射时，写回会以误导性的 MISSING_CHAIN_TAG 状态静默失效（而非 MALFORMED_WRITE_TAG）；目前 62 条链全部映射、由测试强制约束，但该契约无编译期保障。
  证据：FtbNativeQuestSync.java:115/239-244、FtbQuestBridgeService.java:145-147

- **low / smell**（第 67 行）：公开生产 API writeTargets()（line 67-84）在主代码中没有任何调用者，仅单元测试 FtbNativeQuestSyncTest 使用；其失败闭合逻辑与 validateWriteIntent 完全重复（任一畸形 write 标签即整体返回空列表），属于死代码/重复逻辑，容易与 validateWriteIntent 的未来改动产生语义漂移。
  证据：FtbNativeQuestSync.java:67-84、src/test/java/com/xunxian/seekingimmortals/quest/FtbNativeQuestSyncTest.java:47-62

### FtbQuestBridgeService.java

- **low / smell**（第 47 行）：公共 API isFtbPresent() 与 sampleMappings() 在全仓库（src/main、src/test、scripts）均无调用者，属死代码；isFtbPresent() 的 try/catch(Throwable) 包裹的只是本 mod 自身常量读取（ModCompat.FTB_QUESTS_LOADED 不引用任何 FTB 类，FTB 缺失时不会抛错），该守卫会误导后续维护者以为它能防 FTB 缺失导致的类加载崩溃——而本类唯一的设计安全属性正是「类加载不依赖 FTB」。sampleMappings() 内部还有 Math.max(1, limit) 使 limit<=0 时仍返回 1 条样本的契约怪癖。
  证据：FtbQuestBridgeService.java:47-53/63-74、compat/ModCompat.java:9

- **low / smell**（第 88 行）：第 88 行注释引用了不存在的字段 FtbQuestDefaults.SEED_FILES（该类只定义 RESOURCE_ROOT/TARGET_ROOT/MANAGEMENT_ROOT/STATE_FILE，实际清单是 FtbDefaultPackManifest.FILES）；章节目录在桥接层（本文件 89-106 行）、安装器清单（FtbDefaultPackManifest.FILES 10 个文件）、handbook overlay（quest_handbook_i18n_v1.json ftb_chapters，且按索引顺序断言）三处手工维护，仅由单元测试兜底（FtbQuestBridgeServiceTest/QuestHandbookCoverageTest），不在构建 preflight 门控内。新增/删除章节时安装器种子文件与桥接映射会静默偏离，测试不运行则无任何检测。
  证据：FtbQuestBridgeService.java:88、FtbQuestDefaults.java:31-34、FtbDefaultPackManifest.java:10-83、src/test/java/com/xunxian/seekingimmortals/resources/QuestHandbookCoverageTest.java:70-88

- **low / smell**（第 152 行）：putAll() 使用 putIfAbsent，同一 chain 被作者误分配到多个章节时静默保留首个映射，不产生任何编译/运行告警。当前 62 条链经核验为严格单射（与 quest_chains_index.json 的 62 个 id 完全双向一致、无重复），但该吞错行为使维护期新增章节时的手误只被测试捕获（且 keySet 相等断言在「同一章节内重复」场景仍可能误通过），存在具体维护风险。
  证据：FtbQuestBridgeService.java:150-154、src/test/java/com/xunxian/seekingimmortals/quest/FtbQuestBridgeServiceTest.java:27-29

- **low / smell**（第 3 行）：4 个未使用的导入（JsonArray/JsonElement/JsonObject/JsonParser，第 3-6 行）：本文件已不含任何 JSON 解析逻辑，属重构残留，仅产生编译噪音，且容易误导维护者以为存在 JSON 数据接线。
  证据：FtbQuestBridgeService.java:3-6

### QuestHookRuntime.java

- **low / bug**（第 233 行）：onPlayerDailyEvent writes the per-roll idempotency claim (keyed regionId|eventId|untilTick) unconditionally after processing hooks, even when TextQuestChainService.start/advance returned false (unmet start gates such as faction/region/parent, or missing stage costs). The player is then blocked from retrying for the entire roll window even after satisfying the gate. Additionally `handled` is force-set to true at the end of each non-blank hook iteration (line 230) after already being OR-ed with actual results, making the return value meaningless; the only callers (DailyEventScheduler.ensurePlayerEvent / rollAllRegions) ignore it anyway.
  证据：QuestHookRuntime.java:211/230/233/236、region/DailyEventScheduler.java:116/186

- **low / security**（第 297 行）：Dialogue-driven advance path is not proof-gated: onDialogueNode calls tryStartOrAdvanceChain(nodeId) and handleDialogueEffect's default/turnin branches call tryAdvanceActive(questId) with a blank hookId, which bypasses the matchesCurrentStepHook gate (583-586 行) and advances any active non-complete text chain on every qualifying node visit. The producer (NpcDialogueApi.presentNode) only latches the tree:node claim when the node has effects (NpcDialogueApi.java:272-286), and fires DialogueNodeReachedEvent before that latch; effect-less nodes are never claimed, so a node id equal to an active chain id would advance the chain on every visit (bounded only by per-stage item costs). Currently no authored node id collides with a chain id (verified against npc_dialogue_branches_v139.json), and NpcDialogueApi.onDialogueNodeReached (NpcDialogueApi.java:75-86) is an unused public API that can fire the event with an arbitrary nodeId, so this is latent, but the event handler trusts event payloads with no producer/provenance validation.
  证据：QuestHookRuntime.java:295/297/583/589/601、npc/NpcDialogueApi.java:272/279/75

- **low / bug**（第 318 行）：onLivingDrops credits kill/slay hooks and recordEntityKilled only when event.getSource().getEntity() is directly a ServerPlayer. Kills attributed through the established combat-owner resolution (resolveCombatAuthorityPlayer / CultivationBeastEntity.recentCompanionDamageOwner, used by ModEvents for the same event) never advance kill quests when a tamed beast/companion lands the final hit, and the two kill handlers (ModEvents vs QuestHookRuntime) disagree on attribution for the same LivingDropsEvent.
  证据：QuestHookRuntime.java:318/328、event/ModEvents.java:527

- **low / smell**（第 167 行）：onDailyEvent(String, String) is registered into DailyEventScheduler's hook list but is a no-op: the comment claims it should 'mark soft region flag only', yet the body only normalizes and returns without writing any flag or state. The region-level subscription therefore carries no behavior, and register() swallows any Throwable from the event-bus registration (81-90 行), which would silently disable all quest hooks (login/dialogue/kill/craft/pickup) without any log if registration ever failed in production.
  证据：QuestHookRuntime.java:165/169/173/81、region/DailyEventScheduler.java:59

### QuestHookSoftService.java

- **low / bug**（第 90 行）：关键词启发式分支存在顺序遮蔽：第 67 行 ghost|yin|nether、第 70 行 star|chaotic|void|inverse、第 76 行 spirit|tianyuan|ascension|diyuan 先于第 90 行 blood|nether|diyuan|void 命中，使第 90 行的 void/nether/diyuan 分支永不可达（死代码），第 82 行 alchemy 也被第 61 行遮蔽。后果：未进显式映射表的目录钩子被确定性映射到错误领域——如 quest_hooks_index.json 中的 hook_void_palace_key_hunt、void_palace_key_fragment_turnin、void_palace_intel_sell、void_key_fragment_rumor、hook_void_palace_rumor 全部解析为 star_palace_internal_politics/chaotic_sea_politics，而显式映射表对同域钩子（void_key_fragment_hunt→void_palace_campaign 等，第 320-324 行）表明作者意图是 void_palace_campaign 域；diyuan/nether 钩子同理落入 spirit_realm_rise/ghost_path 家族而非 diyuan_campaign/nether_river_campaign。影响：OP 命令 preview/accept 展示或启动错误任务链；未来日报事件 JSON 若加入此类 quest_hook 字段，QuestHookRuntime.onPlayerDailyEvent 第 219 行也会对真实玩家启动错误任务链（当前日报数据的 quest_hook 值均命中显式映射表，故现网玩家路径未受影响）。
  证据：QuestHookSoftService.java:61/67/70/76/82/90、src/main/resources/data/seeking_immortals/catalog/quest_hooks_index.json:683/735、QuestHookRuntime.java:219

- **low / smell**（第 56 行）：mappedChainId 第 54-59 行的子串匹配（id.contains(chainId) || chainId.contains(id)）先于关键词启发式执行，结果依赖 quest_chains_index.json 的条目插入顺序：例如未知 id 含 'sect' 时循环先命中 yin_luo_ghost_sect（JSON 第 26 条），使第 73-75 行作者显式给出的 sect→dajin_kunwu_line 分支对裸 'sect' 输入不可达；含 'void' 的 id 先命中 void_great_cultivation_arc（JSON 第 40 条）而非第 70 行关键词组。accept() 对未知 id（第 124-138 行）直接信任该结果，OP 输入短字符串可启动与意图无关的任务链，且结果随数据文件排序漂移；与下方关键词分支构成双份易漂移的映射契约。
  证据：QuestHookSoftService.java:54/56/73/126、src/main/resources/data/seeking_immortals/catalog/quest_chains_index.json:26

- **low / bug**（第 149 行）：accept() 已开始分支（第 145-155 行）无条件调用 TextQuestChainService.advance() 且丢弃其布尔返回值：advance 在阶段费用不足（TextQuestChainService.java:415 payStageCost 返回 false）或阶段门未过（:411）时失败，本方法仍返回 true 并发送「accepted」成功提示并打开对话。同时该路径未应用运行时路径 QuestHookRuntime.onPlayerDailyEvent（:224-227）的 matchesCurrentStepHook 步骤钩子校验，重复 accept 可在无步骤证据的情况下推进阶段（仅受费用/关卡门控），并可重复扣除玩家物品（如 spirit_stone_shard 阶段费用），与运行路径的钩子匹配契约不一致。影响限于权限 2 的 OP 调试命令（SeekingImmortalsCommand.java:162），故定级 low。
  证据：QuestHookSoftService.java:146/149/151、TextQuestChainService.java:411/415、QuestHookRuntime.java:224、command/SeekingImmortalsCommand.java:162

- **low / bug**（第 107 行）：preview() 第 106-107 行向 translatable 传入 Component.empty() 作为第一个占位符参数，而 lang 条目是双占位符「任务钩子 %s：%s」，实际渲染为「任务钩子 ：<钩子名>」，第一个占位符恒为空串。从 hookDisplay(entry) 占据第二个占位符可推断第一参数本意是 hook id（entry.id()），属传参遗漏。玩家可无权限执行 preview（SeekingImmortalsCommand.java:165-166 无 hasPermission 门控），该文案错误对所有玩家可见。
  证据：QuestHookSoftService.java:106/107、src/main/resources/assets/seeking_immortals/lang/zh_cn.json:1821、src/main/resources/assets/seeking_immortals/lang/en_us.json:1811、command/SeekingImmortalsCommand.java:165

### QuestLineService.java

- **low / bug**（第 55 行）：归一化不对称（潜在正确性缺陷）：find() 用 normalize(lineId) 查 lines 映射，但 lines 在 loadBuiltin 中以原始 id 作为键插入（lines.put(id, line)），未归一化。同样，跨引用校验 lines.containsKey(lid)（第 137、146 行）用归一化后的 ref 对比原始键。当前语料全部为小写 [a-z0-9_]，无实际失败；但一旦任何 JSON 中出现大写/带空格的 id 或 leads_to，find() 将静默返回 empty，crossRefsResolvable() 会把本可解析的引用误报为 unresolved（大小写不一致只在 knownChains 一侧被容忍）。
  证据：QuestLineService.java:55/123/137/146

- **low / smell**（第 129 行）：byChapter 键不一致：空白章节在 computeIfAbsent 中以原始字符串作为键（ch.isBlank() ? chapter : ch），而 linesForChapter()（第 59 行）查询时总是先 normalize；因此空白/纯空格章节条目永远不可被查询到。同时第 126 行的 unresolved 检查跳过空白章节（!ch.isBlank()），该类错误会被静默接受。
  证据：QuestLineService.java:129/59/126

- **low / smell**（第 194 行）：readJson 吞掉所有解析异常并返回 null，无任何日志；若主索引与 fallback 同时缺失或损坏，快照静默退化为空：lineCount()==0 且 crossRefsResolvable() 因 unresolved 列表为空而返回 true（空真）。维护者在运行时收不到任何信号，只能靠 M11QuestCorpusTest 发现。
  证据：QuestLineService.java:194/91、src/test/java/com/xunxian/seekingimmortals/quest/M11QuestCorpusTest.java:37

- **low / smell**（第 109 行）：step_count 兜底只处理数组形式的 "steps" 字段（第 109-111 行），与 ExtendedCatalogService.enrichQuestStartRequirements 同时兼容数组与数值形式（ExtendedCatalogService.java:276-284）不一致；若语料某行仅提供数值 steps 或数字字符串，stepCount 将保持 0 而不报错。当前主索引全部带 step_count、fallback 全部带数组 steps，故为潜在风险。
  证据：QuestLineService.java:109、catalog/ExtendedCatalogService.java:276

### QuestPresentationService.java

- **low / perf**（第 102 行）：finaleRewards()/nextStageCost() are invoked from QuestTrackerScreen.detailLines(), which is executed on every render frame (renderJournalContent). Each call re-runs catalog reward parsing and ForgeRegistries.ITEMS.getValue per reward token (TextQuestChainService.finaleRewardPreview) and re-computes stageCostFor, although all inputs are static data loaded once into BUILTIN. While the quest tracker is open this performs registry/map lookups on the render thread every frame with no caching.
  证据：QuestPresentationService.java:102/111、client/QuestTrackerScreen.java:267/378、TextQuestChainService.java:971

- **low / smell**（第 264 行）：Missing resource wiring: the shipped handbook (quest_handbook_i18n_v1.json) contains 35 authored English stage titles (line_titles_en) and 35 English stage summaries (line_summaries_en), but no Java code reads them (grep across src/main/java finds zero consumers; QuestPresentationService is the only handbook reader and uses only chain_titles_en, hook_labels_zh, numeric_stage_labels). As a result the English tracker UI always shows generic 'Stage N' for the 58 array-based chains and auto-generated descriptions (chainDescriptionEn), despite authored English content shipping in the mod — a display-quality gap that also invites data drift between the two corpora.
  证据：QuestPresentationService.java:264/533、src/main/resources/data/seeking_immortals/text_material/quest_handbook_i18n_v1.json:1

### QuestProgress.java

- **low / bug**（第 132 行）：reputation 字段是唯一一个在 loadNBT 中不带任何边界钳制读取、且 addReputation 直接做无饱和 int 加法（`reputation += amount`）的字段。其余字段（stage、sectQuestStage、contribution）加载时都做了 Math.max/min 防护，唯独 reputation 原样接受 NBT 中的任意 int。损坏或篡改的存档写入极端值（如 Integer.MAX_VALUE）后，QuestService.chooseBranch 的 addReputation(+50)（QuestService.java:174）会整数溢出回绕为负值。当前 getReputation 仅用于状态展示（QuestService.java:47），影响限于显示错误，但属于未处理的损坏数据边界，与其他字段的处理不一致。
  证据：QuestProgress.java:132/236、QuestService.java:174

- **low / bug**（第 249 行）：loadNBT 使用 tag.getList("Flags", 8) / getList("SectFlags", 8) 读取旗标列表：若存档中该键类型不匹配（损坏/跨版本数据），CompoundTag.getList 静默返回空列表，全部任务旗标被无提示清空，而 stage 单独持久化仍保持推进状态。后果：stage 与旗标不变量断裂后，QuestService.completeEntryStage 会再次发放一次性黄龙功卷轴（QuestService.java:300-304）、completeSecretStage 会再次发放神秘小瓶（QuestService.java:329-333）、completeLeaveStage 可因 FLAG_FINAL_REWARD 丢失重复发放 300 灵石最终奖励（QuestService.java:362-365），即物品复制；部分旗标（如 FLAG_YUE_PORTAL）若无标记结构则无法恢复。建议对类型不符或缺失的列表做显式处理或日志。
  证据：QuestProgress.java:249/257、QuestService.java:300/329/362

- **low / smell**（第 91 行）：setSectQuestStage 只做下界钳制（Math.max(0, ...)），上界 STAGE_PHASE10_COMPLETE=5 仅在 loadNBT 的 qinglan_sect 迁移分支（QuestProgress.java:231-234）和运行时 SectContributionService.normalizeSectState（SectContributionService.java:914-927）中强制。部分只读消费者直接取未规范化的原始值：ShopService.snapshot 的 rank 门控展示（ShopService.java:318-326）、ManualCatalogService.meetsFactionRelation（ManualCatalogService.java:1043-1053）、TechniqueGateService.checkMethod（TechniqueGateService.java:284-286），越界值会被当作最高宗门阶位处理。实际可达性仅限存档篡改（宗门大厅购买路径会先 normalizeSectState 钳制），维护风险大于运行时风险；建议在 setter 内统一钳制上下界，使加载/迁移/运行时三处规则一致。
  证据：QuestProgress.java:91、sect/SectContributionService.java:914、shop/ShopService.java:318、catalog/ManualCatalogService.java:1043

### TextQuestChainService.java

- **low / bug**（第 485 行）：潜在的双次推进（repeat-trigger）：QuestHookRuntime.onDialogueNode 对同一 node id 依次调用 tryAdvanceByHook(nodeId) 与 tryStartOrAdvanceChain(nodeId)（另有 treeId_nodeId / treeId:nodeId 两个变体）。发货数据 quest_chains.json 中已存在 hook id 与 chain id 碰撞：spirit_eighteen_pilgrimage 第 2 步 hook 等于其自身 chain id；tianlan_defense_line 第 3 步 hook 等于另一条链 mulan_war_campaign 的 id。一旦任何对话节点/tree 键命中这些 id，(a) 通过 matchesCurrentStepHook 推进一次，(b) 在同一事件内不做任何 hook 校验再次 advance —— 第二次推进跳过该阶段已编写的步骤 hook 门禁（matchesCurrentStepHook 只在 hook 路径上校验，packet/FTB/chain-id 路径均不校验），并额外发放 +1 声望（line 441）且再次扣费。start()/advance() 均无同事件去重或幂等守卫。当前对话数据中尚无节点 id 碰撞（npc_dialogue_branches_v139.json 中的 'spirit_realm_border' 仅为 region 字符串，非节点 id），故为潜伏路径，但碰撞已在数据中实存。
  证据：TextQuestChainService.java:485/494/388、QuestHookRuntime.java:296/297/300/538/589、src/main/resources/data/seeking_immortals/text_material/quest_chains.json

- **low / bug**（第 404 行）：状态机对 stepCount 边界无防护：(1) 若某链 stepCount<=0（数据回归），advance() 永远进不了完成分支（line 404 的 `chain.stepCount() > 0` 恒假），stageCostFor 对 stepCount<=0 直接返回空（line 502），于是每次 advance 免费、stage 无上限增长且每次 +1 声望（line 441）——无限免费推进/刷声望，链永不完成；(2) 若存档 stage 超过当前 stepCount（版本更新下调 index step_count），advance() 被 'complete' 消息永久阻塞（line 404-407），而终局奖励只在推进进入最终阶段的瞬间发放（line 422-433），该链将永远卡死、REW=0，同时 listProgress（line 125）显示 DONE，客户端无任何补救路径。当前 62 条链数据（step_count 2-6，corpus 与 index 一致）未触发，属存档加载/数据迁移健壮性缺口。
  证据：TextQuestChainService.java:404/420/422/441/502/125

- **low / smell**（第 911 行）：读路径上的持久化写入副作用：hasAuthorityReward（line 911-916）在检查旧版 REWARD_TAG / ftb tag 时会执行 markAuthorityReward 迁移，把数据写入 player.getPersistentData()。该方法是纯查询/展示语义，却经 formatTrackerLine（line 1177）→ buildTrackerLines → syncTracker（line 1126-1131）在登录同步（event/ModEvents.java:838）和每次追踪器操作（network/QuestTrackerActionPacket.java:61）时被调用。迁移幂等，但把 NBT 写操作耦合进了只读/UI 路径，且对每条链、每次同步都会重复执行 hasAuthorityReward 的 CompoundTag 拷贝与条件写入。
  证据：TextQuestChainService.java:911/1177/1126、event/ModEvents.java:838、network/QuestTrackerActionPacket.java:61

## 剩余未确认项

- 3 个代理首次返回空结果（QuestPresentationService、TextQuestChainService、TextQuestDialogueService），已重试并补齐，26/26 结果有效。
- TextQuestDialogueTreeService 的 nodeFor 钳位问题（high）与 TextQuestDialogueService 报告的 medium 发现同根因，由两个独立代理交叉印证。
- 严重度/行号以各代理 JSON 为准；部分证据引用 JSON 数据文件（如 detailed_quest_proof_routes.json、quest_chains_playable_v141.json、merchant_shops_runtime.json），其中无逐行号的数据文件无法进一步在报告中定位，已按代理断言保留。
- FtbCustomTaskHooks/FtbQuestCompatBootstrap 的镜像分支团队奖励风险（medium security）依赖「第三方 FTB 包在镜像任务上配置奖励」这一未确认外部条件；当前捆绑 SNBT 由测试钉为无奖励，属环境依赖项。
- DetailedQuestProofEvent/DetailedQuestProofService 报告的 11 条不可产出路由、DetailedQuestRuntimeService 的 5 个未注册奖励物品，被代理确认为已发布数据中的现行问题（非仅潜在），但其是否被上游 Q-B 分期计划刻意接受，需由项目维护者裁决。
- MainStorySoftService 的 secretRealmClear 死门建议以 SecretRealmProgressSavedData 为准重接，代理已给出修复方向，未在此次审计中实施（本报告不修改源码）。

> 工作流输出路径：issues/src-main-java-com-xunxian-seekingimmortals-quest.md
