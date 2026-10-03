package dev.railbound.convert;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Optional;

/** A Blockbench cube, with Minecraft's face corner order and Blockbench's ZYX rotation about the cube origin. */
public record Element(String name, String uuid, double[] from, double[] to, double[] origin, double[] rotation,
                      JsonObject faces) {

    public static final String[] DIRECTIONS = {"north", "south", "east", "west", "up", "down"};

    public static Element of(JsonObject json) {
        return new Element(json.get("name").getAsString(), json.get("uuid").getAsString(),
                vec(json, "from", 0), vec(json, "to", 0), vec(json, "origin", 0), vec(json, "rotation", 0),
                json.has("faces") ? json.getAsJsonObject("faces") : new JsonObject());
    }

    private static double[] vec(JsonObject json, String key, double fallback) {
        if (!json.has(key)) {
            return new double[] {fallback, fallback, fallback};
        }
        JsonArray a = json.getAsJsonArray(key);
        return new double[] {a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()};
    }

    /** The face in this direction, or empty if it has no texture. */
    public Optional<CubeFace> face(String direction) {
        if (!faces.has(direction)) {
            return Optional.empty();
        }
        JsonObject face = faces.getAsJsonObject(direction);
        JsonElement texture = face.get("texture");
        if (texture == null || texture.isJsonNull()) {
            return Optional.empty();
        }
        double[][] corners = corners(direction);
        for (int i = 0; i < 4; i++) {
            corners[i] = rotate(corners[i]);
        }
        JsonArray uv = face.getAsJsonArray("uv");
        double u1 = uv.get(0).getAsDouble(), v1 = uv.get(1).getAsDouble();
        double u2 = uv.get(2).getAsDouble(), v2 = uv.get(3).getAsDouble();
        double[][] uvCorners = {{u1, v1}, {u2, v1}, {u2, v2}, {u1, v2}};
        int steps = face.has("rotation") ? Math.floorMod(face.get("rotation").getAsInt() / 90, 4) : 0;
        double[][] uvs = new double[4][];
        for (int i = 0; i < 4; i++) {
            uvs[i] = uvCorners[Math.floorMod(i - steps, 4)];
        }
        return Optional.of(new CubeFace(direction, corners, uvs, textureIndex(texture)));
    }

    private static int textureIndex(JsonElement texture) {
        return texture.getAsJsonPrimitive().isNumber() ? texture.getAsInt() : Integer.parseInt(texture.getAsString());
    }

    private double[][] corners(String direction) {
        double x0 = from[0], y0 = from[1], z0 = from[2], x1 = to[0], y1 = to[1], z1 = to[2];
        return switch (direction) {
            case "north" -> new double[][] {{x1, y1, z0}, {x0, y1, z0}, {x0, y0, z0}, {x1, y0, z0}};
            case "south" -> new double[][] {{x0, y1, z1}, {x1, y1, z1}, {x1, y0, z1}, {x0, y0, z1}};
            case "east" -> new double[][] {{x1, y1, z1}, {x1, y1, z0}, {x1, y0, z0}, {x1, y0, z1}};
            case "west" -> new double[][] {{x0, y1, z0}, {x0, y1, z1}, {x0, y0, z1}, {x0, y0, z0}};
            case "up" -> new double[][] {{x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}};
            case "down" -> new double[][] {{x0, y0, z1}, {x1, y0, z1}, {x1, y0, z0}, {x0, y0, z0}};
            default -> throw new IllegalArgumentException(direction);
        };
    }

    /** Blockbench applies X, then Y, then Z rotation (Euler order ZYX) about the origin. */
    private double[] rotate(double[] p) {
        double x = p[0] - origin[0], y = p[1] - origin[1], z = p[2] - origin[2];
        double a = Math.toRadians(rotation[0]), b = Math.toRadians(rotation[1]), c = Math.toRadians(rotation[2]);
        double y1 = y * Math.cos(a) - z * Math.sin(a), z1 = y * Math.sin(a) + z * Math.cos(a);
        double x2 = x * Math.cos(b) + z1 * Math.sin(b), z2 = -x * Math.sin(b) + z1 * Math.cos(b);
        double x3 = x2 * Math.cos(c) - y1 * Math.sin(c), y3 = x2 * Math.sin(c) + y1 * Math.cos(c);
        return new double[] {x3 + origin[0], y3 + origin[1], z2 + origin[2]};
    }
}
