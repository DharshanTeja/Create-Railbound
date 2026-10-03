package dev.railbound.convert;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Converts a bogey style's `.bbmodel` (centred on the bogey, carriage y coordinates) into a static `frame` part and
 * one `wheels` part centred on its axle. Groups named `wheels*` are wheelsets; their pivots must sit on Create's
 * axle positions (y -20, z ±16), because the renderer spins the wheels there.
 */
public final class BogeyConverter {
    /** Create draws bogeys from the bottom of the track block, 2 blocks below the carriage floor. */
    private static final double RENDER_ORIGIN_Y = -32;
    private static final double AXLE_Y = -20;
    private static final double AXLE_Z = 16;
    private static final double EPSILON = 1e-6;

    private BogeyConverter() {}

    public static ConvertedBogey convert(String namespace, String style, JsonObject model) throws ConversionException {
        List<String> problems = new ArrayList<>();
        ModelSource source = new ModelSource(model);
        List<CubeFace> frame = new ArrayList<>();
        Map<String, List<CubeFace>> wheelsets = new LinkedHashMap<>();
        Set<Integer> texturesUsed = new TreeSet<>();
        for (Element element : source.elements()) {
            List<String> path = source.pathOf(element);
            if (Parts.partFor(path).isEmpty()) {
                continue;
            }
            Optional<String> wheelset = path.stream().filter(g -> g.startsWith("wheels")).findFirst();
            List<CubeFace> faces = wheelset.map(w -> wheelsets.computeIfAbsent(w, k -> new ArrayList<>())).orElse(frame);
            for (String direction : Element.DIRECTIONS) {
                element.face(direction).ifPresent(face -> {
                    faces.add(face);
                    texturesUsed.add(face.texture());
                });
            }
        }

        if (frame.isEmpty()) {
            problems.add("the bogey has no frame cubes");
        }
        if (wheelsets.isEmpty()) {
            problems.add("the bogey has no 'wheels' groups");
        }
        for (String wheelset : wheelsets.keySet()) {
            double[] pivot = source.originOf(wheelset).orElse(new double[] {Double.NaN, 0, 0});
            if (Math.abs(pivot[0]) > EPSILON || Math.abs(pivot[1] - AXLE_Y) > EPSILON
                    || Math.abs(Math.abs(pivot[2]) - AXLE_Z) > EPSILON) {
                problems.add("group '" + wheelset + "' must pivot on an axle at (0, " + AXLE_Y + ", ±" + AXLE_Z + ")");
            }
        }
        ModelSource.Texture texture = source.texture(texturesUsed, problems);
        if (!problems.isEmpty()) {
            throw new ConversionException("bogey " + style + ": " + String.join("; ", problems));
        }

        Map<String, String> objects = new LinkedHashMap<>();
        objects.put("frame", ModelSource.obj(frame, c -> new double[] {c[0] / 16, (c[1] - RENDER_ORIGIN_Y) / 16, c[2] / 16}, texture));
        String first = wheelsets.keySet().iterator().next();
        double[] pivot = source.originOf(first).orElseThrow();
        objects.put("wheels", ModelSource.obj(wheelsets.get(first),
                c -> new double[] {(c[0] - pivot[0]) / 16, (c[1] - pivot[1]) / 16, (c[2] - pivot[2]) / 16}, texture));
        Map<String, String> modelJsons = new LinkedHashMap<>();
        for (String part : objects.keySet()) {
            modelJsons.put(part, ModelSource.loaderJson(namespace, "bogey", style, part));
        }
        return new ConvertedBogey(namespace, style, objects, modelJsons, ModelSource.MATERIAL, texture.png());
    }

    /** Converts `<bogeys>/bogey_<style>.bbmodel` for every style in our namespace that some design uses. */
    public static List<String> convertUsed(String namespace, Path designs, Path bogeys, Path out)
            throws IOException, ConversionException {
        Set<String> styles = new TreeSet<>();
        try (Stream<Path> files = Files.list(designs)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                JsonObject design = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                if (!design.has("bogeys")) {
                    continue;
                }
                for (JsonElement bogey : design.getAsJsonArray("bogeys")) {
                    JsonObject spec = bogey.getAsJsonObject();
                    if (spec.has("style") && spec.get("style").getAsString().startsWith(namespace + ":")) {
                        styles.add(spec.get("style").getAsString().substring(namespace.length() + 1));
                    }
                }
            }
        }
        List<String> problems = new ArrayList<>();
        List<String> converted = new ArrayList<>();
        for (String style : styles) {
            Path model = bogeys.resolve("bogey_" + style + ".bbmodel");
            if (!Files.exists(model)) {
                problems.add("a design uses bogey style " + namespace + ":" + style + " but art/bogeys/bogey_" + style
                        + ".bbmodel does not exist");
                continue;
            }
            try {
                convert(namespace, style, JsonParser.parseString(Files.readString(model)).getAsJsonObject()).writeTo(out);
                converted.add(style);
            } catch (ConversionException e) {
                problems.add(e.getMessage());
            }
        }
        if (!problems.isEmpty()) {
            throw new ConversionException(String.join("\n", problems));
        }
        return converted;
    }
}
