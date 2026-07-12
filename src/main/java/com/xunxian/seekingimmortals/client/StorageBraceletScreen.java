package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.artifact.ArtifactStorageService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Graphical storage bracelet browser (Wave49).
 * Client preview of NBT-stored stacks; withdraw still uses server right-click LIFO for authority.
 */
public class StorageBraceletScreen extends Screen {
    private static final int WIDTH = 260;
    private static final int HEIGHT = 180;
    private final List<String> lines = new ArrayList<>();

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
        int left = (width - WIDTH) / 2;
        int top = (height - HEIGHT) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + WIDTH - 70, top + HEIGHT - 28, 58, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int left = (width - WIDTH) / 2;
        int top = (height - HEIGHT) / 2;
        ImmortalUiSkin.drawPanel(graphics, left, top, WIDTH, HEIGHT);
        graphics.drawCenteredString(font, title, left + WIDTH / 2, top + 10, ImmortalUiSkin.COLOR_TITLE);
        int y = top + 32;
        if (lines.isEmpty()) {
            graphics.drawString(font, Component.translatable("screen.seeking_immortals.storage_bracelet.empty"),
                    left + 14, y, ImmortalUiSkin.COLOR_TEXT_MUTED, false);
        } else {
            for (String line : lines) {
                ImmortalUiSkin.drawStringFit(font, graphics, line, left + 14, y, WIDTH - 28,
                        ImmortalUiSkin.COLOR_TEXT_NORMAL, false);
                y += 12;
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
