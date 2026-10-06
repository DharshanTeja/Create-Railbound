package dev.railbound.cargo;

import dev.railbound.carriage.CarriagePartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A goods wagon's hold, a solid block inside its body. Players open it with sneak and an empty hand anywhere on the
 * wagon (see {@link CargoAccess}). If the wagon is destroyed (an explosion, a command) the load spills out rather
 * than vanish; picking a loaded wagon up is refused.
 */
public class CargoHoldBlock extends CarriagePartBlock implements EntityBlock {
    public CargoHoldBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !movedByPiston && !newState.is(this) && level.getBlockEntity(pos) instanceof CargoHoldBlockEntity hold) {
            for (int slot = 0; slot < hold.items().getSlots(); slot++) {
                Block.popResource(level, pos, hold.items().getStackInSlot(slot).copy());
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CargoHoldBlockEntity(pos, state);
    }
}
