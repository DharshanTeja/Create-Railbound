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
 * A water crane: a pass-through for water into the loco under its hose, standing or stopped on a train. It holds
 * no water itself. Its arm swings out while it fills and back along the track a little after.
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

    /** The water of the loco whose tank filler hangs within reach of the hose: on a stopped train, or standing. */
    private IFluidHandler locoUnderHose() {
        long now = level.getGameTime();
        if (now - targetFound < RESCAN) {
            return target;
        }
        targetFound = now;
        Vec3 spout = WaterCrane.spout(worldPosition, getBlockState().getValue(WaterCraneBlock.FACING));
        target = trainUnder(spout).or(() -> standingUnder(spout)).orElse(null);
        return target;
    }

    private Optional<IFluidHandler> trainUnder(Vec3 spout) {
        for (CarriageContraptionEntity entity : level.getEntitiesOfClass(CarriageContraptionEntity.class,
                new AABB(spout, spout).inflate(WaterCrane.REACH + 1))) {
            Carriage carriage = entity.getCarriage();
            Contraption contraption = entity.getContraption();
            if (carriage == null || carriage.train == null || contraption == null || Math.abs(carriage.train.speed) > 1e-3) {
                continue;
            }
            for (Map.Entry<BlockPos, StructureBlockInfo> block : contraption.getBlocks().entrySet()) {
                if (block.getValue().state().getBlock() instanceof WaterTankBlock
                        && WaterCrane.inReach(spout, entity.toGlobalVector(Vec3.atCenterOf(block.getKey()), 1))) {
                    return LocoAccess.tankOn(contraption).map(tank -> tank);
                }
            }
        }
        return Optional.empty();
    }

    private Optional<IFluidHandler> standingUnder(Vec3 spout) {
        BlockPos centre = BlockPos.containing(spout);
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-2, -2, -2), centre.offset(2, 2, 2))) {
            if (level.getBlockState(pos).getBlock() instanceof WaterTankBlock && WaterCrane.inReach(spout, Vec3.atCenterOf(pos))) {
                return LocoAccess.bunkerOf(level, pos.immutable()).map(BunkerBlockEntity::waterIn);
            }
        }
        return Optional.empty();
    }

    /** Both sides: the server decides whether the arm is out, clients swing it. */
    public void tick() {
        if (level == null) {
            return;
        }
        if (!level.isClientSide) {
            boolean now = level.getGameTime() - lastFill < FILL_HOLD;
            if (now != filling) {
                filling = now;
                setChanged();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
            return;
        }
        prevSwing = swing;
        swing = WaterCrane.swingTowards(swing, filling);
        if (filling && swing >= 1) {
            Vec3 spout = WaterCrane.spout(worldPosition, getBlockState().getValue(WaterCraneBlock.FACING));
            level.addParticle(net.minecraft.core.particles.ParticleTypes.FALLING_WATER,
                    spout.x + (level.random.nextDouble() - 0.5) * 0.15, spout.y, spout.z + (level.random.nextDouble() - 0.5) * 0.15,
                    0, 0, 0);
        }
    }

    /** Client: the arm's angle from straight out over the track, in degrees. */
    public double armAngle(float partialTick) {
        return WaterCrane.armAngle(prevSwing + (swing - prevSwing) * partialTick);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Filling", filling);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        filling = tag.getBoolean("Filling");
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
