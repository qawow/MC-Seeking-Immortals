package com.xunxian.seekingimmortals.client;

/**
 * @deprecated Transitional alias for the retired four-material system. The live
 * visual language is 云笈墨卷 {@link com.xunxian.seekingimmortals.client.ui.InkScene};
 * palettes here are no longer bound — {@code ImmortalUiSkin} maps every climate
 * to its InkScene replacement. Screens should migrate to {@code defaultScene()}.
 *
 * Layered fanren journal climates for Seeking Immortals UI.
 *
 * <p>One design system, four materials chosen by screen semantics (not a player theme
 * switcher):</p>
 * <ul>
 *   <li>{@link #JADE_SLIP} — cultivation / methods / skills / HUD jade tablets</li>
 *   <li>{@link #BAMBOO_SLIP} — quests / dialogue / lore / travel bamboo notes</li>
 *   <li>{@link #WARM_LACQUER} — market / auction / sect ledger / storage lacquer desk</li>
 *   <li>{@link #CINNABAR_SEAL} — breakthrough / qi-dev / tribulation danger overlay</li>
 * </ul>
 */
@Deprecated
public enum UiClimate {
    JADE_SLIP(new Palette(
            0xFF6A9A88, 0x88607868, 0xF0101816, 0xF214201C, 0xE61C2824, 0xF00E1412,
            0x33202824, 0x77384838, 0xAA2E3C34, 0x44101410,
            0xF0182420, 0xF0283830, 0xDD0E1412, 0xF0304038,
            0xFF7AB898, 0xFFB8D8C8, 0xFFE4E8DC, 0xFF98A098,
            0xFF8AA8A0, 0xFF8E3A32, 0xFFD97A62, 0xFFC4A86A,
            0xEE0C1410, 0x337AB898, 0xFF4A2A22, 0xFF4E2A22,
            0xFF304038, 0xFF4A5A50, 0x556A9A88, 0x990E1410,
            0x887AB898, 0xAAB8D8C8, 0x22E4E8DC, 0x44101816, 0x6690C0A8,
            0xCC6A9A88, 0xE8101816, 0xE814201C, 0x88607868,
            0xCC182420, 0x66101412, 0x996A9A88, 0x66101816, 0x5514201C,
            0x99182420, 0x44101412,
            Material.JADE
    )),
    BAMBOO_SLIP(new Palette(
            0xFF6B8F5A, 0x885A7348, 0xF012160F, 0xF2181E16, 0xE6222A1E, 0xF010140E,
            0x33202818, 0x77384828, 0xAA2E3C28, 0x4410140C,
            0xF01A2218, 0xF0283424, 0xDD10140E, 0xF032402C,
            0xFF7A9E6A, 0xFFB8D0A8, 0xFFE6E0C8, 0xFFA8A890,
            0xFF8AA8A0, 0xFF8E3A32, 0xFFD97A62, 0xFFC4A86A,
            0xEE0E120C, 0x3390B070, 0xFF4A2A22, 0xFF4E2A22,
            0xFF343C30, 0xFF4A5240, 0x556B8F5A, 0x9910140C,
            0x887A9E6A, 0xAAB8D0A8, 0x22E6E0C8, 0x4412160F, 0x6690B070,
            0xCC6B8F5A, 0xE812160F, 0xE8181E16, 0x885A7348,
            0xCC1A2218, 0x6610140E, 0x996B8F5A, 0x6612160F, 0x55181E16,
            0x991A2218, 0x4410140E,
            Material.BAMBOO
    )),
    WARM_LACQUER(new Palette(
            0xFFC4A86A, 0x88A08850, 0xF016120E, 0xF21C1812, 0xE62A2218, 0xF0120E0A,
            0x33282018, 0x77483828, 0xAA3C2E22, 0x4414100C,
            0xF0221A14, 0xF0342820, 0xDD14100C, 0xF0403024,
            0xFF6FAE88, 0xFFB8D0A8, 0xFFF0E4C0, 0xFFB8A882,
            0xFF8AA8A0, 0xFF8E3A32, 0xFFD97A62, 0xFFC4A86A,
            0xEE120E0A, 0x33C4A86A, 0xFF4A2A22, 0xFF4E2A22,
            0xFF3C3428, 0xFF524838, 0x55C4A86A, 0x9914100C,
            0x886FAE88, 0xAAB8D0A8, 0x22F0E4C0, 0x4416120E, 0x66D4BC78,
            0xCCC4A86A, 0xE816120E, 0xE81C1812, 0x88A08850,
            0xCC221A14, 0x6614100C, 0x99C4A86A, 0x6616120E, 0x551C1812,
            0x99221A14, 0x4414100C,
            Material.LACQUER
    )),
    CINNABAR_SEAL(new Palette(
            0xFFA04038, 0x88803028, 0xF01A100E, 0xF2221612, 0xE62A1814, 0xF0140C0A,
            0x33282018, 0x77483028, 0xAA3C2820, 0x4414100C,
            0xF0221814, 0xF0342420, 0xDD14100C, 0xF0402C24,
            0xFFD97A62, 0xFFF0B8A0, 0xFFF0D8C8, 0xFFC0A090,
            0xFF8AA8A0, 0xFF8E3A32, 0xFFD97A62, 0xFFC4A86A,
            0xEE140C0A, 0x33D97A62, 0xFF4A2A22, 0xFF4E2A22,
            0xFF3C2820, 0xFF524038, 0x55A04038, 0x9914100C,
            0x88D97A62, 0xAAF0B8A0, 0x22F0D8C8, 0x441A100E, 0x66D97A62,
            0xCCA04038, 0xE81A100E, 0xE8221612, 0x88803028,
            0xCC221814, 0x6614100C, 0x99A04038, 0x661A100E, 0x55221612,
            0x99221814, 0x4414100C,
            Material.SEAL
    ));

    public enum Material {
        JADE,
        BAMBOO,
        LACQUER,
        SEAL
    }

    /** Full ARGB token set for one climate. */
    public record Palette(
            int border,
            int borderDim,
            int voidFill,
            int panel,
            int inner,
            int header,
            int row,
            int rowHovered,
            int rowSelected,
            int rowDisabled,
            int control,
            int controlHovered,
            int controlDisabled,
            int tabSelected,
            int accent,
            int accentText,
            int paper,
            int paperMuted,
            int spirit,
            int cinnabar,
            int cinnabarBright,
            int warning,
            int barBacking,
            int barHighlight,
            int iconInset,
            int sealInset,
            int nodeEmpty,
            int nodeLocked,
            int dividerGlow,
            int scrollbarTrack,
            int cultivationFill,
            int cultivationHighlight,
            int paperSheen,
            int paperWeight,
            int rimInner,
            int hudBorder,
            int hudBacking,
            int hudInner,
            int hudEdge,
            int hudSlotFilled,
            int hudSlotEmpty,
            int hudSkillBorder,
            int hudSkillBacking,
            int hudSkillInner,
            int hudSkillSlotFilled,
            int hudSkillSlotEmpty,
            Material material
    ) {}

    private final Palette palette;

    UiClimate(Palette palette) {
        this.palette = palette;
    }

    public Palette palette() {
        return palette;
    }

    public static UiClimate safe(UiClimate climate) {
        return climate == null ? BAMBOO_SLIP : climate;
    }
}
