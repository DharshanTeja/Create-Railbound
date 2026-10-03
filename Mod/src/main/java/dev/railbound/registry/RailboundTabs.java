package dev.railbound.registry;

import dev.railbound.Railbound;
import dev.railbound.trainset.item.TrainsetItem;
import dev.railbound.trainset.load.TrainsetDesigns;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RailboundTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Railbound.MOD_ID);

    /** One stack per loaded design. Contents refresh when the creative screen rebuilds (e.g. on rejoin). */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.railbound"))
                    .icon(() -> new ItemStack(RailboundItems.TRAINSET.get()))
                    .displayItems((parameters, output) ->
                            TrainsetDesigns.all().keySet().forEach(id -> output.accept(TrainsetItem.of(id))))
                    .build());

    /** Makes the creative screen rebuild its tabs and search the next time it opens, after designs change. */
    public static void refreshOnDesignChange() {
        TrainsetDesigns.addChangeListener(() -> CreativeModeTabs.CACHED_PARAMETERS = null);
    }

    private RailboundTabs() {}
}
