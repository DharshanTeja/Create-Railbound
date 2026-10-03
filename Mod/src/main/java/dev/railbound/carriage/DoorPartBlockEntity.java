package dev.railbound.carriage;

import dev.railbound.registry.RailboundBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client-side slide progress of a carriage door (0 closed, 1 open). Parked doors tick themselves; on a moving
 * train {@link CarriageDoorMovementBehaviour} ticks the copy inside the contraption.
 */
public class DoorPartBlockEntity extends BlockEntity {
    /** Progress per tick: a door takes half a second to open or close. */
    static final float SPEED = 0.1f;
    private float progress;
    private float lastProgress;

    public DoorPartBlockEntity(BlockPos pos, BlockState state) {
        super(RailboundBlockEntities.DOOR.get(), pos, state);
        progress = lastProgress = state.getValue(DoorPartBlock.OPEN) ? 1 : 0;
    }

    /** Moves one tick towards open or closed; true on the tick the door finishes closing. */
    public boolean animate(boolean open) {
        lastProgress = progress;
        float target = open ? 1 : 0;
        progress = progress < target ? Math.min(target, progress + SPEED) : Math.max(target, progress - SPEED);
        return !open && lastProgress > 0 && progress == 0;
    }

    public float progress(float partialTicks) {
        return Mth.lerp(partialTicks, lastProgress, progress);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, DoorPartBlockEntity door) {
        if (door.animate(state.getValue(DoorPartBlock.OPEN))) {
            level.playLocalSound(pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.25f, 1, false);
        }
    }
}
