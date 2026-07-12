package com.xunxian.seekingimmortals.item.pill;

import com.xunxian.seekingimmortals.cultivation.Realm;
import net.minecraft.world.item.Rarity;

public enum CatalogPillType {
    SPIRIT_GATHERING("spirit_gathering_pill", Realm.MORTAL, Rarity.COMMON),
    FIRE_ORIGIN("fire_origin_pill", Realm.QI_REFINING, Rarity.UNCOMMON),
    ICE_FIRE("ice_fire_pill", Realm.QI_REFINING, Rarity.RARE),
    MARROW_CLEANSING("marrow_cleansing_pill", Realm.QI_REFINING, Rarity.RARE),
    BODY_TEMPERING("body_tempering_pill", Realm.QI_REFINING, Rarity.UNCOMMON),
    ESSENCE_CONDENSING("essence_condensing_pill", Realm.FOUNDATION_ESTABLISHMENT, Rarity.RARE),
    SOUL_GATHERING("soul_gathering_pill", Realm.QI_REFINING, Rarity.UNCOMMON),
    MARROW_REPAIR("marrow_repair_pill", Realm.QI_REFINING, Rarity.RARE),
    CLEAR_VOID("clear_void_pill", Realm.MORTAL, Rarity.UNCOMMON),
    FORGET_DUST("forget_dust_pill", Realm.MORTAL, Rarity.UNCOMMON),
    APPEARANCE_FIXING("appearance_fixing_pill", Realm.QI_REFINING, Rarity.RARE),
    LONGEVITY("longevity_pill", Realm.CORE_FORMATION, Rarity.EPIC),
    BLOOD_QI("blood_qi_pill", Realm.CORE_FORMATION, Rarity.RARE),
    RETURN_YANG_TRUE_WATER("return_yang_true_water", Realm.CORE_FORMATION, Rarity.EPIC),
    MARROW_EXTRACTING("marrow_extracting_pill", Realm.FOUNDATION_ESTABLISHMENT, Rarity.RARE),
    SOUL_BREAKING("soul_breaking_pill", Realm.QI_REFINING, Rarity.RARE),
    POISON_DRAGON_PEARL("poison_dragon_pearl", Realm.CORE_FORMATION, Rarity.EPIC),
    PRESSURE_RESIST("pressure_resist_pill", Realm.VOID_REFINEMENT, Rarity.RARE),
    SPIRIT_REALM_CONDENSE("spirit_realm_condense_pill", Realm.SOUL_TRANSFORMATION, Rarity.EPIC);

    private final String id;
    private final Realm minRealm;
    private final Rarity rarity;

    CatalogPillType(String id, Realm minRealm, Rarity rarity) {
        this.id = id;
        this.minRealm = minRealm;
        this.rarity = rarity;
    }

    public String id() {
        return id;
    }

    public Realm minRealm() {
        return minRealm;
    }

    public Rarity rarity() {
        return rarity;
    }

    public boolean futureSystemDisabled() {
        return switch (this) {
            case CLEAR_VOID, FORGET_DUST, APPEARANCE_FIXING -> true;
            default -> false;
        };
    }
}
