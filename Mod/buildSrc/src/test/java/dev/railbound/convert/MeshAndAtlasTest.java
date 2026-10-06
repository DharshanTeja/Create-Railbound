package dev.railbound.convert;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Blockbench meshes (as a glTF import makes them) and models with several textures, packed into one atlas. */
class MeshAndAtlasTest {

    // ---- meshes --------------------------------------------------------------------------------------

    /** A mesh element: vertices relative to its origin, faces listing vertex keys with a uv per vertex. */
    private static JsonObject mesh(String name, double[] origin, double[] rotation, String vertices, String faces) {
        JsonObject e = JsonParser.parseString("{\"type\":\"mesh\",\"name\":\"" + name + "\",\"uuid\":\"" + name + "\","
                + "\"vertices\":" + vertices + ",\"faces\":" + faces + "}").getAsJsonObject();
        e.add("origin", array(origin));
        e.add("rotation", array(rotation));
        return e;
    }

    private static JsonArray array(double[] v) {
        JsonArray a = new JsonArray();
        for (double d : v) {
            a.add(d);
        }
        return a;
    }

    private static final String SQUARE = "{\"a\":[0,0,0],\"b\":[4,0,0],\"c\":[4,4,0],\"d\":[0,4,0]}";

    /** The OBJ polygon of a face: its vertices in the order written, and the normal written. */
    private record Written(List<double[]> vertices, double[] normal) {}

    private static Written write(CubeFace face) {
        String obj = ModelSource.obj(List.of(face), c -> c, new ModelSource.Texture(new byte[0], 16, 16, java.util.Map.of()));
        List<double[]> v = new ArrayList<>();
        double[] n = null;
        for (String line : obj.split("\n")) {
            String[] p = line.split(" ");
            if (p[0].equals("v")) {
                v.add(new double[] {Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3])});
            } else if (p[0].equals("vn")) {
                n = new double[] {Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3])};
            }
        }
        List<double[]> order = new ArrayList<>();
        for (String line : obj.split("\n")) {
            if (line.startsWith("f ")) {
                for (String corner : line.substring(2).split(" ")) {
                    order.add(v.get(Integer.parseInt(corner.split("/")[0]) - 1));
                }
            }
        }
        return new Written(order, n);
    }

    @Test
    void aMeshTriangleKeepsBlockbenchsFrontFaceAndItsUvs() {
        // counter-clockwise seen from +z, as Blockbench (three.js) shows a triangle's front
        Element e = Element.of(mesh("tri", new double[] {10, 0, 0}, new double[] {0, 0, 0}, SQUARE,
                "{\"f\":{\"vertices\":[\"a\",\"b\",\"d\"],\"uv\":{\"a\":[0,16],\"b\":[16,16],\"d\":[0,0]},\"texture\":0}}"));
        List<CubeFace> faces = e.faces();
        assertEquals(1, faces.size());
        Written w = write(faces.get(0));
        assertArrayEquals(new double[] {0, 0, 1}, w.normal(), 1e-9, "facing +z");
        assertArrayEquals(new double[] {10, 0, 0}, w.vertices().get(0), 1e-9, "placed from the mesh origin");
        assertArrayEquals(new double[] {14, 0, 0}, w.vertices().get(1), 1e-9);
        assertArrayEquals(new double[] {10, 4, 0}, w.vertices().get(2), 1e-9);
        CubeFace f = faces.get(0);
        for (int i = 0; i < 4; i++) {
            double[] corner = f.corners()[i], uv = f.uvs()[i];
            // each corner keeps its own uv: a (10,0) -> (0,16), b (14,0) -> (16,16), d (10,4) -> (0,0)
            double[] expected = corner[0] > 13 ? new double[] {16, 16} : corner[1] > 3 ? new double[] {0, 0} : new double[] {0, 16};
            assertArrayEquals(expected, uv, 1e-9);
        }
    }

    @Test
    void aMeshQuadIsSortedAsBlockbenchSortsIt() {
        // stored crossed over (a, b, d, c); Blockbench draws it as the square d, a, b, c, facing +z
        Element e = Element.of(mesh("quad", new double[] {0, 0, 0}, new double[] {0, 0, 0}, SQUARE,
                "{\"f\":{\"vertices\":[\"a\",\"b\",\"d\",\"c\"],\"uv\":{\"a\":[0,4],\"b\":[4,4],\"c\":[4,0],\"d\":[0,0]},\"texture\":0}}"));
        Written w = write(e.faces().get(0));
        assertArrayEquals(new double[] {0, 0, 1}, w.normal(), 1e-9);
        // walking the written corners goes round the square's edge, never across it
        for (int i = 0; i < 4; i++) {
            double[] p = w.vertices().get(i), q = w.vertices().get((i + 1) % 4);
            assertEquals(4, Math.abs(p[0] - q[0]) + Math.abs(p[1] - q[1]), 1e-9, "edge " + i);
        }
    }

    @Test
    void aSideOfACylinderAsBlockbenchSavesItFacesOutwards() {
        // copied from a mesh Blockbench saved: the side at x 2 of a cylinder round the y axis
        Element e = Element.of(mesh("cylinder", new double[] {0, 4, 0}, new double[] {0, 0, 0},
                "{\"qyZY\":[2,4,2],\"ZyQe\":[2,4,-2],\"K6TI\":[2,0,2],\"5vCx\":[2,0,-2]}",
                "{\"uRuH7pZe\":{\"uv\":{\"5vCx\":[16,16],\"ZyQe\":[16,0],\"K6TI\":[0,16],\"qyZY\":[0,0]},"
                        + "\"vertices\":[\"ZyQe\",\"qyZY\",\"K6TI\",\"5vCx\"],\"texture\":0}}"));
        assertArrayEquals(new double[] {1, 0, 0}, write(e.faces().get(0)).normal(), 1e-9);
    }

    @Test
    void aMeshTurnsAboutItsOrigin() {
        // a quarter turn about y carries the vertex 4 along +x round to -z
        Element e = Element.of(mesh("turned", new double[] {10, 0, 0}, new double[] {0, 90, 0}, SQUARE,
                "{\"f\":{\"vertices\":[\"a\",\"b\",\"d\"],\"uv\":{\"a\":[0,0],\"b\":[1,0],\"d\":[0,1]},\"texture\":0}}"));
        boolean found = false;
        for (double[] corner : e.faces().get(0).corners()) {
            found |= Math.abs(corner[0] - 10) < 1e-9 && Math.abs(corner[2] + 4) < 1e-9;
        }
        assertTrue(found);
    }

    @Test
    void aMeshFaceWithMoreThanFourCornersIsAProblem() {
        Element e = Element.of(mesh("ngon", new double[] {0, 0, 0}, new double[] {0, 0, 0},
                "{\"a\":[0,0,0],\"b\":[4,0,0],\"c\":[5,3,0],\"d\":[2,5,0],\"e\":[-1,3,0]}",
                "{\"f\":{\"vertices\":[\"a\",\"b\",\"c\",\"d\",\"e\"],\"uv\":{\"a\":[0,0],\"b\":[1,0],\"c\":[1,1],\"d\":[0,1],\"e\":[0,0]},\"texture\":0}}"));
        assertTrue(e.problem().orElseThrow().contains("5 corners"));
        assertTrue(e.faces().isEmpty());
    }

    // ---- textures and the atlas ------------------------------------------------------------------------

    private static String png(int w, int h, int argb) throws IOException {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                image.setRGB(x, y, argb);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private static JsonObject texture(String id, int size, int argb) throws IOException {
        JsonObject t = new JsonObject();
        t.addProperty("id", id);
        t.addProperty("uv_width", size);
        t.addProperty("uv_height", size);
        t.addProperty("source", png(size, size, argb));
        return t;
    }

    private static CubeFace faceOn(int texture, double u1, double v1, double u2, double v2) {
        double[][] corners = {{0, 1, 0}, {1, 1, 0}, {1, 0, 0}, {0, 0, 0}};
        double[][] uvs = {{u1, v1}, {u2, v1}, {u2, v2}, {u1, v2}};
        return new CubeFace("north", corners, uvs, texture);
    }

    private static BufferedImage image(ModelSource.Texture texture) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(texture.png()));
    }

    private static ModelSource source(JsonObject... textures) {
        JsonObject model = new JsonObject();
        model.add("outliner", new JsonArray());
        model.add("elements", new JsonArray());
        JsonArray list = new JsonArray();
        for (JsonObject t : textures) {
            list.add(t);
        }
        model.add("textures", list);
        return new ModelSource(model);
    }

    @Test
    void twoTexturesArePackedIntoOneAtlasAndTheirUvsFollow() throws IOException {
        ModelSource source = source(texture("0", 16, 0xFFFF0000), texture("1", 16, 0xFF0000FF));
        List<String> problems = new ArrayList<>();
        List<CubeFace> faces = List.of(faceOn(0, 0, 0, 16, 16), faceOn(1, 0, 0, 16, 16));
        ModelSource.Texture atlas = source.texture(faces, problems);
        assertEquals(List.of(), problems);
        BufferedImage image = image(atlas);
        // each face's uv, mapped into the atlas, lands on its own texture's pixels
        for (CubeFace face : faces) {
            double[] centre = atlas.toAtlas(face.texture(), new double[] {8, 8});
            int rgb = image.getRGB((int) (centre[0] * image.getWidth() / atlas.uvWidth()), (int) (centre[1] * image.getHeight() / atlas.uvHeight()));
            assertEquals(face.texture() == 0 ? 0xFFFF0000 : 0xFF0000FF, rgb, "texture " + face.texture());
        }
        assertTrue(image.getWidth() <= ModelSource.MAX_TEXTURE && image.getHeight() <= ModelSource.MAX_TEXTURE);
    }

    @Test
    void aSingleTextureIsLeftAsItIs() throws IOException {
        ModelSource source = source(texture("0", 64, 0xFF00FF00));
        ModelSource.Texture texture = source.texture(List.of(faceOn(0, 0, 0, 4, 4)), new ArrayList<>());
        assertEquals(64, image(texture).getWidth());
        assertArrayEquals(new double[] {4, 4}, texture.toAtlas(0, new double[] {4, 4}), 1e-9);
    }

    @Test
    void anOversizedTextureIsScaledDownToFit() throws IOException {
        // glTF exports often carry 1024 or 2048 px textures; the trainset texture may be at most 512
        ModelSource source = source(texture("0", 1024, 0xFF00FF00));
        List<String> problems = new ArrayList<>();
        ModelSource.Texture texture = source.texture(List.of(faceOn(0, 0, 0, 1024, 1024)), problems);
        assertEquals(List.of(), problems);
        assertEquals(512, image(texture).getWidth());
        double[] corner = texture.toAtlas(0, new double[] {1024, 1024});
        assertEquals(1, corner[0] / texture.uvWidth(), 1e-9, "the face still covers the whole texture");
    }

    @Test
    void aWholeTrainsetMixingCubesAndAMeshOnTwoTexturesConverts() throws Exception {
        // what a glTF import plus a hand-made floor looks like: a cube on one texture, a mesh triangle on another
        JsonObject model = new JsonObject();
        JsonArray elements = new JsonArray();
        elements.add(JsonParser.parseString("{\"name\":\"floor\",\"uuid\":\"floor\",\"from\":[-24,-2,-8],\"to\":[24,0.4,8],"
                + "\"faces\":{\"up\":{\"uv\":[0,0,16,16],\"texture\":0},\"down\":{\"uv\":[0,0,16,16],\"texture\":0}}}"));
        elements.add(mesh("roof_panel", new double[] {0, 30, 0}, new double[] {0, 0, 0},
                "{\"a\":[-8,0,-4],\"b\":[8,0,-4],\"c\":[0,0,4]}",
                "{\"f\":{\"vertices\":[\"a\",\"c\",\"b\"],\"uv\":{\"a\":[0,32],\"b\":[32,32],\"c\":[16,0]},\"texture\":1}}"));
        model.add("elements", elements);
        model.add("outliner", JsonParser.parseString("[{\"name\":\"body\",\"uuid\":\"g\",\"children\":[\"floor\",\"roof_panel\"]}]"));
        JsonArray textures = new JsonArray();
        textures.add(texture("0", 16, 0xFF808080));
        textures.add(texture("1", 32, 0xFFAA2222));
        model.add("textures", textures);
        JsonObject design = JsonParser.parseString("{\"size\":{\"length\":1,\"width\":3,\"height\":4}}").getAsJsonObject();

        ConvertedTrainset out = TrainsetConverter.convert("railbound", "imported", model, design);
        BufferedImage atlas = ImageIO.read(new ByteArrayInputStream(out.texturePng()));
        assertEquals(64, atlas.getWidth(), "a 16 and a 32 px texture share one 64 px atlas");
        assertEquals(3, out.objects().get("body").lines().filter(l -> l.startsWith("f ")).count(), "two cube faces and a triangle");
        // the triangle's normal points up (its corners a, c, b run counter-clockwise seen from above)
        assertTrue(out.objects().get("body").contains("vn 0 1 0"), out.objects().get("body"));
    }

    @Test
    void uvsRunningOffTheirTextureAreAProblem() throws IOException {
        // tiled glTF uvs would show neighbouring textures once packed into an atlas
        ModelSource source = source(texture("0", 16, 0xFF00FF00), texture("1", 16, 0xFF0000FF));
        List<String> problems = new ArrayList<>();
        source.texture(List.of(faceOn(0, 0, 0, 32, 16), faceOn(1, 0, 0, 16, 16)), problems);
        assertTrue(problems.stream().anyMatch(p -> p.contains("outside")), problems.toString());
    }
}
