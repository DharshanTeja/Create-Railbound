package dev.railbound;

import dev.railbound.registry.RailboundMenus;
import dev.railbound.registry.RailboundParticles;
import dev.railbound.registry.RailboundStorageTypes;
import dev.railbound.steam.BunkerBlockEntity;
import dev.railbound.steam.WaterTankBlock;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import com.mojang.logging.LogUtils;
import dev.railbound.bogey.RailboundBogeyStyles;
import dev.railbound.carriage.CarriageAttachment;
import dev.railbound.carriage.CarriageBehaviours;
import dev.railbound.carriage.CarriageFittings;
import dev.railbound.carriage.CarriageProtection;
import dev.railbound.coupling.TrainCoupling;
import dev.railbound.network.RailboundNetwork;
import dev.railbound.registry.RailboundBlockEntities;
import dev.railbound.registry.RailboundBlocks;
import dev.railbound.registry.RailboundComponents;
import dev.railbound.registry.RailboundItems;
import dev.railbound.registry.RailboundTabs;
import dev.railbound.trainset.load.TrainsetDesignManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;

@Mod(Railbound.MOD_ID)
public final class Railbound {
    public static final String MOD_ID = "railbound";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Railbound(IEventBus modBus, ModContainer container) {
        LOGGER.info("Create: Railbound loading");
        RailboundBlocks.BLOCKS.register(modBus);
        RailboundBogeyStyles.register();
        RailboundBlockEntities.BLOCK_ENTITIES.register(modBus);
        RailboundStorageTypes.ITEM_TYPES.register(modBus);
        RailboundStorageTypes.FLUID_TYPES.register(modBus);
        RailboundParticles.PARTICLES.register(modBus);
        RailboundMenus.MENUS.register(modBus);
        modBus.addListener(Railbound::onRegisterCapabilities);
        modBus.addListener(Railbound::onCommonSetup);
        RailboundComponents.COMPONENTS.register(modBus);
        RailboundItems.ITEMS.register(modBus);
        RailboundTabs.TABS.register(modBus);
        RailboundTabs.refreshOnDesignChange();
        modBus.addListener(RailboundNetwork::register);
        NeoForge.EVENT_BUS.addListener(Railbound::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(RailboundNetwork::onDatapackSync);
        NeoForge.EVENT_BUS.addListener(CarriageProtection::onBreak);
        NeoForge.EVENT_BUS.addListener(CarriageProtection::onDetonate);
        NeoForge.EVENT_BUS.addListener(CarriageProtection::onPlace);
        NeoForge.EVENT_BUS.addListener(CarriageFittings::onRightClick);
        NeoForge.EVENT_BUS.addListener(CarriageFittings::onBreak);
        NeoForge.EVENT_BUS.addListener(TrainCoupling::tick);
        // before the carriage's own break handling, which would remove it
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGH, dev.railbound.cargo.CargoAccess::onBreak);
        NeoForge.EVENT_BUS.addListener(dev.railbound.cargo.CargoAccess::onRightClick);
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        CarriageAttachment.register();
        event.enqueueWork(CarriageBehaviours::register);
        event.enqueueWork(RailboundStorageTypes::linkToBlocks);
    }

    /** A standing loco takes coal into its bunker and water into its bunker or tanks (never giving either back). */
    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RailboundBlockEntities.BUNKER.get(),
                (bunker, side) -> bunker.coalIn());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RailboundBlockEntities.BUNKER.get(),
                (bunker, side) -> bunker.waterIn());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RailboundBlockEntities.WATER_CRANE.get(),
                (crane, side) -> crane.waterIn());
        event.registerBlock(Capabilities.FluidHandler.BLOCK,
                (level, pos, state, be, side) -> WaterTankBlock.bunkerOf(level, pos).map(BunkerBlockEntity::waterIn).orElse(null),
                RailboundBlocks.WATER_TANK.get());
        dev.railbound.cargo.CargoAccess.registerCapabilities(event);
    }

    private static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new TrainsetDesignManager());
    }
}
