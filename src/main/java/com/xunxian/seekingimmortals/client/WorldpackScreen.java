package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.network.ModNetwork;
import com.xunxian.seekingimmortals.network.WorldpackActionPacket;
import com.xunxian.seekingimmortals.worldpack.WorldpackGameplayService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.stream.Collectors;

public class WorldpackScreen extends Screen {
    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 272;
    private static final int LINE = 13;

    private Tab tab = Tab.REGIONS;

    public WorldpackScreen() {
        super(Component.translatable("screen.seeking_immortals.worldpack.title"));
    }

    @Override
    protected void init() {
        super.init();
        ClientWorldpackData.Snapshot data = ClientWorldpackData.get();
        int left = left();
        int top = top();
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();

        addHeaderButtons(left, top, panelWidth, data);
        addTabButtons(left, top, panelWidth);
        if (!data.synced()) {
            return;
        }
        if (tab == Tab.REGIONS) {
            addRegionButtons(left, top, panelWidth, panelHeight, data);
        } else {
            addRealmButtons(left, top, panelWidth, panelHeight, data);
        }
    }

    private void addHeaderButtons(int left, int top, int panelWidth, ClientWorldpackData.Snapshot data) {
        int smallWidth = Math.max(54, Math.min(68, (panelWidth - 36) / 5));
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.worldpack.refresh"), button ->
                        ModNetwork.CHANNEL.sendToServer(new WorldpackActionPacket(WorldpackGameplayService.ACTION_SYNC, "")))
                .bounds(left + panelWidth - smallWidth * 2 - 18, top + 8, smallWidth, 18)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.worldpack.close"), button -> onClose())
                .bounds(left + panelWidth - smallWidth - 12, top + 8, smallWidth, 18)
                .build());
        if (!data.activeSecretRealmId().isBlank()) {
            addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.worldpack.return"), button ->
                            ModNetwork.CHANNEL.sendToServer(new WorldpackActionPacket(WorldpackGameplayService.ACTION_RETURN, "")))
                    .bounds(left + 14, top + 8, smallWidth, 18)
                    .build());
        }
    }

    private void addTabButtons(int left, int top, int panelWidth) {
        int y = top + 82;
        int gap = 4;
        int buttonWidth = Math.max(1, (panelWidth - 28 - gap) / 2);
        addTabButton(Tab.REGIONS, left + 14, y, buttonWidth);
        addTabButton(Tab.REALMS, left + 14 + buttonWidth + gap, y, buttonWidth);
    }

    private void addTabButton(Tab target, int x, int y, int width) {
        addRenderableWidget(Button.builder(Component.translatable(target.key), button -> {
                    tab = target;
                    clearWidgets();
                    init();
                })
                .bounds(x, y, width, 18)
                .build());
    }

    private void addRegionButtons(int left, int top, int panelWidth, int panelHeight, ClientWorldpackData.Snapshot data) {
        int rowY = top + listTopOffset(panelHeight) + 20;
        int rows = Math.min(data.regions().size(), visibleRows(panelWidth, panelHeight));
        int actionWidth = Math.min(54, Math.max(1, panelWidth - 24));
        int actionX = Math.max(left + 4, left + panelWidth - actionWidth - 12);
        for (int i = 0; i < rows; i++) {
            ClientWorldpackData.Region region = data.regions().get(i);
            Button button = Button.builder(Component.translatable("screen.seeking_immortals.worldpack.travel"), ignored ->
                            ModNetwork.CHANNEL.sendToServer(new WorldpackActionPacket(WorldpackGameplayService.ACTION_TRAVEL, region.id())))
                    .bounds(actionX, rowY + i * 24 - 4, actionWidth, 18)
                    .build();
            button.active = region.anchorReady() && !region.current() && data.activeSecretRealmId().isBlank();
            addRenderableWidget(button);
        }
    }

    private void addRealmButtons(int left, int top, int panelWidth, int panelHeight, ClientWorldpackData.Snapshot data) {
        int rowY = top + listTopOffset(panelHeight) + 20;
        int rows = Math.min(data.realms().size(), visibleRows(panelWidth, panelHeight));
        int actionWidth = Math.min(54, Math.max(1, panelWidth - 24));
        int actionX = Math.max(left + 4, left + panelWidth - actionWidth - 12);
        for (int i = 0; i < rows; i++) {
            ClientWorldpackData.SecretRealm realm = data.realms().get(i);
            Button button = Button.builder(Component.translatable("screen.seeking_immortals.worldpack.enter"), ignored ->
                            ModNetwork.CHANNEL.sendToServer(new WorldpackActionPacket(WorldpackGameplayService.ACTION_ENTER, realm.id())))
                    .bounds(actionX, rowY + i * 24 - 4, actionWidth, 18)
                    .build();
            button.active = realm.anchorReady() && realm.currentRegion() && !realm.active()
                    && realm.remainingCooldownTicks() <= 0 && data.activeSecretRealmId().isBlank();
            addRenderableWidget(button);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        renderPanel(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void renderPanel(GuiGraphics graphics) {
        int left = left();
        int top = top();
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        ClientWorldpackData.Snapshot data = ClientWorldpackData.get();

        ImmortalUiSkin.drawPanel(graphics, left, top, panelWidth, panelHeight);
        graphics.drawCenteredString(font, title, left + panelWidth / 2, top + 12, ImmortalUiSkin.COLOR_TITLE);

        int y = top + 34;
        drawLine(graphics, left, y, Component.translatable("screen.seeking_immortals.worldpack.current_region",
                data.currentRegionDisplay().isBlank() ? "-" : data.currentRegionDisplay()));
        y += LINE;
        if (!data.activeSecretRealmId().isBlank()) {
            drawLine(graphics, left, y, Component.translatable("screen.seeking_immortals.worldpack.active_realm",
                    data.activeSecretRealmDisplay()));
            y += LINE;
        }
        if (!data.dailyEventId().isBlank()) {
            drawLine(graphics, left, y, Component.translatable("screen.seeking_immortals.worldpack.daily_event",
                    data.dailyEventDisplay(), Math.max(0L, data.dailyEventRemainingTicks() / 20L)));
            y += LINE;
            drawLine(graphics, left, y, Component.translatable("screen.seeking_immortals.worldpack.effects",
                    formatEffectDescriptions(data.dailyEventEffects())));
        }

        if (!data.synced()) {
            drawLine(graphics, left, top + listTopOffset(panelHeight) + 20,
                    Component.translatable("screen.seeking_immortals.worldpack.waiting"));
            return;
        }
        if (!data.activeSecretRealmId().isBlank()) {
            ImmortalUiSkin.drawStringFit(font, graphics,
                    Component.translatable("screen.seeking_immortals.worldpack.in_realm", data.activeSecretRealmDisplay()).getString(),
                    left + 14, top + 104, Math.max(1, panelWidth - 28), ImmortalUiSkin.COLOR_TEXT_MUTED, false);
        }
        if (tab == Tab.REGIONS) {
            renderRegions(graphics, left, top, panelWidth, panelHeight, data);
        } else {
            renderRealms(graphics, left, top, panelWidth, panelHeight, data.realms());
        }
    }

    private void renderRegions(GuiGraphics graphics, int left, int top, int panelWidth, int panelHeight,
                               ClientWorldpackData.Snapshot data) {
        List<ClientWorldpackData.Region> regions = data.regions();
        int listTop = top + listTopOffset(panelHeight);
        int rows = Math.min(regions.size(), visibleRows(panelWidth, panelHeight));
        for (int i = 0; i < rows; i++) {
            ClientWorldpackData.Region region = regions.get(i);
            String text = region.display() + " / " + region.minRealm() + " / x" + String.format("%.2f", region.auraMultiplier());
            int color = region.current() ? ImmortalUiSkin.COLOR_TITLE : ImmortalUiSkin.COLOR_TEXT_NORMAL;
            ImmortalUiSkin.drawStringFit(font, graphics, text, left + 16, listTop + 20 + i * 24,
                    Math.max(1, panelWidth - 94), color, false);
            String meta = Component.translatable("screen.seeking_immortals.worldpack.entry_id", region.id()).getString()
                    + " / " + bool(region.anchorReady());
            String route = routeRequirementText(WorldpackGameplayService.routeRequirementForDisplay(
                    data.currentRegionId(), region.id()));
            if (!route.isBlank()) {
                meta += " / " + route;
            }
            ImmortalUiSkin.drawStringFit(font, graphics, meta, left + 16, listTop + 20 + i * 24 + LINE,
                    Math.max(1, panelWidth - 94), ImmortalUiSkin.COLOR_TEXT_MUTED, false);
        }
    }

    private void renderRealms(GuiGraphics graphics, int left, int top, int panelWidth, int panelHeight,
                              List<ClientWorldpackData.SecretRealm> realms) {
        int listTop = top + listTopOffset(panelHeight);
        int rows = Math.min(realms.size(), visibleRows(panelWidth, panelHeight));
        for (int i = 0; i < rows; i++) {
            ClientWorldpackData.SecretRealm realm = realms.get(i);
            String cooldown = realm.remainingCooldownTicks() <= 0
                    ? Component.translatable("screen.seeking_immortals.worldpack.ready").getString()
                    : Component.translatable("screen.seeking_immortals.worldpack.cooldown_seconds",
                    Math.max(1L, (realm.remainingCooldownTicks() + 19L) / 20L)).getString();
            String text = realm.display() + " / " + realm.minRealm() + " / " + cooldown;
            int color = realm.active() ? ImmortalUiSkin.COLOR_TITLE : ImmortalUiSkin.COLOR_TEXT_NORMAL;
            ImmortalUiSkin.drawStringFit(font, graphics, text, left + 16, listTop + 20 + i * 24,
                    Math.max(1, panelWidth - 94), color, false);
            String meta = Component.translatable("screen.seeking_immortals.worldpack.entry_id", realm.id()).getString()
                    + " / " + Component.translatable(realm.ticketDescriptionId()).getString();
            ImmortalUiSkin.drawStringFit(font, graphics, meta, left + 16, listTop + 20 + i * 24 + LINE,
                    Math.max(1, panelWidth - 94), ImmortalUiSkin.COLOR_TEXT_MUTED, false);
        }
    }

    private void drawLine(GuiGraphics graphics, int left, int y, Component text) {
        ImmortalUiSkin.drawStringFit(font, graphics, text.getString(), left + 14, y, Math.max(1, panelWidth() - 28),
                ImmortalUiSkin.COLOR_TEXT_NORMAL, false);
    }

    private String bool(boolean value) {
        return Component.translatable(value ? "message.seeking_immortals.sect.yes" : "message.seeking_immortals.sect.no").getString();
    }

    private static String formatEffectDescriptions(List<String> effects) {
        if (effects == null || effects.isEmpty()) {
            return "-";
        }
        return effects.stream()
                .map(WorldpackScreen::effectDescription)
                .filter(description -> !description.isBlank())
                .collect(Collectors.joining(", "));
    }

    private static String effectDescription(String effect) {
        String key = effectDescriptionKey(effect);
        return key.isBlank() ? (effect == null ? "" : effect) : Component.translatable(key).getString();
    }

    static String effectDescriptionKey(String effect) {
        if (effect == null || effect.isBlank()) {
            return "";
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
            default -> "";
        };
    }

    private static String routeRequirementText(WorldpackGameplayService.RouteRequirement requirement) {
        if (requirement == null || !requirement.isPresent()) {
            return "";
        }
        String itemId = requirement.itemId();
        if (requirement.amount() > 0 && itemId != null && !itemId.isBlank()) {
            return Component.translatable(requirement.translationKey(),
                    requirement.amount(),
                    itemNameComponent(itemId)).getString();
        }
        if (itemId != null && !itemId.isBlank()) {
            return Component.translatable(requirement.translationKey(),
                    itemNameComponent(itemId)).getString();
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
                ? Component.literal(itemId)
                : Component.translatable(item.getDescriptionId());
    }

    private int left() {
        return Math.max(0, (this.width - panelWidth()) / 2);
    }

    private int top() {
        return Math.max(0, (this.height - panelHeight()) / 2);
    }

    private int panelWidth() {
        return calculatePanelWidth(this.width);
    }

    private int panelHeight() {
        return calculatePanelHeight(this.height);
    }

    static int calculatePanelWidth(int screenWidth) {
        if (screenWidth <= 0) return 1;
        int margin = screenWidth >= 260 ? 24 : Math.min(8, Math.max(0, screenWidth / 10));
        return Math.max(1, Math.min(PANEL_WIDTH, screenWidth - margin));
    }

    static int calculatePanelHeight(int screenHeight) {
        if (screenHeight <= 0) return 1;
        int margin = screenHeight >= 190 ? 24 : Math.min(8, Math.max(0, screenHeight / 10));
        return Math.max(1, Math.min(PANEL_HEIGHT, screenHeight - margin));
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

    private enum Tab {
        REGIONS("screen.seeking_immortals.worldpack.tab.regions"),
        REALMS("screen.seeking_immortals.worldpack.tab.realms");

        private final String key;

        Tab(String key) {
            this.key = key;
        }
    }
}
