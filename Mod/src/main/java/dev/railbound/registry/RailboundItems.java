package dev.railbound.registry;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import dev.railbound.Railbound;
import dev.railbound.trainset.item.TrainsetItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public final class RailboundItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Railbound.MOD_ID);

    public static final DeferredItem<TrainsetItem> TRAINSET =
            ITEMS.register("trainset", () -> new TrainsetItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<BlockItem> WATER_CRANE = ITEMS.registerSimpleBlockItem(RailboundBlocks.WATER_CRANE);
    public static final DeferredItem<BlockItem> COUPLER = ITEMS.registerSimpleBlockItem(RailboundBlocks.COUPLER);

    /** Crafting parts the trainset recipes are built from. */
    public static final DeferredItem<Item> STEAM_ENGINE_UNIT = part("steam_engine_unit");
    public static final DeferredItem<Item> COACH_BOGEY_UNIT = part("coach_bogey_unit");
    public static final DeferredItem<Item> FREIGHT_BOGEY_UNIT = part("freight_bogey_unit");
    public static final DeferredItem<Item> STEAM_TRUCK_UNIT = part("steam_truck_unit");
    /** Also the buffer block: placing it puts a buffer on a carriage end. */
    public static final DeferredItem<net.minecraft.world.item.BlockItem> ANTI_CLIMBER =
            ITEMS.registerSimpleBlockItem("anti_climber", RailboundBlocks.BUFFER, new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> CARRIAGE_SHELL = part("carriage_shell");
    public static final DeferredItem<Item> WAGON_UNDERFRAME = part("wagon_underframe");

    /** Create's half-built stand-ins for the sequenced assemblies: one per part (trainsets are laid out in mechanical crafters). */
    public static final List<String> UNFINISHED = List.of(
            "steam_engine_unit", "coach_bogey_unit", "freight_bogey_unit", "steam_truck_unit",
            "anti_climber", "carriage_shell", "wagon_underframe");

    static {
        for (String result : UNFINISHED) {
            ITEMS.register("incomplete_" + result, () -> new SequencedAssemblyItem(new Item.Properties()));
        }
    }

    private static DeferredItem<Item> part(String id) {
        return ITEMS.register(id, () -> new Item(new Item.Properties().stacksTo(16)));
    }

    private RailboundItems() {}
}
