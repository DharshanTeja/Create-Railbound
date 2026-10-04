package dev.railbound.convert;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Converts a coupling style's `.bbmodel` (the knuckle coupler at a carriage end) into one OBJ part per top-level
 * group, each centred on its group's pivot so the mod can place and turn it at the carriage end: `head` (the knuckle,
 * its pivot on the coupling face) and `shank` (modelled one block long out from the carriage end, sized to the
 * coupler's length), and optionally `bellows` (one gangway fold, its pivot at the middle of the gangway floor) and
 * `flap` (the gangway floor plate, hinged at its pivot). The model looks along +z, away from its carriage.
 */
public final class CouplingConverter {
    public static final String FOLDER = "coupling";
    private static final Set<String> REQUIRED = Set.of("head", "shank");
    private static final Set<String> OPTIONAL = Set.of("bellows", "flap");

    private CouplingConverter() {}

    public static ConvertedParts convert(String namespace, String style, JsonObject model) throws ConversionException {
        List<String> problems = new ArrayList<>();
        ModelSource source = new ModelSource(model);
        Map<String, List<CubeFace>> parts = new LinkedHashMap<>();
        Set<Integer> texturesUsed = new TreeSet<>();
        Set<String> unknown = new TreeSet<>();
        for (Element element : source.elements()) {
            List<String> path = source.pathOf(element);
            if (Parts.partFor(path).isEmpty()) {
                continue;
            }
            String part = path.isEmpty() ? "(no group)" : path.get(0);
            if (!REQUIRED.contains(part) && !OPTIONAL.contains(part)) {
                unknown.add(part);
                continue;
            }
            List<CubeFace> faces = parts.computeIfAbsent(part, k -> new ArrayList<>());
            for (String direction : Element.DIRECTIONS) {
                element.face(direction).ifPresent(face -> {
                    faces.add(face);
                    texturesUsed.add(face.texture());
                });
            }
        }
        if (!unknown.isEmpty()) {
            problems.add("cubes in " + unknown + " belong to no part; parts are head, shank, bellows and flap");
        }
        for (String required : new TreeSet<>(REQUIRED)) {
            if (!parts.containsKey(required)) {
                problems.add("the coupling has no '" + required + "' group");
            }
        }
        ModelSource.Texture texture = source.texture(texturesUsed, problems);
        if (!problems.isEmpty()) {
            throw new ConversionException("coupling " + style + ": " + String.join("; ", problems));
        }

        Map<String, String> objects = new LinkedHashMap<>();
        Map<String, String> modelJsons = new LinkedHashMap<>();
        for (Map.Entry<String, List<CubeFace>> part : parts.entrySet()) {
            double[] pivot = source.originOf(part.getKey()).orElse(new double[] {0, 0, 0});
            objects.put(part.getKey(), ModelSource.obj(part.getValue(),
                    c -> new double[] {(c[0] - pivot[0]) / 16, (c[1] - pivot[1]) / 16, (c[2] - pivot[2]) / 16}, texture));
            modelJsons.put(part.getKey(), ModelSource.loaderJson(namespace, FOLDER, style, part.getKey()));
        }
        return new ConvertedParts(namespace, FOLDER, style, objects, modelJsons, ModelSource.MATERIAL, texture.png());
    }

    /** Converts every `<art>/coupling_<style>.bbmodel`; none (or no folder) is fine. */
    public static List<String> convertAll(String namespace, Path art, Path out) throws IOException, ConversionException {
        List<String> converted = new ArrayList<>();
        if (!Files.isDirectory(art)) {
            return converted;
        }
        List<String> problems = new ArrayList<>();
        try (Stream<Path> files = Files.list(art)) {
            for (Path file : files.sorted().toList()) {
                String name = file.getFileName().toString();
                if (!name.startsWith("coupling_") || !name.endsWith(".bbmodel")) {
                    continue;
                }
                String style = name.substring("coupling_".length(), name.length() - ".bbmodel".length());
                try {
                    convert(namespace, style, JsonParser.parseString(Files.readString(file)).getAsJsonObject()).writeTo(out);
                    converted.add(style);
                } catch (ConversionException e) {
                    problems.add(e.getMessage());
                }
            }
        }
        if (!problems.isEmpty()) {
            throw new ConversionException(String.join("\n", problems));
        }
        return converted;
    }
}
