package com.xunxian.seekingimmortals.artifact;

import com.xunxian.seekingimmortals.catalog.AuctionSoftService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.network.SyncCultivationDataPacket;
import com.xunxian.seekingimmortals.skill.LifeSkillService;
import com.xunxian.seekingimmortals.skill.SkillType;
import com.xunxian.seekingimmortals.worldpack.AuctionHouseSavedData;
import com.xunxian.seekingimmortals.worldpack.ReputationService;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.Optional;

/**
 * Wave489: artifact / auction appraisal authority.
 * Marks stacks as identified, reveals tier/type/effect, and previews auction lot value.
 */
public final class ArtifactAppraisalService {
    public static final String TAG_APPRAISED = "SeekingImmortalsAppraised";
    public static final String TAG_APPRAISED_TIER = "SeekingImmortalsAppraisedTier";
    public static final String TAG_APPRAISED_TYPE = "SeekingImmortalsAppraisedType";
    public static final String TAG_APPRAISED_EFFECT = "SeekingImmortalsAppraisedEffect";
    public static final String TAG_APPRAISED_VALUE = "SeekingImmortalsAppraisedValue";

    private ArtifactAppraisalService() {}

    public static boolean isAppraisalTool(String artifactOrItemId) {
        String id = normalize(artifactOrItemId);
        return id.contains("appraisal") || id.contains("identify");
    }

    public static boolean isAppraised(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getTag() != null && stack.getTag().getBoolean(TAG_APPRAISED);
    }

    public static boolean appraise(ServerPlayer player, ItemStack tool, ItemStack target) {
        if (player == null || tool == null || target == null || target.isEmpty()) {
            return false;
        }
        if (isAppraised(target) && !player.getAbilities().instabuild) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.appraisal.already"), true);
            return false;
        }

        Optional<ArtifactDataService.ArtifactDefinition> artifact = resolveArtifact(target);
        String tierText = artifact.map(ArtifactDataService.ArtifactDefinition::tier).filter(s -> s != null && !s.isBlank()).orElse("1");
        int tier = parseTierNumber(tierText);
        // Wave490: higher-tier treasures need ARTIFACT_REFINING level gate (tier 4+ needs L3, tier 6+ needs L5).
        int required = tier >= 6 ? 5 : (tier >= 4 ? 3 : 0);
        if (!LifeSkillService.meetsLevel(player, SkillType.ARTIFACT_REFINING, required)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.appraisal.skill_gate", required, Math.max(1, tier)), true);
            LifeSkillService.grantPractice(player, SkillType.ARTIFACT_REFINING, 6, 2);
            return false;
        }

        String type = artifact.map(ArtifactDataService.ArtifactDefinition::type).orElse("unknown");
        String effect = artifact.map(ArtifactDataService.ArtifactDefinition::effect).orElse("");
        String artifactId = artifact.map(ArtifactDataService.ArtifactDefinition::id).orElse(guessId(target));

        // Partial misidentify chance when skill is low relative to tier.
        int skillLv = LifeSkillService.level(player, SkillType.ARTIFACT_REFINING);
        boolean partial = !player.getAbilities().instabuild && skillLv < tier && player.getRandom().nextDouble() < 0.18D;
        if (partial) {
            effect = "";
            type = "obscured";
        }

        int estimatedValue = estimateValue(player, artifactId, tier);
        if (partial) {
            estimatedValue = Math.max(5, estimatedValue / 2);
        }
        CompoundTag tag = target.getOrCreateTag();
        tag.putBoolean(TAG_APPRAISED, true);
        tag.putBoolean(TAG_APPRAISED + "Partial", partial);
        tag.putInt(TAG_APPRAISED_TIER, Math.max(1, tier));
        tag.putString(TAG_APPRAISED_TYPE, type == null || type.isBlank() ? "unknown" : type);
        tag.putString(TAG_APPRAISED_EFFECT, effect == null ? "" : effect);
        tag.putInt(TAG_APPRAISED_VALUE, estimatedValue);

        CultivationHelper.get(player).ifPresent(cultivation -> {
            if (!player.getAbilities().instabuild) {
                cultivation.consumeSpiritualPower(Math.max(5, 8 + tier * 2));
            }
            SyncCultivationDataPacket.send(player, cultivation);
        });
        LifeSkillService.grantPractice(player, SkillType.ARTIFACT_REFINING, partial ? 10 : 16, partial ? 4 : 8);
        ReputationService.add(player, "merchant_guild", 1);
        ReputationService.add(player, "chaotic_sea", 1);

        player.displayClientMessage(Component.translatable(
                partial ? "message.seeking_immortals.appraisal.partial" : "message.seeking_immortals.appraisal.success",
                target.getHoverName(),
                Math.max(1, tier),
                ArtifactDisplayTexts.type(type == null || type.isBlank() ? "unknown" : type),
                estimatedValue), false);
        if (effect != null && !effect.isBlank()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.appraisal.effect", ArtifactDisplayTexts.effect(effect))
                    .withStyle(ChatFormatting.AQUA), false);
        }
        Optional<AuctionSoftService.Lot> lot = AuctionSoftService.builtin().findLot(artifactId);
        if (lot.isPresent()) {
            AuctionHouseSavedData house = AuctionHouseSavedData.get(player.server.overworld());
            int live = house.currentAmount(lot.get().id());
            long shown = live > 0 ? live : lot.get().minEquiv();
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.appraisal.auction_lot",
                    lot.get().display(),
                    shown,
                    lot.get().maxEquiv()), false);
        }
        return true;
    }

    public static int estimateValue(ServerPlayer player, String artifactId, int tier) {
        Optional<AuctionSoftService.Lot> lot = AuctionSoftService.builtin().findLot(artifactId);
        if (lot.isPresent()) {
            int live = 0;
            if (player != null && player.server != null) {
                live = AuctionHouseSavedData.get(player.server.overworld()).currentAmount(lot.get().id());
            }
            return (int) Math.max(lot.get().minEquiv(), live);
        }
        return Math.max(10, Math.max(1, tier) * 35);
    }

    public static Optional<ArtifactDataService.ArtifactDefinition> resolveArtifact(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        if (stack.getItem() instanceof com.xunxian.seekingimmortals.item.ArtifactCatalogItem catalogItem) {
            return ArtifactDataService.builtin().findArtifact(catalogItem.artifactId());
        }
        return ArtifactDataService.builtin().findArtifact(guessId(stack));
    }

    private static int parseTierNumber(String tierText) {
        if (tierText == null || tierText.isBlank()) {
            return 1;
        }
        try {
            String digits = tierText.replaceAll("[^0-9]", "");
            if (!digits.isBlank()) {
                return Math.max(1, Integer.parseInt(digits));
            }
        } catch (NumberFormatException ignored) {
        }
        return 1;
    }

    private static String guessId(ItemStack stack) {
        String desc = stack.getDescriptionId();
        if (desc == null) {
            return "";
        }
        int idx = desc.lastIndexOf('.');
        return idx >= 0 ? desc.substring(idx + 1).toLowerCase(Locale.ROOT) : desc.toLowerCase(Locale.ROOT);
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
