package dev.railbound.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WallClipVertexConsumerTest {
    /** Records every vertex it is given, in order. */
    private static final class Recorder implements VertexConsumer {
        final List<float[]> vertices = new ArrayList<>();
        final List<int[]> light = new ArrayList<>();

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            vertices.add(new float[] {x, y, z});
            light.add(new int[] {0, 0});
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            light.get(light.size() - 1)[0] = u;
            light.get(light.size() - 1)[1] = v;
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return this;
        }
    }

    private static void quad(VertexConsumer out, float[][] corners) {
        for (float[] c : corners) {
            out.addVertex(c[0], c[1], c[2]).setColor(255, 255, 255, 255).setUv(0, 0).setUv1(0, 10).setUv2(240, 240).setNormal(0, 0, 1);
        }
    }

    /** Area of the polygons the recorder was given, taking every four vertices as a quad (two triangles). */
    private static double area(Recorder r) {
        double total = 0;
        for (int i = 0; i + 3 < r.vertices.size(); i += 4) {
            total += triangle(r.vertices.get(i), r.vertices.get(i + 1), r.vertices.get(i + 2));
            total += triangle(r.vertices.get(i), r.vertices.get(i + 2), r.vertices.get(i + 3));
        }
        return total;
    }

    private static double triangle(float[] a, float[] b, float[] c) {
        double ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
        double vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
        double cx = uy * vz - uz * vy, cy = uz * vx - ux * vz, cz = ux * vy - uy * vx;
        return 0.5 * Math.sqrt(cx * cx + cy * cy + cz * cz);
    }

    private static final float[][] STRADDLING = {{-1, 0, 0}, {1, 0, 0}, {1, 1, 0}, {-1, 1, 0}};

    @Test
    void aQuadInFrontOfTheWallPassesThroughUntouched() {
        Recorder out = new Recorder();
        WallClipVertexConsumer clip = new WallClipVertexConsumer();
        clip.begin(out, new Matrix4f(), new Vec3(0, 0, 0), new Vec3(1, 0, 0));
        quad(clip, new float[][] {{1, 0, 0}, {2, 0, 0}, {2, 1, 0}, {1, 1, 0}});
        assertEquals(4, out.vertices.size());
        assertEquals(1.0, area(out), 1e-6);
    }

    @Test
    void aQuadBehindTheWallIsDropped() {
        Recorder out = new Recorder();
        WallClipVertexConsumer clip = new WallClipVertexConsumer();
        clip.begin(out, new Matrix4f(), new Vec3(0, 0, 0), new Vec3(1, 0, 0));
        quad(clip, new float[][] {{-2, 0, 0}, {-1, 0, 0}, {-1, 1, 0}, {-2, 1, 0}});
        assertEquals(0, out.vertices.size());
    }

    @Test
    void aQuadThroughTheWallIsCutAtIt() {
        Recorder out = new Recorder();
        WallClipVertexConsumer clip = new WallClipVertexConsumer();
        clip.begin(out, new Matrix4f(), new Vec3(0, 0, 0), new Vec3(1, 0, 0));
        quad(clip, STRADDLING);
        assertTrue(out.vertices.size() >= 4 && out.vertices.size() % 4 == 0, "whole quads out: " + out.vertices.size());
        for (float[] v : out.vertices) {
            assertTrue(v[0] >= -1e-4f, "nothing behind the wall, got x " + v[0]);
        }
        assertEquals(1.0, area(out), 1e-5, "the half in front, a 1 x 1 square");
    }

    @Test
    void theWallFollowsTheMatrixThePartsAreDrawnWith() {
        Recorder out = new Recorder();
        WallClipVertexConsumer clip = new WallClipVertexConsumer();
        // the drawing frame is shifted 5 along x, so a wall at x 0 in that frame is at x 5 in the vertices given
        clip.begin(out, new Matrix4f().translation(5, 0, 0), new Vec3(0, 0, 0), new Vec3(1, 0, 0));
        quad(clip, new float[][] {{4, 0, 0}, {6, 0, 0}, {6, 1, 0}, {4, 1, 0}});
        for (float[] v : out.vertices) {
            assertTrue(v[0] >= 5 - 1e-4f, "x " + v[0]);
        }
        assertEquals(1.0, area(out), 1e-5);
    }

    @Test
    void theLightOfACutVertexIsKept() {
        Recorder out = new Recorder();
        WallClipVertexConsumer clip = new WallClipVertexConsumer();
        clip.begin(out, new Matrix4f(), new Vec3(0, 0, 0), new Vec3(1, 0, 0));
        quad(clip, STRADDLING);
        for (int[] light : out.light) {
            assertEquals(240, light[0]);
            assertEquals(240, light[1]);
        }
    }

    @Test
    void severalQuadsInARowAreEachJudgedOnTheirOwn() {
        Recorder out = new Recorder();
        WallClipVertexConsumer clip = new WallClipVertexConsumer();
        clip.begin(out, new Matrix4f(), new Vec3(0, 0, 0), new Vec3(1, 0, 0));
        quad(clip, new float[][] {{-2, 0, 0}, {-1, 0, 0}, {-1, 1, 0}, {-2, 1, 0}});
        quad(clip, STRADDLING);
        quad(clip, new float[][] {{1, 0, 0}, {2, 0, 0}, {2, 1, 0}, {1, 1, 0}});
        assertEquals(2.0, area(out), 1e-5);
    }
}
