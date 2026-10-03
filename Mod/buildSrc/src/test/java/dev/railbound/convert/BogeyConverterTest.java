package dev.railbound.convert;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BogeyConverterTest {

    private static final String FACES = "{\"north\":{\"uv\":[0,0,4,4],\"texture\":0},\"south\":{\"uv\":[0,0,4,4],\"texture\":0},"
            + "\"east\":{\"uv\":[0,0,4,4],\"texture\":0},\"west\":{\"uv\":[0,0,4,4],\"texture\":0},"
            + "\"up\":{\"uv\":[0,0,4,4],\"texture\":0},\"down\":{\"uv\":[0,0,4,4],\"texture\":0}}";

    private static String cube(String name, double[] from, double[] to) {
        return "{\"name\":\"" + name + "\",\"uuid\":\"" + name + "\",\"from\":[" + from[0] + "," + from[1] + "," + from[2]
                + "],\"to\":[" + to[0] + "," + to[1] + "," + to[2] + "],\"faces\":" + FACES + "}";
    }

    private static String png() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB), "png", out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    /** A frame bar on top of the bogey block and two wheel groups pivoting on Create's axle positions. */
    private static JsonObject bogey(double frontPivotZ) throws IOException {
        return JsonParser.parseString("{\"resolution\":{\"width\":64,\"height\":64},\"elements\":["
                + cube("bar", new double[] {-16, -16, -8}, new double[] {16, -14, 8}) + ","
                + cube("wheel_f", new double[] {14, -26, frontPivotZ - 6}, new double[] {16, -14, frontPivotZ + 6}) + ","
                + cube("wheel_r", new double[] {14, -26, 10}, new double[] {16, -14, 22}) + ","
                + cube("ghost", new double[] {0, 0, 0}, new double[] {1, 1, 1})
                + "],\"outliner\":[{\"name\":\"bogey\",\"uuid\":\"g\",\"children\":[\"bar\","
                + "{\"name\":\"wheels_front\",\"uuid\":\"w1\",\"origin\":[0,-20," + frontPivotZ + "],\"children\":[\"wheel_f\"]},"
                + "{\"name\":\"wheels_rear\",\"uuid\":\"w2\",\"origin\":[0,-20,16],\"children\":[\"wheel_r\"]},"
                + "{\"name\":\"_preview\",\"uuid\":\"p\",\"children\":[\"ghost\"]}]}],"
                + "\"textures\":[{\"id\":\"0\",\"uv_width\":64,\"uv_height\":64,\"source\":\"data:image/png;base64," + png() + "\"}]}")
                .getAsJsonObject();
    }

    @Test
    void exportsAFrameAndOneWheelset() throws Exception {
        ConvertedBogey out = BogeyConverter.convert("railbound", "coach", bogey(-16));
        assertEquals(Set.of("frame", "wheels"), out.objects().keySet());
    }

    @Test
    void frameIsInCreatesBogeyRenderSpace() throws Exception {
        // Create draws bogeys from the bottom of the track block: model y -16 (bogey block bottom) is 1 block up
        String frame = BogeyConverter.convert("railbound", "coach", bogey(-16)).objects().get("frame");
        assertTrue(frame.contains("v 1 1 -0.5"), frame);
        assertTrue(frame.contains("v -1 1.125 0.5"), frame);
        assertFalse(frame.contains("v 0.0625"), "the preview cube is left out");
    }

    @Test
    void wheelsAreCentredOnTheirAxle() throws Exception {
        String wheels = BogeyConverter.convert("railbound", "coach", bogey(-16)).objects().get("wheels");
        // wheel_f spans y -26..-14 and z -22..-10 around the pivot (0, -20, -16)
        assertTrue(wheels.contains("v 1 -0.375 -0.375"), wheels);
        assertTrue(wheels.contains("v 0.875 0.375 0.375"), wheels);
    }

    @Test
    void rejectsWheelsOffCreatesAxlePositions() throws Exception {
        ConversionException e = assertThrows(ConversionException.class,
                () -> BogeyConverter.convert("railbound", "coach", bogey(-14)));
        assertTrue(e.getMessage().contains("wheels_front"), e.getMessage());
    }

    @Test
    void loaderJsonPointsAtTheBogeyTexture() throws Exception {
        ConvertedBogey out = BogeyConverter.convert("railbound", "coach", bogey(-16));
        JsonObject json = JsonParser.parseString(out.modelJsons().get("frame")).getAsJsonObject();
        assertEquals("railbound:models/bogey/coach/frame.obj", json.get("model").getAsString());
        assertEquals("railbound:bogey/coach", json.getAsJsonObject("textures").get("texture").getAsString());
    }

    @Test
    void batchConvertsOnlyBogeyStylesThatDesignsUse(@TempDir Path root) throws Exception {
        Path designs = Files.createDirectories(root.resolve("designs"));
        Path bogeys = Files.createDirectories(root.resolve("bogeys"));
        Files.writeString(designs.resolve("coach.json"),
                "{\"size\":{\"length\":16},\"bogeys\":[{\"z\":4,\"style\":\"railbound:coach\"},{\"z\":11,\"style\":\"create:standard\"}]}");
        Files.writeString(bogeys.resolve("bogey_coach.bbmodel"), bogey(-16).toString());
        Files.writeString(bogeys.resolve("bogey_steam_truck.bbmodel"), "not even json");

        List<String> converted = BogeyConverter.convertUsed("railbound", designs, bogeys, root.resolve("out"));

        assertEquals(List.of("coach"), converted);
        assertTrue(Files.exists(root.resolve("out/assets/railbound/models/bogey/coach/wheels.obj")));
        assertTrue(Files.exists(root.resolve("out/assets/railbound/textures/bogey/coach.png")));
    }

    @Test
    void failsWhenADesignUsesABogeyStyleWithNoArt(@TempDir Path root) throws Exception {
        Path designs = Files.createDirectories(root.resolve("designs"));
        Files.writeString(designs.resolve("coach.json"), "{\"size\":{\"length\":16},\"bogeys\":[{\"z\":4,\"style\":\"railbound:coach\"}]}");
        ConversionException e = assertThrows(ConversionException.class, () -> BogeyConverter.convertUsed("railbound", designs,
                Files.createDirectories(root.resolve("bogeys")), root.resolve("out")));
        assertTrue(e.getMessage().contains("bogey_coach.bbmodel"), e.getMessage());
    }
}
