package dev.railbound.coupling;

import net.createmod.catnip.data.Couple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The rules for a carriage that runs reversed in its train, as a loco backed onto a train for push-pull does. Create
 * builds every carriage facing its train's way; a reversed one keeps its own front and back (its controls, its seats,
 * its first bogey) but they point the other way along the train.
 */
public final class CarriageFlip {
    private CarriageFlip() {}

    /** A train turned round: the gaps between its carriages run the other way. */
    public static List<Integer> reversedSpacing(List<Integer> spacing) {
        List<Integer> out = new ArrayList<>(spacing);
        Collections.reverse(out);
        return out;
    }

    /**
     * A carriage's two sides (its own front and back: controls, conductors) as the train's (forwards, backwards):
     * the same for a carriage facing the train's way, swapped for a reversed one.
     */
    public static Couple<Boolean> forTrain(boolean ownFront, boolean ownBack, boolean reversed) {
        return reversed ? Couple.create(ownBack, ownFront) : Couple.create(ownFront, ownBack);
    }

    /**
     * Where along the station track a carriage is put back as blocks. Create counts from its train-leading bogey, or
     * from the far bogey when the train stands backwards; a reversed carriage's own first bogey is the other one.
     */
    public static int disassemblyDistance(int createDistance, int bogeySpacing, boolean trainBackwards, boolean reversed) {
        if (!reversed) {
            return createDistance;
        }
        return trainBackwards ? createDistance - bogeySpacing : createDistance + bogeySpacing;
    }

    /**
     * Whether the train can be driven one way: by a driver whose seat faces that way, as in Create, or (one driver
     * shared by the whole train) by a driver anywhere while some carriage has controls facing that way.
     */
    public static boolean canDrive(boolean driverFacingThisWay, boolean anyDriver, boolean controlsFacingThisWay) {
        return driverFacingThisWay || anyDriver && controlsFacingThisWay;
    }
}
