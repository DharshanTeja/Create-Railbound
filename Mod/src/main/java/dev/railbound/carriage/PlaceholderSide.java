package dev.railbound.carriage;

import dev.railbound.trainset.design.PartType;
import net.minecraft.util.StringRepresentable;

/** Which outer wall a placeholder carries: parts on the side columns close the carriage side, except doors. */
public enum PlaceholderSide implements StringRepresentable {
    NONE("none"),
    LEFT("left"),
    RIGHT("right");

    private final String id;

    PlaceholderSide(String id) {
        this.id = id;
    }

    public static PlaceholderSide of(PartType type, int localX) {
        if (type == PartType.DOOR || localX == 0) {
            return NONE;
        }
        return localX < 0 ? LEFT : RIGHT;
    }

    public PlaceholderSide mirrored() {
        return switch (this) {
            case LEFT -> RIGHT;
            case RIGHT -> LEFT;
            case NONE -> NONE;
        };
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
