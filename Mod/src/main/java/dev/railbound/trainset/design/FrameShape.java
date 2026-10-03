package dev.railbound.trainset.design;

import java.util.Arrays;
import java.util.Optional;

public enum FrameShape {
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

    public static Optional<FrameShape> byId(String id) {
        return Arrays.stream(values()).filter(s -> s.id.equals(id)).findFirst();
    }
}
