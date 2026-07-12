package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.artifact.ArtifactDataService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FlyingArtifactItem extends Item {
    public static final String FLYING_SWORD_ARTIFACT_ID = "flying_sword_low";
    public static final String FLYING_ARTIFACT_ID = "wind_escape_sail";

    private final boolean flyingSword;
    private final String artifactId;

    public FlyingArtifactItem(Properties properties, boolean flyingSword) {
        this(properties, flyingSword, flyingSword ? FLYING_SWORD_ARTIFACT_ID : FLYING_ARTIFACT_ID);
    }

    public FlyingArtifactItem(Properties properties, boolean flyingSword, String artifactId) {
        super(properties.stacksTo(1));
        this.flyingSword = flyingSword;
        this.artifactId = artifactId;
    }

    public boolean isFlyingSword() {
        return flyingSword;
    }

    public String artifactId() {
        return artifactId;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.seeking_immortals.flying_artifact.requirement").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.seeking_immortals.flying_artifact.rule").withStyle(ChatFormatting.GRAY));
        appendArtifactDataTooltip(tooltip);
        tooltip.add(Component.translatable(flyingSword
                ? "tooltip.seeking_immortals.flying_sword.flavor"
                : "tooltip.seeking_immortals.flying_artifact.flavor").withStyle(ChatFormatting.GOLD));
    }

    private void appendArtifactDataTooltip(List<Component> tooltip) {
        ArtifactDataService.Snapshot artifactData = ArtifactDataService.builtin();
        artifactData.findArtifact(artifactId).ifPresent(artifact -> {
            tooltip.add(Component.literal("Artifact: " + artifact.display()
                    + " / " + artifactData.tierDisplay(artifact.tier())
                    + " / game tier " + artifact.gameTier()).withStyle(ChatFormatting.DARK_AQUA));
            tooltip.add(Component.literal("Realm: " + artifact.realmMin()
                    + " / type: " + artifact.type()).withStyle(ChatFormatting.DARK_GRAY));
            artifactData.findRecipeByArtifact(artifact.id()).ifPresent(recipe ->
                    tooltip.add(Component.literal("Refine: grade " + recipe.forgeGrade()
                            + " / success " + Math.round(recipe.baseSuccessRate() * 100.0D) + "%")
                            .withStyle(ChatFormatting.DARK_GRAY)));
        });
    }
}
