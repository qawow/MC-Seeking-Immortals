package com.xunxian.seekingimmortals.artifact;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.TechniqueDataManager;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.SkillType;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import com.xunxian.seekingimmortals.skill.effect.SkillEffect;
import com.xunxian.seekingimmortals.skill.effect.SkillEffectRegistry;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxOrchestrator;
import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * M15 法宝主动技：将 catalog effect / type 映射到 M02 {@link SkillEffectRegistry} 效果 id，
 * 冷却与灵力消耗一律服务端校验。
 */
public final class ArtifactActiveSkillService {
    private static final Map<String, String> EFFECT_TO_TECHNIQUE = buildEffectMap();
    private static final Map<String, String> TYPE_TO_TECHNIQUE = buildTypeMap();
    private static final Map<String, String> ID_TO_TECHNIQUE = buildIdMap();

    private ArtifactActiveSkillService() {}

    public record ResolvedSkill(String techniqueId, SkillType skillType, SkillEffect effect) {}

    public enum CastResult {
        UNMAPPED,
        DENIED,
        SUCCESS
    }

    public static Optional<ResolvedSkill> resolve(String artifactId) {
        ArtifactDataService.ArtifactDefinition def = ArtifactDataService.builtin()
                .findArtifact(artifactId).orElse(null);
        if (def == null) {
            return Optional.empty();
        }
        return resolve(def);
    }

    public static Optional<ResolvedSkill> resolve(ArtifactDataService.ArtifactDefinition def) {
        if (def == null) {
            return Optional.empty();
        }
        String techniqueId = mapTechniqueId(def);
        if (techniqueId == null || techniqueId.isBlank()) {
            return Optional.empty();
        }
        try {
            SkillType type = SkillEffectRegistry.byTechniqueId(techniqueId);
            if (type == null) {
                return Optional.empty();
            }
            SkillEffect effect = SkillEffectRegistry.get(type);
            if (effect == null) {
                return Optional.empty();
            }
            return Optional.of(new ResolvedSkill(techniqueId, type, effect));
        } catch (Throwable t) {
            // Unit tests without full MC bootstrap cannot init SkillEffectRegistry.
            return Optional.empty();
        }
    }

    /** Pure mapping without touching SkillEffectRegistry (unit-test safe). */
    public static boolean hasTechniqueMapping(String artifactId) {
        return ArtifactDataService.builtin().findArtifact(artifactId)
                .map(def -> !mapTechniqueId(def).isBlank())
                .orElse(false);
    }

    public static int mappedTechniqueCount() {
        ArtifactDataService.Snapshot snap = ArtifactDataService.builtin();
        int count = 0;
        for (ArtifactDataService.ArtifactDefinition def : snap.artifacts().values()) {
            if (!mapTechniqueId(def).isBlank()) {
                count++;
            }
        }
        return count;
    }

    /**
     * 尝试以 M02 效果释放法宝主动技。只有 {@link CastResult#UNMAPPED} 允许调用方回退 kind 激活；
     * 映射存在但校验或执行失败时返回 {@link CastResult#DENIED}。
     */
    public static CastResult tryCast(ServerPlayer player, PlayerCultivation cultivation,
                                     ItemStack stack, ArtifactDataService.ArtifactDefinition def,
                                     double powerScale) {
        if (player == null || cultivation == null || def == null) {
            return CastResult.DENIED;
        }
        Optional<ResolvedSkill> resolved = resolve(def);
        if (resolved.isEmpty()) {
            return CastResult.UNMAPPED;
        }
        ResolvedSkill skill = resolved.get();
        SkillEffect effect = skill.effect();
        int level = Math.max(1, 1 + ArtifactOwnershipService.refinementLayer(stack)
                + (ArtifactOwnershipService.isSpiritAwakened(stack) ? 2 : 0));
        int cost = ArtifactPowerService.scaledSpiritualCost(effect.getSpiritualPowerCost(level), powerScale);
        int cooldown = ArtifactPowerService.scaledCooldown(effect.getCooldownTicks(level), powerScale);

        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return CastResult.DENIED;
        }
        if (!effect.canExecute(player, cultivation)) {
            return CastResult.DENIED;
        }
        if (cultivation.getSpiritualPower() < cost) {
            return CastResult.DENIED;
        }

        CultivationSkill virtual = new CultivationSkill(skill.skillType());
        virtual.unlock();
        if (level > 1) {
            net.minecraft.nbt.CompoundTag tag = virtual.saveNBT();
            tag.putInt("Level", Math.min(level, Math.max(1, virtual.getMaxLevel())));
            tag.putBoolean("Unlocked", true);
            CultivationSkill loaded = CultivationSkill.loadNBT(tag);
            if (loaded != null) {
                virtual = loaded;
            }
        }
        SkillContext context = SkillContext.builder()
                .level(player.serverLevel())
                .position(player.position())
                .lookDirection(player.getLookAngle())
                .powerScale(powerScale)
                .build();

        boolean ok;
        Vec3 beforeCast = player.position();
        TechniqueVfxPacket.CaptureScope vfxCapture = TechniqueVfxPacket.captureSynchronousIntents();
        try {
            // Push scale so all SpellEffect.calculateDamage call sites honor over-tier suppression.
            com.xunxian.seekingimmortals.skill.effect.spell.SpellEffect.pushPowerScale(powerScale);
            ok = effect.execute(player, cultivation, virtual, context);
        } catch (Throwable t) {
            return CastResult.DENIED;
        } finally {
            com.xunxian.seekingimmortals.skill.effect.spell.SpellEffect.clearPowerScale();
            vfxCapture.close();
        }
        if (!ok) {
            return CastResult.DENIED;
        }
        if (!cultivation.consumeSpiritualPower(cost)) {
            return CastResult.DENIED;
        }
        TechniqueVfxOrchestrator.emitSuccessfulCast(
                player,
                TechniqueDataManager.getTechnique(player.getServer(), skill.techniqueId()).orElse(null),
                skill.skillType(),
                beforeCast,
                vfxCapture.packets(),
                false,
                ArtifactVfxOrchestrator.overrideFor(def.id()));
        player.getCooldowns().addCooldown(stack.getItem(), cooldown);
        // 本命成长
        if (def.id().equals(NatalBindingService.boundId(player))) {
            NatalBindingService.grow(player);
        }
        return CastResult.SUCCESS;
    }

    static boolean shouldFallbackToGeneric(CastResult result) {
        return result == CastResult.UNMAPPED;
    }

    public static String mapTechniqueId(ArtifactDataService.ArtifactDefinition def) {
        if (def == null) {
            return "";
        }
        String id = def.id() == null ? "" : def.id().toLowerCase(Locale.ROOT);
        if (ID_TO_TECHNIQUE.containsKey(id)) {
            return ID_TO_TECHNIQUE.get(id);
        }
        String effect = def.effect() == null ? "" : def.effect().toLowerCase(Locale.ROOT);
        if (!effect.isBlank() && EFFECT_TO_TECHNIQUE.containsKey(effect)) {
            return EFFECT_TO_TECHNIQUE.get(effect);
        }
        // effect 关键字模糊
        for (Map.Entry<String, String> entry : EFFECT_TO_TECHNIQUE.entrySet()) {
            if (!entry.getKey().isBlank() && effect.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        String type = def.type() == null ? "" : def.type().toLowerCase(Locale.ROOT);
        if (TYPE_TO_TECHNIQUE.containsKey(type)) {
            return TYPE_TO_TECHNIQUE.get(type);
        }
        for (Map.Entry<String, String> entry : TYPE_TO_TECHNIQUE.entrySet()) {
            if (!entry.getKey().isBlank() && type.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "";
    }

    public static int mappedSkillCount() {
        // Prefer registry-resolved count when MC is bootstrapped; fall back to pure mapping.
        try {
            ArtifactDataService.Snapshot snap = ArtifactDataService.builtin();
            int count = 0;
            for (ArtifactDataService.ArtifactDefinition def : snap.artifacts().values()) {
                if (resolve(def).isPresent()) {
                    count++;
                }
            }
            if (count > 0) {
                return count;
            }
        } catch (Throwable ignored) {
        }
        return mappedTechniqueCount();
    }

    private static Map<String, String> buildEffectMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("wind_blade", "wind_blade");
        map.put("flame_slash", "fireball_art");
        map.put("frost_slash", "ice_spear");
        map.put("heavy_slash", "flying_sword_strike");
        map.put("golden_brick_strike", "metal_sword_finger");
        map.put("meteor_strike", "elemental_burst_fire");
        map.put("space_cut", "void_palace_heaven_earth");
        map.put("sound_stun_aoe", "soul_cry_shock");
        map.put("soul_stun_short", "soul_attack_wave");
        map.put("soul_harvest_on_kill", "yinluo_soul_harvest");
        map.put("soul_bind", "soul_contract");
        map.put("bind_slow", "wind_binding_art");
        map.put("ring_bind_burn", "lieyan_true_fire_secret");
        map.put("ring_throw_bind", "wind_binding_art");
        map.put("freeze_attackers", "xuantian_ice_prison");
        map.put("earth_split", "earth_mountain_press");
        map.put("venom_burst", "qingluo_poison_needle");
        map.put("flame_barrier_aoe", "flame_ring");
        map.put("deploy_flame_barrier_once", "flame_ring");
        map.put("deploy_fire_formation", "formation_trap_basic");
        map.put("void_zone_slow", "earth_prison_art");
        map.put("block_projectile", "sword_shield");
        map.put("spell_block_pulse", "aura_body_shield");
        map.put("block_melee_spell", "aura_body_shield");
        map.put("spin_aoe", "thousand_sword_array");
        map.put("thunder_palm_cd", "thunder_palm");
        map.put("suppress_beast_tier", "beast_tame_call");
        map.put("beast_obey_chance", "spirit_beast_contract");
        map.put("summon_t0_puppet_60s", "puppet_summon_basic");
        map.put("hide_aura_sense", "sword_escape");
        map.put("reveal_spirit_nodes", "formation_sense");
        map.put("physical_reduce_light", "sword_shield");
        map.put("damage_reduce_flat", "aura_body_shield");
        map.put("head_damage_reduce", "aura_body_shield");
        return Map.copyOf(map);
    }

    private static Map<String, String> buildTypeMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("flying_sword", "flying_sword_strike");
        map.put("offense", "flying_sword_strike");
        map.put("attack", "single_sword_thrust");
        map.put("thunder", "thunder_palm");
        map.put("poison", "qingluo_poison_needle");
        map.put("soul_attack", "soul_attack_wave");
        map.put("soul", "soul_devour");
        map.put("soul_destroy", "soul_attack_wave");
        map.put("defense", "aura_body_shield");
        map.put("movement", "wind_escape");
        map.put("illusion", "illusion_formation");
        map.put("control", "wind_binding_art");
        map.put("formation", "defense_formation");
        map.put("formation_token", "formation_trap_basic");
        map.put("formation_deploy", "defense_formation");
        map.put("beast_control", "beast_tame_call");
        map.put("beast", "beast_tame_call");
        map.put("puppet", "puppet_summon_basic");
        map.put("puppet_control", "array_puppet");
        map.put("puppet_summon", "puppet_summon_basic");
        map.put("space_control", "void_palace_heaven_earth");
        map.put("mirror", "water_mirror_reflect");
        map.put("ruler", "metal_sword_finger");
        map.put("sound", "soul_cry_shock");
        map.put("swarm", "puppet_swarm");
        map.put("yin", "yin_soul_chain");
        map.put("vehicle", "wind_escape");
        map.put("vehicle_key", "wind_escape");
        map.put("talisman_treasure", "cast_fire_burst_talisman");
        return Map.copyOf(map);
    }

    private static Map<String, String> buildIdMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("flying_sword_low", "flying_sword_strike");
        map.put("green_bamboo_cloud_sword", "green_bamboo_sword_qi");
        map.put("green_bamboo_leaf_sword", "green_bamboo_sword_ray");
        map.put("silver_giant_sword", "blade_giant_sword_ultimate_secret");
        map.put("gold_demon_chain", "dual_sword_dance");
        map.put("peerless_flying_knives", "invisible_sword");
        map.put("thousand_bee_needles", "qingluo_poison_needle");
        map.put("invisible_needle_set", "invisible_sword");
        map.put("thunder_palm_artifact", "thunder_palm");
        map.put("wind_escape_sail", "wind_escape");
        map.put("cloud_boots", "wind_escape");
        map.put("evil_illusion_mirror", "illusion_formation");
        map.put("qingning_mirror", "water_mirror_reflect");
        map.put("xuanguang_mirror", "soul_attack_wave");
        map.put("xuanhuang_mirror", "soul_attack_wave");
        map.put("soul_summon_bell", "soul_banner");
        map.put("soul_gathering_bowl", "poluo_soul_pull");
        map.put("poluo_beads", "poluo_soul_pull");
        map.put("beast_taming_whip", "beast_tame_call");
        map.put("spirit_beast_bridle", "spirit_beast_contract");
        map.put("talisman_treasure_demon_seal", "cast_soul_lock_talisman");
        map.put("talisman_treasure_soul_charm", "hehuan_soul_charm");
        map.put("talisman_treasure_fire_spear", "cast_fire_burst_talisman");
        map.put("talisman_treasure_thunder_rod", "cast_thunder_talisman");
        map.put("talisman_treasure_ice_shield", "cast_ice_seal_talisman");
        map.put("talisman_treasure_golden_wheel", "gold_beam");
        map.put("four_symbols_ruler_replica", "metal_sword_finger");
        map.put("void_refining_bell", "void_palace_heaven_earth");
        map.put("great_shift_token", "void_palace_heaven_earth");
        map.put("artifact_spirit_awakening_incense", "artifact_spirit_awaken_secret");
        return Map.copyOf(map);
    }
}
