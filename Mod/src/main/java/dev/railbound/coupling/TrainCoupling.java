package dev.railbound.coupling;

import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.GlobalRailwayManager;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.station.GlobalStation;
import dev.railbound.carriage.CarriageCouplers;
import dev.railbound.carriage.CouplingGeometry;
import dev.railbound.mixin.TrainAccessor;
import dev.railbound.network.TrainJoinPayload;
import dev.railbound.network.TrainSplitPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Splitting and joining trains at their knuckle couplers. A stopped train wrenched at a coupler splits there: the half
 * with the loco keeps the train, the other half stands as a train of its own. Two trains whose couplers meet slowly
 * join into the moving one, turning the other train round when two fronts or two rears meet; faster, they bump
 * and stop. The server changes its trains in place and tells clients to
 * make the same change to theirs, so every carriage keeps its entity, passengers and cargo.
 */
public final class TrainCoupling {
    private TrainCoupling() {}

    /** A train's free end with a coupler: where it is in the world, how far its knuckle reaches, and in which level. */
    private record FreeEnd(CouplingGeometry.End end, double reach, Level level) {}

    // --- server: splitting ---

    /** A player wrenched the coupler behind carriage {@code gap}. */
    public static void uncouple(ServerPlayer player, UUID trainId, int gap) {
        Train train = Create.RAILWAYS.trains.get(trainId);
        if (train == null || train.graph == null || train.derailed || gap < 0 || gap >= train.carriages.size() - 1
                || !AllItems.WRENCH.isIn(player.getMainHandItem())) {
            return;
        }
        Carriage front = train.carriages.get(gap), back = train.carriages.get(gap + 1);
        CarriageContraptionEntity a = front.anyAvailableEntity(), b = back.anyAvailableEntity();
        if (a == null || b == null || a.level() != player.level() || front.presentInMultipleDimensions()
                || back.presentInMultipleDimensions()) {
            return;
        }
        Optional<Vec3> meet = meet(a, b);
        double reach = player.blockInteractionRange() + 2;
        if (meet.isEmpty() || player.getEyePosition().distanceToSqr(meet.get()) > reach * reach) {
            return;
        }
        if (Math.abs(train.speed) > 1e-3) {
            player.displayClientMessage(Component.translatable("railbound.coupling.stop_first"), true);
            return;
        }
        List<Carriage> frontHalf = train.carriages.subList(0, gap + 1), backHalf = train.carriages.subList(gap + 1, train.carriages.size());
        TrainCut.Split split = TrainCut.split(train.carriages.size(), train.carriageSpacing, gap, hasControls(frontHalf),
                hasControls(backHalf));
        GlobalStation station = train.getCurrentStation();

        Train cut = cut(train, gap, split.frontKeeps());
        replan(train);
        // the half whose end stands at the station is the one there, ready to disassemble
        if (station != null && !TrainCut.stillAtStation(split.frontKeeps(), train.currentlyBackwards)) {
            train.leaveStation();
            arrive(cut, station, train.currentlyBackwards);
        }
        player.level().playSound(null, meet.get().x, meet.get().y, meet.get().z, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS,
                1, 0.7f);
    }

    /**
     * Server: cuts a train behind carriage {@code gap}, keeping the front or back half, and tells clients. The other
     * half stands as a new train, which is returned.
     */
    private static Train cut(Train train, int gap, boolean frontKeeps) {
        List<Carriage> frontHalf = train.carriages.subList(0, gap + 1), backHalf = train.carriages.subList(gap + 1, train.carriages.size());
        boolean keptDoubleEnded = backControls(frontKeeps ? frontHalf : backHalf);
        boolean cutDoubleEnded = backControls(frontKeeps ? backHalf : frontHalf);
        UUID newId = UUID.randomUUID();
        Train cut = applySplit(Create.RAILWAYS, true, train, gap, frontKeeps, newId, keptDoubleEnded, cutDoubleEnded);
        train.updateSignalBlocks = true;
        cut.updateSignalBlocks = true;
        PacketDistributor.sendToAllPlayers(new TrainSplitPayload(train.id, gap, frontKeeps, newId, keptDoubleEnded, cutDoubleEnded));
        return cut;
    }

    /** How many of a train's carriages, from its end at the station, Create's disassembly can lay out (see {@link TrainCut#fitAtStation}). */
    public static int fitAtStation(Train train) {
        return fitAtStation(train, train.currentlyBackwards);
    }

    /** As {@link #fitAtStation(Train)}, for a client, which learns from the station whether the train arrived backwards. */
    public static int fitAtStation(Train train, boolean arrivedBackwards) {
        List<Optional<TrainCut.Pose>> poses = new ArrayList<>();
        for (Carriage carriage : train.carriages) {
            CarriageContraptionEntity entity = carriage.presentInMultipleDimensions() ? null : carriage.anyAvailableEntity();
            poses.add(entity == null ? Optional.empty() : Optional.of(new TrainCut.Pose(entity.yaw, entity.pitch)));
        }
        return TrainCut.fitAtStation(poses, arrivedBackwards);
    }

    /**
     * Server, as a train at a station disassembles: when only some of its carriages fit the station's straight, the
     * rest are uncoupled first and stay behind as their own train, so the carriages that fit can be disassembled.
     */
    public static void leaveBehindWhatDoesNotFit(Train train) {
        int count = train.carriages.size(), fit = fitAtStation(train);
        if (fit <= 0 || fit >= count) {
            return;
        }
        boolean frontKeeps = !train.currentlyBackwards;   // the end standing at the station
        cut(train, frontKeeps ? fit - 1 : count - fit - 1, frontKeeps);
    }

    /** Where two neighbouring carriages' knuckles lock, if they are joined by ours. */
    private static Optional<Vec3> meet(CarriageContraptionEntity a, CarriageContraptionEntity b) {
        List<CouplingGeometry.End> endsA = ends(a), endsB = ends(b);
        return CouplingGeometry.facing(endsA, a.getBoundingBox().getCenter(), endsB, b.getBoundingBox().getCenter())
                .map(pair -> CouplingGeometry.meet(endsA.get(pair[0]), endsB.get(pair[1])));
    }

    private static List<CouplingGeometry.End> ends(CarriageContraptionEntity entity) {
        return entity.getContraption() == null ? List.of() : CarriageCouplers.of(entity.getContraption()).stream()
                .map(local -> CarriageCouplers.end(entity, local, 1)).toList();
    }

    private static boolean hasControls(List<Carriage> carriages) {
        return carriages.stream().map(TrainCoupling::contraption)
                .anyMatch(c -> c != null && (c.hasForwardControls() || c.hasBackwardControls()));
    }

    /** Create lets a train be driven backwards when any carriage has controls facing back along it. */
    private static boolean backControls(List<Carriage> carriages) {
        return CarriageReversal.controlsFacing(carriages, false);
    }

    @Nullable
    private static CarriageContraption contraption(Carriage carriage) {
        CarriageContraptionEntity entity = carriage.anyAvailableEntity();
        return entity != null && entity.getContraption() instanceof CarriageContraption cc ? cc : null;
    }

    /** A train whose carriages changed plans its route afresh, as Create does after putting a train back on track. */
    private static void replan(Train train) {
        train.speed = 0;
        train.navigation.cancelNavigation();
        if (train.runtime.getSchedule() != null && train.runtime.state == ScheduleRuntime.State.IN_TRANSIT) {
            train.runtime.state = ScheduleRuntime.State.PRE_TRANSIT;
        }
    }

    /** A standing train takes over a stop at a station, as if it had arrived there itself. */
    private static void arrive(Train train, GlobalStation station, boolean backwards) {
        train.currentlyBackwards = backwards;
        train.setCurrentStation(station);
        station.reserveFor(train);
    }

    // --- server: joining ---

    /** After the trains have moved each tick: couplers that have met join or bump. */
    public static void tick(ServerTickEvent.Post event) {
        Map<Train, Optional<FreeEnd>> fronts = new HashMap<>(), rears = new HashMap<>();
        for (Train moving : new ArrayList<>(Create.RAILWAYS.trains.values())) {
            if (!Create.RAILWAYS.trains.containsKey(moving.id) || !onTrack(moving) || Math.abs(moving.speed) < 1e-4) {
                continue;
            }
            boolean movingFront = moving.speed > 0;
            Optional<FreeEnd> movingEnd = freeEnd(moving, movingFront, fronts, rears);
            if (movingEnd.isEmpty()) {
                continue;
            }
            others:
            for (Train other : new ArrayList<>(Create.RAILWAYS.trains.values())) {
                if (other == moving || other.graph != moving.graph || !onTrack(other)) {
                    continue;
                }
                for (boolean otherFront : new boolean[] {true, false}) {
                    Optional<FreeEnd> otherEnd = freeEnd(other, otherFront, fronts, rears);
                    if (otherEnd.isEmpty() || otherEnd.get().level() != movingEnd.get().level()
                            || !CouplingGeometry.touching(movingEnd.get().end(), otherEnd.get().end(),
                            movingEnd.get().reach() + otherEnd.get().reach())) {
                        continue;
                    }
                    // two trains running into each other: the faster one carries on, so its own check handles it
                    boolean otherRunningIn = other.speed != 0 && (other.speed > 0) == otherFront;
                    if (otherRunningIn && Math.abs(other.speed) > Math.abs(moving.speed)) {
                        continue;
                    }
                    Vec3 at = CouplingGeometry.meet(movingEnd.get().end(), otherEnd.get().end());
                    contact(moving, movingFront, other, otherFront, at, movingEnd.get().level());
                    break others;
                }
            }
        }
    }

    private static boolean onTrack(Train train) {
        return train.graph != null && !train.derailed && !train.carriages.isEmpty();
    }

    private static Optional<FreeEnd> freeEnd(Train train, boolean front, Map<Train, Optional<FreeEnd>> fronts,
                                             Map<Train, Optional<FreeEnd>> rears) {
        return (front ? fronts : rears).computeIfAbsent(train, t -> findFreeEnd(t, front));
    }

    /** The coupler at a train's front (pointing the way it runs forwards) or rear, if that end has one of ours. */
    private static Optional<FreeEnd> findFreeEnd(Train train, boolean front) {
        Carriage carriage = train.carriages.get(front ? 0 : train.carriages.size() - 1);
        CarriageContraptionEntity entity = carriage.anyAvailableEntity();
        if (entity == null || carriage.getLeadingPoint().edge == null || carriage.getTrailingPoint().edge == null) {
            return Optional.empty();
        }
        Vec3 forward = carriage.getLeadingPoint().getPosition(train.graph).subtract(carriage.getTrailingPoint().getPosition(train.graph));
        if (forward.lengthSqr() < 1e-6) {
            return Optional.empty();
        }
        Vec3 out = forward.normalize().scale(front ? 1 : -1);
        FreeEnd best = null;
        double bestDot = 0.7;
        if (entity.getContraption() == null) {
            return Optional.empty();
        }
        for (CarriageCouplers.Local local : CarriageCouplers.of(entity.getContraption())) {
            CouplingGeometry.End end = CarriageCouplers.end(entity, local, 1);
            double dot = end.outward().dot(out);
            if (dot > bestDot) {
                bestDot = dot;
                best = new FreeEnd(end, local.reach(), entity.level());
            }
        }
        return Optional.ofNullable(best);
    }

    /** The outermost bogey at a train's front or rear. */
    private static CarriageBogey endBogey(Train train, boolean front) {
        return front ? train.carriages.get(0).leadingBogey() : train.carriages.get(train.carriages.size() - 1).trailingBogey();
    }

    private static void contact(Train moving, boolean movingFront, Train other, boolean otherFront, Vec3 at, Level level) {
        double combined = Math.abs(moving.speed) + Math.abs(other.speed);
        if (TrainCut.contact(combined) == TrainCut.Contact.BUMP) {
            moving.speed = 0;
            other.speed = 0;
            if (combined > 0.02) {
                level.playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.4f, 0.6f);
            }
            return;
        }
        // the gap Create keeps between the two bogeys either side of the new join, measured before anything changes
        Vec3 a = endBogey(moving, movingFront).getAnchorPosition(), b = endBogey(other, otherFront).getAnchorPosition();
        if (a == null || b == null) {
            return;
        }
        int gap = (int) Math.round(a.distanceTo(b));
        TrainCut.Join join = TrainCut.join(movingFront, otherFront);
        if (join.reverseOther()) {
            // front to front or rear to rear: the other train turns round, so its carriages face the moving train's way
            CarriageReversal.reverse(other);
        }
        boolean otherFirst = join.order() == TrainCut.Order.OTHER_FIRST;
        List<Carriage> all = new ArrayList<>(moving.carriages);
        all.addAll(other.carriages);
        boolean doubleEnded = CarriageReversal.controlsFacing(all, false);

        // the joined train is the moving one, with its name and schedule, unless only the other train has a schedule
        // (a helper loco backed onto a scheduled train): then the other one carries on
        boolean keepMoving = moving.runtime.getSchedule() != null || other.runtime.getSchedule() == null;
        Train keep = keepMoving ? moving : other, drop = keepMoving ? other : moving;
        boolean keepFirst = (keep == other) == otherFirst;
        GlobalStation station = drop.getCurrentStation();
        boolean dropBackwards = drop.currentlyBackwards;
        if (station != null) {
            station.cancelReservation(drop);
        }
        // a schedule on the train that stops being one goes back to whoever is there to take it
        ItemStack schedule = drop.runtime.returnSchedule(level.registryAccess());
        if (!schedule.isEmpty()) {
            level.addFreshEntity(new ItemEntity(level, at.x, at.y + 0.5, at.z, schedule));
        }
        applyJoin(Create.RAILWAYS, true, keep, drop, keepFirst, gap, doubleEnded);
        replan(keep);
        // the joined train is at a station while the end that stopped there is still one of its ends: a loco coupled
        // onto a train standing at a station leaves it there, ready to disassemble
        if (keep.getCurrentStation() != null && !TrainCut.stillAtStation(keepFirst, keep.currentlyBackwards)) {
            keep.leaveStation();
        }
        if (station != null && keep.getCurrentStation() == null && TrainCut.stillAtStation(!keepFirst, dropBackwards)) {
            arrive(keep, station, dropBackwards);
        }
        keep.updateSignalBlocks = true;
        PacketDistributor.sendToAllPlayers(new TrainJoinPayload(keep.id, drop.id,
                TrainJoinPayload.flags(join.reverseOther() && keep == other, join.reverseOther() && drop == other, keepFirst),
                gap, doubleEnded));
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.35f, 1.5f);
    }

    // --- both sides: the change itself ---

    /**
     * Cuts a train behind carriage {@code gap}: it keeps one half (front or back) and the other half becomes a new
     * train, which is returned. The same on the server and, mirrored, on each client.
     */
    public static Train applySplit(GlobalRailwayManager railways, boolean server, Train train, int gap, boolean frontKeeps,
                                   UUID newId, boolean keptDoubleEnded, boolean cutDoubleEnded) {
        int count = train.carriages.size();
        List<Carriage> front = new ArrayList<>(train.carriages.subList(0, gap + 1));
        List<Carriage> back = new ArrayList<>(train.carriages.subList(gap + 1, count));
        TrainCut.Split split = TrainCut.split(count, train.carriageSpacing, gap, frontKeeps, !frontKeeps);
        reshape(train, frontKeeps ? front : back, new ArrayList<>(frontKeeps ? split.frontSpacing() : split.backSpacing()),
                keptDoubleEnded);
        Train cut = new Train(newId, train.owner, train.graph, frontKeeps ? back : front,
                new ArrayList<>(frontKeeps ? split.backSpacing() : split.frontSpacing()), cutDoubleEnded, train.mapColorIndex);
        cut.currentlyBackwards = train.currentlyBackwards;
        rebind(cut);
        if (server) {
            railways.addTrain(cut);
        } else {
            railways.trains.put(cut.id, cut);
        }
        return cut;
    }

    /** Puts train {@code drop}'s carriages behind (or in front of) {@code keep}'s, and removes {@code drop}. */
    public static void applyJoin(GlobalRailwayManager railways, boolean server, Train keep, Train drop, boolean keepFirst,
                                 int gap, boolean doubleEnded) {
        Train first = keepFirst ? keep : drop, second = keepFirst ? drop : keep;
        List<Carriage> carriages = new ArrayList<>(first.carriages);
        carriages.addAll(second.carriages);
        List<Integer> spacing = TrainCut.joinedSpacing(first.carriageSpacing, gap, second.carriageSpacing);
        reshape(keep, carriages, spacing, doubleEnded);
        if (server) {
            railways.removeTrain(drop.id);
        } else {
            railways.trains.remove(drop.id);
        }
    }

    private static void reshape(Train train, List<Carriage> carriages, List<Integer> spacing, boolean doubleEnded) {
        train.carriages = carriages;
        train.carriageSpacing = spacing;
        ((TrainAccessor) train).setStress(new double[spacing.size()]);
        train.doubleEnded = doubleEnded;
        rebind(train);
    }

    /** Points each carriage, and the entity drawing it, at its train and its place in it. */
    static void rebind(Train train) {
        for (int i = 0; i < train.carriages.size(); i++) {
            Carriage carriage = train.carriages.get(i);
            carriage.setTrain(train);
            int index = i;
            carriage.forEachPresentEntity(entity -> {
                entity.trainId = train.id;
                entity.carriageIndex = index;
            });
        }
    }

    // --- client: mirroring the server ---

    public static void mirrorSplit(Level level, TrainSplitPayload payload) {
        GlobalRailwayManager railways = Create.RAILWAYS.sided(level);
        Train train = railways.trains.get(payload.train());
        if (train == null || sharesServerTrain(railways, payload.train())) {
            return;
        }
        if (payload.gap() >= 0 && payload.gap() < train.carriages.size() - 1) {
            applySplit(railways, false, train, payload.gap(), payload.frontKeeps(), payload.newTrain(),
                    payload.keptDoubleEnded(), payload.cutDoubleEnded());
        }
    }

    public static void mirrorJoin(Level level, TrainJoinPayload payload) {
        GlobalRailwayManager railways = Create.RAILWAYS.sided(level);
        Train keep = railways.trains.get(payload.keep()), drop = railways.trains.get(payload.drop());
        if (keep != null && drop != null && !sharesServerTrain(railways, payload.keep())) {
            if (payload.reverseKeep()) {
                CarriageReversal.reverse(keep);
            }
            if (payload.reverseDrop()) {
                CarriageReversal.reverse(drop);
            }
            applyJoin(railways, false, keep, drop, payload.keepFirst(), payload.gap(), payload.doubleEnded());
        }
    }

    /** Whether this client's copy of a train is the server's own object (already changed): never cut it twice. */
    private static boolean sharesServerTrain(GlobalRailwayManager railways, UUID id) {
        return railways == Create.RAILWAYS || Create.RAILWAYS.trains.get(id) == railways.trains.get(id);
    }
}
