package dev.railbound.steam;

import dev.railbound.carriage.CarriagePartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;

/**
 * The cab backhead with the firebox door: what a player in the cab faces. Coal in hand goes into the loco's bunker,
 * an empty hand opens the bunker screen. On a train, {@link FireboxMovingInteraction} does the same.
 */
public class FireboxBlock extends CarriagePartBlock {
    public static final Component TITLE = Component.translatable("block.railbound.loco_bunker");

    public FireboxBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!CoalSlots.isCoal(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        Optional<BunkerBlockEntity> bunker = level.isClientSide ? Optional.empty() : LocoAccess.bunkerOf(level, pos);
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        return bunker.map(b -> LocoAccess.feedCoal(level, pos, b, player, hand, stack)).orElse(ItemInteractionResult.CONSUME);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            LocoAccess.bunkerOf(level, pos).ifPresent(bunker -> LocoAccess.openBunker(serverPlayer, bunker, TITLE));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
