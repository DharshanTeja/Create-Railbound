package dev.railbound.carriage;

import dev.railbound.trainset.design.PartType;
import net.minecraft.util.StringRepresentable;

import java.util.Optional;

/** Stand-ins for parts that later sub-phases implement (seats 1.2, doors 1.2, cab 1.4, cargo 1.5/1.6). */
public enum PlaceholderPart implements StringRepresentable {
    SEAT("seat"),
    DOOR("door"),
    CAB("cab"),
    CARGO_ITEM("cargo_item"),
    CARGO_FLUID("cargo_fluid");

    private final String id;

    PlaceholderPart(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public static Optional<PlaceholderPart> of(PartType type) {
        return switch (type) {
            case SEAT -> Optional.of(SEAT);
            case DOOR -> Optional.of(DOOR);
            case CAB -> Optional.of(CAB);
            case CARGO_ITEM -> Optional.of(CARGO_ITEM);
            case CARGO_FLUID -> Optional.of(CARGO_FLUID);
            default -> Optional.empty();
        };
    }
}
