package dev.railbound.convert;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TrainsetBatchTest {

    private static final String DESIGN = "{\"size\":{\"length\":1,\"width\":3,\"height\":4},\"doors\":[]}";

    private static final String MODEL = model();

    private static String model() {
        try {
            java.io.ByteArrayOutputStream png = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB), "png", png);
            return "{\"resolution\":{\"width\":16,\"height\":16},"
                    + "\"elements\":[{\"name\":\"floor\",\"uuid\":\"a\",\"from\":[-8,-2,-8],\"to\":[8,0,8],"
                    + "\"faces\":{\"up\":{\"uv\":[0,0,16,16],\"texture\":0}}}],"
                    + "\"outliner\":[{\"name\":\"coach\",\"uuid\":\"g\",\"children\":[\"a\"]}],"
                    + "\"textures\":[{\"id\":\"0\",\"uv_width\":16,\"uv_height\":16,\"source\":\"data:image/png;base64,"
                    + java.util.Base64.getEncoder().encodeToString(png.toByteArray()) + "\"}]}";
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    @Test
    void convertsEveryDesignThatHasAModelAndSkipsTheRest(@TempDir Path root) throws Exception {
        Path designs = Files.createDirectories(root.resolve("designs"));
        Path art = Files.createDirectories(root.resolve("art"));
        Path out = root.resolve("out");
        Files.writeString(designs.resolve("tiny.json"), DESIGN);
        Files.writeString(designs.resolve("plain.json"), DESIGN);
        Files.createDirectories(art.resolve("tiny"));
        Files.writeString(art.resolve("tiny/tiny.bbmodel"), MODEL);

        TrainsetBatch.Result result = TrainsetBatch.run("railbound", designs, art, out);

        assertEquals(List.of("tiny"), result.converted());
        assertEquals(List.of("plain"), result.withoutModel());
        assertTrue(Files.exists(out.resolve("assets/railbound/models/trainset/tiny/body.obj")));
        assertTrue(Files.exists(out.resolve("assets/railbound/models/trainset/tiny/body.json")));
        assertTrue(Files.exists(out.resolve("assets/railbound/models/trainset/tiny/trainset.mtl")));
        assertTrue(Files.exists(out.resolve("assets/railbound/textures/trainset/tiny.png")));
    }

    @Test
    void modelsWithoutADesignAreIgnored(@TempDir Path root) throws Exception {
        Path designs = Files.createDirectories(root.resolve("designs"));
        Path art = Files.createDirectories(root.resolve("art/loco"));
        Files.writeString(art.resolve("loco.bbmodel"), MODEL);

        TrainsetBatch.Result result = TrainsetBatch.run("railbound", designs, root.resolve("art"), root.resolve("out"));

        assertEquals(List.of(), result.converted());
    }

    @Test
    void reportsEveryBrokenModel(@TempDir Path root) throws Exception {
        Path designs = Files.createDirectories(root.resolve("designs"));
        Path art = Files.createDirectories(root.resolve("art/tiny"));
        Files.writeString(designs.resolve("tiny.json"), "{\"size\":{\"length\":1},\"doors\":[{\"part\":\"door_x\",\"pos\":[1,1,0]}]}");
        Files.writeString(art.resolve("tiny.bbmodel"), MODEL);

        ConversionException e = assertThrows(ConversionException.class,
                () -> TrainsetBatch.run("railbound", designs, root.resolve("art"), root.resolve("out")));
        assertTrue(e.getMessage().contains("door_x"), e.getMessage());
    }
}
