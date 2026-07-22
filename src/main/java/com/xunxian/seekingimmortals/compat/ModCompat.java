package com.xunxian.seekingimmortals.compat;

import net.minecraftforge.fml.ModList;

public class ModCompat {
    public static final boolean CURIOS_LOADED = ModList.get().isLoaded("curios");
    public static final boolean JEI_LOADED = ModList.get().isLoaded("jei");
    public static final boolean PATCHOULI_LOADED = ModList.get().isLoaded("patchouli");
    public static final boolean FTB_QUESTS_LOADED = ModList.get().isLoaded("ftbquests");

    // 可选兼容性检测。
    public static final boolean JADE_LOADED = ModList.get().isLoaded("jade");

    // 必需运行时依赖；保留该标记用于诊断。
    public static final boolean GECKOLIB_LOADED = ModList.get().isLoaded("geckolib");

    private ModCompat() {}
}
