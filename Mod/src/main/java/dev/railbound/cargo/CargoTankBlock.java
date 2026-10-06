package dev.railbound.cargo;

import dev.railbound.carriage.CarriagePartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A tank wagon's tank, a solid block inside its body. Players open it with sneak and an empty hand anywhere on the
 * wagon (see {@link CargoAccess}); picking a wagon with fluid in it up is refused.
 */
public class CargoTankBlock extends CarriagePartBlock implements EntityBlock {
    public CargoTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CargoTankBlockEntity(pos, state);
    }
}
