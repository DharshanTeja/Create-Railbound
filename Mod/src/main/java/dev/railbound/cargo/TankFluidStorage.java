package dev.railbound.cargo;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.api.contraption.storage.fluid.WrapperMountedFluidStorage;
import dev.railbound.registry.RailboundStorageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * A tank wagon's tank on a train. Create counts it as the train's cargo, so Portable Fluid Interfaces fill and drain
 * it and schedules and observers see it. Players open it through {@link CargoMovingInteraction}.
 */
public class TankFluidStorage extends WrapperMountedFluidStorage<FluidTank> {
    public static final MapCodec<TankFluidStorage> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("capacity").forGetter(s -> s.wrapped.getCapacity()),
            FluidStack.OPTIONAL_CODEC.fieldOf("fluid").forGetter(s -> s.wrapped.getFluid())
    ).apply(i, TankFluidStorage::new));

    private TankFluidStorage(int capacity, FluidStack fluid) {
        super(RailboundStorageTypes.TANK_FLUID.get(), tank(capacity, fluid));
    }

    private static FluidTank tank(int capacity, FluidStack fluid) {
        FluidTank tank = new FluidTank(capacity);
        tank.setFluid(fluid.copy());
        return tank;
    }

    public static TankFluidStorage of(CargoTankBlockEntity tank) {
        return new TankFluidStorage(tank.tank().getCapacity(), tank.tank().getFluid());
    }

    /** The tank itself, for the tank screen. */
    public FluidTank load() {
        return wrapped;
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (be instanceof CargoTankBlockEntity tank) {
            tank.restoreFromTrain(wrapped.getCapacity(), wrapped.getFluid());
        }
    }
}
