package dev.railbound.client;

import com.simibubi.create.content.trains.bogey.BogeyBlockEntityRenderer;
import com.simibubi.create.content.trains.bogey.BogeyBlockEntityVisual;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import dev.railbound.Railbound;
import dev.railbound.registry.RailboundBlockEntities;
import dev.railbound.registry.RailboundItems;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Client-only setup: carriage rendering, trainset models and the 3D trainset item. */
@Mod(value = Railbound.MOD_ID, dist = Dist.CLIENT)
public final class RailboundClient {

    public RailboundClient(IEventBus modBus, ModContainer container) {
        modBus.addListener(TrainsetModels::register);
        modBus.addListener(TrainsetItemModels::registerAdditional);
        modBus.addListener(TrainsetItemModels::wrap);
        modBus.addListener(RailboundClient::onRegisterRenderers);
        modBus.addListener(RailboundClient::onClientSetup);
        modBus.addListener(RailboundClient::onRegisterClientExtensions);
    }

    private static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        TrainsetItemRenderer renderer = new TrainsetItemRenderer();
        event.registerItem(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        }, RailboundItems.TRAINSET.get());
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(RailboundBlockEntities.ANCHOR.get(), AnchorRenderer::new);
        event.registerBlockEntityRenderer(RailboundBlockEntities.COACH_BOGEY.get(), BogeyBlockEntityRenderer::new);
    }

    /** Our bogey block entity draws like Create's own: its style's Flywheel visual, or the renderer without Flywheel. */
    private static void onClientSetup(FMLClientSetupEvent event) {
        SimpleBlockEntityVisualizer.builder(RailboundBlockEntities.COACH_BOGEY.get())
                .factory(BogeyBlockEntityVisual::new)
                .skipVanillaRender(be -> true)
                .apply();
    }
}
