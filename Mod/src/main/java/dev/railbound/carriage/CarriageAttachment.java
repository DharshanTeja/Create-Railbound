package dev.railbound.carriage;

import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Glues a carriage together for Create's assembly: parts stick to neighbouring parts, and parts and bogeys
 * stick to each other on every side (layer 0 sits beside the bogey). Create's bogeys are only sticky front/back.
 */
public final class CarriageAttachment {
    private CarriageAttachment() {}

    public static void register() {
        BlockMovementChecks.registerAttachedCheck((state, level, pos, direction) -> {
            BlockState neighbour = level.getBlockState(pos.relative(direction));
            return evaluate(isPart(state), isBogey(state), isPart(neighbour), isBogey(neighbour), direction);
        });
        BlockMovementChecks.registerMovementNecessaryCheck((state, level, pos) -> movementNecessary(isPart(state)));
    }

    /** Create skips blocks without collision (upper door placeholders); every part must travel with the carriage. */
    public static CheckResult movementNecessary(boolean isPart) {
        return isPart ? CheckResult.SUCCESS : CheckResult.PASS;
    }

    public static CheckResult evaluate(boolean selfIsPart, boolean selfIsBogey,
                                       boolean neighbourIsPart, boolean neighbourIsBogey, Direction towards) {
        if (selfIsPart && (neighbourIsPart || neighbourIsBogey)) {
            return CheckResult.SUCCESS;
        }
        if (selfIsBogey && neighbourIsPart) {
            return CheckResult.SUCCESS;
        }
        return CheckResult.PASS;
    }

    private static boolean isPart(BlockState state) {
        return state.getBlock() instanceof CarriagePartBlock;
    }

    private static boolean isBogey(BlockState state) {
        return state.getBlock() instanceof AbstractBogeyBlock<?>;
    }
}
