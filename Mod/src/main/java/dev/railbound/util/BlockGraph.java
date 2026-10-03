package dev.railbound.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public final class BlockGraph {
    private BlockGraph() {}

    /** Positions reachable from start through face-adjacent members, at most limit of them. */
    public static Set<BlockPos> connected(BlockPos start, Predicate<BlockPos> member, int limit) {
        Set<BlockPos> found = new HashSet<>();
        if (!member.test(start)) {
            return found;
        }
        Deque<BlockPos> frontier = new ArrayDeque<>();
        frontier.add(start.immutable());
        found.add(start.immutable());
        while (!frontier.isEmpty() && found.size() < limit) {
            BlockPos current = frontier.poll();
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (found.size() >= limit) {
                    break;
                }
                if (!found.contains(next) && member.test(next)) {
                    found.add(next);
                    frontier.add(next);
                }
            }
        }
        return found;
    }
}
