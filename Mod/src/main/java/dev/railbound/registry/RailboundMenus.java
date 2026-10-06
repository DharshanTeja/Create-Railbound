package dev.railbound.registry;

import dev.railbound.Railbound;
import dev.railbound.steam.BunkerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RailboundMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Railbound.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<BunkerMenu>> BUNKER =
            MENUS.register("loco_bunker", () -> IMenuTypeExtension.create(BunkerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<dev.railbound.cargo.HoldMenu>> CARGO_HOLD =
            MENUS.register("cargo_hold", () -> IMenuTypeExtension.create(dev.railbound.cargo.HoldMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<dev.railbound.cargo.TankMenu>> CARGO_TANK =
            MENUS.register("cargo_tank", () -> IMenuTypeExtension.create(dev.railbound.cargo.TankMenu::new));

    private RailboundMenus() {}
}
