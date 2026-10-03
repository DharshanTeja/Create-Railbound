package dev.railbound.convert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** A converted bogey style: frame and wheelset OBJ parts with loader JSON, the material and the texture. */
public record ConvertedBogey(String namespace, String style, Map<String, String> objects, Map<String, String> modelJsons,
                             String material, byte[] texturePng) {

    public void writeTo(Path resourcesRoot) throws IOException {
        Path models = resourcesRoot.resolve("assets/" + namespace + "/models/bogey/" + style);
        Files.createDirectories(models);
        for (Map.Entry<String, String> obj : objects.entrySet()) {
            Files.writeString(models.resolve(obj.getKey() + ".obj"), obj.getValue());
        }
        for (Map.Entry<String, String> json : modelJsons.entrySet()) {
            Files.writeString(models.resolve(json.getKey() + ".json"), json.getValue());
        }
        Files.writeString(models.resolve("trainset.mtl"), material);
        Path textures = resourcesRoot.resolve("assets/" + namespace + "/textures/bogey");
        Files.createDirectories(textures);
        Files.write(textures.resolve(style + ".png"), texturePng);
    }
}
