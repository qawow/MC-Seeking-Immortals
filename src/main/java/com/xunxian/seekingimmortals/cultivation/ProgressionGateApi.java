package com.xunxian.seekingimmortals.cultivation;

import net.minecraft.world.entity.player.Player;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * M01 对外门槛查询 API。供 M02/M04/M05/M08/M10/M11/M13/M14/M15 等模块做解锁判定。
 * <p>所有方法对 null/空串视为“无门槛，通过”。玩家缺失 capability 时返回 false。</p>
 */
public final class ProgressionGateApi {
    private ProgressionGateApi() {}

    public static boolean meetsRealm(Player player, String minRealmId) {
        if (minRealmId == null || minRealmId.isBlank()) return true;
        return CultivationHelper.get(player)
                .map(c -> meetsRealm(c, minRealmId))
                .orElse(false);
    }

    public static boolean meetsRealm(PlayerCultivation cultivation, String minRealmId) {
        if (cultivation == null) return false;
        if (minRealmId == null || minRealmId.isBlank()) return true;
        Realm required = Realm.fromDesignId(minRealmId);
        if (required == null) return false;
        return cultivation.getRealm().ordinal() >= required.ordinal();
    }

    public static boolean meetsRealm(PlayerCultivation cultivation, Realm minRealm) {
        if (cultivation == null) return false;
        if (minRealm == null) return true;
        return cultivation.getRealm().ordinal() >= minRealm.ordinal();
    }

    public static boolean meetsRoot(Player player, String rootRequirement) {
        if (rootRequirement == null || rootRequirement.isBlank()) return true;
        return CultivationHelper.get(player)
                .map(c -> meetsRoot(c, rootRequirement))
                .orElse(false);
    }

    /**
     * 灵根门槛：支持分类名（HEAVENLY/DUAL…）、属性 corpus id（fire/metal…）、
     * 或 “any”/“awakened”/“tested” 语义。
     */
    public static boolean meetsRoot(PlayerCultivation cultivation, String rootRequirement) {
        if (cultivation == null) return false;
        if (rootRequirement == null || rootRequirement.isBlank()) return true;
        String raw = rootRequirement.trim();
        String key = raw.toLowerCase(Locale.ROOT);
        if ("any".equals(key) || "true".equals(key) || "*".equals(key)) return true;
        if ("awakened".equals(key)) return cultivation.isSpiritualRootAwakened();
        if ("tested".equals(key)) return cultivation.isSpiritualRootTested();
        if ("five_elements".equals(key) || "complete_five".equals(key)) {
            return cultivation.hasCompleteFiveElements();
        }

        Optional<SpiritualRoot> category = parseRootCategory(raw);
        if (category.isPresent()) {
            SpiritualRoot required = category.get();
            SpiritualRoot current = cultivation.getSpiritualRoot();
            if (current == required) return true;
            if (required == SpiritualRoot.MUTATED && current.isVariantCategory()) return true;
            if (required.getCategoryName().equals(current.getCategoryName())) return true;
            return false;
        }

        SpiritualRootAttribute attribute = SpiritualRootAttribute.fromCorpusId(key);
        if (attribute != SpiritualRootAttribute.NONE) {
            return cultivation.getSpiritualRootAttributes().contains(attribute);
        }
        for (SpiritualRootAttribute attr : cultivation.getSpiritualRootAttributes()) {
            if (attr.name().equalsIgnoreCase(raw)
                    || attr.getDisplayName().equals(raw)
                    || attr.getCorpusId().equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }

    public static boolean meetsPath(Player player, String pathId) {
        if (pathId == null || pathId.isBlank()) return true;
        return CultivationHelper.get(player)
                .map(c -> meetsPath(c, pathId))
                .orElse(false);
    }

    public static boolean meetsPath(PlayerCultivation cultivation, String pathId) {
        if (cultivation == null) return false;
        if (pathId == null || pathId.isBlank()) return true;
        String required = pathId.trim().toLowerCase(Locale.ROOT);
        String current = Optional.ofNullable(cultivation.getCultivationPathId()).orElse("").trim().toLowerCase(Locale.ROOT);
        if (current.isEmpty()) {
            return "orthodox".equals(required) || "default".equals(required) || "human".equals(required);
        }
        if (current.equals(required)) return true;
        if (("ghost".equals(required) || "ghost_cultivator".equals(required))
                && ("ghost".equals(current) || "ghost_cultivator".equals(current))) {
            return true;
        }
        return false;
    }

    public static boolean meetsRace(Player player, String raceId) {
        if (raceId == null || raceId.isBlank()) return true;
        return CultivationHelper.get(player)
                .map(c -> meetsRace(c, raceId))
                .orElse(false);
    }

    public static boolean meetsRace(PlayerCultivation cultivation, String raceId) {
        if (cultivation == null) return false;
        if (raceId == null || raceId.isBlank()) return true;
        String required = raceId.trim().toLowerCase(Locale.ROOT);
        String current = Optional.ofNullable(cultivation.getPlayableRaceId()).orElse("").trim().toLowerCase(Locale.ROOT);
        if (current.isEmpty()) {
            current = "human_mortal";
        }
        if (current.equals(required)) return true;
        if (("human".equals(required) || "human_cultivator".equals(required) || "human_mortal".equals(required))
                && (current.equals("human_mortal") || current.equals("human_cultivator"))) {
            return true;
        }
        return false;
    }

    public static boolean meetsConstitution(Player player, String constitutionId) {
        if (constitutionId == null || constitutionId.isBlank()) return true;
        return CultivationHelper.get(player)
                .map(c -> meetsConstitution(c, constitutionId))
                .orElse(false);
    }

    public static boolean meetsConstitution(PlayerCultivation cultivation, String constitutionId) {
        if (cultivation == null) return false;
        if (constitutionId == null || constitutionId.isBlank()) return true;
        String required = constitutionId.trim().toLowerCase(Locale.ROOT);
        String current = Optional.ofNullable(cultivation.getConstitutionId()).orElse("").trim().toLowerCase(Locale.ROOT);
        if (!current.isEmpty() && current.equals(required)) return true;
        SpecialPhysique physique = cultivation.getSpecialPhysique();
        if (physique != null && physique != SpecialPhysique.NONE) {
            String mapped = SpecialPhysique.toConstitutionId(physique);
            if (required.equals(mapped) || required.equals(physique.name().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return "none".equals(required) && (current.isEmpty() || "none".equals(current))
                && (physique == null || physique == SpecialPhysique.NONE);
    }

    public static boolean meetsAll(Player player, String minRealmId, String rootRequirement,
                                   String pathId, String raceId) {
        return meetsRealm(player, minRealmId)
                && meetsRoot(player, rootRequirement)
                && meetsPath(player, pathId)
                && meetsRace(player, raceId);
    }

    private static Optional<SpiritualRoot> parseRootCategory(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        String key = value.trim().toUpperCase(Locale.ROOT);
        return switch (key) {
            case "HEAVENLY", "天灵根" -> Optional.of(SpiritualRoot.HEAVENLY);
            case "HIDDEN", "隐灵根" -> Optional.of(SpiritualRoot.HIDDEN);
            case "MUTATED", "VARIANT", "变异灵根", "异灵根" -> Optional.of(SpiritualRoot.MUTATED);
            case "DUAL", "双灵根" -> Optional.of(SpiritualRoot.DUAL);
            case "TRIPLE", "三灵根" -> Optional.of(SpiritualRoot.TRIPLE);
            case "FALSE_ROOT", "PSEUDO", "伪灵根" -> Optional.of(SpiritualRoot.FALSE_ROOT);
            case "MIXED", "FIVE_ELEMENTS", "杂灵根" -> Optional.of(SpiritualRoot.MIXED);
            default -> Optional.empty();
        };
    }
}
