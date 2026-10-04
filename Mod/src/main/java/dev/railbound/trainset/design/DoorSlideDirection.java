package dev.railbound.trainset.design;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Which way a door leaf slides open, in design space: x runs left to right, z front to rear. */
public enum DoorSlideDirection implements StringRepresentable {
    FRONT("front", 0, -1),
    REAR("rear", 0, 1),
    LEFT("left", -1, 0),
    RIGHT("right", 1, 0);

    public static final Codec<DoorSlideDirection> CODEC = StringRepresentable.fromEnum(DoorSlideDirection::values);

    private final String id;
    private final int dx;
    private final int dz;

    DoorSlideDirection(String id, int dx, int dz) {
        this.id = id;
        this.dx = dx;
        this.dz = dz;
    }

    public int dx() {
        return dx;
    }

    public int dz() {
        return dz;
    }

    /** Front and rear slide along the carriage; left and right across it. */
    public boolean sideways() {
        return dx != 0;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
