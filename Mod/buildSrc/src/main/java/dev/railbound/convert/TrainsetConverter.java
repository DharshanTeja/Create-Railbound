package dev.railbound.convert;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Turns a trainset's `.bbmodel` into OBJ parts in design block space, plus their NeoForge loader JSON, a shared
 * material and the design texture. Fails with every broken model rule listed at once.
 */
public final class TrainsetConverter {
    public static final int MAX_TEXTURE = ModelSource.MAX_TEXTURE;
    /** Half the 3-block body width, in model units. */
    public static final double HALF_WIDTH = 24;
    /** Gangways and buffers may reach this far past the carriage ends: half the one-block gap between carriages. */
    public static final double END_OVERHANG = 8;
    /** Create's bogey top sits just under y 0; upward faces need this much clearance (model units) above it. */
    public static final double BOGEY_CLEARANCE = 0.3;
    /** Half the width of a bogey top, in model units. */
    private static final double BOGEY_HALF_WIDTH = 16;
    private static final double EPSILON = 1e-6;

    private TrainsetConverter() {}

    public static ConvertedTrainset convert(String namespace, String id, JsonObject model, JsonObject design)
            throws ConversionException {
        List<String> problems = new ArrayList<>();
        int length = design.getAsJsonObject("size").get("length").getAsInt();
        ModelSpace space = new ModelSpace(length);
        ModelSource source = new ModelSource(model);

        List<double[]> bogeyZones = new ArrayList<>();
        if (design.has("bogeys")) {
            for (JsonElement bogey : design.getAsJsonArray("bogeys")) {
                double start = 16 * bogey.getAsJsonObject().get("z").getAsInt() - 8 * length;
                bogeyZones.add(new double[] {start, start + 16});
            }
        }

        Map<String, List<CubeFace>> partFaces = new LinkedHashMap<>();
        Set<Integer> texturesUsed = new TreeSet<>();
        for (Element element : source.elements()) {
            Optional<String> part = Parts.partFor(source.pathOf(element));
            if (part.isEmpty()) {
                continue;
            }
            element.problem().ifPresent(problems::add);
            List<CubeFace> faces = partFaces.computeIfAbsent(part.get(), p -> new ArrayList<>());
            String kind = element.isMesh() ? "mesh" : "cube";
            boolean outside = false;
            boolean flush = false;
            for (CubeFace face : element.faces()) {
                faces.add(face);
                for (double[] corner : face.corners()) {
                    outside |= Math.abs(corner[0]) > HALF_WIDTH + EPSILON || Math.abs(corner[2]) > 8 * length + END_OVERHANG + EPSILON;
                }
                flush |= facesUp(face) && flushWithBogeyTop(face, bogeyZones);
            }
            if (outside) {
                problems.add(kind + " '" + element.name() + "' sticks out past the 3-block width (x within ±24) or the carriage ends");
            }
            if (flush) {
                problems.add(kind + " '" + element.name() + "' has its top at y 0 over a bogey; raise it to y "
                        + BOGEY_CLEARANCE + " or more so Create's bogey top does not flicker through it");
            }
        }

        if (!partFaces.containsKey(Parts.BODY)) {
            problems.add("the model has no body cubes");
        }
        Set<String> modelDoors = new TreeSet<>();
        partFaces.keySet().stream().filter(p -> p.startsWith("door_")).forEach(modelDoors::add);
        Set<String> designDoors = new TreeSet<>();
        if (design.has("doors")) {
            for (JsonElement door : design.getAsJsonArray("doors")) {
                designDoors.add(door.getAsJsonObject().get("part").getAsString());
            }
        }
        for (String door : designDoors) {
            if (!modelDoors.contains(door)) {
                problems.add("the design has door '" + door + "' but the model has no group of that name");
            }
        }
        for (String door : modelDoors) {
            if (!designDoors.contains(door)) {
                problems.add("the model has door group '" + door + "' that the design does not list");
            }
        }

        Set<String> modelDrive = new TreeSet<>();
        partFaces.keySet().stream().filter(p -> p.startsWith("drive_")).forEach(modelDrive::add);
        Set<String> designDrive = design.has("drive") ? driveParts(design.getAsJsonObject("drive")) : Set.of();
        for (String part : designDrive) {
            if (!modelDrive.contains(part)) {
                problems.add("the design's drive needs a model group '" + part + "'");
            }
        }
        for (String part : modelDrive) {
            if (!designDrive.contains(part)) {
                problems.add("the model has drive group '" + part + "' that the design's drive does not have");
            }
        }

        List<CubeFace> allFaces = new ArrayList<>();
        partFaces.values().forEach(allFaces::addAll);
        ModelSource.Texture texture = allFaces.isEmpty() ? null : source.texture(allFaces, problems);
        if (!problems.isEmpty()) {
            throw new ConversionException(id + ": " + String.join("; ", problems));
        }

        Map<String, String> objects = new LinkedHashMap<>();
        Map<String, String> modelJsons = new LinkedHashMap<>();
        for (Map.Entry<String, List<CubeFace>> part : partFaces.entrySet()) {
            objects.put(part.getKey(), ModelSource.obj(part.getValue(), c -> space.toBlock(c[0], c[1], c[2]), texture));
            modelJsons.put(part.getKey(), ModelSource.loaderJson(namespace, "trainset", id, part.getKey()));
        }
        return new ConvertedTrainset(namespace, id, objects, modelJsons, ModelSource.MATERIAL, texture.png());
    }

    /** A cube's top, or a mesh face pointing straight up. */
    private static boolean facesUp(CubeFace face) {
        return face.direction().equals("up") || face.direction().equals("mesh") && ModelSource.normal(face.corners())[1] > 0.99;
    }

    /** An upward face within BOGEY_CLEARANCE of y 0, over the bogey's width, inside a bogey's cell. */
    private static boolean flushWithBogeyTop(CubeFace face, List<double[]> bogeyZones) {
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (double[] c : face.corners()) {
            if (Math.abs(c[1]) >= BOGEY_CLEARANCE) {
                return false;
            }
            minX = Math.min(minX, c[0]);
            maxX = Math.max(maxX, c[0]);
            minZ = Math.min(minZ, c[2]);
            maxZ = Math.max(maxZ, c[2]);
        }
        if (maxX <= -BOGEY_HALF_WIDTH || minX >= BOGEY_HALF_WIDTH) {
            return false;
        }
        for (double[] zone : bogeyZones) {
            if (maxZ > zone[0] && minZ < zone[1]) {
                return true;
            }
        }
        return false;
    }

    /** The moving parts a design's drive names (the mod's DriveSpec.parts(): wheels per axle and side, then rods). */
    static Set<String> driveParts(JsonObject drive) {
        Set<String> parts = new TreeSet<>();
        int axles = drive.has("axles") ? drive.getAsJsonArray("axles").size() : 0;
        for (String side : List.of("right", "left")) {
            for (int axle = 1; axle <= axles; axle++) {
                parts.add("drive_wheels_" + axle + "_" + side);
            }
            for (String piece : List.of("coupling_rod", "connecting_rod", "crosshead")) {
                parts.add("drive_" + piece + "_" + side);
            }
        }
        return parts;
    }
}
