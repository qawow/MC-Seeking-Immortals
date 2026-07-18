# 2026-07-19 飞升备份复制漏洞收口（彻底方案）

## 范围
- 分支：`task/m13-dimensions-ascension`（当前 worktree）
- 红线：不升 `mod_version`（保持 0.2.17）；协议不变；不处理 immortal_realm/deferred 维度收口
- 不新增任何正式物资取回渠道

## 问题
飞升成功后仍保留 `seeking_immortals_ascension_backup` 快照，玩家可用
`/seeking_immortals catalog ascension restore` 把唯一物/装备再注入一次，造成双份。

## 修复
1. **成功即清备份**：`attemptAscension` 在 `teleported=true` 后立刻 `clearBackup`，
   移除整个 `seeking_immortals_ascension_backup` PersistentData 根。
2. **失败回滚不动**：`teleported=false` 仍走系统 `restoreBackup`。
3. **玩家 restore 下线**：命令 `catalog ascension restore` 改为 `requires(permission 2)`，
   帮助输出标明 admin diagnostic，非玩家取回路径。成功后 restore → `no_backup`。
4. **文案**：去掉“可用 restore 取回 / 备份箱”暗示；说明成功后重置物资不保留给玩家。
5. **再飞升门控**：只看 `FLAG_ASCENDED`，不再要求当前维度含 `tianyuan`。
6. **起步礼幂等**：`FLAG_STARTER_GRANTED` 门控 `alliance_merit_token` / `spirit_stone_shard`。

## 验证
- 单测：`M13DimensionsAscensionTest` 增成功清备份 / 回滚一次性 / 再飞升门控 / 起步礼幂等
- 构建：`./gradlew build --no-daemon -PaiSkipVersionBumpCheck=true`（任务红线不 bump 版本）

## 复现步骤复核（逻辑走读）
1. 玩家持唯一物+装备，满足飞升条件并 confirm
2. 系统 backup → loadout reset（唯一物/装备保留在身）→ 传送成功
3. **旧**：backup 仍 hasBackup=true；restore 再注入 → 双份
4. **新**：成功后 clearBackup；hasBackup=false；restore → no_backup；唯一物/装备仅一份
5. 传送失败路径仍 restore 回滚；已升玩家任意维度再 attempt → already；起步礼 flag 后不重复发放

## 备份
`.bak/20260719_002034_ascension_backup_lockdown/`
