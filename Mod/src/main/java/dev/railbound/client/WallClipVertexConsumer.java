package dev.railbound.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Hands quads on to another consumer after cutting off whatever is behind a wall: a plane through a carriage end,
 * facing out. A knuckle swung round on a sharp curve can have part of its head or shank behind the end of its own
 * carriage; cut at the wall, it never shows through the carriage's side or end.
 *
 * <p>It takes vertices the way Minecraft's model renderer gives them (position, colour, uv, overlay, light, then the
 * normal last), so the plane has to be given in the same frame as those vertices: pass the matrix the parts are drawn
 * with and the wall in the frame that matrix puts them in.
 */
public final class WallClipVertexConsumer implements VertexConsumer {
    /** Behind the wall by no more than this still counts as in front: float rounding on the wall itself. */
    private static final float EPSILON = 1e-4f;

    private static final class Vert {
        float x, y, z;
        int r, g, b, a;
        float u, v;
        int overlayU, overlayV, lightU, lightV;
        float nx, ny, nz;

        void copyFrom(Vert o) {
            x = o.x; y = o.y; z = o.z;
            r = o.r; g = o.g; b = o.b; a = o.a;
            u = o.u; v = o.v;
            overlayU = o.overlayU; overlayV = o.overlayV; lightU = o.lightU; lightV = o.lightV;
            nx = o.nx; ny = o.ny; nz = o.nz;
        }

        /** The point a share t of the way from one vertex to another. */
        void lerp(Vert from, Vert to, float t) {
            x = Mth.lerp(t, from.x, to.x);
            y = Mth.lerp(t, from.y, to.y);
            z = Mth.lerp(t, from.z, to.z);
            r = Math.round(Mth.lerp(t, from.r, to.r));
            g = Math.round(Mth.lerp(t, from.g, to.g));
            b = Math.round(Mth.lerp(t, from.b, to.b));
            a = Math.round(Mth.lerp(t, from.a, to.a));
            u = Mth.lerp(t, from.u, to.u);
            v = Mth.lerp(t, from.v, to.v);
            nx = Mth.lerp(t, from.nx, to.nx);
            ny = Mth.lerp(t, from.ny, to.ny);
            nz = Mth.lerp(t, from.nz, to.nz);
            Vert lit = t < 0.5f ? from : to;
            overlayU = lit.overlayU; overlayV = lit.overlayV; lightU = lit.lightU; lightV = lit.lightV;
        }
    }

    private final Vert[] quad = {new Vert(), new Vert(), new Vert(), new Vert()};
    /** A quad cut by a plane has at most five corners; a few spare. */
    private final Vert[] polygon = new Vert[8];
    private final Vert[] scratch = new Vert[8];
    private final Vector3f normal = new Vector3f();

    private VertexConsumer target;
    private float offset;
    private int filled;
    private boolean vertexOpen;

    public WallClipVertexConsumer() {
        for (int i = 0; i < polygon.length; i++) {
            polygon[i] = new Vert();
            scratch[i] = new Vert();
        }
    }

    /**
     * Starts clipping for {@code target}: {@code outward} is the wall's facing and {@code point} a point on it, both in
     * the frame that {@code base} (the matrix the parts are drawn with) carries to the vertices' frame.
     */
    public void begin(VertexConsumer target, Matrix4f base, Vec3 point, Vec3 outward) {
        this.target = target;
        this.filled = 0;
        this.vertexOpen = false;
        base.transformDirection((float) outward.x, (float) outward.y, (float) outward.z, normal).normalize();
        Vector3f p = base.transformPosition((float) point.x, (float) point.y, (float) point.z, new Vector3f());
        offset = -normal.dot(p);
    }

    private float side(Vert v) {
        return normal.x * v.x + normal.y * v.y + normal.z * v.z + offset;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        Vert v = quad[filled];
        v.x = x;
        v.y = y;
        v.z = z;
        vertexOpen = true;
        return this;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        Vert v = quad[filled];
        v.r = r;
        v.g = g;
        v.b = b;
        v.a = a;
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        quad[filled].u = u;
        quad[filled].v = v;
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        quad[filled].overlayU = u;
        quad[filled].overlayV = v;
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        quad[filled].lightU = u;
        quad[filled].lightV = v;
        return this;
    }

    /** The model renderer gives the normal last, which closes the vertex; the fourth one closes the quad. */
    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        Vert v = quad[filled];
        v.nx = x;
        v.ny = y;
        v.nz = z;
        vertexOpen = false;
        if (++filled == 4) {
            filled = 0;
            clipAndEmit();
        }
        return this;
    }

    private void clipAndEmit() {
        boolean allIn = true;
        boolean allOut = true;
        for (Vert v : quad) {
            boolean in = side(v) >= -EPSILON;
            allIn &= in;
            allOut &= !in;
        }
        if (allOut) {
            return;
        }
        if (allIn) {
            emit(quad, 4);
            return;
        }
        // Sutherland-Hodgman against the one plane
        int count = 0;
        for (int i = 0; i < 4; i++) {
            Vert from = quad[i];
            Vert to = quad[(i + 1) % 4];
            float sf = side(from);
            float st = side(to);
            boolean fromIn = sf >= -EPSILON;
            boolean toIn = st >= -EPSILON;
            if (fromIn) {
                polygon[count++].copyFrom(from);
            }
            if (fromIn != toIn) {
                polygon[count++].lerp(from, to, sf / (sf - st));
            }
        }
        if (count < 3) {
            return;
        }
        // a polygon of 3 to 5 corners becomes quads, a fan from the first corner (the last corner repeated for a triangle)
        for (int i = 1; i + 1 < count; i++) {
            scratch[0].copyFrom(polygon[0]);
            scratch[1].copyFrom(polygon[i]);
            scratch[2].copyFrom(polygon[i + 1]);
            scratch[3].copyFrom(polygon[i + 1]);
            emit(scratch, 4);
        }
    }

    private void emit(Vert[] vertices, int count) {
        for (int i = 0; i < count; i++) {
            Vert v = vertices[i];
            target.addVertex(v.x, v.y, v.z)
                    .setColor(v.r, v.g, v.b, v.a)
                    .setUv(v.u, v.v)
                    .setUv1(v.overlayU, v.overlayV)
                    .setUv2(v.lightU, v.lightV)
                    .setNormal(v.nx, v.ny, v.nz);
        }
    }
}
