package dev.railbound.carriage;

import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import dev.railbound.trainset.item.TrainsetItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * A carriage's bogeys are ordinary Create bogeys with their own loot (Railway Casing). Breaking one must
 * remove the whole carriage for one trainset item instead, and explosions leave them to the carriage cascade.
 */
public final class CarriageProtection {
    private CarriageProtection() {}

    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide) {
            return;
        }
        BlockPos pos = event.getPos();
        Optional<BlockPos> part = carriagePartBeside(level, pos);
        if (part.isEmpty() || !(level.getBlockState(pos).getBlock() instanceof AbstractBogeyBlock<?>)) {
            return;
        }
        event.setCanceled(true);
        Player player = event.getPlayer();
        CarriageRemover.removeCarriage(level, part.get()).ifPresent(id -> {
            if (player == null || !player.isCreative()) {
                Block.popResource(level, pos, TrainsetItem.of(id));
            }
        });
    }

    public static void onDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        removeCarriageBogeys(event.getAffectedBlocks(), pos -> isCarriageBogey(level, pos));
    }

    /** Takes carriage bogeys out of an explosion; any part it hits removes them without loot. */
    static void removeCarriageBogeys(List<BlockPos> affected, Predicate<BlockPos> isCarriageBogey) {
        affected.removeIf(isCarriageBogey);
    }

    private static boolean isCarriageBogey(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof AbstractBogeyBlock<?>
                && carriagePartBeside(level, pos).isPresent();
    }

    private static Optional<BlockPos> carriagePartBeside(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos next = pos.relative(direction);
            if (level.getBlockState(next).getBlock() instanceof CarriagePartBlock) {
                return Optional.of(next);
            }
        }
        return Optional.empty();
    }
}
