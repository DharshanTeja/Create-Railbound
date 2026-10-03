package dev.railbound.trainset.design;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;

public record LayoutSpec(Map<String, String> palette, List<List<String>> layers) {
    public static final Codec<LayoutSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("palette").forGetter(LayoutSpec::palette),
            Codec.STRING.listOf().listOf().fieldOf("layers").forGetter(LayoutSpec::layers)
    ).apply(i, LayoutSpec::new));
}
