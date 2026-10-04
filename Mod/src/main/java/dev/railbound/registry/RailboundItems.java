package dev.railbound.registry;

import dev.railbound.Railbound;
import dev.railbound.trainset.item.TrainsetItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RailboundItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Railbound.MOD_ID);

    public static final DeferredItem<TrainsetItem> TRAINSET =
            ITEMS.register("trainset", () -> new TrainsetItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<BlockItem> WATER_CRANE = ITEMS.registerSimpleBlockItem(RailboundBlocks.WATER_CRANE);
    public static final DeferredItem<BlockItem> COUPLER = ITEMS.registerSimpleBlockItem(RailboundBlocks.COUPLER);

    private RailboundItems() {}
}
