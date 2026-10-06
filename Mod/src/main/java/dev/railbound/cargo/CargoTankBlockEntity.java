package dev.railbound.cargo;

import dev.railbound.registry.RailboundBlockEntities;
import dev.railbound.trainset.load.TrainsetDesigns;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * A parked tank wagon's tank: one fluid, as much as its design's {@code fluid_mb} (144 buckets for the tank wagon,
 * like a 3x3x2 Create tank). On a train the same load travels in {@link TankFluidStorage}.
 */
public class CargoTankBlockEntity extends BlockEntity {
    public static final int DEFAULT_CAPACITY = 144_000;

    @Nullable
    private ResourceLocation designId;
    private final FluidTank tank = new FluidTank(DEFAULT_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    public CargoTankBlockEntity(BlockPos pos, BlockState state) {
        super(RailboundBlockEntities.CARGO_TANK.get(), pos, state);
    }

    /** Placement tells the tank which design it belongs to: that sets how much it holds. */
    public void setDesignId(ResourceLocation designId) {
        this.designId = designId;
        tank.setCapacity(capacityOf(designId));
        setChanged();
    }

    public static int capacityOf(@Nullable ResourceLocation designId) {
        return Optional.ofNullable(designId).flatMap(TrainsetDesigns::get)
                .map(design -> design.design().cargo().fluidMb())
                .filter(mb -> mb > 0)
                .orElse(DEFAULT_CAPACITY);
    }

    public FluidTank tank() {
        return tank;
    }

    public boolean isEmpty() {
        return tank.isEmpty();
    }

    /** Written back by the mounted tank when the train is disassembled. */
    void restoreFromTrain(int capacity, FluidStack fluid) {
        tank.setCapacity(capacity);
        tank.setFluid(fluid.copy());
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (designId != null) {
            tag.putString("Design", designId.toString());
        }
        tag.putInt("Capacity", tank.getCapacity());
        tank.writeToNBT(registries, tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        designId = tag.contains("Design") ? ResourceLocation.tryParse(tag.getString("Design")) : null;
        tank.setCapacity(tag.contains("Capacity") ? tag.getInt("Capacity") : DEFAULT_CAPACITY);
        tank.readFromNBT(registries, tag);
    }
}
