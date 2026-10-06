package dev.railbound.steam;

import com.simibubi.create.content.trains.HonkPacket;
import com.simibubi.create.content.trains.entity.Navigation;
import com.simibubi.create.content.trains.entity.Train;
import dev.railbound.coupling.CarriageReversal;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Sounds our steam locos' whistles when a mob drives them on their schedule (see {@link AutoWhistle}), server side,
 * through Create's own horn packet, so the sound and the whistle's steam plume are Create's and the loco's as usual.
 */
public final class LocoWhistle {
    private static final Map<Train, AutoWhistle> WHISTLES = new WeakHashMap<>();
    private static final Map<Train, Deque<long[]>> PENDING = new WeakHashMap<>();

    private LocoWhistle() {}

    public static void tick(Train train, Level level) {
        long now = level.getGameTime();
        Deque<long[]> pending = PENDING.get(train);
        while (pending != null && !pending.isEmpty() && pending.peekFirst()[0] <= now) {
            PacketDistributor.sendToAllPlayers(new HonkPacket.Clientbound(train, pending.pollFirst()[1] == 1));
        }
        boolean automatic = train.runtime.getSchedule() != null && !train.runtime.paused && CarriageReversal.anyDriver(train)
                && LocoSteam.hasLoco(train, level);
        Navigation navigation = train.navigation;
        AutoWhistle.Pattern pattern = WHISTLES.computeIfAbsent(train, t -> new AutoWhistle()).tick(new AutoWhistle.Seen(
                automatic, Math.abs(train.speed) > 1e-3, train.getCurrentStation() != null,
                navigation.waitingForSignal != null, navigation.destination == null ? null : navigation.destination.id,
                navigation.distanceToDestination));
        if (pattern != AutoWhistle.Pattern.NONE) {
            Deque<long[]> queue = PENDING.computeIfAbsent(train, t -> new ArrayDeque<>());
            queue.clear();
            for (AutoWhistle.Press press : pattern.presses()) {
                queue.addLast(new long[] {now + press.tick(), press.honk() ? 1 : 0});
            }
        }
    }
}
