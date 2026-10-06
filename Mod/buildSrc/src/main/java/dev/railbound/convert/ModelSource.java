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

    /** Where a texture lands in the exported one: its uv offset and scale (identity when absent). */
    public record Placement(double offsetU, double offsetV, double scaleU, double scaleV) {}

    public record Texture(byte[] png, double uvWidth, double uvHeight, Map<Integer, Placement> placements) {
        /** A face uv on texture {@code texture}, in the exported texture's uv units. */
        public double[] toAtlas(int texture, double[] uv) {
            Placement p = placements.get(texture);
            return p == null ? uv : new double[] {p.offsetU() + uv[0] * p.scaleU(), p.offsetV() + uv[1] * p.scaleV()};
        }
    }

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

    /** A texture the faces use: its image and the size of its uv space. */
    private record Source(BufferedImage image, double uvWidth, double uvHeight) {}

    /**
     * The one texture the exported faces use. A model with several textures (a glTF import usually has one per
     * material) has them packed into one atlas, and a texture over the size budget is scaled down to fit, so faces
     * keep their looks; the returned texture maps each face's uvs into it. Null (with problems) if broken.
     */
    public Texture texture(java.util.Collection<CubeFace> faces, List<String> problems) {
        Set<Integer> used = new java.util.TreeSet<>();
        faces.forEach(f -> used.add(f.texture()));
        if (used.isEmpty()) {
            problems.add("the model has no textured faces");
            return null;
        }
        Map<Integer, Source> sources = new java.util.LinkedHashMap<>();
        for (int index : used) {
            Source source = source(index, problems);
            if (source == null) {
                return null;
            }
            sources.put(index, source);
        }
        Set<Integer> offTexture = new java.util.TreeSet<>();
        for (CubeFace face : faces) {
            Source source = sources.get(face.texture());
            for (double[] uv : face.uvs()) {
                if (uv[0] < -UV_SLACK || uv[1] < -UV_SLACK || uv[0] > source.uvWidth() + UV_SLACK
                        || uv[1] > source.uvHeight() + UV_SLACK) {
                    offTexture.add(face.texture());
                }
            }
        }
        for (int index : offTexture) {
            problems.add("faces on texture " + index + " have uvs outside it (tiled or wrapped uvs aren't supported; "
                    + "keep every uv inside its texture)");
        }
        if (!offTexture.isEmpty()) {
            return null;
        }
        if (sources.size() == 1) {
            int index = used.iterator().next();
            Source only = sources.get(index);
            double scale = 1;
            while (only.image().getWidth() * scale > MAX_TEXTURE || only.image().getHeight() * scale > MAX_TEXTURE) {
                scale /= 2;
            }
            // the same uv space at any scale, so faces need no remapping
            return new Texture(scale == 1 ? pngs.get(index) : png(scaled(only.image(), scale)),
                    only.uvWidth(), only.uvHeight(), Map.of());
        }
        return atlas(sources, problems);
    }

    /** UVs may stray this far (texture pixels) past their texture's edge: rounding only. */
    private static final double UV_SLACK = 0.01;
    /** Textures in an atlas are scaled down at most this far before giving up. */
    private static final double MIN_SCALE = 1 / 16.0;

    /** Each texture's PNG as stored, so a single texture within budget is exported byte for byte. */
    private final Map<Integer, byte[]> pngs = new HashMap<>();

    private Source source(int index, List<String> problems) {
        JsonObject texture = null;
        JsonArray textures = model.has("textures") ? model.getAsJsonArray("textures") : new JsonArray();
        for (JsonElement t : textures) {
            if (t.getAsJsonObject().has("id") && t.getAsJsonObject().get("id").getAsString().equals(Integer.toString(index))) {
                texture = t.getAsJsonObject();
            }
        }
        if (texture == null && index >= 0 && index < textures.size()) {
            texture = textures.get(index).getAsJsonObject();
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
            problems.add("texture " + index + " is not a readable PNG: " + e.getMessage());
            return null;
        }
        if (image == null) {
            problems.add("texture " + index + " is not a readable PNG");
            return null;
        }
        pngs.put(index, png);
        return new Source(image,
                texture.has("uv_width") ? texture.get("uv_width").getAsDouble() : image.getWidth(),
                texture.has("uv_height") ? texture.get("uv_height").getAsDouble() : image.getHeight());
    }

    /**
     * Packs several textures into one square atlas, the smallest power of two that holds them, scaling them all down
     * by halves until they fit the budget. Rows of textures, tallest first.
     */
    private Texture atlas(Map<Integer, Source> sources, List<String> problems) {
        List<Integer> order = new ArrayList<>(sources.keySet());
        order.sort((a, b) -> Integer.compare(sources.get(b).image().getHeight(), sources.get(a).image().getHeight()));
        for (double scale = 1; scale >= MIN_SCALE; scale /= 2) {
            for (int size = 16; size <= MAX_TEXTURE; size *= 2) {
                Map<Integer, int[]> spots = pack(order, sources, scale, size);
                if (spots == null) {
                    continue;
                }
                BufferedImage atlas = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g = atlas.createGraphics();
                Map<Integer, Placement> placements = new HashMap<>();
                for (int index : order) {
                    Source source = sources.get(index);
                    int[] spot = spots.get(index);
                    g.drawImage(scale == 1 ? source.image() : scaled(source.image(), scale), spot[0], spot[1], null);
                    placements.put(index, new Placement(spot[0], spot[1], spot[2] / source.uvWidth(), spot[3] / source.uvHeight()));
                }
                g.dispose();
                return new Texture(png(atlas), size, size, placements);
            }
        }
        problems.add("the model's " + sources.size() + " textures don't fit one " + MAX_TEXTURE + "x" + MAX_TEXTURE
                + " texture even at 1/16 of their size; use fewer or smaller textures");
        return null;
    }

    /** Where each texture goes (x, y, width, height) in a square atlas this size, or null if they don't fit. */
    private static Map<Integer, int[]> pack(List<Integer> order, Map<Integer, Source> sources, double scale, int size) {
        Map<Integer, int[]> spots = new HashMap<>();
        int x = 0, y = 0, rowHeight = 0;
        for (int index : order) {
            BufferedImage image = sources.get(index).image();
            int w = Math.max(1, (int) Math.round(image.getWidth() * scale));
            int h = Math.max(1, (int) Math.round(image.getHeight() * scale));
            if (w > size || h > size) {
                return null;
            }
            if (x + w > size) {
                x = 0;
                y += rowHeight;
                rowHeight = 0;
            }
            if (y + h > size) {
                return null;
            }
            spots.put(index, new int[] {x, y, w, h});
            x += w;
            rowHeight = Math.max(rowHeight, h);
        }
        return spots;
    }

    /** The image at a smaller scale, averaging the pixels it merges. */
    private static BufferedImage scaled(BufferedImage image, double scale) {
        int w = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(image.getHeight() * scale));
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = out.createGraphics();
        g.drawImage(image.getScaledInstance(w, h, java.awt.Image.SCALE_AREA_AVERAGING), 0, 0, null);
        g.dispose();
        return out;
    }

    private static byte[] png(BufferedImage image) {
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
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
            for (double[] faceUv : face.uvs()) {
                double[] uv = texture.toAtlas(face.texture(), faceUv);
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

    static double[] normal(double[][] c) {
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
