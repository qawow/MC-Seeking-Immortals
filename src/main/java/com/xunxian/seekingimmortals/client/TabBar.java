package com.xunxian.seekingimmortals.client;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Shared tab strip using the majority ImmortalButton-as-tab pattern:
 * selected tabs are primary buttons, others are secondary.
 *
 * <p>CultivationStatsScreen still uses {@link ImmortalUiSkin#drawTab}; later phases can
 * migrate it onto this component without changing the public selection API.</p>
 */
public final class TabBar<T> {
    public record TabSpec<T>(T id, Component label, UiRect bounds) {
        public TabSpec {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(bounds, "bounds");
        }
    }

    private final List<TabSpec<T>> tabs = new ArrayList<>();
    private T selected;
    private Consumer<T> onSelect = value -> {
    };

    public TabBar() {
    }

    public TabBar(T initial) {
        this.selected = initial;
    }

    public T selected() {
        return selected;
    }

    public TabBar<T> setSelected(T selected) {
        this.selected = selected;
        return this;
    }

    public TabBar<T> setOnSelect(Consumer<T> onSelect) {
        this.onSelect = onSelect == null ? value -> {
        } : onSelect;
        return this;
    }

    public TabBar<T> clearTabs() {
        tabs.clear();
        return this;
    }

    public TabBar<T> addTab(T id, Component label, UiRect bounds) {
        tabs.add(new TabSpec<>(id, label, bounds));
        return this;
    }

    public TabBar<T> setTabs(List<TabSpec<T>> next) {
        tabs.clear();
        if (next != null) {
            tabs.addAll(next);
        }
        return this;
    }

    public List<TabSpec<T>> tabs() {
        return List.copyOf(tabs);
    }

    public boolean select(T id) {
        if (id == null || Objects.equals(selected, id)) {
            return false;
        }
        selected = id;
        onSelect.accept(id);
        return true;
    }

    /** Builds ImmortalButtons for the current tab set and attaches them to the screen. */
    public List<ImmortalButton> attach(Consumer<GuiEventListener> addWidget) {
        List<ImmortalButton> buttons = new ArrayList<>(tabs.size());
        for (TabSpec<T> tab : tabs) {
            boolean isSelected = Objects.equals(selected, tab.id());
            ImmortalButton button = isSelected
                    ? ImmortalButton.primary(tab.bounds().x(), tab.bounds().y(),
                    tab.bounds().width(), tab.bounds().height(), tab.label(),
                    ignored -> select(tab.id()))
                    : ImmortalButton.secondary(tab.bounds().x(), tab.bounds().y(),
                    tab.bounds().width(), tab.bounds().height(), tab.label(),
                    ignored -> select(tab.id()));
            buttons.add(button);
            if (addWidget != null) {
                addWidget.accept(button);
            }
        }
        return buttons;
    }
}
