# QA-01 / QA-02 实机签字清单 · 0.2.266

> 文件名刻意避开 `manual_live_smoke_checklist.md` 与 `manual_multiplayer_signoff_checklist.md`
> —— 那两个文件会被 `/seeking_immortals live_smoke sign` 与 `live_smoke mp sign` **整体覆盖**
> （`LiveSmokeChecklistService:396` 与 `:416`）。本清单是手写件，不要改名到那两个名字上。

本清单由 AI 编写，**签字必须由实际执行者填写**。计划要求「不复用旧版本签字」，`project_docs/manual_multiplayer_signoff_checklist.md`
仍是 0.2.104 / 协议 26 时期的产物，已作废，不要拿它当本次依据。

## 0. 执行者填写栏

| 项 | 值 |
| --- | --- |
| 执行时间（开始 / 结束） |  |
| mod_version | 期望 `0.2.266`，实测填写： |
| ModNetwork.PROTOCOL_VERSION | 期望 `31`，实测填写： |
| JAR 文件名 | 期望 `seeking_immortals-0.2.266.jar`，实测填写： |
| JAR SHA-256 | 期望 `46be731b1f73c235a3502e1b9068b19e887f229a066854ae22ebe8948398a638`，实测填写： |
| 客户端日志位置 | |
| 服务端日志位置（QA-02） | |
| 失败与重试记录 | |
| 结论（通过 / 不通过） | |

校验 JAR：`sha256sum build/libs/seeking_immortals-0.2.266.jar`。若哈希不符，说明跑的不是本次构建物，**停止签字**。

## 1. 通用规则

- **QA-01 全程不得使用管理员命令**。凡标注 `[permission 2]` 的命令一律不准在 QA-01 使用；只读命令（`/seeking_immortals live_smoke`、`realm`、`root`、`quest check`）没有权限门，可作辅助观察。
- 日志审计：每步记录 `latest.log` 中的关键字与行号。出现 `Exception`、`Failed to`、`fail-closed` 以外的报错一律记为失败。
- 失败即记录，不要就地打管理员命令绕过；绕过会让该步作废。
- `live_smoke` 各路由会往 `project_docs/` 写文件，**`run` 也写**（不是只打印）。想保留旧报告就先备份：
  - `live_smoke run` → `live_smoke_report_latest.md`
  - `live_smoke mp run` → `live_smoke_mp_report_latest.md`
  - `live_smoke sign` → 上面那个 + `live_smoke_report_signed.md` + **覆盖** `manual_live_smoke_checklist.md`
  - `live_smoke mp sign` → mp 两个 + **覆盖** `manual_multiplayer_signoff_checklist.md`
  - 以上都**不会**动本文件。

## 2. QA-01 单客户端全流程

`./gradlew runClient`，新建存档，**不开创造、不给权限**。逐行填 结果 / 日志行号。

### 2.1 开局与同步

| # | 操作 | 预期 | 日志关键字 | 结果 |
| --- | --- | --- | --- | --- |
| 1.1 | 进入世界，打开背包点「修仙」按钮 | 修仙面板显示 炼气 / 灵力 / 灵根 / 战斗属性 五段 | `SyncCultivationDataPacket` | |
| 1.2 | `/seeking_immortals realm`、`root` | 境界、寿元、灵根、突破率均有值，无 `unknown` | | |
| 1.3 | `/seeking_immortals live_smoke run` | 打印各 check 项；记录此刻 `human_signoff` 与 `mp_human_signoff` 为未签 | `live_smoke` | |
| 1.4 | 退出到标题再进世界 | 面板数值一致，无重置 | 登录同步包 | |

### 2.2 突破

| # | 操作 | 预期 | 日志关键字 | 结果 |
| --- | --- | --- | --- | --- |
| 2.1 | 打坐蒲团上右键修炼至可突破 | 灵力/修为增长，移动即中断 | | |
| 2.2 | `/seeking_immortals breakthrough` | 成功或失败都要有明确提示；失败不得清空修为 | | |
| 2.3 | 突破失败后重试直至成功 | 境界+1，寿元与战斗属性同步刷新 | | |

### 2.3 宗门

| # | 操作 | 预期 | 日志关键字 | 结果 |
| --- | --- | --- | --- | --- |
| 3.1 | `/seeking_immortals sect candidates` → 加入一个宗门 | 身份写入，`sect status` 可见 | | |
| 3.2 | `sect donate spirit_grass` | 贡献增加，物品扣除数量正确 | | |
| 3.3 | `sect shop` 兑换一件物品 | 贡献扣除，物品进背包；贡献不足时明确拒绝 | | |

### 2.4 对话世界动作（D-A 四批的实机验证）

D-A 家族共 9 处作者动作。**必须逐个触发，不能只看代码**。

| # | 操作 | 预期 | 日志关键字 | 结果 |
| --- | --- | --- | --- | --- |
| 4.1 | 与 NPC 对话触发 `mark_structure` | 结构标记落到意图匹配的目标上，不是任选一个 | | |
| 4.2 | 触发 `hint` / `clue` | 提示带来源绑定，不落 `effect_unsupported` | | |
| 4.3 | 触发 `add_suspicion`，随后静置 | 疑点值随时间衰减（D-A-3） | | |
| 4.4 | 触发 `anomaly_log` | 按势力分桶记录 | | |
| 4.5 | 触发 `call_guard` | 召出的守卫**归属玩家**、GUARD 姿态、护卫而非攻击玩家；同势力只出 1 个 | | |
| 4.6 | 触发 `combat_flag` | 只写敌对账本与声望惩罚，**不生成任何实体** | | |
| 4.7 | `combat_or_arrest` 在 suspectLevel=0 | 仅警告，不生成实体 | | |
| 4.8 | `combat_or_arrest` 在 suspectLevel=1 | 缴罚（扣贡献或灵石），疑点清空 | | |
| 4.9 | `combat_or_arrest` 在 suspectLevel=2 | 逮捕：持久标记 + 看守；随后释放能传回 Recover 坐标 | | |
| 4.10 | 被逮捕状态下再次触发 | 提示 `already_arrested`，不重复逮捕 | | |

### 2.5 拍卖（0.2.266 新入口，重点验证）

**这一步在 0.2.266 之前无法在不开权限的情况下执行**，本版刚接通，属于首次实机验证。

| # | 操作 | 预期 | 日志关键字 | 结果 |
| --- | --- | --- | --- | --- |
| 5.1 | 在 `qinglan_mountains`（起始区域）右键拍卖请柬 | 提示「此地没有拍卖行」，**不开界面、不发声望** | | |
| 5.2 | travel 到 `dajin` 或 `chaotic_sea` | 区域切换成功 | | |
| 5.3 | 在乱星海岛杂货花 30 灵石买拍卖请柬 | 库存 5，扣款正确 | | |
| 5.4 | 在 `dajin` / `chaotic_sea` 右键请柬 | **拍卖行界面打开**；首次获得 +2 merchant_guild 声望 | | |
| 5.5 | 请柬**仍在背包**（不被消耗） | 数量不变，这是作者的通行证语义 | | |
| 5.6 | 连续右键请柬 5 次 | 界面每次都开，但声望**只加过一次** | | |
| 5.7 | 界面内翻页、preview、出价 | 出价扣灵石碎片，阶梯刷新 | | |
| 5.8 | 走开 8 格以上再点界面按钮 | 拒绝并提示 `invalid_context` | | |
| 5.9 | 结算已领先的拍品 | 奖励入包或进 outbox，不重复发放 | | |

### 2.6 阴阳窟闭环（Y-A/Y-B/Y-C 三批的实机验证）

链 `peiying_material_hunt`，三步全部要在不开权限的情况下走通。

| # | 操作 | 预期 | 日志关键字 | 结果 |
| --- | --- | --- | --- | --- |
| 6.1 | 完成前置链 `yinyang_ku_intel`（买抗毒抗阴、找窟门） | 解锁 `yinyang_ku_entry` | | |
| 6.2 | 进阴阳窟（需 元婴 + 仙玉门票 + 区域 `dajin`） | 进入成功，冷却 168000 tick 写入 | | |
| 6.3 | step 1：过银翅夜叉巢 `yy_yezha` | `ENCOUNTER_CLEARED` 证明落账 | | |
| 6.4 | step 2：在 `yy_yinzhi` **活捕**阴芝马（血量降到 35% 以下再捕） | 得到载体，状态 `LIVE` | | |
| 6.5 | 载体放主手/副手/盔甲位分别静置 20 分钟（`LIVE_TIMEOUT_TICKS`） | 三个位置都会降级为 `DEGRADED`（Y-C 修的就是只 tick 主格） | | |
| 6.6 | step 3：3 阶丹炉炼培婴丹 | 成功率落在 `[0.20, 0.35]`（活体）区间 | | |
| 6.7 | 改用击杀所得劣材重炼 | 成功率落在 `[0.15, 0.18]`，**永远低于活体**，换更好的炉/更高技能/加人都抬不上去 | | |
| 6.8 | 链完成 | `peiying_dan`（即 `nascent_soul_pill`）产出，链状态收尾 | | |

**已知偏差，照实记录不算失败**：step 3 目前只校验站点是 3 阶丹炉，在同一座炉子炼**任意**一炉丹也能过 step 3（收紧需要改证明路由 schema）。

### 2.7 迁移与旧档路径（M-B / M-C）

| # | 操作 | 预期 | 日志关键字 | 结果 |
| --- | --- | --- | --- | --- |
| 7.1 | 用本命飞剑胚 + 已认主法宝做双手绑定 | 绑定成功，胚消耗 1 个 | | |
| 7.2 | 再锻造一把**同 id** 的新法宝 | 新的那把**不享受**冷却/灵力/完整度减免，也不成长 | | |
| 7.3 | 拿新那把做炼制成长 | 不得让背包里那把已绑定的实例成长 | | |
| 7.4 | 给普通村民挂「墨老先生」命名牌，在错误区域右键 | 拒绝，且拒绝被闩住（再右键不重复评估） | | |
| 7.5 | 同样的命名牌村民放在 `tiannan` 右键 | 一次性迁移，写入持久 id + 版本 + 区域 + 时间 | | |
| 7.6 | 迁移后把命名牌改成别的名字再右键 | 身份不变（只认持久 id） | | |
| 7.7 | 伪造村民放身边，做「与绑定 NPC 对话」步骤 | 不满足门禁（邻近校验只信持久 id） | | |

### 2.8 已知不可达项（预期就是走不通，照实记录）

不要为这些项打管理员命令绕过；它们是 M-A 的有意结果或既有缺口。

| # | 项 | 预期表现 |
| --- | --- | --- |
| 8.1 | 传送到 `immortal_realm` / `asura_realm` | 被拒绝。M-A 把两者重分类为 `PREVIEW_LOCKED`，此前 `immortal_realm` 是能进的空壳 |
| 8.2 | `node_immortal_zhenyan` / `node_immortal_hub` / `node_immortal_heifeng` | **不可达**。三者 `dimension_from` 与 `dimension_to` 同为 `immortal_realm`，被 `SpatialNodeCatalogService:92` 的 `sourceDimensionMatches` 挡在前面（要求你已在仙界内），不是被 M-A 的 `!enterable()` 拒绝 |
| 8.3 | `gate_spirit_to_immortal` | 走 `ascension_gate` → `AscensionService.attemptAscension`，目标是天元而非仙界，不经 M-A 的门 |
| 8.4 | 24 条 `access_route` | `isDirectRouteImplemented` 只放行 `mortal_to_tianyuan` 与 `tianyuan_to_fengyuan`，其余未实现 |
| 8.5 | 阴阳窟 `poison_insect` / `array_spirit` / `yin_sha_mist` | 无对应实体，不生成（fail-closed），应表现为「该条目缺席」而非报错 |
| 8.6 | `yy_alchemy_coop` 的 `furnace_safety_array` 防爆陷阱 | 未实现，协作点目前只是普通丹炉所在地 |

## 3. QA-02 专服 + 双客户端

`./gradlew runServer` + 两个客户端（玩家 A / 玩家 B）。以下每项都要**两侧同时观察**。

| # | 操作 | 预期 | 日志关键字 | 结果 |
| --- | --- | --- | --- | --- |
| 9.1 | 两客户端登入专服 | 协议 31 双侧一致，无 mismatch 断连 | `PROTOCOL_VERSION` | |
| 9.2 | `live_smoke mp run`（任一侧） | MP 探针项全部有值：`protocol_version`、`server_mode`、`auction_outbid_ledger`、`outbox_uuid_isolation`、`station_ops_ledger`、`mp_sequence_catalog` | | |
| 9.3 | A 出价，B 随后加价 | A 收到被超越通知；A 的押金退还账本正确，不双退 | `auction_outbid_ledger` | |
| 9.4 | A、B 同时对同一拍品出价 | 只有一个成为领先者，另一个明确被拒；灵石不凭空消失 | | |
| 9.5 | A 领先时再次出价 | 提示 `already_leader`，不重复扣押金 | | |
| 9.6 | 结算后 A、B 各自的 outbox | 奖励只进赢家账户，**UUID 隔离**，B 拿不到 | `outbox_uuid_isolation` | |
| 9.7 | A 满背包时结算 | 进 outbox 而非掉地上；重登后可领取，且只领一次 | | |
| 9.8 | 两人同时操作同一工站（form / repair / overhaul） | 操作账本串行化，不出现双记 | `station_ops_ledger` | |
| 9.9 | 协作炼丹：A 主炼，B 站 6 格内 | B 计入协作，成功率 +3%/人，最多 4 人；仍不越出活体上限 0.35 | | |
| 9.10 | 协作炼丹失败/炸炉 | 参与者名单正常结算，不残留 | | |
| 9.11 | 术法冷却：A 释放后 B 释放同一术法 | 冷却各自独立，不共享 | | |
| 9.12 | PvP：A 打 B | 走 `LivingHurtEvent`（护甲前），命中/闪避/暴击/减伤流水线正常，无递归伤害 | | |
| 9.13 | A 出价后立刻断线，B 加价 | A 的押金不丢；A 重连后账本一致 | | |
| 9.14 | A 在拍卖界面开着时被踢/断线 | 界面 token 失效，重连后需重新开界面 | | |
| 9.15 | A 携带 `LIVE` 载体断线 20 分钟后重连 | 载体降级行为符合预期，不因离线卡在 `LIVE` | | |
| 9.16 | A 被逮捕状态下断线重连 | 逮捕标记持久保留，释放仍能传回 Recover 坐标 | | |

## 4. 签字

两段都跑完、失败项都记录后，由执行者填写：

- QA-01 结论：______（通过 / 不通过）　执行者：______
- QA-02 结论：______（通过 / 不通过）　执行者：______

可选：在游戏内执行 `/seeking_immortals live_smoke sign <备注>` 与 `live_smoke mp sign <备注>` 留机器记录。
注意这两条会覆盖 `manual_live_smoke_checklist.md` 与 `manual_multiplayer_signoff_checklist.md`（见第 1 节的文件映射），
但**不会**动本文件。机器签字只是辅助记录，不能替代本清单里逐格的人工结果。

## 5. 清单本身的局限

- 本清单由 AI 依据源码与作者数据编写，**未经实机运行**。第 2、3 节的预期值来自代码与数据，不是观测结果。
- 我无法运行客户端或专服，因此无法代填任何一格。
- 2.6 的 6.2 需要元婴境界，正常推进耗时较长；若为验证而用管理员命令垫境界，该步及其下游（6.3–6.8）必须标注「已用管理员命令」并作废其签字效力。

