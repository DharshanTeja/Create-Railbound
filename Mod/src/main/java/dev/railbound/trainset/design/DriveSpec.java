package dev.railbound.trainset.design;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * A steam loco's motion, in model pixels (y up from the floor, z towards the rear): its driving wheels' radius and
 * axles, the crank throw, the crosshead line and the connecting rod to the main axle. The model draws each moving
 * part in its own group, named as {@link #parts()} lists, posed with every crank pin straight above its axle.
 */
public record DriveSpec(double wheelRadius, double axleY, List<Double> axles, int mainAxle,
                        double crankRadius, double crossheadY, double rodLength) {
    public static final Codec<DriveSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.fieldOf("wheel_radius").forGetter(DriveSpec::wheelRadius),
            Codec.DOUBLE.fieldOf("axle_y").forGetter(DriveSpec::axleY),
            Codec.DOUBLE.listOf().fieldOf("axles").forGetter(DriveSpec::axles),
            Codec.INT.fieldOf("main_axle").forGetter(DriveSpec::mainAxle),
            Codec.DOUBLE.fieldOf("crank_radius").forGetter(DriveSpec::crankRadius),
            Codec.DOUBLE.fieldOf("crosshead_y").forGetter(DriveSpec::crossheadY),
            Codec.DOUBLE.fieldOf("rod_length").forGetter(DriveSpec::rodLength)
    ).apply(i, DriveSpec::new));

    public static final List<String> SIDES = List.of("right", "left");

    /** The model groups that move: each axle's wheels per side, then each side's coupling rod, connecting rod and crosshead. */
    public List<String> parts() {
        List<String> parts = new ArrayList<>();
        for (int axle = 1; axle <= axles.size(); axle++) {
            for (String side : SIDES) {
                parts.add("drive_wheels_" + axle + "_" + side);
            }
        }
        for (String piece : List.of("coupling_rod", "connecting_rod", "crosshead")) {
            for (String side : SIDES) {
                parts.add("drive_" + piece + "_" + side);
            }
        }
        return parts;
    }
}
