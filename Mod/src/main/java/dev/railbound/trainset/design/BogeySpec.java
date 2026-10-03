package dev.railbound.trainset.design;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record BogeySpec(int z, ResourceLocation style) {
    public static final ResourceLocation DEFAULT_STYLE = ResourceLocation.fromNamespaceAndPath("create", "standard");

    public static final Codec<BogeySpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("z").forGetter(BogeySpec::z),
            ResourceLocation.CODEC.optionalFieldOf("style", DEFAULT_STYLE).forGetter(BogeySpec::style)
    ).apply(i, BogeySpec::new));
}
