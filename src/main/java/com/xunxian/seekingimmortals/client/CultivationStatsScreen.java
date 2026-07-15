package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.network.AttemptBreakthroughPacket;
import com.xunxian.seekingimmortals.network.ModNetwork;
import com.xunxian.seekingimmortals.network.SetMovementSpeedScalePacket;
import com.xunxian.seekingimmortals.skill.SkillType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

public class CultivationStatsScreen extends Screen {
    private static final int DEFAULT_PANEL_WIDTH = 548;
    private static final int DEFAULT_PANEL_HEIGHT = 360;
    private static final int PANEL_MARGIN = 4;
    private static final int WIDE_LAYOUT_WIDTH = 470;
    private static final int PAGE_COLUMN_WIDTH = 320;
    private static final int PAGE_COLUMN_GAP = 12;
    private static final int SCROLLBAR_RESERVE = 7;
    private static final int LINE_HEIGHT = 12;
    private static final int SECTION_GAP = 8;
    private static final double MOVEMENT_SLIDER_STEP = 0.05D;

    private static final int INK_SOFT = ImmortalUiSkin.JOURNAL_INNER;
    private static final int INK_ROW = ImmortalUiSkin.JOURNAL_ROW;
    private static final int BRONZE = ImmortalUiSkin.JOURNAL_BORDER;
    private static final int BRONZE_DIM = ImmortalUiSkin.JOURNAL_BORDER_DIM;
    private static final int JADE_LINE = ImmortalUiSkin.JOURNAL_JADE;
    private static final int JADE = ImmortalUiSkin.JOURNAL_JADE_TEXT;
    private static final int PAPER = ImmortalUiSkin.JOURNAL_PAPER;
    private static final int PAPER_MUTED = ImmortalUiSkin.JOURNAL_PAPER_MUTED;
    private static final int SPIRIT_BLUE = ImmortalUiSkin.JOURNAL_SPIRIT;
    private static final int CINNABAR = ImmortalUiSkin.JOURNAL_CINNABAR;
    private static final int CINNABAR_BRIGHT = ImmortalUiSkin.JOURNAL_CINNABAR_BRIGHT;
    private static final int WARNING = ImmortalUiSkin.JOURNAL_WARNING;
    private static final int BAR_HIGHLIGHT = ImmortalUiSkin.JOURNAL_BAR_HIGHLIGHT;

    private static final List<LifeSkillEntry> LIFE_SKILLS = List.of(
            new LifeSkillEntry("炼丹", SkillType.ALCHEMY),
            new LifeSkillEntry("炼器", SkillType.ARTIFACT_REFINING),
            new LifeSkillEntry("符箓", SkillType.TALISMAN_CRAFTING),
            new LifeSkillEntry("阵法", SkillType.FORMATION),
            new LifeSkillEntry("驭兽", SkillType.BEAST_TAMING),
            new LifeSkillEntry("傀儡", SkillType.PUPPET_CONTROL));

    private static final List<LifeSkillEntry> SPECIAL_SKILLS = List.of(
            new LifeSkillEntry("御剑初解", SkillType.FLYING_SWORD_BEGINNER),
            new LifeSkillEntry("御剑进阶", SkillType.FLYING_SWORD_ADVANCED),
            new LifeSkillEntry("神识扩展", SkillType.DIVINE_SENSE_EXPANSION),
            new LifeSkillEntry("阵法感知", SkillType.FORMATION_SENSE),
            new LifeSkillEntry("分神施法", SkillType.MULTI_CASTING));

    private final LocalPlayer player;
    private final boolean returnToInventory;
    private StatsTab activeTab = StatsTab.FOUNDATION;
    private MovementSpeedSlider movementSpeedSlider;
    private int scrollOffset;
    private int renderedContentHeight;
    private int contentRevision = Integer.MIN_VALUE;

    public CultivationStatsScreen(LocalPlayer player) {
        this(player, false);
    }

    public CultivationStatsScreen(LocalPlayer player, boolean returnToInventory) {
        super(Component.translatable("screen.seeking_immortals.cultivation_stats.title"));
        this.player = player;
        this.returnToInventory = returnToInventory;
    }

    @Override
    protected void init() {
        super.init();
        PanelLayout layout = calculateLayout(width, height);

        addRenderableWidget(new JournalTabButton(layout.foundationTab(), StatsTab.FOUNDATION));
        addRenderableWidget(new JournalTabButton(layout.combatTab(), StatsTab.COMBAT));
        addRenderableWidget(new JournalTabButton(layout.studyTab(), StatsTab.STUDY));

        addRenderableWidget(ImmortalButton.primary(layout.breakthroughButton().x(), layout.breakthroughButton().y(),
                layout.breakthroughButton().width(), layout.breakthroughButton().height(),
                Component.translatable("screen.seeking_immortals.cultivation_stats.breakthrough"),
                button -> ModNetwork.CHANNEL.sendToServer(new AttemptBreakthroughPacket())));
        addRenderableWidget(ImmortalButton.secondary(layout.methodTreeButton().x(), layout.methodTreeButton().y(),
                layout.methodTreeButton().width(), layout.methodTreeButton().height(),
                Component.translatable("screen.seeking_immortals.cultivation_stats.methods"), button -> {
                    if (minecraft != null) {
                        minecraft.setScreen(new MethodTreeScreen(this));
                    }
                }));
        addRenderableWidget(ImmortalButton.secondary(layout.skillTreeButton().x(), layout.skillTreeButton().y(),
                layout.skillTreeButton().width(), layout.skillTreeButton().height(),
                Component.translatable("screen.seeking_immortals.cultivation_stats.skills"), button -> {
                    if (minecraft != null) {
                        minecraft.setScreen(new LifeSkillTreeScreen(this));
                    }
                }));
        addRenderableWidget(ImmortalButton.secondary(layout.closeButton().x(), layout.closeButton().y(),
                layout.closeButton().width(), layout.closeButton().height(), closeButtonLabel(returnToInventory),
                button -> onClose()));

        movementSpeedSlider = new MovementSpeedSlider(layout.slider().x(), layout.slider().y(),
                layout.slider().width(), layout.slider().height(),
                ClientCultivationData.getSnapshot().movementSpeedScale());
        addRenderableWidget(movementSpeedSlider);
        updateSliderVisibility(layout);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        renderCultivationJournal(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        scrollOffset = 0;
        renderedContentHeight = 0;
        contentRevision = Integer.MIN_VALUE;
        super.resize(minecraft, width, height);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        PanelLayout layout = calculateLayout(width, height);
        UiRect viewport = layout.pageViewport(activeTab);
        int maxScroll = maxScroll(renderedContentHeight, viewport.height());
        if (viewport.contains(mouseX, mouseY) && maxScroll > 0) {
            scrollOffset = clampScroll(scrollOffset - (int)Math.round(delta * 20.0D),
                    renderedContentHeight, viewport.height());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (returnToInventory && player != null && minecraft != null) {
            minecraft.setScreen(new InventoryScreen(player));
            return;
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    boolean returnsToInventory() {
        return returnToInventory;
    }

    static Component closeButtonLabel(boolean returnToInventory) {
        return Component.translatable(returnToInventory
                ? "screen.seeking_immortals.cultivation_stats.back_to_inventory"
                : "screen.seeking_immortals.cultivation_stats.close");
    }

    private void selectTab(StatsTab tab) {
        if (tab == null || tab == activeTab) {
            return;
        }
        activeTab = tab;
        scrollOffset = 0;
        renderedContentHeight = 0;
        contentRevision = Integer.MIN_VALUE;
        updateSliderVisibility(calculateLayout(width, height));
    }

    private void updateSliderVisibility(PanelLayout layout) {
        if (movementSpeedSlider != null) {
            movementSpeedSlider.visible = activeTab == StatsTab.COMBAT && layout.showsMovementSlider();
            movementSpeedSlider.active = movementSpeedSlider.visible;
        }
    }

    static int calculatePanelWidth(int screenWidth) {
        return Math.min(DEFAULT_PANEL_WIDTH, Math.max(1, screenWidth - PANEL_MARGIN * 2));
    }

    static int calculatePanelHeight(int screenHeight) {
        return Math.min(DEFAULT_PANEL_HEIGHT, Math.max(1, screenHeight - PANEL_MARGIN * 2));
    }

    static boolean usesWideLayout(int panelWidth, int panelHeight) {
        return panelWidth >= WIDE_LAYOUT_WIDTH && panelHeight >= 250;
    }

    static boolean usesPageColumns(int contentWidth) {
        return contentWidth >= PAGE_COLUMN_WIDTH;
    }

    static PanelLayout calculateLayout(int screenWidth, int screenHeight) {
        int panelWidth = calculatePanelWidth(screenWidth);
        int panelHeight = calculatePanelHeight(screenHeight);
        int left = Math.max(0, (screenWidth - panelWidth) / 2);
        int top = Math.max(0, (screenHeight - panelHeight) / 2);
        int padding = panelWidth >= 220 ? 10 : 5;
        int headerHeight = panelHeight >= 220 ? 42 : panelHeight >= 140 ? 30 : 20;
        int tabHeight = panelHeight >= 140 ? 18 : 14;
        int buttonHeight = panelHeight >= 180 ? 20 : panelHeight >= 118 ? 16 : 14;
        int footerInset = panelHeight >= 140 ? 7 : 4;
        int buttonY = top + panelHeight - buttonHeight - footerInset;
        int contentBottom = Math.max(top + headerHeight + tabHeight + 6, buttonY - 6);
        int tabY = top + headerHeight;
        int contentTop = tabY + tabHeight + 5;
        boolean wide = usesWideLayout(panelWidth, panelHeight);

        int profileWidth = wide ? 108 : 0;
        int profileGap = wide ? 10 : 0;
        int contentX = left + padding + profileWidth + profileGap;
        int contentRight = left + panelWidth - padding;
        int contentWidth = Math.max(20, contentRight - contentX);
        int contentHeight = Math.max(1, contentBottom - contentTop);

        UiRect header = new UiRect(left + padding, top + 4,
                Math.max(20, panelWidth - padding * 2), Math.max(12, headerHeight - 5));
        UiRect profile = new UiRect(left + padding, contentTop, profileWidth, contentHeight);
        UiRect content = new UiRect(contentX, contentTop, contentWidth, contentHeight);

        int tabGap = 3;
        int tabWidth = Math.max(8, (contentWidth - tabGap * 2) / 3);
        UiRect foundationTab = new UiRect(contentX, tabY, tabWidth, tabHeight);
        UiRect combatTab = new UiRect(foundationTab.right() + tabGap, tabY, tabWidth, tabHeight);
        UiRect studyTab = new UiRect(combatTab.right() + tabGap, tabY,
                Math.max(8, contentRight - combatTab.right() - tabGap), tabHeight);

        int innerWidth = Math.max(20, panelWidth - padding * 2);
        int buttonGap = 3;
        int buttonWidth = Math.max(12, Math.min(86, (innerWidth - buttonGap * 3) / 4));
        int totalButtonWidth = buttonWidth * 4 + buttonGap * 3;
        int buttonX = left + padding + Math.max(0, (innerWidth - totalButtonWidth) / 2);
        UiRect breakthroughButton = new UiRect(buttonX, buttonY, buttonWidth, buttonHeight);
        UiRect methodTreeButton = new UiRect(breakthroughButton.right() + buttonGap, buttonY, buttonWidth, buttonHeight);
        UiRect skillTreeButton = new UiRect(methodTreeButton.right() + buttonGap, buttonY, buttonWidth, buttonHeight);
        UiRect closeButton = new UiRect(skillTreeButton.right() + buttonGap, buttonY, buttonWidth, buttonHeight);

        int sliderHeight = panelHeight >= 180 ? 18 : 14;
        int sliderY = Math.max(contentTop, contentBottom - sliderHeight);
        UiRect slider = new UiRect(contentX, sliderY, contentWidth, sliderHeight);
        boolean showsMovementSlider = contentHeight >= sliderHeight + 4;

        return new PanelLayout(left, top, panelWidth, panelHeight, wide, showsMovementSlider,
                header, profile, foundationTab, combatTab, studyTab, content,
                breakthroughButton, methodTreeButton, skillTreeButton, closeButton, slider);
    }

    static int clampScroll(int requested, int contentHeight, int viewportHeight) {
        return Math.max(0, Math.min(requested, maxScroll(contentHeight, viewportHeight)));
    }

    private static int maxScroll(int contentHeight, int viewportHeight) {
        return Math.max(0, contentHeight - Math.max(1, viewportHeight));
    }

    private void renderCultivationJournal(GuiGraphics graphics, int mouseX, int mouseY) {
        PanelLayout layout = calculateLayout(width, height);
        ClientCultivationData.Snapshot data = ClientCultivationData.getSnapshot();
        if (movementSpeedSlider != null) {
            movementSpeedSlider.syncFromSnapshot(data.movementSpeedScale());
        }
        updateSliderVisibility(layout);

        drawJournalFrame(graphics, layout);
        drawHeader(graphics, layout, data);
        if (layout.wide()) {
            drawProfile(graphics, layout.profile(), data, mouseX, mouseY);
        }

        if (!ClientCultivationData.isSynced()) {
            drawWaitingForSync(graphics, layout.pageViewport(activeTab));
            renderedContentHeight = 0;
            return;
        }
        renderActivePage(graphics, layout, data);
    }

    private void renderActivePage(GuiGraphics graphics, PanelLayout layout, ClientCultivationData.Snapshot data) {
        UiRect viewport = layout.pageViewport(activeTab);
        int revision = currentContentRevision();
        if (revision != contentRevision) {
            scrollOffset = 0;
            renderedContentHeight = 0;
            contentRevision = revision;
        }
        int pageWidth = Math.max(20, viewport.width() - SCROLLBAR_RESERVE);
        int measuredEndY = renderPage(null, viewport.x(), viewport.y(), pageWidth, data);
        renderedContentHeight = contentHeightFromBounds(viewport.y(), measuredEndY);
        scrollOffset = clampScroll(scrollOffset, renderedContentHeight, viewport.height());
        int startY = viewport.y() - scrollOffset;

        ImmortalUiSkin.withScissor(graphics, viewport.x(), viewport.y(), viewport.width(), viewport.height(),
                () -> renderPage(graphics, viewport.x(), startY, pageWidth, data));
        drawScrollBar(graphics, viewport, renderedContentHeight, scrollOffset);
    }

    private int renderPage(GuiGraphics graphics, int x, int y, int width,
                           ClientCultivationData.Snapshot data) {
        return switch (activeTab) {
            case FOUNDATION -> renderFoundationPage(graphics, x, y, width, data);
            case COMBAT -> renderCombatPage(graphics, x, y, width, data);
            case STUDY -> renderStudyPage(graphics, x, y, width);
        };
    }

    static int contentHeightFromBounds(int startY, int endY) {
        return Math.max(0, endY - startY + 2);
    }

    private int currentContentRevision() {
        if (activeTab != StatsTab.STUDY) {
            return activeTab.ordinal();
        }
        int methodCount = ClientMethodData.isSynced() ? ClientMethodData.getLearnedMethodCount() : -1;
        int techniqueCount = ClientTechniqueData.getLearnedTechniques().size();
        return 31 * (31 * (31 + methodCount) + techniqueCount) + (ClientSkillData.isSynced() ? 1 : 0);
    }

    private int renderFoundationPage(GuiGraphics graphics, int x, int y, int width,
                                     ClientCultivationData.Snapshot data) {
        if (!usesPageColumns(width)) {
            y = renderRealmFoundation(graphics, x, y, width, data);
            return renderRootAndMeditation(graphics, x, y + SECTION_GAP, width, data);
        }
        int columnWidth = (width - PAGE_COLUMN_GAP) / 2;
        int rightX = x + columnWidth + PAGE_COLUMN_GAP;
        drawColumnDivider(graphics, x + columnWidth + PAGE_COLUMN_GAP / 2, y, 244);
        int leftBottom = renderRealmFoundation(graphics, x, y, columnWidth, data);
        int rightBottom = renderRootAndMeditation(graphics, rightX, y, width - columnWidth - PAGE_COLUMN_GAP, data);
        return Math.max(leftBottom, rightBottom);
    }

    private int renderRealmFoundation(GuiGraphics graphics, int x, int y, int width,
                                      ClientCultivationData.Snapshot data) {
        y = sectionTitle(graphics, x, y, width, "境界修为");
        y = row(graphics, x, y, width, "境界", data.realm() + data.stage(), BRONZE);
        y = row(graphics, x, y, width, "神识 / 肉身", shortNumber(data.divSense()) + " / " + data.bodyRef(), PAPER);
        y = row(graphics, x, y, width, "寿元", data.remainingLifespanYears() + " / " + data.lifespanYears()
                + " 年 · 年龄 " + data.ageYears(), PAPER);
        y = row(graphics, x, y, width, "体质", data.specialPhysique(), JADE);
        y = progressBar(graphics, x, y + 2, width, "修为",
                progressFraction(data.cultivation(), data.cultivationMax()),
                shortNumber(data.cultivation()) + " / " + shortNumber(data.cultivationMax()), JADE_LINE);
        y = progressBar(graphics, x, y + 2, width, "灵力",
                fraction(data.mana(), data.manaMax()),
                shortNumber(data.mana()) + " / " + shortNumber(data.manaMax()), SPIRIT_BLUE);

        y += SECTION_GAP;
        y = sectionTitle(graphics, x, y, width, "道途状态");
        y = row(graphics, x, y, width, "当前状态", statusText(data), statusColor(data));
        y = row(graphics, x, y, width, "灵力储备",
                shortNumber(data.spiritualPower()) + " / " + shortNumber(data.maxSpiritualPower()), SPIRIT_BLUE);
        return row(graphics, x, y, width, "所学术法", data.learnedTechniqueCount() + " 门", PAPER);
    }

    private int renderRootAndMeditation(GuiGraphics graphics, int x, int y, int width,
                                        ClientCultivationData.Snapshot data) {
        y = sectionTitle(graphics, x, y, width, "灵根资质");
        y = row(graphics, x, y, width, "测灵 / 觉醒",
                yesNo(data.spiritualRootTested()) + " / " + yesNo(data.spiritualRootAwakened()), PAPER);
        y = row(graphics, x, y, width, "灵根", data.spiritualRoot(), JADE);
        y = row(graphics, x, y, width, "属性", data.spiritualRootAttributes(), PAPER);
        y = progressBar(graphics, x, y + 2, width, "灵根纯度",
                fraction(data.spiritualRootPurity(), 100), data.spiritualRootPurity() + "%", JADE_LINE);
        y = row(graphics, x, y, width, "修炼速率", "×" + formatDouble(data.cultivationSpeedMultiplier()), BRONZE);
        y = row(graphics, x, y, width, "根骨加成", "灵根 ×" + formatDouble(data.rootCultivationSpeedCoefficient())
                + " · 体质 ×" + formatDouble(data.physiqueCultivationSpeedMultiplier()), PAPER_MUTED);

        y += SECTION_GAP;
        y = sectionTitle(graphics, x, y, width, "吐纳周天");
        y = row(graphics, x, y, width, "周遭灵气", data.auraNature() + " · " + data.auraConcentration(), SPIRIT_BLUE);
        y = row(graphics, x, y, width, "吐纳所得", formatDouble(data.meditationTotalPerSecond()) + " / 秒", JADE);
        y = row(graphics, x, y, width, "基础 / 灵根", formatDouble(data.meditationBasePerSecond())
                + " / ×" + formatDouble(data.meditationRootMultiplier()), PAPER);
        y = row(graphics, x, y, width, "体质 / 灵气", "×" + formatDouble(data.meditationPhysiqueMultiplier())
                + " / ×" + formatDouble(data.meditationAuraMultiplier()), PAPER);
        return row(graphics, x, y, width, "功法 / 灵石", "×" + formatDouble(data.meditationTechniqueMultiplier())
                + " / +" + formatDouble(data.meditationStoneBonus()), PAPER_MUTED);
    }

    private int renderCombatPage(GuiGraphics graphics, int x, int y, int width,
                                 ClientCultivationData.Snapshot data) {
        if (!usesPageColumns(width)) {
            y = renderCombatFoundation(graphics, x, y, width, data);
            y = renderBreakthrough(graphics, x, y + SECTION_GAP, width, data);
            return renderAfflictions(graphics, x, y + SECTION_GAP, width, data);
        }
        int columnWidth = (width - PAGE_COLUMN_GAP) / 2;
        int rightX = x + columnWidth + PAGE_COLUMN_GAP;
        drawColumnDivider(graphics, x + columnWidth + PAGE_COLUMN_GAP / 2, y, 244);
        int leftBottom = renderCombatFoundation(graphics, x, y, columnWidth, data);
        int rightBottom = renderBreakthrough(graphics, rightX, y,
                width - columnWidth - PAGE_COLUMN_GAP, data);
        rightBottom = renderAfflictions(graphics, rightX, rightBottom + SECTION_GAP,
                width - columnWidth - PAGE_COLUMN_GAP, data);
        return Math.max(leftBottom, rightBottom);
    }

    private int renderCombatFoundation(GuiGraphics graphics, int x, int y, int width,
                                       ClientCultivationData.Snapshot data) {
        y = sectionTitle(graphics, x, y, width, "战力道基");
        y = row(graphics, x, y, width, "攻伐 / 护体",
                formatDouble(data.baseAttack()) + " / " + formatDouble(data.baseDefense()), PAPER);
        y = row(graphics, x, y, width, "会心 / 会伤",
                percent(data.critChance()) + " / " + formatDouble(data.critDamage()) + "×", BRONZE);
        y = row(graphics, x, y, width, "闪避 / 命中",
                percent(data.dodgeChance()) + " / " + percent(data.accuracy()), PAPER);
        double defenseReduction = data.baseDefense() / (data.baseDefense() + 100.0D);
        y = row(graphics, x, y, width, "护体减伤", percent(defenseReduction), JADE);
        y = row(graphics, x, y, width, "身法运转", percent(data.movementSpeedScale()), SPIRIT_BLUE);
        y = row(graphics, x, y, width, "身法加成", percent(data.movementSpeedBonus()), PAPER_MUTED);

        y += SECTION_GAP;
        y = sectionTitle(graphics, x, y, width, "临敌气象");
        y = row(graphics, x, y, width, "神识强度", shortNumber(data.divSense()), PAPER);
        y = row(graphics, x, y, width, "肉身根基", Integer.toString(data.bodyRef()), PAPER);
        y = row(graphics, x, y, width, "走火风险", data.qiDevRisk() + "%", dangerColor(data.qiDevRisk() > 0));
        return row(graphics, x, y, width, "抗劫底蕴", data.tribRes() + "%", JADE);
    }

    private int renderBreakthrough(GuiGraphics graphics, int x, int y, int width,
                                   ClientCultivationData.Snapshot data) {
        y = sectionTitle(graphics, x, y, width, "破境天劫");
        y = row(graphics, x, y, width, "结丹品相", data.goldCoreGrade() + " · " + data.goldCoreScore() + " 分",
                data.goldCoreScore() > 0 ? BRONZE : PAPER_MUTED);
        y = row(graphics, x, y, width, "五行圆满", data.completeFiveElements() ? "五行合一" : "未合五行",
                data.completeFiveElements() ? JADE : PAPER_MUTED);
        y = row(graphics, x, y, width, "突破把握", percent(data.breakthroughChance()), BRONZE);
        y = row(graphics, x, y, width, "执念加成", percent(data.breakthroughObsessionBonus()), PAPER);
        y = row(graphics, x, y, width, "丹药 / 灵眼", percent(data.breakthroughPillBonus())
                + " / " + percent(data.breakthroughSpiritEyeBonus()), PAPER_MUTED);
        y = row(graphics, x, y, width, "功法助力", percent(data.breakthroughTechniqueQualityBonus()), PAPER_MUTED);
        if (data.tribulationActive()) {
            y = row(graphics, x, y, width, "天劫",
                    data.tribulationTargetRealm() + " · " + data.tribulationCurrentStrike() + "/"
                            + data.tribulationTotalStrikes(), CINNABAR_BRIGHT);
            y = row(graphics, x, y, width, "下道劫雷",
                    Math.max(0, (int)Math.ceil(data.tribulationNextStrikeTicks() / 20.0D)) + " 秒", CINNABAR_BRIGHT);
        } else {
            y = row(graphics, x, y, width, "天劫", "未临劫云", PAPER_MUTED);
        }
        return row(graphics, x, y, width, "失败次数", Integer.toString(data.failedBreakthroughs()),
                data.failedBreakthroughs() > 0 ? WARNING : PAPER_MUTED);
    }

    private int renderAfflictions(GuiGraphics graphics, int x, int y, int width,
                                  ClientCultivationData.Snapshot data) {
        y = sectionTitle(graphics, x, y, width, "伤势心魔");
        y = row(graphics, x, y, width, "总览", statusText(data), statusColor(data));
        y = row(graphics, x, y, width, "重伤", data.severeInjury() ? "存在 · 气血恢复受损" : "无",
                data.severeInjury() ? CINNABAR_BRIGHT : JADE);
        y = row(graphics, x, y, width, "心魔 / 碎丹",
                (data.heartDemonLevel() > 0 ? data.heartDemonLevel() + " 层" : "无")
                        + " / " + (data.shatteredCore() ? "存在" : "无"),
                data.heartDemonLevel() > 0 || data.shatteredCore() ? CINNABAR_BRIGHT : JADE);
        return row(graphics, x, y, width, "跌境伤痕",
                data.realmFallScars() > 0 ? data.realmFallScars() + " 道" : "无",
                data.realmFallScars() > 0 ? WARNING : JADE);
    }

    private int renderStudyPage(GuiGraphics graphics, int x, int y, int width) {
        if (!usesPageColumns(width)) {
            y = renderMethodsAndTechniques(graphics, x, y, width);
            return renderSkillPractice(graphics, x, y + SECTION_GAP, width);
        }
        int columnWidth = (width - PAGE_COLUMN_GAP) / 2;
        int rightX = x + columnWidth + PAGE_COLUMN_GAP;
        drawColumnDivider(graphics, x + columnWidth + PAGE_COLUMN_GAP / 2, y, 260);
        int leftBottom = renderMethodsAndTechniques(graphics, x, y, columnWidth);
        int rightBottom = renderSkillPractice(graphics, rightX, y,
                width - columnWidth - PAGE_COLUMN_GAP);
        return Math.max(leftBottom, rightBottom);
    }

    private int renderMethodsAndTechniques(GuiGraphics graphics, int x, int y, int width) {
        y = sectionTitle(graphics, x, y, width, "功法玉简");
        if (ClientMethodData.isSynced()) {
            int learned = ClientMethodData.getLearnedMethodCount();
            y = row(graphics, x, y, width, "已学功法", learned + " 门", learned > 0 ? JADE : PAPER_MUTED);
            List<String> lines = ClientMethodData.displayLines(5);
            if (lines.isEmpty()) {
                y = row(graphics, x, y, width, "名录", "尚未修习功法", PAPER_MUTED);
            } else {
                for (String line : lines) {
                    y = row(graphics, x, y, width, "·", line, PAPER);
                }
                if (learned > lines.size()) {
                    y = row(graphics, x, y, width, "余录", "+" + (learned - lines.size()) + " 门", BRONZE);
                }
            }
        } else {
            y = row(graphics, x, y, width, "功法名录", "等待服务器同步", PAPER_MUTED);
        }

        y += SECTION_GAP;
        y = sectionTitle(graphics, x, y, width, "术法所学");
        return renderTechniqueCards(graphics, x, y, width);
    }

    private int renderTechniqueCards(GuiGraphics graphics, int x, int y, int width) {
        List<String> techniques = ClientTechniqueData.getLearnedTechniques();
        if (techniques.isEmpty()) {
            return row(graphics, x, y, width, "玉简", "暂无已同步术法", PAPER_MUTED);
        }
        int shown = Math.min(6, techniques.size());
        for (int i = 0; i < shown; i++) {
            String techniqueId = techniques.get(i);
            ClientTechniqueData.TechniqueSummary summary = ClientTechniqueData.getTechniqueSummary(techniqueId);
            drawTechniqueCard(graphics, x, y, width, techniqueId, summary);
            y += 25;
        }
        if (techniques.size() > shown) {
            y = row(graphics, x, y, width, "余录", "+" + (techniques.size() - shown) + " 门 · 可在技能编辑查看", BRONZE);
        }
        return y;
    }

    private int renderSkillPractice(GuiGraphics graphics, int x, int y, int width) {
        y = sectionTitle(graphics, x, y, width, "生活百艺");
        if (!ClientSkillData.isSynced()) {
            y = row(graphics, x, y, width, "技能",
                    Component.translatable("screen.seeking_immortals.skill_tree.waiting_sync").getString(), PAPER_MUTED);
        } else {
            for (LifeSkillEntry entry : LIFE_SKILLS) {
                ClientSkillData.SkillSnapshot skill = ClientSkillData.get(entry.type());
                y = row(graphics, x, y, width, entry.label(), skillSummary(entry.type(), skill),
                        skill.unlocked() ? PAPER : PAPER_MUTED);
            }
        }

        y += SECTION_GAP;
        y = sectionTitle(graphics, x, y, width, "异术旁门");
        if (!ClientSkillData.isSynced()) {
            return row(graphics, x, y, width, "异术",
                    Component.translatable("screen.seeking_immortals.skill_tree.waiting_sync").getString(), PAPER_MUTED);
        }
        for (LifeSkillEntry entry : SPECIAL_SKILLS) {
            ClientSkillData.SkillSnapshot skill = ClientSkillData.get(entry.type());
            y = row(graphics, x, y, width, entry.label(), skillSummary(entry.type(), skill),
                    skill.unlocked() ? PAPER : PAPER_MUTED);
        }
        return y;
    }

    private String skillSummary(SkillType type, ClientSkillData.SkillSnapshot skill) {
        String realm = type.getRequiredRealm() == null ? "-" : type.getRequiredRealm().getDisplayName();
        if (!skill.unlocked()) {
            return Component.translatable("screen.seeking_immortals.skill_tree.locked_requirement", realm).getString();
        }
        int maxLevel = LifeSkillTreeScreen.skillMaxLevel(type);
        Component next = skill.level() >= maxLevel
                ? Component.translatable("screen.seeking_immortals.skill_tree.maxed")
                : Component.literal(Integer.toString(LifeSkillTreeScreen.experienceForNextLevel(skill.level())));
        return Component.translatable("screen.seeking_immortals.skill_tree.summary",
                skill.level(), maxLevel, skill.experience(), next,
                Math.round(skill.proficiency() / 100.0D)).getString();
    }

    private void drawTechniqueCard(GuiGraphics graphics, int x, int y, int width, String techniqueId,
                                   ClientTechniqueData.TechniqueSummary summary) {
        if (graphics == null) {
            return;
        }
        graphics.fill(x, y, x + width, y + 22, BRONZE_DIM);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 21, INK_SOFT);
        int iconX = x + 3;
        int iconY = y + 3;
        if (ImmortalUiSkin.hasSkillIcon(techniqueId)) {
            ImmortalUiSkin.drawSkillIcon(graphics, iconX, iconY, 16, techniqueId);
        } else {
            graphics.fill(iconX, iconY, iconX + 16, iconY + 16, CINNABAR);
            graphics.fill(iconX + 1, iconY + 1, iconX + 15, iconY + 15, 0xFF532823);
            String mark = summary.name().isBlank() ? "术" : summary.name().substring(0, 1);
            graphics.drawCenteredString(font, mark, iconX + 8, iconY + 4, PAPER);
        }
        drawFit(graphics, summary.name(), x + 23, y + 3, Math.max(8, width - 27), BRONZE);
        drawFit(graphics, summary.source() + " · " + summary.attribute(), x + 23, y + 12,
                Math.max(8, width - 27), PAPER_MUTED);
    }

    private void drawJournalFrame(GuiGraphics graphics, PanelLayout layout) {
        ImmortalUiSkin.drawLayeredPanel(graphics, layout.left(), layout.top(),
                layout.panelWidth(), layout.panelHeight());
        ImmortalUiSkin.drawTitleBar(graphics, layout.header().x(), layout.header().y(),
                layout.header().width(), layout.header().height());
        ImmortalUiSkin.drawInnerFrame(graphics, layout.content().x(), layout.content().y(),
                layout.content().width(), layout.content().height());
        if (layout.wide()) {
            ImmortalUiSkin.drawInnerFrame(graphics, layout.profile().x(), layout.profile().y(),
                    layout.profile().width(), layout.profile().height());
        }

        int tabRailY = layout.foundationTab().bottom() + 1;
        graphics.fill(layout.content().x(), tabRailY, layout.content().right(), tabRailY + 1, BRONZE_DIM);
        graphics.fill(layout.content().x(), layout.closeButton().y() - 4,
                layout.content().right(), layout.closeButton().y() - 3, BRONZE_DIM);
    }

    private void drawHeader(GuiGraphics graphics, PanelLayout layout, ClientCultivationData.Snapshot data) {
        UiRect header = layout.header();
        String title = Component.translatable("screen.seeking_immortals.cultivation_stats.journal_title").getString();
        int titleY = header.y() + (header.height() >= 28 ? 5 : 3);
        int titleAreaRight = header.height() >= 28 ? header.right() - 30 : header.right();
        graphics.drawCenteredString(font, fit(title, Math.max(12, titleAreaRight - header.x() - 8)),
                (header.x() + titleAreaRight) / 2, titleY, BRONZE);
        if (header.height() >= 28) {
            String name = player == null ? "" : player.getName().getString();
            String subtitle = ClientCultivationData.isSynced()
                    ? layout.wide()
                            ? data.auraNature() + " · " + statusText(data)
                            : name + (name.isBlank() ? "" : " · ") + data.realm() + data.stage() + " · " + statusText(data)
                    : Component.translatable("screen.seeking_immortals.cultivation_stats.waiting_sync").getString();
            drawFit(graphics, subtitle, header.x() + 8, header.y() + 20,
                    Math.max(20, header.width() - 42), ClientCultivationData.isSynced() ? JADE : PAPER_MUTED);
            drawSeal(graphics, header.right() - 25, header.y() + 4, 20,
                    Component.translatable("screen.seeking_immortals.cultivation_stats.seal").getString());
        }
    }

    private void drawProfile(GuiGraphics graphics, UiRect rect, ClientCultivationData.Snapshot data,
                             int mouseX, int mouseY) {
        if (rect.width() <= 0 || rect.height() <= 0) {
            return;
        }
        String name = player == null ? "无名散修" : player.getName().getString();
        graphics.drawCenteredString(font, fit(name, rect.width() - 12), rect.x() + rect.width() / 2,
                rect.y() + 7, PAPER);
        graphics.fill(rect.x() + 9, rect.y() + 18, rect.right() - 9, rect.y() + 19, BRONZE_DIM);

        int infoY = rect.y() + 26;
        if (player != null && rect.height() >= 150) {
            int modelBottom = rect.y() + 111;
            int scale = Math.min(38, Math.max(22, rect.width() / 3));
            graphics.enableScissor(rect.x() + 2, rect.y() + 20, rect.right() - 2, rect.y() + 116);
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                    rect.x() + rect.width() / 2, modelBottom, scale,
                    rect.x() + rect.width() / 2.0F - mouseX,
                    rect.y() + 58.0F - mouseY, player);
            graphics.disableScissor();
            infoY = rect.y() + 120;
        }

        graphics.drawCenteredString(font, fit(data.realm() + data.stage(), rect.width() - 10),
                rect.x() + rect.width() / 2, infoY, BRONZE);
        infoY += 14;
        graphics.fill(rect.x() + 10, infoY - 3, rect.right() - 10, infoY - 2, BRONZE_DIM);
        infoY = profileLine(graphics, rect, infoY, "寿元", data.remainingLifespanYears() + " 年", PAPER);
        infoY = profileLine(graphics, rect, infoY, "灵根", data.spiritualRoot(), JADE);
        infoY = profileLine(graphics, rect, infoY, "灵气", data.auraNature(), SPIRIT_BLUE);
        infoY = profileLine(graphics, rect, infoY, "状态", statusText(data), statusColor(data));
        profileLine(graphics, rect, infoY, "修速", "×" + formatDouble(data.cultivationSpeedMultiplier()), BRONZE);

        if (rect.height() >= 210) {
            drawSeal(graphics, rect.x() + rect.width() / 2 - 10, rect.bottom() - 27, 20,
                    data.meditating() ? "定" : "修");
        }
    }

    private int profileLine(GuiGraphics graphics, UiRect rect, int y, String label, String value, int color) {
        if (y + LINE_HEIGHT >= rect.bottom() - 28) {
            return y;
        }
        drawFit(graphics, label, rect.x() + 8, y, 28, PAPER_MUTED);
        drawFit(graphics, value, rect.x() + 38, y, Math.max(8, rect.width() - 46), color);
        return y + LINE_HEIGHT;
    }

    private void drawWaitingForSync(GuiGraphics graphics, UiRect viewport) {
        int x = viewport.x() + 8;
        int y = viewport.y() + 10;
        drawFit(graphics, Component.translatable("screen.seeking_immortals.cultivation_stats.waiting_sync").getString(),
                x, y, Math.max(20, viewport.width() - 16), PAPER_MUTED);
        if (viewport.height() >= 34) {
            drawFit(graphics, Component.translatable("screen.seeking_immortals.cultivation_stats.waiting_hint").getString(),
                    x, y + LINE_HEIGHT, Math.max(20, viewport.width() - 16), PAPER_MUTED);
        }
    }

    private int sectionTitle(GuiGraphics graphics, int x, int y, int width, String title) {
        if (graphics != null) {
            ImmortalUiSkin.drawTitleBar(graphics, x, y, width, 14);
            drawFit(graphics, title, x + 8, y + 3, Math.max(8, width - 12), BRONZE);
        }
        return y + 18;
    }

    private int row(GuiGraphics graphics, int x, int y, int width, String label, String value, int color) {
        if (graphics != null) {
            if (((y / LINE_HEIGHT) & 1) == 0) {
                graphics.fill(x, y - 1, x + width, y + 10, INK_ROW);
            }
            int labelWidth = Math.min(68, Math.max(38, width / 3));
            graphics.fill(x + 2, y + 3, x + 3, y + 7, JADE_LINE);
            drawFit(graphics, label, x + 6, y, Math.max(8, labelWidth - 6), PAPER_MUTED);
            drawFit(graphics, value, x + labelWidth + 4, y,
                    Math.max(8, width - labelWidth - 4), color);
        }
        return y + LINE_HEIGHT;
    }

    private int progressBar(GuiGraphics graphics, int x, int y, int width, String label,
                            double fraction, String value, int fillColor) {
        if (graphics != null) {
            drawFit(graphics, label, x + 2, y, Math.max(20, width / 2), PAPER_MUTED);
            int valueWidth = font.width(value);
            drawFit(graphics, value, Math.max(x + width / 2, x + width - valueWidth), y,
                    Math.max(8, width / 2), PAPER);
            ImmortalUiSkin.StatusBarStyle style = fillColor == SPIRIT_BLUE
                    ? ImmortalUiSkin.StatusBarStyle.SPIRIT : ImmortalUiSkin.StatusBarStyle.CULTIVATION;
            ImmortalUiSkin.drawSemanticStatusBar(graphics, x, y + 10, width, 7, fraction, style);
        }
        return y + 21;
    }

    private void drawScrollBar(GuiGraphics graphics, UiRect viewport, int contentHeight, int offset) {
        ImmortalUiSkin.drawThinScrollbar(graphics, viewport.right() - 3, viewport.y(), viewport.height(),
                contentHeight, viewport.height(), offset);
    }

    private void drawColumnDivider(GuiGraphics graphics, int x, int y, int height) {
        if (graphics != null) {
            ImmortalUiSkin.drawVerticalDivider(graphics, x, y + 2, Math.max(0, height - 2));
        }
    }

    private void drawSeal(GuiGraphics graphics, int x, int y, int size, String mark) {
        graphics.fill(x, y, x + size, y + size, CINNABAR_BRIGHT);
        graphics.fill(x + 2, y + 2, x + size - 2, y + size - 2, 0xFF582A24);
        graphics.fill(x + 4, y + 4, x + size - 4, y + size - 4, CINNABAR);
        graphics.drawCenteredString(font, fit(mark, size - 6), x + size / 2, y + Math.max(3, (size - 8) / 2), PAPER);
    }

    private void drawFit(GuiGraphics graphics, String value, int x, int y, int maxWidth, int color) {
        ImmortalUiSkin.drawStringFit(font, graphics, value == null ? "" : value,
                x, y, Math.max(0, maxWidth), color, false);
    }

    private String fit(String value, int width) {
        return ImmortalUiSkin.fitWidth(font, value == null ? "" : value, Math.max(0, width));
    }

    private double progressFraction(long value, long max) {
        return max <= 0 ? 0.0D : value / (double)max;
    }

    private double fraction(int value, int max) {
        return max <= 0 ? 0.0D : value / (double)max;
    }

    private String percent(double fraction) {
        return String.format(Locale.ROOT, "%.0f%%", clamp01(fraction) * 100.0D);
    }

    private String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String shortNumber(long value) {
        double abs = Math.abs((double)value);
        if (abs >= 1_000_000_000D) return String.format(Locale.ROOT, "%.1fB", value / 1_000_000_000D);
        if (abs >= 1_000_000D) return String.format(Locale.ROOT, "%.1fM", value / 1_000_000D);
        if (abs >= 10_000D) return String.format(Locale.ROOT, "%.1f万", value / 10_000D);
        return Long.toString(value);
    }

    private int dangerColor(boolean danger) {
        return danger ? CINNABAR_BRIGHT : PAPER;
    }

    private int statusColor(ClientCultivationData.Snapshot data) {
        return hasAffliction(data) ? CINNABAR_BRIGHT : JADE;
    }

    private boolean hasAffliction(ClientCultivationData.Snapshot data) {
        return data.severeInjury()
                || data.heartDemonLevel() > 0
                || data.shatteredCore()
                || data.tribulationActive()
                || data.realmFallScars() > 0;
    }

    private String statusText(ClientCultivationData.Snapshot data) {
        StringBuilder builder = new StringBuilder();
        if (data.meditating()) builder.append("打坐 ");
        if (data.severeInjury()) builder.append("重伤 ");
        if (data.heartDemonLevel() > 0) builder.append("心魔").append(data.heartDemonLevel()).append("层 ");
        if (data.shatteredCore()) builder.append("碎丹 ");
        if (data.tribulationActive()) builder.append("天劫中 ");
        if (data.realmFallScars() > 0) builder.append("跌境伤痕").append(data.realmFallScars()).append(' ');
        return builder.isEmpty() ? "道基平稳" : builder.toString().trim();
    }

    private String yesNo(boolean value) {
        return value ? "是" : "否";
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    static int statusBarHighlightColor() {
        return BAR_HIGHLIGHT;
    }

    enum StatsTab {
        FOUNDATION("screen.seeking_immortals.cultivation_stats.tab.foundation"),
        COMBAT("screen.seeking_immortals.cultivation_stats.tab.combat"),
        STUDY("screen.seeking_immortals.cultivation_stats.tab.study");

        private final String translationKey;

        StatsTab(String translationKey) {
            this.translationKey = translationKey;
        }

        Component title() {
            return Component.translatable(translationKey);
        }
    }

    record UiRect(int x, int y, int width, int height) {
        int right() {
            return x + width;
        }

        int bottom() {
            return y + height;
        }

        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }

        boolean intersects(UiRect other) {
            return x < other.right() && right() > other.x() && y < other.bottom() && bottom() > other.y();
        }
    }

    record PanelLayout(
            int left,
            int top,
            int panelWidth,
            int panelHeight,
            boolean wide,
            boolean showsMovementSlider,
            UiRect header,
            UiRect profile,
            UiRect foundationTab,
            UiRect combatTab,
            UiRect studyTab,
            UiRect content,
            UiRect breakthroughButton,
            UiRect methodTreeButton,
            UiRect skillTreeButton,
            UiRect closeButton,
            UiRect slider) {

        UiRect pageViewport(StatsTab tab) {
            int bottom = tab == StatsTab.COMBAT && showsMovementSlider
                    ? Math.max(content.y() + 1, slider.y() - 4)
                    : content.bottom();
            return new UiRect(content.x(), content.y(), content.width(), Math.max(1, bottom - content.y()));
        }
    }

    private record LifeSkillEntry(String label, SkillType type) {}

    private final class JournalTabButton extends Button {
        private final StatsTab tab;

        JournalTabButton(UiRect rect, StatsTab tab) {
            super(rect.x(), rect.y(), rect.width(), rect.height(), tab.title(),
                    button -> selectTab(tab), DEFAULT_NARRATION);
            this.tab = tab;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean selected = activeTab == tab;
            int textColor = selected ? PAPER : isHoveredOrFocused() ? JADE : PAPER_MUTED;
            ImmortalUiSkin.InteractionState state = selected ? ImmortalUiSkin.InteractionState.SELECTED
                    : isHoveredOrFocused() ? ImmortalUiSkin.InteractionState.HOVERED
                    : ImmortalUiSkin.InteractionState.NORMAL;
            ImmortalUiSkin.drawTab(graphics, getX(), getY(), getWidth(), getHeight(), state);
            FontHolder.drawCentered(graphics, getMessage(), getX(), getY(), getWidth(), getHeight(), textColor);
        }
    }

    private static final class MovementSpeedSlider extends AbstractSliderButton {
        private boolean syncing;
        private double pendingScale = Double.NaN;

        MovementSpeedSlider(int x, int y, int width, int height, double scale) {
            super(x, y, width, height, Component.empty(), step(scale));
            updateMessage();
        }

        void syncFromSnapshot(double scale) {
            double stepped = step(scale);
            if (!Double.isNaN(pendingScale)) {
                if (Math.abs(stepped - pendingScale) < 0.0001D) {
                    pendingScale = Double.NaN;
                } else {
                    return;
                }
            }
            if (Math.abs(value - stepped) < 0.0001D) {
                return;
            }
            syncing = true;
            value = stepped;
            updateMessage();
            syncing = false;
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("screen.seeking_immortals.cultivation_stats.movement_scale",
                    Math.round(value * 100.0D)));
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            int width = getWidth();
            int height = getHeight();
            ImmortalUiSkin.drawButtonBackground(graphics, x, y, width, height,
                    isHoveredOrFocused(), active, false);

            int trackX = x + 7;
            int trackWidth = Math.max(1, width - 14);
            int trackY = height >= 18 ? y + height - 6 : y + height - 4;
            int progressWidth = Math.max(0, Math.min(trackWidth,
                    (int)Math.round(trackWidth * clamp01(value))));
            ImmortalUiSkin.drawSemanticStatusBar(graphics, trackX, trackY, trackWidth, 3, value,
                    ImmortalUiSkin.StatusBarStyle.CULTIVATION);
            int thumbCenter = trackX + progressWidth;
            graphics.fill(thumbCenter - 2, trackY - 2, thumbCenter + 3, trackY + 4, BRONZE);
            graphics.fill(thumbCenter - 1, trackY - 1, thumbCenter + 2, trackY + 3, CINNABAR);

            int textY = height >= 18 ? y + 2 : y + Math.max(1, (height - 8) / 2);
            FontHolder.drawFit(graphics, getMessage(), x + 6, textY,
                    Math.max(8, width - 12), active ? PAPER : PAPER_MUTED);
        }

        @Override
        protected void applyValue() {
            value = step(value);
            if (!syncing) {
                pendingScale = value;
                ModNetwork.CHANNEL.sendToServer(new SetMovementSpeedScalePacket(value));
            }
        }

        private static double step(double value) {
            return clamp01(Math.round(value / MOVEMENT_SLIDER_STEP) * MOVEMENT_SLIDER_STEP);
        }
    }

    private static final class FontHolder {
        private FontHolder() {}

        static void drawCentered(GuiGraphics graphics, Component message, int x, int y,
                                 int width, int height, int color) {
            var font = Minecraft.getInstance().font;
            String fitted = ImmortalUiSkin.fitWidth(font, message.getString(), Math.max(0, width - 8));
            graphics.drawCenteredString(font, fitted, x + width / 2,
                    y + Math.max(1, (height - 8) / 2), color);
        }

        static void drawFit(GuiGraphics graphics, Component message, int x, int y, int width, int color) {
            var font = Minecraft.getInstance().font;
            ImmortalUiSkin.drawStringFit(font, graphics, message.getString(), x, y, width, color, false);
        }
    }
}
