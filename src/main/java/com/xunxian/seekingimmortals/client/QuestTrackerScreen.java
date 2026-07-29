package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.artifact.ArtifactDisplayTexts;
import com.xunxian.seekingimmortals.catalog.ExtendedCatalogService;
import com.xunxian.seekingimmortals.npc.NamedNpcRegistry;
import com.xunxian.seekingimmortals.quest.QuestPresentationService;
import com.xunxian.seekingimmortals.quest.TextQuestChainService;
import com.xunxian.seekingimmortals.region.RegionRegistry;
import com.xunxian.seekingimmortals.sect.SectDefinitionService;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Native, server-authoritative quest journal.
 *
 * <p>The packet still carries compact machine lines. This screen resolves only authored,
 * player-facing metadata and never renders a chain id, hook id, raw state token, or fallback
 * English implementation text.</p>
 */
public class QuestTrackerScreen extends AbstractJournalScreen {
    private static final int DESIRED_WIDTH = 500;
    private static final int DESIRED_HEIGHT = 320;
    private static final int ROW_HEIGHT = 24;
    private static final int ROW_GAP = 1;
    private static final int DETAIL_GAP = 3;

    private enum Filter {
        ALL("all"), AVAILABLE("available"), LOCKED("locked"), ACTIVE("active"), DONE("done");

        private final String key;

        Filter(String key) {
            this.key = key;
        }
    }

    private enum DisplayState {
        AVAILABLE,
        LOCKED,
        ACTIVE,
        DONE
    }

    private record DetailLine(String text, int color, int gapBefore) {}

    private Filter filter = Filter.ALL;
    private List<ClientQuestTrackerData.ChainLine> view = List.of();
    private ViewSignature renderedViewSignature;
    private String renderedSelectedChainId = "";
    private final TabBar<Filter> filterBar = new TabBar<>(Filter.ALL);
    private final ScrollableListPanel listPanel = new ScrollableListPanel();
    private final ScrollableListPanel detailPanel = new ScrollableListPanel();

    private Button primaryButton;
    private Button righteousButton;
    private Button neutralButton;
    private Button demonicButton;

    public QuestTrackerScreen() {
        super(Component.translatable("screen.seeking_immortals.quest_tracker.title"));
        listPanel.setScrollStep(18)
                .setRowMetrics(ROW_HEIGHT, ROW_GAP)
                .setContentInsets(4, 2, 6, 2)
                .setScissorInsets(1, 1, 1, 1)
                .setScrollbarInsetRight(3)
                .setScrollbarTrackInsets(1, 1);
        detailPanel.setScrollStep(18)
                .setContentInsets(6, 5, 8, 5)
                .setScissorInsets(1, 1, 1, 1)
                .setScrollHeightReduce(8)
                .setScrollbarInsetRight(3)
                .setScrollbarTrackInsets(1, 1);
    }

    @Override
    protected UiClimate defaultClimate() {
        return UiClimate.BAMBOO_SLIP;
    }

    @Override
    protected void init() {
        super.init();
        rebuildView();
        rebuildButtons();
    }

    /** Called by the packet handler after a fresh authoritative snapshot arrives. */
    public void refreshWidgets() {
        rebuildView();
        clearWidgets();
        rebuildButtons();
    }

    private void rebuildView() {
        Map<String, ClientQuestTrackerData.ChainLine> unique = new LinkedHashMap<>();
        for (String raw : ClientQuestTrackerData.lines()) {
            ClientQuestTrackerData.parseChainLine(raw).ifPresent(line -> {
                if (matchesFilter(stateOf(line))) {
                    unique.putIfAbsent(line.id(), line);
                }
            });
        }
        List<ClientQuestTrackerData.ChainLine> next = new ArrayList<>(unique.values());
        // Keep authored/catalog order where possible; unknown rows are placed last without exposing ids.
        next.sort(Comparator.comparingInt((ClientQuestTrackerData.ChainLine line) -> stateOrder(stateOf(line)))
                .thenComparing(line -> QuestPresentationService.title(line.id(), chineseLocale()),
                        String.CASE_INSENSITIVE_ORDER));
        ViewSignature nextSignature = viewSignature(filter.key, next);
        view = List.copyOf(next);
        boolean selectedStillVisible = view.stream()
                .anyMatch(line -> line.id().equals(ClientQuestTrackerData.selectedChainId()));
        if (!selectedStillVisible && !view.isEmpty()) {
            ClientQuestTrackerData.selectChain(view.get(0).id());
        }
        String currentSelectedChainId = ClientQuestTrackerData.selectedChainId();
        if (renderedViewSignature == null || viewChanged(renderedViewSignature, nextSignature)) {
            listPanel.resetScroll();
        }
        if (selectedChainChanged(renderedSelectedChainId, currentSelectedChainId)) {
            detailPanel.resetScroll();
        }
        renderedViewSignature = nextSignature;
        renderedSelectedChainId = currentSelectedChainId;
    }

    private boolean matchesFilter(DisplayState state) {
        return filter == Filter.ALL || switch (filter) {
            case AVAILABLE -> state == DisplayState.AVAILABLE;
            case LOCKED -> state == DisplayState.LOCKED;
            case ACTIVE -> state == DisplayState.ACTIVE;
            case DONE -> state == DisplayState.DONE;
            case ALL -> true;
        };
    }

    private void rebuildButtons() {
        Layout layout = calculateLayout(width, height);
        filterBar.setSelected(filter).clearTabs()
                .addTab(Filter.ALL, Component.translatable("screen.seeking_immortals.quest_tracker.filter_all"),
                        layout.filters().get(0))
                .addTab(Filter.AVAILABLE, Component.translatable("screen.seeking_immortals.quest_tracker.filter_available"),
                        layout.filters().get(1))
                .addTab(Filter.LOCKED, Component.translatable("screen.seeking_immortals.quest_tracker.filter_locked"),
                        layout.filters().get(2))
                .addTab(Filter.ACTIVE, Component.translatable("screen.seeking_immortals.quest_tracker.filter_active"),
                        layout.filters().get(3))
                .addTab(Filter.DONE, Component.translatable("screen.seeking_immortals.quest_tracker.filter_done"),
                        layout.filters().get(4))
                .setOnSelect(this::setFilter);
        for (ImmortalButton button : filterBar.attach(null)) {
            addRenderableWidget(button);
        }

        Optional<ClientQuestTrackerData.ChainLine> selected = selectedLine();
        String chainId = selected.map(ClientQuestTrackerData.ChainLine::id).orElse("");
        DisplayState state = selected.map(QuestTrackerScreen::stateOf).orElse(DisplayState.LOCKED);
        boolean active = selected.isPresent() && state == DisplayState.ACTIVE && !chainId.isBlank();
        boolean affordable = selected.map(line -> !costMissing(line)).orElse(false);
        boolean canBranch = active && selected.map(line -> !line.branchLocked()).orElse(false);

        List<UiRect> buttons = layout.buttons();
        addButton(buttons.get(0), Component.translatable("screen.seeking_immortals.quest_tracker.refresh"),
                button -> sendAction("sync"), false);
        primaryButton = addButton(buttons.get(1), primaryLabel(state), button -> {
            if (chainId.isBlank()) {
                return;
            }
            if (state == DisplayState.AVAILABLE) {
                sendAction("start:" + chainId);
            } else if (state == DisplayState.ACTIVE) {
                sendAction("advance:" + chainId);
            }
        }, true);
        primaryButton.active = state == DisplayState.AVAILABLE || (active && affordable);

        righteousButton = branchButton(buttons.get(2), "righteous", chainId, canBranch,
                "screen.seeking_immortals.quest_tracker.branch_righteous");
        neutralButton = branchButton(buttons.get(3), "neutral", chainId, canBranch,
                "screen.seeking_immortals.quest_tracker.branch_neutral");
        demonicButton = branchButton(buttons.get(4), "demonic", chainId, canBranch,
                "screen.seeking_immortals.quest_tracker.branch_demonic");
        addButton(buttons.get(5), Component.translatable("gui.done"), button -> onClose(), false);
    }

    private void setFilter(Filter next) {
        if (next == null || next == filter) {
            return;
        }
        filter = next;
        rebuildView();
        clearWidgets();
        rebuildButtons();
    }

    private Button branchButton(UiRect rect, String branch, String chainId, boolean active, String key) {
        Button button = addButton(rect, Component.translatable(key), pressed -> {
            if (!chainId.isBlank()) {
                sendAction("branch:" + chainId + ":" + branch);
            }
        }, false);
        button.active = active && !chainId.isBlank();
        return button;
    }

    private Button addButton(UiRect rect, Component label, Button.OnPress onPress, boolean primary) {
        Button button = primary
                ? ImmortalButton.primary(rect.x(), rect.y(), rect.width(), rect.height(), label, onPress)
                : ImmortalButton.secondary(rect.x(), rect.y(), rect.width(), rect.height(), label, onPress);
        addRenderableWidget(button);
        return button;
    }

    private void sendAction(String action) {
        com.xunxian.seekingimmortals.network.ModNetwork.CHANNEL.sendToServer(
                new com.xunxian.seekingimmortals.network.QuestTrackerActionPacket(action));
    }

    @Override
    protected JournalChrome journalChrome() {
        Layout layout = calculateLayout(width, height);
        return new JournalChrome(layout.panel().x(), layout.panel().y(), layout.panel().width(), layout.panel().height(),
                layout.titleBar(), null);
    }

    @Override
    protected void renderJournalTitle(GuiGraphics graphics, JournalChrome chrome, UiRect header) {
        ImmortalUiSkin.drawStringFit(font, graphics, getTitle().getString(), header.x() + 6,
                header.y() + Math.max(2, (header.height() - font.lineHeight) / 2),
                Math.max(1, header.width() - 12), ImmortalUiSkin.JOURNAL_PAPER, false);
    }

    @Override
    protected void renderJournalContent(GuiGraphics graphics, JournalChrome chrome,
                                        int mouseX, int mouseY, float partialTick) {
        Layout layout = calculateLayout(width, height);
        ImmortalUiSkin.drawInnerFrame(graphics, layout.list().x(), layout.list().y(),
                layout.list().width(), layout.list().height());
        ImmortalUiSkin.drawInnerFrame(graphics, layout.detail().x(), layout.detail().y(),
                layout.detail().width(), layout.detail().height());

        listPanel.setBounds(layout.list()).setContentRows(view.size());
        if (view.isEmpty()) {
            ImmortalUiSkin.drawWrappedText(font, graphics,
                    Component.translatable("screen.seeking_immortals.quest_tracker.empty_filtered"),
                    layout.list().x() + 6, layout.list().y() + 6,
                    Math.max(1, layout.list().width() - 12), Math.max(1, layout.list().height() - 10),
                    ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
        } else {
            listPanel.renderRows(graphics, view.size(), mouseX, mouseY, (g, index, bounds, state, hovered) -> {
                ClientQuestTrackerData.ChainLine line = view.get(index);
                boolean selected = line.id().equals(ClientQuestTrackerData.selectedChainId());
                if (selected) {
                    ImmortalUiSkin.drawListRow(g, bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                            ImmortalUiSkin.InteractionState.SELECTED);
                }
                renderListRow(g, line, bounds);
            });
        }

        List<DetailLine> details = detailLines();
        int detailWidth = Math.max(1, layout.detail().width() - 14);
        detailPanel.setBounds(layout.detail())
                .setContentHeight(measureDetail(details, detailWidth));
        detailPanel.renderContent(graphics, (g, x, y, contentWidth) ->
                renderDetailLines(g, details, x, y, Math.max(1, contentWidth), mouseX, mouseY));
    }

    private void renderListRow(GuiGraphics graphics, ClientQuestTrackerData.ChainLine line, UiRect bounds) {
        DisplayState state = stateOf(line);
        String title = displayTitle(line.id());
        String status = Component.translatable(stateKey(state)).getString();
        String top = status + " · " + title;
        ImmortalUiSkin.drawStringFit(font, graphics, top, bounds.x() + 5, bounds.y() + 3,
                Math.max(1, bounds.width() - 12), stateColor(state), false);
        if (bounds.height() >= 18) {
            String progress = progressText(line, state);
            ImmortalUiSkin.drawStringFit(font, graphics, progress, bounds.x() + 5, bounds.y() + 13,
                    Math.max(1, bounds.width() - 12), ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
        }
    }

    private List<DetailLine> detailLines() {
        Optional<ClientQuestTrackerData.ChainLine> selected = selectedLine();
        if (selected.isEmpty()) {
            return List.of(new DetailLine(Component.translatable(
                    "screen.seeking_immortals.quest_tracker.pick_hint").getString(),
                    ImmortalUiSkin.JOURNAL_PAPER_MUTED, 0));
        }
        ClientQuestTrackerData.ChainLine line = selected.get();
        DisplayState state = stateOf(line);
        boolean chinese = chineseLocale();
        Optional<QuestPresentationService.ChainPresentation> metadata =
                QuestPresentationService.find(line.id());
        List<DetailLine> details = new ArrayList<>();
        details.add(new DetailLine(displayTitle(line.id()), stateColor(state), 0));
        details.add(new DetailLine(Component.translatable("screen.seeking_immortals.quest_tracker.detail_status",
                Component.translatable(stateKey(state)), line.stage(), Math.max(0, line.steps())).getString(),
                ImmortalUiSkin.JOURNAL_SPIRIT, 1));
        details.add(new DetailLine(Component.translatable("screen.seeking_immortals.quest_tracker.detail_description",
                metadata.map(value -> chinese ? value.descriptionZh() : value.descriptionEn())
                        .filter(value -> !value.isBlank())
                        .orElseGet(() -> Component.translatable("screen.seeking_immortals.quest_tracker.description_missing").getString())).getString(),
                ImmortalUiSkin.JOURNAL_PAPER, 3));

        String requirements = requirementsText(metadata.orElse(null));
        details.add(new DetailLine(Component.translatable(
                "screen.seeking_immortals.quest_tracker.detail_requirements", requirements).getString(),
                ImmortalUiSkin.JOURNAL_PAPER, 3));

        String gate = gateText(line, state, metadata.orElse(null));
        if (!gate.isBlank()) {
            details.add(new DetailLine(Component.translatable(
                    "screen.seeking_immortals.quest_tracker.detail_gate", gate).getString(),
                    ImmortalUiSkin.JOURNAL_CINNABAR_BRIGHT, 2));
        }

        String npc = npcDisplay(TextQuestChainService.npcFor(line.id()));
        details.add(new DetailLine(Component.translatable(
                "screen.seeking_immortals.quest_tracker.detail_guide", npc).getString(),
                ImmortalUiSkin.JOURNAL_PAPER, 3));

        int objectiveStage = objectiveStage(line);
        String objective = metadata.flatMap(value -> value.stage(objectiveStage))
                .map(value -> chinese ? value.summaryZh() : value.summaryEn())
                .filter(value -> !value.isBlank())
                .orElseGet(() -> Component.translatable(
                        "screen.seeking_immortals.quest_tracker.objective_generic").getString());
        details.add(new DetailLine(Component.translatable(
                "screen.seeking_immortals.quest_tracker.detail_objective", objective).getString(),
                ImmortalUiSkin.JOURNAL_JADE_TEXT, 3));

        Optional<QuestPresentationService.StagePresentation> stageMetadata =
                metadata.flatMap(value -> value.stage(objectiveStage));
        String authoredStageConditions = stageMetadata
                .map(value -> authoredRequirementsText(value.requirements(), chinese))
                .orElse("");
        if (!authoredStageConditions.isBlank()) {
            details.add(new DetailLine(Component.translatable(
                    "screen.seeking_immortals.quest_tracker.detail_condition_authored",
                    authoredStageConditions).getString(), ImmortalUiSkin.JOURNAL_CINNABAR, 2));
        }

        if (state == DisplayState.ACTIVE) {
            details.add(new DetailLine(Component.translatable(
                    "screen.seeking_immortals.quest_tracker.detail_condition_npc", npc).getString(),
                    ImmortalUiSkin.JOURNAL_PAPER, 2));
            String cost = nextCostText(line, chinese);
            details.add(new DetailLine(Component.translatable(
                    "screen.seeking_immortals.quest_tracker.detail_condition_material", cost).getString(),
                    costMissing(line) ? ImmortalUiSkin.JOURNAL_CINNABAR_BRIGHT : ImmortalUiSkin.JOURNAL_PAPER,
                    1));
        } else if (state == DisplayState.AVAILABLE) {
            details.add(new DetailLine(Component.translatable(
                    "screen.seeking_immortals.quest_tracker.detail_condition_accept").getString(),
                    ImmortalUiSkin.JOURNAL_PAPER, 2));
        }

        details.addAll(rewardLines(line, metadata.orElse(null), chinese));
        return List.copyOf(details);
    }

    private List<DetailLine> rewardLines(ClientQuestTrackerData.ChainLine line,
                                          QuestPresentationService.ChainPresentation metadata,
                                          boolean chinese) {
        List<DetailLine> result = new ArrayList<>();
        List<TextQuestChainService.RewardPreview> finale =
                QuestPresentationService.finaleRewards(line.id());
        result.add(new DetailLine(Component.translatable(
                "screen.seeking_immortals.quest_tracker.detail_rewards",
                rewardText(finale, chinese)).getString(), ImmortalUiSkin.JOURNAL_SPIRIT, 4));
        List<TextQuestChainService.RewardPreview> midpoint =
                QuestPresentationService.midpointRewards(line.id());
        if (!midpoint.isEmpty()) {
            int milestone = metadata == null ? 0 : Math.max(2, metadata.stepCount() / 2);
            result.add(new DetailLine(Component.translatable(
                    "screen.seeking_immortals.quest_tracker.detail_mid_reward", milestone,
                    rewardText(midpoint, chinese)).getString(), ImmortalUiSkin.JOURNAL_PAPER, 2));
        }
        String branch = normalizedBranch(line.branch());
        result.add(new DetailLine(Component.translatable(
                "screen.seeking_immortals.quest_tracker.detail_branch_reward",
                branchDisplay(branch), rewardText(QuestPresentationService.branchRewards(branch), chinese)).getString(),
                ImmortalUiSkin.JOURNAL_PAPER, 2));
        if (line.rewarded()) {
            result.add(new DetailLine(Component.translatable(
                    "screen.seeking_immortals.quest_tracker.reward_claimed").getString(),
                    ImmortalUiSkin.JOURNAL_JADE_TEXT, 1));
        }
        return result;
    }

    private int measureDetail(List<DetailLine> lines, int width) {
        if (lines == null || lines.isEmpty()) {
            return font.lineHeight;
        }
        int height = 0;
        for (DetailLine line : lines) {
            int wrapped = Math.max(1, font.split(Component.literal(line.text()), Math.max(1, width)).size());
            height += line.gapBefore() + wrapped * (font.lineHeight + DETAIL_GAP);
        }
        return Math.max(1, height + 4);
    }

    private void renderDetailLines(GuiGraphics graphics, List<DetailLine> lines, int x, int y,
                                   int width, int mouseX, int mouseY) {
        int cursor = y;
        for (DetailLine line : lines) {
            cursor += line.gapBefore();
            for (var sequence : font.split(Component.literal(line.text()), Math.max(1, width))) {
                graphics.drawString(font, sequence, x, cursor, line.color(), false);
                cursor += font.lineHeight + DETAIL_GAP;
            }
        }
    }

    private Optional<ClientQuestTrackerData.ChainLine> selectedLine() {
        String selectedId = ClientQuestTrackerData.selectedChainId();
        if (selectedId != null && !selectedId.isBlank()) {
            for (ClientQuestTrackerData.ChainLine line : view) {
                if (selectedId.equals(line.id())) {
                    return Optional.of(line);
                }
            }
        }
        return view.isEmpty() ? Optional.empty() : Optional.of(view.get(0));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        Layout layout = calculateLayout(width, height);
        listPanel.setBounds(layout.list()).setContentRows(view.size());
        if (listPanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        List<DetailLine> details = detailLines();
        int detailWidth = Math.max(1, layout.detail().width() - 14);
        detailPanel.setBounds(layout.detail())
                .setContentHeight(measureDetail(details, detailWidth));
        if (detailPanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (listPanel.mouseDragged(mouseX, mouseY, button, dragX, dragY)
                || detailPanel.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        Layout layout = calculateLayout(width, height);
        listPanel.setBounds(layout.list()).setContentRows(view.size());
        ScrollableListPanel.ReleaseResult listRelease =
                listPanel.mouseReleasedResult(mouseX, mouseY, button);
        List<DetailLine> details = detailLines();
        int detailWidth = Math.max(1, layout.detail().width() - 14);
        detailPanel.setBounds(layout.detail())
                .setContentHeight(measureDetail(details, detailWidth));
        ScrollableListPanel.ReleaseResult detailRelease =
                detailPanel.mouseReleasedResult(mouseX, mouseY, button);
        if (listRelease.hasRowClick() && listRelease.clickedRow() < view.size()) {
            String selectedId = view.get(listRelease.clickedRow()).id();
            ClientQuestTrackerData.selectChain(selectedId);
            renderedSelectedChainId = selectedId;
            detailPanel.resetScroll();
            clearWidgets();
            rebuildButtons();
            return true;
        }
        if (listRelease.consumed() || detailRelease.consumed()) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        Layout layout = calculateLayout(width, height);
        listPanel.setBounds(layout.list()).setContentRows(view.size());
        if (listPanel.mouseScrolledRows(mouseX, mouseY, delta, view.size())) {
            return true;
        }
        List<DetailLine> details = detailLines();
        detailPanel.setBounds(layout.detail())
                .setContentHeight(measureDetail(details, Math.max(1, layout.detail().width() - 14)));
        if (detailPanel.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private String requirementsText(QuestPresentationService.ChainPresentation chain) {
        if (chain == null) {
            return Component.translatable("screen.seeking_immortals.quest_tracker.requirement_unknown").getString();
        }
        List<String> parts = new ArrayList<>();
        if (!chain.realmMin().isBlank()) {
            parts.add(Component.translatable("screen.seeking_immortals.quest_tracker.requirement_realm",
                    ArtifactDisplayTexts.realm(chain.realmMin())).getString());
        }
        if (!chain.regionId().isBlank()) {
            String key = "qixuan_mortal_path".equals(chain.id()) && "tiannan".equals(chain.regionId())
                    ? "screen.seeking_immortals.quest_tracker.requirement_region_tutorial"
                    : "screen.seeking_immortals.quest_tracker.requirement_region";
            parts.add(Component.translatable(key, regionDisplay(chain.regionId())).getString());
        }
        if (!chain.factionId().isBlank()) {
            parts.add(Component.translatable("screen.seeking_immortals.quest_tracker.requirement_faction",
                    factionDisplay(chain.factionId())).getString());
        }
        String authored = authoredRequirementsText(chain.requirements(), chineseLocale());
        if (!authored.isBlank()) {
            parts.add(authored);
        }
        return parts.isEmpty()
                ? Component.translatable("screen.seeking_immortals.quest_tracker.requirement_none").getString()
                : String.join(chineseLocale() ? "；" : "; ", parts);
    }

    private String authoredRequirementsText(List<QuestPresentationService.RequirementPresentation> requirements,
                                            boolean chinese) {
        if (requirements == null || requirements.isEmpty()) {
            return "";
        }
        List<String> values = new ArrayList<>();
        for (QuestPresentationService.RequirementPresentation requirement : requirements) {
            if (requirement == null) {
                continue;
            }
            String value = chinese ? requirement.textZh() : requirement.textEn();
            if (value == null || value.isBlank()) {
                continue;
            }
            if (!requirement.enforced()) {
                value = Component.translatable(
                        "screen.seeking_immortals.quest_tracker.requirement_informational", value).getString();
            }
            values.add(value);
        }
        return String.join(chinese ? "；" : "; ", values);
    }

    private String gateText(ClientQuestTrackerData.ChainLine line, DisplayState state,
                            QuestPresentationService.ChainPresentation chain) {
        if (state != DisplayState.LOCKED) {
            return "";
        }
        String gate = gateOf(line);
        return switch (gate) {
            case "REALM" -> Component.translatable("screen.seeking_immortals.quest_tracker.gate_realm").getString();
            case "REGION" -> Component.translatable("screen.seeking_immortals.quest_tracker.gate_region").getString();
            case "FACTION" -> Component.translatable("screen.seeking_immortals.quest_tracker.gate_faction").getString();
            case "PATH" -> Component.translatable("screen.seeking_immortals.quest_tracker.gate_path").getString();
            case "RACE" -> Component.translatable("screen.seeking_immortals.quest_tracker.gate_race").getString();
            case "PARENT" -> Component.translatable("screen.seeking_immortals.quest_tracker.gate_parent").getString();
            case "DATA" -> Component.translatable("screen.seeking_immortals.quest_tracker.gate_data").getString();
            default -> Component.translatable("screen.seeking_immortals.quest_tracker.gate_unknown").getString();
        };
    }

    private String nextCostText(ClientQuestTrackerData.ChainLine line, boolean chinese) {
        if (line.costNeed() > 0) {
            return itemDisplay(line.costItem(), chinese) + " " + line.owned() + "/" + line.costNeed();
        }
        Optional<TextQuestChainService.StageCost> cost =
                QuestPresentationService.nextStageCost(line.id(), line.stage());
        if (cost.isPresent()) {
            TextQuestChainService.StageCost value = cost.get();
            return itemDisplay(value.itemId(), chinese) + " 0/" + value.count();
        }
        return Component.translatable("screen.seeking_immortals.quest_tracker.material_none").getString();
    }

    private String rewardText(List<TextQuestChainService.RewardPreview> rewards, boolean chinese) {
        if (rewards == null || rewards.isEmpty()) {
            return Component.translatable("screen.seeking_immortals.quest_tracker.reward_none").getString();
        }
        List<String> values = new ArrayList<>();
        for (TextQuestChainService.RewardPreview reward : rewards) {
            if (reward == null || reward.itemId() == null || reward.itemId().isBlank()) {
                continue;
            }
            values.add(itemDisplay(reward.itemId(), chinese) + " ×" + Math.max(1, reward.count()));
        }
        if (values.isEmpty()) {
            return Component.translatable("screen.seeking_immortals.quest_tracker.reward_story").getString();
        }
        return String.join(chinese ? "、" : ", ", values);
    }

    private String itemDisplay(String rawId, boolean chinese) {
        Component unknown = Component.translatable("text.seeking_immortals.unknown_item");
        Component item = PlayerDisplayText.itemName(rawId);
        String value = item.getString();
        return value.equals(unknown.getString())
                ? QuestPresentationService.rewardFallback(rawId, chinese) : value;
    }

    private String displayTitle(String chainId) {
        String title = QuestPresentationService.title(chainId, chineseLocale());
        return title.isBlank() ? Component.translatable("text.seeking_immortals.unknown_quest").getString() : title;
    }

    private String progressText(ClientQuestTrackerData.ChainLine line, DisplayState state) {
        return switch (state) {
            case AVAILABLE -> Component.translatable("screen.seeking_immortals.quest_tracker.progress_available").getString();
            case LOCKED -> Component.translatable("screen.seeking_immortals.quest_tracker.progress_locked").getString();
            case ACTIVE, DONE -> Component.translatable("screen.seeking_immortals.quest_tracker.progress_stage",
                    Math.max(0, line.stage()), Math.max(0, line.steps())).getString();
        };
    }

    private Component primaryLabel(DisplayState state) {
        return Component.translatable(switch (state) {
            case AVAILABLE -> "screen.seeking_immortals.quest_tracker.accept";
            case ACTIVE -> "screen.seeking_immortals.quest_tracker.advance";
            case LOCKED -> "screen.seeking_immortals.quest_tracker.action_locked";
            case DONE -> "screen.seeking_immortals.quest_tracker.action_done";
        });
    }

    private static int stateColor(DisplayState state) {
        return switch (state) {
            case AVAILABLE -> ImmortalUiSkin.JOURNAL_JADE_TEXT;
            case ACTIVE -> ImmortalUiSkin.JOURNAL_SPIRIT;
            case LOCKED -> ImmortalUiSkin.JOURNAL_PAPER_MUTED;
            case DONE -> ImmortalUiSkin.JOURNAL_PAPER;
        };
    }

    private static int stateOrder(DisplayState state) {
        return switch (state) {
            case ACTIVE -> 0;
            case AVAILABLE -> 1;
            case LOCKED -> 2;
            case DONE -> 3;
        };
    }

    private String stateKey(DisplayState state) {
        return "screen.seeking_immortals.quest_tracker.state." + state.name().toLowerCase(Locale.ROOT);
    }

    private static boolean costMissing(ClientQuestTrackerData.ChainLine line) {
        return line.costNeed() > 0 && line.owned() < line.costNeed();
    }

    private String branchDisplay(String branch) {
        String normalized = normalizedBranch(branch);
        return Component.translatable("screen.seeking_immortals.quest_tracker.branch_name." + normalized).getString();
    }

    private String regionDisplay(String id) {
        return RegionRegistry.find(id).map(region -> chineseLocale() ? region.displayZh() : region.displayEn())
                .filter(value -> value != null && !value.isBlank())
                .orElseGet(() -> Component.translatable("text.seeking_immortals.unknown_region").getString());
    }

    private String factionDisplay(String id) {
        String normalized = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
        Optional<SectDefinitionService.SectDefinition> sect = SectDefinitionService.find(normalized);
        if (sect.isPresent()) {
            String value = chineseLocale() ? sect.get().displayZh() : sect.get().displayEn();
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        Optional<ExtendedCatalogService.SectEntry> catalog = ExtendedCatalogService.builtin().findSect(normalized);
        if (catalog.isPresent() && PlayerDisplayText.isSafe(catalog.get().display())) {
            return catalog.get().display();
        }
        String key = switch (normalized) {
            case "mortal_realm" -> "text.seeking_immortals.faction.mortal_realm";
            case "chaotic_sea" -> "text.seeking_immortals.faction.chaotic_sea";
            case "dajin" -> "text.seeking_immortals.faction.dajin";
            case "demonic_path" -> "text.seeking_immortals.faction.demonic_path";
            case "tianyuan" -> "text.seeking_immortals.faction.tianyuan";
            case "mulan" -> "text.seeking_immortals.faction.mulan";
            default -> "text.seeking_immortals.unknown_faction";
        };
        return Component.translatable(key).getString();
    }

    private String npcDisplay(String id) {
        return NamedNpcRegistry.find(id).map(npc -> PlayerDisplayText.isSafe(npc.display())
                        ? npc.display() : Component.translatable("text.seeking_immortals.quest_guide").getString())
                .orElseGet(() -> Component.translatable("text.seeking_immortals.quest_guide").getString());
    }

    private static String normalizedBranch(String branch) {
        String value = branch == null ? "" : branch.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "righteous", "zheng", "dao" -> "righteous";
            case "demonic", "mo", "xie" -> "demonic";
            default -> "neutral";
        };
    }

    private static DisplayState stateOf(ClientQuestTrackerData.ChainLine line) {
        if (line == null || line.state() == null) {
            return DisplayState.LOCKED;
        }
        return switch (line.state()) {
            case AVAILABLE -> DisplayState.AVAILABLE;
            case LOCKED -> DisplayState.LOCKED;
            case ACTIVE -> DisplayState.ACTIVE;
            case DONE -> DisplayState.DONE;
        };
    }

    static int objectiveStage(ClientQuestTrackerData.ChainLine line) {
        if (line == null) {
            return 1;
        }
        int last = Math.max(1, line.steps());
        return switch (stateOf(line)) {
            case ACTIVE -> Math.min(last, Math.max(1, line.stage() + 1));
            case DONE -> last;
            case AVAILABLE, LOCKED -> 1;
        };
    }

    private static String gateOf(ClientQuestTrackerData.ChainLine line) {
        return line == null || line.gate() == null ? "" : line.gate().name();
    }

    private boolean chineseLocale() {
        try {
            String selected = Minecraft.getInstance().getLanguageManager().getSelected();
            return selected == null || selected.toLowerCase(Locale.ROOT).startsWith("zh");
        } catch (Throwable ignored) {
            return true;
        }
    }

    static Layout calculateLayout(int screenWidth, int screenHeight) {
        int safeWidth = Math.max(1, screenWidth);
        int safeHeight = Math.max(1, screenHeight);
        int margin = safeWidth < 180 || safeHeight < 120 ? 4 : 10;
        margin = Math.min(margin, Math.min(Math.max(0, (safeWidth - 1) / 2), Math.max(0, (safeHeight - 1) / 2)));
        int panelWidth = Math.max(1, Math.min(DESIRED_WIDTH, safeWidth - margin * 2));
        int panelHeight = Math.max(1, Math.min(DESIRED_HEIGHT, safeHeight - margin * 2));
        int left = Math.max(0, (safeWidth - panelWidth) / 2);
        int top = Math.max(0, (safeHeight - panelHeight) / 2);
        UiRect panel = new UiRect(left, top, panelWidth, panelHeight);
        int pad = panelWidth < 180 || panelHeight < 120 ? 4 : 8;
        int gap = panelHeight < 120 ? 2 : 5;
        int titleHeight = Math.min(panelHeight, panelHeight < 120 ? 14 : 20);
        UiRect title = new UiRect(left + Math.min(4, panelWidth - 1), top + Math.min(4, panelHeight - 1),
                Math.max(1, panelWidth - Math.min(4, panelWidth - 1) * 2), Math.max(1, titleHeight));
        int innerX = left + pad;
        int innerW = Math.max(1, panelWidth - pad * 2);
        int filterH = Math.max(1, Math.min(panelHeight < 120 ? 11 : 15, panelHeight));
        int filterY = Math.min(panel.bottom(), title.bottom() + gap);
        int filterGap = innerW < 20 ? 0 : Math.min(3, (innerW - 5) / 4);
        int filterSpace = Math.max(1, innerW - filterGap * 4);
        List<UiRect> filters = new ArrayList<>(5);
        int consumed = 0;
        for (int i = 0; i < 5; i++) {
            int remaining = filterSpace - consumed;
            int slots = 5 - i;
            int w = Math.max(1, remaining / slots);
            int x = innerX + i * (Math.max(1, filterSpace / 5) + filterGap);
            if (i == 4) {
                x = innerX + innerW - w;
            }
            filters.add(new UiRect(Math.max(innerX, x), filterY, Math.max(1, Math.min(w, left + panelWidth - pad - x)), filterH));
            consumed += w;
        }
        int rows = panelWidth < 390 ? 2 : 1;
        int buttonH = Math.max(1, Math.min(panelHeight < 120 ? 12 : 18, panelHeight));
        int buttonGap = Math.min(4, gap);
        int footerH = rows * buttonH + (rows - 1) * buttonGap;
        int footerY = Math.max(top, panel.bottom() - pad - footerH);
        int contentY = Math.min(footerY, filterY + filterH + gap);
        int contentH = Math.max(1, footerY - contentY - gap);
        boolean stacked = panelWidth < 390 || contentH < 90;
        int listX = innerX;
        int listY = contentY;
        int listW;
        int listH;
        int detailX;
        int detailY;
        int detailW;
        int detailH;
        if (stacked) {
            int splitGap = Math.min(4, Math.max(0, contentH - 2));
            listH = Math.max(1, (contentH - splitGap) / 2);
            detailY = contentY + listH + splitGap;
            detailH = Math.max(1, contentH - listH - splitGap);
            listW = innerW;
            detailX = innerX;
            detailW = innerW;
        } else {
            int splitGap = Math.min(8, Math.max(1, innerW / 30));
            listW = Math.max(1, Math.min(210, (innerW - splitGap) * 42 / 100));
            detailX = innerX + listW + splitGap;
            detailW = Math.max(1, innerX + innerW - detailX);
            listH = contentH;
            detailY = contentY;
            detailH = contentH;
        }
        UiRect list = new UiRect(listX, listY, listW, listH);
        UiRect detail = new UiRect(detailX, detailY, detailW, detailH);
        List<UiRect> buttons = new ArrayList<>(6);
        if (rows == 2) {
            int colGap = Math.min(3, buttonGap);
            int colW = Math.max(1, (innerW - colGap * 2) / 3);
            for (int i = 0; i < 6; i++) {
                int row = i / 3;
                int col = i % 3;
                int x = innerX + col * (colW + colGap);
                int w = col == 2 ? Math.max(1, innerX + innerW - x) : colW;
                buttons.add(new UiRect(x, footerY + row * (buttonH + buttonGap), w, buttonH));
            }
        } else {
            int colGap = Math.min(4, buttonGap);
            int colW = Math.max(1, (innerW - colGap * 5) / 6);
            for (int i = 0; i < 6; i++) {
                int x = innerX + i * (colW + colGap);
                int w = i == 5 ? Math.max(1, innerX + innerW - x) : colW;
                buttons.add(new UiRect(x, footerY, w, buttonH));
            }
        }
        return new Layout(panel, title, list, detail, List.copyOf(filters),
                new UiRect(innerX, filterY, innerW, filterH), List.copyOf(buttons), stacked);
    }

    static int clampScroll(int offset, int contentHeight, int viewportHeight) {
        return ScrollableListPanel.clampScroll(offset, contentHeight, viewportHeight);
    }

    static ViewSignature viewSignature(String filterKey,
                                       List<ClientQuestTrackerData.ChainLine> lines) {
        List<String> ids = lines == null ? List.of() : lines.stream()
                .filter(Objects::nonNull)
                .map(ClientQuestTrackerData.ChainLine::id)
                .filter(Objects::nonNull)
                .toList();
        return new ViewSignature(filterKey == null ? "" : filterKey, ids);
    }

    static boolean viewChanged(ViewSignature previous, ViewSignature next) {
        return !Objects.equals(previous, next);
    }

    static boolean selectedChainChanged(String previous, String next) {
        return !Objects.equals(previous, next);
    }

    record ViewSignature(String filterKey, List<String> ids) {
    }

    record Layout(UiRect panel, UiRect titleBar, UiRect list, UiRect detail, List<UiRect> filters,
                  UiRect hint, List<UiRect> buttons, boolean stacked) {}
}
