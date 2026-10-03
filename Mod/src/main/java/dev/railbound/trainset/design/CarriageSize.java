package dev.railbound.trainset.design;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;

public record CarriageSize(int length, int width, int height) {
    public static final Codec<CarriageSize> CODEC = RecordCodecBuilder.create(i -> i.group(
            ExtraCodecs.POSITIVE_INT.fieldOf("length").forGetter(CarriageSize::length),
            ExtraCodecs.POSITIVE_INT.fieldOf("width").forGetter(CarriageSize::width),
            ExtraCodecs.POSITIVE_INT.fieldOf("height").forGetter(CarriageSize::height)
    ).apply(i, CarriageSize::new));
}
