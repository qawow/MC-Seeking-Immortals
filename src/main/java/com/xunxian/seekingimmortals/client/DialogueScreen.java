package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.network.DialogueActionPacket;
import com.xunxian.seekingimmortals.network.ModNetwork;
import com.xunxian.seekingimmortals.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.List;
import java.util.Locale;

/**
 * Visual dialogue GUI for text-quest dialogue trees.
 * Wave56: portrait textures + dialogue voice cues.
 */
public class DialogueScreen extends Screen {
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 240;
    private static final ResourceLocation PORTRAIT_DEFAULT =
            new ResourceLocation(SeekingImmortalsMod.MODID, "textures/gui/dialogue/portrait_default.png");
    private static final ResourceLocation PORTRAIT_MO_LAO =
            new ResourceLocation(SeekingImmortalsMod.MODID, "textures/gui/dialogue/portrait_mo_lao.png");
    private static final ResourceLocation PORTRAIT_MULAN =
            new ResourceLocation(SeekingImmortalsMod.MODID, "textures/gui/dialogue/portrait_mulan.png");
    private static final ResourceLocation PORTRAIT_YINLUO =
            new ResourceLocation(SeekingImmortalsMod.MODID, "textures/gui/dialogue/portrait_yinluo.png");
    private static final ResourceLocation PORTRAIT_STAR =
            new ResourceLocation(SeekingImmortalsMod.MODID, "textures/gui/dialogue/portrait_star_broker.png");
    private static final ResourceLocation PORTRAIT_KUNWU =
            new ResourceLocation(SeekingImmortalsMod.MODID, "textures/gui/dialogue/portrait_kunwu.png");

    private final String chainId;
    private boolean greetingPlayed;

    public DialogueScreen(String chainId) {
        super(Component.translatable("screen.seeking_immortals.dialogue.title"));
        this.chainId = chainId == null ? "" : chainId;
    }

    @Override
    protected void init() {
        super.init();
        int left = left();
        int top = top();
        int panelWidth = panelWidth();
        int small = Math.max(54, Math.min(72, (panelWidth - 32) / 4));

        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.dialogue.refresh"), button -> {
                    playVoice(ModSounds.DIALOGUE_GREETING.get());
                    ModNetwork.CHANNEL.sendToServer(new DialogueActionPacket(
                            DialogueActionPacket.ACTION_TALK, chainId, ""));
                })
                .bounds(left + panelWidth - small * 2 - 18, top + 8, small, 18)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.dialogue.close"), button -> {
                    ModNetwork.CHANNEL.sendToServer(new DialogueActionPacket(
                            DialogueActionPacket.ACTION_CLOSE, chainId, ""));
                    onClose();
                })
                .bounds(left + panelWidth - small - 12, top + 8, small, 18)
                .build());

        int y = top + 150;
        int bw = Math.min(100, Math.max(70, (panelWidth - 40) / 3));
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.dialogue.start"), button -> {
                    playVoice(npcVoice());
                    ModNetwork.CHANNEL.sendToServer(new DialogueActionPacket(
                            DialogueActionPacket.ACTION_ACT, chainId, "start"));
                })
                .bounds(left + 16, y, bw, 18)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.dialogue.advance"), button -> {
                    playVoice(ModSounds.DIALOGUE_ADVANCE.get());
                    ModNetwork.CHANNEL.sendToServer(new DialogueActionPacket(
                            DialogueActionPacket.ACTION_ACT, chainId, "advance"));
                })
                .bounds(left + 16 + bw + 8, y, bw, 18)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.dialogue.righteous"), button -> {
                    playVoice(ModSounds.DIALOGUE_BRANCH.get());
                    ModNetwork.CHANNEL.sendToServer(new DialogueActionPacket(
                            DialogueActionPacket.ACTION_ACT, chainId, "righteous"));
                })
                .bounds(left + 16, y + 24, bw, 18)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.dialogue.neutral"), button -> {
                    playVoice(ModSounds.DIALOGUE_BRANCH.get());
                    ModNetwork.CHANNEL.sendToServer(new DialogueActionPacket(
                            DialogueActionPacket.ACTION_ACT, chainId, "neutral"));
                })
                .bounds(left + 16 + bw + 8, y + 24, bw, 18)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.dialogue.demonic"), button -> {
                    playVoice(ModSounds.DIALOGUE_BRANCH.get());
                    ModNetwork.CHANNEL.sendToServer(new DialogueActionPacket(
                            DialogueActionPacket.ACTION_ACT, chainId, "demonic"));
                })
                .bounds(left + 16 + (bw + 8) * 2, y + 24, bw, 18)
                .build());

        if (!greetingPlayed) {
            greetingPlayed = true;
            playVoice(npcVoice());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int left = left();
        int top = top();
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        ImmortalUiSkin.drawPanel(graphics, left, top, panelWidth, panelHeight);
        graphics.drawCenteredString(font, Component.translatable("screen.seeking_immortals.dialogue.title"),
                left + panelWidth / 2, top + 12, ImmortalUiSkin.COLOR_TITLE);

        ImmortalUiSkin.drawStringFit(font, graphics,
                Component.translatable("screen.seeking_immortals.dialogue.chain", chainId).getString(),
                left + 14, top + 34, Math.max(1, panelWidth - 28), ImmortalUiSkin.COLOR_TEXT_MUTED, false);

        int portraitX = left + panelWidth - 92;
        int portraitY = top + 48;
        graphics.blit(portraitForChain(), portraitX, portraitY, 0, 0, 72, 88, 72, 88);
        graphics.renderOutline(portraitX, portraitY, 72, 88, ImmortalUiSkin.COLOR_TITLE);

        List<String> prompts = List.of(
                Component.translatable("screen.seeking_immortals.dialogue.hint_talk").getString(),
                Component.translatable("screen.seeking_immortals.dialogue.hint_branch").getString(),
                Component.translatable("screen.seeking_immortals.dialogue.hint_actions").getString()
        );
        int y = top + 54;
        for (String line : prompts) {
            ImmortalUiSkin.drawStringFit(font, graphics, line, left + 16, y, Math.max(1, panelWidth - 120),
                    ImmortalUiSkin.COLOR_TEXT_NORMAL, false);
            y += 14;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private ResourceLocation portraitForChain() {
        String id = chainId == null ? "" : chainId.toLowerCase(Locale.ROOT);
        if (id.contains("huangfeng") || id.contains("qixuan")) {
            return PORTRAIT_MO_LAO;
        }
        if (id.contains("mulan") || id.contains("tianlan")) {
            return PORTRAIT_MULAN;
        }
        if (id.contains("ghost") || id.contains("yin")) {
            return PORTRAIT_YINLUO;
        }
        if (id.contains("star") || id.contains("chaotic") || id.contains("void")) {
            return PORTRAIT_STAR;
        }
        if (id.contains("dajin") || id.contains("kunwu")) {
            return PORTRAIT_KUNWU;
        }
        return PORTRAIT_DEFAULT;
    }

    private SoundEvent npcVoice() {
        String id = chainId == null ? "" : chainId.toLowerCase(Locale.ROOT);
        if (id.contains("huangfeng") || id.contains("qixuan")) {
            return ModSounds.DIALOGUE_NPC_MO_LAO.get();
        }
        return ModSounds.DIALOGUE_NPC_GUIDE.get();
    }

    private void playVoice(SoundEvent sound) {
        if (sound == null) {
            return;
        }
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int left() {
        return Math.max(0, (this.width - panelWidth()) / 2);
    }

    private int top() {
        return Math.max(0, (this.height - panelHeight()) / 2);
    }

    private int panelWidth() {
        return Math.max(1, Math.min(PANEL_WIDTH, this.width - 24));
    }

    private int panelHeight() {
        return Math.max(1, Math.min(PANEL_HEIGHT, this.height - 24));
    }
}
