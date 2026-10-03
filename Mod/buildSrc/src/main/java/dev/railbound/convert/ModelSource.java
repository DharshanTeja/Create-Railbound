package dev.railbound.convert;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

/** A parsed `.bbmodel`: cubes with their group paths and group pivots, plus the shared texture and OBJ output. */
public final class ModelSource {
    public static final int MAX_TEXTURE = 512;
    public static final String MATERIAL = "newmtl trainset\nmap_Kd #texture\n";
    private static final double EPSILON = 1e-6;

    private final JsonObject model;
    private final Map<String, List<String>> groupPaths = new HashMap<>();
    private final Map<String, double[]> groupOrigins = new HashMap<>();

    public ModelSource(JsonObject model) {
        this.model = model;
        Map<String, JsonObject> groups = new HashMap<>();
        if (model.has("groups")) {
            for (JsonElement g : model.getAsJsonArray("groups")) {
                groups.put(g.getAsJsonObject().get("uuid").getAsString(), g.getAsJsonObject());
            }
        }
        collect(model.getAsJsonArray("outliner"), new ArrayList<>(), groups);
    }

    public record Texture(byte[] png, double uvWidth, double uvHeight) {}

    public List<Element> elements() {
        List<Element> out = new ArrayList<>();
        for (JsonElement json : model.getAsJsonArray("elements")) {
            out.add(Element.of(json.getAsJsonObject()));
        }
        return out;
    }

    public List<String> pathOf(Element element) {
        return groupPaths.getOrDefault(element.uuid(), List.of());
    }

    public Optional<double[]> originOf(String groupName) {
        return Optional.ofNullable(groupOrigins.get(groupName));
    }

    private void collect(JsonArray nodes, List<String> path, Map<String, JsonObject> groups) {
        for (JsonElement node : nodes) {
            if (node.isJsonPrimitive()) {
                groupPaths.put(node.getAsString(), List.copyOf(path));
                continue;
            }
            JsonObject group = node.getAsJsonObject();
            JsonObject full = groups.getOrDefault(group.get("uuid").getAsString(), group);
            String name = group.has("name") ? group.get("name").getAsString() : full.has("name") ? full.get("name").getAsString() : "";
            JsonObject withOrigin = group.has("origin") ? group : full;
            if (withOrigin.has("origin")) {
                JsonArray o = withOrigin.getAsJsonArray("origin");
                groupOrigins.putIfAbsent(name, new double[] {o.get(0).getAsDouble(), o.get(1).getAsDouble(), o.get(2).getAsDouble()});
            }
            List<String> child = new ArrayList<>(path);
            child.add(name);
            collect(group.getAsJsonArray("children"), child, groups);
        }
    }

    /** The one texture the exported faces use, checked against the size budget; null (with problems) if broken. */
    public Texture texture(Set<Integer> used, List<String> problems) {
        if (used.size() != 1) {
            problems.add("the model must use exactly one texture, but uses " + used.size());
            return null;
        }
        int reference = used.iterator().next();
        JsonObject texture = null;
        JsonArray textures = model.getAsJsonArray("textures");
        for (JsonElement t : textures) {
            if (t.getAsJsonObject().has("id") && t.getAsJsonObject().get("id").getAsString().equals(Integer.toString(reference))) {
                texture = t.getAsJsonObject();
            }
        }
        if (texture == null && reference >= 0 && reference < textures.size()) {
            texture = textures.get(reference).getAsJsonObject();
        }
        if (texture == null) {
            problems.add("a face refers to a texture the model does not have");
            return null;
        }
        byte[] png = Base64.getDecoder().decode(texture.get("source").getAsString().split(",", 2)[1]);
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(png));
        } catch (IOException e) {
            problems.add("the texture is not a readable PNG: " + e.getMessage());
            return null;
        }
        if (image.getWidth() > MAX_TEXTURE || image.getHeight() > MAX_TEXTURE) {
            problems.add("the texture is " + image.getWidth() + "x" + image.getHeight()
                    + "; textures may be at most " + MAX_TEXTURE + "x" + MAX_TEXTURE);
            return null;
        }
        return new Texture(png,
                texture.has("uv_width") ? texture.get("uv_width").getAsDouble() : image.getWidth(),
                texture.has("uv_height") ? texture.get("uv_height").getAsDouble() : image.getHeight());
    }

    /** OBJ text for the faces, with `toBlock` taking model-unit corners to block-space vertices. */
    public static String obj(List<CubeFace> faces, UnaryOperator<double[]> toBlock, Texture texture) {
        StringBuilder out = new StringBuilder("mtllib trainset.mtl\nusemtl trainset\n");
        int index = 1;
        for (CubeFace face : faces) {
            double[][] c = face.corners();
            for (double[] corner : c) {
                double[] b = toBlock.apply(corner);
                out.append("v ").append(num(b[0])).append(' ').append(num(b[1])).append(' ').append(num(b[2])).append('\n');
            }
            // NeoForge reads vt v as Minecraft's top-down V (we leave flip_v off), so no OBJ-style flip here
            for (double[] uv : face.uvs()) {
                out.append("vt ").append(num(uv[0] / texture.uvWidth())).append(' ').append(num(uv[1] / texture.uvHeight())).append('\n');
            }
            double[] n = normal(c);
            out.append("vn ").append(num(n[0])).append(' ').append(num(n[1])).append(' ').append(num(n[2])).append('\n');
            // top-left, bottom-left, bottom-right, top-right: counter-clockwise seen from outside
            int tl = index, tr = index + 1, br = index + 2, bl = index + 3;
            int vn = (index - 1) / 4 + 1;
            out.append("f ").append(tl).append('/').append(tl).append('/').append(vn)
                    .append(' ').append(bl).append('/').append(bl).append('/').append(vn)
                    .append(' ').append(br).append('/').append(br).append('/').append(vn)
                    .append(' ').append(tr).append('/').append(tr).append('/').append(vn).append('\n');
            index += 4;
        }
        return out.toString();
    }

    private static double[] normal(double[][] c) {
        double[] a = {c[3][0] - c[0][0], c[3][1] - c[0][1], c[3][2] - c[0][2]};
        double[] b = {c[1][0] - c[0][0], c[1][1] - c[0][1], c[1][2] - c[0][2]};
        double[] n = {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
        double len = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
        return len < EPSILON ? new double[] {0, 1, 0} : new double[] {n[0] / len, n[1] / len, n[2] / len};
    }

    private static String num(double value) {
        BigDecimal d = BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP).stripTrailingZeros();
        if (d.signum() == 0) {
            return "0";
        }
        return d.scale() < 0 ? d.setScale(0).toPlainString() : d.toPlainString();
    }

    /** NeoForge OBJ loader JSON for one part under {@code models/<folder>/<id>/}, textured by {@code <folder>/<id>}. */
    public static String loaderJson(String namespace, String folder, String id, String part) {
        JsonObject json = new JsonObject();
        json.addProperty("loader", "neoforge:obj");
        json.addProperty("model", namespace + ":models/" + folder + "/" + id + "/" + part + ".obj");
        json.addProperty("automatic_culling", false);
        json.addProperty("flip_v", false);
        json.addProperty("emissive_ambient", false);
        JsonObject textures = new JsonObject();
        textures.addProperty("texture", namespace + ":" + folder + "/" + id);
        textures.addProperty("particle", namespace + ":" + folder + "/" + id);
        json.add("textures", textures);
        return new GsonBuilder().setPrettyPrinting().create().toJson(json);
    }
}
