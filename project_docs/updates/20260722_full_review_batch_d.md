# 全量代码审查修复 — Batch D：MEDIUM 实体同步与物品 NBT

**完成时间**: 2026-07-22
**版本**: 0.2.165 → 0.2.166
**变更类别**: 代码（Java）

---

## 修复内容

### 弹射物双端模拟（同步/安全）
**文件**: `entity/SwordProjectileEntity.java`、`entity/CultivationFireballEntity.java`
**问题**: `tick()` 中的命中判定、`target.hurt(...)` 伤害与 `discard()` 在客户端和服务端都会执行（无 `isClientSide` 判断），客户端会独立修改本地生物血量并提前丢弃弹射物。
**修复**: 将命中/伤害/移除逻辑包在 `!level().isClientSide` 中，客户端仅做位置插值。

### 灵舟双端移动失步（同步）
**文件**: `entity/SpiritBoatEntity.java`
**问题**: 移动计算（`setDeltaMovement` + `move`）在双端各自执行，客户端自行模拟载具，与服务端权威位置失步/抖动。
**修复**: 权威移动仅在服务端计算并应用，客户端依赖实体追踪插值。

### 灵兽成长重复计入（持久化）
**文件**: `entity/CultivationBeastEntity.java`
**问题**: `terminalGrowthCredited` 未写入/读取 NBT，灵兽在生命末期区块卸载后重新加载、再被遣散时会向 `BeastContractService` 重复计入一次成长。
**修复**: 新增 `TAG_TERMINAL_GROWTH_CREDITED`，在 `addAdditionalSaveData`/`readAdditionalSaveData` 中持久化该布尔值。

### 灵液瓶客户端改写 NBT（同步）
**文件**: `item/MysticVialItem.java`
**问题**: `refillIfNeeded()` 会写入 `CHARGES_KEY`/`LAST_REFILL_KEY`，却在 `use()`/`useOn()`/`appendHoverText()` 中于客户端调用，导致客户端物品栈 NBT 被本地改写、与服务端失步。
**修复**: 充能仅在服务端执行（`use`/`useOn` 在 `isClientSide` 早退之后调用）；tooltip 改为只读展示，不再改写 NBT。

### 灵石悬停改写 NBT（同步）
**文件**: `item/SpiritStoneItem.java`
**问题**: `getStoredPower()` 在每次读取（含客户端悬停/耐久条）时惰性写入 `STORED_POWER_TAG`，导致仅悬停即修改 NBT，新获得的灵石在客户端被悄悄初始化为满。
**修复**: 读取路径不再写 NBT；tag 缺失时返回默认值（满），仅在服务端真正吸取/切换时持久化。

### 阴棺钉不可获得（内容缺口）
**文件**: `registry/ModCreativeTabs.java`
**问题**: `YinCoffinNailItem` 已注册/本地化/有模型，但不在创造物品栏、也无任何配方/战利品来源，鬼契核心物品在正常游玩中无法获得。
**修复**: 将其加入创造物品栏（“鬼契系统”分组）。

---

## 修改文件清单

1. `gradle.properties` — mod_version 0.2.165 → 0.2.166
2. `src/main/java/com/xunxian/seekingimmortals/entity/SwordProjectileEntity.java`
3. `src/main/java/com/xunxian/seekingimmortals/entity/CultivationFireballEntity.java`
4. `src/main/java/com/xunxian/seekingimmortals/entity/SpiritBoatEntity.java`
5. `src/main/java/com/xunxian/seekingimmortals/entity/CultivationBeastEntity.java`
6. `src/main/java/com/xunxian/seekingimmortals/item/MysticVialItem.java`
7. `src/main/java/com/xunxian/seekingimmortals/item/SpiritStoneItem.java`
8. `src/main/java/com/xunxian/seekingimmortals/registry/ModCreativeTabs.java`

## 备份路径

`.bak/20260722_batch_d/`

## 验证结果

`./gradlew build` — BUILD SUCCESSFUL（1m 18s），preflight 记录 mod_version=0.2.166。

## 版本与协议

mod_version 0.2.165 → 0.2.166。未改动网络包字段/顺序/编码，`ModNetwork.PROTOCOL_VERSION` 不变。
