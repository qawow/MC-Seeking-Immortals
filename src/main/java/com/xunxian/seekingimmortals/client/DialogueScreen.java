package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.network.DialogueActionPacket;
import com.xunxian.seekingimmortals.network.ModNetwork;
import com.xunxian.seekingimmortals.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.Locale;

/** Visual dialogue GUI for text-quest dialogue trees. */
public class DialogueScreen extends Screen {
    private static final int DESIRED_WIDTH = 360;
    private static final int DESIRED_HEIGHT = 240;
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
    private int promptScroll;
    private int renderedPromptHeight;

    public DialogueScreen(String chainId) {
        super(Component.translatable("screen.seeking_immortals.dialogue.title"));
        this.chainId = chainId == null ? "" : chainId;
    }

    @Override
    protected void init() {
        super.init();
        Layout layout = calculateLayout(width, height);
        addButton(layout.refresh(), Component.translatable("screen.seeking_immortals.dialogue.refresh"), button -> {
            playVoice(ModSounds.DIALOGUE_GREETING.get());
            ModNetwork.CHANNEL.sendToServer(new DialogueActionPacket(DialogueActionPacket.ACTION_TALK, chainId, ""));
        }, false);
        addButton(layout.close(), Component.translatable("screen.seeking_immortals.dialogue.close"), button -> {
            ModNetwork.CHANNEL.sendToServer(new DialogueActionPacket(DialogueActionPacket.ACTION_CLOSE, chainId, ""));
            onClose();
        }, false);
        addButton(layout.start(), Component.translatable("screen.seeking_immortals.dialogue.start"), button -> {
            playVoice(npcVoice());
            sendAct("start");
        }, true);
        addButton(layout.advance(), Component.translatable("screen.seeking_immortals.dialogue.advance"), button -> {
            playVoice(ModSounds.DIALOGUE_ADVANCE.get());
            sendAct("advance");
        }, true);
        addBranchButton(layout.righteous(), "righteous",
                Component.translatable("screen.seeking_immortals.dialogue.righteous"));
        addBranchButton(layout.neutral(), "neutral",
                Component.translatable("screen.seeking_immortals.dialogue.neutral"));
        addBranchButton(layout.demonic(), "demonic",
                Component.translatable("screen.seeking_immortals.dialogue.demonic"));

        if (!greetingPlayed) {
            greetingPlayed = true;
            playVoice(npcVoice());
        }
    }

    private void addBranchButton(Rect rect, String action, Component label) {
        addButton(rect, label, button -> {
            playVoice(ModSounds.DIALOGUE_BRANCH.get());
            sendAct(action);
        }, false);
    }

    private void addButton(Rect rect, Component label, net.minecraft.client.gui.components.Button.OnPress onPress,
                           boolean primary) {
        addRenderableWidget(primary
                ? ImmortalButton.primary(rect.x(), rect.y(), rect.width(), rect.height(), label, onPress)
                : ImmortalButton.secondary(rect.x(), rect.y(), rect.width(), rect.height(), label, onPress));
    }

    private void sendAct(String action) {
        ModNetwork.CHANNEL.sendToServer(new DialogueActionPacket(DialogueActionPacket.ACTION_ACT, chainId, action));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        Layout layout = calculateLayout(width, height);
        Rect panel = layout.panel();
        Rect titleArea = layout.titleArea();
        Rect portrait = layout.portrait();
        Rect prompts = layout.promptViewport();

        ImmortalUiSkin.drawLayeredPanel(graphics, panel.x(), panel.y(), panel.width(), panel.height());
        int headerHeight = Math.max(12, layout.refresh().bottom() - panel.y() + layout.padding());
        ImmortalUiSkin.drawTitleBar(graphics, panel.x() + 4, panel.y() + 4,
                Math.max(1, panel.width() - 8), Math.min(headerHeight, panel.height() - 4));
        ImmortalUiSkin.drawStringFit(font, graphics, title.getString(), titleArea.x(), titleArea.y(),
                titleArea.width(), ImmortalUiSkin.JOURNAL_BORDER, false);

        if (portrait.width() > 1 && portrait.height() > 1) {
            graphics.blit(portraitForChain(), portrait.x(), portrait.y(), portrait.width(), portrait.height(),
                    0.0F, 0.0F, 72, 88, 72, 88);
            graphics.renderOutline(portrait.x(), portrait.y(), portrait.width(), portrait.height(),
                    ImmortalUiSkin.JOURNAL_BORDER);
        }

        ImmortalUiSkin.drawInnerFrame(graphics, prompts.x(), prompts.y(), prompts.width(), prompts.height());
        int contentWidth = Math.max(1, prompts.width() - 9);
        renderedPromptHeight = measurePrompts(contentWidth);
        int visibleHeight = Math.max(1, prompts.height() - 6);
        promptScroll = clampScroll(promptScroll, renderedPromptHeight, visibleHeight);
        ImmortalUiSkin.withScissor(graphics, prompts.x() + 1, prompts.y() + 1,
                Math.max(1, prompts.width() - 2), Math.max(1, prompts.height() - 2),
                () -> renderPrompts(graphics, prompts.x() + 4,
                        prompts.y() + 3 - promptScroll, contentWidth));
        ImmortalUiSkin.drawThinScrollbar(graphics, prompts.right() - 3, prompts.y() + 1,
                Math.max(1, prompts.height() - 2), renderedPromptHeight, visibleHeight, promptScroll);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        Rect viewport = calculateLayout(width, height).promptViewport();
        int visibleHeight = Math.max(1, viewport.height() - 6);
        if (viewport.contains(mouseX, mouseY) && renderedPromptHeight > visibleHeight) {
            promptScroll = clampScroll(promptScroll - (int)Math.round(delta * 14.0D),
                    renderedPromptHeight, visibleHeight);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private List<Component> promptLines() {
        return List.of(
                Component.translatable("screen.seeking_immortals.dialogue.chain", chainId),
                Component.translatable("screen.seeking_immortals.dialogue.hint_talk"),
                Component.translatable("screen.seeking_immortals.dialogue.hint_branch"),
                Component.translatable("screen.seeking_immortals.dialogue.hint_actions"));
    }

    private int measurePrompts(int contentWidth) {
        int height = 0;
        for (Component line : promptLines()) {
            height += Math.max(1, font.split(line, contentWidth).size()) * (font.lineHeight + 2);
        }
        return Math.max(1, height - 2);
    }

    private void renderPrompts(GuiGraphics graphics, int x, int y, int contentWidth) {
        int cursorY = y;
        List<Component> promptLines = promptLines();
        for (int index = 0; index < promptLines.size(); index++) {
            int color = index == 0 ? ImmortalUiSkin.JOURNAL_PAPER_MUTED : ImmortalUiSkin.JOURNAL_PAPER;
            for (FormattedCharSequence sequence : font.split(promptLines.get(index), contentWidth)) {
                graphics.drawString(font, sequence, x, cursorY, color, false);
                cursorY += font.lineHeight + 2;
            }
        }
    }

    private ResourceLocation portraitForChain() {
        String id = chainId == null ? "" : chainId.toLowerCase(Locale.ROOT);
        if (id.contains("huangfeng") || id.contains("qixuan")) return PORTRAIT_MO_LAO;
        if (id.contains("mulan") || id.contains("tianlan")) return PORTRAIT_MULAN;
        if (id.contains("ghost") || id.contains("yin")) return PORTRAIT_YINLUO;
        if (id.contains("star") || id.contains("chaotic") || id.contains("void")) return PORTRAIT_STAR;
        if (id.contains("dajin") || id.contains("kunwu")) return PORTRAIT_KUNWU;
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
        if (sound != null) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    static Layout calculateLayout(int screenWidth, int screenHeight) {
        int safeWidth = Math.max(1, screenWidth);
        int safeHeight = Math.max(1, screenHeight);
        int margin = safeWidth < 180 || safeHeight < 120 ? 4 : 12;
        margin = Math.min(margin, Math.min((safeWidth - 1) / 2, (safeHeight - 1) / 2));
        int panelWidth = Math.max(1, Math.min(DESIRED_WIDTH, safeWidth - margin * 2));
        int panelHeight = Math.max(1, Math.min(DESIRED_HEIGHT, safeHeight - margin * 2));
        int left = Math.max(0, (safeWidth - panelWidth) / 2);
        int top = Math.max(0, (safeHeight - panelHeight) / 2);
        Rect panel = new Rect(left, top, panelWidth, panelHeight);
        boolean narrow = panelWidth < 240;
        int padding = narrow || panelHeight < 120 ? 4 : 12;
        int gap = panelHeight < 100 ? 1 : narrow || panelHeight < 120 ? 2 : 6;
        int buttonHeight = panelHeight < 100 ? 10 : panelHeight < 120 ? 12 : 18;
        int innerWidth = Math.max(1, panelWidth - padding * 2);
        int headerY = top + padding;

        Rect refresh;
        Rect close;
        Rect titleArea;
        if (narrow) {
            int buttonWidth = Math.max(1, (innerWidth - gap) / 2);
            refresh = new Rect(left + padding, headerY, buttonWidth, buttonHeight);
            close = new Rect(refresh.right() + gap, headerY,
                    Math.max(1, innerWidth - gap - buttonWidth), buttonHeight);
            titleArea = new Rect(left + padding, refresh.bottom() + gap,
                    innerWidth, Math.min(9, Math.max(1, panelHeight - buttonHeight - padding * 2)));
        } else {
            int small = Math.max(48, Math.min(72, (innerWidth - gap) / 4));
            close = new Rect(panel.right() - padding - small, headerY, small, buttonHeight);
            refresh = new Rect(close.x() - gap - small, headerY, small, buttonHeight);
            titleArea = new Rect(left + padding, headerY + Math.max(1, (buttonHeight - 9) / 2),
                    Math.max(1, refresh.x() - gap - left - padding), 10);
        }

        int actionRows = narrow ? 3 : 2;
        int footerHeight = actionRows * buttonHeight + (actionRows - 1) * gap;
        int footerY = Math.max(top, panel.bottom() - padding - footerHeight);
        Rect start;
        Rect advance;
        Rect righteous;
        Rect neutral;
        Rect demonic;
        if (narrow) {
            int columnWidth = Math.max(1, (innerWidth - gap) / 2);
            start = new Rect(left + padding, footerY, columnWidth, buttonHeight);
            advance = new Rect(start.right() + gap, footerY,
                    Math.max(1, innerWidth - gap - columnWidth), buttonHeight);
            righteous = new Rect(left + padding, footerY + buttonHeight + gap, columnWidth, buttonHeight);
            neutral = new Rect(righteous.right() + gap, righteous.y(), advance.width(), buttonHeight);
            demonic = new Rect(left + padding + Math.max(0, (innerWidth - columnWidth) / 2),
                    footerY + (buttonHeight + gap) * 2, columnWidth, buttonHeight);
        } else {
            int firstWidth = Math.max(1, (innerWidth - gap) / 2);
            start = new Rect(left + padding, footerY, firstWidth, buttonHeight);
            advance = new Rect(start.right() + gap, footerY,
                    Math.max(1, innerWidth - gap - firstWidth), buttonHeight);
            int branchWidth = Math.max(1, (innerWidth - gap * 2) / 3);
            int branchY = footerY + buttonHeight + gap;
            righteous = new Rect(left + padding, branchY, branchWidth, buttonHeight);
            neutral = new Rect(righteous.right() + gap, branchY, branchWidth, buttonHeight);
            demonic = new Rect(neutral.right() + gap, branchY,
                    Math.max(1, innerWidth - branchWidth * 2 - gap * 2), buttonHeight);
        }

        int contentTop = refresh.bottom() + gap;
        if (narrow) contentTop = titleArea.bottom() + gap;
        int contentBottom = Math.max(contentTop + 1, footerY - gap);
        int contentHeight = Math.max(1, contentBottom - contentTop);
        int portraitHeight = Math.min(88, contentHeight);
        int portraitWidth = Math.max(1, Math.min(72, (int)Math.round(portraitHeight * 72.0D / 88.0D)));
        int contentRight = panel.right() - padding;
        Rect portrait = new Rect(Math.max(left + padding, contentRight - portraitWidth), contentTop,
                portraitWidth, portraitHeight);
        int promptRight = Math.max(left + padding + 1, portrait.x() - gap);
        Rect promptViewport = new Rect(left + padding, contentTop,
                Math.max(1, promptRight - left - padding), contentHeight);

        return new Layout(panel, titleArea, promptViewport, portrait, refresh, close,
                start, advance, righteous, neutral, demonic, padding);
    }

    static int clampScroll(int offset, int contentHeight, int viewportHeight) {
        return Math.max(0, Math.min(offset, Math.max(0, contentHeight - Math.max(1, viewportHeight))));
    }

    record Layout(Rect panel, Rect titleArea, Rect promptViewport, Rect portrait,
                  Rect refresh, Rect close, Rect start, Rect advance,
                  Rect righteous, Rect neutral, Rect demonic, int padding) {}

    record Rect(int x, int y, int width, int height) {
        int right() { return x + width; }
        int bottom() { return y + height; }
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
        boolean intersects(Rect other) {
            return other != null && x < other.right() && right() > other.x
                    && y < other.bottom() && bottom() > other.y;
        }
        boolean inside(int screenWidth, int screenHeight) {
            return width > 0 && height > 0 && x >= 0 && y >= 0
                    && right() <= screenWidth && bottom() <= screenHeight;
        }
    }
}
