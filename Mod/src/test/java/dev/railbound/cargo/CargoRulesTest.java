package dev.railbound.cargo;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CargoRulesTest {
    @Test
    void aWagonOpensOnlyOnAStoppedTrain() {
        assertTrue(CargoRules.canOpenOnTrain(0));
        assertFalse(CargoRules.canOpenOnTrain(0.2));
        assertFalse(CargoRules.canOpenOnTrain(-0.05));
    }

    @Test
    void tankAmountsBeyondAShortReachTheScreenWhole() {
        // Minecraft sends menu data as shorts; 144 buckets is 144000 millibuckets
        for (int amount : new int[] {0, 1, 32767, 32768, 144_000, 1_000_000}) {
            int[] data = CargoRules.split(amount);
            assertTrue(data[0] <= Short.MAX_VALUE && data[1] <= Short.MAX_VALUE);
            assertEquals(amount, CargoRules.join(data[0], data[1]));
        }
    }

    @Test
    void aFullBucketPoursIntoTheTankAndTheEmptyBucketComesBack() {
        FluidTank tank = new FluidTank(144_000);
        CargoRules.Poured result = CargoRules.pour(new ItemStack(Items.WATER_BUCKET), tank);
        assertEquals(1000, tank.getFluidAmount());
        assertTrue(ItemStack.matches(new ItemStack(Items.BUCKET), result.giveBack()));
        assertTrue(result.left().isEmpty());
    }

    @Test
    void anEmptyBucketFillsFromTheTank() {
        FluidTank tank = new FluidTank(144_000);
        tank.fill(new FluidStack(Fluids.WATER, 2000), FluidTank.FluidAction.EXECUTE);
        CargoRules.Poured result = CargoRules.draw(new ItemStack(Items.BUCKET, 3), tank);
        assertEquals(1000, tank.getFluidAmount());
        assertTrue(ItemStack.matches(new ItemStack(Items.WATER_BUCKET), result.giveBack()));
        assertEquals(2, result.left().getCount(), "the other empty buckets stay in the slot");
    }

    @Test
    void theTankTakesOnlyTheFluidItHolds() {
        FluidTank tank = new FluidTank(144_000);
        tank.fill(new FluidStack(Fluids.WATER, 1000), FluidTank.FluidAction.EXECUTE);
        CargoRules.Poured result = CargoRules.pour(new ItemStack(Items.LAVA_BUCKET), tank);
        assertEquals(1000, tank.getFluidAmount());
        assertTrue(result.giveBack().isEmpty());
        assertTrue(ItemStack.matches(new ItemStack(Items.LAVA_BUCKET), result.left()), "the lava bucket stays put");
    }

    @Test
    void anEmptyTankFillsNoBuckets() {
        CargoRules.Poured result = CargoRules.draw(new ItemStack(Items.BUCKET), new FluidTank(144_000));
        assertTrue(result.giveBack().isEmpty());
        assertEquals(1, result.left().getCount());
    }
}
