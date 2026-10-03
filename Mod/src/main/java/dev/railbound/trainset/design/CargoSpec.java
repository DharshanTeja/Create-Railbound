package dev.railbound.trainset.design;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record CargoSpec(int itemSlots, int fluidMb) {
    public static final CargoSpec NONE = new CargoSpec(0, 0);

    public static final Codec<CargoSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            StrictInt.NON_NEGATIVE.optionalFieldOf("item_slots", 0).forGetter(CargoSpec::itemSlots),
            StrictInt.NON_NEGATIVE.optionalFieldOf("fluid_mb", 0).forGetter(CargoSpec::fluidMb)
    ).apply(i, CargoSpec::new));
}
