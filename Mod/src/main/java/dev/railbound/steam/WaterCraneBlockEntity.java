package dev.railbound.steam;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import dev.railbound.registry.RailboundBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

/**
 * A water crane: a pass-through for water into the loco whose tank filler is in reach, standing or stopped on a
 * train. It holds no water itself. While it fills, its arm swings round and out over the filler and lowers the hose
 * onto it; a little after, it swings back along the track.
 */
public class WaterCraneBlockEntity extends BlockEntity {
    /** How long after the last fill the arm stays out (ticks), and how often the loco under it is looked for again. */
    private static final int FILL_HOLD = 20;
    private static final int RESCAN = 5;

    private long lastFill = -1000;
    private boolean filling;
    private double swing;
    private double prevSwing;
    private IFluidHandler target;
    private long targetFound = -1000;
    /** The last filler reached for, from the base's bottom centre (zero: none yet), and the one clients last got. */
    private Vec3 filler = Vec3.ZERO;
    private Vec3 sentFiller = Vec3.ZERO;

    private final IFluidHandler handler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return 1000;
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return stack.getFluid().isSame(Fluids.WATER);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (level == null || level.isClientSide || resource.isEmpty() || !resource.getFluid().isSame(Fluids.WATER)) {
                return 0;
            }
            IFluidHandler loco = locoUnderHose();
            if (loco == null) {
                return 0;
            }
            int taken = loco.fill(resource, action);
            if (taken > 0 && action.execute()) {
                lastFill = level.getGameTime();
            }
            return taken;
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    };

    public WaterCraneBlockEntity(BlockPos pos, BlockState state) {
        super(RailboundBlockEntities.WATER_CRANE.get(), pos, state);
    }

    public IFluidHandler waterIn() {
        return handler;
    }

    private Vec3 base() {
        return Vec3.atBottomCenterOf(worldPosition);
    }

    /** A loco's water and where its tank filler is (the top of a water tank cell). */
    private record Filler(IFluidHandler water, Vec3 at) {}

    /** The water of the loco with a tank filler in reach, nearest first: on a stopped train, or standing. */
    private IFluidHandler locoUnderHose() {
        long now = level.getGameTime();
        if (now - targetFound < RESCAN) {
            return target;
        }
        targetFound = now;
        Optional<Filler> found = trainFiller().or(this::standingFiller);
        target = found.map(Filler::water).orElse(null);
        found.ifPresent(f -> filler = f.at().subtract(base()));
        return target;
    }

    private Optional<Filler> trainFiller() {
        Filler best = null;
        for (CarriageContraptionEntity entity : level.getEntitiesOfClass(CarriageContraptionEntity.class,
                new AABB(worldPosition).inflate(WaterCrane.MAX_REACH + 1, WaterCrane.ARM_HEIGHT, WaterCrane.MAX_REACH + 1))) {
            Carriage carriage = entity.getCarriage();
            Contraption contraption = entity.getContraption();
            if (carriage == null || carriage.train == null || contraption == null || Math.abs(carriage.train.speed) > 1e-3) {
                continue;
            }
            Optional<IFluidHandler> water = LocoAccess.tankOn(contraption).map(tank -> tank);
            if (water.isEmpty()) {
                continue;
            }
            for (Map.Entry<BlockPos, StructureBlockInfo> block : contraption.getBlocks().entrySet()) {
                if (block.getValue().state().getBlock() instanceof WaterTankBlock) {
                    Vec3 at = entity.toGlobalVector(Vec3.atCenterOf(block.getKey()).add(0, 0.5, 0), 1);
                    best = nearer(best, new Filler(water.get(), at));
                }
            }
        }
        return Optional.ofNullable(best);
    }

    private Optional<Filler> standingFiller() {
        Filler best = null;
        int reach = (int) Math.ceil(WaterCrane.MAX_REACH);
        for (BlockPos pos : BlockPos.betweenClosed(worldPosition.offset(-reach, -1, -reach),
                worldPosition.offset(reach, (int) WaterCrane.ARM_HEIGHT, reach))) {
            if (level.getBlockState(pos).getBlock() instanceof WaterTankBlock) {
                Optional<IFluidHandler> water = LocoAccess.bunkerOf(level, pos.immutable()).map(BunkerBlockEntity::waterIn);
                if (water.isPresent()) {
                    best = nearer(best, new Filler(water.get(), Vec3.atCenterOf(pos).add(0, 0.5, 0)));
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /** The nearer in-reach filler of the two. */
    private Filler nearer(Filler best, Filler candidate) {
        if (!WaterCrane.inReach(base(), candidate.at())) {
            return best;
        }
        return best == null || candidate.at().distanceToSqr(base()) < best.at().distanceToSqr(base()) ? candidate : best;
    }

    /** Both sides: the server decides whether the arm is out, clients swing it. */
    public void tick() {
        if (level == null) {
            return;
        }
        if (!level.isClientSide) {
            boolean now = level.getGameTime() - lastFill < FILL_HOLD;
            if (now != filling || (now && !filler.equals(sentFiller))) {
                filling = now;
                sentFiller = filler;
                setChanged();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
            return;
        }
        // no drips from the nozzle: particles only stop on real blocks, so on a train they fell through the loco
        prevSwing = swing;
        swing = WaterCrane.swingTowards(swing, filling);
    }

    private net.minecraft.core.Direction facing() {
        return getBlockState().getValue(WaterCraneBlock.FACING);
    }

    /** Client: the arm's pose between parked and on the last filler it reached for. */
    public WaterCrane.Aim aim(float partialTick) {
        WaterCrane.Aim target = filler.equals(Vec3.ZERO) ? WaterCrane.PARKED : WaterCrane.aim(base(), facing(), base().add(filler));
        return WaterCrane.swung(target, prevSwing + (swing - prevSwing) * partialTick);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Filling", filling);
        tag.putDouble("FillerX", filler.x);
        tag.putDouble("FillerY", filler.y);
        tag.putDouble("FillerZ", filler.z);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        filling = tag.getBoolean("Filling");
        filler = new Vec3(tag.getDouble("FillerX"), tag.getDouble("FillerY"), tag.getDouble("FillerZ"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
