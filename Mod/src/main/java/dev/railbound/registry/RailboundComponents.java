package dev.railbound.registry;

import dev.railbound.Railbound;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RailboundComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Railbound.MOD_ID);

    /** Which trainset design an item stack places. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> TRAINSET_DESIGN =
            COMPONENTS.register("trainset_design", () -> DataComponentType.<ResourceLocation>builder()
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC)
                    .build());

    private RailboundComponents() {}
}
