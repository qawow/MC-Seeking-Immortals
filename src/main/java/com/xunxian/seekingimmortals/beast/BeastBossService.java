package com.xunxian.seekingimmortals.beast;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.entity.SummonedServitorEntity;
import com.xunxian.seekingimmortals.skill.effect.AbstractTechniqueEffectResolver;
import com.xunxian.seekingimmortals.worldpack.BossEncounterService;
import com.xunxian.seekingimmortals.worldpack.TrialCombatShellService;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;


import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * M10: secret-realm boss supply — entity registration via SummonedServitor shells + phased skills.
 * Skill effect types reference M02 abstract effect ids; status ids are M14 stubs until StatusRegistry lands.
 */
public final class BeastBossService {
    private static final String PHASE_TAG = "seeking_immortals_boss_phase";
    private static final String PHASE_TICK = "seeking_immortals_boss_phase_tick";
    private static final Snapshot SNAPSHOT = load();

    /** M14 status id stubs (string-stable for cross-module). */
    public static final List<String> STATUS_ID_STUBS = List.of(
            "burn", "frozen", "soul_shock", "illusion", "karma", "demonic_qi",
            "foundation_unstable", "marrow_drain", "seal_nascent", "conceal_qi",
            "bleed", "poison", "paralyze", "fear", "suppress", "rage",
            "shield", "regen", "haste", "slow");

    private BeastBossService() {}

    public record PhaseSkill(String effectType, String statusId, int cooldownTicks, String note) {}

    public record BossDef(
            String bossId,
            String secretRealmId,
            String display,
            int beastTier,
            String realmEquiv,
            String lootBand,
            boolean demonCore,
            List<PhaseSkill> phases) {}

    public record Snapshot(Map<String, BossDef> byId) {
        public int size() {
            return byId.size();
        }
    }

    public static Snapshot snapshot() {
        return SNAPSHOT;
    }

    public static int size() {
        return SNAPSHOT.size();
    }

    public static Optional<BossDef> find(String bossId) {
        if (bossId == null || bossId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(SNAPSHOT.byId().get(bossId.trim().toLowerCase(Locale.ROOT)));
    }

    public static List<String> allBossIds() {
        return List.copyOf(SNAPSHOT.byId().keySet());
    }

    public static boolean isKnownStatusId(String statusId) {
        if (statusId == null || statusId.isBlank()) {
            return false;
        }
        return STATUS_ID_STUBS.contains(statusId.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Spawn a catalog boss with tier-scaled stats. Tags for loot/phase AI.
     */
    public static Mob spawnBoss(ServerLevel level, BlockPos pos, float yRot, String bossId) {
        Optional<BossDef> defOpt = find(bossId);
        String id = bossId == null ? "unknown_boss" : bossId.trim().toLowerCase(Locale.ROOT);
        int tier = defOpt.map(BossDef::beastTier).orElse(7);
        if (tier <= 0) {
            tier = 7;
        }
        BeastTierService.ScaledStats stats = BeastTierService.scaleStats(tier);
        // Bosses are denser than wild ecology shells.
        double health = stats.health() * 2.4D;
        double damage = stats.damage() * 1.6D;
        SummonedServitorEntity.Archetype archetype = TrialCombatShellService.archetypeFor(id);
        if (defOpt.isPresent() && id.contains("puppet")) {
            archetype = SummonedServitorEntity.Archetype.PUPPET;
        }
        SummonedServitorEntity shell = TrialCombatShellService.spawnHostile(
                level, pos, yRot, "boss_" + id, health, damage, archetype);
        if (shell == null) {
            return null;
        }
        CompoundTag tag = shell.getPersistentData().getCompound(BossEncounterService.BOSS_TAG).copy();
        tag.putString(BossEncounterService.BOSS_ID, id);
        tag.putInt("BeastTier", tier);
        tag.putString("LootBand", defOpt.map(BossDef::lootBand).orElse(BeastTierService.lootBandFor(tier)));
        shell.getPersistentData().put(BossEncounterService.BOSS_TAG, tag);
        shell.getPersistentData().putString("seeking_immortals_beast_id", id);
        shell.getPersistentData().putInt("seeking_immortals_beast_tier", tier);
        shell.getPersistentData().putBoolean("seeking_immortals_ecology_beast", false);
        shell.getPersistentData().putInt(PHASE_TAG, 0);
        shell.getPersistentData().putInt(PHASE_TICK, 0);
        String display = defOpt.map(BossDef::display).filter(s -> !s.isBlank()).orElse(id);
        shell.setCustomName(Component.translatable("entity.seeking_immortals.boss.name", display));
        shell.setCustomNameVisible(true);
        return shell;
    }

    /**
     * Tick phased skills for a boss shell. Safe no-op for non-bosses.
     * Uses M02 abstract effect type names as skill labels + M14 status stubs applied via vanilla proxies.
     */
    public static void tickBossSkills(Mob mob) {
        if (mob == null || mob.level().isClientSide || !BossEncounterService.isBossMob(mob)) {
            return;
        }
        String bossId = BossEncounterService.bossIdOf(mob);
        Optional<BossDef> def = find(bossId);
        if (def.isEmpty() || def.get().phases().isEmpty()) {
            return;
        }
        int tick = mob.getPersistentData().getInt(PHASE_TICK) + 1;
        mob.getPersistentData().putInt(PHASE_TICK, tick);
        int phaseIndex = mob.getPersistentData().getInt(PHASE_TAG);
        List<PhaseSkill> phases = def.get().phases();
        PhaseSkill skill = phases.get(Math.floorMod(phaseIndex, phases.size()));
        int cd = Math.max(40, skill.cooldownTicks());
        if (tick % cd != 0) {
            return;
        }
        // Advance phase on each cast.
        mob.getPersistentData().putInt(PHASE_TAG, (phaseIndex + 1) % phases.size());
        applyPhaseSkill(mob, skill);
    }

    private static void applyPhaseSkill(Mob mob, PhaseSkill skill) {
        // M02 abstract effect type is recorded; runtime uses vanilla proxies until full M02 cast pipeline for mobs.
        String type = skill.effectType() == null ? "aoe" : skill.effectType().toLowerCase(Locale.ROOT);
        boolean known = AbstractTechniqueEffectResolver.isAbstractTypeRegistered(type);
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        // Status stub → vanilla effect proxy (M14 will replace with StatusRegistry).
        applyStatusStub(target, skill.statusId());
        switch (type) {
            case "aoe", "aoe_dot", "field", "domain" -> {
                target.hurt(mob.damageSources().magic(), 4.0F);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
            }
            case "projectile", "beam", "strike", "melee" -> target.hurt(mob.damageSources().mobAttack(mob), 6.0F);
            case "control", "debuff", "dot" -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
            case "buff_self", "buff", "rage" -> mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 80, 0));
            case "heal", "heal_spirit", "regen" -> mob.heal(6.0F);
            case "soul_attack", "drain" -> {
                target.hurt(mob.damageSources().magic(), 5.0F);
                mob.heal(2.0F);
            }
            default -> {
                if (known) {
                    target.hurt(mob.damageSources().magic(), 3.0F);
                }
            }
        }
    }

    /** M14 status id stub application via vanilla proxies. */
    public static void applyStatusStub(LivingEntity target, String statusId) {
        if (target == null || statusId == null || statusId.isBlank()) {
            return;
        }
        String id = statusId.trim().toLowerCase(Locale.ROOT);
        switch (id) {
            case "burn" -> target.setSecondsOnFire(4);
            case "frozen", "slow" -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            case "poison", "marrow_drain" -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
            case "bleed" -> target.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
            case "fear", "soul_shock" -> target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
            case "paralyze", "seal_nascent", "suppress" -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
            case "illusion" -> target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
            case "demonic_qi", "karma", "foundation_unstable" -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
            case "shield" -> target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0));
            case "regen" -> target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
            case "haste", "rage" -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0));
            case "conceal_qi" -> target.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0));
            default -> {
                // unknown stub — no-op
            }
        }
    }

    /**
     * Bridge for BossEncounterService: prefer catalog spawn when boss id is known.
     */
    public static boolean spawnCatalogBoss(ServerPlayer player, String bossId) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        Optional<BossDef> def = find(bossId);
        if (def.isEmpty()) {
            return false;
        }
        BlockPos pos = player.blockPosition().offset(2, 0, 2);
        Mob boss = spawnBoss(level, pos, player.getYRot(), bossId);
        if (boss == null) {
            return false;
        }
        boss.setTarget(player);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.boss.spawned",
                def.get().display().isBlank() ? bossId : def.get().display()), true);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.boss.kill_gate_hint", bossId), false);
        return true;
    }

    private static Snapshot load() {
        Map<String, BossDef> map = new LinkedHashMap<>();
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/beast_boss_tier_secret_realm_map.json");
        if (root == null || !root.has("entries") || !root.get("entries").isJsonArray()) {
            return new Snapshot(Map.of());
        }
        for (JsonElement element : root.getAsJsonArray("entries")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject o = element.getAsJsonObject();
            String id = str(o, "boss_id").toLowerCase(Locale.ROOT);
            if (id.isBlank()) {
                continue;
            }
            int tier = o.has("beast_tier_typical") && !o.get("beast_tier_typical").isJsonNull()
                    ? o.get("beast_tier_typical").getAsInt() : 7;
            if (tier <= 0) {
                tier = 7;
            }
            List<PhaseSkill> phases = defaultPhasesFor(id, tier);
            map.put(id, new BossDef(
                    id,
                    str(o, "secret_realm_id"),
                    str(o, "display"),
                    BeastTierService.clampTier(tier),
                    str(o, "realm_equiv"),
                    str(o, "loot_band").isBlank() ? BeastTierService.lootBandFor(tier) : str(o, "loot_band"),
                    o.has("demon_core") && o.get("demon_core").getAsBoolean(),
                    phases));
        }
        return new Snapshot(Collections.unmodifiableMap(map));
    }

    private static List<PhaseSkill> defaultPhasesFor(String bossId, int tier) {
        List<PhaseSkill> list = new ArrayList<>();
        String key = bossId == null ? "" : bossId;
        if (key.contains("puppet")) {
            list.add(new PhaseSkill("melee", "suppress", 80, "puppet smash"));
            list.add(new PhaseSkill("aoe", "slow", 100, "gear shockwave"));
            list.add(new PhaseSkill("buff_self", "shield", 160, "iron shell"));
        } else if (key.contains("ghost") || key.contains("yin") || key.contains("nether")) {
            list.add(new PhaseSkill("soul_attack", "soul_shock", 70, "ghost wail"));
            list.add(new PhaseSkill("drain", "marrow_drain", 110, "life drain"));
            list.add(new PhaseSkill("debuff", "fear", 90, "terror field"));
        } else if (key.contains("jiao") || key.contains("dragon") || key.contains("blood")) {
            list.add(new PhaseSkill("projectile", "bleed", 60, "blood lance"));
            list.add(new PhaseSkill("aoe_dot", "burn", 100, "blood miasma"));
            list.add(new PhaseSkill("rage", "rage", 140, "berserk"));
        } else if (key.contains("ice") || key.contains("moon") || key.contains("guanghan")) {
            list.add(new PhaseSkill("beam", "frozen", 80, "frost beam"));
            list.add(new PhaseSkill("field", "slow", 120, "ice field"));
            list.add(new PhaseSkill("control", "paralyze", 100, "shatter lock"));
        } else {
            list.add(new PhaseSkill("strike", "bleed", 70, "opening strike"));
            list.add(new PhaseSkill("aoe", tier >= 10 ? "demonic_qi" : "poison", 100, "pressure wave"));
            list.add(new PhaseSkill("ultimate", "suppress", 160, "domain crush"));
        }
        return List.copyOf(list);
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = BeastBossService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String str(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        try {
            return object.get(key).getAsString().trim();
        } catch (Exception ignored) {
            return "";
        }
    }
}
