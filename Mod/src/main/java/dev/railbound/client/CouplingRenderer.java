package dev.railbound.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import dev.railbound.Railbound;
import dev.railbound.carriage.CarriageCouplers;
import dev.railbound.carriage.CouplingGeometry;
import dev.railbound.trainset.design.GangwaySpec;
import dev.railbound.trainset.design.ParsedDesign;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Knuckle couplers on trains, drawn instead of Create's chain: at both ends of our trainsets, and on Create-built
 * carriages at their coupler blocks. The two heads always lock together in the middle of the gap; each knuckle
 * swivels at its carriage end to point there and, on curves tight enough to swing the ends apart, slides out of its
 * draft gear on a slimmer bar. Between two coaches the gangway bellows and floor flaps bend from one end door to the other; at a free
 * coach end its half of the bellows stands out. Parked carriages show the same through {@link #drawParked}. The parts
 * are Blockbench art (art/couplings/coupling_knuckle.bbmodel), each looking along +z away from its carriage.
 */
public final class CouplingRenderer {
    static final RenderType TYPE = RenderType.cutout();
    public static final ModelResourceLocation SHANK = part("shank");
    public static final ModelResourceLocation HEAD = part("head");
    public static final ModelResourceLocation BELLOWS = part("bellows");
    public static final ModelResourceLocation FLAP = part("flap");
    public static final List<ModelResourceLocation> MODELS = List.of(SHANK, HEAD, BELLOWS, FLAP);
    /** How far the knuckle head reaches back from its coupling face (blocks): the shank runs up to it. */
    private static final double HEAD_BACK = 7.6 / 16;
    /** The bar a knuckle slides out on, as thick as this share of its shank. */
    private static final float SLIDE_THICKNESS = 0.7f;
    /** The art's sizes (blocks): a fold 4 px deep, the bellows' opening. Half the bellows stands out of a free end. */
    private static final double FOLD = 4 / 16.0;
    private static final double BELLOWS_HALF_WIDTH = 11.5 / 16;
    private static final double BELLOWS_HEIGHT = 35.5 / 16;
    private static final double HALF_BELLOWS = 0.5;
    /** Folds overlap a little so the outside of a curve shows no gaps; each flap reaches past the middle. */
    private static final double FOLD_OVERLAP = 1.2;
    private static final double FLAP_REACH = 0.55;
    /** How far from the meeting point a wrench still finds a joined coupler (blocks). */
    private static final double PICK_SIZE = 0.4;

    /** A coupler in the world this frame. */
    private record Coupler(CouplingGeometry.End end, CarriageCouplers.Local local) {}

    /** A joined coupler a wrench points at: its train, the gap behind carriage {@code gap}, and how far away. */
    public record Pick(UUID train, int gap, double distance) {}

    /** A carriage's couplers this frame and its middle (which way is towards it). */
    private record CarriageEnds(List<Coupler> couplers, Vec3 middle) {
        static final CarriageEnds NONE = new CarriageEnds(List.of(), Vec3.ZERO);
    }

    private CouplingRenderer() {}

    private static ModelResourceLocation part(String name) {
        return ModelResourceLocation.standalone(Railbound.rl("coupling/knuckle/" + name));
    }

    /** Whether we draw this join ourselves (then Create's chain is hidden). */
    public static boolean drawsOwn(@Nullable Level level, Carriage a, Carriage b) {
        return level != null && joined(couplers(entityOf(level, a), 1), couplers(entityOf(level, b), 1)).isPresent();
    }

    private static Optional<int[]> joined(CarriageEnds a, CarriageEnds b) {
        return CouplingGeometry.facing(a.couplers().stream().map(Coupler::end).toList(), a.middle(),
                b.couplers().stream().map(Coupler::end).toList(), b.middle());
    }

    /** The nearest joined coupler on the line from one point to another, as drawn at partialTick. */
    public static Optional<Pick> pick(Level level, Vec3 from, Vec3 to, float partialTick) {
        Pick best = null;
        for (Train train : CreateClient.RAILWAYS.trains.values()) {
            CarriageEnds previous = null;
            for (int i = 0; i < train.carriages.size(); i++) {
                CarriageEnds current = couplers(entityOf(level, train.carriages.get(i)), partialTick);
                if (previous != null) {
                    Optional<int[]> pair = joined(previous, current);
                    if (pair.isPresent()) {
                        Vec3 meet = CouplingGeometry.meet(previous.couplers().get(pair.get()[0]).end(),
                                current.couplers().get(pair.get()[1]).end());
                        Optional<Vec3> hit = new AABB(meet, meet).inflate(PICK_SIZE).clip(from, to);
                        if (hit.isPresent() && (best == null || hit.get().distanceTo(from) < best.distance())) {
                            best = new Pick(train.id, i - 1, hit.get().distanceTo(from));
                        }
                    }
                }
                previous = current;
            }
        }
        return Optional.ofNullable(best);
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(TYPE);

        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        for (Train train : CreateClient.RAILWAYS.trains.values()) {
            List<CarriageEnds> carriages = new ArrayList<>();
            List<boolean[]> used = new ArrayList<>();
            for (Carriage carriage : train.carriages) {
                CarriageEnds couplers = couplers(entityOf(level, carriage), partialTick);
                carriages.add(couplers);
                used.add(new boolean[couplers.couplers().size()]);
            }
            for (int i = 0; i < carriages.size() - 1; i++) {
                CarriageEnds a = carriages.get(i), b = carriages.get(i + 1);
                Optional<int[]> pair = joined(a, b);
                if (pair.isEmpty()) {
                    continue;
                }
                used.get(i)[pair.get()[0]] = true;
                used.get(i + 1)[pair.get()[1]] = true;
                Coupler ca = a.couplers().get(pair.get()[0]), cb = b.couplers().get(pair.get()[1]);
                Vec3 meet = CouplingGeometry.meet(ca.end(), cb.end());
                if (meet.distanceToSqr(camera) > 96 * 96) {
                    continue;
                }
                int light = LevelRenderer.getLightColor(level, BlockPos.containing(meet));
                drawKnuckle(pose, consumer, ca.end(), meet, ca.local().reach(), light);
                drawKnuckle(pose, consumer, cb.end(), meet, cb.local().reach(), light);
                if (ca.local().gangway().isPresent() && cb.local().gangway().isPresent()) {
                    drawGangway(pose, consumer, ca.end(), cb.end(), ca.local().gangway().get(), ca.local().height(),
                            cb.local().gangway().get(), cb.local().height(), true, light);
                }
            }
            // couplers with no neighbour (a train's free ends): the knuckle rests straight out, half the bellows stands
            for (int i = 0; i < carriages.size(); i++) {
                for (int k = 0; k < carriages.get(i).couplers().size(); k++) {
                    Coupler free = carriages.get(i).couplers().get(k);
                    if (used.get(i)[k] || free.end().centre().distanceToSqr(camera) > 96 * 96) {
                        continue;
                    }
                    int light = LevelRenderer.getLightColor(level, BlockPos.containing(free.end().centre()));
                    drawFreeEnd(pose, consumer, free.end(), free.local().reach(), free.local().height(), free.local().gangway(), light);
                }
            }
        }
        pose.popPose();
        buffers.endBatch(TYPE);
    }

    /**
     * A parked trainset's couplers and half bellows, drawn by its anchor in the carriage's design space (blocks, the
     * carriage's front end at z 0 facing -z).
     */
    static void drawParked(PoseStack pose, VertexConsumer consumer, ParsedDesign design, int light) {
        design.design().coupler().ifPresent(coupler -> {
            double y = coupler.height() / 16 + 1;
            Vec3 up = new Vec3(0, 1, 0);
            CouplingGeometry.End front = new CouplingGeometry.End(new Vec3(0.5, y, 0), new Vec3(0, 0, -1), new Vec3(-1, 0, 0), up);
            CouplingGeometry.End back = new CouplingGeometry.End(new Vec3(0.5, y, design.length()), new Vec3(0, 0, 1), new Vec3(1, 0, 0), up);
            for (CouplingGeometry.End end : new CouplingGeometry.End[] {front, back}) {
                drawFreeEnd(pose, consumer, end, CarriageCouplers.TRAINSET_REACH, coupler.height(), coupler.gangway(), light);
            }
        });
    }

    private static void drawFreeEnd(PoseStack pose, VertexConsumer consumer, CouplingGeometry.End end, double reach, double height,
                                    Optional<GangwaySpec> gangway, int light) {
        drawKnuckle(pose, consumer, end, CouplingGeometry.rest(end, reach), reach, light);
        gangway.ifPresent(g -> {
            CouplingGeometry.End out = new CouplingGeometry.End(CouplingGeometry.rest(end, HALF_BELLOWS), end.outward().scale(-1),
                    end.right().scale(-1), end.up());
            drawGangway(pose, consumer, end, out, g, height, g, height, false, light);
        });
    }

    @Nullable
    private static CarriageContraptionEntity entityOf(Level level, Carriage carriage) {
        Carriage.DimensionalCarriageEntity dce = carriage.getDimensionalIfPresent(level.dimension());
        return dce == null ? null : dce.entity.get();
    }

    /** A carriage's couplers in the world this frame (none for a carriage without any), and its middle. */
    private static CarriageEnds couplers(@Nullable CarriageContraptionEntity entity, float partialTick) {
        if (entity == null || entity.getContraption() == null) {
            return CarriageEnds.NONE;
        }
        List<Coupler> out = new ArrayList<>();
        for (CarriageCouplers.Local local : CarriageCouplers.of(entity.getContraption())) {
            out.add(new Coupler(CarriageCouplers.end(entity, local, partialTick), local));
        }
        return new CarriageEnds(out, entity.getBoundingBox().getCenter());
    }

    /**
     * One knuckle coupler with its head at the given point: its shank from the carriage end, swivelled to point at the
     * head, and where the head is further out than the knuckle's own length, a slimmer bar slid out of the draft gear.
     */
    static void drawKnuckle(PoseStack pose, VertexConsumer consumer, CouplingGeometry.End end, Vec3 head, double reach, int light) {
        Vec3 along = head.subtract(end.centre());
        if (along.lengthSqr() < 1e-6) {
            along = end.outward().scale(1e-3);
        }
        double shank = Math.min(along.length(), reach) - HEAD_BACK;
        if (shank > 1e-3) {
            pose.pushPose();
            place(pose, end.centre(), along, end.up());
            pose.scale(1, 1, (float) shank);
            draw(pose, consumer, SHANK, light);
            pose.popPose();
        }
        if (CouplingGeometry.slide(end, head, reach) > 1e-3) {
            pose.pushPose();
            place(pose, end.centre(), along, end.up());
            pose.scale(SLIDE_THICKNESS, SLIDE_THICKNESS, (float) (along.length() - HEAD_BACK));
            draw(pose, consumer, SHANK, light);
            pose.popPose();
        }
        pose.pushPose();
        place(pose, head, along, end.up());
        draw(pose, consumer, HEAD, light);
        pose.popPose();
    }

    /**
     * Bellows folds strung from one gangway opening to another, each turned a share of the way between them so the
     * gangway bends round curves, and (between two coaches) a floor flap hinged at each end reaching past the middle.
     */
    private static void drawGangway(PoseStack pose, VertexConsumer consumer, CouplingGeometry.End a, CouplingGeometry.End b,
                                    GangwaySpec gangA, double couplerA, GangwaySpec gangB, double couplerB, boolean flaps,
                                    int light) {
        double floorA = (gangA.bottom() - couplerA) / 16, floorB = (gangB.bottom() - couplerB) / 16;
        float width = (float) (gangA.halfWidth() / 16 / BELLOWS_HALF_WIDTH);
        float height = (float) ((gangA.top() - gangA.bottom()) / 16 / BELLOWS_HEIGHT);
        double length = a.at(0, floorA).distanceTo(b.at(0, floorB));
        int folds = Math.max(2, (int) Math.round(length / FOLD));
        for (int i = 0; i < folds; i++) {
            CouplingGeometry.End from = CouplingGeometry.between(a, b, i / (double) folds);
            CouplingGeometry.End to = CouplingGeometry.between(a, b, (i + 1) / (double) folds);
            Vec3 start = from.at(0, floorA + (floorB - floorA) * i / folds);
            Vec3 step = to.at(0, floorA + (floorB - floorA) * (i + 1) / folds).subtract(start);
            if (step.lengthSqr() < 1e-8) {
                continue;
            }
            pose.pushPose();
            place(pose, start, step, from.up());
            pose.scale(width, height, (float) (step.length() / FOLD * FOLD_OVERLAP));
            draw(pose, consumer, BELLOWS, light);
            pose.popPose();
        }
        if (flaps) {
            flap(pose, consumer, a.at(0, floorA), b.at(0, floorB), a.up(), width, 0, light);
            flap(pose, consumer, b.at(0, floorB), a.at(0, floorA), b.up(), width, -0.004, light);
        }
    }

    /** A floor flap hinged at one end's floor, lying towards the other's; the second lies a hair lower. */
    private static void flap(PoseStack pose, VertexConsumer consumer, Vec3 hinge, Vec3 other, Vec3 up, float width, double drop,
                             int light) {
        Vec3 across = other.subtract(hinge);
        if (across.lengthSqr() < 1e-8) {
            return;
        }
        pose.pushPose();
        place(pose, hinge.add(up.scale(drop)), across, up);
        pose.scale(width, 1, (float) (across.length() * FLAP_REACH));
        draw(pose, consumer, FLAP, light);
        pose.popPose();
    }

    /** Moves the pose to a point and turns it so the art's +z points along forward and +y as near up as it can. */
    private static void place(PoseStack pose, Vec3 at, Vec3 forward, Vec3 up) {
        Vec3 z = forward.normalize();
        Vec3 x = up.cross(z);
        if (x.lengthSqr() < 1e-8) {
            x = new Vec3(1, 0, 0);
        }
        x = x.normalize();
        Vec3 y = z.cross(x);
        pose.translate(at.x, at.y, at.z);
        pose.mulPose(new Quaternionf().setFromNormalized(new Matrix3f(x.toVector3f(), y.toVector3f(), z.toVector3f())));
    }

    private static void draw(PoseStack pose, VertexConsumer consumer, ModelResourceLocation model, int light) {
        Minecraft mc = Minecraft.getInstance();
        mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(), consumer, null, mc.getModelManager().getModel(model),
                1, 1, 1, light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, TYPE);
    }
}
