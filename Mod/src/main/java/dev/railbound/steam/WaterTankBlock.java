package dev.railbound.steam;

import dev.railbound.carriage.CarriageFootprint;
import dev.railbound.carriage.CarriagePartBlock;
import dev.railbound.carriage.CarriageRemover;
import dev.railbound.trainset.design.PartType;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.Optional;

/**
 * A steam loco's water tank. The water itself is kept with the boiler in the bunker; pipes and buckets on the tank
 * of a standing loco fill it there.
 */
public class WaterTankBlock extends CarriagePartBlock {
    public static final int BUCKET = 1000;

    public WaterTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    /** The bunker of the loco this tank belongs to. */
    public static Optional<BunkerBlockEntity> bunkerOf(Level level, BlockPos tankPos) {
        return LocoAccess.bunkerOf(level, tankPos);
    }

    /** Pours a water bucket into the tank of a standing loco. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.WATER_BUCKET)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        Optional<BunkerBlockEntity> bunker = bunkerOf(level, pos);
        if (bunker.isEmpty() || !pourBucket(bunker.get().waterIn())) {
            return ItemInteractionResult.CONSUME;
        }
        if (!player.getAbilities().instabuild) {
            player.setItemInHand(hand, new ItemStack(Items.BUCKET));
        }
        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1, 1);
        return ItemInteractionResult.CONSUME;
    }

    /** Fills a whole bucket's worth, or nothing if the tank has no room for it. */
    public static boolean pourBucket(IFluidHandler tank) {
        FluidStack bucket = new FluidStack(Fluids.WATER, BUCKET);
        if (tank.fill(bucket, IFluidHandler.FluidAction.SIMULATE) < BUCKET) {
            return false;
        }
        tank.fill(bucket, IFluidHandler.FluidAction.EXECUTE);
        return true;
    }
}
