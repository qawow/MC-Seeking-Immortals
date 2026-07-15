package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.network.ModNetwork;
import com.xunxian.seekingimmortals.network.SkillTreeActionPacket;
import com.xunxian.seekingimmortals.skill.LifeSkillService;
import com.xunxian.seekingimmortals.skill.SkillCategory;
import com.xunxian.seekingimmortals.skill.SkillType;
import com.xunxian.seekingimmortals.skill.SpecialSkillService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Wave491: interactive life/special skill tree UI.
 * Authority only through LifeSkillService / SpecialSkillService + SkillTreeActionPacket.
 */
public class LifeSkillTreeScreen extends Screen {
    private final Screen parent;

    private static final SkillType[] LIFE = {
            SkillType.ALCHEMY,
            SkillType.ARTIFACT_REFINING,
            SkillType.TALISMAN_CRAFTING,
            SkillType.FORMATION
    };

    public LifeSkillTreeScreen(Screen parent) {
        super(Component.translatable("screen.seeking_immortals.skill_tree.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int panelW = Math.min(420, width - 24);
        int left = (width - panelW) / 2;
        int top = 40;
        addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.skill_tree.close"),
                        button -> onClose())
                .bounds(left + panelW - 70, top - 24, 64, 18)
                .build());

        int y = top + 8;
        y = addSkillRows(left + 12, y, panelW - 24, LIFE);
        y += 10;
        addSkillRows(left + 12, y, panelW - 24, SpecialSkillService.SPECIALS);
    }

    private int addSkillRows(int x, int y, int width, SkillType[] types) {
        int rowH = 22;
        for (SkillType type : types) {
            String id = type.name().toLowerCase();
            addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.skill_tree.practice"),
                            button -> ModNetwork.CHANNEL.sendToServer(
                                    new SkillTreeActionPacket(SkillTreeActionPacket.ACTION_PRACTICE, id)))
                    .bounds(x + width - 70, y, 64, 18)
                    .build());
            addRenderableWidget(Button.builder(Component.translatable("screen.seeking_immortals.skill_tree.info"),
                            button -> ModNetwork.CHANNEL.sendToServer(
                                    new SkillTreeActionPacket(SkillTreeActionPacket.ACTION_INFO, id)))
                    .bounds(x + width - 140, y, 64, 18)
                    .build());
            y += rowH;
        }
        return y;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int panelW = Math.min(420, width - 24);
        int panelH = Math.min(300, height - 40);
        int left = (width - panelW) / 2;
        int top = 28;
        ImmortalUiSkin.drawPanel(graphics, left, top, panelW, panelH);
        graphics.drawCenteredString(font, title, width / 2, top + 8, ImmortalUiSkin.COLOR_TITLE);

        int y = top + 28;
        y = section(graphics, left + 14, y, panelW - 28, "生活技能", LIFE);
        y += 8;
        section(graphics, left + 14, y, panelW - 28, "特殊技能", SpecialSkillService.SPECIALS);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private int section(GuiGraphics graphics, int x, int y, int width, String title, SkillType[] types) {
        ImmortalUiSkin.drawStringFit(font, graphics, title, x, y, width, ImmortalUiSkin.COLOR_TITLE, false);
        y += 14;
        if (minecraft == null || minecraft.player == null) {
            ImmortalUiSkin.drawStringFit(font, graphics, "…", x, y, width, ImmortalUiSkin.COLOR_TEXT_MUTED, false);
            return y + 12;
        }
        var optional = CultivationHelper.get(minecraft.player);
        for (SkillType type : types) {
            String line = optional.map(c -> LifeSkillService.summaryLine(c, type))
                    .orElse(type.getDisplayName() + " L?");
            String prereq = type.getRequiredRealm() == null ? "" : (" [" + type.getRequiredRealm().getDisplayName() + "]");
            String cat = type.getCategory() == SkillCategory.SPECIAL ? "特" : "生";
            ImmortalUiSkin.drawStringFit(font, graphics, cat + " " + line + prereq,
                    x, y, width - 150, ImmortalUiSkin.COLOR_TEXT_NORMAL, false);
            y += 22;
        }
        return y;
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
}
