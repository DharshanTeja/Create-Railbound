package dev.railbound.trainset.design;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * A steam loco's water tank capacity (millibuckets) and coal bunker size (slots), and where its smoke and steam come
 * out, in model pixels: the chimney top, the front of the right-hand cylinder (the left one is its mirror) and the
 * safety valve and the whistle.
 */
public record SteamSpec(int water, int bunkerSlots, List<Double> chimney, List<Double> cylinder, List<Double> safetyValve,
                        List<Double> whistle) {
    public static final List<Double> NO_POINT = List.of(0.0, 0.0, 0.0);

    public static final Codec<SteamSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("water").forGetter(SteamSpec::water),
            Codec.INT.fieldOf("bunker_slots").forGetter(SteamSpec::bunkerSlots),
            Codec.DOUBLE.listOf(3, 3).optionalFieldOf("chimney", NO_POINT).forGetter(SteamSpec::chimney),
            Codec.DOUBLE.listOf(3, 3).optionalFieldOf("cylinder", NO_POINT).forGetter(SteamSpec::cylinder),
            Codec.DOUBLE.listOf(3, 3).optionalFieldOf("safety_valve", NO_POINT).forGetter(SteamSpec::safetyValve),
            Codec.DOUBLE.listOf(3, 3).optionalFieldOf("whistle", NO_POINT).forGetter(SteamSpec::whistle)
    ).apply(i, SteamSpec::new));

    public SteamSpec(int water, int bunkerSlots) {
        this(water, bunkerSlots, NO_POINT, NO_POINT, NO_POINT, NO_POINT);
    }
}
