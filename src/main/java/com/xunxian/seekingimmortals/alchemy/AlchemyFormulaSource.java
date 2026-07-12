package com.xunxian.seekingimmortals.alchemy;

public enum AlchemyFormulaSource {
    PAPER(false, -0.25D, 0.05D, "paper"),
    JADE(false, -0.08D, 0.01D, "jade"),
    SECT_SECRET(true, 0.03D, -0.02D, "sect_secret");

    private final boolean sectAuthorized;
    private final double controlledSuccessModifier;
    private final double controlledExplosionModifier;
    private final String id;

    AlchemyFormulaSource(boolean sectAuthorized, double controlledSuccessModifier, double controlledExplosionModifier, String id) {
        this.sectAuthorized = sectAuthorized;
        this.controlledSuccessModifier = controlledSuccessModifier;
        this.controlledExplosionModifier = controlledExplosionModifier;
        this.id = id;
    }

    public boolean isSectAuthorized() {
        return sectAuthorized;
    }

    public double controlledSuccessModifier() {
        return controlledSuccessModifier;
    }

    public double controlledExplosionModifier() {
        return controlledExplosionModifier;
    }

    public String id() {
        return id;
    }

    public static AlchemyFormulaSource byId(String id) {
        for (AlchemyFormulaSource source : values()) {
            if (source.id.equals(id)) {
                return source;
            }
        }
        return PAPER;
    }
}
