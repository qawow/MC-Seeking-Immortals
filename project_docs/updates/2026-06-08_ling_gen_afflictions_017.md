# 2026-06-08 更新：版本 0.1.7，灵根固定规则与不可清除负面状态基础

## 版本

- `mod_version` 从 `0.1.6` 提升到 `0.1.7`。
- 构建产物目标为 `seeking_immortals-0.1.7.jar`。

## 灵根测试石规则修正

- 灵根测试石现在遵循“首次使用创建，后续使用只读取”的规则：
  - 玩家首次被测试时才随机生成灵根、星级、属性组合、纯度、觉醒状态。
  - 玩家已经测试过后，再次使用灵根测试石只显示既有结果，不会重新随机。
  - 灵根数据继续保存于现有玩家修炼 Capability/NBT。
- 预留了特殊道具/事件修改灵根的接口方向：普通测试石不再承担刷新灵根职责。

## 新增不可牛奶/死亡清除的修仙负面状态基础

新增 `ImmortalAffliction` 与玩家修炼数据字段，状态保存进 Capability/NBT，并通过玩家死亡 Clone 继承。由于核心状态不是原版药水效果，牛奶不会清除这些修仙负面状态；死亡重生后也会继承。

### 重伤

- 保存字段：`SevereInjury`。
- 效果：
  - 最大生命上限降低 80%。
  - 灵力恢复速度降低 40%。
- 解除条件：灵力恢复到当前最大灵力后自动解除。

### 碎丹

- 保存字段：`ShatteredCore`。
- 效果：玩家造成伤害降低 30%。

### 跌境

- 保存字段：`RealmFallScars`。
- 效果：触发时回退一层境界进度，20% 概率额外再回退一层。
- 会同步压制当前灵力不超过跌境后的最大灵力。

### 心魔（基础版）

- 保存字段：`HeartDemonLevel`、`HeartDemonTriggerTicks`。
- 首次获得心魔时建立 1 层心魔并安排 10-30 分钟随机触发。
- 触发时给予反胃与失明 1 分钟，并提示心魔翻涌。
- 心魔层数提升会缩短后续触发间隔。
- 内心世界、无法放置方块/船、心魔幻象、感知错乱等内容标记为后续复杂迭代；不再规划“心魔击杀玩家后削弱自我数值”的机制。

## 调试命令

新增 OP 调试命令，便于测试服验证状态继承与效果：

- `/seeking_immortals affliction severe_injury`
- `/seeking_immortals affliction heart_demon`
- `/seeking_immortals affliction realm_fall`
- `/seeking_immortals affliction shattered_core`

## 验证

- 已执行 `./gradlew build`。
- 构建结果：`BUILD SUCCESSFUL`。
