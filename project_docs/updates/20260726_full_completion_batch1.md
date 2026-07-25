# 2026-07-26 全量功能闭环（0.2.176）

## 范围

本批承接 `0.2.174` 的发布审查修复，集中处理作者目录中仍有数据但缺生产消费端、缺可达入口或可被旁路绕过的功能。没有新增网络包、注册 id 或不兼容持久化格式。

## 已完成

- 多方块：新增 `CatalogStationGeometry`，把 32 类原失败关闭 validator 编译为 47 个有界真实设施几何；`MultiblockStationService` 使用同一结果校验，失败关闭集合归零。
- 建造投影：`MultiblockProjectionCatalog` 直接转换共享几何，投影从 43 个旧控制器扩展到 90 个定义；客户端同时接受 BlockItem 与普通目录设施载体。
- NPC：按地域/旅行锚点每批最多投放 3 名作者 NPC，以服务器 SavedData 持久去重，并在登录和每 600 tick 补齐；只在记录区块已加载且实体确实失效后解除记录并补生。
- 商店：执行商店/商品境界范围、正负声望、`access.any_of`、商品级声望、`illegal` 与 `risk_events`；未知非空境界失败关闭，所有门禁发生在扣款和库存变更前，失败购买按实际普通/采珠事件库存桶回滚并立即持久化。
- 每日事件：`merit_mult_2`、`inverse_star_smuggle_chance`、`star_palace_patrol_bonus`、`pvp_disabled_factions`、`pvp_local`、`shop_pearl_raw_stock` 均接入任务/Boss/秘境奖励、走私、巡防、PvP 或库存生产路径。
- 任务：执行业力、扩展父链、最低境界、最大队伍、阶段分支、阶段前置和势力任一条件；原生推进与 FTB 精确单阶段回写共享门禁。
- 秘境：23 个作者秘境显式投影为 10 类入口场景，场景按维度/秘境只生成一次并保留安全落脚。
- 法宝：现有裂隙司南改用专用实现，成功激活后扫描最近已成形空间节点并报告名称、方向和距离。
- 视觉：作者 `SCREEN_OVERLAY` 进入有界 HUD 叠层；`MODEL_ANIMATION` 保存作者状态并触发锚定生物主手动作；跨世界/重置路径清理两类状态。

## 权威与兼容

- `mod_version`: `0.2.174 -> 0.2.176`；`0.2.175` 首次完整构建后又加入库存/NPC/境界收尾，版本门禁要求继续升一个补丁版本。
- `ModNetwork.PROTOCOL_VERSION`: 保持 `30`。
- 理由：没有改变网络包字段、顺序、类型、注册表或频道兼容行为。
- 商店、任务、NPC、秘境与法宝结果均由服务端判定；客户端视觉仅消费既有 S2C 意图。

## 验证

- `./gradlew compileTestJava --no-daemon --max-workers=1`: 成功。
- 任务精确回写、多方块投影、客户端视觉三组定向测试：成功。
- `./gradlew build --no-daemon --max-workers=1`: `BUILD SUCCESSFUL in 1m 18s`。
- 全量测试：1,128 项，failure/error/skipped 均为 0。
- `aiPreflight`: 通过并记录 `mod_version=0.2.176`。

## 回滚与剩余风险

- 主回滚：`.bak/20260725_204810_full_completion_batch1/`。
- 收尾回滚：`.bak/20260726_full_completion_followup/`。
- 仍需真实客户端、专服和双客户端检查 NPC 落点/密度、设施投影、秘境场景、司南方向、HUD 叠层/持物动作、多人事件与长时重连。
- `MODEL_ANIMATION` 当前是状态记录加生物持物动作，不等同于逐法宝骨骼动画。
- 已删除的凭据诊断包不可恢复；曾暴露的 Minecraft 令牌仍需账号持有人在账号侧撤销或刷新。
