package com.xunxian.seekingimmortals.client.ui;

import com.xunxian.seekingimmortals.client.UiClimate;

/**
 * 云笈墨卷 (InkScroll) scene registry — the mod's current visual language.
 *
 * <p>The surface is always paper and ink: light paper grounds carry dark ink
 * strokes, and the only saturated accent is a seal color. Screen semantics pick
 * the scene (never a player toggle), replacing the retired four-material system:</p>
 * <ul>
 *   <li>{@link #QUIET_STUDY} 静室 — cultivation / methods / skills / HUD; cool paper, jade-ink seal</li>
 *   <li>{@link #FIELD_NOTES} 行录 — quests / dialogue / lore / travel; warm rice paper, plain cinnabar (default)</li>
 *   <li>{@link #LEDGER_HALL} 账房 — market / auction / sect / storage; ochre aged paper, gold-thread accents</li>
 *   <li>{@link #OMEN_RED} 凶兆 — breakthrough / qi-deviation / tribulation danger; dry-brush paper, cinnabar wash</li>
 * </ul>
 *
 * <p>Palettes still use the {@link UiClimate.Palette} token record so every
 * existing {@code ImmortalUiSkin.JOURNAL_*} call site keeps working; the values
 * are the ink-on-paper inversion (panel = light paper, {@code paper} token = ink
 * text color).</p>
 */
public enum InkScene {
    QUIET_STUDY(new UiClimate.Palette(
            // border 浓墨(冷), borderDim 淡墨, voidFill 卷外烟影, panel 冷纸, inner 纸芯, header 题签带
            0xFF3A4A42, 0x885A6A60, 0xC81A201C, 0xF2E4EAE0, 0xEED8E0D4, 0xF0CCD6C8,
            // row, rowHovered, rowSelected, rowDisabled — 墨洗层
            0x14324038, 0x2E324038, 0x473E5448, 0x0F202824,
            // control, controlHovered, controlDisabled, tabSelected
            0xF0D2DCD0, 0xF0C2D0C4, 0xDDDCE2D8, 0xF0BACCBC,
            // accent 青印, accentText 题字青墨, paper 正文墨, paperMuted 注墨
            0xFF3E6E5A, 0xFF2E5646, 0xFF262E28, 0xFF5E6A60,
            // spirit, cinnabar, cinnabarBright, warning（跨场景语义色）
            0xFF3A6A72, 0xFF9E3226, 0xFFC05038, 0xFF9A7020,
            // barBacking 计量凹槽, barHighlight 墨染高光
            0xFFCAD6CA, 0x33324038, 0xFF7A4638, 0xFF8E4A3A,
            // nodeEmpty, nodeLocked, dividerGlow 墨线, scrollbarTrack
            0xFFB8C4B8, 0xFF98A69A, 0x553A4A42, 0x66C0CCC0,
            // cultivationFill, cultivationHighlight, paperSheen, paperWeight, rimInner
            0xCC3E6E5A, 0xEE5E8E76, 0x2AFFFFFF, 0x38262E28, 0x664A7A64,
            // hudBorder, hudBacking, hudInner, hudEdge — 纸牍 HUD（半透明浅纸）
            0xC83A4A42, 0xD8E4EAE0, 0xD0D8E0D4, 0x885A6A60,
            // hudSlotFilled, hudSlotEmpty
            0xC0CCD6C8, 0x5EE4EAE0,
            // hudSkillBorder, hudSkillBacking, hudSkillInner, hudSkillSlotFilled, hudSkillSlotEmpty
            0x903A4A42, 0x52E4EAE0, 0x44D8E0D4, 0x8CCCD6C8, 0x38E4EAE0,
            UiClimate.Material.JADE
    )),
    FIELD_NOTES(new UiClimate.Palette(
            0xFF3E362A, 0x88645A48, 0xC8201C14, 0xF2ECE6D2, 0xEEE2DAC4, 0xF0D8CEB6,
            0x14342E22, 0x2E342E22, 0x47443A2A, 0x0F282418,
            0xF0DCD4C0, 0xF0CEC6B0, 0xDDE4DECC, 0xF0C8BEA6,
            0xFF9E3226, 0xFF7E2A20, 0xFF2E2A22, 0xFF6A6254,
            0xFF3A6A72, 0xFF9E3226, 0xFFC05038, 0xFF9A7020,
            0xFFD8CEB6, 0x33342E22, 0xFF7A4638, 0xFF8E4A3A,
            0xFFC4BAA2, 0xFFA69C86, 0x553E362A, 0x66CCC2AA,
            0xCC5E5A3E, 0xEE86805A, 0x2AFFFFFF, 0x382E2A22, 0x66766A4E,
            0xC83E362A, 0xD8ECE6D2, 0xD0E2DAC4, 0x88645A48,
            0xC0D8CEB6, 0x5EECE6D2,
            0x903E362A, 0x52ECE6D2, 0x44E2DAC4, 0x8CD8CEB6, 0x38ECE6D2,
            UiClimate.Material.BAMBOO
    )),
    LEDGER_HALL(new UiClimate.Palette(
            0xFF463424, 0x88705A3C, 0xC8221A10, 0xF2E6D8B4, 0xEEDCCCA4, 0xF0D0BE92,
            0x143C3020, 0x2E3C3020, 0x474E3C24, 0x0F2C2416,
            0xF0D6C69E, 0xF0C8B88E, 0xDDDECEAA, 0xF0C2B084,
            0xFF8E6420, 0xFF6E4C18, 0xFF322A1E, 0xFF6E6250,
            0xFF3A6A72, 0xFF9E3226, 0xFFC05038, 0xFF9A7020,
            0xFFD2C096, 0x333C3020, 0xFF7A4638, 0xFF8E4A3A,
            0xFFBEAC82, 0xFFA08E68, 0x55463424, 0x66C6B488,
            0xCC8E6420, 0xEEB08A38, 0x2AFFFFFF, 0x38322A1E, 0x66997642,
            0xC8463424, 0xD8E6D8B4, 0xD0DCCCA4, 0x88705A3C,
            0xC0D0BE92, 0x5EE6D8B4,
            0x90463424, 0x52E6D8B4, 0x44DCCCA4, 0x8CD0BE92, 0x38E6D8B4,
            UiClimate.Material.LACQUER
    )),
    OMEN_RED(new UiClimate.Palette(
            0xFF54241C, 0x88804038, 0xC8281410, 0xF2ECDCD2, 0xEEE2D0C4, 0xF0D8C2B4,
            0x14402A22, 0x2E402A22, 0x47562E24, 0x0F2C1E18,
            0xF0DCC8BC, 0xF0D0B8AA, 0xDDE4D2C6, 0xF0CAB2A2,
            0xFFB03A28, 0xFF8E2C1E, 0xFF32241E, 0xFF6E5A50,
            0xFF3A6A72, 0xFF9E3226, 0xFFC05038, 0xFF9A7020,
            0xFFDCC6B8, 0x33402A22, 0xFF7A4638, 0xFF8E4A3A,
            0xFFC6AC9E, 0xFFA88E80, 0x5554241C, 0x66CEB4A6,
            0xCCB03A28, 0xEECC5A40, 0x2AFFFFFF, 0x3832241E, 0x66A05040,
            0xC854241C, 0xD8ECDCD2, 0xD0E2D0C4, 0x88804038,
            0xC0D8C2B4, 0x5EECDCD2,
            0x9054241C, 0x52ECDCD2, 0x44E2D0C4, 0x8CD8C2B4, 0x38ECDCD2,
            UiClimate.Material.SEAL
    ));

    private final UiClimate.Palette palette;

    InkScene(UiClimate.Palette palette) {
        this.palette = palette;
    }

    public UiClimate.Palette palette() {
        return palette;
    }

    /** Default scene when nothing is pushed: 行录 warm rice paper. */
    public static InkScene safe(InkScene scene) {
        return scene == null ? FIELD_NOTES : scene;
    }

    /** Transition mapping from the retired four-material climates. */
    public static InkScene fromClimate(UiClimate climate) {
        if (climate == null) {
            return FIELD_NOTES;
        }
        return switch (climate) {
            case JADE_SLIP -> QUIET_STUDY;
            case BAMBOO_SLIP -> FIELD_NOTES;
            case WARM_LACQUER -> LEDGER_HALL;
            case CINNABAR_SEAL -> OMEN_RED;
        };
    }
}
