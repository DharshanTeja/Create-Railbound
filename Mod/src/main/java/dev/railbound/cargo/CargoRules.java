package dev.railbound.cargo;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Goods wagon rules apart from blocks and screens: when a hold opens, and buckets in and out of a tank. */
public final class CargoRules {
    /** Menu data travels as shorts: amounts go in two 15-bit halves. */
    private static final int HALF = 1 << 15;

    private CargoRules() {}

    /** A wagon on a train opens only while the train stands still (Create's speed, blocks per tick). */
    public static boolean canOpenOnTrain(double speed) {
        return Math.abs(speed) < 1e-3;
    }

    public static int[] split(int amount) {
        return new int[] {amount % HALF, amount / HALF};
    }

    public static int join(int low, int high) {
        return high * HALF + low;
    }

    /** What is left in the slot, and what goes back to the player (an emptied or filled container). */
    public record Poured(ItemStack left, ItemStack giveBack) {}

    /** One full container from the slot poured into the tank, if the tank takes all of it. */
    public static Poured pour(ItemStack slot, IFluidHandler tank) {
        return oneAtATime(slot, FluidUtil.tryEmptyContainer(slot.copyWithCount(1), tank, Integer.MAX_VALUE, null, true));
    }

    /** One empty container from the slot filled from the tank, if the tank has enough. */
    public static Poured draw(ItemStack slot, IFluidHandler tank) {
        return oneAtATime(slot, FluidUtil.tryFillContainer(slot.copyWithCount(1), tank, Integer.MAX_VALUE, null, true));
    }

    private static Poured oneAtATime(ItemStack slot, FluidActionResult result) {
        if (!result.isSuccess()) {
            return new Poured(slot, ItemStack.EMPTY);
        }
        ItemStack left = slot.copy();
        left.shrink(1);
        return new Poured(left, result.getResult());
    }
}
