package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.artifact.ArtifactStorageService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Client preview of NBT-stored bracelet contents. Server withdrawal remains authoritative. */
public class StorageBraceletScreen extends AbstractJournalScreen {
    private static final int DESIRED_WIDTH = 260;
    private static final int DESIRED_HEIGHT = 180;
    private static final int LINE_GAP = 2;

    private final List<String> lines = new ArrayList<>();
    private int scrollOffset;
    private int renderedContentHeight;

    public StorageBraceletScreen(List<String> previewLines) {
        super(Component.translatable("screen.seeking_immortals.storage_bracelet.title"));
        if (previewLines != null) {
            lines.addAll(previewLines);
        }
    }

    public static StorageBraceletScreen fromHeld(ItemStack stack) {
        List<String> preview = new ArrayList<>();
        preview.add(Component.translatable("screen.seeking_immortals.storage_bracelet.hint").getString());
        preview.add("slots=" + ArtifactStorageService.countStored(stack));
        return new StorageBraceletScreen(preview);
    }

    @Override
    protected void init() {
        super.init();
        Layout layout = calculateLayout(width, height);
        Rect done = layout.doneButton();
        addRenderableWidget(ImmortalButton.secondary(done.x(), done.y(), done.width(), done.height(),
                Component.translatable("gui.done"), button -> onClose()));
    }

    @Override
    protected JournalChrome journalChrome() {
        Layout layout = calculateLayout(width, height);
        Rect panel = layout.panel();
        return new JournalChrome(panel.x(), panel.y(), panel.width(), panel.height(),
                toUi(layout.titleBar()), toUi(layout.viewport()));
    }

    @Override
    protected void renderJournalTitle(GuiGraphics graphics, JournalChrome chrome, UiRect header) {
        ImmortalUiSkin.drawStringFit(font, graphics, title.getString(),
                header.x() + 6, header.y() + Math.max(2, (header.height() - font.lineHeight) / 2),
                Math.max(1, header.width() - 12), ImmortalUiSkin.JOURNAL_BORDER, false);
    }

    @Override
    protected void renderJournalContent(GuiGraphics graphics, JournalChrome chrome,
                                         int mouseX, int mouseY, float partialTick) {
        Layout layout = calculateLayout(width, height);
        Rect viewport = layout.viewport();
        int contentWidth = Math.max(1, viewport.width() - 10);
        renderedContentHeight = measureContent(contentWidth);
        int visibleHeight = Math.max(1, viewport.height() - 6);
        scrollOffset = clampScroll(scrollOffset, renderedContentHeight, visibleHeight);
        ImmortalUiSkin.withScissor(graphics, viewport.x() + 1, viewport.y() + 1,
                Math.max(1, viewport.width() - 2), Math.max(1, viewport.height() - 2),
                () -> renderContent(graphics, viewport.x() + 5,
                        viewport.y() + 3 - scrollOffset, contentWidth));
        ImmortalUiSkin.drawThinScrollbar(graphics, viewport.right() - 3, viewport.y() + 1,
                Math.max(1, viewport.height() - 2), renderedContentHeight, visibleHeight, scrollOffset);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        Layout layout = calculateLayout(width, height);
        Rect viewport = layout.viewport();
        int visibleHeight = Math.max(1, viewport.height() - 6);
        if (viewport.contains(mouseX, mouseY) && renderedContentHeight > visibleHeight) {
            scrollOffset = clampScroll(scrollOffset - (int)Math.round(delta * 16.0D),
                    renderedContentHeight, visibleHeight);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private static UiRect toUi(Rect rect) {
        return new UiRect(rect.x(), rect.y(), rect.width(), rect.height());
    }

    private int measureContent(int contentWidth) {
        if (lines.isEmpty()) return font.lineHeight;
        int contentHeight = 0;
        for (String line : lines) {
            contentHeight += Math.max(1, font.split(Component.literal(line == null ? "" : line), contentWidth).size())
                    * (font.lineHeight + LINE_GAP);
        }
        return Math.max(1, contentHeight - LINE_GAP);
    }

    private void renderContent(GuiGraphics graphics, int x, int y, int contentWidth) {
        if (lines.isEmpty()) {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.storage_bracelet.empty").getString(),
                    x, y, contentWidth, ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
            return;
        }
        int cursorY = y;
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            List<FormattedCharSequence> wrapped = font.split(
                    Component.literal(lines.get(lineIndex) == null ? "" : lines.get(lineIndex)), contentWidth);
            int color = lineIndex == 0 ? ImmortalUiSkin.JOURNAL_JADE_TEXT : ImmortalUiSkin.JOURNAL_PAPER;
            for (FormattedCharSequence sequence : wrapped) {
                graphics.drawString(font, sequence, x, cursorY, color, false);
                cursorY += font.lineHeight + LINE_GAP;
            }
        }
    }

    static Layout calculateLayout(int screenWidth, int screenHeight) {
        int safeWidth = Math.max(1, screenWidth);
        int safeHeight = Math.max(1, screenHeight);
        int margin = safeWidth < 160 || safeHeight < 110 ? 4 : 12;
        margin = Math.min(margin, Math.min((safeWidth - 1) / 2, (safeHeight - 1) / 2));
        int panelWidth = Math.max(1, Math.min(DESIRED_WIDTH, safeWidth - margin * 2));
        int panelHeight = Math.max(1, Math.min(DESIRED_HEIGHT, safeHeight - margin * 2));
        int left = Math.max(0, (safeWidth - panelWidth) / 2);
        int top = Math.max(0, (safeHeight - panelHeight) / 2);
        Rect panel = new Rect(left, top, panelWidth, panelHeight);
        int padding = panelWidth < 160 || panelHeight < 110 ? 5 : 12;
        int gap = panelHeight < 110 ? 3 : 6;
        int buttonHeight = panelHeight < 110 ? 14 : 20;
        int buttonWidth = Math.max(28, Math.min(58, panelWidth - padding * 2));
        int footerY = Math.max(top, panel.bottom() - padding - buttonHeight);
        Rect done = new Rect(Math.max(left, panel.right() - padding - buttonWidth), footerY,
                Math.min(buttonWidth, panelWidth), Math.min(buttonHeight, panelHeight));
        int titleHeight = Math.max(12, Math.min(20, panelHeight / 4));
        int frameInset = Math.min(4, Math.max(0, panelWidth - 1));
        Rect titleBar = new Rect(left + frameInset, top + Math.min(4, Math.max(0, panelHeight - 1)),
                Math.max(1, panelWidth - frameInset * 2), Math.min(titleHeight, panelHeight));
        int viewportY = Math.min(footerY, titleBar.bottom() + gap);
        int viewportBottom = Math.max(viewportY + 1, footerY - gap);
        Rect viewport = new Rect(left + Math.min(padding, Math.max(0, panelWidth - 1)), viewportY,
                Math.max(1, panelWidth - Math.min(padding * 2, Math.max(0, panelWidth - 1))),
                Math.max(1, viewportBottom - viewportY));
        return new Layout(panel, titleBar, viewport, done);
    }

    static int clampScroll(int offset, int contentHeight, int viewportHeight) {
        return Math.max(0, Math.min(offset, Math.max(0, contentHeight - Math.max(1, viewportHeight))));
    }

    record Layout(Rect panel, Rect titleBar, Rect viewport, Rect doneButton) {}

    record Rect(int x, int y, int width, int height) {
        int right() { return x + width; }
        int bottom() { return y + height; }
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
        boolean inside(int screenWidth, int screenHeight) {
            return width > 0 && height > 0 && x >= 0 && y >= 0
                    && right() <= screenWidth && bottom() <= screenHeight;
        }
    }
}
