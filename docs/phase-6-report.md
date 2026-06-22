# Phase 6 Report - 神秘小瓶系统

> Date: 2026-06-19
> Scope: Phase 6 only
> Build: `.\gradlew.bat --no-daemon --max-workers=1 build` BUILD SUCCESSFUL in 34s
> Phase boundary: Did not implement Phase 7 HUD or any later system.

## Implemented

- 新增 `MysticVialItem`：绑定玩家、唯一堆叠、附魔光效、不可丢弃（`onDroppedByPlayer` 返回 false 并提示）。
- NBT 存储 `vialCharges` / `vialLastRefill` / `vialMaxCharges` / `vialOwner` / `vialGrade`，不破坏已有 NBT；缺失字段按默认值补全。
- 灵液充能：每现实 24 小时（`MILLIS_PER_CHARGE`）积累 1 份，默认最大 5 份（复用 `VialGrade.BASIC`）。
- 离线充能：`refillIfNeeded` 按现实毫秒时间补算 `(now - lastRefill) / 24h`，仅消耗已用于充能的时间，保留未满一份的余量；在 use/useOn/tooltip 时触发。
- 植物加速：手持小瓶右键作物/可催熟方块，消耗 1 份灵液；对 `CropBlock` 按成长倍率多次步进催熟，对 `BonemealableBlock` 执行多次 `performBonemeal`，附带粒子与音效。
- 绑定接入 `PlayerCultivation`：新增 `mysticVialGranted` 字段与 NBT，灵根测试后对伪/杂灵根（`isLowTalent()`）发放一次小瓶并标记。
- UI/提示：tooltip 显示当前灵液份数；右键无灵液提示 `mystic_vial.no_charges`；成功使用提示 `mystic_vial.used`；发放提示 `mystic_vial.granted`；不可丢弃提示 `mystic_vial.cannot_drop`。
- 注册 `MYSTIC_VIAL` 物品、加入创造栏、补 `mystic_vial` 物品模型与中英文本地化。

## GUI Decision

未做独立小瓶 GUI。MVP 用 tooltip + 聊天提示展示灵液份数与使用结果，符合任务范围“显示当前灵液份数/无灵液提示/成功提示”。

## Validation

```powershell
.\gradlew.bat --no-daemon --max-workers=1 build
```
Result: BUILD SUCCESSFUL in 34s（compileJava / test / build 通过）。

构建过程修复：
1. 首次编译因 PowerShell `Set-Content -Encoding UTF8` 写入 BOM 导致 `非法字符: '\ufeff'`，已用 .NET UTF8Encoding(false) 去除 5 个文件 BOM。
2. 第二次编译 `Inventory.contains(Item)` 类型不匹配，已改为 `contains(ItemStack)`。

## Non-Goals

- 未实现 Phase 7 HUD 或后续阶段。
- 未实现绿色/金色/彩虹灵液升级、天雷竹特殊逻辑、炼丹/炼器注入副功能。
- 未实现南海 arc、青竹蜂云剑、高阶小瓶升级、任务线。
- 未做无关重构。
