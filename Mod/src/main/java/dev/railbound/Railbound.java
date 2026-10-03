package dev.railbound;

import com.mojang.logging.LogUtils;
import dev.railbound.network.RailboundNetwork;
import dev.railbound.registry.RailboundComponents;
import dev.railbound.registry.RailboundItems;
import dev.railbound.registry.RailboundTabs;
import dev.railbound.trainset.load.TrainsetDesignManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;

@Mod(Railbound.MOD_ID)
public final class Railbound {
    public static final String MOD_ID = "railbound";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Railbound(IEventBus modBus, ModContainer container) {
        LOGGER.info("Create: Railbound loading");
        RailboundComponents.COMPONENTS.register(modBus);
        RailboundItems.ITEMS.register(modBus);
        RailboundTabs.TABS.register(modBus);
        RailboundTabs.refreshOnDesignChange();
        modBus.addListener(RailboundNetwork::register);
        NeoForge.EVENT_BUS.addListener(Railbound::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(RailboundNetwork::onDatapackSync);
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new TrainsetDesignManager());
    }
}
