package com.xunxian.seekingimmortals.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.cultivation.BeastContractService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.entity.CultivationFireballEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Role/id keyword casting for bulk catalog talismans.
 */
public final class CatalogTalismanService {
    private static final Map<String, String> ROLE_BY_ID = loadRoles();

    private CatalogTalismanService() {}

    public static String roleOf(String catalogId) {
        return ROLE_BY_ID.getOrDefault(normalize(catalogId), "");
    }

    public static boolean cast(ServerPlayer player, String catalogId, String role) {
        if (player == null) {
            return false;
        }
        String effectiveRole = (role == null || role.isBlank()) ? roleOf(catalogId) : role;
        Mode mode = resolveMode(catalogId, effectiveRole);
        int qiCost = qiCost(mode, gradeScale(catalogId));
        boolean[] ok = {false};
        CultivationHelper.get(player).ifPresent(cultivation -> {
            if (!player.getAbilities().instabuild && !cultivation.consumeQi(qiCost)) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.not_enough_qi"), true);
                return;
            }
            boolean success = switch (mode) {
                case PROJECTILE -> castProjectile(player, CultivationFireballEntity.SpellElement.FIRE, 8.0D + gradeScale(catalogId));
                case ICE_PROJECTILE -> castProjectile(player, CultivationFireballEntity.SpellElement.ICE_SPEAR, 7.0D + gradeScale(catalogId));
                case THUNDER -> castProjectile(player, CultivationFireballEntity.SpellElement.THUNDER, 9.0D + gradeScale(catalogId));
                case AOE -> castAoe(player, 6.0D + gradeScale(catalogId));
                case ARMOR -> castArmor(player, 20 * 20 + gradeScale(catalogId) * 40);
                case ESCAPE -> castEscape(player);
                case SPEED -> castSpeed(player);
                case INVIS -> castInvis(player);
                case CONTROL -> castControl(player);
                case WARD -> castWard(player);
                case HEAL -> castHeal(player);
                case CONTRACT -> BeastContractService.feedFromConsumable(player);
                case UTILITY -> castUtility(player, catalogId);
            };
            if (success) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.catalog_talisman.cast",
                        Component.translatable("item.seeking_immortals." + normalize(catalogId))), true);
                player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                        SoundSource.PLAYERS, 0.5F, 1.2F);
            } else if (!player.getAbilities().instabuild) {
                cultivation.addSpiritualPower(qiCost);
            }
            ok[0] = success;
        });
        return ok[0];
    }

    public static String modeKey(String catalogId, String role) {
        String effectiveRole = (role == null || role.isBlank()) ? roleOf(catalogId) : role;
        return resolveMode(catalogId, effectiveRole).key;
    }

    static Mode resolveMode(String catalogId, String role) {
        String blob = (normalize(catalogId) + " " + normalize(role)).toLowerCase(Locale.ROOT);
        if (containsAny(blob, "ice_projectile", "ice_talisman", "ice_seal", "ice_prison", "slow_ice")) {
            return Mode.ICE_PROJECTILE;
        }
        if (containsAny(blob, "thunder", "lightning", "tribulation", "xuan")) {
            return Mode.THUNDER;
        }
        if (containsAny(blob, "aoe_fire", "explosion", "burst", "fire_burst", "yang_flame", "storm")) {
            return Mode.AOE;
        }
        if (containsAny(blob, "fire_projectile", "fire_talisman", "fire_seal", "metal_blade", "wind_blade",
                "soul_scatter", "soul_damage", "wind_damage", "metal_damage", "yang_ghost")) {
            return Mode.PROJECTILE;
        }
        if (containsAny(blob, "armor", "shield", "body_guard", "defense", "golden_light", "liu_ding",
                "water_shield", "high_body", "protect")) {
            return Mode.ARMOR;
        }
        if (containsAny(blob, "escape", "teleport", "void_escape", "blood_escape", "wanli", "earth_escape",
                "one_way", "array_teleport", "utility_transport")) {
            return Mode.ESCAPE;
        }
        if (containsAny(blob, "speed", "wind_talisman", "movement") && !blob.contains("wind_blade")) {
            return Mode.SPEED;
        }
        if (containsAny(blob, "invis", "ghost_hide", "mask_qi", "hide")) {
            return Mode.INVIS;
        }
        if (containsAny(blob, "bind", "seal", "control", "dingshen", "chain", "anchor", "lock", "suppress")) {
            return Mode.CONTROL;
        }
        if (containsAny(blob, "demon", "yin_protect", "yin_soul", "anti_illusion", "dispel", "ward", "resist",
                "patrol", "guard_talisman", "anti_fashi", "anti_demon")) {
            return Mode.WARD;
        }
        if (containsAny(blob, "life_save", "heal", "bu_tian", "hua_ling", "resurrect", "spirit_form",
                "spirit_gather", "local_spirit")) {
            return Mode.HEAL;
        }
        if (containsAny(blob, "contract", "beast_contract")) {
            return Mode.CONTRACT;
        }
        if (containsAny(blob, "cipher", "key", "hint", "intel", "map", "root_repair", "secret_realm", "wooden_ox")) {
            return Mode.UTILITY;
        }
        return Mode.PROJECTILE;
    }

    private static boolean castProjectile(ServerPlayer player, CultivationFireballEntity.SpellElement element, double damage) {
        Vec3 look = player.getLookAngle();
        CultivationFireballEntity projectile = new CultivationFireballEntity(
                player.level(), player, look, damage, 1.15D, element);
        player.level().addFreshEntity(projectile);
        return true;
    }

    private static boolean castAoe(ServerPlayer player, double damage) {
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position().add(player.getLookAngle().scale(4.0D));
        AABB area = new AABB(center, center).inflate(3.5D, 2.0D, 3.5D);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                        entity -> entity.isAlive() && entity != player && entity.isAttackable())
                .stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(center)))
                .limit(10)
                .toList();
        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().indirectMagic(player, player), (float) damage);
            target.setSecondsOnFire(3);
        }
        level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 0.4D, center.z, 24, 0.8D, 0.3D, 0.8D, 0.02D);
        return true;
    }

    private static boolean castArmor(ServerPlayer player, int durationTicks) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, durationTicks, 1, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, durationTicks, 1, false, true, true));
        return true;
    }

    private static boolean castEscape(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        double distance = 10.0D + player.getRandom().nextDouble() * 4.0D;
        double x = player.getX() + look.x * distance;
        double z = player.getZ() + look.z * distance;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
        player.teleportTo(x, y + 1.0D, z);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 1, false, true, true));
        return true;
    }

    private static boolean castSpeed(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 20, 1, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 20 * 20, 0, false, true, true));
        return true;
    }

    private static boolean castInvis(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 20 * 15, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 10, 0, false, true, true));
        return true;
    }

    private static boolean castControl(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 center = player.position().add(look.scale(5.0D));
        AABB area = new AABB(center, center).inflate(3.0D, 2.0D, 3.0D);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity.isAlive() && entity != player && entity.isAttackable());
        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 6, 2, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 4, 0, false, true, true));
        }
        return true;
    }

    private static boolean castWard(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 25, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 25, 0, false, true, true));
        player.removeEffect(MobEffects.WITHER);
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.DARKNESS);
        return true;
    }

    private static boolean castHeal(ServerPlayer player) {
        player.heal(8.0F);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 8, 0, false, true, true));
        CultivationHelper.get(player).ifPresent(c -> c.addSpiritualPower(40));
        return true;
    }

    private static boolean castUtility(ServerPlayer player, String catalogId) {
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 30, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 12, 0, false, true, true));
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.catalog_talisman.utility",
                Component.translatable("item.seeking_immortals." + normalize(catalogId))), false);
        return true;
    }

    private static int gradeScale(String catalogId) {
        String id = normalize(catalogId);
        if (id.contains("high") || id.contains("xuan") || id.contains("di") || id.contains("spirit_realm")) {
            return 3;
        }
        if (id.contains("mid") || id.contains("guard") || id.contains("heaven")) {
            return 2;
        }
        return 1;
    }

    private static int qiCost(Mode mode, int scale) {
        return switch (mode) {
            case PROJECTILE, ICE_PROJECTILE, THUNDER -> 8 + scale * 2;
            case AOE -> 14 + scale * 2;
            case ARMOR, WARD -> 12 + scale;
            case ESCAPE -> 16 + scale;
            case SPEED, INVIS, CONTROL, HEAL, CONTRACT, UTILITY -> 10 + scale;
        };
    }

    private static boolean containsAny(String blob, String... keys) {
        for (String key : keys) {
            if (blob.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static Map<String, String> loadRoles() {
        Map<String, String> map = new LinkedHashMap<>();
        String path = "data/seeking_immortals/text_material/talisman_catalog.json";
        try (InputStream in = CatalogTalismanService.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                return Collections.emptyMap();
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("talismans");
            if (arr == null) {
                return Collections.emptyMap();
            }
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) {
                    continue;
                }
                JsonObject o = el.getAsJsonObject();
                String id = o.has("id") ? o.get("id").getAsString() : "";
                String role = o.has("role") ? o.get("role").getAsString() : "";
                if (id != null && !id.isBlank()) {
                    map.put(normalize(id), normalize(role));
                }
            }
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(map);
    }

    enum Mode {
        PROJECTILE("projectile"),
        ICE_PROJECTILE("ice_projectile"),
        THUNDER("thunder"),
        AOE("aoe"),
        ARMOR("armor"),
        ESCAPE("escape"),
        SPEED("speed"),
        INVIS("invis"),
        CONTROL("control"),
        WARD("ward"),
        HEAL("heal"),
        CONTRACT("contract"),
        UTILITY("utility");

        final String key;

        Mode(String key) {
            this.key = key;
        }
    }
}
