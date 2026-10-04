package dev.railbound.steam;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.api.contraption.storage.SyncedMountedStorage;
import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorage;
import com.simibubi.create.content.contraptions.Contraption;
import dev.railbound.registry.RailboundStorageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** A steam loco's water on a train. Portable Fluid Interfaces can top it up, but never drain it. */
public class BunkerFluidStorage extends MountedFluidStorage implements SyncedMountedStorage {
    public static final MapCodec<BunkerFluidStorage> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("capacity").forGetter(BunkerFluidStorage::capacity),
            Codec.INT.fieldOf("amount").forGetter(BunkerFluidStorage::amount)
    ).apply(i, BunkerFluidStorage::new));

    private final int capacity;
    private int amount;
    private boolean dirty;
    private final WaterTank tank = new WaterTank(this::amount, this::capacity, this::setAmount);

    private BunkerFluidStorage(int capacity, int amount) {
        super(RailboundStorageTypes.BUNKER_WATER.get());
        this.capacity = capacity;
        this.amount = Math.min(capacity, amount);
    }

    public static BunkerFluidStorage of(BunkerBlockEntity bunker) {
        return new BunkerFluidStorage(bunker.steam().water(), bunker.water());
    }

    public int capacity() {
        return capacity;
    }

    public int amount() {
        return amount;
    }

    public void setAmount(int amount) {
        int clamped = Math.max(0, Math.min(capacity, amount));
        // the gauge only needs to follow whole buckets; the boiler sips a little every tick
        dirty |= clamped / 1000 != this.amount / 1000 || (clamped == 0) != (this.amount == 0);
        this.amount = clamped;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        return this.tank.getFluidInTank(tank);
    }

    @Override
    public int getTankCapacity(int tank) {
        return capacity;
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return this.tank.isFluidValid(tank, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return tank.fill(resource, action);
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        return FluidStack.EMPTY;
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        return FluidStack.EMPTY;
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (be instanceof BunkerBlockEntity bunker) {
            bunker.restoreWater(amount);
        }
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void markClean() {
        dirty = false;
    }

    @Override
    public void afterSync(Contraption contraption, BlockPos localPos) {
    }
}
