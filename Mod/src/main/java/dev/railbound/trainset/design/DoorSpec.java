package dev.railbound.trainset.design;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record DoorSpec(String part, BlockPos pos) {
    public static final Codec<DoorSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("part").forGetter(DoorSpec::part),
            BlockPos.CODEC.fieldOf("pos").forGetter(DoorSpec::pos)
    ).apply(i, DoorSpec::new));
}
