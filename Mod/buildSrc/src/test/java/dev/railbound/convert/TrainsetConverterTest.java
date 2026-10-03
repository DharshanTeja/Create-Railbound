package dev.railbound.convert;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TrainsetConverterTest {

    // ---- model space -> design block space ------------------------------------------------------

    @Test
    void modelOriginIsTheFloorTopAtTheCarriageCentre() {
        ModelSpace space = new ModelSpace(16);
        assertArrayEquals(new double[] {0.5, 1, 8}, space.toBlock(0, 0, 0), 1e-9);
    }

    @Test
    void frontLeftBottomCornerIsTheFirstCellOfLayerZero() {
        ModelSpace space = new ModelSpace(16);
        assertArrayEquals(new double[] {-1, 0, 0}, space.toBlock(-24, -16, -128), 1e-9);
        assertArrayEquals(new double[] {2, 4, 16}, space.toBlock(24, 48, 128), 1e-9);
    }

    // ---- faces ---------------------------------------------------------------------------------------

    private static JsonObject cube(String name, double[] from, double[] to, String facesJson) {
        JsonObject e = JsonParser.parseString("{\"name\":\"" + name + "\",\"uuid\":\"" + name + "\",\"faces\":" + facesJson + "}")
                .getAsJsonObject();
        e.add("from", array(from));
        e.add("to", array(to));
        return e;
    }

    private static JsonArray array(double[] v) {
        JsonArray a = new JsonArray();
        for (double d : v) {
            a.add(d);
        }
        return a;
    }

    private static final String ALL_FACES = "{\"north\":{\"uv\":[0,0,4,4],\"texture\":0},\"south\":{\"uv\":[0,0,4,4],\"texture\":0},"
            + "\"east\":{\"uv\":[0,0,4,4],\"texture\":0},\"west\":{\"uv\":[0,0,4,4],\"texture\":0},"
            + "\"up\":{\"uv\":[0,0,4,4],\"texture\":0},\"down\":{\"uv\":[0,0,4,4],\"texture\":0}}";

    @Test
    void northFaceRunsFromPlusXToMinusXSeenFromOutside() {
        Element e = Element.of(cube("c", new double[] {0, 0, 0}, new double[] {2, 3, 4}, ALL_FACES));
        CubeFace north = e.face("north").orElseThrow();
        // corners: top-left, top-right, bottom-right, bottom-left
        assertArrayEquals(new double[] {2, 3, 0}, north.corners()[0], 1e-9);
        assertArrayEquals(new double[] {0, 3, 0}, north.corners()[1], 1e-9);
        assertArrayEquals(new double[] {0, 0, 0}, north.corners()[2], 1e-9);
        assertArrayEquals(new double[] {2, 0, 0}, north.corners()[3], 1e-9);
    }

    @Test
    void upFaceHasNorthAtTheTopOfTheTexture() {
        Element e = Element.of(cube("c", new double[] {0, 0, 0}, new double[] {2, 3, 4}, ALL_FACES));
        CubeFace up = e.face("up").orElseThrow();
        assertArrayEquals(new double[] {0, 3, 0}, up.corners()[0], 1e-9);
        assertArrayEquals(new double[] {2, 3, 4}, up.corners()[2], 1e-9);
    }

    @Test
    void uvCornersFollowTheFaceUvIncludingMirroring() {
        Element e = Element.of(cube("c", new double[] {0, 0, 0}, new double[] {1, 1, 1},
                "{\"east\":{\"uv\":[10,2,6,8],\"texture\":0}}"));
        CubeFace east = e.face("east").orElseThrow();
        assertArrayEquals(new double[] {10, 2}, east.uvs()[0], 1e-9);
        assertArrayEquals(new double[] {6, 2}, east.uvs()[1], 1e-9);
        assertArrayEquals(new double[] {6, 8}, east.uvs()[2], 1e-9);
        assertArrayEquals(new double[] {10, 8}, east.uvs()[3], 1e-9);
    }

    @Test
    void faceRotationTurnsTheTextureClockwise() {
        Element e = Element.of(cube("c", new double[] {0, 0, 0}, new double[] {1, 1, 1},
                "{\"south\":{\"uv\":[0,0,4,4],\"texture\":0,\"rotation\":90}}"));
        CubeFace south = e.face("south").orElseThrow();
        // the texture's top-left corner lands on the face's top-right vertex
        assertArrayEquals(new double[] {0, 0}, south.uvs()[1], 1e-9);
        assertArrayEquals(new double[] {0, 4}, south.uvs()[0], 1e-9);
    }

    @Test
    void untexturedFacesAreLeftOut() {
        Element e = Element.of(cube("c", new double[] {0, 0, 0}, new double[] {1, 1, 1},
                "{\"up\":{\"uv\":[0,0,4,4],\"texture\":null},\"down\":{\"uv\":[0,0,4,4],\"texture\":0}}"));
        assertTrue(e.face("up").isEmpty());
        assertTrue(e.face("down").isPresent());
        assertTrue(e.face("north").isEmpty());
    }

    @Test
    void cubeRotationTurnsCornersAboutTheOrigin() {
        JsonObject json = cube("c", new double[] {0, 0, 0}, new double[] {2, 1, 1}, ALL_FACES);
        json.add("origin", array(new double[] {0, 0, 0}));
        json.add("rotation", array(new double[] {0, 90, 0}));
        Element e = Element.of(json);
        // rotating +90 about Y takes +X to -Z
        double[] topRightOfSouth = e.face("south").orElseThrow().corners()[1]; // (2,1,1) before rotation
        assertArrayEquals(new double[] {1, 1, -2}, topRightOfSouth, 1e-9);
    }

    // ---- groups -> parts ---------------------------------------------------------------------------

    @Test
    void groupsDecideWhichPartACubeBelongsTo() {
        assertEquals(Optional.of("body"), Parts.partFor(List.of("coach", "roof")));
        assertEquals(Optional.of("lamps"), Parts.partFor(List.of("coach", "lamps")));
        assertEquals(Optional.of("door_left_front"), Parts.partFor(List.of("coach", "doors", "door_left_front")));
        assertEquals(Optional.empty(), Parts.partFor(List.of("loco", "_preview_bogeys")));
        assertEquals(Optional.empty(), Parts.partFor(List.of("ref_bogey")));
    }

    // ---- whole conversion --------------------------------------------------------------------------

    private static String png(int w, int h) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB), "png", out);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
    }

    /** A one-block-long carriage: a floor cube in `coach`, a door leaf in `door_right_front`, a lamp. */
    private static JsonObject model(int textureSize, double floorHalfWidth, String doorGroup) throws IOException {
        JsonObject m = new JsonObject();
        JsonObject res = new JsonObject();
        res.addProperty("width", textureSize);
        res.addProperty("height", textureSize);
        m.add("resolution", res);
        JsonArray elements = new JsonArray();
        elements.add(cube("floor", new double[] {-floorHalfWidth, -2, -8}, new double[] {floorHalfWidth, 0, 8}, ALL_FACES));
        elements.add(cube("leaf", new double[] {23, 0, -7}, new double[] {24, 32, 7}, ALL_FACES));
        elements.add(cube("lamp", new double[] {-1, 30, -1}, new double[] {1, 31, 1}, ALL_FACES));
        elements.add(cube("ghost", new double[] {0, -30, 0}, new double[] {1, -29, 1}, ALL_FACES));
        m.add("elements", elements);
        m.add("outliner", JsonParser.parseString("[{\"name\":\"coach\",\"uuid\":\"g1\",\"children\":[\"floor\","
                + "{\"name\":\"" + doorGroup + "\",\"uuid\":\"g2\",\"children\":[\"leaf\"]},"
                + "{\"name\":\"lamps\",\"uuid\":\"g3\",\"children\":[\"lamp\"]},"
                + "{\"name\":\"_preview\",\"uuid\":\"g4\",\"children\":[\"ghost\"]}]}]"));
        JsonArray textures = new JsonArray();
        JsonObject tex = new JsonObject();
        tex.addProperty("id", "0");
        tex.addProperty("width", textureSize);
        tex.addProperty("height", textureSize);
        tex.addProperty("uv_width", textureSize);
        tex.addProperty("uv_height", textureSize);
        tex.addProperty("source", png(textureSize, textureSize));
        textures.add(tex);
        m.add("textures", textures);
        return m;
    }

    private static JsonObject design(String doorPart) {
        return JsonParser.parseString("{\"size\":{\"length\":1,\"width\":3,\"height\":4},"
                + "\"doors\":[{\"part\":\"" + doorPart + "\",\"pos\":[1,1,0]}]}").getAsJsonObject();
    }

    @Test
    void exportsBodyLampsAndDoorsButNotPreviewGroups() throws Exception {
        ConvertedTrainset out = TrainsetConverter.convert("railbound", "tiny", model(64, 24, "door_right_front"), design("door_right_front"));
        assertEquals(java.util.Set.of("body", "lamps", "door_right_front"), out.objects().keySet());
        assertEquals(6, out.objects().get("body").lines().filter(l -> l.startsWith("f ")).count());
        assertTrue(out.objects().values().stream().noneMatch(obj -> obj.contains("-0.875")), "the preview cube is not exported");
    }

    @Test
    void objVerticesAreInDesignBlockSpace() throws Exception {
        ConvertedTrainset out = TrainsetConverter.convert("railbound", "tiny", model(64, 24, "door_right_front"), design("door_right_front"));
        // floor spans x -24..24, y -2..0, z -8..8 in model units = x -1..2, y 0.875..1, z 0..1 in blocks
        String body = out.objects().get("body");
        assertTrue(body.contains("v -1 0.875 0"), body);
        assertTrue(body.contains("v 2 1 1"), body);
    }

    @Test
    void objTextureVRunsTopDownAsNeoForgeReadsIt() throws Exception {
        ConvertedTrainset out = TrainsetConverter.convert("railbound", "tiny", model(64, 24, "door_right_front"), design("door_right_front"));
        // NeoForge's OBJ loader uses vt v as Minecraft's top-down V (flip_v is off): uv [0,0,4,4] on 64px -> 0..0.0625
        assertTrue(out.objects().get("body").contains("vt 0 0\n"));
        assertTrue(out.objects().get("body").contains("vt 0.0625 0.0625\n"));
        assertFalse(out.objects().get("body").contains("vt 0 1\n"));
    }

    @Test
    void writesALoaderJsonPerPartPointingAtItsObjAndTheDesignTexture() throws Exception {
        ConvertedTrainset out = TrainsetConverter.convert("railbound", "tiny", model(64, 24, "door_right_front"), design("door_right_front"));
        JsonObject json = JsonParser.parseString(out.modelJsons().get("door_right_front")).getAsJsonObject();
        assertEquals("neoforge:obj", json.get("loader").getAsString());
        assertEquals("railbound:models/trainset/tiny/door_right_front.obj", json.get("model").getAsString());
        assertEquals("railbound:trainset/tiny", json.getAsJsonObject("textures").get("texture").getAsString());
        assertTrue(out.objects().get("door_right_front").startsWith("mtllib trainset.mtl"));
        assertTrue(out.material().contains("map_Kd #texture"));
    }

    @Test
    void rejectsATextureLargerThan512() throws Exception {
        ConversionException e = assertThrows(ConversionException.class,
                () -> TrainsetConverter.convert("railbound", "tiny", model(1024, 24, "door_right_front"), design("door_right_front")));
        assertTrue(e.getMessage().contains("512"), e.getMessage());
    }

    @Test
    void rejectsADesignDoorWithoutAModelGroup() throws Exception {
        ConversionException e = assertThrows(ConversionException.class,
                () -> TrainsetConverter.convert("railbound", "tiny", model(64, 24, "door_right_front"), design("door_left_front")));
        assertTrue(e.getMessage().contains("door_left_front"), e.getMessage());
    }

    @Test
    void rejectsAModelDoorGroupTheDesignDoesNotHave() throws Exception {
        ConversionException e = assertThrows(ConversionException.class,
                () -> TrainsetConverter.convert("railbound", "tiny", model(64, 24, "door_spare"), design("door_right_front")));
        assertTrue(e.getMessage().contains("door_spare"), e.getMessage());
    }

    private static JsonObject designWithBogey() {
        return JsonParser.parseString("{\"size\":{\"length\":1,\"width\":3,\"height\":4},"
                + "\"bogeys\":[{\"z\":0,\"style\":\"create:standard\"}],"
                + "\"doors\":[{\"part\":\"door_right_front\",\"pos\":[1,1,0]}]}").getAsJsonObject();
    }

    @Test
    void rejectsAFloorFlushWithTheBogeyTop() throws Exception {
        // the fixture's floor top is at y = 0, right over the bogey: Create's bogey top would flicker through it
        ConversionException e = assertThrows(ConversionException.class,
                () -> TrainsetConverter.convert("railbound", "tiny", model(64, 24, "door_right_front"), designWithBogey()));
        assertTrue(e.getMessage().contains("floor") && e.getMessage().contains("bogey"), e.getMessage());
    }

    @Test
    void acceptsAFloorRaisedClearOfTheBogeyTop() throws Exception {
        JsonObject model = model(64, 24, "door_right_front");
        model.getAsJsonArray("elements").get(0).getAsJsonObject().add("to", array(new double[] {24, 0.4, 8}));
        assertDoesNotThrow(() -> TrainsetConverter.convert("railbound", "tiny", model, designWithBogey()));
    }

    @Test
    void gangwaysMayReachHalfwayIntoTheGapBetweenCarriages() throws Exception {
        // carriages are placed one block apart, so each end may reach half a block (8 units) into the gap
        JsonObject model = model(64, 24, "door_right_front");
        model.getAsJsonArray("elements").add(cube("gangway", new double[] {-8, 0, 8}, new double[] {8, 16, 15.5}, ALL_FACES));
        model.getAsJsonArray("outliner").get(0).getAsJsonObject().getAsJsonArray("children").add("gangway");
        assertDoesNotThrow(() -> TrainsetConverter.convert("railbound", "tiny", model, design("door_right_front")));
    }

    @Test
    void rejectsPartsReachingPastHalfTheGap() throws Exception {
        JsonObject model = model(64, 24, "door_right_front");
        model.getAsJsonArray("elements").add(cube("gangway", new double[] {-8, 0, 8}, new double[] {8, 16, 16.5}, ALL_FACES));
        model.getAsJsonArray("outliner").get(0).getAsJsonObject().getAsJsonArray("children").add("gangway");
        ConversionException e = assertThrows(ConversionException.class,
                () -> TrainsetConverter.convert("railbound", "tiny", model, design("door_right_front")));
        assertTrue(e.getMessage().contains("gangway"), e.getMessage());
    }

    @Test
    void rejectsABodyWiderThanThreeBlocks() throws Exception {
        ConversionException e = assertThrows(ConversionException.class,
                () -> TrainsetConverter.convert("railbound", "tiny", model(64, 25, "door_right_front"), design("door_right_front")));
        assertTrue(e.getMessage().contains("floor"), e.getMessage());
    }
}
