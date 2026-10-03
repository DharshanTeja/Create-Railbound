package dev.railbound.trainset.design;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum PowerType implements StringRepresentable {
    NONE("none"),
    STEAM("steam"),
    DIESEL("diesel"),
    ELECTRIC("electric");

    public static final Codec<PowerType> CODEC = StringRepresentable.fromEnum(PowerType::values);

    private final String id;

    PowerType(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
