package dev.railbound.carriage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Where the knuckle couplers between neighbouring carriages go, in world space, from the carriage ends. Each knuckle
 * keeps its own length and swivels at its carriage end to point at the middle of the gap: on straight track the two
 * heads lock together there; where a tight curve swings the ends further apart, a gap shows between them.
 */
public final class CouplingGeometry {
    private CouplingGeometry() {}

    /** A carriage end: its centre at coupler height, and its outward, right and up directions (unit vectors). */
    public record End(Vec3 centre, Vec3 outward, Vec3 right, Vec3 up) {
        public Vec3 at(double side, double height) {
            return centre.add(right.scale(side)).add(up.scale(height));
        }
    }

    /** The middle of the gap between two ends, where locked knuckles meet. */
    public static Vec3 meet(End a, End b) {
        return a.centre().add(b.centre()).scale(0.5);
    }

    /** Degrees a knuckle can swivel either way from straight out, as far as a draft gear lets a real one. */
    public static final double MAX_SWIVEL = 30;

    /**
     * Where a knuckle's coupling face is: its length from the end, towards the meeting point but never past it, and
     * turned no more than {@link #MAX_SWIVEL} from straight out.
     */
    public static Vec3 head(End end, Vec3 meet, double reach) {
        Vec3 along = meet.subtract(end.centre());
        double length = along.length();
        if (length < 1e-9) {
            return meet;
        }
        Vec3 direction = along.scale(1 / length);
        double cos = direction.dot(end.outward());
        double limit = Math.cos(Math.toRadians(MAX_SWIVEL));
        if (cos < limit) {
            // keep the sideways lean, cut back to the limit: outward cos + sideways sin
            Vec3 sideways = direction.subtract(end.outward().scale(cos));
            sideways = sideways.lengthSqr() < 1e-12 ? end.right() : sideways.normalize();
            direction = end.outward().scale(limit).add(sideways.scale(Math.sin(Math.toRadians(MAX_SWIVEL))));
            return end.centre().add(direction.scale(reach));
        }
        return length <= reach ? meet : end.centre().add(direction.scale(reach));
    }

    /**
     * The frame a fraction t of the way from a to b (0 is a, 1 is b turned to face the same way as a), for the bellows'
     * folds: it slides along between the centres and turns from a's directions to b's.
     */
    public static End between(End a, End b, double t) {
        Vec3 centre = a.centre().add(b.centre().subtract(a.centre()).scale(t));
        Vec3 outward = lerp(a.outward(), b.outward().scale(-1), t).normalize();
        Vec3 up = lerp(a.up(), b.up(), t).normalize();
        Vec3 right = outward.cross(up);
        // keep right on a's side even if a's right is not outward x up
        if (right.dot(a.right()) < 0) {
            right = right.scale(-1);
        }
        right = right.normalize();
        up = right.cross(outward).scale(a.right().cross(a.outward()).dot(a.up()) < 0 ? -1 : 1).normalize();
        return new End(centre, outward, right, up);
    }

    private static Vec3 lerp(Vec3 from, Vec3 to, double t) {
        return from.add(to.subtract(from).scale(t));
    }

    /** Where a knuckle's coupling face is at a train's free end: straight out. */
    public static Vec3 rest(End end, double reach) {
        return end.centre().add(end.outward().scale(reach));
    }

    /**
     * Of two neighbouring carriages' coupler ends, the pair that join: {index in a, index in b}. Only ends pointing out
     * towards the other carriage's middle can join, however far a tight curve swings them apart; of those, the
     * closest pair. None when either carriage has no coupler on the side facing the other.
     */
    public static Optional<int[]> facing(List<End> a, Vec3 aMiddle, List<End> b, Vec3 bMiddle) {
        int[] best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < a.size(); i++) {
            if (!pointsAt(a.get(i), bMiddle)) {
                continue;
            }
            for (int j = 0; j < b.size(); j++) {
                double distance = a.get(i).centre().distanceToSqr(b.get(j).centre());
                if (pointsAt(b.get(j), aMiddle) && distance < bestDistance) {
                    bestDistance = distance;
                    best = new int[] {i, j};
                }
            }
        }
        return Optional.ofNullable(best);
    }

    private static boolean pointsAt(End end, Vec3 target) {
        return end.outward().dot(target.subtract(end.centre())) > 0;
    }

    /** Where a coupler block's knuckle starts (carriage coordinates): the middle of the block's back face. */
    public static Vec3 blockEnd(BlockPos pos, Direction facing) {
        return Vec3.atCenterOf(pos).subtract(Vec3.atLowerCornerOf(facing.getNormal()).scale(0.5));
    }
}
