package dev.railbound.convert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Converted OBJ parts with loader JSON under {@code models/<folder>/<id>/}, the material, and the texture. */
public record ConvertedParts(String namespace, String folder, String id, Map<String, String> objects,
                             Map<String, String> modelJsons, String material, byte[] texturePng) {

    public void writeTo(Path resourcesRoot) throws IOException {
        Path models = resourcesRoot.resolve("assets/" + namespace + "/models/" + folder + "/" + id);
        Files.createDirectories(models);
        for (Map.Entry<String, String> obj : objects.entrySet()) {
            Files.writeString(models.resolve(obj.getKey() + ".obj"), obj.getValue());
        }
        for (Map.Entry<String, String> json : modelJsons.entrySet()) {
            Files.writeString(models.resolve(json.getKey() + ".json"), json.getValue());
        }
        Files.writeString(models.resolve("trainset.mtl"), material);
        Path textures = resourcesRoot.resolve("assets/" + namespace + "/textures/" + folder);
        Files.createDirectories(textures);
        Files.write(textures.resolve(id + ".png"), texturePng);
    }
}
