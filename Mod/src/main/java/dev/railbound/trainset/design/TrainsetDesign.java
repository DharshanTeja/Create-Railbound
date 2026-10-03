package dev.railbound.trainset.design;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record TrainsetDesign(
        String name,
        TrainsetCategory category,
        CarriageSize size,
        List<BogeySpec> bogeys,
        PowerType power,
        CargoSpec cargo,
        List<DoorSpec> doors,
        LayoutSpec layout) {

    public static final Codec<TrainsetDesign> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(TrainsetDesign::name),
            TrainsetCategory.CODEC.fieldOf("category").forGetter(TrainsetDesign::category),
            CarriageSize.CODEC.fieldOf("size").forGetter(TrainsetDesign::size),
            BogeySpec.CODEC.listOf().fieldOf("bogeys").forGetter(TrainsetDesign::bogeys),
            PowerType.CODEC.optionalFieldOf("power", PowerType.NONE).forGetter(TrainsetDesign::power),
            CargoSpec.CODEC.optionalFieldOf("cargo", CargoSpec.NONE).forGetter(TrainsetDesign::cargo),
            DoorSpec.CODEC.listOf().optionalFieldOf("doors", List.of()).forGetter(TrainsetDesign::doors),
            LayoutSpec.CODEC.fieldOf("layout").forGetter(TrainsetDesign::layout)
    ).apply(i, TrainsetDesign::new));

    public static final int SEATS_PER_BLOCK = 1;
}
