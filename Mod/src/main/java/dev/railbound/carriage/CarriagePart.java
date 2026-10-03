package dev.railbound.carriage;

import net.minecraft.world.level.block.state.BlockState;

/**
 * Marks every hidden carriage block. Most extend {@link CarriagePartBlock}; seats extend Create's seat block
 * instead (Create's seat entity only stays on its own seat blocks) and share behaviour through {@link CarriageParts}.
 */
public interface CarriagePart {

    static boolean is(BlockState state) {
        return state.getBlock() instanceof CarriagePart;
    }
}
