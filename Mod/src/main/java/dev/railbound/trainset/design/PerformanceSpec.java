package dev.railbound.trainset.design;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** A loco's speeds at full power, in metres (blocks) per second and m/s per second, as in Create's train config. */
public record PerformanceSpec(double topSpeed, double curveSpeed, double acceleration) {
    public static final Codec<PerformanceSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.fieldOf("top_speed").forGetter(PerformanceSpec::topSpeed),
            Codec.DOUBLE.fieldOf("curve_speed").forGetter(PerformanceSpec::curveSpeed),
            Codec.DOUBLE.fieldOf("acceleration").forGetter(PerformanceSpec::acceleration)
    ).apply(i, PerformanceSpec::new));
}
