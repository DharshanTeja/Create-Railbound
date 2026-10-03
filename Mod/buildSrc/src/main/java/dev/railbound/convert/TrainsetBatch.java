package dev.railbound.convert;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Converts every design that has art: `<designs>/<id>.json` pairs with `<art>/<id>/<id>.bbmodel`. */
public final class TrainsetBatch {

    public record Result(List<String> converted, List<String> withoutModel) {}

    private TrainsetBatch() {}

    public static Result run(String namespace, Path designs, Path art, Path out) throws IOException, ConversionException {
        List<String> converted = new ArrayList<>();
        List<String> withoutModel = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        List<Path> designFiles;
        try (Stream<Path> files = Files.list(designs)) {
            designFiles = files.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        }
        for (Path designFile : designFiles) {
            String id = designFile.getFileName().toString().replaceFirst("\\.json$", "");
            Path model = art.resolve(id).resolve(id + ".bbmodel");
            if (!Files.exists(model)) {
                withoutModel.add(id);
                continue;
            }
            JsonObject design = JsonParser.parseString(Files.readString(designFile)).getAsJsonObject();
            try {
                TrainsetConverter.convert(namespace, id, JsonParser.parseString(Files.readString(model)).getAsJsonObject(), design)
                        .writeTo(out);
                converted.add(id);
            } catch (ConversionException e) {
                problems.add(e.getMessage());
            }
        }
        if (!problems.isEmpty()) {
            throw new ConversionException(String.join("\n", problems));
        }
        return new Result(converted, withoutModel);
    }
}
