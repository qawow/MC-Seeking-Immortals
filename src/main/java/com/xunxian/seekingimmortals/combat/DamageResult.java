package com.xunxian.seekingimmortals.combat;

public class DamageResult {
    private final double finalDamage;
    private final boolean isCrit;
    private final boolean isDodged;
    private final boolean isMissed;
    private final double rawDamage;
    private final double mitigatedDamage;

    public DamageResult(double finalDamage, boolean isCrit, boolean isDodged, boolean isMissed,
                       double rawDamage, double mitigatedDamage) {
        this.finalDamage = finalDamage;
        this.isCrit = isCrit;
        this.isDodged = isDodged;
        this.isMissed = isMissed;
        this.rawDamage = rawDamage;
        this.mitigatedDamage = mitigatedDamage;
    }

    public double getFinalDamage() { return finalDamage; }
    public boolean isCrit() { return isCrit; }
    public boolean isDodged() { return isDodged; }
    public boolean isMissed() { return isMissed; }
    public double getRawDamage() { return rawDamage; }
    public double getMitigatedDamage() { return mitigatedDamage; }
}
