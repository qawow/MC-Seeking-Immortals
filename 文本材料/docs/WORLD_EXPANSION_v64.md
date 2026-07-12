# 全模块继续扩充 v64

在功法/灵兽/物品等既有管线之外，对**世界玩法层**做一轮统一扩充（结构优先，符合汇编 §9/§13/§16）。

## 扩充范围

| 模块 | 新增/补全 |
|------|-----------|
| `secret_realms.json` | +6 秘境（蛮荒遗迹、天澜、深渊裂隙、灵缈园残片、阴罗冢、七脉试炼洞）；**全秘境** `setting` + `learn_requirements.entry` |
| `daily_random_events.json` | +8 日常（灵雨、魔探、拍卖传闻、海兽迁徙等）+ `setting` |
| `quest_hooks.json` | +10 任务钩（七派试炼、落云丹堂、星宫登记、虚天钥等）+ `setting` / `learn_requirements.accept` |
| `sects.json` | 七派/乱星海/千竹等 **详述 patch**；其余宗门默认 `setting` + `learn_requirements.join` |
| `constitution_catalog.json` | +4 体质 + 全条 `setting` / `learn_requirements` |
| `spirit_herbs_catalog.json` | +5 灵草 + 全条 `setting` / `gather` 限制 |
| `npc_dialogue_templates.json` | +5 对话原型 |
| `economy_contribution_exchange.json` | +2 贡献兑换物（若有 items 表） |

## 种子文件

`data/novel_world_expansion_waves_v64.json`

## 脚本

```bash
python3 scripts/expand_world_modules_v64.py
```

## 打包

`seeking_immortals_world_v64.zip`

## 后续建议

- 炼丹/炼器/符箓：可对 `alchemy_recipes` / `refinement_recipes` / `talisman_recipes` 同样加 `learn_requirements`（宗门秘传、丹炉品阶）  
- 天劫：`tribulation_rules.json` 与境界突破联动  
- 再跑一轮 **FULL_pack** 合并全部 data