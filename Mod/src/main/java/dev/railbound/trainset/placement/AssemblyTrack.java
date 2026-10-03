package dev.railbound.trainset.placement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** A station's assembly track: offset 0 is firstTrack, offsets grow in direction (away from the station). */
public record AssemblyTrack(BlockPos firstTrack, Direction direction, int length) {
    public BlockPos track(int offset) {
        return firstTrack.relative(direction, offset);
    }
}
