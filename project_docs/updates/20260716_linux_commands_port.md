# 20260716 Linux 命令与脚本移植

## 变更类别

build/scripts + docs（可发布构建逻辑变更，升 `mod_version`）。

## 背景

当前工作环境为 Linux（WSL：`/root/mc-mod`）。仓库内大量可执行命令与路径仍写 Windows 形态（`gradlew.bat`、`powershell`、`scripts/preflight.ps1`、硬编码 `D:/codex/mc-mod`），在无 `pwsh` 的 Linux 上 `./gradlew build` 的 AI preflight 门禁会直接失败。

## 更新内容

1. **preflight 移植**：新增 `scripts/preflight.sh`（bash，行为对齐 `preflight.ps1`：git porcelain 可发布路径检测、`mod_version` 0.1.X 格式、fingerprint 回退、`--skip-version-bump-check` / `--record-state-only`）。`preflight.ps1` 保留作 Windows 回退，文首注明 Linux 等价脚本。
2. **`build.gradle`**：`aiPreflight` 与 `build.doLast` 按 OS 选择：
   - Windows → `powershell` + `scripts/preflight.ps1`
   - 非 Windows → `bash` + `scripts/preflight.sh`（不再依赖 `pwsh`）
3. **根目录辅助脚本**：新增 `set_java_home.sh` / `check_java.sh` / `check_build.sh` / `test_env.sh`；对应 `.bat` 保留并在文首加 `REM Linux equivalent`。
4. **Python 审计脚本**：11 个 `scripts/audit_*.py` 中 `Path(r"D:/codex/mc-mod")` → `Path(__file__).resolve().parents[1]`。
5. **文档批量替换**（约 430+ md）：
   - `.\gradlew.bat` / `gradlew.bat` → `./gradlew`
   - ` ```powershell ` → ` ```bash `
   - `D:/codex/mc-mod` / `D:\codex\mc-mod` → `/root/mc-mod`
   - 指令性 `scripts/preflight.ps1` → `scripts/preflight.sh`
   - 覆盖 `CLAUDE.md` / `AGENTS.md` / `MAINTENANCE.md` / `project_docs/**` / `docs/**` / 任务简报
6. **Zone.Identifier 清理**：Windows ADS 残留（`*:Zone.Identifier`，曾随 `5a181a6a` 入库）导致 Linux 上 `processResources` 失败（`Cannot convert URL 'pack.mcmeta:Zone.Identifier' to a file`）。从活跃树删除并 `git update-index --force-remove` 982 条；`.gitignore` 追加 `*:Zone.Identifier`。

## 版本与协议

- `mod_version`：`0.1.504` → `0.1.505`（`scripts/` + `build.gradle` 可发布变更）。
- `ModNetwork.PROTOCOL_VERSION`：保持 `19`（无网络包变更）。

## 备份

`.bak/20260716_linux_commands/`（preflight.ps1、build.gradle、bat、关键 workflow 文档、audit py 等）。

## 验证

- `bash scripts/preflight.sh` → `AI preflight passed: mod_version=0.1.505`
- `./gradlew --no-daemon aiPreflight` → BUILD SUCCESSFUL
- `./gradlew --no-daemon --max-workers=1 build`：`aiPreflight`/`compileJava`/`processResources`/`jar` 通过；`:test` 383 中 2 失败为既有 M00 语料基线阻塞（`SettingCatalogSummaryServiceTest`、`JsonSanityTest#textMaterialIndexesAreCoherent`：747 vs 346 / `body.json.json`），与本次 Linux 命令移植无关。
- 活跃 md/sh/py/gradle 中可执行形态的 `gradlew.bat` / ` ```powershell ` / `D:/codex` 残留为 0（`.bat` 本体内 Windows 命令有意保留；历史说明文字可出现旧路径）。
