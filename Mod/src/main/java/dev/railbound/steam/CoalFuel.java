package dev.railbound.steam;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/** What a steam loco's bunker takes: the coal family only, counted in lumps of coal. */
public final class CoalFuel {
    private static final Map<ResourceLocation, Integer> LUMPS = Map.of(
            ResourceLocation.withDefaultNamespace("coal"), 1,
            ResourceLocation.withDefaultNamespace("charcoal"), 1,
            ResourceLocation.withDefaultNamespace("coal_block"), 9);

    private CoalFuel() {}

    /** Lumps of coal one item is worth, 0 for anything a loco does not burn. */
    public static int value(ResourceLocation item) {
        return LUMPS.getOrDefault(item, 0);
    }

    public static boolean accepts(ResourceLocation item) {
        return value(item) > 0;
    }
}
