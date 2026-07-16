# 2026-07-16 M14 战斗与状态落地

## 范围

实现 `project_docs/task_briefs/M14_战斗状态.md` 全部功能点：统一状态注册表、境界/体质抗性规则、配装基线、十毒解药映射、三条战斗管线风险修复、对外 applyStatus/伤害钩子。

## 变更摘要

### 语料

- 新建权威 `文本材料/data/status_effects.json`（22 个状态 id，含 bible 10 核心 + icon sheet + 毒/伪装扩展）
- 发布副本：
  - `src/main/resources/data/seeking_immortals/text_material/status_effects.json`
  - `loadout_by_realm_v99.json`
  - `ten_poisons_antidotes_v99.json`

### 代码

- `registry/ModMobEffects`：按语料 id 注册 Forge `MobEffect`
- `combat/status/SeekingStatusEffect`：tick 伤/疗、移速/护甲修饰
- `combat/status/StatusCatalogService`：读 JSON + 兜底
- `combat/status/StatusRegistry`：`applyStatus` / `clearStatus` / 命中率公式
- `combat/status/PoisonAntidoteService`：十毒变体 → 状态；解药驱散
- `combat/LoadoutByRealmService`：配装语料 + 同境伤害基线
- `combat/DamagePipelineHooks`：伤害前后置回调（M15/M07）
- `combat/CombatCalculator`：公开 `getCombatStats` Optional；接钩子；null 安全
- `event/ModEvents`：`LivingAttackEvent` HIGH 早裁决 + `ThreadLocal` PvP 守卫；Hurt 消费裁决缓存
- 中英 lang 增加 `effect.seeking_immortals.*`

### 边界

- M01 `ImmortalAffliction`（心魔/重伤/碎丹/跌境）与 M14 短时状态 id 空间不重叠（测试断言）
- 物品注册仍归 M03；本模块只提供毒/解药效果映射 API

### 网络 / 版本

- **未新增**自定义网络包；状态同步依赖原版 MobEffect 通道
- `ModNetwork.PROTOCOL_VERSION` 保持 `21`（是否递增交用户确认；本轮不改）
- `mod_version` 按任务红线保持 `0.1.506`；构建使用 `-PaiSkipVersionBumpCheck=true`

## 备份

`.bak/20260716_172740_m14_combat_status/`

## 验证

- 测试：`M14CombatStatusTest`
- 构建：`./gradlew --no-daemon build -PaiSkipVersionBumpCheck=true`

## 2026-07-17 主线合并校正

- 主线已在 `LivingHurtEvent` 通过 `setAmount/cancel` 完成非递归早阶段裁决，因此未采用分支的 `LivingAttackEvent` 持久伤害缓存。
- 该取舍避免缓存残留污染后续伤害，也避免缓存结果覆盖 M15 法宝攻防协同倍率。
- `StatusRegistry.applyStatus` 的无施术者入口按契约改为必然命中，有害状态仍受体质抗性缩短持续时间。
- M14 本身无自定义网络包变化；主线集成基线为 `mod_version=0.1.509`、protocol 22，等待 17 模块统一发布。
- 主线集成态 `./gradlew build --no-daemon -PaiSkipVersionBumpCheck=true` 已完成，`BUILD SUCCESSFUL`（1m12s）。
