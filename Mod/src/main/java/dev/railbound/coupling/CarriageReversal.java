package dev.railbound.coupling;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import dev.railbound.carriage.CarriageCouplers;
import dev.railbound.mixin.CarriageBogeyAccessor;
import dev.railbound.mixin.TrainAccessor;
import net.createmod.catnip.data.Couple;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Carriages that run reversed in their train, so trains can join front to front or rear to rear (a loco backed onto a
 * train for push-pull). Create keeps every carriage facing its train's way; turning a whole train round flips each of
 * its carriages, and a mark in the bogey data (saved and synced with the train) remembers it. Mixins then place,
 * draw and drive such carriages the other way round; Create's own code is never changed.
 */
public final class CarriageReversal {
    static final String MARK = "RailboundReversed";

    private CarriageReversal() {}

    public static boolean marked(@Nullable CarriageBogey bogey) {
        return bogey != null && bogey.bogeyData != null && bogey.bogeyData.getBoolean(MARK);
    }

    /** Whether a carriage runs reversed: its own front faces its train's back. */
    public static boolean reversed(Carriage carriage) {
        return marked(carriage.leadingBogey());
    }

    /** Bogey data without the reversed mark (for a bogey going back to a block). */
    public static CompoundTag unmarked(@Nullable CompoundTag data) {
        if (data == null || !data.contains(MARK)) {
            return data;
        }
        CompoundTag copy = data.copy();
        copy.remove(MARK);
        return copy;
    }

    /**
     * Turns a train round where it stands: its carriages, and the gaps between them, run the other way, and each
     * carriage is flipped. Its bogeys and wheel points swap to keep train order, while each bogey keeps whether it is
     * its carriage's own first one. The same on the server and, mirrored, on each client.
     */
    public static void reverse(Train train) {
        List<Carriage> carriages = new ArrayList<>(train.carriages);
        Collections.reverse(carriages);
        train.carriages = carriages;
        train.carriageSpacing = new ArrayList<>(CarriageFlip.reversedSpacing(train.carriageSpacing));
        ((TrainAccessor) train).setStress(new double[train.carriageSpacing.size()]);
        for (Carriage carriage : carriages) {
            boolean wasReversed = reversed(carriage);
            if (carriage.isOnTwoBogeys()) {
                carriage.bogeys = Couple.create(carriage.bogeys.getSecond(), carriage.bogeys.getFirst());
            }
            for (CarriageBogey bogey : carriage.bogeys) {
                if (bogey == null) {
                    continue;
                }
                CarriageBogeyAccessor access = (CarriageBogeyAccessor) bogey;
                Couple<TravellingPoint> points = access.getPoints();
                access.setPoints(Couple.create(points.getSecond(), points.getFirst()));
                for (TravellingPoint point : access.getPoints()) {
                    if (train.graph != null && point.edge != null && point.node1 != null && point.node2 != null) {
                        point.reverse(train.graph);
                    }
                }
                if (wasReversed) {
                    bogey.bogeyData.remove(MARK);
                } else {
                    bogey.bogeyData.putBoolean(MARK, true);
                }
            }
            carriage.presentConductors = Couple.create(carriage.presentConductors.getSecond(), carriage.presentConductors.getFirst());
            // Create measures each coupling's strain from these anchors before it next moves the train: left in the
            // old order they read a gap a bogey span too long, and the train is stopped for stress on its couplings
            carriage.updateContraptionAnchors();
        }
        train.currentlyBackwards = !train.currentlyBackwards;
        train.speed = -train.speed;
        train.targetSpeed = -train.targetSpeed;
        TrainCoupling.rebind(train);
    }

    // --- one driver shared by a train with cabs at both ends ---

    /** Our trains (with our couplers) share one driver between their cabs; Create's own trains keep Create's rule. */
    public static boolean sharesDriver(Train train) {
        for (Carriage carriage : train.carriages) {
            CarriageContraptionEntity entity = carriage.anyAvailableEntity();
            if (entity != null && entity.getContraption() != null && !CarriageCouplers.of(entity.getContraption()).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** Whether a conductor sits at any controls of the train, whichever way they face. */
    public static boolean anyDriver(Train train) {
        for (Carriage carriage : train.carriages) {
            if (carriage.presentConductors.getFirst() || carriage.presentConductors.getSecond()) {
                return true;
            }
        }
        return false;
    }

    /** Whether some carriage has Train Controls facing the train's forwards (or backwards). */
    public static boolean controlsFacing(Train train, boolean forwards) {
        return controlsFacing(train.carriages, forwards);
    }

    public static boolean controlsFacing(List<Carriage> carriages, boolean forwards) {
        for (Carriage carriage : carriages) {
            CarriageContraptionEntity entity = carriage.anyAvailableEntity();
            if (entity != null && entity.getContraption() instanceof CarriageContraption cc && CarriageFlip
                    .forTrain(cc.hasForwardControls(), cc.hasBackwardControls(), reversed(carriage)).get(forwards)) {
                return true;
            }
        }
        return false;
    }
}
