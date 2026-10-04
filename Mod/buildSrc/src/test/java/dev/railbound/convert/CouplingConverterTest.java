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
import java.util.List;
import java.util.Base64;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CouplingConverterTest {

    private static final String FACES = "{\"north\":{\"uv\":[0,0,4,4],\"texture\":0},\"south\":{\"uv\":[0,0,4,4],\"texture\":0},"
            + "\"east\":{\"uv\":[0,0,4,4],\"texture\":0},\"west\":{\"uv\":[0,0,4,4],\"texture\":0},"
            + "\"up\":{\"uv\":[0,0,4,4],\"texture\":0},\"down\":{\"uv\":[0,0,4,4],\"texture\":0}}";

    private static String cube(String name, double[] from, double[] to) {
        return "{\"name\":\"" + name + "\",\"uuid\":\"" + name + "\",\"from\":[" + from[0] + "," + from[1] + "," + from[2]
                + "],\"to\":[" + to[0] + "," + to[1] + "," + to[2] + "],\"faces\":" + FACES + "}";
    }

    private static String group(String name, double[] origin, String child) {
        return "{\"name\":\"" + name + "\",\"uuid\":\"g_" + name + "\",\"origin\":[" + origin[0] + "," + origin[1] + "," + origin[2]
                + "],\"children\":[\"" + child + "\"]}";
    }

    private static String png() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB), "png", out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    /** A knuckle head at the coupling face, a shank one block long from the carriage end, and a preview. */
    private static JsonObject coupling(String... extraGroups) throws IOException {
        StringBuilder elements = new StringBuilder()
                .append(cube("head_c", new double[] {-3, 10, 20}, new double[] {3, 16, 25})).append(',')
                .append(cube("shank_c", new double[] {-1.5, 11.5, 0}, new double[] {1.5, 14.5, 16})).append(',')
                .append(cube("ghost", new double[] {0, 0, 0}, new double[] {1, 1, 1}));
        StringBuilder outliner = new StringBuilder()
                .append(group("head", new double[] {0, 13, 25}, "head_c")).append(',')
                .append(group("shank", new double[] {0, 13, 0}, "shank_c")).append(',')
                .append(group("_preview", new double[] {0, 0, 0}, "ghost"));
        for (String extra : extraGroups) {
            elements.append(',').append(cube(extra + "_c", new double[] {0, 0, 0}, new double[] {2, 2, 2}));
            outliner.append(',').append(group(extra, new double[] {0, 0, 0}, extra + "_c"));
        }
        return JsonParser.parseString("{\"resolution\":{\"width\":32,\"height\":32},\"elements\":[" + elements
                + "],\"outliner\":[" + outliner + "],\"textures\":[{\"id\":\"0\",\"uv_width\":32,\"uv_height\":32,"
                + "\"source\":\"data:image/png;base64," + png() + "\"}]}").getAsJsonObject();
    }

    @Test
    void exportsEachPartGroup() throws Exception {
        assertEquals(Set.of("head", "shank"), CouplingConverter.convert("railbound", "knuckle", coupling()).objects().keySet());
    }

    @Test
    void eachPartIsCentredOnItsGroupsPivot() throws Exception {
        ConvertedParts out = CouplingConverter.convert("railbound", "knuckle", coupling());
        // the head reaches back 5 px from the coupling face at its pivot
        assertTrue(out.objects().get("head").contains("v 0.1875 0.1875 0\n"), out.objects().get("head"));
        assertTrue(out.objects().get("head").contains("v -0.1875 -0.1875 -0.3125\n"), out.objects().get("head"));
        // the shank is one block long from the carriage end
        assertTrue(out.objects().get("shank").contains("v 0.09375 0.09375 1\n"), out.objects().get("shank"));
    }

    @Test
    void previewsAreLeftOut() throws Exception {
        ConvertedParts out = CouplingConverter.convert("railbound", "knuckle", coupling());
        assertFalse(out.objects().values().stream().anyMatch(obj -> obj.contains("v 0.0625 0.0625 0.0625")));
    }

    @Test
    void aCouplingNeedsAHeadAndAShank() throws Exception {
        JsonObject model = coupling();
        model.getAsJsonArray("outliner").remove(1);
        ConversionException e = assertThrows(ConversionException.class, () -> CouplingConverter.convert("railbound", "knuckle", model));
        assertTrue(e.getMessage().contains("shank"), e.getMessage());
    }

    @Test
    void anUnknownPartIsAMistake() throws Exception {
        ConversionException e = assertThrows(ConversionException.class,
                () -> CouplingConverter.convert("railbound", "knuckle", coupling("buffers")));
        assertTrue(e.getMessage().contains("buffers"), e.getMessage());
    }

    @Test
    void bellowsAndAFloorFlapAreOptionalParts() throws Exception {
        ConvertedParts out = CouplingConverter.convert("railbound", "knuckle", coupling("bellows", "flap"));
        assertEquals(Set.of("head", "shank", "bellows", "flap"), out.objects().keySet());
    }

    @Test
    void loaderJsonPointsAtTheCouplingTexture() throws Exception {
        JsonObject json = JsonParser.parseString(CouplingConverter.convert("railbound", "knuckle", coupling()).modelJsons().get("head"))
                .getAsJsonObject();
        assertEquals("railbound:models/coupling/knuckle/head.obj", json.get("model").getAsString());
        assertEquals("railbound:coupling/knuckle", json.getAsJsonObject("textures").get("texture").getAsString());
    }

    @Test
    void convertsEveryCouplingInTheFolder(@TempDir Path root) throws Exception {
        Path art = Files.createDirectories(root.resolve("couplings"));
        Files.writeString(art.resolve("coupling_knuckle.bbmodel"), coupling().toString());
        Files.writeString(art.resolve("notes.txt"), "not a model");

        assertEquals(List.of("knuckle"), CouplingConverter.convertAll("railbound", art, root.resolve("out")));
        assertTrue(Files.exists(root.resolve("out/assets/railbound/models/coupling/knuckle/head.obj")));
        assertTrue(Files.exists(root.resolve("out/assets/railbound/models/coupling/knuckle/head.json")));
        assertTrue(Files.exists(root.resolve("out/assets/railbound/textures/coupling/knuckle.png")));
    }
}
