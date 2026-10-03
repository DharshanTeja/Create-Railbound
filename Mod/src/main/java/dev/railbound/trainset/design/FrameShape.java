package dev.railbound.trainset.design;

import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.Optional;

public enum FrameShape implements StringRepresentable {
    FLOOR("floor"),
    FLOOR_WALL_LEFT("floor_wall_left"),
    FLOOR_WALL_RIGHT("floor_wall_right"),
    WALL_LEFT("wall_left"),
    WALL_RIGHT("wall_right"),
    ROOF("roof"),
    ROOF_WALL_LEFT("roof_wall_left"),
    ROOF_WALL_RIGHT("roof_wall_right"),
    PARTITION("partition"),
    FULL("full");

    private final String id;

    FrameShape(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** The same shape seen in a mirror: left and right swap. */
    public FrameShape mirrored() {
        return switch (this) {
            case FLOOR_WALL_LEFT -> FLOOR_WALL_RIGHT;
            case FLOOR_WALL_RIGHT -> FLOOR_WALL_LEFT;
            case WALL_LEFT -> WALL_RIGHT;
            case WALL_RIGHT -> WALL_LEFT;
            case ROOF_WALL_LEFT -> ROOF_WALL_RIGHT;
            case ROOF_WALL_RIGHT -> ROOF_WALL_LEFT;
            default -> this;
        };
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public static Optional<FrameShape> byId(String id) {
        return Arrays.stream(values()).filter(s -> s.id.equals(id)).findFirst();
    }
}
