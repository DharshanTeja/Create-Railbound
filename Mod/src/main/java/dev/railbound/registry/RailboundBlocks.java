package dev.railbound.registry;

import dev.railbound.Railbound;
import dev.railbound.carriage.AnchorBlock;
import dev.railbound.carriage.CarriagePartBlock;
import dev.railbound.carriage.DoorPartBlock;
import dev.railbound.carriage.FrameBlock;
import dev.railbound.carriage.PlaceholderPartBlock;
import dev.railbound.carriage.SeatPartBlock;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Hidden carriage blocks. They have no block items: only trainset items place them. */
public final class RailboundBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Railbound.MOD_ID);

    public static final DeferredBlock<FrameBlock> FRAME =
            BLOCKS.register("carriage_frame", () -> new FrameBlock(CarriagePartBlock.partProperties()));
    public static final DeferredBlock<PlaceholderPartBlock> PLACEHOLDER =
            BLOCKS.register("carriage_placeholder", () -> new PlaceholderPartBlock(CarriagePartBlock.partProperties()));
    public static final DeferredBlock<AnchorBlock> ANCHOR =
            BLOCKS.register("carriage_anchor", () -> new AnchorBlock(CarriagePartBlock.partProperties()));
    public static final DeferredBlock<SeatPartBlock> SEAT =
            BLOCKS.register("carriage_seat", () -> new SeatPartBlock(CarriagePartBlock.partProperties()));
    public static final DeferredBlock<DoorPartBlock> DOOR =
            BLOCKS.register("carriage_door", () -> new DoorPartBlock(CarriagePartBlock.partProperties()));

    private RailboundBlocks() {}
}
