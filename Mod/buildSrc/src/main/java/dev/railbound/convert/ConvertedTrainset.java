package dev.railbound.convert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Everything the converter makes for one design: OBJ text and loader JSON per part, the material, the texture. */
public record ConvertedTrainset(String namespace, String id, Map<String, String> objects, Map<String, String> modelJsons,
                                String material, byte[] texturePng) {

    /** Writes into a resources root (the folder holding `assets/`). */
    public void writeTo(Path resourcesRoot) throws IOException {
        Path models = resourcesRoot.resolve("assets/" + namespace + "/models/trainset/" + id);
        Files.createDirectories(models);
        for (Map.Entry<String, String> obj : objects.entrySet()) {
            Files.writeString(models.resolve(obj.getKey() + ".obj"), obj.getValue());
        }
        for (Map.Entry<String, String> json : modelJsons.entrySet()) {
            Files.writeString(models.resolve(json.getKey() + ".json"), json.getValue());
        }
        Files.writeString(models.resolve("trainset.mtl"), material);
        Path textures = resourcesRoot.resolve("assets/" + namespace + "/textures/trainset");
        Files.createDirectories(textures);
        Files.write(textures.resolve(id + ".png"), texturePng);
    }
}
