package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.network.AttemptBreakthroughPacket;
import com.xunxian.seekingimmortals.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

public class CultivationStatsScreen extends Screen {
    private static final int DEFAULT_PANEL_WIDTH = 390;
    private static final int DEFAULT_PANEL_HEIGHT = 276;
    private static final int MIN_PANEL_WIDTH = 180;
    private static final int MIN_PANEL_HEIGHT = 154;
    private static final int LINE_HEIGHT = 11;
    private static final int SECTION_GAP = 5;

    private final LocalPlayer player;
    private final boolean returnToInventory;

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
        int left = panelLeft();
        int top = panelTop();
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int buttonWidth = Math.min(66, Math.max(46, panelWidth - 24));
        int buttonHeight = panelHeight < 178 ? 16 : 20;
        int buttonY = top + panelHeight - buttonHeight - 6;
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.cultivation_stats.breakthrough"), button ->
                        ModNetwork.CHANNEL.sendToServer(new AttemptBreakthroughPacket()))
                .bounds(left + 12, buttonY, buttonWidth, buttonHeight)
                .build());
        addRenderableWidget(Button.builder(Component.translatable(returnToInventory
                        ? "screen.seeking_immortals.cultivation_stats.back_to_inventory"
                        : "screen.seeking_immortals.cultivation_stats.close"), button -> onClose())
                .bounds(left + panelWidth - buttonWidth - 12, buttonY, buttonWidth, buttonHeight)
                .build());
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

    private void renderCultivationPanel(GuiGraphics graphics) {
        int left = panelLeft();
        int top = panelTop();
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        ImmortalUiSkin.drawPanel(graphics, left, top, panelWidth, panelHeight);
        if (panelHeight >= 184) {
            graphics.drawCenteredString(font, Component.literal("修仙属性"), left + panelWidth / 2, top + 9, 0xFFE6D59A);
        }

        ClientCultivationData.Snapshot data = ClientCultivationData.getSnapshot();
        int x = left + 12;
        int y = top + (panelHeight >= 184 ? 25 : 10);
        if (!ClientCultivationData.isSynced()) {
            text(graphics, x, y, "等待服务端同步修仙数据...", 0xFFBFAF8A);
            if (y + LINE_HEIGHT * 2 < top + panelHeight - 8) {
                text(graphics, x, y + LINE_HEIGHT, "请稍候，或重新进入世界触发同步。", 0xFFBFAF8A);
            }
            return;
        }

        int bottom = top + panelHeight - (panelHeight < 178 ? 26 : 32);
        y = renderBasicStatus(graphics, x, y, data, bottom);
        y = renderCombatStats(graphics, x, y + SECTION_GAP, data, bottom);
        y = renderSpiritualRoot(graphics, x, y + SECTION_GAP, data, bottom);
        y = renderTechniques(graphics, x, y + SECTION_GAP, data, bottom);
        renderAfflictions(graphics, x, y + SECTION_GAP, data, bottom);
    }

    private int renderBasicStatus(GuiGraphics graphics, int x, int y, ClientCultivationData.Snapshot data, int bottom) {
        y = sectionTitle(graphics, x, y, "基础状态");
        if (y + LINE_HEIGHT > bottom) return y;
        line(graphics, x, y, "境界", data.realm() + data.stage()); y += LINE_HEIGHT;
        line(graphics, x, y, "修为", Integer.toString(data.cultivation())); y += LINE_HEIGHT;
        line(graphics, x, y, "灵力", data.mana() + " / " + data.manaMax()); y += LINE_HEIGHT;
        line(graphics, x, y, "神识/肉身", data.divSense() + " 格 / " + data.bodyRef()); y += LINE_HEIGHT;
        line(graphics, x, y, "走火/天劫", data.qiDevRisk() + "% / " + data.tribRes() + "%"); y += LINE_HEIGHT;
        line(graphics, x, y, "突破概率", percent(data.breakthroughChance()) + "，执念 " + percent(data.breakthroughObsessionBonus())); y += LINE_HEIGHT;
        line(graphics, x, y, "突破加成", "丹药 " + percent(data.breakthroughPillBonus()) + " / 灵眼 " + percent(data.breakthroughSpiritEyeBonus()) + " / 功法 " + percent(data.breakthroughTechniqueQualityBonus())); y += LINE_HEIGHT;
        line(graphics, x, y, "寿元", data.remainingLifespanYears() + " / " + data.lifespanYears() + "，年龄 " + data.ageYears()); y += LINE_HEIGHT;
        line(graphics, x, y, "特殊体质", data.specialPhysique()); y += LINE_HEIGHT;
        line(graphics, x, y, "打坐吐纳", data.meditating() ? "吐纳中" : "未吐纳"); y += LINE_HEIGHT + 3;
        if (y + 10 <= bottom) {
            int barWidth = contentWidth();
            ImmortalUiSkin.drawStatusBar(graphics, x, y, barWidth, 10, spiritualPowerFraction(data));
            text(graphics, x + 4, y + 1, "灵力进度 " + percent(spiritualPowerFraction(data)), 0xFFEFE4C2);
            return y + 12;
        }
        return y;
    }

    private int renderCombatStats(GuiGraphics graphics, int x, int y, ClientCultivationData.Snapshot data, int bottom) {
        if (y + LINE_HEIGHT > bottom) return y;
        y = sectionTitle(graphics, x, y, "战斗属性");
        line(graphics, x, y, "攻击", formatDouble(data.baseAttack())); y += LINE_HEIGHT;
        line(graphics, x, y, "防御", formatDouble(data.baseDefense())); y += LINE_HEIGHT;
        line(graphics, x, y, "暴击率/暴击伤害", percent(data.critChance()) + " / " + formatDouble(data.critDamage()) + "x"); y += LINE_HEIGHT;
        line(graphics, x, y, "闪避率/命中率", percent(data.dodgeChance()) + " / " + percent(data.accuracy())); y += LINE_HEIGHT;
        if (y + LINE_HEIGHT <= bottom) {
            double defenseReduction = data.baseDefense() / (data.baseDefense() + 100.0D);
            line(graphics, x, y, "防御减伤", percent(defenseReduction)); y += LINE_HEIGHT;
        }
        return y;
    }
    private int renderSpiritualRoot(GuiGraphics graphics, int x, int y, ClientCultivationData.Snapshot data, int bottom) {
        if (y + LINE_HEIGHT > bottom) return y;
        y = sectionTitle(graphics, x, y, "灵根信息");
        line(graphics, x, y, "是否测灵", data.spiritualRootTested() ? "已测灵" : "未测灵"); y += LINE_HEIGHT;
        line(graphics, x, y, "是否觉醒", data.spiritualRootAwakened() ? "已觉醒" : "未觉醒"); y += LINE_HEIGHT;
        line(graphics, x, y, "灵根/属性", data.spiritualRoot() + " · " + data.spiritualRootAttributes()); y += LINE_HEIGHT;
        line(graphics, x, y, "纯度/亲和", data.spiritualRootPurity() + "% · " + formatDouble(data.rootCultivationSpeedCoefficient()) + "x"); y += LINE_HEIGHT + 3;
        if (y + 10 <= bottom) {
            ImmortalUiSkin.drawStatusBar(graphics, x, y, contentWidth(), 10, Math.min(1.0D, data.spiritualRootPurity() / 100.0D));
            text(graphics, x + 4, y + 1, "纯度条", 0xFFEFE4C2);
            return y + 12;
        }
        return y;
    }

    private int renderTechniques(GuiGraphics graphics, int x, int y, ClientCultivationData.Snapshot data, int bottom) {
        if (y + LINE_HEIGHT > bottom) return y;
        y = sectionTitle(graphics, x, y, "功法信息");
        List<String> techniques = ClientTechniqueData.getLearnedTechniques();
        line(graphics, x, y, "已学数量", Integer.toString(data.learnedTechniqueCount())); y += LINE_HEIGHT;
        line(graphics, x, y, "功法倍率/总吐纳效率", formatDouble(data.physiqueCultivationSpeedMultiplier()) + "x / " + formatDouble(data.cultivationSpeedMultiplier()) + "x"); y += LINE_HEIGHT;
        if (techniques.isEmpty()) {
            text(graphics, x, y, "暂无已同步功法/术法", 0xFFBFAF8A);
            return y + LINE_HEIGHT;
        }
        int maxRows = Math.max(0, Math.min(3, (bottom - y) / 20));
        for (int i = 0; i < Math.min(maxRows, techniques.size()); i++) {
            ClientTechniqueData.TechniqueSummary summary = ClientTechniqueData.getTechniqueSummary(techniques.get(i));
            text(graphics, x, y, (i + 1) + ". " + summary.name(), 0xFFEFE4C2);
            text(graphics, x + 12, y + 10, summary.source() + " · " + summary.attribute(), 0xFFB8F5A2);
            y += 20;
        }
        if (techniques.size() > maxRows && y + LINE_HEIGHT <= bottom) {
            text(graphics, x, y, "+" + (techniques.size() - maxRows) + " 个可在技能编辑界面查看", 0xFFE6D59A);
            y += LINE_HEIGHT;
        }
        return y;
    }

    private int renderAfflictions(GuiGraphics graphics, int x, int y, ClientCultivationData.Snapshot data, int bottom) {
        if (y + LINE_HEIGHT > bottom) return y;
        y = sectionTitle(graphics, x, y, "负面状态");
        line(graphics, x, y, "总状态", statusText(data)); y += LINE_HEIGHT;
        line(graphics, x, y, "重伤", data.severeInjury() ? "存在：生命/恢复受损" : "无"); y += LINE_HEIGHT;
        line(graphics, x, y, "心魔/碎丹", (data.heartDemonLevel() > 0 ? data.heartDemonLevel() + " 层" : "无") + " / " + (data.shatteredCore() ? "存在" : "无")); y += LINE_HEIGHT;
        if (y + LINE_HEIGHT <= bottom) {
            line(graphics, x, y, "跌境伤痕", data.realmFallScars() > 0 ? data.realmFallScars() + " 道" : "无"); y += LINE_HEIGHT;
        }
        if (y + LINE_HEIGHT <= bottom) {
            text(graphics, x, y, "提示：修仙根状态不会被牛奶或死亡清除。", 0xFFBFAF8A);
        }
        return y;
    }

    private int sectionTitle(GuiGraphics graphics, int x, int y, String title) {
        text(graphics, x, y, "【" + title + "】", 0xFFE6D59A);
        return y + LINE_HEIGHT;
    }

    private int panelLeft() {
        return Math.max(2, (width - panelWidth()) / 2);
    }

    private int panelTop() {
        return Math.max(2, (height - panelHeight()) / 2);
    }

    private int panelWidth() {
        int available = Math.max(80, width - 8);
        return Math.max(Math.min(MIN_PANEL_WIDTH, available), Math.min(DEFAULT_PANEL_WIDTH, available));
    }

    private int panelHeight() {
        int available = Math.max(80, height - 8);
        return Math.max(Math.min(MIN_PANEL_HEIGHT, available), Math.min(DEFAULT_PANEL_HEIGHT, available));
    }

    private int contentWidth() {
        return Math.max(32, panelWidth() - 24);
    }

    private void line(GuiGraphics graphics, int x, int y, String label, String value) {
        text(graphics, x, y, label + "：" + value, 0xFFEFE4C2);
    }

    private void text(GuiGraphics graphics, int x, int y, String value, int color) {
        graphics.drawString(font, fit(value, Math.max(8, panelLeft() + panelWidth() - x - 10)), x, y, color, false);
    }

    private String fit(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("..."))) + "...";
    }

    private double spiritualPowerFraction(ClientCultivationData.Snapshot data) {
        return data.manaMax() <= 0 ? 0.0D : (double) data.mana() / (double) data.manaMax();
    }

    private String percent(double fraction) {
        return String.format(Locale.ROOT, "%.0f%%", Math.max(0.0D, Math.min(1.0D, fraction)) * 100.0D);
    }

    private String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String statusText(ClientCultivationData.Snapshot data) {
        StringBuilder builder = new StringBuilder();
        if (data.meditating()) builder.append("打坐 ");
        if (data.severeInjury()) builder.append("重伤 ");
        if (data.heartDemonLevel() > 0) builder.append("心魔").append(data.heartDemonLevel()).append("层 ");
        if (data.shatteredCore()) builder.append("碎丹 ");
        if (data.realmFallScars() > 0) builder.append("跌境伤痕").append(data.realmFallScars()).append(" ");
        return builder.isEmpty() ? "正常" : builder.toString().trim();
    }
}
