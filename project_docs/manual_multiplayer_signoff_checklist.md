# 专服 + 双客户端多人复签清单（0.2.104）

> 对应 `master_plan.md` §6 第 9 项。  
> 代码侧权威回归与自动探测已落地；**真人双客户端签字**仍需本清单在实机完成。

## 前置

1. 版本一致：`mod_version=0.2.105`，`ModNetwork.PROTOCOL_VERSION=26`。
2. 启动专服：
   - 开发：`./gradlew runServer`
   - 或将 `build/libs/seeking_immortals-0.2.105.jar` 放入生产服 `mods/`。
3. 两个客户端使用**同一协议 26** 的客户端 jar 加入专服。
4. 游戏内命令：
   - `/seeking_immortals live_smoke mp` — 多人权威自动探测
   - `/seeking_immortals live_smoke mp sign <note>` — 人工多人签字

## 自动探测（专服上任意一客户端执行）

`/seeking_immortals live_smoke mp` 应通过：

| id | 含义 |
|---|---|
| `protocol_version` | 协议字符串可读（期望 26） |
| `server_mode` | `dedicated=true` 且 online≥1 |
| `auction_outbid_ledger` | 拍卖 SavedData 可读 |
| `outbox_uuid_isolation` | 交付 outbox 按 UUID 计数 |
| `station_ops_ledger` | 工站运营 SavedData 可读 |
| `mp_sequence_catalog` | 多人序列展示目录（仅展示，不锁） |

报告写入：`project_docs/live_smoke_mp_report_latest.md`

## 人工步骤（master_plan §6.9）

- [ ] **mp_pvp** — 开 PvP（或同队关闭后再开）：玩家 A 对 B 释放术法/近战一次；确认伤害走服务端权威、友好目标不误伤（`SpellEffect.canAffect` / `canHarmPlayer`）。
- [ ] **mp_station_concurrent_form** — 同一工站壳（如一品丹炉）两人几乎同时 `form`/右键启封：仅一方成功扣材，另一方收到已启封/需 overhaul 提示；工站状态不双写吞材料。
- [ ] **mp_auction_outbid** — A 对某 lot 出价后下线；B 抬价；A 重登后收到 outbid 退款（在线即时退 / 离线 `pendingRefund` + 登录 `claimPendingRefunds`）。
- [ ] **mp_outbox_reconnect** — 背包塞满后触发奖励（制符/拍卖退款/任务奖励任选）；确认进 outbox；断线重连后 `claimQueued` 补发，不丢不双发。
- [ ] **mp_technique_cooldown_reconnect** — A 释放术法进入冷却后断线重连；冷却仍在（`TechniqueCooldownUntilTicks` 能力 NBT）。
- [ ] **mp_protocol_match** — 故意用旧协议客户端尝试连接应被拒绝；双端均为 26 时正常同步修仙数据。

## 签字

全部人工步骤通过后，在专服上执行：

```text
/seeking_immortals live_smoke mp sign 0.2.105_dedicated_two_client
```

将生成：

- `project_docs/live_smoke_mp_report_signed.md`
- `project_docs/manual_multiplayer_signoff_checklist.md`（本文件被运行时覆盖为 signed 版时以命令输出为准）

## 代码侧已覆盖（无需真人即可回归）

- `MultiplayerAuthorityRegressionTest`：拍卖 outbid 账本隔离与 NBT 往返、工站 key 隔离/后写、form 拒双启封源契约、登录 claim outbox+refund、冷却 NBT、PvP canHarm、协议 26、live_smoke 多人面存在性。
- `ModNetworkDirectionTest`：全部包方向显式声明。
- `CultivationAuthorityRegressionTest`：冷却全局时间 NBT 版本。
- `AuctionHouseSavedDataCompatibilityTest`：旧档缺 PendingRefunds 可读。

## 本批无法在本环境完成的部分

- 真实两个 GUI 客户端同时在线操作。
- 人工观感/手感（HUD、粒子、拍卖 UI 竞价动画）。
- 生产网络延迟下的竞态（本批用 SavedData 权威 + 预留→提交→失败退款路径约束）。
