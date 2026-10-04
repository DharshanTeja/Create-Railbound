package dev.railbound.trainset.design;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum TrainsetCategory implements StringRepresentable {
    PASSENGER("passenger"),
    BOX_CAR("box_car"),
    TANK_CAR("tank_car"),
    LOCOMOTIVE("locomotive"),
    MULTIPLE_UNIT("multiple_unit");

    public static final Codec<TrainsetCategory> CODEC = StringRepresentable.fromEnum(TrainsetCategory::values);

    private final String id;

    TrainsetCategory(String id) {
        this.id = id;
    }

    /** Cars a crew drives, the only ones that may carry Train Controls. */
    public boolean drivable() {
        return this == LOCOMOTIVE || this == MULTIPLE_UNIT;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
