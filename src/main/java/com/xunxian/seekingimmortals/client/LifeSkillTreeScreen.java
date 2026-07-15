package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.skill.SkillType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Read-only client mirror of life and special skill progression. */
public class LifeSkillTreeScreen extends Screen {
    private static final int PANEL_MARGIN = 4;
    private static final int DEFAULT_PANEL_WIDTH = 520;
    private static final int DEFAULT_PANEL_HEIGHT = 360;
    private static final int COLUMN_BREAKPOINT = 390;
    private static final int COLUMN_GAP = 10;
    private static final int SECTION_HEADER_HEIGHT = 18;
    private static final int SKILL_ROW_HEIGHT = 52;
    private static final int SKILL_ROW_GAP = 4;

    private static final SkillType[] LIFE_SKILLS = {
            SkillType.ALCHEMY,
            SkillType.ARTIFACT_REFINING,
            SkillType.TALISMAN_CRAFTING,
            SkillType.FORMATION
    };

    private static final SkillType[] SPECIAL_SKILLS = {
            SkillType.FLYING_SWORD_BEGINNER,
            SkillType.FLYING_SWORD_ADVANCED,
            SkillType.DIVINE_SENSE_EXPANSION,
            SkillType.FORMATION_SENSE,
            SkillType.BEAST_TAMING,
            SkillType.PUPPET_CONTROL,
            SkillType.MULTI_CASTING
    };

    private final Screen parent;
    private int scrollOffset;
    private int renderedContentHeight;

    public LifeSkillTreeScreen(Screen parent) {
        super(Component.translatable("screen.seeking_immortals.skill_tree.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        SkillTreeLayout layout = calculateLayout(width, height);
        addRenderableWidget(ImmortalButton.secondary(
                layout.closeButton().x(), layout.closeButton().y(),
                layout.closeButton().width(), layout.closeButton().height(),
                Component.translatable("screen.seeking_immortals.skill_tree.close"),
                button -> onClose()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        SkillTreeLayout layout = calculateLayout(width, height);
        drawFrame(graphics, layout);

        UiRect viewport = layout.viewport();
        renderedContentHeight = ClientSkillData.isSynced()
                ? calculateContentHeight(layout.columns()) : viewport.height();
        scrollOffset = clampScroll(scrollOffset, renderedContentHeight, viewport.height());
        int startY = viewport.y() + 5 - scrollOffset;

        ImmortalUiSkin.withScissor(graphics, viewport.x(), viewport.y(), viewport.width(), viewport.height(), () -> {
            if (!ClientSkillData.isSynced()) {
                ImmortalUiSkin.drawWrappedText(font, graphics,
                        Component.translatable("screen.seeking_immortals.skill_tree.waiting_sync"),
                        viewport.x() + 8, viewport.y() + 10, Math.max(1, viewport.width() - 16),
                        Math.max(1, viewport.height() - 16), ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
                return;
            }

            int contentX = viewport.x() + 5;
            int contentWidth = Math.max(1, viewport.width() - 12);
            if (layout.columns()) {
                int columnWidth = Math.max(1, (contentWidth - COLUMN_GAP) / 2);
                renderSection(graphics, contentX, startY, columnWidth,
                        Component.translatable("screen.seeking_immortals.skill_tree.section.life"),
                        LIFE_SKILLS, mouseX, mouseY);
                renderSection(graphics, contentX + columnWidth + COLUMN_GAP, startY,
                        Math.max(1, contentWidth - columnWidth - COLUMN_GAP),
                        Component.translatable("screen.seeking_immortals.skill_tree.section.special"),
                        SPECIAL_SKILLS, mouseX, mouseY);
            } else {
                int nextY = renderSection(graphics, contentX, startY, contentWidth,
                        Component.translatable("screen.seeking_immortals.skill_tree.section.life"),
                        LIFE_SKILLS, mouseX, mouseY);
                renderSection(graphics, contentX, nextY + 6, contentWidth,
                        Component.translatable("screen.seeking_immortals.skill_tree.section.special"),
                        SPECIAL_SKILLS, mouseX, mouseY);
            }
        });
        ImmortalUiSkin.drawThinScrollbar(graphics, viewport.right() - 3, viewport.y(), viewport.height(),
                renderedContentHeight, viewport.height(), scrollOffset);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        SkillTreeLayout layout = calculateLayout(width, height);
        if (layout.viewport().contains(mouseX, mouseY) && renderedContentHeight > layout.viewport().height()) {
            scrollOffset = clampScroll(scrollOffset - (int)Math.round(delta * 18.0D),
                    renderedContentHeight, layout.viewport().height());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
            return;
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    static SkillTreeLayout calculateLayout(int screenWidth, int screenHeight) {
        int panelWidth = Math.min(DEFAULT_PANEL_WIDTH, Math.max(1, screenWidth - PANEL_MARGIN * 2));
        int panelHeight = Math.min(DEFAULT_PANEL_HEIGHT, Math.max(1, screenHeight - PANEL_MARGIN * 2));
        int left = Math.max(0, (screenWidth - panelWidth) / 2);
        int top = Math.max(0, (screenHeight - panelHeight) / 2);
        int padding = panelWidth >= 180 ? 10 : 3;
        int headerHeight = panelHeight >= 150 ? 32 : panelHeight >= 80 ? 22 : 14;
        int buttonHeight = Math.min(20, Math.max(10, panelHeight / 7));
        int buttonWidth = Math.min(72, Math.max(1, panelWidth - padding * 2));
        int buttonY = Math.max(top, top + panelHeight - padding - buttonHeight);
        int viewportY = Math.min(buttonY, top + headerHeight + 4);
        int viewportBottom = Math.max(viewportY + 1, buttonY - 5);
        UiRect header = new UiRect(left + padding, top + 4,
                Math.max(1, panelWidth - padding * 2), Math.max(1, headerHeight - 4));
        UiRect viewport = new UiRect(left + padding, viewportY,
                Math.max(1, panelWidth - padding * 2), Math.max(1, viewportBottom - viewportY));
        UiRect closeButton = new UiRect(left + panelWidth - padding - buttonWidth, buttonY,
                buttonWidth, Math.min(buttonHeight, Math.max(1, top + panelHeight - buttonY)));
        return new SkillTreeLayout(left, top, panelWidth, panelHeight,
                viewport.width() >= COLUMN_BREAKPOINT, header, viewport, closeButton);
    }

    static int calculateContentHeight(boolean columns) {
        int lifeHeight = sectionHeight(LIFE_SKILLS.length);
        int specialHeight = sectionHeight(SPECIAL_SKILLS.length);
        return columns ? Math.max(lifeHeight, specialHeight) + 10 : lifeHeight + specialHeight + 16;
    }

    static int clampScroll(int requested, int contentHeight, int viewportHeight) {
        int maximum = Math.max(0, contentHeight - Math.max(1, viewportHeight));
        return Math.max(0, Math.min(requested, maximum));
    }

    static int skillMaxLevel(SkillType type) {
        if (type == null) return 0;
        return switch (type.getCategory()) {
            case CULTIVATION_METHOD, CRAFTING -> 10;
            case SPELL -> 9;
            case SPECIAL -> 5;
        };
    }

    static int experienceForNextLevel(int level) {
        return 100 + Math.max(0, level) * 50;
    }

    private static int sectionHeight(int entries) {
        return SECTION_HEADER_HEIGHT + Math.max(0, entries) * (SKILL_ROW_HEIGHT + SKILL_ROW_GAP);
    }

    private void drawFrame(GuiGraphics graphics, SkillTreeLayout layout) {
        ImmortalUiSkin.drawLayeredPanel(graphics, layout.left(), layout.top(),
                layout.panelWidth(), layout.panelHeight());
        ImmortalUiSkin.drawTitleBar(graphics, layout.header().x(), layout.header().y(),
                layout.header().width(), layout.header().height());
        graphics.drawCenteredString(font,
                ImmortalUiSkin.fitWidth(font, title.getString(), Math.max(1, layout.header().width() - 16)),
                layout.header().x() + layout.header().width() / 2,
                layout.header().y() + Math.max(2, (layout.header().height() - 8) / 2),
                ImmortalUiSkin.JOURNAL_BORDER);
        ImmortalUiSkin.drawInnerFrame(graphics, layout.viewport().x(), layout.viewport().y(),
                layout.viewport().width(), layout.viewport().height());
    }

    private int renderSection(GuiGraphics graphics, int x, int y, int width, Component heading,
                              SkillType[] types, int mouseX, int mouseY) {
        ImmortalUiSkin.drawTitleBar(graphics, x, y, width, 14);
        ImmortalUiSkin.drawStringFit(font, graphics, heading.getString(), x + 8, y + 3,
                Math.max(1, width - 12), ImmortalUiSkin.JOURNAL_BORDER, false);
        y += SECTION_HEADER_HEIGHT;
        for (SkillType type : types) {
            drawSkillRow(graphics, x, y, width, type, mouseX, mouseY);
            y += SKILL_ROW_HEIGHT + SKILL_ROW_GAP;
        }
        return y;
    }

    private void drawSkillRow(GuiGraphics graphics, int x, int y, int width, SkillType type,
                              int mouseX, int mouseY) {
        ClientSkillData.SkillSnapshot skill = ClientSkillData.get(type);
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + SKILL_ROW_HEIGHT;
        ImmortalUiSkin.InteractionState state = !skill.unlocked()
                ? ImmortalUiSkin.InteractionState.DISABLED
                : hovered ? ImmortalUiSkin.InteractionState.HOVERED : ImmortalUiSkin.InteractionState.NORMAL;
        ImmortalUiSkin.drawListRow(graphics, x, y, width, SKILL_ROW_HEIGHT, state);

        int innerX = x + 6;
        int innerWidth = Math.max(1, width - 12);
        Component stateText = Component.translatable(skill.unlocked()
                ? "screen.seeking_immortals.skill_tree.status.unlocked"
                : "screen.seeking_immortals.skill_tree.status.locked");
        int stateWidth = Math.min(innerWidth / 3, Math.max(24, font.width(stateText)));
        ImmortalUiSkin.drawStringFit(font, graphics, type.getDisplayName(), innerX, y + 4,
                Math.max(1, innerWidth - stateWidth - 5),
                skill.unlocked() ? ImmortalUiSkin.JOURNAL_PAPER : ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
        ImmortalUiSkin.drawStringFit(font, graphics, stateText.getString(),
                innerX + innerWidth - stateWidth, y + 4, stateWidth,
                skill.unlocked() ? ImmortalUiSkin.JOURNAL_JADE_TEXT : ImmortalUiSkin.JOURNAL_WARNING, false);

        String realm = type.getRequiredRealm() == null ? "-" : type.getRequiredRealm().getDisplayName();
        ImmortalUiSkin.drawStringFit(font, graphics,
                Component.translatable("screen.seeking_immortals.skill_tree.realm_requirement", realm).getString(),
                innerX, y + 15, innerWidth, ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);

        int maxLevel = skillMaxLevel(type);
        Component levelText = Component.translatable("screen.seeking_immortals.skill_tree.level",
                skill.level(), maxLevel);
        Component nextValue = skill.unlocked() && skill.level() >= maxLevel
                ? Component.translatable("screen.seeking_immortals.skill_tree.maxed")
                : Component.literal(Integer.toString(experienceForNextLevel(skill.level())));
        Component experienceText = Component.translatable("screen.seeking_immortals.skill_tree.experience",
                skill.experience(), nextValue);
        int half = Math.max(1, (innerWidth - 5) / 2);
        ImmortalUiSkin.drawStringFit(font, graphics, levelText.getString(), innerX, y + 26,
                half, ImmortalUiSkin.JOURNAL_BORDER, false);
        ImmortalUiSkin.drawStringFit(font, graphics, experienceText.getString(), innerX + half + 5, y + 26,
                Math.max(1, innerWidth - half - 5), ImmortalUiSkin.JOURNAL_PAPER, false);

        Component proficiencyText = Component.translatable("screen.seeking_immortals.skill_tree.proficiency",
                Math.round(skill.proficiency() / 100.0D));
        ImmortalUiSkin.drawStringFit(font, graphics, proficiencyText.getString(), innerX, y + 37,
                innerWidth, ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
        ImmortalUiSkin.drawSemanticStatusBar(graphics, innerX, y + 46, innerWidth, 5,
                skill.proficiency() / 10000.0D,
                skill.unlocked() ? ImmortalUiSkin.StatusBarStyle.CULTIVATION
                        : ImmortalUiSkin.StatusBarStyle.NEUTRAL);
    }

    record UiRect(int x, int y, int width, int height) {
        int right() {
            return x + width;
        }

        int bottom() {
            return y + height;
        }

        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < y + height;
        }

        boolean intersects(UiRect other) {
            return other != null && x < other.right() && right() > other.x()
                    && y < other.bottom() && bottom() > other.y();
        }
    }

    record SkillTreeLayout(int left, int top, int panelWidth, int panelHeight, boolean columns,
                           UiRect header, UiRect viewport, UiRect closeButton) {}
}
