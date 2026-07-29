package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.artifact.ArtifactDisplayTexts;
import com.xunxian.seekingimmortals.network.ModNetwork;
import com.xunxian.seekingimmortals.network.WorldpackActionPacket;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import com.xunxian.seekingimmortals.worldpack.WorldpackGameplayService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.stream.Collectors;

/** Region / secret-realm travel journal with dual tabs and per-row action buttons. */
public class WorldpackScreen extends AbstractJournalScreen {
    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 272;
    private static final int PANEL_MARGIN = 4;
    private static final int LINE = 13;
    private static final int ROW_HEIGHT = 28;

    private final ScrollableListPanel listPanel = new ScrollableListPanel();
    private final TabBar<Tab> tabBar = new TabBar<>(Tab.REGIONS);

    private Tab tab = Tab.REGIONS;
    private long observedRevision = Long.MIN_VALUE;
    private int observedActionState = Integer.MIN_VALUE;


    @Override
    protected UiClimate defaultClimate() {
        return UiClimate.BAMBOO_SLIP;
    }

    public WorldpackScreen() {
        super(Component.translatable("screen.seeking_immortals.worldpack.title"));
        this.listPanel.setScrollStep(ROW_HEIGHT)
                .setRowMetrics(ROW_HEIGHT, 0)
                .setScrollbarInsetRight(3);
        this.tabBar.setOnSelect(this::setTab);
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

        tabBar.clearTabs()
                .setSelected(tab)
                .addTab(Tab.REGIONS, Component.translatable(Tab.REGIONS.key), toUi(layout.regionTab()))
                .addTab(Tab.REALMS, Component.translatable(Tab.REALMS.key), toUi(layout.realmTab()));
        for (ImmortalButton button : tabBar.attach(null)) {
            addRenderableWidget(button);
        }

        if (!data.synced()) {
            listPanel.resetScroll();
            return;
        }

        int total = tab == Tab.REGIONS ? data.regions().size() : data.realms().size();
        int rowHeight = layout.rowHeight();
        Rect viewport = listViewport(layout);
        listPanel.setBounds(toUi(viewport))
                .setRowMetrics(rowHeight, 0)
                .setContentRows(total);
        listPanel.clampToViewport();
        int visible = listPanel.visibleRowCount();
        int listScroll = Mth.clamp(listPanel.scrollRows(), 0, Math.max(0, total - visible));
        listPanel.setScrollRows(listScroll);

        for (int row = 0; row < visible && listScroll + row < total; row++) {
            UiRect action = toUi(rowAction(layout, row));
            if (tab == Tab.REGIONS) {
                ClientWorldpackData.Region region = data.regions().get(listScroll + row);
                ImmortalButton button = ImmortalButton.primary(action.x(), action.y(), action.width(), action.height(),
                        Component.translatable("screen.seeking_immortals.worldpack.travel"), ignored ->
                                ModNetwork.CHANNEL.sendToServer(new WorldpackActionPacket(
                                        WorldpackGameplayService.ACTION_TRAVEL, region.id())));
                button.active = canTravelRegion(data, region);
                addRenderableWidget(button);
            } else {
                ClientWorldpackData.SecretRealm realm = data.realms().get(listScroll + row);
                ImmortalButton button = ImmortalButton.primary(action.x(), action.y(), action.width(), action.height(),
                        Component.translatable("screen.seeking_immortals.worldpack.gate_required"), ignored -> {});
                button.active = false;
                addRenderableWidget(button);
            }
        }
    }

    private void setTab(Tab next) {
        if (tab != next) {
            tab = next;
            listPanel.resetScroll();
            rebuildActionWidgets();
        }
    }

    @Override
    protected JournalChrome journalChrome() {
        Layout layout = calculateLayout(width, height);
        return new JournalChrome(layout.left(), layout.top(), layout.panelWidth(), layout.panelHeight(),
                toUi(layout.header()), toUi(layout.content()));
    }

    @Override
    protected void renderJournalTitle(GuiGraphics graphics, JournalChrome chrome, UiRect header) {
        Layout layout = calculateLayout(width, height);
        ImmortalUiSkin.drawStringFit(font, graphics, title.getString(), layout.titleArea().x(),
                layout.titleArea().y() + Math.max(2, (layout.titleArea().height() - 8) / 2),
                layout.titleArea().width(), ImmortalUiSkin.JOURNAL_PAPER, false);
    }

    @Override
    protected void renderJournalContent(GuiGraphics graphics, JournalChrome chrome,
                                        int mouseX, int mouseY, float partialTick) {
        Layout layout = calculateLayout(width, height);
        ClientWorldpackData.Snapshot data = ClientWorldpackData.get();
        renderStatus(graphics, layout.status(), data);

        Rect viewport = listViewport(layout);
        if (!data.synced()) {
            drawNotice(graphics, viewport,
                    Component.translatable("screen.seeking_immortals.worldpack.waiting"));
            return;
        }

        int total = tab == Tab.REGIONS ? data.regions().size() : data.realms().size();
        int rowHeight = layout.rowHeight();
        listPanel.setBounds(toUi(viewport))
                .setRowMetrics(rowHeight, 0)
                .setContentRows(total);
        listPanel.clampToViewport();

        int listScroll = listPanel.scrollRows();
        int visible = listPanel.visibleRowCount();
        int hovered = listPanel.hoveredRow(mouseX, mouseY, total);
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
        listPanel.drawScrollbar(graphics);
    }

    private void renderStatus(GuiGraphics graphics, Rect status, ClientWorldpackData.Snapshot data) {
        if (status.height() < 9) return;
        int y = status.y() + 1;
        int bottom = status.bottom();
        y = statusLine(graphics, status, y, bottom,
                Component.translatable("screen.seeking_immortals.worldpack.current_region",
                        PlayerDisplayText.safeLiteral(data.currentRegionDisplay(),
                                "text.seeking_immortals.unknown_region")),
                ImmortalUiSkin.JOURNAL_PAPER);
        if (!data.activeSecretRealmId().isBlank()) {
            y = statusLine(graphics, status, y, bottom,
                    Component.translatable("screen.seeking_immortals.worldpack.active_realm",
                            PlayerDisplayText.safeLiteral(data.activeSecretRealmDisplay(),
                                    "text.seeking_immortals.unknown_secret_realm")), ImmortalUiSkin.JOURNAL_SPIRIT);
            y = statusLine(graphics, status, y, bottom,
                    Component.translatable("screen.seeking_immortals.worldpack.in_realm",
                            PlayerDisplayText.safeLiteral(data.activeSecretRealmDisplay(),
                                    "text.seeking_immortals.unknown_secret_realm")), ImmortalUiSkin.JOURNAL_PAPER_MUTED);
        }
        long eventRemaining = data.currentDailyEventRemainingTicks();
        if (!data.dailyEventId().isBlank() && eventRemaining > 0L) {
            y = statusLine(graphics, status, y, bottom,
                    Component.translatable("screen.seeking_immortals.worldpack.daily_event",
                            PlayerDisplayText.safeLiteral(data.dailyEventDisplay(),
                                    "text.seeking_immortals.unknown_event"),
                            Math.max(1L, eventRemaining / 20L)),
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
        String text = Component.translatable("screen.seeking_immortals.worldpack.region_summary",
                PlayerDisplayText.safeLiteral(region.display(), "text.seeking_immortals.unknown_region"),
                ArtifactDisplayTexts.realm(region.minRealm()),
                String.format("%.2f", region.auraMultiplier())).getString();
        ImmortalUiSkin.drawStringFit(font, graphics, text, row.x() + 4, row.y() + 3, textWidth,
                region.current() ? ImmortalUiSkin.JOURNAL_JADE_TEXT : ImmortalUiSkin.JOURNAL_PAPER, false);
        if (layout.rowHeight() >= 24) {
            String meta = Component.translatable("screen.seeking_immortals.worldpack.anchor_state",
                    bool(region.anchorReady())).getString();
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
        String text = Component.translatable("screen.seeking_immortals.worldpack.realm_summary",
                PlayerDisplayText.safeLiteral(realm.display(), "text.seeking_immortals.unknown_secret_realm"),
                ArtifactDisplayTexts.realm(realm.minRealm()), cooldown).getString();
        ImmortalUiSkin.drawStringFit(font, graphics, text, row.x() + 4, row.y() + 3, textWidth,
                realm.active() ? ImmortalUiSkin.JOURNAL_SPIRIT : ImmortalUiSkin.JOURNAL_PAPER, false);
        if (layout.rowHeight() >= 24) {
            String meta = Component.translatable("screen.seeking_immortals.worldpack.ticket_requirement",
                    PlayerDisplayText.translatedOr(realm.ticketDescriptionId(),
                            "text.seeking_immortals.unknown_requirement")).getString();
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
        ClientWorldpackData.Snapshot data = ClientWorldpackData.get();
        int total = tab == Tab.REGIONS ? data.regions().size() : data.realms().size();
        listPanel.setBounds(toUi(listViewport(layout)))
                .setRowMetrics(layout.rowHeight(), 0)
                .setContentRows(total);
        int before = listPanel.scrollRows();
        if (listPanel.mouseScrolledRows(mouseX, mouseY, delta, total)) {
            if (listPanel.scrollRows() != before) {
                rebuildActionWidgets();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private boolean prepareListPointer(Layout layout) {
        ClientWorldpackData.Snapshot data = ClientWorldpackData.get();
        if (!data.synced()) {
            return false;
        }
        int total = tab == Tab.REGIONS ? data.regions().size() : data.realms().size();
        listPanel.setBounds(toUi(listViewport(layout)))
                .setRowMetrics(layout.rowHeight(), 0)
                .setContentRows(total);
        listPanel.clampToViewport();
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (prepareListPointer(calculateLayout(width, height))
                && listPanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        int before = listPanel.scrollRows();
        if (listPanel.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            if (listPanel.scrollRows() != before) {
                rebuildActionWidgets();
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (listPanel.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
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

    /** Package-visible for tests: rebuild fingerprint for return-button / cooldown readiness. */
    static int actionState(ClientWorldpackData.Snapshot data) {
        int hash = Boolean.hashCode(!data.activeSecretRealmId().isBlank());
        for (ClientWorldpackData.SecretRealm realm : data.realms()) {
            hash = 31 * hash + Boolean.hashCode(data.currentRealmCooldownTicks(realm) <= 0L);
        }
        return hash;
    }

    /** Travel is only active when the region is anchored, not current, and no secret realm is open. */
    static boolean canTravelRegion(ClientWorldpackData.Snapshot data, ClientWorldpackData.Region region) {
        return region != null && data != null
                && region.anchorReady()
                && !region.current()
                && data.activeSecretRealmId().isBlank();
    }

    /** Enter is only active when anchor/current-region ready, not active, cooldown elapsed, and not already in a realm. */
    static boolean canEnterRealm(ClientWorldpackData.Snapshot data, ClientWorldpackData.SecretRealm realm) {
        return realm != null && data != null
                && realm.anchorReady()
                && realm.currentRegion()
                && !realm.active()
                && data.currentRealmCooldownTicks(realm) <= 0
                && data.activeSecretRealmId().isBlank();
    }

    private static String effectDescription(String effect) {
        String key = effectDescriptionKey(effect);
        return key.isBlank()
                ? Component.translatable("screen.seeking_immortals.worldpack.effect.unknown").getString()
                : Component.translatable(key).getString();
    }

    static String effectDescriptionKey(String effect) {
        if (effect == null || effect.isBlank()) return "";
        if (effect.startsWith("quest_") || effect.startsWith("auction_")) {
            return "screen.seeking_immortals.worldpack.effect.opportunity";
        }
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
            case "cultivation_speed_1.1_1day", "cultivation_speed_1.2_3day",
                    "cultivation_buff_minor", "cultivation_buff_tag" ->
                    "screen.seeking_immortals.worldpack.effect.cultivation_bonus";
            case "breakthrough_chance_small" ->
                    "screen.seeking_immortals.worldpack.effect.breakthrough_bonus";
            case "contribution_gain_1.5_1day" ->
                    "screen.seeking_immortals.worldpack.effect.contribution_bonus";
            case "merit_mult_2" -> "screen.seeking_immortals.worldpack.effect.merit_double";
            case "herb_shop_price_x1.3" -> "screen.seeking_immortals.worldpack.effect.herb_price_up";
            case "shop_herb_discount" -> "screen.seeking_immortals.worldpack.effect.herb_price_down";
            case "tax_mult" -> "screen.seeking_immortals.worldpack.effect.market_tax";
            case "ferry_cost_double" -> "screen.seeking_immortals.worldpack.effect.ferry_cost_double";
            case "ferry_delay" -> "screen.seeking_immortals.worldpack.effect.ferry_delayed";
            case "movement_debuff_outdoor", "movement_debuff" ->
                    "screen.seeking_immortals.worldpack.effect.movement_debuff";
            case "demon_qi_tick", "demonization_risk_tag", "yin_damage_ambient",
                    "ghost_hunt_risk", "spatial_damage_risk", "tribulation_pressure",
                    "tribulation_prep_event" -> "screen.seeking_immortals.worldpack.effect.environment_hazard";
            case "spawn_beast_wave", "spawn_elite", "random_ambush_low", "sea_spawn_boost",
                    "yin_wraith", "spawn_multiplier", "combat_tier" ->
                    "screen.seeking_immortals.worldpack.effect.encounter";
            case "herb_growth_boost" -> "screen.seeking_immortals.worldpack.effect.herb_growth";
            case "star_palace_patrol_bonus" -> "screen.seeking_immortals.worldpack.effect.patrol_bonus";
            case "inverse_star_smuggle_chance" -> "screen.seeking_immortals.worldpack.effect.smuggle_chance";
            case "pvp_disabled_factions", "pvp_local" ->
                    "screen.seeking_immortals.worldpack.effect.local_pvp_rules";
            case "pearl_raw_stock", "high_herb", "bu_tian_pill" ->
                    "screen.seeking_immortals.worldpack.effect.rare_stock";
            case "huangfeng_entry", "treasure_fair_invite", "auction_notice",
                    "clan_quest_offer", "faction_conflict_minor" ->
                    "screen.seeking_immortals.worldpack.effect.opportunity";
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
            return PlayerDisplayText.itemName(itemId);
        }
        Item item = ForgeRegistries.ITEMS.getValue(location);
        return item == null || item == Items.AIR
                ? PlayerDisplayText.itemName(itemId) : PlayerDisplayText.itemName(item);
    }

    private static UiRect toUi(Rect rect) {
        return new UiRect(rect.x(), rect.y(), rect.width(), rect.height());
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
