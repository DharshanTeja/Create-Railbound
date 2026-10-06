package dev.railbound.convert;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Optional;

/**
 * A Blockbench element: a cube, with Minecraft's face corner order, or a mesh (as Blockbench's glTF import makes),
 * whose triangles and quads are kept as Blockbench draws them. Both turn with Blockbench's ZYX rotation about their
 * origin; a mesh's vertices are relative to its origin.
 */
public record Element(String name, String uuid, double[] from, double[] to, double[] origin, double[] rotation,
                      JsonObject faceJson, String type, JsonObject vertices) {

    public static final String[] DIRECTIONS = {"north", "south", "east", "west", "up", "down"};

    public static Element of(JsonObject json) {
        return new Element(json.get("name").getAsString(), json.get("uuid").getAsString(),
                vec(json, "from", 0), vec(json, "to", 0), vec(json, "origin", 0), vec(json, "rotation", 0),
                json.has("faces") ? json.getAsJsonObject("faces") : new JsonObject(),
                json.has("type") ? json.get("type").getAsString() : "cube",
                json.has("vertices") ? json.getAsJsonObject("vertices") : new JsonObject());
    }

    private static double[] vec(JsonObject json, String key, double fallback) {
        if (!json.has(key)) {
            return new double[] {fallback, fallback, fallback};
        }
        JsonArray a = json.getAsJsonArray(key);
        return new double[] {a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()};
    }

    public boolean isMesh() {
        return type.equals("mesh");
    }

    /** Every textured face: a cube's sides, or a mesh's triangles and quads. */
    public java.util.List<CubeFace> faces() {
        java.util.List<CubeFace> out = new java.util.ArrayList<>();
        if (isMesh()) {
            for (String key : faceJson.keySet()) {
                meshFace(faceJson.getAsJsonObject(key)).ifPresent(out::add);
            }
            return out;
        }
        for (String direction : DIRECTIONS) {
            face(direction).ifPresent(out::add);
        }
        return out;
    }

    /** What is wrong with this element for conversion, if anything. */
    public Optional<String> problem() {
        if (!isMesh()) {
            return Optional.empty();
        }
        for (String key : faceJson.keySet()) {
            int corners = faceJson.getAsJsonObject(key).getAsJsonArray("vertices").size();
            if (corners > 4) {
                return Optional.of("mesh '" + name + "' has a face with " + corners
                        + " corners; meshes may only use triangles and quads (triangulate it in Blockbench or Blender)");
            }
        }
        return Optional.empty();
    }

    /**
     * A mesh face as a corner list the OBJ writer walks in reverse, counter-clockwise from the front: Blockbench's own
     * front (three.js: counter-clockwise), a triangle written as a quad with its last corner doubled.
     */
    private Optional<CubeFace> meshFace(JsonObject face) {
        JsonElement texture = face.get("texture");
        JsonArray keys = face.getAsJsonArray("vertices");
        if (texture == null || texture.isJsonNull() || keys.size() < 3 || keys.size() > 4) {
            return Optional.empty();
        }
        java.util.List<String> stored = new java.util.ArrayList<>();
        keys.forEach(k -> stored.add(k.getAsString()));
        java.util.List<String> order = stored.size() == 4 ? sortedQuad(stored) : stored;
        // reversed, so the writer's top-left, bottom-left, bottom-right, top-right walk goes round the front
        int[] pick = order.size() == 3 ? new int[] {0, 2, 2, 1} : new int[] {0, 3, 2, 1};
        JsonObject uvs = face.getAsJsonObject("uv");
        double[][] corners = new double[4][], uv = new double[4][];
        for (int i = 0; i < 4; i++) {
            String key = order.get(pick[i]);
            double[] local = local(key);
            corners[i] = rotate(new double[] {origin[0] + local[0], origin[1] + local[1], origin[2] + local[2]});
            JsonArray u = uvs.getAsJsonArray(key);
            uv[i] = new double[] {u.get(0).getAsDouble(), u.get(1).getAsDouble()};
        }
        return Optional.of(new CubeFace("mesh", corners, uv, textureIndex(texture)));
    }

    private double[] local(String key) {
        JsonArray v = vertices.getAsJsonArray(key);
        return new double[] {v.get(0).getAsDouble(), v.get(1).getAsDouble(), v.get(2).getAsDouble()};
    }

    /** Blockbench's MeshFace.getSortedVertices: a quad's corners in the order it draws them, (0,1,2) and (0,2,3). */
    private java.util.List<String> sortedQuad(java.util.List<String> v) {
        if (otherSide(local(v.get(1)), local(v.get(2)), local(v.get(0)), local(v.get(3)))) {
            return java.util.List.of(v.get(2), v.get(0), v.get(1), v.get(3));
        }
        if (otherSide(local(v.get(0)), local(v.get(1)), local(v.get(2)), local(v.get(3)))) {
            return java.util.List.of(v.get(0), v.get(2), v.get(1), v.get(3));
        }
        return v;
    }

    /** Whether check lies on the far side of the line base1-base2 from top (Blockbench's test). */
    private static boolean otherSide(double[] base1, double[] base2, double[] top, double[] check) {
        double[] line = sub(base2, base1);
        double lengthSq = dot(line, line);
        double t = lengthSq == 0 ? 0 : dot(sub(top, base1), line) / lengthSq;
        double[] closest = {base1[0] + line[0] * t, base1[1] + line[1] * t, base1[2] + line[2] * t};
        double[] normal = sub(closest, top);
        return dot(normal, sub(check, base2)) > 0;
    }

    private static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    /** The face in this direction, or empty if it has no texture. */
    public Optional<CubeFace> face(String direction) {
        if (!faceJson.has(direction)) {
            return Optional.empty();
        }
        JsonObject face = faceJson.getAsJsonObject(direction);
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
