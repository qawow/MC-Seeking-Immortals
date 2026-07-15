package com.xunxian.seekingimmortals.catalog;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.network.SyncLearnedMethodsPacket;
import com.xunxian.seekingimmortals.worldpack.WorldpackGameplayService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Applies manuals_catalog entries via existing catalog carriers.
 * Wave464: persistent studied set + forge-grade unlock ceiling from rich text_material fields.
 * Wave473: cultivation method learn authority (persistent learned methods for TechniqueGate).
 * Wave474: manual unlocks + sect outer promotion grant starter methods.
 * Wave475: technique-manual source text maps to related cultivation methods.
 * Wave481: method layer cultivation (1-9) with spiritual/cultivation costs.
 * Reuses TextMaterialCatalogService.ManualEntry/MethodEntry and CultivationHelper only (no new systems).
 */
public final class ManualCatalogService {
    public static final String STUDIED_TAG = "seeking_immortals_studied_manuals";
    public static final String LEARNED_METHODS_TAG = "seeking_immortals_learned_methods";
    public static final String METHOD_LAYERS_TAG = "seeking_immortals_method_layers";
    public static final int MAX_METHOD_LAYER = 9;
    private static final List<String> PROGRESSION_TAGS = List.of(
            STUDIED_TAG,
            LEARNED_METHODS_TAG,
            METHOD_LAYERS_TAG);

    private ManualCatalogService() {}

    /** Preserve consumed-manual progression when Forge clones a player after death. */
    public static void copyProgressionData(CompoundTag originalData, CompoundTag clonedData) {
        if (originalData == null || clonedData == null) {
            return;
        }
        for (String key : PROGRESSION_TAGS) {
            if (originalData.contains(key) && originalData.get(key) != null) {
                clonedData.put(key, originalData.get(key).copy());
            }
        }
    }

    public static boolean study(ServerPlayer player, String manualId) {
        Optional<TextMaterialCatalogService.ManualEntry> optional =
                TextMaterialCatalogService.builtin().findManual(manualId);
        if (optional.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.manual.unknown", manualId), false);
            return false;
        }
        TextMaterialCatalogService.ManualEntry manual = optional.get();
        boolean[] ok = {false};
        CultivationHelper.get(player).ifPresent(cultivation -> {
            if (!manual.realmMin().isBlank() && !WorldpackGameplayService.meetsMinRealm(cultivation.getRealm(), manual.realmMin())) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.manual.realm_too_low",
                        displayName(manual),
                        com.xunxian.seekingimmortals.artifact.ArtifactDisplayTexts.realm(manual.realmMin())), false);
                return;
            }
            if (hasStudied(player, manual.id())) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.manual.already_studied",
                        displayName(manual)), false);
                return;
            }
            markStudied(player, manual.id());
            applyInsight(player, manual);
            // Wave474: unlock tokens that map to cultivation methods are granted as learned methods.
            int grantedMethods = grantUnlockMethods(player, manual.unlocks());
            player.displayClientMessage(Component.translatable("message.seeking_immortals.manual.studied",
                    displayName(manual), typeDisplay(manual.type())), true);
            if (!manual.note().isBlank() && !manual.note().equals(manual.display())
                    && !looksLikeCode(manual.note())) {
                player.displayClientMessage(Component.literal(manual.note()), false);
            }
            if (!manual.unlocks().isEmpty()) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.manual.unlocks",
                        unlocksDisplay(manual.unlocks())), false);
            }
            if (grantedMethods > 0) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.manual.methods_granted", grantedMethods), false);
            }
            if (manual.unlocksForgeGrade() > 0) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.manual.forge_grade",
                        manual.unlocksForgeGrade()), false);
            }
            if (!manual.recipeId().isBlank()) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.manual.recipe",
                        recipeDisplay(manual.recipeId())), false);
            }
            ok[0] = true;
        });
        return ok[0];
    }

    public static Component typeDisplay(String typeCode) {
        String code = typeCode == null ? "" : typeCode.trim().toLowerCase(Locale.ROOT);
        if (code.isBlank()) {
            return Component.translatable("manual.type.seeking_immortals.unknown");
        }
        String key = "manual.type.seeking_immortals." + code;
        net.minecraft.locale.Language language = net.minecraft.locale.Language.getInstance();
        if (language != null && language.has(key)) {
            return Component.translatable(key);
        }
        return Component.literal(switch (code) {
            case "alchemy" -> "炼丹";
            case "refinement" -> "炼器";
            case "talisman" -> "符箓";
            case "formation" -> "阵法";
            case "puppet" -> "傀儡";
            case "cultivation_path" -> "修炼道路";
            case "quest" -> "任务";
            default -> code;
        });
    }

    public static Component recipeDisplay(String recipeId) {
        String id = recipeId == null ? "" : recipeId.trim();
        if (id.isBlank()) {
            return Component.literal("");
        }
        net.minecraft.locale.Language language = net.minecraft.locale.Language.getInstance();
        String itemKey = "item.seeking_immortals." + id;
        if (language != null && language.has(itemKey)) {
            return Component.translatable(itemKey);
        }
        String alchemyKey = "alchemy_recipe.seeking_immortals." + id;
        if (language != null && language.has(alchemyKey)) {
            return Component.translatable(alchemyKey);
        }
        // Prefer catalog manual display when recipe id equals a manual carrier.
        Optional<TextMaterialCatalogService.ManualEntry> manual =
                TextMaterialCatalogService.builtin().findManual(id);
        if (manual.isPresent() && manual.get().display() != null && !manual.get().display().isBlank()) {
            return Component.literal(manual.get().display());
        }
        return Component.literal(id);
    }

    public static Component displayName(TextMaterialCatalogService.ManualEntry manual) {
        if (manual == null) {
            return Component.literal("未知典籍");
        }
        if (manual.display() != null && !manual.display().isBlank() && !looksLikeCode(manual.display())) {
            return Component.literal(manual.display());
        }
        net.minecraft.locale.Language language = net.minecraft.locale.Language.getInstance();
        String itemKey = "item.seeking_immortals." + manual.id();
        if (language != null && language.has(itemKey)) {
            return Component.translatable(itemKey);
        }
        return Component.literal(manual.id());
    }

    private static String unlocksDisplay(List<String> unlocks) {
        if (unlocks == null || unlocks.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (String unlock : unlocks) {
            if (unlock == null || unlock.isBlank()) {
                continue;
            }
            Optional<TextMaterialCatalogService.MethodEntry> method =
                    TextMaterialCatalogService.builtin().findMethod(unlock);
            if (method.isPresent() && method.get().display() != null && !method.get().display().isBlank()
                    && !looksLikeCode(method.get().display())) {
                parts.add(method.get().display());
                continue;
            }
            net.minecraft.locale.Language language = net.minecraft.locale.Language.getInstance();
            String itemKey = "item.seeking_immortals." + unlock;
            if (language != null && language.has(itemKey)) {
                parts.add(language.getOrDefault(itemKey));
            } else {
                parts.add(unlock);
            }
        }
        return String.join("、", parts);
    }

    private static boolean looksLikeCode(String text) {
        if (text == null || text.isBlank()) {
            return true;
        }
        for (int i = 0; i < text.length(); i++) {
            if (Character.UnicodeScript.of(text.charAt(i)) == Character.UnicodeScript.HAN) {
                return false;
            }
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("_") || lower.matches("[a-z0-9\\-./: ]+");
    }

    public static boolean hasStudied(ServerPlayer player, String manualId) {
        if (player == null || manualId == null || manualId.isBlank()) {
            return false;
        }
        return player.getPersistentData().getCompound(STUDIED_TAG).getBoolean(manualId.trim().toLowerCase(Locale.ROOT));
    }

    public static void markStudied(ServerPlayer player, String manualId) {
        if (player == null || manualId == null || manualId.isBlank()) {
            return;
        }
        CompoundTag tag = player.getPersistentData().getCompound(STUDIED_TAG).copy();
        tag.putBoolean(manualId.trim().toLowerCase(Locale.ROOT), true);
        player.getPersistentData().put(STUDIED_TAG, tag);
    }

    /**
     * Max forge grade unlocked by studied refinement manuals.
     * Grade-1 is always available to callers (never returns below 1 for gameplay gating).
     */
    public static int maxUnlockedForgeGrade(ServerPlayer player) {
        if (player == null) {
            return 1;
        }
        int max = 1;
        CompoundTag studied = player.getPersistentData().getCompound(STUDIED_TAG);
        for (TextMaterialCatalogService.ManualEntry manual : TextMaterialCatalogService.builtin().manuals().values()) {
            if (manual == null || manual.unlocksForgeGrade() <= 0) {
                continue;
            }
            if (studied.getBoolean(manual.id().toLowerCase(Locale.ROOT))) {
                max = Math.max(max, manual.unlocksForgeGrade());
            }
        }
        return Math.max(1, max);
    }

    public static int manualCount() {
        return TextMaterialCatalogService.builtin().manuals().size();
    }

    public static int methodCount() {
        return TextMaterialCatalogService.builtin().methods().size();
    }

    /**
     * Wave473: learn a cultivation method from the catalog index.
     * Marks persistent learned flag used by TechniqueGateService requires_method checks.
     */
    public static boolean learnMethod(ServerPlayer player, String methodId) {
        Optional<TextMaterialCatalogService.MethodEntry> optional =
                TextMaterialCatalogService.builtin().findMethod(methodId);
        if (optional.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.method.unknown", methodId), false);
            return false;
        }
        TextMaterialCatalogService.MethodEntry method = optional.get();
        boolean[] ok = {false};
        CultivationHelper.get(player).ifPresent(cultivation -> {
            if (!method.realmMin().isBlank()
                    && !WorldpackGameplayService.meetsMinRealm(cultivation.getRealm(), method.realmMin())) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.method.realm_too_low",
                        method.display(), method.realmMin()), false);
                return;
            }
            if (hasLearnedMethod(player, method.id())) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.method.already_learned",
                        method.display()), false);
                return;
            }
            markLearnedMethod(player, method.id());
            setMethodLayer(player, method.id(), 1);
            // Light insight buff so learning is immediately felt.
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 30, 0));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 60, 0));
            player.displayClientMessage(Component.translatable("message.seeking_immortals.method.learned",
                    method.display(), method.school().isBlank() ? "-" : method.school()), true);
            ok[0] = true;
        });
        return ok[0];
    }

    /**
     * Wave481: cultivate an already-learned method to raise its layer (max 9).
     * Costs spiritual power + cultivation exp scaled by current layer.
     */
    public static boolean cultivateMethod(ServerPlayer player, String methodId) {
        Optional<TextMaterialCatalogService.MethodEntry> optional =
                TextMaterialCatalogService.builtin().findMethod(methodId);
        if (optional.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.method.unknown", methodId), false);
            return false;
        }
        TextMaterialCatalogService.MethodEntry method = optional.get();
        if (!hasLearnedMethod(player, method.id())) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.method.not_learned",
                    method.display()), false);
            return false;
        }
        int layer = getMethodLayer(player, method.id());
        if (layer >= MAX_METHOD_LAYER) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.method.layer_max",
                    method.display(), MAX_METHOD_LAYER), false);
            return false;
        }
        boolean[] ok = {false};
        CultivationHelper.get(player).ifPresent(cultivation -> {
            int spCost = cultivateSpiritualCost(layer);
            int expCost = cultivateCultivationCost(layer);
            if (cultivation.getSpiritualPower() < spCost) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.method.cultivate_need_sp",
                        spCost, cultivation.getSpiritualPower()), false);
                return;
            }
            long haveExp = cultivation.getCultivationLong() - cultivation.getCurrentStageStartExp();
            if (haveExp < expCost) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.method.cultivate_need_exp",
                        expCost, haveExp), false);
                return;
            }
            if (!player.getAbilities().instabuild) {
                if (!cultivation.consumeSpiritualPower(spCost)) {
                    return;
                }
                long next = Math.max(cultivation.getCurrentStageStartExp(),
                        cultivation.getCultivationLong() - expCost);
                cultivation.setCultivation(next);
            }
            int nextLayer = layer + 1;
            setMethodLayer(player, method.id(), nextLayer);
            SyncLearnedMethodsPacket.send(player);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 15, 0));
            player.displayClientMessage(Component.translatable("message.seeking_immortals.method.cultivated",
                    method.display(), nextLayer, MAX_METHOD_LAYER), true);
            ok[0] = true;
        });
        return ok[0];
    }

    public static int cultivateSpiritualCost(int currentLayer) {
        int layer = Math.max(1, currentLayer);
        return 20 + layer * 12;
    }

    public static int cultivateCultivationCost(int currentLayer) {
        int layer = Math.max(1, currentLayer);
        return 40 + layer * 30;
    }

    public static int getMethodLayer(ServerPlayer player, String methodId) {
        if (player == null || methodId == null || methodId.isBlank()) {
            return 0;
        }
        String key = methodId.trim().toLowerCase(Locale.ROOT);
        if (!hasLearnedMethod(player, key)) {
            return 0;
        }
        CompoundTag layers = player.getPersistentData().getCompound(METHOD_LAYERS_TAG);
        int layer = layers.getInt(key);
        return layer <= 0 ? 1 : Math.min(MAX_METHOD_LAYER, layer);
    }

    public static void setMethodLayer(ServerPlayer player, String methodId, int layer) {
        if (player == null || methodId == null || methodId.isBlank()) {
            return;
        }
        String key = methodId.trim().toLowerCase(Locale.ROOT);
        CompoundTag layers = player.getPersistentData().getCompound(METHOD_LAYERS_TAG).copy();
        layers.putInt(key, Math.max(1, Math.min(MAX_METHOD_LAYER, layer)));
        player.getPersistentData().put(METHOD_LAYERS_TAG, layers);
        // Ensure learned flag stays true when layer is set.
        if (!hasLearnedMethod(player, key)) {
            CompoundTag learned = player.getPersistentData().getCompound(LEARNED_METHODS_TAG).copy();
            learned.putBoolean(key, true);
            player.getPersistentData().put(LEARNED_METHODS_TAG, learned);
        }
    }

    /** Wave481: ordered id -> layer map for network sync. */
    public static Map<String, Integer> learnedMethodLayers(ServerPlayer player) {
        Map<String, Integer> map = new java.util.LinkedHashMap<>();
        for (String id : learnedMethodIds(player)) {
            map.put(id, getMethodLayer(player, id));
        }
        return map;
    }

    public static boolean hasLearnedMethod(ServerPlayer player, String methodId) {
        if (player == null || methodId == null || methodId.isBlank()) {
            return false;
        }
        return player.getPersistentData().getCompound(LEARNED_METHODS_TAG)
                .getBoolean(methodId.trim().toLowerCase(Locale.ROOT));
    }

    public static void markLearnedMethod(ServerPlayer player, String methodId) {
        if (player == null || methodId == null || methodId.isBlank()) {
            return;
        }
        CompoundTag tag = player.getPersistentData().getCompound(LEARNED_METHODS_TAG).copy();
        String key = methodId.trim().toLowerCase(Locale.ROOT);
        boolean already = tag.getBoolean(key);
        tag.putBoolean(key, true);
        player.getPersistentData().put(LEARNED_METHODS_TAG, tag);
        if (!already) {
            // Wave481: first learn starts at layer 1.
            CompoundTag layers = player.getPersistentData().getCompound(METHOD_LAYERS_TAG).copy();
            if (layers.getInt(key) <= 0) {
                layers.putInt(key, 1);
                player.getPersistentData().put(METHOD_LAYERS_TAG, layers);
            }
            SyncLearnedMethodsPacket.send(player);
        }
    }

    public static int learnedMethodCount(ServerPlayer player) {
        return learnedMethodIds(player).size();
    }

    /** Wave477: sorted learned method ids for network sync / UI. */
    public static List<String> learnedMethodIds(ServerPlayer player) {
        if (player == null) {
            return List.of();
        }
        CompoundTag tag = player.getPersistentData().getCompound(LEARNED_METHODS_TAG);
        List<String> ids = new ArrayList<>();
        for (String key : tag.getAllKeys()) {
            if (tag.getBoolean(key) && key != null && !key.isBlank()) {
                ids.add(key.trim().toLowerCase(Locale.ROOT));
            }
        }
        ids.sort(String::compareTo);
        return List.copyOf(ids);
    }

    public static void syncLearnedMethods(ServerPlayer player) {
        SyncLearnedMethodsPacket.send(player);
    }

    /**
     * Wave474: grant starter method when a player becomes outer disciple of a sect.
     * Returns method id granted, or empty if none/already known.
     */
    public static Optional<String> grantSectStarterMethod(ServerPlayer player, String sectId) {
        if (player == null || sectId == null || sectId.isBlank()) {
            return Optional.empty();
        }
        String methodId = starterMethodForSect(sectId);
        if (methodId.isBlank()) {
            return Optional.empty();
        }
        if (hasLearnedMethod(player, methodId)) {
            return Optional.empty();
        }
        // Prefer catalog-known methods; still mark even if index omits rich entry.
        markLearnedMethod(player, methodId);
        String display = TextMaterialCatalogService.builtin().findMethod(methodId)
                .map(TextMaterialCatalogService.MethodEntry::display)
                .filter(s -> !s.isBlank())
                .orElse(methodId);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.method.sect_granted", display, sectId), true);
        return Optional.of(methodId);
    }

    private static int grantUnlockMethods(ServerPlayer player, java.util.List<String> unlocks) {
        if (player == null || unlocks == null || unlocks.isEmpty()) {
            return 0;
        }
        int granted = 0;
        for (String raw : unlocks) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String token = raw.trim().toLowerCase(Locale.ROOT);
            // Direct method id.
            if (TextMaterialCatalogService.builtin().findMethod(token).isPresent()
                    || looksLikeMethodId(token)) {
                if (!hasLearnedMethod(player, token)) {
                    markLearnedMethod(player, token);
                    granted++;
                }
                continue;
            }
            // Soft map: unlock token contains a known method id.
            for (TextMaterialCatalogService.MethodEntry method
                    : TextMaterialCatalogService.builtin().methods().values()) {
                String mid = method.id() == null ? "" : method.id().toLowerCase(Locale.ROOT);
                if (mid.isBlank()) {
                    continue;
                }
                if (token.contains(mid) || mid.contains(token)) {
                    if (!hasLearnedMethod(player, mid)) {
                        markLearnedMethod(player, mid);
                        granted++;
                    }
                    break;
                }
            }
        }
        return granted;
    }

    private static boolean looksLikeMethodId(String token) {
        return token.endsWith("_art")
                || token.endsWith("_gong")
                || token.endsWith("_jue")
                || token.endsWith("_method")
                || token.contains("_body_art")
                || token.contains("_soul_art")
                || token.contains("_blood_art")
                || token.contains("_phantom_art")
                || token.contains("_seal_art")
                || token.contains("_sword_art");
    }

    /**
     * Wave475: when a player learns techniques from a technique-manual source string,
     * also mark matching cultivation methods as learned for TechniqueGate.
     */
    public static int grantMethodsFromTechniqueSource(ServerPlayer player, String source) {
        if (player == null || source == null || source.isBlank()) {
            return 0;
        }
        String blob = source.trim().toLowerCase(Locale.ROOT);
        int granted = 0;
        // Direct known method ids appearing in source text.
        for (TextMaterialCatalogService.MethodEntry method
                : TextMaterialCatalogService.builtin().methods().values()) {
            String mid = method.id() == null ? "" : method.id().toLowerCase(Locale.ROOT);
            if (mid.isBlank()) {
                continue;
            }
            if (blob.contains(mid) || blob.contains(mid.replace('_', ' '))) {
                if (!hasLearnedMethod(player, mid)) {
                    markLearnedMethod(player, mid);
                    granted++;
                }
            }
        }
        // Keyword map for common Chinese/English source labels.
        String mapped = methodFromSourceKeywords(blob);
        if (!mapped.isBlank() && !hasLearnedMethod(player, mapped)) {
            markLearnedMethod(player, mapped);
            granted++;
        }
        return granted;
    }

    private static String methodFromSourceKeywords(String blob) {
        if (blob.contains("长春") || blob.contains("changchun") || blob.contains("黄枫")) {
            return "changchun_gong";
        }
        if (blob.contains("青元") || blob.contains("qingyuan") || blob.contains("剑诀") || blob.contains("剑宗") || blob.contains("巨剑") || blob.contains("凌霄")) {
            return "qingyuan_sword_art";
        }
        if (blob.contains("玄阴") || blob.contains("xuan_yin") || blob.contains("xuanyin")) {
            return "xuan_yin_art";
        }
        if (blob.contains("大衍") || blob.contains("dayan") || blob.contains("傀儡")) {
            return "dayan_art";
        }
        if (blob.contains("烈焰") || blob.contains("lieyan") || blob.contains("魔焰")) {
            return "lieyan_gong";
        }
        if (blob.contains("鬼灵") || blob.contains("guiling") || blob.contains("摄魂")) {
            return "guiling_soul_art";
        }
        if (blob.contains("天魔") || blob.contains("tianmo")) {
            return "tianmo_body_art";
        }
        if (blob.contains("血巫") || blob.contains("xuewu")) {
            return "xuewu_blood_art";
        }
        if (blob.contains("万狐") || blob.contains("wanhu") || blob.contains("千幻")) {
            return "wanhu_phantom_art";
        }
        if (blob.contains("星宫") || blob.contains("star_palace")) {
            return "star_palace_art";
        }
        if (blob.contains("逆星") || blob.contains("inverse_star")) {
            return "inverse_star_art";
        }
        if (blob.contains("掩月") || blob.contains("yanyue") || blob.contains("幻术")) {
            return "yanyue_illusion_art";
        }
        if (blob.contains("清虚") || blob.contains("qingxu") || blob.contains("符箓")) {
            return "qingxu_pure_tao_art";
        }
        if (blob.contains("千竹") || blob.contains("qianzhu")) {
            return "qianzhu_puppet_art";
        }
        if (blob.contains("御灵") || blob.contains("yuling") || blob.contains("灵兽")) {
            return "yuling_beast_puppet_art";
        }
        if (blob.contains("合欢") || blob.contains("hehuan")) {
            return "hehuan_mind_art";
        }
        if (blob.contains("七玄") || blob.contains("qixuan")) {
            return "qixuan_mortal_art";
        }
        if (blob.contains("木兰") || blob.contains("mulan")) {
            return "mulan_wind_spirit_art";
        }
        if (blob.contains("天岚") || blob.contains("tianlan")) {
            return "tianlan_holy_beast_art";
        }
        if (blob.contains("昆吾") || blob.contains("kunwu") || blob.contains("阵法") || blob.contains("seal")) {
            return "kunwu_ice_guard_art";
        }
        if (blob.contains("佛") || blob.contains("buddhist") || blob.contains("金刚")) {
            return "dajin_clan_ancestor_art";
        }
        if (blob.contains("青萝") || blob.contains("qingluo") || blob.contains("毒")) {
            return "qingluo_poison_art";
        }
        if (blob.contains("乱星") || blob.contains("chaotic")) {
            return "chaotic_sea_nav_art";
        }
        return "";
    }

    private static String starterMethodForSect(String sectId) {
        String id = sectId == null ? "" : sectId.trim().toLowerCase(Locale.ROOT);
        return switch (id) {
            case "huangfeng_valley" -> "changchun_gong";
            case "qinglan_sect" -> "kunwu_ice_guard_art";
            case "yanyue_sect" -> "yanyue_illusion_art";
            case "guiling_gate" -> "guiling_soul_art";
            case "tianmo_sect" -> "tianmo_body_art";
            case "xuewu_sect" -> "xuewu_blood_art";
            case "wanhu_sect" -> "wanhu_phantom_art";
            case "star_palace" -> "star_palace_art";
            case "inverse_star_alliance" -> "inverse_star_art";
            case "qingxu_gate" -> "qingxu_pure_tao_art";
            case "qianzhu_sect" -> "qianzhu_puppet_art";
            case "spirit_beast_mountain", "yuling_pavilion" -> "yuling_beast_puppet_art";
            case "giant_sword_gate", "lingxiao_sword_sect" -> "qingyuan_sword_art";
            case "hehuan_sect" -> "hehuan_mind_art";
            case "luoyun_sect" -> "huangfeng_alchemy_scripture";
            case "qixuan_men" -> "qixuan_mortal_art";
            case "dajin_buddhist_temple_line" -> "dajin_clan_ancestor_art";
            case "moyan_gate" -> "lieyan_gong";
            case "qianhuan_sect" -> "wanhu_phantom_art";
            case "tiansha_sect" -> "tianmo_body_art";
            case "mulan_fashi_council" -> "mulan_wind_spirit_art";
            case "tianlan_temple" -> "tianlan_holy_beast_art";
            case "qingluo_sect" -> "qingluo_poison_art";
            case "huadao_wu" -> "huadao_blade_intent";
            default -> "";
        };
    }

    private static void applyInsight(ServerPlayer player, TextMaterialCatalogService.ManualEntry manual) {
        String type = manual.type() == null ? "" : manual.type().toLowerCase(Locale.ROOT);
        int duration = 20 * 60 * 5;
        if (type.contains("refinement") || type.contains("forge")) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, duration, 0));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0));
        } else if (type.contains("puppet")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 0));
        } else if (type.contains("ghost") || type.contains("cultivation")) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 0));
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, duration, 0));
            player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, Math.min(duration, 20 * 90), 0));
        }
    }
}
