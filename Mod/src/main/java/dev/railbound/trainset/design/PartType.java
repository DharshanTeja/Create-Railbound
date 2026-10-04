package dev.railbound.trainset.design;

import java.util.Arrays;
import java.util.Optional;

public enum PartType {
    FRAME("frame"),
    SEAT("seat"),
    DOOR("door"),
    STEP("step"),
    ANCHOR("anchor"),
    /** Create's Train Controls: the driver's position, facing the seat directly ahead or behind it. */
    CONTROLS("controls"),
    /** A steam loco's coal bunker, which also holds the boiler. */
    BUNKER("bunker"),
    /** A steam loco's firebox door on the cab backhead: coal and the bunker screen, reached from the cab. */
    FIREBOX("firebox"),
    /** A steam loco's water tank filler. */
    WATER_TANK("water_tank"),
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
