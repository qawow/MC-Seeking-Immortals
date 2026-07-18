# 2026-07-19 M14 状态消费端接线

## 范围

仅补齐 `outgoingDamageMul`、`blocksTechnique`、`hidesRealm` 三个既有状态字段的下游消费路径。M02 术法、M10 妖兽的真实状态施加端，以及 M15 `DamagePipelineHooks` 注册不在本批范围。

## 实现

- `StatusRegistry.outgoingDamageMultiplier(LivingEntity)` 遍历实体当前生效的 `SeekingStatusEffect`，将各状态出伤倍率连乘，空状态保持 `1.0`。
- `ModEvents.onLivingHurt` 在修炼倍率、弹射物专用倍率和 M15 法宝协同倍率之后，继续乘入状态倍率；未新建伤害路径，也未修改 `CombatCalculator`。
- `TechniqueGateService.canCast` 前置复用 `StatusRegistry.blocksTechnique(player)`，封婴/禁术状态返回统一拒绝结果。现有主释放和 `tryDualCast` 均继续通过该中央门禁。
- `StatusRegistry.hidesRealm(LivingEntity)` 与封术判断同构，读取当前生效的 `SeekingStatusEffect` 标志。
- `DivineSenseSpell.castMindRead` 对玩家目标新增境界读取：先判定敛息，隐藏时结果不携带真实境界；未隐藏时才读取目标 capability 的真实境界，capability 缺失显示无法判定。
- 境界结果继续走现有服务端 `displayClientMessage`，未触碰 `SyncCultivationDataPacket` 或自身 HUD。

## 测试

- `StatusRegistryTest`：默认倍率 `1.0`；`berserk 1.20 × sword_intent 1.10 = 1.32`；`seal_nascent` / `conceal_qi` 标志消费。
- `TechniqueGateServiceTest`：封术状态中央 `canCast` 返回拒绝及正确提示 key；未封术继续正常门禁。
- `DivineSenseSpellTest`：敛息结果无真实境界参数；公开结果返回真实境界；能力缺失不回退为凡人。
- 聚焦测试：7/7 通过。
- 全量测试：594/594 通过。
- 提交前全量构建：`./gradlew cleanTest build --no-daemon -PaiSkipVersionBumpCheck=true`，`BUILD SUCCESSFUL in 1m 4s`。`cleanTest` 仅清理筛选测试遗留的生成缓存，随后执行完整 `build`。

## 版本、协议与备份

- 备份：`.bak/20260719_003407_m14_status_consumers/`
- `mod_version`：按任务明确红线保持 `0.2.17`。
- `ModNetwork.PROTOCOL_VERSION`：保持 `24`；未新增或修改包字段、顺序、编码或通道行为。
- 因代码变更但任务禁止版本升级，提交前构建将按既有 M14 红线惯例显式使用 `-PaiSkipVersionBumpCheck=true`；完成本地提交后再运行普通 `./gradlew build --no-daemon` 验证最终干净树。

## 后续边界

本批只证明消费端已就绪。`seal_nascent`、`conceal_qi`、`berserk` 等状态在 M02/M10 实际战斗场景中仍需后续把上游施加逻辑改为真实 `StatusRegistry.applyStatus`，否则不会自然进入这些消费路径。
