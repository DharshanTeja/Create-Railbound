package dev.railbound.carriage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AnchorDesignTest {

    @Test
    void readsTheDesignFromTheAnchorsSavedData() {
        // a moving carriage keeps each block's data with it; the anchor's tells which design it is
        CompoundTag data = new CompoundTag();
        data.putString("Design", "railbound:coach_standard");
        assertEquals(Optional.of(ResourceLocation.parse("railbound:coach_standard")), AnchorDesign.of(data));
    }

    @Test
    void noDataOrNoDesignMeansNone() {
        assertEquals(Optional.empty(), AnchorDesign.of(null));
        assertEquals(Optional.empty(), AnchorDesign.of(new CompoundTag()));
    }

    @Test
    void aBrokenIdMeansNone() {
        CompoundTag data = new CompoundTag();
        data.putString("Design", "Not A Valid:id!");
        assertEquals(Optional.empty(), AnchorDesign.of(data));
    }
}
