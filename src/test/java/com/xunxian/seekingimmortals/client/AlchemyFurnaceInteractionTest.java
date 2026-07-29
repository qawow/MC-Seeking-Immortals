package com.xunxian.seekingimmortals.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AlchemyFurnaceInteractionTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");

    @Test
    void menuExposesIdleStateWithoutInventingOneTick() throws Exception {
        String source = Files.readString(JAVA_ROOT.resolve(
                Path.of("menu", "AlchemyFurnaceMenu.java")));
        assertTrue(source.contains("return Math.max(0, data.get(1));"),
                "idle furnace total must remain zero instead of being rewritten to one");
        assertTrue(source.contains("public boolean isCrafting()"),
                "menu must expose an explicit crafting state");
        assertTrue(source.contains("getProgress() > 0 && getTotal() > 0"),
                "crafting state must require positive remaining and total ticks");
    }

    @Test
    void furnaceLabelsUseTheContainerLabelPassBeforeTheFinalTooltip() throws Exception {
        String source = Files.readString(JAVA_ROOT.resolve(
                Path.of("client", "AlchemyFurnaceScreen.java")));
        int labels = source.indexOf("protected void renderLabels(");
        int helper = source.indexOf("renderFurnaceLabels(graphics)", labels);
        assertTrue(labels >= 0 && helper > labels,
                "furnace labels must be drawn from renderLabels before AbstractContainerScreen tooltip");
        assertTrue(source.contains("progressTextKey(false)"),
                "idle furnace must use a localized idle label");
    }
}
