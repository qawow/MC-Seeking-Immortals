package com.xunxian.seekingimmortals.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

/**
 * Lightweight quest tracker screen (Wave49 Phase9 depth).
 * Wave457: authority action buttons (advance/branch) using existing packet encodings.
 */
public class QuestTrackerScreen extends Screen {
    private static final int WIDTH = 340;
    private static final int HEIGHT = 230;

    private Button advanceButton;
    private Button righteousButton;
    private Button neutralButton;
    private Button demonicButton;

    public QuestTrackerScreen() {
        super(Component.translatable("screen.seeking_immortals.quest_tracker.title"));
    }

    @Override
    protected void init() {
        super.init();
        rebuildButtons();
    }

    /** Called when tracker data refreshes while this screen is open. */
    public void refreshWidgets() {
        clearWidgets();
        rebuildButtons();
    }

    private void rebuildButtons() {
        int left = (this.width - WIDTH) / 2;
        int top = (this.height - HEIGHT) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + WIDTH - 70, top + HEIGHT - 28, 58, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.quest_tracker.refresh"), b ->
                        sendAction("sync"))
                .bounds(left + 12, top + HEIGHT - 28, 58, 20)
                .build());

        Optional<ClientQuestTrackerData.ChainLine> active = ClientQuestTrackerData.firstActiveChain();
        String chainId = active.map(ClientQuestTrackerData.ChainLine::id).orElse("");
        boolean canAct = active.isPresent() && !active.get().complete() && !chainId.isBlank();
        boolean locked = active.map(ClientQuestTrackerData.ChainLine::branchLocked).orElse(false);
        boolean canAfford = active.map(line -> line.costNeed() <= 0 || line.owned() >= line.costNeed()).orElse(false);

        advanceButton = Button.builder(Component.translatable("screen.seeking_immortals.quest_tracker.advance"), b -> {
                    if (!chainId.isBlank()) {
                        sendAction("advance:" + chainId);
                    }
                })
                .bounds(left + 76, top + HEIGHT - 28, 58, 20)
                .build();
        advanceButton.active = canAct && canAfford;
        addRenderableWidget(advanceButton);

        righteousButton = branchButton(left + 140, top + HEIGHT - 28, "righteous", chainId,
                Component.translatable("screen.seeking_immortals.quest_tracker.branch_righteous"), canAct && !locked);
        neutralButton = branchButton(left + 188, top + HEIGHT - 28, "neutral", chainId,
                Component.translatable("screen.seeking_immortals.quest_tracker.branch_neutral"), canAct);
        demonicButton = branchButton(left + 236, top + HEIGHT - 28, "demonic", chainId,
                Component.translatable("screen.seeking_immortals.quest_tracker.branch_demonic"), canAct && !locked);
    }

    private Button branchButton(int x, int y, String branch, String chainId, Component label, boolean active) {
        Button button = Button.builder(label, b -> {
                    if (!chainId.isBlank()) {
                        sendAction("branch:" + chainId + ":" + branch);
                    }
                })
                .bounds(x, y, 44, 20)
                .build();
        button.active = active && !chainId.isBlank();
        addRenderableWidget(button);
        return button;
    }

    private void sendAction(String action) {
        com.xunxian.seekingimmortals.network.ModNetwork.CHANNEL.sendToServer(
                new com.xunxian.seekingimmortals.network.QuestTrackerActionPacket(action));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int left = (this.width - WIDTH) / 2;
        int top = (this.height - HEIGHT) / 2;
        ImmortalUiSkin.drawPanel(graphics, left, top, WIDTH, HEIGHT);
        graphics.drawCenteredString(font, title, left + WIDTH / 2, top + 10, ImmortalUiSkin.COLOR_TITLE);
        List<String> lines = ClientQuestTrackerData.lines();
        int y = top + 28;
        if (lines.isEmpty()) {
            graphics.drawString(font, Component.translatable("screen.seeking_immortals.quest_tracker.empty"),
                    left + 14, y, ImmortalUiSkin.COLOR_TEXT_MUTED, false);
        } else {
            int shown = 0;
            for (String line : lines) {
                int color = ImmortalUiSkin.COLOR_TEXT_NORMAL;
                if (line.startsWith("OK ")) {
                    color = 0xFF55FF55;
                } else if (line.startsWith("ERR ")) {
                    color = 0xFFFF5555;
                }
                ImmortalUiSkin.drawStringFit(font, graphics, line, left + 14, y, WIDTH - 28, color, false);
                y += 11;
                if (++shown >= 14) {
                    break;
                }
            }
        }
        Optional<ClientQuestTrackerData.ChainLine> active = ClientQuestTrackerData.firstActiveChain();
        if (active.isPresent()) {
            ClientQuestTrackerData.ChainLine line = active.get();
            String hint;
            if (line.complete()) {
                hint = Component.translatable("screen.seeking_immortals.quest_tracker.hint_done", line.id()).getString();
            } else if (line.costNeed() > 0 && line.owned() < line.costNeed()) {
                hint = Component.translatable("screen.seeking_immortals.quest_tracker.hint_cost",
                        line.costItem(), line.owned(), line.costNeed()).getString();
            } else if (line.branchLocked()) {
                hint = Component.translatable("screen.seeking_immortals.quest_tracker.hint_locked", line.branch()).getString();
            } else {
                hint = Component.translatable("screen.seeking_immortals.quest_tracker.hint_ready", line.id()).getString();
            }
            ImmortalUiSkin.drawStringFit(font, graphics, hint, left + 14, top + HEIGHT - 44, WIDTH - 28,
                    ImmortalUiSkin.COLOR_TEXT_MUTED, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
