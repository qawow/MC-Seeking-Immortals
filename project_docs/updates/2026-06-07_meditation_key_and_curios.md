# 2026-06-07 更新：打坐按键与 Curios 兼容增强

## 新增

- 新增项目文档目录 `project_docs/`。
- 新增当前物品清单 `project_docs/items.md`。
- 新增当前功能清单 `project_docs/features.md`。
- 新增打坐修炼按键：默认 `V`。
- 新增客户端到服务端网络包，用于切换打坐状态。
- 玩家打坐时每 5 秒额外恢复灵力并增加修为。

## 调整

- 灵力护符继续通过 Curios API 检测。
- 增加 Curios 饰品标签，让 `spirit_charm` 可放入 `charm` 槽位。

## 验证

- 本次更新后需要执行 `./gradlew --no-daemon build --stacktrace`。
