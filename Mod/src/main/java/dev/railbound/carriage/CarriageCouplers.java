package dev.railbound.carriage;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import dev.railbound.trainset.design.CouplerSpec;
import dev.railbound.trainset.design.GangwaySpec;
import dev.railbound.trainset.design.ParsedDesign;
import dev.railbound.trainset.load.TrainsetDesigns;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * A carriage's knuckle couplers: both ends of our trainsets, and a Create-built carriage's coupler blocks. Found once
 * per contraption in its own coordinates, and placed in the world wherever the carriage is; the renderer draws them and
 * the server couples trains by them.
 */
public final class CarriageCouplers {
    /**
     * A coupler's length, carriage end to coupling face (blocks): our trainsets are placed a block apart, so two of
     * theirs lock halfway; a coupler block reaches through its own block and half a block more.
     */
    public static final double TRAINSET_REACH = 0.5;
    public static final double BLOCK_REACH = 1.5;
    /** Server and client contraptions are looked up from their own threads. */
    private static final Map<Contraption, List<Local>> FOUND = Collections.synchronizedMap(new WeakHashMap<>());

    private CarriageCouplers() {}

    /** A coupler on a carriage: where its knuckle starts, which way it points out, how long it is, and any gangway. */
    public record Local(Vec3 start, Direction out, double reach, double height, Optional<GangwaySpec> gangway) {}

    /**
     * Our trainset's two ends (from its design: the design comes from the anchor's data, which travels with the
     * carriage), or a Create-built carriage's coupler blocks. Only firm answers are remembered, so a trainset seen
     * before its data or designs arrive is looked at again.
     */
    public static List<Local> of(Contraption contraption) {
        List<Local> known = FOUND.get(contraption);
        if (known != null) {
            return known;
        }
        List<Local> found = new ArrayList<>();
        for (Map.Entry<BlockPos, StructureBlockInfo> block : contraption.getBlocks().entrySet()) {
            BlockPos pos = block.getKey();
            if (block.getValue().state().getBlock() instanceof AnchorBlock) {
                Optional<ResourceLocation> id = AnchorDesign.of(block.getValue().nbt());
                if (id.isEmpty() && contraption.getBlockEntityClientSide(pos) instanceof AnchorBlockEntity anchor) {
                    id = Optional.ofNullable(anchor.designId());
                }
                Optional<ParsedDesign> design = id.flatMap(TrainsetDesigns::get);
                if (design.isEmpty()) {
                    return List.of();
                }
                Direction facing = block.getValue().state().getValue(AnchorBlock.FACING);
                design.get().design().coupler().ifPresent(coupler -> {
                    found.add(trainsetEnd(pos, design.get(), coupler, facing, 0, facing));
                    found.add(trainsetEnd(pos, design.get(), coupler, facing, design.get().length(), facing.getOpposite()));
                });
            } else if (block.getValue().state().getBlock() instanceof CouplerBlock) {
                Direction out = block.getValue().state().getValue(CouplerBlock.FACING);
                found.add(new Local(CouplingGeometry.blockEnd(pos, out), out, BLOCK_REACH, 0, Optional.empty()));
            }
        }
        FOUND.put(contraption, found);
        return found;
    }

    private static Local trainsetEnd(BlockPos anchor, ParsedDesign design, CouplerSpec coupler, Direction facing, double z,
                                     Direction out) {
        Vec3 start = Vec3.atLowerCornerOf(anchor)
                .add(CarriageTransform.toAnchorRelative(design, facing, 0.5, coupler.height() / 16 + 1, z));
        return new Local(start, out, TRAINSET_REACH, coupler.height(), coupler.gangway());
    }

    /** A coupler's end of the carriage in the world, where the carriage is at partialTick (1 on the server). */
    public static CouplingGeometry.End end(CarriageContraptionEntity entity, Local local, float partialTick) {
        Vec3 centre = global(entity, local.start(), partialTick);
        Vec3 outward = global(entity, local.start().add(Vec3.atLowerCornerOf(local.out().getNormal())), partialTick).subtract(centre);
        Vec3 right = global(entity, local.start().add(Vec3.atLowerCornerOf(local.out().getClockWise().getNormal())), partialTick)
                .subtract(centre);
        Vec3 up = global(entity, local.start().add(0, 1, 0), partialTick).subtract(centre);
        return new CouplingGeometry.End(centre, outward.normalize(), right.normalize(), up.normalize());
    }

    /**
     * A point on a carriage in the world, where the carriage is drawn this frame. Create's toGlobalVector turns by the
     * in-between rotation but adds this tick's position, a tick ahead of the drawn body at speed; use the in-between
     * position too.
     */
    private static Vec3 global(CarriageContraptionEntity entity, Vec3 local, float partialTick) {
        return entity.toGlobalVector(local, partialTick).subtract(entity.position()).add(entity.getPosition(partialTick));
    }
}
