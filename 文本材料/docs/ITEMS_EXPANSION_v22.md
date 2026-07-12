# 物品扩充 v22（2026-07-03）

> Lang 生成、spawn/beast 校验、古宝/灵宝模板、缺失妖兽补全。

## 新增脚本

| 脚本 | 作用 |
|------|------|
| `generate_lang_from_registry.py` | `forge_registry/lang/zh_cn.json`、`en_us.json`（**218** keys） |
| `validate_spawn_beast_ids.py` | spawn 中 `beast_id` 必须在 `beast_bestiary` |
| `add_spawn_missing_beasts.py` | 补 **泣魂兽**、**幻焰蛾** |

## 数据

- **artifacts** +2：九龙神火罩（仿·古宝 tier10）、炼虚钟（灵宝 tier11），带 `compliance` 模板标记
- **beast_bestiary** **32** 种（+2）
- **forge_registry** **218** 物品；worldpack **191** 文件

## 测试

- `tests/test_spawn_beast_ids.py`（CI 可跑 `validate_spawn_beast_ids.py`）

## 汇编 §11.1

- 邪幻镜/玄黄镜/混元钵已在前期版本；本版补 **古宝/灵宝结构占位**，非原著命名绑定。

## 命令

```bash
python3 scripts/add_spawn_missing_beasts.py
python3 scripts/validate_spawn_beast_ids.py
python3 scripts/generate_forge_item_registry.py
python3 scripts/generate_lang_from_registry.py
python3 scripts/pack_world.py
pytest tests/test_spawn_beast_ids.py tests/test_forge_registry.py -q
```

## v23 候选

- `en_us` 机器翻译占位 → 拼音/英文 id 名
- `refinement_recipes` 挂新古宝残片链
- Patchouli `items_generated` 与 lang 合并