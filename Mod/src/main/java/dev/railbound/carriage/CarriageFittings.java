package dev.railbound.carriage;

import dev.railbound.Railbound;
import dev.railbound.carriage.CarriageRemover.FoundCarriage;
import dev.railbound.trainset.design.HiddenPart;
import dev.railbound.trainset.design.LayoutCell;
import dev.railbound.trainset.design.ParsedDesign;
import dev.railbound.trainset.design.PartType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.Optional;

/**
 * Fittings (Create's interfaces) placed against a carriage's outer skin go into the wall cell instead of in
 * front of it, so they sit flush and travel with the carriage. Breaking one puts the wall back.
 */
public final class CarriageFittings {
    public static final TagKey<Block> TAG = TagKey.create(Registries.BLOCK, Railbound.rl("carriage_fittings"));

    private CarriageFittings() {}

    public static boolean is(BlockState state) {
        return state.is(TAG);
    }

    /** Inverse of {@link CarriageFootprint}: the design position of a world block, given the carriage's anchor. */
    public static BlockPos toLocal(BlockPos world, BlockPos anchorWorld, Direction facing, BlockPos anchorLocal) {
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        int dx = world.getX() - anchorWorld.getX();
        int dz = world.getZ() - anchorWorld.getZ();
        int alongRight = dx * right.getStepX() + dz * right.getStepZ();
        int alongBack = dx * back.getStepX() + dz * back.getStepZ();
        return new BlockPos(anchorLocal.getX() + alongRight,
                anchorLocal.getY() + world.getY() - anchorWorld.getY(),
                anchorLocal.getZ() + alongBack);
    }

    /**
     * A fitting may take a frame cell's place when it was clicked from outside the carriage (the target cell lies
     * outside the design's box) on any face but the bottom. Seats, doors and the anchor are never replaced.
     */
    public static boolean canFitInto(ParsedDesign design, BlockPos clickedLocal, BlockPos targetLocal, Direction face) {
        if (face == Direction.DOWN) {
            return false;
        }
        boolean frame = design.partAt(clickedLocal).map(part -> part.type() == PartType.FRAME).orElse(false);
        return frame && !insideBox(design, targetLocal);
    }

    private static boolean insideBox(ParsedDesign design, BlockPos local) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (LayoutCell cell : design.cells()) {
            BlockPos pos = cell.pos();
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        return local.getX() >= minX && local.getX() <= maxX
                && local.getY() >= minY && local.getY() <= maxY
                && local.getZ() >= minZ && local.getZ() <= maxZ;
    }

    /** Server side. The client predicts a normal placement; vanilla resyncs both cells after this runs. */
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        Direction face = event.getFace();
        BlockPos clicked = event.getPos();
        if (level.isClientSide || face == null || !(stack.getItem() instanceof BlockItem blockItem)
                || !is(blockItem.getBlock().defaultBlockState())
                || !(level.getBlockState(clicked).getBlock() instanceof FrameBlock)) {
            return;
        }
        Optional<FoundCarriage> carriage = CarriageRemover.findCarriage(level, clicked);
        if (carriage.isEmpty()) {
            return;
        }
        FoundCarriage found = carriage.get();
        BlockPos clickedLocal = toLocal(clicked, found.anchor(), found.facing(), found.design().anchor());
        BlockPos targetLocal = toLocal(clicked.relative(face), found.anchor(), found.facing(), found.design().anchor());
        if (!canFitInto(found.design(), clickedLocal, targetLocal, face)
                || !level.mayInteract(player, clicked) || !player.mayUseItemAt(clicked, face, stack)) {
            return;
        }
        BlockState state = blockItem.getBlock().getStateForPlacement(
                new BlockPlaceContext(player, event.getHand(), stack, event.getHitVec()));
        if (state == null) {
            return;
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            state = state.setValue(BlockStateProperties.FACING, face);
        }

        CarriageRemover.replacePart(level, clicked, state);
        state.getBlock().setPlacedBy(level, clicked, state, player, stack);
        SoundType sound = state.getSoundType(level, clicked, player);
        level.playSound(null, clicked, sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1) / 2, sound.getPitch() * 0.8f);
        level.gameEvent(GameEvent.BLOCK_PLACE, clicked, GameEvent.Context.of(player, state));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /** Breaking a fitting that sits in a carriage cell drops it and puts the frame part back. */
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide || !is(event.getState())) {
            return;
        }
        BlockPos pos = event.getPos();
        for (Direction direction : Direction.values()) {
            BlockPos next = pos.relative(direction);
            if (!CarriagePart.is(level.getBlockState(next))) {
                continue;
            }
            Optional<FoundCarriage> carriage = CarriageRemover.findCarriage(level, next);
            if (carriage.isEmpty()) {
                continue;
            }
            FoundCarriage found = carriage.get();
            BlockPos local = toLocal(pos, found.anchor(), found.facing(), found.design().anchor());
            Optional<HiddenPart> part = found.design().partAt(local);
            if (part.isEmpty() || part.get().type() != PartType.FRAME) {
                return;
            }
            event.setCanceled(true);
            Player player = event.getPlayer();
            BlockState state = event.getState();
            state.getBlock().playerWillDestroy(level, pos, state, player);
            if (!player.isCreative()) {
                Block.dropResources(state, level, pos, level.getBlockEntity(pos), player, player.getMainHandItem());
            }
            level.setBlock(pos, CarriageBlocks.stateFor(found.design(), part.get(), found.facing(), local), Block.UPDATE_ALL);
            return;
        }
    }
}
