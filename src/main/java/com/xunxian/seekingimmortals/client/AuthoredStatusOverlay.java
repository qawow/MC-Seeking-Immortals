package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.client.gui.overlay.ForgeGui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Compact authored status strip: six readable icons, then a bounded overflow count. */
public final class AuthoredStatusOverlay {
    private static final int MAX_VISIBLE = 6;
    private static final int ICON_SIZE = 18;

    private AuthoredStatusOverlay() {}

    public static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.getActiveEffects().isEmpty()) {
            return;
        }
        List<MobEffectInstance> effects = new ArrayList<>(player.getActiveEffects());
        effects.sort(Comparator
                .comparing((MobEffectInstance effect) -> effect.getEffect().getCategory()
                        == MobEffectCategory.HARMFUL ? 0 : 1)
                .thenComparingInt(MobEffectInstance::getAmplifier).reversed());
        int visible = Math.min(MAX_VISIBLE, effects.size());
        int x = width - ICON_SIZE - 4;
        int y = 4;
        for (int index = 0; index < visible; index++) {
            MobEffectInstance instance = effects.get(index);
            boolean harmful = instance.getEffect().getCategory() == MobEffectCategory.HARMFUL;
            int background = harmful ? 0xB52C2026 : 0xB52E5A46;
            graphics.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, background);
            ResourceLocation effectId = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS
                    .getKey(instance.getEffect());
            if (effectId != null) {
                ResourceLocation texture = new ResourceLocation(effectId.getNamespace(),
                        "textures/mob_effect/" + effectId.getPath() + ".png");
                graphics.blit(texture, x + 1, y + 1, 0, 0, 16, 16, 18, 18);
            }
            if (instance.getAmplifier() > 0) {
                graphics.drawString(Minecraft.getInstance().font,
                        Integer.toString(instance.getAmplifier() + 1), x + 11, y + 9,
                        0xFFFFFFFF, true);
            }
            x -= ICON_SIZE + 2;
        }
        int overflow = effects.size() - visible;
        if (overflow > 0) {
            String label = "+" + Math.min(99, overflow);
            int labelWidth = Minecraft.getInstance().font.width(label);
            int labelX = Math.max(2, x - labelWidth - 4);
            graphics.drawString(Minecraft.getInstance().font, label, labelX, y + 5, 0xFFE8D48B, true);
        }
    }
}
