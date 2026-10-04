package dev.railbound.trainset.design;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

import java.util.Optional;

/** A door leaf: its model group, its lower cell, and optionally which way it slides (side doors work it out). */
public record DoorSpec(String part, BlockPos pos, Optional<DoorSlideDirection> slide) {
    public static final Codec<DoorSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("part").forGetter(DoorSpec::part),
            BlockPos.CODEC.fieldOf("pos").forGetter(DoorSpec::pos),
            DoorSlideDirection.CODEC.optionalFieldOf("slide").forGetter(DoorSpec::slide)
    ).apply(i, DoorSpec::new));

    public DoorSpec(String part, BlockPos pos) {
        this(part, pos, Optional.empty());
    }
}
