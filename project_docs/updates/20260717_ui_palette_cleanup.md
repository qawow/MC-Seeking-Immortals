# 2026-07-17 ImmortalUiSkin 遗留色板与散落 hex 清理

## 变更分类
代码重构（client 色板）。任务红线 **不修改** `mod_version` / `PROTOCOL_VERSION`。

## Legacy 色板引用审计

### 4 个未迁移 legacy 屏幕
`ShopScreen` / `AuctionScreen` / `SectScreen` / `StorageBraceletScreen`
**全部只引用 `JOURNAL_*`**，不依赖 `PANEL_*` / `COLOR_*` / `SKILL_*` / `STATUS_BAR_*` / `TOOLTIP_*`。
因此这 4 个屏幕 **不阻塞** legacy 常量删除。

### 外部仍在用的旧常量（保留）
| 常量 | 引用方 |
|---|---|
| `COLOR_TEXT_MUTED` / `COLOR_TEXT_NORMAL` | `TechniqueSkillBarOverlay`、`BreathingHudOverlay` |
| `PANEL_*` / `SKILL_*` / `STATUS_BAR_*` / `HEALTH_BAR_*` / `ABSORPTION_BAR_*` / `TOOLTIP_*` | `ImmortalUiSkin` 自身绘制 helper（`drawPanel`/`drawSkillSlot`/`drawStatusBar`/`drawHealthBar`/`drawTooltipPanel` 等）仍在被 HUD/槽位调用 |

### 已删除（确认零外部引用）
- `COLOR_TITLE`
- `COLOR_TEXT_SUCCESS`
- `COLOR_TEXT_DANGER`
- `COLOR_TEXT_BLUE`
- `COLOR_HOVER_BG`

## 新增 journal/HUD 具名常量 + 引用替换
| 原裸 hex | 新常量 | 替换点 |
|---|---|---|
| `0xFF532823` | `JOURNAL_ICON_INSET` | `CultivationStatsScreen` |
| `0xFF582A24` | `JOURNAL_SEAL_INSET` | `CultivationStatsScreen` |
| `0xFF3B493C` | `JOURNAL_NODE_EMPTY` | `MethodTreeScreen` |
| `0xFF5B5646` | `JOURNAL_NODE_LOCKED` | `MethodTreeScreen` |
| `0x88000000`（禁用遮罩） | 复用 `JOURNAL_SHADOW` | `TechniqueEditScreen` |
| `0x66120D0A` | `HUD_SKILL_DISABLED_OVERLAY` | `TechniqueSkillBarOverlay` |
| `0xCC4E1712` | `HUD_COOLDOWN_OVERLAY` | `TechniqueSkillBarOverlay` |
| 占位图标 seed 合成 | `skillPlaceholderColor()` + `HUD_SKILL_PLACEHOLDER_*` | `TechniqueSkillBarOverlay` |
| `0x00000000` | `JOURNAL_TRANSPARENT` | `BestiaryScreen` / `ChronicleScreen` |
| `ImmortalUiSkin` 内部 | `JOURNAL_DIVIDER_GLOW` / `JOURNAL_SCROLLBAR_TRACK` / `JOURNAL_CULTIVATION_FILL` / `JOURNAL_CULTIVATION_HIGHLIGHT` | 皮肤自身 |

## 明确未动
- JEI / compat 包硬编码颜色
- 4 个 legacy 屏幕源文件
- 非 UI 业务色（`GoldCoreGrade` / `MaterialRarity` / status catalog 等）

## 验证
`bash ./gradlew build --no-daemon -PaiSkipVersionBumpCheck=true` BUILD SUCCESSFUL
client 包（`ImmortalUiSkin` 常量定义除外）裸 hex = 0

## 版本
- mod_version 0.2.2
- protocol 24
