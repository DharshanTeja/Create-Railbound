package dev.railbound.steam;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoalFuelTest {
    private static ResourceLocation mc(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    @Test
    void coalAndCharcoalAreOneLumpEach() {
        assertEquals(1, CoalFuel.value(mc("coal")));
        assertEquals(1, CoalFuel.value(mc("charcoal")));
    }

    @Test
    void aCoalBlockIsNineLumps() {
        assertEquals(9, CoalFuel.value(mc("coal_block")));
    }

    @Test
    void otherFuelsAreRefused() {
        assertEquals(0, CoalFuel.value(mc("oak_planks")));
        assertEquals(0, CoalFuel.value(mc("blaze_rod")));
        assertEquals(0, CoalFuel.value(mc("lava_bucket")));
        assertFalse(CoalFuel.accepts(mc("oak_log")));
        assertTrue(CoalFuel.accepts(mc("coal_block")));
    }
}
