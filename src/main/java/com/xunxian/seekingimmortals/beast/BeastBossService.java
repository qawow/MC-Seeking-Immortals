package com.xunxian.seekingimmortals.beast;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.combat.status.StatusCatalogService;
import com.xunxian.seekingimmortals.combat.status.StatusRegistry;
import com.xunxian.seekingimmortals.entity.SummonedServitorEntity;
import com.xunxian.seekingimmortals.skill.effect.AbstractTechniqueEffectResolver;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
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
 * Skill effect types reference M02 abstract effect ids; status application is routed through M14.
 */
public final class BeastBossService {
    private static final String PHASE_TAG = "seeking_immortals_boss_phase";
    private static final String PHASE_TICK = "seeking_immortals_boss_phase_tick";
    private static final Snapshot SNAPSHOT = load();

    public static final List<String> STATUS_ID_STUBS = List.copyOf(StatusRegistry.allIds());

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
        return StatusRegistry.isKnown(statusId);
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
        shell.setCustomName(Component.translatable("entity.seeking_immortals.boss.name",
                displayName(defOpt.orElse(null))));
        shell.setCustomNameVisible(true);
        return shell;
    }

    /**
     * Tick phased skills for a boss shell. Safe no-op for non-bosses.
     * Uses M02 abstract effect type names as skill labels and M14 status ids for runtime effects.
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
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        int tick = mob.getPersistentData().getInt(PHASE_TICK) + 1;
        int phaseIndex = mob.getPersistentData().getInt(PHASE_TAG);
        List<PhaseSkill> phases = def.get().phases();
        PhaseSkill skill = phases.get(Math.floorMod(phaseIndex, phases.size()));
        if (!isPhaseReady(tick, skill.cooldownTicks())) {
            mob.getPersistentData().putInt(PHASE_TICK, tick);
            return;
        }
        if (applyPhaseSkill(mob, target, skill)) {
            mob.getPersistentData().putInt(PHASE_TAG, (phaseIndex + 1) % phases.size());
            mob.getPersistentData().putInt(PHASE_TICK, 0);
        }
    }

    static boolean isPhaseReady(int elapsedTicks, int cooldownTicks) {
        return elapsedTicks >= Math.max(40, cooldownTicks);
    }

    static boolean statusTargetsSelf(PhaseSkill skill) {
        return skill != null && StatusRegistry.definition(skill.statusId())
                .map(StatusCatalogService.StatusDefinition::beneficial)
                .orElse(false);
    }

    private static boolean applyPhaseSkill(Mob mob, LivingEntity target, PhaseSkill skill) {
        String type = skill.effectType() == null ? "aoe" : skill.effectType().toLowerCase(Locale.ROOT);
        boolean known = AbstractTechniqueEffectResolver.isAbstractTypeRegistered(type);
        LivingEntity statusTarget = statusTargetsSelf(skill) ? mob : target;
        StatusRegistry.applyGuaranteedStatus(statusTarget, mob, skill.statusId(), 0, 0);
        switch (type) {
            case "aoe", "aoe_dot", "field", "domain" -> {
                target.hurt(mob.damageSources().indirectMagic(mob, mob), 4.0F);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
            }
            case "projectile", "beam", "strike", "melee" -> target.hurt(mob.damageSources().mobAttack(mob), 6.0F);
            case "control", "debuff", "dot" -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
            case "buff_self", "buff", "rage" -> mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 80, 0));
            case "heal", "heal_spirit", "regen" -> mob.heal(6.0F);
            case "soul_attack", "drain" -> {
                target.hurt(mob.damageSources().indirectMagic(mob, mob), 5.0F);
                mob.heal(2.0F);
            }
            default -> {
                if (known) {
                    target.hurt(mob.damageSources().indirectMagic(mob, mob), 3.0F);
                }
            }
        }
        return true;
    }

    /**
     * Bridge for BossEncounterService: prefer catalog spawn when boss id is known.
     */
    public static Mob spawnCatalogBoss(ServerPlayer player, String bossId) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return null;
        }
        Optional<BossDef> def = find(bossId);
        if (def.isEmpty()) {
            return null;
        }
        BlockPos pos = player.blockPosition().offset(2, 0, 2);
        Mob boss = spawnBoss(level, pos, player.getYRot(), bossId);
        if (boss == null) {
            return null;
        }
        boss.setTarget(player);
        Component bossName = displayName(def.get());
        player.displayClientMessage(Component.translatable("message.seeking_immortals.boss.spawned",
                bossName), true);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.boss.kill_gate_hint", bossName), false);
        return boss;
    }

    private static Component displayName(BossDef definition) {
        if (definition != null && PlayerDisplayText.isSafe(definition.display())) {
            return Component.literal(definition.display().trim());
        }
        return Component.translatable("text.seeking_immortals.unknown_boss");
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
            list.add(new PhaseSkill("melee", "stun", 80, "puppet smash"));
            list.add(new PhaseSkill("aoe", "array_bind", 100, "gear shockwave"));
            list.add(new PhaseSkill("buff_self", "shield", 160, "iron shell"));
        } else if (key.contains("ghost") || key.contains("yin") || key.contains("nether")) {
            list.add(new PhaseSkill("soul_attack", "soul_shock", 70, "ghost wail"));
            list.add(new PhaseSkill("drain", "marrow_drain", 110, "life drain"));
            list.add(new PhaseSkill("debuff", "fear", 90, "terror field"));
        } else if (key.contains("jiao") || key.contains("dragon") || key.contains("blood")) {
            list.add(new PhaseSkill("projectile", "bleed", 60, "blood lance"));
            list.add(new PhaseSkill("aoe_dot", "burn", 100, "blood miasma"));
            list.add(new PhaseSkill("rage", "berserk", 140, "berserk"));
        } else if (key.contains("ice") || key.contains("moon") || key.contains("guanghan")) {
            list.add(new PhaseSkill("beam", "frozen", 80, "frost beam"));
            list.add(new PhaseSkill("field", "frozen", 120, "ice field"));
            list.add(new PhaseSkill("control", "stun", 100, "shatter lock"));
        } else {
            list.add(new PhaseSkill("strike", "bleed", 70, "opening strike"));
            list.add(new PhaseSkill("aoe", tier >= 10 ? "demonic_qi" : "poison", 100, "pressure wave"));
            list.add(new PhaseSkill("ultimate", "seal_nascent", 160, "domain crush"));
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
