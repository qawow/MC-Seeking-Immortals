package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.cultivation.*;
import com.xunxian.seekingimmortals.quest.QuestProgress;
import com.xunxian.seekingimmortals.sect.SectContributionService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * M08 物品使用限制门禁服务。
 * 提供境界、灵根、功法、宗门等使用条件检查，适用于高级物品、法宝、秘籍等。
 */
public final class ItemUsageGateService {
    private ItemUsageGateService() {}

    public record GateResult(boolean allowed, Component message) {
        public static GateResult ok() {
            return new GateResult(true, Component.empty());
        }

        public static GateResult deny(String key, Object... args) {
            return new GateResult(false, Component.translatable(key, args));
        }

        public static GateResult denyLiteral(String text) {
            return new GateResult(false, Component.literal(text));
        }
    }

    public record ItemRequirement(
            Realm minRealm,
            Set<String> requiredRoots,
            Set<String> requiredMethods,
            String requiredSect,
            int minSectStage,
            String requiredPath,
            String requiredRace
    ) {
        public ItemRequirement {
            requiredRoots = requiredRoots == null ? Set.of() : Set.copyOf(requiredRoots);
            requiredMethods = requiredMethods == null ? Set.of() : Set.copyOf(requiredMethods);
            requiredSect = requiredSect == null ? "" : requiredSect.trim();
            requiredPath = requiredPath == null ? "" : requiredPath.trim();
            requiredRace = requiredRace == null ? "" : requiredRace.trim();
            minSectStage = Math.max(0, minSectStage);
        }

        public static ItemRequirement none() {
            return new ItemRequirement(null, Set.of(), Set.of(), "", 0, "", "");
        }

        public static ItemRequirement realm(Realm realm) {
            return new ItemRequirement(realm, Set.of(), Set.of(), "", 0, "", "");
        }

        public static ItemRequirement realmAndRoot(Realm realm, String... roots) {
            return new ItemRequirement(realm, Set.of(roots), Set.of(), "", 0, "", "");
        }

        public boolean hasAnyRestriction() {
            return minRealm != null
                    || !requiredRoots.isEmpty()
                    || !requiredMethods.isEmpty()
                    || !requiredSect.isBlank()
                    || !requiredPath.isBlank()
                    || !requiredRace.isBlank();
        }
    }

    /**
     * 检查玩家是否满足物品使用条件。
     */
    public static GateResult canUse(Player player, ItemRequirement requirement) {
        if (player == null) {
            return GateResult.deny("message.seeking_immortals.item_gate.no_player");
        }
        if (player instanceof ServerPlayer sp && sp.getAbilities().instabuild) {
            return GateResult.ok();
        }
        if (requirement == null || !requirement.hasAnyRestriction()) {
            return GateResult.ok();
        }

        Optional<PlayerCultivation> cultOpt = CultivationHelper.get(player);
        if (cultOpt.isEmpty()) {
            return GateResult.deny("message.seeking_immortals.item_gate.no_cultivation");
        }
        PlayerCultivation cultivation = cultOpt.get();

        // 境界检查
        if (requirement.minRealm() != null) {
            GateResult realmCheck = checkRealm(cultivation, requirement.minRealm());
            if (!realmCheck.allowed()) {
                return realmCheck;
            }
        }

        // 灵根检查（任意一个满足即可）
        if (!requirement.requiredRoots().isEmpty()) {
            GateResult rootCheck = checkRoots(cultivation, requirement.requiredRoots());
            if (!rootCheck.allowed()) {
                return rootCheck;
            }
        }

        // 功法检查（需要学习任意一个）
        if (!requirement.requiredMethods().isEmpty()) {
            GateResult methodCheck = checkMethods(player, cultivation, requirement.requiredMethods());
            if (!methodCheck.allowed()) {
                return methodCheck;
            }
        }

        // 宗门检查
        if (!requirement.requiredSect().isBlank() || requirement.minSectStage() > 0) {
            GateResult sectCheck = checkSect(cultivation, requirement.requiredSect(), requirement.minSectStage());
            if (!sectCheck.allowed()) {
                return sectCheck;
            }
        }

        // 修炼路线检查
        if (!requirement.requiredPath().isBlank()) {
            if (!ProgressionGateApi.meetsPath(cultivation, requirement.requiredPath())) {
                return GateResult.deny("message.seeking_immortals.item_gate.path",
                        requirement.requiredPath());
            }
        }

        // 种族检查
        if (!requirement.requiredRace().isBlank()) {
            if (!ProgressionGateApi.meetsRace(cultivation, requirement.requiredRace())) {
                return GateResult.deny("message.seeking_immortals.item_gate.race",
                        requirement.requiredRace());
            }
        }

        return GateResult.ok();
    }

    /**
     * 快速境界检查。
     */
    public static GateResult checkRealm(PlayerCultivation cultivation, Realm minRealm) {
        if (minRealm == null) {
            return GateResult.ok();
        }
        Realm current = cultivation.getRealm();
        if (current.ordinal() < minRealm.ordinal()) {
            return GateResult.deny("message.seeking_immortals.item_gate.realm",
                    minRealm.getDisplayName(),
                    current.getDisplayName());
        }
        return GateResult.ok();
    }

    /**
     * 灵根检查（任意一个满足即可）。
     */
    private static GateResult checkRoots(PlayerCultivation cultivation, Set<String> requiredRoots) {
        if (requiredRoots == null || requiredRoots.isEmpty()) {
            return GateResult.ok();
        }
        for (String rootReq : requiredRoots) {
            if (ProgressionGateApi.meetsRoot(cultivation, rootReq)) {
                return GateResult.ok();
            }
        }
        return GateResult.deny("message.seeking_immortals.item_gate.root",
                String.join("/", requiredRoots));
    }

    /**
     * 功法检查（需要学习任意一个）。
     */
    private static GateResult checkMethods(Player player, PlayerCultivation cultivation,
                                           Set<String> requiredMethods) {
        if (requiredMethods == null || requiredMethods.isEmpty()) {
            return GateResult.ok();
        }
        for (String method : requiredMethods) {
            String normalized = method.trim().toLowerCase(Locale.ROOT);
            if (normalized.isBlank()) {
                continue;
            }
            // 检查已学技能
            if (cultivation.hasLearnedTechnique(normalized)) {
                return GateResult.ok();
            }
            // 检查已学功法（通过 ManualCatalogService）
            if (player instanceof ServerPlayer sp) {
                try {
                    if (com.xunxian.seekingimmortals.catalog.ManualCatalogService.hasLearnedMethod(sp, normalized)) {
                        return GateResult.ok();
                    }
                } catch (Throwable ignored) {
                    // optional in tests
                }
            }
            // 模糊匹配已学技能ID
            for (String learned : cultivation.getLearnedTechniques()) {
                String token = learned.trim().toLowerCase(Locale.ROOT);
                if (token.contains(normalized) || normalized.contains(token)) {
                    return GateResult.ok();
                }
            }
        }
        return GateResult.deny("message.seeking_immortals.item_gate.method",
                String.join("/", requiredMethods));
    }

    /**
     * 宗门检查。
     */
    private static GateResult checkSect(PlayerCultivation cultivation, String requiredSect, int minStage) {
        QuestProgress progress = cultivation.getSevenMysteriesQuest();
        if (progress == null) {
            if (!requiredSect.isBlank() || minStage > 0) {
                return GateResult.deny("message.seeking_immortals.item_gate.no_sect");
            }
            return GateResult.ok();
        }

        String currentSect = progress.getSectId() == null ? "" : progress.getSectId().trim().toLowerCase(Locale.ROOT);
        int currentStage = progress.getSectQuestStage();

        if (!requiredSect.isBlank()) {
            String required = requiredSect.trim().toLowerCase(Locale.ROOT);
            if (!currentSect.equals(required) && !currentSect.contains(required) && !required.contains(currentSect)) {
                return GateResult.deny("message.seeking_immortals.item_gate.sect",
                        requiredSect, currentSect.isBlank() ? "-" : currentSect);
            }
        }

        if (minStage > 0 && currentStage < minStage) {
            String stageName = getStageName(minStage);
            return GateResult.deny("message.seeking_immortals.item_gate.sect_stage",
                    stageName, currentStage);
        }

        return GateResult.ok();
    }

    private static String getStageName(int stage) {
        if (stage >= SectContributionService.STAGE_PHASE10_COMPLETE) {
            return "核心弟子";
        }
        if (stage >= SectContributionService.STAGE_INNER_DISCIPLE) {
            return "内门弟子";
        }
        if (stage >= SectContributionService.STAGE_OUTER_DISCIPLE) {
            return "外门弟子";
        }
        return "记名弟子";
    }

    /**
     * 为物品堆栈添加使用限制提示。
     */
    public static void appendRequirementTooltip(ItemStack stack, List<Component> tooltip, ItemRequirement requirement) {
        if (requirement == null || !requirement.hasAnyRestriction()) {
            return;
        }
        if (requirement.minRealm() != null) {
            tooltip.add(Component.translatable("tooltip.seeking_immortals.item_requirement.realm",
                    requirement.minRealm().getDisplayName()));
        }
        if (!requirement.requiredRoots().isEmpty()) {
            tooltip.add(Component.translatable("tooltip.seeking_immortals.item_requirement.root",
                    String.join("/", requirement.requiredRoots())));
        }
        if (!requirement.requiredMethods().isEmpty()) {
            tooltip.add(Component.translatable("tooltip.seeking_immortals.item_requirement.method",
                    String.join("/", requirement.requiredMethods())));
        }
        if (!requirement.requiredSect().isBlank()) {
            tooltip.add(Component.translatable("tooltip.seeking_immortals.item_requirement.sect",
                    requirement.requiredSect()));
        }
        if (requirement.minSectStage() > 0) {
            tooltip.add(Component.translatable("tooltip.seeking_immortals.item_requirement.sect_stage",
                    getStageName(requirement.minSectStage())));
        }
        if (!requirement.requiredPath().isBlank()) {
            tooltip.add(Component.translatable("tooltip.seeking_immortals.item_requirement.path",
                    requirement.requiredPath()));
        }
        if (!requirement.requiredRace().isBlank()) {
            tooltip.add(Component.translatable("tooltip.seeking_immortals.item_requirement.race",
                    requirement.requiredRace()));
        }
    }
}
