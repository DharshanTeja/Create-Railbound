package dev.railbound.steam;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.ContainerData;

/**
 * What a loco's gauges read: boiler pressure (0 to 1), water and tank size (millibuckets), coal left (lumps), and
 * whether the fire is lit or the safety plug has melted. Packs into a menu's data slots (vanilla sends them as
 * shorts, so pressure goes as thousandths and water as whole tens of millibuckets).
 */
public record SteamGauges(double pressure, int water, int capacity, int lumps, boolean fireLit, boolean plugMelted) {
    public static final int DATA_SLOTS = 5;

    public static final StreamCodec<ByteBuf, SteamGauges> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, SteamGauges::pressure,
            ByteBufCodecs.VAR_INT, SteamGauges::water,
            ByteBufCodecs.VAR_INT, SteamGauges::capacity,
            ByteBufCodecs.VAR_INT, SteamGauges::lumps,
            ByteBufCodecs.BOOL, SteamGauges::fireLit,
            ByteBufCodecs.BOOL, SteamGauges::plugMelted,
            SteamGauges::new);

    public static SteamGauges of(BunkerItemStorage bunker, BunkerFluidStorage tank) {
        return new SteamGauges(bunker.pressure(), tank.amount(), tank.capacity(), bunker.lumps(), bunker.fireLit(),
                Boiler.plugStillMelted(bunker.plugMelted(), tank.amount(), tank.capacity()));
    }

    public static SteamGauges of(BunkerBlockEntity bunker) {
        Boiler.Snapshot boiler = bunker.boiler();
        return new SteamGauges(boiler.pressure(), bunker.water(), bunker.steam().water(), bunker.coal().lumps(),
                boiler.fire() > 0, Boiler.plugStillMelted(boiler.plugMelted(), bunker.water(), bunker.steam().water()));
    }

    /** Whether there is enough steam to move (see {@link Boiler#pull()}). */
    public boolean canMove() {
        return pressure >= Boiler.MOVING_PRESSURE;
    }

    public static double movingPressure() {
        return Boiler.MOVING_PRESSURE;
    }

    /** Menu data slots that read a live gauge source each time the menu syncs. */
    public static ContainerData data(java.util.function.Supplier<SteamGauges> source) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                SteamGauges g = source.get();
                return switch (index) {
                    case 0 -> (int) Math.round(g.pressure() * 1000);
                    case 1 -> g.water() / 10;
                    case 2 -> g.capacity() / 10;
                    case 3 -> g.lumps();
                    default -> (g.fireLit() ? 1 : 0) | (g.plugMelted() ? 2 : 0);
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return DATA_SLOTS;
            }
        };
    }

    /** The gauges a client menu received. */
    public static SteamGauges read(ContainerData data) {
        return new SteamGauges(data.get(0) / 1000.0, data.get(1) * 10, data.get(2) * 10, data.get(3),
                (data.get(4) & 1) != 0, (data.get(4) & 2) != 0);
    }
}
