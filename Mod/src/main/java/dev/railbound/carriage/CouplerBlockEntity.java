package dev.railbound.carriage;

import dev.railbound.registry.RailboundBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Holds nothing: it is there so the coupler block can be drawn by a renderer (see {@link CouplerBlock}). */
public class CouplerBlockEntity extends BlockEntity {
    public CouplerBlockEntity(BlockPos pos, BlockState state) {
        super(RailboundBlockEntities.COUPLER.get(), pos, state);
    }
}
