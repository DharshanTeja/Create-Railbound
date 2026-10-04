package dev.railbound.carriage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * The design an anchor holds, as saved in its data. A moving carriage keeps every block's data with it, so this works
 * on a train even where Create has not built the client copies of its block entities.
 */
public final class AnchorDesign {
    public static final String KEY = "Design";

    private AnchorDesign() {}

    public static Optional<ResourceLocation> of(@Nullable CompoundTag data) {
        if (data == null || !data.contains(KEY)) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(data.getString(KEY)));
    }
}
