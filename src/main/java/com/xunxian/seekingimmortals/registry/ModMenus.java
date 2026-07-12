package com.xunxian.seekingimmortals.registry;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.menu.AlchemyFurnaceMenu;
import com.xunxian.seekingimmortals.menu.StorageBraceletMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, SeekingImmortalsMod.MODID);

    public static final RegistryObject<MenuType<AlchemyFurnaceMenu>> ALCHEMY_FURNACE =
            MENUS.register("alchemy_furnace", () -> IForgeMenuType.create(AlchemyFurnaceMenu::fromNetwork));

    public static final RegistryObject<MenuType<StorageBraceletMenu>> STORAGE_BRACELET =
            MENUS.register("storage_bracelet", () -> IForgeMenuType.create(StorageBraceletMenu::fromNetwork));

    private ModMenus() {}

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
