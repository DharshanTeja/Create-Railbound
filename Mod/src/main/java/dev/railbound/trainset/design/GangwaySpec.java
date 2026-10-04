package dev.railbound.trainset.design;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** A gangway opening at both carriage ends (model pixels): half its width, and its floor and top heights. */
public record GangwaySpec(double halfWidth, double bottom, double top) {
    public static final Codec<GangwaySpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.fieldOf("half_width").forGetter(GangwaySpec::halfWidth),
            Codec.DOUBLE.fieldOf("bottom").forGetter(GangwaySpec::bottom),
            Codec.DOUBLE.fieldOf("top").forGetter(GangwaySpec::top)
    ).apply(i, GangwaySpec::new));
}
