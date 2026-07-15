package com.xunxian.seekingimmortals.registry;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.menu.AlchemyFurnaceMenu;
import com.xunxian.seekingimmortals.menu.AuctionHallMenu;
import com.xunxian.seekingimmortals.menu.MarketHallMenu;
import com.xunxian.seekingimmortals.menu.SectHallMenu;
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

    // Wave490: productized multi-layer halls.
    public static final RegistryObject<MenuType<MarketHallMenu>> MARKET_HALL =
            MENUS.register("market_hall", () -> IForgeMenuType.create(MarketHallMenu::fromNetwork));
    public static final RegistryObject<MenuType<AuctionHallMenu>> AUCTION_HALL =
            MENUS.register("auction_hall", () -> IForgeMenuType.create(AuctionHallMenu::fromNetwork));
    public static final RegistryObject<MenuType<SectHallMenu>> SECT_HALL =
            MENUS.register("sect_hall", () -> IForgeMenuType.create(SectHallMenu::fromNetwork));

    private ModMenus() {}

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
