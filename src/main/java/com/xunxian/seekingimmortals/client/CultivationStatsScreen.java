package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.network.AttemptBreakthroughPacket;
import com.xunxian.seekingimmortals.network.ModNetwork;
import com.xunxian.seekingimmortals.network.SetMovementSpeedScalePacket;
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
    private static final int DEFAULT_PANEL_WIDTH = 456;
    private static final int DEFAULT_PANEL_HEIGHT = 340;
    private static final int MIN_AVAILABLE_WIDTH = 112;
    private static final int MIN_AVAILABLE_HEIGHT = 82;
    private static final int TWO_COLUMN_WIDTH = 372;
    private static final int LINE_HEIGHT = 11;
    private static final int SECTION_GAP = 6;
    private static final int PANEL_MARGIN = 4;
    private static final int CONTENT_PADDING = 12;
    private static final int COLUMN_GAP = 12;
    private static final double MOVEMENT_SLIDER_STEP = 0.05D;

    private static final int INK_SOFT = 0xAA2A1B0D;
    private static final int INK_STRIP = 0x9920150A;
    private static final int GOLD = 0xFFE6D59A;
    private static final int GOLD_DIM = 0x88E6D59A;
    private static final int TEXT = 0xFFEFE4C2;
    private static final int TEXT_MUTED = 0xFFBFAF8A;
    private static final int JADE = 0xFFB8F5A2;
    private static final int SPIRIT_BLUE = 0xFF7FDCE8;
    private static final int BLOOD_RED = 0xFFFF8A8A;
    private static final int WARNING_ORANGE = 0xFFFFC06A;
    private static final int BAR_BACKING = 0x99130C05;
    private static final int BAR_BORDER = 0xAAE6D59A;
    private static final int BAR_HIGHLIGHT = 0x443B2F18;
    private static final int CONTROL_BACKING = 0xDD1A1007;
    private static final int CONTROL_BACKING_HOVERED = 0xDD2B1C0D;
    private static final int CONTROL_DISABLED = 0xAA100B06;
    private static final int CONTROL_TRACK = 0xAA130C05;
    private static final int CONTROL_PROGRESS = 0xCC2F8F45;
    private static final int CONTROL_THUMB = 0xFFE6D59A;

    private final LocalPlayer player;
    private final boolean returnToInventory;
    private MovementSpeedSlider movementSpeedSlider;

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

        addRenderableWidget(new InkButton(layout.breakthroughButton(),
                Component.translatable("screen.seeking_immortals.cultivation_stats.breakthrough"),
                button -> ModNetwork.CHANNEL.sendToServer(new AttemptBreakthroughPacket())));
        // Wave478: open interactive method tree (catalog browse + server learn).
        addRenderableWidget(new InkButton(layout.methodTreeButton(),
                Component.translatable("screen.seeking_immortals.cultivation_stats.methods"),
                button -> {
                    if (minecraft != null) {
                        minecraft.setScreen(new MethodTreeScreen(this));
                    }
                }));
        addRenderableWidget(new InkButton(layout.closeButton(), closeButtonLabel(returnToInventory), button -> onClose()));

        movementSpeedSlider = new MovementSpeedSlider(layout.slider().x(), layout.slider().y(),
                layout.slider().width(), layout.slider().height(), ClientCultivationData.getSnapshot().movementSpeedScale());
        addRenderableWidget(movementSpeedSlider);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        renderCultivationPanel(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (returnToInventory && player != null) {
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

    static int calculatePanelWidth(int screenWidth) {
        int available = Math.max(MIN_AVAILABLE_WIDTH, screenWidth - PANEL_MARGIN * 2);
        return Math.min(DEFAULT_PANEL_WIDTH, available);
    }

    static int calculatePanelHeight(int screenHeight) {
        int available = Math.max(MIN_AVAILABLE_HEIGHT, screenHeight - PANEL_MARGIN * 2);
        return Math.min(DEFAULT_PANEL_HEIGHT, available);
    }

    static boolean usesTwoColumns(int panelWidth) {
        return panelWidth >= TWO_COLUMN_WIDTH;
    }

    static PanelLayout calculateLayout(int screenWidth, int screenHeight) {
        int panelWidth = calculatePanelWidth(screenWidth);
        int panelHeight = calculatePanelHeight(screenHeight);
        int left = Math.max(2, (screenWidth - panelWidth) / 2);
        int top = Math.max(2, (screenHeight - panelHeight) / 2);
        int innerLeft = left + CONTENT_PADDING;
        int innerWidth = Math.max(40, panelWidth - CONTENT_PADDING * 2);
        int buttonHeight = panelHeight < 118 ? 14 : panelHeight < 180 ? 16 : 20;
        // Wave478: three bottom actions — breakthrough / methods / close, equal slots with gap.
        int buttonGap = 4;
        int buttonWidth = Math.max(28, Math.min(78, (innerWidth - buttonGap * 2) / 3));
        int buttonY = top + panelHeight - buttonHeight - 6;
        int totalButtonsWidth = buttonWidth * 3 + buttonGap * 2;
        int buttonsStartX = innerLeft + Math.max(0, (innerWidth - totalButtonsWidth) / 2);
        UiRect breakthroughButton = new UiRect(buttonsStartX, buttonY, buttonWidth, buttonHeight);
        UiRect methodTreeButton = new UiRect(buttonsStartX + buttonWidth + buttonGap, buttonY, buttonWidth, buttonHeight);
        UiRect closeButton = new UiRect(buttonsStartX + (buttonWidth + buttonGap) * 2, buttonY, buttonWidth, buttonHeight);
        int sliderHeight = buttonHeight;
        int sliderY = Math.max(top + 18, buttonY - sliderHeight - 4);
        UiRect slider = new UiRect(innerLeft, sliderY, innerWidth, sliderHeight);

        boolean twoColumns = usesTwoColumns(panelWidth);
        int contentTop = top + (panelHeight >= 170 ? 48 : panelHeight >= 118 ? 34 : 10);
        int contentBottom = Math.max(contentTop + 1, slider.y() - 6);
        int columnWidth = twoColumns ? Math.max(40, (innerWidth - COLUMN_GAP) / 2) : innerWidth;
        int rightX = twoColumns ? innerLeft + columnWidth + COLUMN_GAP : innerLeft;
        int rightWidth = twoColumns ? Math.max(40, innerWidth - columnWidth - COLUMN_GAP) : innerWidth;
        return new PanelLayout(left, top, panelWidth, panelHeight, twoColumns, contentTop, contentBottom,
                innerLeft, columnWidth, rightX, rightWidth, breakthroughButton, methodTreeButton, closeButton, slider);
    }

    private void renderCultivationPanel(GuiGraphics graphics) {
        PanelLayout layout = calculateLayout(width, height);
        ClientCultivationData.Snapshot data = ClientCultivationData.getSnapshot();
        if (movementSpeedSlider != null) {
            movementSpeedSlider.syncFromSnapshot(data.movementSpeedScale());
        }

        drawInkPanel(graphics, layout);
        drawHeader(graphics, layout, data);
        if (!ClientCultivationData.isSynced()) {
            drawWaitingForSync(graphics, layout);
            return;
        }

        if (layout.twoColumns()) {
            renderPathColumn(graphics, layout.leftColumnX(), layout.contentTop(), layout.leftColumnWidth(), layout.contentBottom(), data);
            renderFoundationColumn(graphics, layout.rightColumnX(), layout.contentTop(), layout.rightColumnWidth(), layout.contentBottom(), data);
        } else {
            int y = layout.contentTop();
            y = renderPathColumn(graphics, layout.leftColumnX(), y, layout.leftColumnWidth(), layout.contentBottom(), data);
            y = renderFoundationColumn(graphics, layout.leftColumnX(), y + SECTION_GAP, layout.leftColumnWidth(), layout.contentBottom(), data);
            renderAfflictionSection(graphics, layout.leftColumnX(), y + SECTION_GAP, layout.leftColumnWidth(), layout.contentBottom(), data);
        }
    }

    private int renderPathColumn(GuiGraphics graphics, int x, int y, int width, int bottom, ClientCultivationData.Snapshot data) {
        y = sectionTitle(graphics, x, y, width, bottom, "境界修为");
        y = row(graphics, x, y, width, bottom, "境界", data.realm() + data.stage(), GOLD);
        y = row(graphics, x, y, width, bottom, "神识/肉身", shortNumber(data.divSense()) + " / " + data.bodyRef(), TEXT);
        y = row(graphics, x, y, width, bottom, "寿元", data.remainingLifespanYears() + " / " + data.lifespanYears() + " 年，龄 " + data.ageYears(), TEXT);
        y = row(graphics, x, y, width, bottom, "体质", data.specialPhysique(), JADE);
        y = progressBar(graphics, x, y + 2, width, bottom, "修为",
                progressFraction(data.cultivation(), data.cultivationMax()),
                shortNumber(data.cultivation()) + " / " + shortNumber(data.cultivationMax()), JADE);
        y = progressBar(graphics, x, y + 2, width, bottom, "灵力",
                fraction(data.mana(), data.manaMax()),
                shortNumber(data.mana()) + " / " + shortNumber(data.manaMax()), SPIRIT_BLUE);

        y += SECTION_GAP;
        y = sectionTitle(graphics, x, y, width, bottom, "灵根功法");
        y = row(graphics, x, y, width, bottom, "测灵/觉醒", yesNo(data.spiritualRootTested()) + " / " + yesNo(data.spiritualRootAwakened()), TEXT);
        y = row(graphics, x, y, width, bottom, "灵根", data.spiritualRoot() + " · " + data.spiritualRootAttributes(), JADE);
        y = progressBar(graphics, x, y + 2, width, bottom, "纯度",
                fraction(data.spiritualRootPurity(), 100), data.spiritualRootPurity() + "%", JADE);
        y = row(graphics, x, y, width, bottom, "功法", data.learnedTechniqueCount() + " 门，速率 " + formatDouble(data.cultivationSpeedMultiplier()) + "x", TEXT);
        y = row(graphics, x, y, width, bottom, "根骨加成",
                formatDouble(data.rootCultivationSpeedCoefficient()) + "x / " + formatDouble(data.physiqueCultivationSpeedMultiplier()) + "x", TEXT);
        y = renderMethodCatalogHint(graphics, x, y, width, bottom);
        return renderTechniqueSummaries(graphics, x, y, width, bottom);
    }

    /**
     * Wave476/477: show learned methods when synced; otherwise catalog hint.
     */
    private int renderMethodCatalogHint(GuiGraphics graphics, int x, int y, int width, int bottom) {
        if (ClientMethodData.isSynced()) {
            int learned = ClientMethodData.getLearnedMethodCount();
            y = row(graphics, x, y, width, bottom, "已学功法",
                    learned + " 门", learned > 0 ? JADE : TEXT_MUTED);
            List<String> lines = ClientMethodData.displayLines(4);
            if (lines.isEmpty()) {
                y = row(graphics, x, y, width, bottom, "·", "暂无（/si catalog methods learn）", TEXT_MUTED);
            } else {
                for (String line : lines) {
                    y = row(graphics, x, y, width, bottom, "·", line, TEXT);
                }
                if (learned > lines.size()) {
                    y = row(graphics, x, y, width, bottom, "更多",
                            "+" + (learned - lines.size()) + " 门", GOLD);
                }
            }
            return y;
        }

        int methodCount = 0;
        try {
            methodCount = com.xunxian.seekingimmortals.catalog.TextMaterialCatalogService.builtin().methods().size();
        } catch (Throwable ignored) {
            methodCount = 0;
        }
        if (methodCount <= 0) {
            return y;
        }
        y = row(graphics, x, y, width, bottom, "功法目录",
                methodCount + " 门（等待同步）", TEXT_MUTED);
        return y;
    }

    private int renderFoundationColumn(GuiGraphics graphics, int x, int y, int width, int bottom, ClientCultivationData.Snapshot data) {
        y = sectionTitle(graphics, x, y, width, bottom, "战力道基");
        y = row(graphics, x, y, width, bottom, "攻防", formatDouble(data.baseAttack()) + " / " + formatDouble(data.baseDefense()), TEXT);
        y = row(graphics, x, y, width, bottom, "暴击/暴伤", percent(data.critChance()) + " / " + formatDouble(data.critDamage()) + "x", TEXT);
        y = row(graphics, x, y, width, bottom, "闪避/命中", percent(data.dodgeChance()) + " / " + percent(data.accuracy()), TEXT);
        double defenseReduction = data.baseDefense() / (data.baseDefense() + 100.0D);
        y = row(graphics, x, y, width, bottom, "护体减伤", percent(defenseReduction), JADE);
        y = progressBar(graphics, x, y + 2, width, bottom, "移速",
                data.movementSpeedScale(), percent(data.movementSpeedScale()) + "，加成 " + percent(data.movementSpeedBonus()), SPIRIT_BLUE);

        y += SECTION_GAP;
        y = sectionTitle(graphics, x, y, width, bottom, "劫厄与突破");
        y = row(graphics, x, y, width, bottom, "走火/抗劫", data.qiDevRisk() + "% / " + data.tribRes() + "%", dangerColor(data.qiDevRisk() > 0 || data.tribulationActive()));
        y = row(graphics, x, y, width, bottom, "金丹", data.goldCoreGrade() + " · " + data.goldCoreScore() + " 分", data.goldCoreScore() > 0 ? GOLD : TEXT_MUTED);
        y = row(graphics, x, y, width, bottom, "五行圆满", data.completeFiveElements() ? "五行合一" : "未合五行", data.completeFiveElements() ? JADE : TEXT_MUTED);
        y = row(graphics, x, y, width, bottom, "突破", percent(data.breakthroughChance()) + "，执念 " + percent(data.breakthroughObsessionBonus()), TEXT);
        y = row(graphics, x, y, width, bottom, "助力",
                "丹 " + percent(data.breakthroughPillBonus()) + " / 灵眼 " + percent(data.breakthroughSpiritEyeBonus())
                        + " / 功法 " + percent(data.breakthroughTechniqueQualityBonus()), TEXT_MUTED);
        if (data.tribulationActive()) {
            y = row(graphics, x, y, width, bottom, "天劫",
                    data.tribulationTargetRealm() + " " + data.tribulationCurrentStrike() + "/" + data.tribulationTotalStrikes()
                            + "，下击 " + Math.max(0, (int)Math.ceil(data.tribulationNextStrikeTicks() / 20.0D)) + " 秒",
                    BLOOD_RED);
        } else {
            y = row(graphics, x, y, width, bottom, "天劫", "未临劫云", TEXT_MUTED);
        }
        y = row(graphics, x, y, width, bottom, "失败次数", Integer.toString(data.failedBreakthroughs()), data.failedBreakthroughs() > 0 ? WARNING_ORANGE : TEXT_MUTED);

        y += SECTION_GAP;
        return renderAfflictionSection(graphics, x, y, width, bottom, data);
    }

    private int renderAfflictionSection(GuiGraphics graphics, int x, int y, int width, int bottom, ClientCultivationData.Snapshot data) {
        y = sectionTitle(graphics, x, y, width, bottom, "负面状态");
        y = row(graphics, x, y, width, bottom, "总览", statusText(data), statusColor(data));
        y = row(graphics, x, y, width, bottom, "重伤", data.severeInjury() ? "存在，气血恢复受损" : "无", data.severeInjury() ? BLOOD_RED : JADE);
        y = row(graphics, x, y, width, bottom, "心魔/碎丹",
                (data.heartDemonLevel() > 0 ? data.heartDemonLevel() + " 层" : "无") + " / " + (data.shatteredCore() ? "存在" : "无"),
                data.heartDemonLevel() > 0 || data.shatteredCore() ? BLOOD_RED : JADE);
        return row(graphics, x, y, width, bottom, "跌境伤痕", data.realmFallScars() > 0 ? data.realmFallScars() + " 道" : "无",
                data.realmFallScars() > 0 ? WARNING_ORANGE : JADE);
    }

    private int renderTechniqueSummaries(GuiGraphics graphics, int x, int y, int width, int bottom) {
        List<String> techniques = ClientTechniqueData.getLearnedTechniques();
        if (techniques.isEmpty()) {
            return row(graphics, x, y, width, bottom, "玉简", "暂无已同步功法术法", TEXT_MUTED);
        }
        int maxRows = Math.max(0, Math.min(2, (bottom - y) / 20));
        for (int i = 0; i < Math.min(maxRows, techniques.size()); i++) {
            ClientTechniqueData.TechniqueSummary summary = ClientTechniqueData.getTechniqueSummary(techniques.get(i));
            if (y + 18 > bottom) return y;
            drawFit(graphics, (i + 1) + ". " + summary.name(), x, y, width, GOLD);
            drawFit(graphics, summary.source() + " · " + summary.attribute(), x + 10, y + 10, Math.max(20, width - 10), JADE);
            y += 20;
        }
        if (techniques.size() > maxRows) {
            y = row(graphics, x, y, width, bottom, "更多", "+" + (techniques.size() - maxRows) + " 门可在技能编辑查看", GOLD);
        }
        return y;
    }

    private void drawInkPanel(GuiGraphics graphics, PanelLayout layout) {
        ImmortalUiSkin.drawPanel(graphics, layout.left(), layout.top(), layout.panelWidth(), layout.panelHeight());
        graphics.fill(layout.left() + 7, layout.top() + 7, layout.left() + layout.panelWidth() - 7,
                layout.top() + layout.panelHeight() - 7, INK_SOFT);
        graphics.fill(layout.left() + 4, layout.top() + 4, layout.left() + layout.panelWidth() - 4,
                layout.top() + Math.min(layout.panelHeight() - 4, 38), INK_STRIP);
        graphics.fill(layout.left() + 10, layout.top() + 30, layout.left() + layout.panelWidth() - 10,
                layout.top() + 31, GOLD_DIM);
        int mark = Math.min(26, Math.max(8, layout.panelWidth() / 12));
        graphics.fill(layout.left() + 8, layout.top() + 8, layout.left() + 8 + mark, layout.top() + 9, GOLD);
        graphics.fill(layout.left() + layout.panelWidth() - 8 - mark, layout.top() + 8,
                layout.left() + layout.panelWidth() - 8, layout.top() + 9, GOLD);
        graphics.fill(layout.left() + 8, layout.top() + layout.panelHeight() - 9,
                layout.left() + 8 + mark, layout.top() + layout.panelHeight() - 8, GOLD_DIM);
        graphics.fill(layout.left() + layout.panelWidth() - 8 - mark, layout.top() + layout.panelHeight() - 9,
                layout.left() + layout.panelWidth() - 8, layout.top() + layout.panelHeight() - 8, GOLD_DIM);
        if (layout.twoColumns()) {
            int dividerX = layout.rightColumnX() - COLUMN_GAP / 2;
            graphics.fill(dividerX, layout.contentTop() - 2, dividerX + 1, layout.contentBottom(), 0x553B2F18);
        }
    }

    private void drawHeader(GuiGraphics graphics, PanelLayout layout, ClientCultivationData.Snapshot data) {
        if (layout.panelHeight() < 118) {
            drawFit(graphics, "修仙道途", layout.left() + CONTENT_PADDING, layout.top() + 8,
                    Math.max(20, layout.panelWidth() - CONTENT_PADDING * 2), GOLD);
            return;
        }
        graphics.drawCenteredString(font, Component.translatable("screen.seeking_immortals.cultivation_stats.path_title"),
                layout.left() + layout.panelWidth() / 2, layout.top() + 10, GOLD);
        String subtitle = ClientCultivationData.isSynced()
                ? data.realm() + data.stage() + " · " + statusText(data)
                : "等待服务器同步道途";
        drawFit(graphics, subtitle, layout.left() + CONTENT_PADDING, layout.top() + 24,
                Math.max(20, layout.panelWidth() - CONTENT_PADDING * 2), ClientCultivationData.isSynced() ? JADE : TEXT_MUTED);
    }

    private void drawWaitingForSync(GuiGraphics graphics, PanelLayout layout) {
        int x = layout.leftColumnX();
        int y = layout.contentTop();
        drawFit(graphics, "等待服务器同步修仙数据...", x, y, layout.leftColumnWidth(), TEXT_MUTED);
        if (y + LINE_HEIGHT * 2 < layout.contentBottom()) {
            drawFit(graphics, "请稍候，或重新进入世界触发同步。", x, y + LINE_HEIGHT, layout.leftColumnWidth(), TEXT_MUTED);
        }
    }

    private int sectionTitle(GuiGraphics graphics, int x, int y, int width, int bottom, String title) {
        if (y + LINE_HEIGHT > bottom) return y;
        graphics.fill(x, y + 8, x + width, y + 9, 0x553B2F18);
        graphics.fill(x, y + 8, x + Math.min(width, 54), y + 9, GOLD_DIM);
        drawFit(graphics, "「" + title + "」", x, y, width, GOLD);
        return y + 13;
    }

    private int row(GuiGraphics graphics, int x, int y, int width, int bottom, String label, String value, int color) {
        if (y + LINE_HEIGHT > bottom) return y;
        int labelWidth = Math.min(54, Math.max(32, width / 3));
        drawFit(graphics, label, x, y, labelWidth, TEXT_MUTED);
        drawFit(graphics, value, x + labelWidth + 4, y, Math.max(8, width - labelWidth - 4), color);
        return y + LINE_HEIGHT;
    }

    private int progressBar(GuiGraphics graphics, int x, int y, int width, int bottom, String label, double fraction, String value, int fillColor) {
        int barHeight = 12;
        if (y + barHeight > bottom) return y;
        drawStatusBar(graphics, x, y, width, barHeight, fraction, fillColor);
        drawFit(graphics, label + " " + value, x + 4, y + 2, Math.max(8, width - 8), TEXT);
        return y + barHeight + 3;
    }

    private void drawStatusBar(GuiGraphics graphics, int x, int y, int width, int height, double fraction, int fillColor) {
        if (width <= 0 || height <= 0) return;
        graphics.fill(x, y, x + width, y + height, BAR_BORDER);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, BAR_BACKING);
        int fillWidth = Math.max(0, Math.min(width - 4, (int)Math.round((width - 4) * clamp01(fraction))));
        if (fillWidth > 0) {
            graphics.fill(x + 2, y + 2, x + 2 + fillWidth, y + height - 2, fillColor);
            graphics.fill(x + 2, y + 2, x + 2 + fillWidth, y + Math.max(y + 3, y + height / 2), BAR_HIGHLIGHT);
        }
    }

    private void drawFit(GuiGraphics graphics, String value, int x, int y, int maxWidth, int color) {
        ImmortalUiSkin.drawStringFit(font, graphics, value, x, y, maxWidth, color, false);
    }

    private double progressFraction(long value, long max) {
        return max <= 0 ? 0.0D : (double)value / (double)max;
    }

    private double fraction(int value, int max) {
        return max <= 0 ? 0.0D : (double)value / (double)max;
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
        return danger ? BLOOD_RED : TEXT;
    }

    private int statusColor(ClientCultivationData.Snapshot data) {
        return hasAffliction(data) ? BLOOD_RED : JADE;
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
        if (data.realmFallScars() > 0) builder.append("跌境伤痕").append(data.realmFallScars()).append(" ");
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

    record UiRect(int x, int y, int width, int height) {
        int right() {
            return x + width;
        }

        int bottom() {
            return y + height;
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
            boolean twoColumns,
            int contentTop,
            int contentBottom,
            int leftColumnX,
            int leftColumnWidth,
            int rightColumnX,
            int rightColumnWidth,
            UiRect breakthroughButton,
            UiRect methodTreeButton,
            UiRect closeButton,
            UiRect slider) {
    }

    private static final class InkButton extends Button {
        InkButton(UiRect rect, Component message, OnPress onPress) {
            super(rect.x(), rect.y(), rect.width(), rect.height(), message, onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int fill = active ? (isHoveredOrFocused() ? CONTROL_BACKING_HOVERED : CONTROL_BACKING) : CONTROL_DISABLED;
            int border = active ? BAR_BORDER : 0x66755F32;
            int textColor = active ? (isHoveredOrFocused() ? JADE : TEXT) : TEXT_MUTED;
            drawControlBox(graphics, getX(), getY(), getWidth(), getHeight(), fill, border);
            FontHolder.drawCentered(graphics, getMessage(), getX(), getY(), getWidth(), getHeight(), textColor);
        }
    }

    private static final class MovementSpeedSlider extends AbstractSliderButton {
        private boolean syncing;

        MovementSpeedSlider(int x, int y, int width, int height, double scale) {
            super(x, y, width, height, Component.empty(), step(scale));
            updateMessage();
        }

        void syncFromSnapshot(double scale) {
            double stepped = step(scale);
            if (Math.abs(value - stepped) < 0.0001D) return;
            syncing = true;
            value = stepped;
            updateMessage();
            syncing = false;
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal("移速 " + Math.round(value * 100.0D) + "%"));
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            int width = getWidth();
            int height = getHeight();
            int fill = active ? (isHoveredOrFocused() ? CONTROL_BACKING_HOVERED : CONTROL_BACKING) : CONTROL_DISABLED;
            drawControlBox(graphics, x, y, width, height, fill, BAR_BORDER);

            int trackX = x + 6;
            int trackWidth = Math.max(1, width - 12);
            int trackY = height >= 18 ? y + height - 7 : y + height - 5;
            int trackHeight = 3;
            graphics.fill(trackX, trackY, trackX + trackWidth, trackY + trackHeight, CONTROL_TRACK);
            int progressWidth = Math.max(0, Math.min(trackWidth, (int)Math.round(trackWidth * clamp01(value))));
            if (progressWidth > 0) {
                graphics.fill(trackX, trackY, trackX + progressWidth, trackY + trackHeight, CONTROL_PROGRESS);
            }
            int thumbCenter = trackX + progressWidth;
            graphics.fill(thumbCenter - 2, trackY - 2, thumbCenter + 3, trackY + trackHeight + 2, CONTROL_THUMB);
            graphics.fill(thumbCenter - 1, trackY - 1, thumbCenter + 2, trackY + trackHeight + 1, CONTROL_BACKING);

            int textY = height >= 18 ? y + 2 : y + Math.max(1, (height - 8) / 2);
            FontHolder.drawFit(graphics, getMessage(), x + 5, textY, Math.max(8, width - 10), active ? TEXT : TEXT_MUTED);
        }

        @Override
        protected void applyValue() {
            value = step(value);
            if (!syncing) {
                ModNetwork.CHANNEL.sendToServer(new SetMovementSpeedScalePacket(value));
            }
        }

        private static double step(double value) {
            return clamp01(Math.round(value / MOVEMENT_SLIDER_STEP) * MOVEMENT_SLIDER_STEP);
        }
    }

    private static void drawControlBox(GuiGraphics graphics, int x, int y, int width, int height, int fill, int border) {
        if (width <= 0 || height <= 0) return;
        graphics.fill(x, y, x + width, y + height, border);
        if (width > 2 && height > 2) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
            graphics.fill(x + 2, y + 2, x + width - 2, y + 3, GOLD_DIM);
            graphics.fill(x + 2, y + height - 3, x + width - 2, y + height - 2, 0x553B2F18);
        }
    }

    private static final class FontHolder {
        private FontHolder() {}

        static void drawCentered(GuiGraphics graphics, Component message, int x, int y, int width, int height, int color) {
            net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
            String fitted = ImmortalUiSkin.fitWidth(font, message.getString(), Math.max(0, width - 8));
            graphics.drawCenteredString(font, fitted, x + width / 2, y + Math.max(1, (height - 8) / 2), color);
        }

        static void drawFit(GuiGraphics graphics, Component message, int x, int y, int width, int color) {
            net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
            ImmortalUiSkin.drawStringFit(font, graphics, message.getString(), x, y, width, color, false);
        }
    }
}
