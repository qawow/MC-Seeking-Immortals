# UI 维护

> 本表用于记录“一次目标”被拆成小步骤后推进到哪里。
> **强制规则：每次开始任务必须先读取本表；每次完成小步、遇到阻塞、完成构建验证或阶段收尾，都必须更新本表。**

## 1. 使用规则

1. 开始任何开发任务前，先读取：
   - `project_docs/ai_handoff.md`
   - `project_docs/step_progress.md`
2. 如果任务涉及物品、系统、版本更新，还要读取：
   - `project_docs/items.md`
   - `project_docs/pending_requests.md`
3. 每完成一个小步，都要在本表中更新状态、时间和备注。
4. 如果任务中断，要把当前做到哪里、下一步该做什么写清楚。
5. 每次版本完成后，同步更新：
   - `items.md`
   - `pending_requests.md`
   - `updates/YYYYMMDD_版本号.md`
6. 每次代码修改后必须执行 `./gradlew build`，并记录结果。

## 2. 状态标识

| 状态 | 含义 |
| --- | --- |
| 📋 规划中 | 已列入步骤，但还未开始 |
| ⚡ 进行中 | 正在执行 |
| ✅ 已完成 | 已完成并通过必要验证 |
| ⚠️ 阻塞 | 遇到问题，需处理后继续 |
| 🔄 暂停 | 暂时搁置，后续继续 |
| ❌ 取消 | 不再执行 |

## 3. 当前一次目标

**目标名称**：维护原生 Forge/Minecraft UI 的左侧技能栏、独立修仙面板、打坐吐纳 HUD、功法/灵根信息界面。

**目标说明**：

- 先采用安全的兼容式迁移：保留 Forge Overlay / Screen 入口，逐步把绘制与组件结构抽到 旧第三方 UI 风格适配层。
- 等 API 和结构稳定后，再进一步替换为完整 旧第三方 UI 组件 / Screen。
- 当前 `0.1.25` 已完成第一阶段。

**当前版本**：`0.1.34`

**最后更新时间**：2026-06-11 13:38 CST

## 4. 总体进度

| 阶段 | 内容 | 状态 | 进度 | 备注 |
| --- | --- | --- | --- | --- |
| 阶段一 | 左侧技能栏 旧第三方 UI 适配 | ✅ 已完成 | 100% | 0.1.25 已完成，固定 7 格，空槽透明背景 |
| 阶段二 | 背包“修仙”页 旧第三方 UI 信息面板 | ✅ 已完成 | 80% | 0.1.26 已修复背包槽显示，并完成四分页；后续仍可换完整 旧第三方 UI 组件 |
| 阶段三 | 打坐吐纳状态栏 / HUD | ✅ 已完成 | 70% | 0.1.26 已新增屏幕下方独立吐纳 HUD；后续补闭关/风险/威胁 |
| 阶段四 | 功法/灵根信息界面 | ✅ 已完成 | 60% | 0.1.26 已独立为分页渲染方法，为后续详情页做准备 |
| 阶段五 | 技能交互、tooltip、冷却、释放 | ⚡ 进行中 | 55% | 0.1.34 已完成绑定槽持久化、保存/清空网络包、绑定槽快捷键释放和 5 秒默认冷却；真实效果/UI 拖拽待实现 |

## 5. 详细步骤表

| 步骤编号 | 任务项 | 状态 | 完成时间 | 备注 |
| --- | --- | --- | --- | --- |
| 1.1 | 检查当前 旧第三方 UI 依赖接入 | ✅ 已完成 | 2026-06-09 | 0.1.24 已接入 `已移除的旧第三方 UI 依赖` |
| 1.2 | 分析现有左侧技能栏代码 | ✅ 已完成 | 2026-06-09 | 文件：`TechniqueSkillBarOverlay.java` |
| 1.3 | 左侧技能栏固定显示 7 个槽位 | ✅ 已完成 | 2026-06-09 | 常量：`SKILL_SLOT_COUNT = 7` |
| 1.4 | 无技能时显示空框 + 透明背景 | ✅ 已完成 | 2026-06-09 | 通过 `ImmortalUiSkin.drawSkillSlot(..., false)` 绘制 |
| 1.5 | 有技能时显示槽位 + 占位图标 | ✅ 已完成 | 2026-06-09 | 当前仍是按技能 ID hash 生成占位色块和首字母 |
| 1.6 | 新增统一 旧第三方 UI UI 皮肤适配层 | ✅ 已完成 | 2026-06-09 | 文件：`ImmortalUiSkin.java` |
| 1.7 | 背包“修仙”页主面板接入统一皮肤 | ✅ 已完成 | 2026-06-09 | 文件：`CultivationStatsScreen.java` |
| 1.8 | 修仙页底部新增灵力/吐纳状态条 | ✅ 已完成 | 2026-06-09 | 方法：`drawBreathingStatusBar(...)` |
| 1.9 | 更新版本号到 0.1.25 | ✅ 已完成 | 2026-06-09 | 文件：`gradle.properties` |
| 1.10 | 执行 `./gradlew build` 验证 | ✅ 已完成 | 2026-06-09 | BUILD SUCCESSFUL，产物 `seeking_immortals-0.1.25.jar` |
| 1.11 | 同步 `items.md` | ✅ 已完成 | 2026-06-09 | 已记录 0.1.25 UI 迁移状态 |
| 1.12 | 同步 `pending_requests.md` | ✅ 已完成 | 2026-06-09 | 已记录后续完整 旧第三方 UI 组件 / Screen 待办 |
| 1.13 | 同步 `updates/20260609_0.1.25.md` | ✅ 已完成 | 2026-06-09 | 已记录实现与后续迁移点 |
| 1.14 | 创建 AI 交接文档 | ✅ 已完成 | 2026-06-09 15:49 | 文件：`ai_handoff.md` |
| 1.15 | 创建本步骤进度表 | ✅ 已完成 | 2026-06-09 15:49 | 文件：`step_progress.md`，后续每次任务必须读取和更新 |
| 2.1 | 开始 0.1.26 前读取本步骤表 | ✅ 已完成 | 2026-06-09 16:17 | 已读取；同时发现 `ai_handoff.md` 缺失并恢复 |
| 2.2 | 重新分析 `CultivationStatsScreen.java` 当前布局 | ✅ 已完成 | 2026-06-09 16:22 | 根因初判：修仙页继承普通 `Screen` 并替代 `InventoryScreen`，不会渲染原版玩家物品槽 |
| 2.3 | 修复玩家物品栏不显示物品 bug | ✅ 已完成 | 2026-06-09 16:28 | `CultivationStatsScreen` 改为继承 `InventoryScreen` 并打开时传入当前玩家，保留原版背包槽渲染 |
| 2.4 | 抽取可复用信息行 / 面板绘制方法 | ✅ 已完成 | 2026-06-09 16:28 | 修仙页拆成基础状态、灵根、功法、负面状态四个独立渲染方法；保留后续详情入口 |
| 2.5 | 背包“修仙”页增加灵根/功法更明确分组 | ✅ 已完成 | 2026-06-09 16:28 | 已改为 旧第三方 UI 风格四分页按钮：基础状态、灵根信息、功法信息、负面状态 |
| 2.6 | 构建验证并更新文档 | ✅ 已完成 | 2026-06-09 16:33 | 首次构建因同步包括号失败，修复后 `./gradlew build` 成功，产物 `0.1.26.jar` |
| 2.7 | 复查 UI 显示问题根因 | ✅ 已完成 | 2026-06-09 17:17 | 当前工作区非 Git 仓库；旧第三方 UI-MultiLoader README 仅为多加载器模板说明，结合 Forge 1.20.1 改为 MOD bus 注册式 Overlay 生命周期 |
| 2.8 | 修复 HUD/技能栏依赖背包初始化和渲染状态污染 | ✅ 已完成 | 2026-06-09 17:21 | 技能栏/吐纳 HUD 改为 `RegisterGuiOverlaysEvent.registerAboveAll` 注册，并在屏幕打开时不渲染；修仙页面板移入 `renderBg`，避免覆盖原版槽与物品 |
| 3.1 | 设计独立打坐吐纳 HUD 位置 | ✅ 已完成 | 2026-06-09 16:30 | 已放置在屏幕下方中央，避开原版快捷栏上方 |
| 3.2 | 抽出吐纳状态条绘制工具方法 | ✅ 已完成 | 2026-06-09 16:30 | 新增 `BreathingHudOverlay` 并复用 `ImmortalUiSkin.drawStatusBar` |
| 3.3 | 显示灵气浓度、功法倍率、灵根亲和、5 秒结算进度 | ✅ 已完成 | 2026-06-09 16:30 | `SyncCultivationDataPacket` 新增灵气浓度/性质同步，HUD 显示 5 秒进度 |
| 4.1 | 规划功法信息界面 | ✅ 已完成 | 2026-06-09 16:28 | 修仙页已独立功法分页，展示已学技能来源/属性 |
| 4.2 | 规划灵根信息界面 | ✅ 已完成 | 2026-06-09 16:28 | 修仙页已独立灵根分页，展示类型、属性、纯度、觉醒、倍率 |
| 5.1 | 技能槽 tooltip 数据结构预留 | ✅ 已完成 | 2026-06-09 16:30 | `ClientTechniqueData.TechniqueSummary` 读取内置 JSON，tooltip 竖向展示五项信息 |
| 5.2 | 技能点击/快捷键释放设计 | 📋 规划中 | - | 下一步应加入服务端释放包、冷却、真实消耗和校验 |

## 6. 当前停靠点

当前做到：

- `0.1.26` 已完成。
- 背包“修仙”页已修复玩家物品栏不显示 bug：现在继承 `InventoryScreen` 并保留原版槽位渲染。
- 背包“修仙”页已完成 旧第三方 UI 风格四分页：基础状态、灵根信息、功法信息、负面状态。
- 左侧技能栏已增加竖向 tooltip：技能名、所属功法、属性、消耗、是否可释放。
- 打坐吐纳 HUD 已独立显示在屏幕下方，显示当前吐纳效率、灵气浓度、功法倍率、灵根亲和和 5 秒结算进度条。
- 功法/灵根信息界面已独立为分页渲染方法，准备后续点击技能槽进入详情。
- 下一次任务建议从 **步骤 5.2：技能点击/快捷键释放设计** 或完整 旧第三方 UI 组件/Screen 化开始。

## 7. 构建记录

| 时间 | 命令 | 结果 | 备注 |
| --- | --- | --- | --- |
| 2026-06-09 | `./gradlew build` | ✅ 成功 | 生成 `build/libs/seeking_immortals-0.1.25.jar` |
| 2026-06-09 16:29 | `./gradlew build` | ❌ 失败 | `SyncCultivationDataPacket` 结尾括号缺失，已修复 |
| 2026-06-09 16:30 | `./gradlew build` | ✅ 成功 | 生成 `build/libs/seeking_immortals-0.1.26.jar` |
| 2026-06-09 17:19 | `./gradlew build` | ✅ 成功 | UI 显示复修后构建通过，产物仍为 `build/libs/seeking_immortals-0.1.26.jar` |
| 2026-06-10 23:59 | `./gradlew build` | ❌ 失败 | 独立 `Screen` 中误保留 `InventoryScreen#renderTooltip` 调用，已移除 |
| 2026-06-11 00:10 | `./gradlew build` | ✅ 成功 | 独立修仙面板构建通过，升版前产物仍为 `0.1.28.jar` |
| 2026-06-11 00:18 | `./gradlew build` | ✅ 成功 | 升版后构建通过，产物 `build/libs/seeking_immortals-0.1.29.jar` |
| 2026-06-11 03:20 | `./gradlew build` | ✅ 成功 | 0.1.31 修炼进度条/技能栏左移/左上角图案移除后构建通过，产物 `build/libs/seeking_immortals-0.1.31.jar` |

## 8. 阻塞与风险记录

| 时间 | 问题 | 状态 | 处理 |
| --- | --- | --- | --- |
| 2026-06-09 | `coding_agent` Provider 不可用 | ⚠️ 已绕过 | 由主调度器直接接管代码修改 |
| 2026-06-09 | `repo_agent` 上游附件错误 | ⚠️ 已绕过 | 由主调度器直接接管代码修改 |
| 2026-06-09 | 当前工作区不是 Git 仓库 | ⚠️ 持续注意 | 使用 `safe_edit` 自动备份，构建验证 |
| 2026-06-09 | 旧第三方 UI API 不应凭空假设 | ⚠️ 持续注意 | 当前只使用已验证可编译的 `原生矩形绘制` |
| 2026-06-09 16:29 | `SyncCultivationDataPacket` 构建括号错误 | ✅ 已解决 | 修复 `ClientCultivationData.Snapshot` 构造闭合括号后重新构建成功 |

---

**维护规则再次强调**：下一次任何任务开始前，必须先读取本文件；每完成一个小步，必须更新本文件。

## 9. 2026-06-09 18:23 UI/图形高优先级修复记录

| 步骤 | 状态 | 备注 |
| --- | --- | --- |
| 读取交接文档 | ✅ 已完成 | 已读取 `ai_handoff.md` 与 `step_progress.md` |
| 显式备份 | ✅ 已完成 | 已备份 `src`、`project_docs` 与 Gradle 配置到 `backups/20260609_181708_ui_fix_before/` |
| Git 状态检查 | ✅ 已完成 | 当前工作区不是 Git 仓库，继续用显式备份与工具备份保护修改 |
| P0/P1 UI 修复 | ✅ 已完成 | 已修复原版背包按钮自注入、客户端同步状态、技能顺序、HUD 稳定隐藏/占位、面板边界与皮肤颜色状态污染 |
| 构建验证 | ✅ 已完成 | `./gradlew build` 已通过；升版后将再次执行最终构建 |

| 版本文档同步 | ✅ 已完成 | 已更新 `items.md`、`pending_requests.md`、`features.md`、`missing_and_placeholders.md` 与 `updates/20260609_0.1.27.md` |

## 10. 2026-06-10 0.1.28 旧第三方 UI UI/图形修复记录

| 步骤 | 状态 | 备注 |
| --- | --- | --- |
| 读取交接文档 | ✅ 已完成 | 已读取 `ai_handoff.md` 与 `step_progress.md` |
| Git 状态检查 | ✅ 已完成 | `git rev-parse` 确认为非 Git 仓库；继续使用工具备份与既有 `backups/20260610_003025_旧第三方 UI库2_ui_bugfix_before/` |
| 旧第三方 UI 指南复查 | ✅ 已完成 | 按 Agent 指南重点复查 Screen/Menu、Overlay/HUD、布局、样式、事件生命周期与数据同步；本次不替用户决定最终样式 |
| 当前 UI bug 定位 | ✅ 已完成 | 0.1.27 后仍存在修仙页 `PANEL_WIDTH` 固定宽度残留，极窄屏/高 GUI Scale 时分页按钮、返回按钮和状态条可能越界 |
| 最小代码修复 | ✅ 已完成 | `CultivationStatsScreen` 新增自适应 `panelWidth()`，分页按钮与灵力/纯度状态条改为跟随面板宽度 |
| 构建验证 | ✅ 已完成 | `./gradlew build` 成功，产物 `build/libs/seeking_immortals-0.1.28.jar` |
| 文档同步 | ✅ 已完成 | 已同步 `ai_handoff.md`、`step_progress.md`、`items.md`、`pending_requests.md`、`features.md`、`missing_and_placeholders.md` 与 `updates/20260610_0.1.28.md` |

## 11. 2026-06-10 0.1.29 独立修仙面板与技能栏坐标修复记录

| 步骤 | 状态 | 备注 |
| --- | --- | --- |
| 读取交接文档 | ✅ 已完成 | 已读取 `ai_handoff.md` 与 `step_progress.md` |
| Git/备份检查 | ✅ 已完成 | `git rev-parse` 确认为非 Git 仓库；已创建显式备份 `backups/20260610_0129_independent_screen_before/`，代码修改使用 `safe_edit` 自动备份 |
| 技能栏堆叠排查 | ✅ 已完成 | `TechniqueSkillBarOverlay` 槽位循环使用 `slotY = y + i * (SLOT_SIZE + SLOT_GAP)`，未发现循环递增丢失；补充基于 HUD `screenHeight` 的顶部钳制，确保左侧 7 槽不被右上角/背包定位覆盖 |
| B 方案独立面板 | ✅ 已完成 | `CultivationStatsScreen` 改为独立 `Screen`，居中绘制大面板，保留基础/灵根/功法/负面状态四分页；原版背包仅保留入口按钮 |
| 返回逻辑 | ✅ 已完成 | 从背包按钮进入时返回原版 `InventoryScreen`，其他入口关闭页面 |
| 构建验证 | ✅ 已完成 | 首次构建因独立 `Screen` 无 `renderTooltip` 方法失败，移除该调用后 `./gradlew build` 成功；升版后已再次构建验证 |
| 文档同步 | ✅ 已完成 | 已同步 `ai_handoff.md`、`step_progress.md`、`items.md`、`pending_requests.md`、`features.md`、`missing_and_placeholders.md` 与 `updates/20260610_0.1.29.md` |


## 12. 2026-06-11 0.1.30 左侧技能栏参考图外框接入记录

| 步骤 | 状态 | 备注 |
| --- | --- | --- |
| 读取交接文档 | ✅ 已完成 | 已读取 `ai_handoff.md` 与 `step_progress.md` |
| Git/备份检查 | ✅ 已完成 | `git rev-parse` 确认为非 Git 仓库；已创建显式备份 `backups/20260611_0130_skill_bar_frame_before/`，代码修改使用 `safe_edit` 自动备份 |
| 资源处理 | ✅ 已完成 | 用户 JPG 已自动裁剪主体、尝试透明化棋盘格并缩放为 `textures/gui/skill_bar_frame.png`；源图为 JPG，边缘透明可能不如正式 PNG 完美 |
| 技能栏接入 | ✅ 已完成 | `TechniqueSkillBarOverlay` 新增外框纹理绘制，7 槽按外框尺寸居中排列在内部开口区域，未同步仍显示空槽 |
| 构建验证 | ✅ 已完成 | 接入纹理后 `./gradlew build` 成功；升版后已再次构建验证 |
| 文档同步 | ✅ 已完成 | 已同步 `ai_handoff.md`、`step_progress.md`、`items.md`、`pending_requests.md`、`features.md`、`missing_and_placeholders.md` 与 `updates/20260611_0.1.30.md` |


## 13. 2026-06-11 0.1.31 修炼进度条与左上角图案处理记录

| 步骤 | 状态 | 备注 |
| --- | --- | --- |
| 读取交接文档 | ✅ 已完成 | 已读取 `ai_handoff.md` 与 `step_progress.md`，并补读物品/待办/功能/占位文档 |
| Git/备份检查 | ✅ 已完成 | `git rev-parse` 确认为非 Git 仓库；已创建显式备份 `backups/20260611_0131_progress_bar_before/`，代码修改使用 `safe_edit` 自动备份 |
| HUD/Overlay 定位 | ✅ 已完成 | 仅发现本模组客户端注册 `technique_skill_bar` 与 `breathing_hud` 两个 Overlay；左上角图案来源确认为 0.1.30 技能栏外框 `skill_bar_frame.png` 的整张装饰绘制 |
| 资源处理 | ✅ 已完成 | 用户 JPG 裁剪第三条完整进度条，尝试以边缘泛洪方式透明化棋盘格，输出 `textures/gui/cultivation_progress_bar.png`；JPG 抗锯齿边缘可能仍有少量浅色残留 |
| 代码接入 | ✅ 已完成 | `BreathingHudOverlay` 的 5 秒结算进度改用新修炼进度条纹理；技能槽左移，且不再绘制导致左上角图案的旧外框纹理 |
| 构建验证 | ✅ 已完成 | `./gradlew build` 成功，产物 `build/libs/seeking_immortals-0.1.31.jar` |
| 文档同步 | ✅ 已完成 | 已同步 `ai_handoff.md`、`items.md`、`pending_requests.md`、`features.md`、`missing_and_placeholders.md` 与 `updates/20260611_0.1.31.md` |


## 14. 2026-06-11 0.1.32 移除 旧第三方 UI 库 并重实现原生 UI 记录

| 步骤 | 状态 | 备注 |
| --- | --- | --- |
| 读取交接文档 | ✅ 已完成 | 已读取 `ai_handoff.md` 与 `step_progress.md` |
| Git/备份检查 | ✅ 已完成 | 当前工作区不是 Git 仓库；已创建显式备份 `backups/20260611_0333_remove_旧第三方 UI库_before/` |
| 清理与重实现 | ⚡ 进行中 | 正在移除 旧第三方 UI 库 依赖、兼容命名与文档表述，改为原生 Forge/Minecraft UI |

## 15. 2026-06-11 0.1.33 技能快捷键与单页修仙面板记录

| 步骤 | 状态 | 备注 |
| --- | --- | --- |
| 读取交接文档 | ✅ 已完成 | 已读取 `ai_handoff.md` 与 `step_progress.md` |
| Git/备份检查 | ✅ 已完成 | 当前工作区不是 Git 仓库；已创建显式备份 `backups/20260611_0133_skill_keys_before/` |
| 全局搜索 | ✅ 已完成 | 已搜索 `CultivationStatsScreen`、`ClientEvents`、按键注册、网络包、打坐吐纳提示与技能数据/释放相关逻辑 |
| 实现修改 | ⚡ 进行中 | 正在合并修仙面板四分页、抑制打坐吐纳反复动作栏提示、添加 7 个技能释放键和技能编辑界面入口 |

| 实现修改 | ✅ 已完成 | 修仙面板四分页合并为单页分区；抑制打坐吐纳 5 秒动作栏状态提示；新增 7 个释放键、技能编辑界面和服务端释放包 |
| 构建验证 | ✅ 已完成 | `./gradlew build` 成功，升版前产物验证通过；升版后已再次执行最终构建 |

## 16. 0.1.33 构建记录补充

| 时间 | 命令 | 结果 | 备注 |
| --- | --- | --- | --- |
| 2026-06-11 12:55 | `./gradlew build` | ✅ 成功 | 技能快捷键、单页修仙面板、服务端释放包初版构建通过 |


## 17. 2026-06-11 0.1.34 技能绑定/冷却/HUD 修复记录

| 步骤 | 状态 | 备注 |
| --- | --- | --- |
| 读取交接文档 | ✅ 已完成 | 已读取 `ai_handoff.md` 与 `step_progress.md`，并补读物品/待办/功能/占位文档 |
| Git/备份检查 | ✅ 已完成 | 当前工作区不是 Git 仓库；已创建显式备份 `backups/20260611_1530_skill_binding_cooldown_before/` |
| 相关代码搜索 | ✅ 已完成 | 已检查 `ClientEvents`、`TechniqueEditScreen`、`ClientTechniqueData`、`ReleaseTechniquePacket`、`ModNetwork`、`PlayerCultivation`、同步包、HUD 与语言文件 |
| 实现修改 | ✅ 已完成 | 已实现 7 槽持久化、旧存档默认填槽、保存/清空绑定包、绑定槽同步、快捷键按绑定槽释放、5 秒默认冷却与 tooltip 冷却秒数；按本阶段要求未做 UI 拖拽/HUD 位置 |
| 构建验证 | ✅ 已完成 | 首次构建因 `ModEvents` 仍使用旧同步包构造失败，已改为 `SyncLearnedTechniquesPacket.send`；随后 `./gradlew build` 成功，产物 `build/libs/seeking_immortals-0.1.34.jar` |
| 文档同步 | ✅ 已完成 | 已同步 `ai_handoff.md`、`items.md`、`pending_requests.md`、`features.md`、`missing_and_placeholders.md` 与 `updates/20260611_0.1.34.md` |

## 18. 2026-06-11 0.1.34 第二阶段 UI 拖拽/HUD 修复记录

| 步骤 | 状态 | 备注 |
| --- | --- | --- |
| 读取交接文档 | ✅ 已完成 | 已读取 `ai_handoff.md` 与 `step_progress.md`，并补读待办、功能、占位和 0.1.34 更新文档 |
| Git/备份检查 | ✅ 已完成 | 当前工作区不是 Git 仓库；已创建显式备份 `backups/20260611_0134_ui_drag_hud_before/` |
| 技能编辑拖拽 | ✅ 已完成 | `TechniqueEditScreen` 显示 7 槽与已学技能列表，支持拖拽技能到槽位绑定、右键清空，并保留左键槽位绑定同序号已学技能 |
| 快捷键冲突优化 | ✅ 已完成 | 技能释放键和技能编辑键默认改为未绑定；Screen 打开时清理点击，不触发释放或重复打开编辑界面；打坐键保留 V |
| HUD/进度条 | ✅ 已完成 | 打坐吐纳 HUD 上移到 `screenHeight - HEIGHT - 72` 并做上下钳制；进度条先画背景/框，再按 5 秒周期裁剪左侧填充 |
| 构建验证 | ✅ 已完成 | `./gradlew build` 已通过，产物仍为 `build/libs/seeking_immortals-0.1.34.jar` |
| 文档同步 | ✅ 已完成 | 已同步 `step_progress.md`、`pending_requests.md`、`features.md`、`missing_and_placeholders.md` 与 `updates/20260611_0.1.34.md`；版本保持 0.1.34 |

## 19. 0.1.34 构建记录补充

| 时间 | 命令 | 结果 | 备注 |
| --- | --- | --- | --- |
| 2026-06-11 13:27 | `./gradlew build` | ❌ 失败 | `ModEvents` 仍调用旧版 `new SyncLearnedTechniquesPacket(List)`，已修复为统一 send 方法 |
| 2026-06-11 13:28 | `./gradlew build` | ✅ 成功 | 技能槽持久化、绑定同步与 5 秒冷却构建通过，产物 `build/libs/seeking_immortals-0.1.34.jar` |
| 2026-06-11 13:31 | `./gradlew build` | ✅ 成功 | 文档同步后最终复验通过，产物 `build/libs/seeking_immortals-0.1.34.jar` |
| 2026-06-11 13:35 | `./gradlew build` | ✅ 成功 | 第二阶段技能拖拽、快捷键默认未绑定、HUD 上移和进度条裁剪后构建通过 |
| 2026-06-11 13:36 | `./gradlew build` | ✅ 成功 | 修正已学列表 hover 负坐标误判后复验通过，产物 `build/libs/seeking_immortals-0.1.34.jar` |

## 20. 2026-06-11 0.1.34 第三阶段进度条资源/HUD/缩放修复记录

| 步骤 | 状态 | 备注 |
| --- | --- | --- |
| 读取交接文档 | ✅ 已完成 | 已读取 `ai_handoff.md` 与 `step_progress.md` |
| Git/备份检查 | ✅ 已完成 | 当前工作区不是 Git 仓库；已创建显式备份 `backups/20260611_0134_progress_scale_before/` |
| 资源处理 | ✅ 已完成 | 使用用户新上传 PNG 裁剪整条进度条基底，透明化上传预览背景，覆盖 `textures/gui/cultivation_progress_bar.png`，保留 PNG 透明通道 |
| HUD 调整 | ✅ 已完成 | 移除 HUD “5秒结算进度”文字行；HUD 高度降到 46，放在血条上方安全距离并按 scaledWidth/scaledHeight 钳制 |
| 进度条实现 | ✅ 已完成 | 先绘制新基底/外框，再按 progress 从左到右绘制青绿色半透明填充矩形和高光，不再整张满格显示 |
| 修仙属性面板 | ✅ 已完成 | 去掉“原生UI · 独立修仙面板 · 单页总览”文案，保留简洁“修仙属性”标题；小窗口时可隐藏标题腾空间 |
| 缩放兼容 | ✅ 已完成 | 修仙面板最小宽高、按钮、文字截断、分区数量和底部边界改为按当前 GUI Scale/窗口尺寸自适应；HUD 按屏幕边界钳制 |
| 构建验证 | ✅ 已完成 | `./gradlew build` 成功，产物仍为 `build/libs/seeking_immortals-0.1.34.jar` |

## 21. 0.1.34 构建记录补充（二）

| 时间 | 命令 | 结果 | 备注 |
| --- | --- | --- | --- |
| 2026-06-11 18:05 | `./gradlew build` | ✅ 成功 | 新 PNG 进度条基底、HUD 降高/文案移除、修仙属性面板去总览文案与缩放兼容修复后构建通过 |
| 2026-06-13 02:12 | `./gradlew build` | ✅ 成功 | 0.1.47 飞剑/法宝飞行系统初版构建通过 |
| 2026-06-11 18:09 | `./gradlew build` | ✅ 成功 | HUD 文本截断与极窄 scaledWidth 防护后复验通过 |
| 2026-06-13 04:22 | `./gradlew build` | ✅ 成功 | 1.1 六大核心属性存储、展示、指令与文档同步后构建通过，产物 `build/libs/seeking_immortals-0.1.47.jar` |
| 2026-06-13 12:38 | `./gradlew build` | ✅ 成功 | 手动突破流程、破境丹/药力资源校验、失败走火风险和按键/GUI/命令入口构建通过，产物 `build/libs/seeking_immortals-0.1.47.jar` |
| 2026-06-13 13:12 | `./gradlew build` | ✅ 成功 | 突破成功率丹药/灵眼/功法/执念加成、同步展示与协议版本 4 构建通过，产物 `build/libs/seeking_immortals-0.1.47.jar` |
| 2026-06-13 | `./gradlew build` | ✅ 成功 | 打坐退出交互失效与穿方块修复后构建通过，产物 `build/libs/seeking_immortals-0.1.47.jar` |

## 22. 2026-06-13 0.1.47 六大核心属性记录

| 步骤 | 状态 | 备注 |
| --- | --- | --- |
| 读取交接文档 | ✅ 已完成 | 已读取 `ai_handoff.md` 与 `step_progress.md`，并补读 `items.md`、`pending_requests.md`、`features.md`、`missing_and_placeholders.md` |
| 安全计划与备份 | ✅ 已完成 | 当前工作区不是 Git 仓库；已创建显式备份 `.bak/20260613_042140_core_attributes/` |
| 核心属性存储 | ✅ 已完成 | `PlayerCultivation` 统一保存 `cultivation`、`mana`/`manaMax`、`divSense`、`bodyRef`、`qiDevRisk`、`tribRes`；新存档神识默认 5，旧存档缺字段时回退默认值 |
| 展示与指令 | ✅ 已完成 | 修仙属性面板和 `/seeking_immortals qi` 已显示六大核心属性；同步包沿用既有字段顺序，未新增 packet 字段 |
| 文档同步 | ✅ 已完成 | 已同步 `features.md`、`pending_requests.md`、`missing_and_placeholders.md` 与 `updates/20260613_0.1.47.md` |
| 构建验证 | ✅ 已完成 | `./gradlew build` 成功，产物 `build/libs/seeking_immortals-0.1.47.jar` |

## 23. 2026-06-13 0.1.47 手动境界突破流程记录

| 步骤 | 状态 | 备注 |
| --- | --- | --- |
| 读取交接文档 | ✅ 已完成 | 已读取 `ai_handoff.md`、`step_progress.md`，并补读物品/待办/功能/占位与 0.1.47 更新文档 |
| 安全计划与备份 | ✅ 已完成 | 已创建显式备份 `.bak/20260613_123641_breakthrough_flow/` |
| 修为瓶颈逻辑 | ✅ 已完成 | 普通修炼现在只累积到当前层/阶段上限，不再自动跨层；旧存档优先恢复保存的 `Realm`/`Stage` |
| 突破结算 | ✅ 已完成 | 成功显式推进一阶并清空新阶段进度；失败回退当前阶段 20% 修为、走火风险 +10%，70% 以上进行心魔检定 |
| 构建验证 | ✅ 已完成 | `./gradlew build` 成功，产物 `build/libs/seeking_immortals-0.1.47.jar` |

## 24. 2026-06-13 0.1.47 突破成功率加成记录

| 步骤 | 状态 | 备注 |
| --- | --- | --- |
| 读取与规划 | ✅ 已完成 | 已基于手动突破流程继续规划成功率加成，并读取突破、丹药、灵气、功法数据和文档相关文件 |
| 安全备份 | ✅ 已完成 | 已创建显式备份 `.bak/20260613_131101_breakthrough_chance/` |
| 丹药加成 | ✅ 已完成 | 普通破境丹/下品筑基丹为 +5%，`PillQuality` 预留中品/上品/极品 +10%/+15%/+20%，药力数值持久化到 `BreakthroughPillBonus` |
| 环境与功法加成 | ✅ 已完成 | 灵脉/灵眼复用 `AuraInfo.leyline()` 提供 +15%；功法品质支持可选 `quality` 字段并用 source/id 启发式兜底，最高 +10% |
| 执念加成 | ✅ 已完成 | 连续失败改为每次 +5%，最多 +30%，成功后清零 |
| 同步与展示 | ✅ 已完成 | 同步突破概率和丹药/灵眼/功法/执念加成；修仙面板、`/realm` 和突破反馈使用同一预览结果 |
| 文档同步 | ✅ 已完成 | 已同步 `features.md`、`pending_requests.md`、`missing_and_placeholders.md` 与 `updates/20260613_0.1.47.md` |
| 构建验证 | ✅ 已完成 | `./gradlew build` 成功，产物 `build/libs/seeking_immortals-0.1.47.jar` |

## 26. 2026-06-13 0.1.47 走火入魔机制（MVP）实现记录

| 步骤 | 状态 | 备注 |
| --- | --- | --- |
| 读取与规划 | ✅ 已完成 | 已读取 `PlayerCultivation.java`、`ModEvents.java`、`ReleaseTechniquePacket.java`、`BreakthroughService.java`、`BasePillItem.java`、`ModItems.java`、`PillType.java`、`SpiritualAuraManager.java`、`ModCreativeTabs.java` 与 `zh_cn.json` |
| 安全备份 | ✅ 已完成 | 已创建显式备份 `.bak/20260613_qi_deviation/` |
| 分级效果枚举 | ✅ 已完成 | `PlayerCultivation` 新增 `QiDeviationTier`（NONE/MINOR/MODERATE/SEVERE/EXTREME）和 `determineQiDeviationTier()`；`BreakthroughAttemptResult` 新增 `qiDeviationTier` 字段 |
| 走火判定重构 | ✅ 已完成 | `checkQiDeviation` 改为纯概率判定（不再直接施加心魔）；`BreakthroughService.applyQiDeviationEffect` 按分级执行效果 |
| 分级效果实现 | ✅ 已完成 | 轻微（-30% 修为）、中度（-50% 修为+昏迷 30 秒）、严重（掉境界+昏迷 3 分钟+装备损坏）、极端（当场死亡+背包掉落 50%） |
| 受伤修炼风险 | ✅ 已完成 | `ModEvents` 每秒检测：打坐中血量低于最大值时 +2% 走火风险 |
| 风险自然衰减 | ✅ 已完成 | 平稳打坐每 720 秒 -1%（≈每小时 -5%）；灵脉打坐额外每 360 秒 -1%（≈每小时 -10%） |
| 功法境界校验 | ✅ 已完成 | `ReleaseTechniquePacket` 新增 `estimateTechniqueRealm`；功法超出当前境界 2 级以上时 +5% 走火风险 |
| 稳神丹 | ✅ 已完成 | 新增 `CalmingPill`（继承 `BasePillItem`），服用后 -20% 走火风险；`PillType.CALMING` 已注册 |
| 物品注册 | ✅ 已完成 | `ModItems.CALMING_PILL_LOW` 已注册，已加入创造栏和语言文件 |
| 构建验证 | ✅ 已完成 | `./gradlew build` 成功，产物 `build/libs/seeking_immortals-0.1.47.jar` |


## 25. 2026-06-13 0.1.47 打坐退出交互失效与穿方块修复记录

| 步骤 | 状态 | 备注 |
| --- | --- | --- |
| Bug 分析 | ✅ 已完成 | 客户端按移动键退出打坐时直接调用 `player.stopRiding()` 导致客户端/服务端状态不同步（交互失效）；服务端 `stopMeditation` 下马后未重新定位玩家导致脚陷入蒲团碰撞箱（穿方块） |
| 客户端修复 | ✅ 已完成 | `ClientEvents.onKeyInput` 移除客户端 `stopRiding()` 调用，仅发送 `SetMeditatingPacket(false)` 让服务端统一处理下马 |
| 服务端修复 | ✅ 已完成 | `ModEvents.stopMeditation()` 与 `SetMeditatingPacket.handle()` 在下马后显式 `setPos` 到蒲团顶部（Y + 6/16），避免穿方块 |
| 构建验证 | ✅ 已完成 | `./gradlew build` 成功，产物 `build/libs/seeking_immortals-0.1.47.jar` |

## 27. 2026-06-14 丹药系统适配灵根重构记录

| 步骤 | 状态 | 备注 |
| --- | --- | --- |
| 读取交接文档 | ✅ 已完成 | 已读取 `ai_handoff.md` 与 `step_progress.md` |
| CultivationPillItem 吸收率 | ✅ 已完成 | `addCultivationExp` 改为 `adjustedExp = expValue * getPillAbsorptionMultiplier()`，提示显示实际获得值 |
| QiRecoveryPillItem 吸收率 | ✅ 已完成 | `addQi` 改为 `adjustedAmount = qiValue * getPillAbsorptionMultiplier()`，提示显示实际获得值 |
| BasePillItem 便捷方法 | ✅ 已完成 | 新增 `getPillAbsorptionMultiplier(ServerPlayer)` 供子类使用 |
| RejuvenationPill 吸收率 | ✅ 已完成 | 灵力和修为增益都乘以丹药吸收率 |
| HealingPill 分析 | ✅ 已完成 | 恢复原版生命值（非灵力/修为），不需吸收率 |
| CalmingPill/FastingPill/ClearSpiritPowder | ✅ 已完成 | 功能性丹药，不需吸收率 |
| LingGenTestStoneItem 更新 | ✅ 已完成 | 显示修炼速度、灵力回复、突破倍率、灵根突破加成；低资质灵根额外显示丹药吸收率和青玉小瓶获取率 |
| SeekingImmortalsCommand 更新 | ✅ 已完成 | `/seeking_immortals root` 显示灵根分类名、修炼速度、灵力回复、突破加成、丹药吸收、体质 |
| SyncCultivationDataPacket 检查 | ✅ 已完成 | 已使用 displayName 字符串同步，无需修改；`PlayerCultivation` 已使用 `fromName()` |
| 语言文件更新 | ✅ 已完成 | zh_cn/en_us 均添加灵根分类翻译 key、丹药吸收率和青玉小瓶获取率 tooltip |
| 全局搜索旧枚举名 | ✅ 已完成 | 无 `SpiritualRoot.PSEUDO` 或 `SpiritualRoot.FIVE_ELEMENTS` 引用，无 `.purity()` 调用 |
| 构建验证 | ✅ 已完成 | `./gradlew build` 成功，产物 `build/libs/seeking_immortals-0.1.47.jar` |

## 28. 2026-06-18 Phase 1 核心属性与境界系统修复记录

| 步骤 | 状态 | 备注 |
| --- | --- | --- |
| 读取阶段文档 | ✅ 已完成 | 已读取 `docs/task-board.md`、`docs/phase-0-3-audit-report.md`、`docs/mvp-scope.md`、`docs/implementation-roadmap.md`、`docs/existing-implementation.md` |
| 安全备份 | ✅ 已完成 | 已备份 Phase 1 相关文件到 `.bak/20260618_135342_phase_1_repair/` |
| 核心属性兼容 | ✅ 已完成 | 保留 `cultivationExp`、`spiritualPower`、`divineConsciousness` 等内部字段，补齐设计语义 getter/setter 与 NBT 兼容 |
| 境界映射 | ✅ 已完成 | `Realm`/`RealmStage` 新增 `getDesignId()` / `getDesignKey()`，映射 `QI_1`~`QI_13` 与筑基四阶设计名 |
| 单元测试 | ✅ 已完成 | 新增 `Phase1CultivationSystemTest`，覆盖基准表、衍生属性、核心属性读写和旧 NBT 迁移 |
| 构建验证 | ✅ 已完成 | `./gradlew.bat test` 与 `./gradlew.bat build` 均成功；未进入 Phase 2 实现 |

## 29. 2026-06-18 Phase 2 Realm System 审计记录

| 步骤 | 状态 | 备注 |
| --- | --- | --- |
| 读取任务与边界 | ✅ 已完成 | 已读取 `docs/task-board.md`、`project_docs/ai_handoff.md`、`project_docs/step_progress.md`；按用户最新要求仅处理 Phase 2：Realm System |
| 安全备份 | ✅ 已完成 | 已备份 `docs/task-board.md` 与 `project_docs/step_progress.md` 到 `.bak/20260618_223913_phase_2_realm_docs/` |
| 代码证据审计 | ✅ 已完成 | `RealmStage`、`RealmStageConfig`、`PlayerCultivation`、`BreakthroughService` 已覆盖本次 Realm System 清单；未修改 Java 代码 |
| 报告生成 | ✅ 已完成 | 已生成 `docs/phase-2-report.md` |
| 任务板更新 | ✅ 已完成 | 已在 `docs/task-board.md` 增加本次 Realm System 子范围记录；因 build 失败，相关条目保持 `[ ] Unknown` |
| 构建验证 | ⚠️ 阻塞 | `./gradlew.bat build` 先因沙箱网络受限无法下载 Gradle，授权后进入编译但 `:compileJava` 失败；阻塞点为既有 Java 编译错误，未按本任务修复 |
