package dev.railbound.registry;

import dev.railbound.Railbound;
import dev.railbound.trainset.item.TrainsetItem;
import dev.railbound.trainset.load.TrainsetDesigns;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RailboundTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Railbound.MOD_ID);

    /** The tab shows the standard coach (its 3D model); a plain trainset would show the minecart icon. */
    private static final ResourceLocation ICON_DESIGN = Railbound.rl("coach_standard");

    /** One stack per loaded design. Contents refresh when the creative screen rebuilds (e.g. on rejoin). */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.railbound"))
                    .icon(() -> TrainsetItem.of(ICON_DESIGN))
                    .displayItems((parameters, output) ->
                    {
                        TrainsetDesigns.all().keySet().forEach(id -> output.accept(TrainsetItem.of(id)));
                        output.accept(RailboundItems.WATER_CRANE.get());
                    })
                    .build());

    /** Makes the creative screen rebuild its tabs and search the next time it opens, after designs change. */
    public static void refreshOnDesignChange() {
        TrainsetDesigns.addChangeListener(() -> CreativeModeTabs.CACHED_PARAMETERS = null);
    }

    private RailboundTabs() {}
}
