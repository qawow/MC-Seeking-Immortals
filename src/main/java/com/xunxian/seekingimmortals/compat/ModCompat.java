package com.xunxian.seekingimmortals.compat;

import net.minecraftforge.fml.ModList;

public class ModCompat {
    public static final boolean CURIOS_LOADED = ModList.get().isLoaded("curios");
    public static final boolean JEI_LOADED = ModList.get().isLoaded("jei");
    public static final boolean PATCHOULI_LOADED = ModList.get().isLoaded("patchouli");

    // 以下模组需要玩家手动安装，代码已预留兼容性接口
    public static final boolean JADE_LOADED = ModList.get().isLoaded("jade");
    public static final boolean GECKOLIB_LOADED = ModList.get().isLoaded("geckolib");

    private ModCompat() {}
}
