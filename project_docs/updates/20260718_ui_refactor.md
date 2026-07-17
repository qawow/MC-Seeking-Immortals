feat: 重构界面基础渲染逻辑至修仙风

更新内容：
- 将 CultivationStatsScreen, MeditationScreen, QuestTrackerScreen 的部分 UI 绘制逻辑调整为使用 ImmortalUiSkin 渲染。
- 修改背景绘制、标题栏以及部分组件交互逻辑，使其风格统一为玉简/古籍风格。
验证结果：修改涉及核心修仙界面的基础渲染，Gradle 构建成功。
版本与协议：mod_version 升级至 0.2.7。
