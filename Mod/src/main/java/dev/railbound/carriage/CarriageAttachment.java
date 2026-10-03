package dev.railbound.carriage;

import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Optional;

/**
 * Glues a carriage together for Create's assembly: parts stick to neighbouring parts, and parts and bogeys
 * stick to each other on every side (layer 0 sits beside the bogey). Create's bogeys are only sticky front/back.
 * Fittings (interfaces) stick to parts too, unless their front faces the part: that is a station-side interface.
 */
public final class CarriageAttachment {
    public enum Kind { PART, BOGEY, FITTING, OTHER }

    private CarriageAttachment() {}

    public static void register() {
        BlockMovementChecks.registerAttachedCheck((state, level, pos, direction) -> {
            BlockState neighbour = level.getBlockState(pos.relative(direction));
            return evaluate(kindOf(state, direction), kindOf(neighbour, direction.getOpposite()));
        });
        BlockMovementChecks.registerMovementNecessaryCheck((state, level, pos) -> movementNecessary(CarriagePart.is(state)));
    }

    /** Create skips blocks without collision (upper door placeholders); every part must travel with the carriage. */
    public static CheckResult movementNecessary(boolean isPart) {
        return isPart ? CheckResult.SUCCESS : CheckResult.PASS;
    }

    public static CheckResult evaluate(Kind self, Kind neighbour) {
        boolean glued = switch (self) {
            case PART -> neighbour == Kind.PART || neighbour == Kind.BOGEY || neighbour == Kind.FITTING;
            case BOGEY, FITTING -> neighbour == Kind.PART;
            case OTHER -> false;
        };
        return glued ? CheckResult.SUCCESS : CheckResult.PASS;
    }

    /** What a block counts as when Create asks whether it sticks to the block in direction towardsOther. */
    public static Kind kind(boolean part, boolean bogey, boolean fitting, Optional<Direction> front, Direction towardsOther) {
        if (part) {
            return Kind.PART;
        }
        if (bogey) {
            return Kind.BOGEY;
        }
        if (fitting && front.map(f -> f != towardsOther).orElse(true)) {
            return Kind.FITTING;
        }
        return Kind.OTHER;
    }

    private static Kind kindOf(BlockState state, Direction towardsOther) {
        return kind(CarriagePart.is(state), state.getBlock() instanceof AbstractBogeyBlock<?>, CarriageFittings.is(state),
                state.getOptionalValue(BlockStateProperties.FACING), towardsOther);
    }
}
