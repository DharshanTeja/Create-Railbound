package dev.railbound.trainset.design;

import java.util.Arrays;
import java.util.Optional;

public enum PartType {
    FRAME("frame"),
    SEAT("seat"),
    DOOR("door"),
    ANCHOR("anchor"),
    CAB("cab"),
    CARGO_ITEM("cargo_item"),
    CARGO_FLUID("cargo_fluid"),
    AIR("air");

    private final String id;

    PartType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Optional<PartType> byId(String id) {
        return Arrays.stream(values()).filter(t -> t.id.equals(id)).findFirst();
    }
}
