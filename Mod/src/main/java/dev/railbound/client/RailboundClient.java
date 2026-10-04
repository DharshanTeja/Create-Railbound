package dev.railbound.client;

import dev.railbound.registry.RailboundMenus;
import dev.railbound.registry.RailboundParticles;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
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
        modBus.addListener((net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional event) -> {
            WaterCraneRenderer.MODELS.forEach(event::register);
            CouplingRenderer.MODELS.forEach(event::register);
            event.register(CouplerBlockRenderer.MODEL);
        });
        modBus.addListener(TrainsetItemModels::registerAdditional);
        modBus.addListener(TrainsetItemModels::wrap);
        modBus.addListener(RailboundClient::onRegisterRenderers);
        modBus.addListener(RailboundClient::onClientSetup);
        modBus.addListener(RailboundClient::onRegisterClientExtensions);
        modBus.addListener(RailboundClient::onRegisterParticles);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(CouplingRenderer::render);
        // entity ids are reused in the next world: forget the last one's loco gauges
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) ->
                        dev.railbound.steam.LocoGaugeCache.clear());
        modBus.addListener(RailboundClient::onRegisterScreens);
        modBus.addListener(RailboundClient::onRegisterGuiLayers);
    }

    private static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(RailboundMenus.BUNKER.get(), BunkerScreen::new);
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, Railbound.rl("steam_gauges"), SteamHud::render);
    }

    private static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        event.registerSpecial(RailboundParticles.NONE, (type, level, x, y, z, dx, dy, dz) -> null);
        event.registerSpriteSet(RailboundParticles.LOCO_SMOKE, sprites -> new LocoPuffParticle.Provider(sprites, 0.2f, 90, 0.6f, 3.2f));
        event.registerSpriteSet(RailboundParticles.LOCO_SMOKE_DARK, sprites -> new LocoPuffParticle.Provider(sprites, 0.08f, 110, 0.7f, 3.6f));
        event.registerSpriteSet(RailboundParticles.LOCO_STEAM, sprites -> new LocoPuffParticle.Provider(sprites, 1.0f, 30, 0.5f, 2.0f));
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
        event.registerBlockEntityRenderer(RailboundBlockEntities.STEAM_TRUCK_BOGEY.get(), BogeyBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(RailboundBlockEntities.WATER_CRANE.get(), WaterCraneRenderer::new);
        event.registerBlockEntityRenderer(RailboundBlockEntities.COUPLER.get(), CouplerBlockRenderer::new);
    }

    /** Our bogey block entities draw like Create's own: their style's Flywheel visual, or the renderer without Flywheel. */
    private static void onClientSetup(FMLClientSetupEvent event) {
        SimpleBlockEntityVisualizer.builder(RailboundBlockEntities.COACH_BOGEY.get())
                .factory(BogeyBlockEntityVisual::new)
                .skipVanillaRender(be -> true)
                .apply();
        SimpleBlockEntityVisualizer.builder(RailboundBlockEntities.STEAM_TRUCK_BOGEY.get())
                .factory(BogeyBlockEntityVisual::new)
                .skipVanillaRender(be -> true)
                .apply();
    }
}
