package dev.railbound.trainset.design;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record CarriageSize(int length, int width, int height) {
    public static final Codec<CarriageSize> CODEC = RecordCodecBuilder.create(i -> i.group(
            StrictInt.POSITIVE.fieldOf("length").forGetter(CarriageSize::length),
            StrictInt.POSITIVE.fieldOf("width").forGetter(CarriageSize::width),
            StrictInt.POSITIVE.fieldOf("height").forGetter(CarriageSize::height)
    ).apply(i, CarriageSize::new));
}
