package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.network.ModNetwork;
import com.xunxian.seekingimmortals.network.WorldpackActionPacket;
import com.xunxian.seekingimmortals.worldpack.WorldpackGameplayService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.stream.Collectors;

public class WorldpackScreen extends Screen {
    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 272;
    private static final int PANEL_MARGIN = 4;
    private static final int LINE = 13;
    private static final int ROW_HEIGHT = 28;

    private Tab tab = Tab.REGIONS;
    private int listScroll;
    private long observedRevision = Long.MIN_VALUE;
    private int observedActionState = Integer.MIN_VALUE;

    public WorldpackScreen() {
        super(Component.translatable("screen.seeking_immortals.worldpack.title"));
    }

    @Override
    protected void init() {
        super.init();
        rebuildActionWidgets();
    }

    public void refreshFromSync() {
        rebuildActionWidgets();
    }

    @Override
    public void tick() {
        super.tick();
        ClientWorldpackData.Snapshot data = ClientWorldpackData.get();
        int actionState = actionState(data);
        if (data.revision() != observedRevision || actionState != observedActionState) {
            rebuildActionWidgets();
        }
    }

    private void rebuildActionWidgets() {
        clearWidgets();
        Layout layout = calculateLayout(width, height);
        ClientWorldpackData.Snapshot data = ClientWorldpackData.get();
        observedRevision = data.revision();
        observedActionState = actionState(data);

        addRenderableWidget(ImmortalButton.secondary(layout.refreshButton().x(), layout.refreshButton().y(),
                layout.refreshButton().width(), layout.refreshButton().height(),
                Component.translatable("screen.seeking_immortals.worldpack.refresh"), button ->
                        ModNetwork.CHANNEL.sendToServer(new WorldpackActionPacket(
                                WorldpackGameplayService.ACTION_SYNC, ""))));
        addRenderableWidget(ImmortalButton.secondary(layout.closeButton().x(), layout.closeButton().y(),
                layout.closeButton().width(), layout.closeButton().height(),
                Component.translatable("screen.seeking_immortals.worldpack.close"), button -> onClose()));
        if (!data.activeSecretRealmId().isBlank()) {
            addRenderableWidget(ImmortalButton.primary(layout.returnButton().x(), layout.returnButton().y(),
                    layout.returnButton().width(), layout.returnButton().height(),
                    Component.translatable("screen.seeking_immortals.worldpack.return"), button ->
                            ModNetwork.CHANNEL.sendToServer(new WorldpackActionPacket(
                                    WorldpackGameplayService.ACTION_RETURN, ""))));
        }

        addTabButton(layout.regionTab(), Tab.REGIONS);
        addTabButton(layout.realmTab(), Tab.REALMS);
        if (!data.synced()) {
            listScroll = 0;
            return;
        }

        int total = tab == Tab.REGIONS ? data.regions().size() : data.realms().size();
        int visible = visibleRows(layout);
        listScroll = Mth.clamp(listScroll, 0, Math.max(0, total - visible));
        for (int row = 0; row < visible && listScroll + row < total; row++) {
            Rect action = rowAction(layout, row);
            if (tab == Tab.REGIONS) {
                ClientWorldpackData.Region region = data.regions().get(listScroll + row);
                ImmortalButton button = ImmortalButton.primary(action.x(), action.y(), action.width(), action.height(),
                        Component.translatable("screen.seeking_immortals.worldpack.travel"), ignored ->
                        ModNetwork.CHANNEL.sendToServer(new WorldpackActionPacket(
                                WorldpackGameplayService.ACTION_TRAVEL, region.id())));
                button.active = region.anchorReady() && !region.current()
                        && data.activeSecretRealmId().isBlank();
                addRenderableWidget(button);
            } else {
                ClientWorldpackData.SecretRealm realm = data.realms().get(listScroll + row);
                ImmortalButton button = ImmortalButton.primary(action.x(), action.y(), action.width(), action.height(),
                        Component.translatable("screen.seeking_immortals.worldpack.enter"), ignored ->
                        ModNetwork.CHANNEL.sendToServer(new WorldpackActionPacket(
                                WorldpackGameplayService.ACTION_ENTER, realm.id())));
                button.active = realm.anchorReady() && realm.currentRegion() && !realm.active()
                        && data.currentRealmCooldownTicks(realm) <= 0 && data.activeSecretRealmId().isBlank();
                addRenderableWidget(button);
            }
        }
    }

    private void addTabButton(Rect bounds, Tab target) {
        ImmortalButton button = target == tab
                ? ImmortalButton.primary(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                Component.translatable(target.key), ignored -> setTab(target))
                : ImmortalButton.secondary(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                Component.translatable(target.key), ignored -> setTab(target));
        addRenderableWidget(button);
    }

    private void setTab(Tab next) {
        if (tab != next) {
            tab = next;
            listScroll = 0;
            rebuildActionWidgets();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        renderPanel(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        Layout layout = calculateLayout(width, height);
        ClientWorldpackData.Snapshot data = ClientWorldpackData.get();
        ImmortalUiSkin.drawLayeredPanel(graphics, layout.left(), layout.top(),
                layout.panelWidth(), layout.panelHeight());
        ImmortalUiSkin.drawTitleBar(graphics, layout.header().x(), layout.header().y(),
                layout.header().width(), layout.header().height());
        ImmortalUiSkin.drawStringFit(font, graphics, title.getString(), layout.titleArea().x(),
                layout.titleArea().y() + Math.max(2, (layout.titleArea().height() - 8) / 2),
                layout.titleArea().width(), ImmortalUiSkin.JOURNAL_BORDER, false);
        renderStatus(graphics, layout.status(), data);

        ImmortalUiSkin.drawInnerFrame(graphics, layout.content().x(), layout.content().y(),
                layout.content().width(), layout.content().height());
        Rect viewport = listViewport(layout);
        if (!data.synced()) {
            drawNotice(graphics, viewport,
                    Component.translatable("screen.seeking_immortals.worldpack.waiting"));
            return;
        }
        int total = tab == Tab.REGIONS ? data.regions().size() : data.realms().size();
        int visible = visibleRows(layout);
        listScroll = Mth.clamp(listScroll, 0, Math.max(0, total - visible));
        int hovered = hoveredRow(layout, mouseX, mouseY);
        ImmortalUiSkin.withScissor(graphics, viewport.x(), viewport.y(), viewport.width(), viewport.height(), () -> {
            for (int row = 0; row < visible && listScroll + row < total; row++) {
                Rect item = rowRect(layout, row);
                ImmortalUiSkin.drawListRow(graphics, item.x(), item.y(), item.width(), item.height(),
                        hovered == row ? ImmortalUiSkin.InteractionState.HOVERED
                                : ImmortalUiSkin.InteractionState.NORMAL);
                if (tab == Tab.REGIONS) {
                    renderRegion(graphics, layout, item, data, data.regions().get(listScroll + row));
                } else {
                    renderRealm(graphics, layout, item, data.realms().get(listScroll + row));
                }
            }
        });
        ImmortalUiSkin.drawThinScrollbar(graphics, layout.content().right() - 3,
                viewport.y(), viewport.height(), total * layout.rowHeight(), viewport.height(),
                listScroll * layout.rowHeight());
    }

    private void renderStatus(GuiGraphics graphics, Rect status, ClientWorldpackData.Snapshot data) {
        if (status.height() < 9) return;
        int y = status.y() + 1;
        int bottom = status.bottom();
        y = statusLine(graphics, status, y, bottom,
                Component.translatable("screen.seeking_immortals.worldpack.current_region",
                        data.currentRegionDisplay().isBlank() ? "-" : data.currentRegionDisplay()),
                ImmortalUiSkin.JOURNAL_PAPER);
        if (!data.activeSecretRealmId().isBlank()) {
            y = statusLine(graphics, status, y, bottom,
                    Component.translatable("screen.seeking_immortals.worldpack.active_realm",
                            data.activeSecretRealmDisplay()), ImmortalUiSkin.JOURNAL_SPIRIT);
            y = statusLine(graphics, status, y, bottom,
                    Component.translatable("screen.seeking_immortals.worldpack.in_realm",
                            data.activeSecretRealmDisplay()), ImmortalUiSkin.JOURNAL_PAPER_MUTED);
        }
        if (!data.dailyEventId().isBlank()) {
            y = statusLine(graphics, status, y, bottom,
                    Component.translatable("screen.seeking_immortals.worldpack.daily_event",
                            data.dailyEventDisplay(), Math.max(0L, data.currentDailyEventRemainingTicks() / 20L)),
                    ImmortalUiSkin.JOURNAL_WARNING);
            statusLine(graphics, status, y, bottom,
                    Component.translatable("screen.seeking_immortals.worldpack.effects",
                            formatEffectDescriptions(data.dailyEventEffects())),
                    ImmortalUiSkin.JOURNAL_PAPER_MUTED);
        }
    }

    private int statusLine(GuiGraphics graphics, Rect status, int y, int bottom,
                           Component text, int color) {
        if (y + 8 > bottom) return y;
        ImmortalUiSkin.drawStringFit(font, graphics, text.getString(), status.x(), y,
                status.width(), color, false);
        return y + LINE;
    }

    private void renderRegion(GuiGraphics graphics, Layout layout, Rect row,
                              ClientWorldpackData.Snapshot data, ClientWorldpackData.Region region) {
        Rect action = rowAction(layout, (row.y() - listViewport(layout).y()) / layout.rowHeight());
        int textWidth = Math.max(1, action.x() - row.x() - 8);
        String text = region.display() + " / " + region.minRealm() + " / x"
                + String.format("%.2f", region.auraMultiplier());
        ImmortalUiSkin.drawStringFit(font, graphics, text, row.x() + 4, row.y() + 3, textWidth,
                region.current() ? ImmortalUiSkin.JOURNAL_JADE_TEXT : ImmortalUiSkin.JOURNAL_PAPER, false);
        if (layout.rowHeight() >= 24) {
            String meta = Component.translatable("screen.seeking_immortals.worldpack.entry_id", region.id()).getString()
                    + " / " + bool(region.anchorReady());
            String route = routeRequirementText(WorldpackGameplayService.routeRequirementForDisplay(
                    data.currentRegionId(), region.id()));
            if (!route.isBlank()) meta += " / " + route;
            ImmortalUiSkin.drawStringFit(font, graphics, meta, row.x() + 4, row.y() + LINE,
                    textWidth, ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
        }
    }

    private void renderRealm(GuiGraphics graphics, Layout layout, Rect row,
                             ClientWorldpackData.SecretRealm realm) {
        Rect action = rowAction(layout, (row.y() - listViewport(layout).y()) / layout.rowHeight());
        int textWidth = Math.max(1, action.x() - row.x() - 8);
        long remainingCooldownTicks = ClientWorldpackData.get().currentRealmCooldownTicks(realm);
        String cooldown = remainingCooldownTicks <= 0
                ? Component.translatable("screen.seeking_immortals.worldpack.ready").getString()
                : Component.translatable("screen.seeking_immortals.worldpack.cooldown_seconds",
                Math.max(1L, (remainingCooldownTicks + 19L) / 20L)).getString();
        String text = realm.display() + " / " + realm.minRealm() + " / " + cooldown;
        ImmortalUiSkin.drawStringFit(font, graphics, text, row.x() + 4, row.y() + 3, textWidth,
                realm.active() ? ImmortalUiSkin.JOURNAL_SPIRIT : ImmortalUiSkin.JOURNAL_PAPER, false);
        if (layout.rowHeight() >= 24) {
            String meta = Component.translatable("screen.seeking_immortals.worldpack.entry_id", realm.id()).getString()
                    + " / " + Component.translatable(realm.ticketDescriptionId()).getString();
            ImmortalUiSkin.drawStringFit(font, graphics, meta, row.x() + 4, row.y() + LINE,
                    textWidth, ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
        }
    }

    private void drawNotice(GuiGraphics graphics, Rect viewport, Component text) {
        ImmortalUiSkin.drawStringFit(font, graphics, text.getString(), viewport.x() + 2, viewport.y() + 2,
                Math.max(1, viewport.width() - 4), ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        Layout layout = calculateLayout(width, height);
        if (listViewport(layout).contains(mouseX, mouseY) && delta != 0.0D) {
            ClientWorldpackData.Snapshot data = ClientWorldpackData.get();
            int total = tab == Tab.REGIONS ? data.regions().size() : data.realms().size();
            int next = Mth.clamp(listScroll - (int)Math.signum(delta),
                    0, Math.max(0, total - visibleRows(layout)));
            if (next != listScroll) {
                listScroll = next;
                rebuildActionWidgets();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private String bool(boolean value) {
        return Component.translatable(value
                ? "message.seeking_immortals.sect.yes"
                : "message.seeking_immortals.sect.no").getString();
    }

    private static String formatEffectDescriptions(List<String> effects) {
        if (effects == null || effects.isEmpty()) return "-";
        return effects.stream().map(WorldpackScreen::effectDescription)
                .filter(description -> !description.isBlank()).collect(Collectors.joining(", "));
    }

    private static int actionState(ClientWorldpackData.Snapshot data) {
        int hash = Boolean.hashCode(!data.activeSecretRealmId().isBlank());
        for (ClientWorldpackData.SecretRealm realm : data.realms()) {
            hash = 31 * hash + Boolean.hashCode(data.currentRealmCooldownTicks(realm) <= 0L);
        }
        return hash;
    }

    private static String effectDescription(String effect) {
        String key = effectDescriptionKey(effect);
        return key.isBlank() ? (effect == null ? "" : effect) : Component.translatable(key).getString();
    }

    static String effectDescriptionKey(String effect) {
        if (effect == null || effect.isBlank()) return "";
        return switch (effect) {
            case WorldpackGameplayService.EFFECT_AURA_PLUS_5 ->
                    "screen.seeking_immortals.worldpack.effect.aura_plus_5";
            case WorldpackGameplayService.EFFECT_SPIRIT_RAIN_BONUS ->
                    "screen.seeking_immortals.worldpack.effect.spirit_rain_bonus";
            case WorldpackGameplayService.EFFECT_HERB_SHOP_BONUS ->
                    "screen.seeking_immortals.worldpack.effect.herb_shop_bonus";
            case WorldpackGameplayService.EFFECT_TRADE_RISK_UP ->
                    "screen.seeking_immortals.worldpack.effect.trade_risk_up";
            case WorldpackGameplayService.EFFECT_SECRET_REALM_TICKET_HINT ->
                    "screen.seeking_immortals.worldpack.effect.secret_realm_ticket_hint";
            case WorldpackGameplayService.EFFECT_SECT_CONTRIBUTION_BONUS ->
                    "screen.seeking_immortals.worldpack.effect.sect_contribution_bonus";
            case "rare_loot_hint" -> "screen.seeking_immortals.worldpack.effect.rare_loot_hint";
            default -> "";
        };
    }

    private static String routeRequirementText(WorldpackGameplayService.RouteRequirement requirement) {
        if (requirement == null || !requirement.isPresent()) return "";
        String itemId = requirement.itemId();
        if (requirement.amount() > 0 && itemId != null && !itemId.isBlank()) {
            return Component.translatable(requirement.translationKey(), requirement.amount(),
                    itemNameComponent(itemId)).getString();
        }
        if (itemId != null && !itemId.isBlank()) {
            return Component.translatable(requirement.translationKey(), itemNameComponent(itemId)).getString();
        }
        return Component.translatable(requirement.translationKey()).getString();
    }

    private static Component itemNameComponent(String itemId) {
        ResourceLocation location = ResourceLocation.tryParse(itemId == null ? "" : itemId);
        if (location == null || ForgeRegistries.ITEMS == null) {
            return Component.literal(itemId == null ? "" : itemId);
        }
        Item item = ForgeRegistries.ITEMS.getValue(location);
        return item == null || item == Items.AIR
                ? Component.literal(itemId) : Component.translatable(item.getDescriptionId());
    }

    static Layout calculateLayout(int screenWidth, int screenHeight) {
        int panelWidth = calculatePanelWidth(screenWidth);
        int panelHeight = calculatePanelHeight(screenHeight);
        int left = Math.max(0, (screenWidth - panelWidth) / 2);
        int top = Math.max(0, (screenHeight - panelHeight) / 2);
        int padding = panelWidth >= 220 ? 10 : 5;
        int innerX = left + padding;
        int innerWidth = Math.max(1, panelWidth - padding * 2);
        int headerHeight = panelHeight >= 170 ? 36 : 20;
        Rect header = new Rect(innerX, top + 4, innerWidth,
                Math.min(headerHeight, Math.max(1, panelHeight - 8)));
        int buttonGap = innerWidth >= 80 ? 3 : 1;
        int buttonWidth = Math.max(1, Math.min(60, (innerWidth - 24 - buttonGap * 2) / 3));
        int buttonHeight = Math.max(12, Math.min(18, header.height() - 4));
        int buttonY = header.y() + Math.max(1, (header.height() - buttonHeight) / 2);
        Rect returnButton = new Rect(header.x() + 3, buttonY, buttonWidth, buttonHeight);
        Rect closeButton = new Rect(header.right() - buttonWidth - 3, buttonY, buttonWidth, buttonHeight);
        Rect refreshButton = new Rect(closeButton.x() - buttonGap - buttonWidth, buttonY,
                buttonWidth, buttonHeight);
        Rect titleArea = new Rect(returnButton.right() + buttonGap, header.y(),
                Math.max(1, refreshButton.x() - returnButton.right() - buttonGap * 2), header.height());
        int statusHeight = panelHeight >= 220 ? 54 : panelHeight >= 150 ? 32 : panelHeight >= 100 ? 18 : 0;
        Rect status = new Rect(innerX, header.bottom() + 2, innerWidth, statusHeight);
        int tabHeight = panelHeight >= 120 ? 18 : 14;
        int tabY = status.bottom() + (statusHeight > 0 ? 2 : 1);
        int tabGap = innerWidth >= 60 ? 4 : 1;
        int tabWidth = Math.max(1, (innerWidth - tabGap) / 2);
        Rect regionTab = new Rect(innerX, tabY, tabWidth, tabHeight);
        Rect realmTab = new Rect(regionTab.right() + tabGap, tabY,
                Math.max(1, innerX + innerWidth - regionTab.right() - tabGap), tabHeight);
        int contentY = regionTab.bottom() + 3;
        int contentBottom = top + panelHeight - 5;
        Rect content = new Rect(innerX, contentY, innerWidth, Math.max(1, contentBottom - contentY));
        int rowHeight = content.height() >= 58 ? ROW_HEIGHT : 20;
        return new Layout(left, top, panelWidth, panelHeight, header, titleArea, status,
                regionTab, realmTab, content, returnButton, refreshButton, closeButton, rowHeight);
    }

    private static Rect listViewport(Layout layout) {
        Rect content = layout.content();
        return new Rect(content.x() + 3, content.y() + 3,
                Math.max(1, content.width() - 8), Math.max(1, content.height() - 6));
    }

    private static int visibleRows(Layout layout) {
        return Math.max(1, listViewport(layout).height() / layout.rowHeight());
    }

    private static Rect rowRect(Layout layout, int row) {
        Rect viewport = listViewport(layout);
        return new Rect(viewport.x(), viewport.y() + row * layout.rowHeight(),
                viewport.width(), layout.rowHeight());
    }

    private static Rect rowAction(Layout layout, int row) {
        Rect item = rowRect(layout, row);
        int width = Math.max(22, Math.min(54, item.width() / 3));
        int height = Math.max(12, Math.min(18, item.height() - 4));
        return new Rect(item.right() - width - 3, item.y() + Math.max(1, (item.height() - height) / 2),
                width, height);
    }

    private static int hoveredRow(Layout layout, double mouseX, double mouseY) {
        Rect viewport = listViewport(layout);
        if (!viewport.contains(mouseX, mouseY)) return -1;
        int row = (int)((mouseY - viewport.y()) / layout.rowHeight());
        return row < visibleRows(layout) ? row : -1;
    }

    static int calculatePanelWidth(int screenWidth) {
        return Math.min(PANEL_WIDTH, Math.max(1, screenWidth - PANEL_MARGIN * 2));
    }

    static int calculatePanelHeight(int screenHeight) {
        return Math.min(PANEL_HEIGHT, Math.max(1, screenHeight - PANEL_MARGIN * 2));
    }

    static int listTopOffset(int panelHeight) {
        int preferred = Math.min(116, Math.max(88, panelHeight - 124));
        int maxOffset = Math.max(28, panelHeight - 54);
        int minOffset = Math.min(panelHeight < 150 ? 60 : 100, maxOffset);
        return Math.max(28, Math.max(minOffset, Math.min(preferred, maxOffset)));
    }

    static int visibleRows(int panelWidth, int panelHeight) {
        int available = panelHeight - listTopOffset(panelHeight) - (panelWidth < 300 ? 44 : 30);
        return Math.max(0, Math.min(5, available / 24));
    }

    record Rect(int x, int y, int width, int height) {
        int right() {
            return x + width;
        }

        int bottom() {
            return y + height;
        }

        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
    }

    record Layout(int left, int top, int panelWidth, int panelHeight, Rect header,
                  Rect titleArea, Rect status, Rect regionTab, Rect realmTab, Rect content,
                  Rect returnButton, Rect refreshButton, Rect closeButton, int rowHeight) {
    }

    private enum Tab {
        REGIONS("screen.seeking_immortals.worldpack.tab.regions"),
        REALMS("screen.seeking_immortals.worldpack.tab.realms");

        private final String key;

        Tab(String key) {
            this.key = key;
        }
    }
}
