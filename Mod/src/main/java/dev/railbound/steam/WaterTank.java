package dev.railbound.steam;

import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** A loco's water as seen from outside: water goes in up to the tank's capacity, nothing comes out. */
public record WaterTank(IntSupplier amount, IntSupplier capacity, IntConsumer setAmount) implements IFluidHandler {

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        int water = amount.getAsInt();
        return water <= 0 ? FluidStack.EMPTY : new FluidStack(Fluids.WATER, water);
    }

    @Override
    public int getTankCapacity(int tank) {
        return capacity.getAsInt();
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return stack.getFluid().isSame(Fluids.WATER);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !isFluidValid(0, resource)) {
            return 0;
        }
        int accepted = Math.max(0, Math.min(resource.getAmount(), capacity.getAsInt() - amount.getAsInt()));
        if (action.execute() && accepted > 0) {
            setAmount.accept(amount.getAsInt() + accepted);
        }
        return accepted;
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        return FluidStack.EMPTY;
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        return FluidStack.EMPTY;
    }
}
