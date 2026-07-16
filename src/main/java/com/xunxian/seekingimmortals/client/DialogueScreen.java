package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.network.DialogueActionPacket;
import com.xunxian.seekingimmortals.network.ModNetwork;
import com.xunxian.seekingimmortals.network.OpenDialogueScreenPacket;
import com.xunxian.seekingimmortals.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Visual dialogue GUI driven entirely by a bounded server view. */
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

    private final OpenDialogueScreenPacket view;
    private final List<ImmortalButton> actionButtons = new ArrayList<>();
    private boolean actionPending;
    private boolean closeSent;
    private int promptScroll;
    private int renderedPromptHeight;

    public DialogueScreen(OpenDialogueScreenPacket view) {
        super(Component.translatable("screen.seeking_immortals.dialogue.title"));
        this.view = view == null
                ? new OpenDialogueScreenPacket("", "", "", "", Component.empty(), List.of(), List.of())
                : view;
    }

    @Override
    protected void init() {
        super.init();
        actionButtons.clear();
        Layout layout = calculateLayout(width, height, view.choices().size());
        addButton(layout.refresh(), Component.translatable("screen.seeking_immortals.dialogue.refresh"), button -> {
            if (beginAction()) {
                playVoice(ModSounds.DIALOGUE_GREETING.get());
                send(DialogueActionPacket.ACTION_TALK, "");
            }
        }, false, true);
        addButton(layout.close(), Component.translatable("screen.seeking_immortals.dialogue.close"), button -> onClose(),
                false, false);

        int count = Math.min(view.choices().size(), layout.choiceButtons().size());
        for (int index = 0; index < count; index++) {
            OpenDialogueScreenPacket.Choice choice = view.choices().get(index);
            Rect rect = layout.choiceButtons().get(index);
            addButton(rect, choice.label(), button -> {
                if (beginAction()) {
                    playVoice(ModSounds.DIALOGUE_BRANCH.get());
                    send(DialogueActionPacket.ACTION_ACT, choice.id());
                }
            }, index == 0, true);
        }
        playVoice(npcVoice());
    }

    private void addButton(Rect rect, Component label, net.minecraft.client.gui.components.Button.OnPress onPress,
                           boolean primary, boolean actionButton) {
        ImmortalButton button = primary
                ? ImmortalButton.primary(rect.x(), rect.y(), rect.width(), rect.height(), label, onPress)
                : ImmortalButton.secondary(rect.x(), rect.y(), rect.width(), rect.height(), label, onPress);
        addRenderableWidget(button);
        if (actionButton) {
            actionButtons.add(button);
        }
    }

    private boolean beginAction() {
        if (actionPending || view.context().isBlank()) {
            return false;
        }
        actionPending = true;
        for (ImmortalButton button : actionButtons) {
            button.active = false;
        }
        return true;
    }

    private void send(String action, String choice) {
        ModNetwork.CHANNEL.sendToServer(new DialogueActionPacket(action, view.context(), choice));
    }

    @Override
    public void onClose() {
        if (!closeSent && !view.context().isBlank()) {
            closeSent = true;
            send(DialogueActionPacket.ACTION_CLOSE, "");
        }
        super.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        Layout layout = calculateLayout(width, height, view.choices().size());
        Rect panel = layout.panel();
        Rect titleArea = layout.titleArea();
        Rect portrait = layout.portrait();
        Rect prompts = layout.promptViewport();

        ImmortalUiSkin.drawLayeredPanel(graphics, panel.x(), panel.y(), panel.width(), panel.height());
        int headerHeight = Math.max(12, layout.refresh().bottom() - panel.y() + layout.padding());
        ImmortalUiSkin.drawTitleBar(graphics, panel.x() + 4, panel.y() + 4,
                Math.max(1, panel.width() - 8), Math.min(headerHeight, Math.max(1, panel.height() - 4)));
        ImmortalUiSkin.drawStringFit(font, graphics, title.getString(), titleArea.x(), titleArea.y(),
                titleArea.width(), ImmortalUiSkin.JOURNAL_BORDER, false);

        if (portrait.width() > 1 && portrait.height() > 1) {
            graphics.blit(portraitForView(), portrait.x(), portrait.y(), portrait.width(), portrait.height(),
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
        Rect viewport = calculateLayout(width, height, view.choices().size()).promptViewport();
        int visibleHeight = Math.max(1, viewport.height() - 6);
        if (viewport.contains(mouseX, mouseY) && renderedPromptHeight > visibleHeight) {
            promptScroll = clampScroll(promptScroll - (int)Math.round(delta * 14.0D),
                    renderedPromptHeight, visibleHeight);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private List<Component> promptLines() {
        List<Component> lines = new ArrayList<>();
        if (!view.speaker().getString().isBlank()) {
            lines.add(view.speaker());
        }
        lines.addAll(view.lines());
        if (lines.isEmpty()) {
            lines.add(Component.literal("……"));
        }
        return List.copyOf(lines);
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
        List<Component> lines = promptLines();
        for (int index = 0; index < lines.size(); index++) {
            int color = index == 0 ? ImmortalUiSkin.JOURNAL_JADE_TEXT : ImmortalUiSkin.JOURNAL_PAPER;
            for (FormattedCharSequence sequence : font.split(lines.get(index), contentWidth)) {
                graphics.drawString(font, sequence, x, cursorY, color, false);
                cursorY += font.lineHeight + 2;
            }
            if (index == 0 && lines.size() > 1) {
                cursorY += 2;
            }
        }
    }

    private ResourceLocation portraitForView() {
        String id = (view.npcId() + " " + view.sourceId()).toLowerCase(Locale.ROOT);
        if (id.contains("huangfeng") || id.contains("qixuan") || id.contains("mo_lao")) return PORTRAIT_MO_LAO;
        if (id.contains("mulan") || id.contains("tianlan")) return PORTRAIT_MULAN;
        if (id.contains("ghost") || id.contains("yin")) return PORTRAIT_YINLUO;
        if (id.contains("star") || id.contains("chaotic") || id.contains("void")) return PORTRAIT_STAR;
        if (id.contains("dajin") || id.contains("kunwu")) return PORTRAIT_KUNWU;
        return PORTRAIT_DEFAULT;
    }

    private SoundEvent npcVoice() {
        String id = (view.npcId() + " " + view.sourceId()).toLowerCase(Locale.ROOT);
        if (id.contains("huangfeng") || id.contains("qixuan") || id.contains("mo_lao")) {
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
        return calculateLayout(screenWidth, screenHeight, OpenDialogueScreenPacket.MAX_CHOICES);
    }

    static Layout calculateLayout(int screenWidth, int screenHeight, int choiceCount) {
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

        int count = Math.max(0, Math.min(OpenDialogueScreenPacket.MAX_CHOICES, choiceCount));
        int columns = count <= 1 ? 1 : 2;
        int rows = count == 0 ? 0 : (count + columns - 1) / columns;
        int footerHeight = rows == 0 ? 0 : rows * buttonHeight + (rows - 1) * gap;
        int footerY = Math.max(top, panel.bottom() - padding - footerHeight);
        int columnWidth = columns == 1 ? innerWidth : Math.max(1, (innerWidth - gap) / 2);
        List<Rect> choices = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            int column = index % columns;
            int row = index / columns;
            int x = left + padding + column * (columnWidth + gap);
            int width = column == columns - 1 ? Math.max(1, panel.right() - padding - x) : columnWidth;
            choices.add(new Rect(x, footerY + row * (buttonHeight + gap), width, buttonHeight));
        }

        int contentTop = narrow ? titleArea.bottom() + gap : refresh.bottom() + gap;
        int contentBottom = Math.max(contentTop + 1, footerY - (rows == 0 ? 0 : gap));
        contentBottom = Math.min(panel.bottom() - padding, contentBottom);
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
                List.copyOf(choices), padding);
    }

    static int clampScroll(int offset, int contentHeight, int viewportHeight) {
        return Math.max(0, Math.min(offset, Math.max(0, contentHeight - Math.max(1, viewportHeight))));
    }

    record Layout(Rect panel, Rect titleArea, Rect promptViewport, Rect portrait,
                  Rect refresh, Rect close, List<Rect> choiceButtons, int padding) {}

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
