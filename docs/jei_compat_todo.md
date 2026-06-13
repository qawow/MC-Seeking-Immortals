# JEI 兼容说明 / TODO

当前版本只包含原版 `minecraft:crafting_shaped` / `minecraft:crafting_shapeless` 合成配方；安装 JEI 后，这些配方会被 JEI 自动显示，不需要额外 JEI Plugin。

后续如果加入丹炉、炼丹台、符箓绘制台等自定义配方类型，应新增 JEI 兼容模块，例如：

- 创建 `compat.jei` 包；
- 添加带 `@JeiPlugin` 注解的插件类；
- 注册自定义 recipe category、recipe catalyst、recipe transfer handler；
- 仅引用 JEI API，保持 `mods.toml` 中 JEI 为 `mandatory=false`。

依赖策略：`build.gradle` 使用 `compileOnly` 引入 JEI API，`runtimeOnly` 仅用于开发运行环境测试，避免把 JEI 变成硬依赖。
