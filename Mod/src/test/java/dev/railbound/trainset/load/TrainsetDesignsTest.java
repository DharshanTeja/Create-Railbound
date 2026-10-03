package dev.railbound.trainset.load;

import dev.railbound.testutil.TestDesigns;
import dev.railbound.trainset.design.ParsedDesign;
import dev.railbound.trainset.design.TrainsetDesign;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TrainsetDesignsTest {

    @AfterEach
    void reset() {
        TrainsetDesigns.replace(Map.of());
    }

    @Test
    void replaceAndGet() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("railbound", "coach_standard");
        TrainsetDesign sample = TestDesigns.sample();
        TrainsetDesigns.replace(Map.of(id, sample));
        assertEquals(Optional.of(sample), TrainsetDesigns.get(id).map(ParsedDesign::design));
        assertEquals(Optional.empty(), TrainsetDesigns.get(ResourceLocation.fromNamespaceAndPath("railbound", "missing")));
    }

    @Test
    void getNullIsEmpty() {
        assertEquals(Optional.empty(), TrainsetDesigns.get(null));
    }

    @Test
    void rawDesignsReturnsTheOriginals() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("railbound", "coach_standard");
        TrainsetDesigns.replace(Map.of(id, TestDesigns.sample()));
        assertEquals(Map.of(id, TestDesigns.sample()), TrainsetDesigns.rawDesigns());
    }

    @Test
    void allIsSortedById() {
        Map<ResourceLocation, TrainsetDesign> input = new HashMap<>();
        input.put(ResourceLocation.fromNamespaceAndPath("railbound", "zeta"), TestDesigns.sample());
        input.put(ResourceLocation.fromNamespaceAndPath("railbound", "alpha"), TestDesigns.sample());
        TrainsetDesigns.replace(input);
        assertEquals(List.of("alpha", "zeta"),
                TrainsetDesigns.all().keySet().stream().map(ResourceLocation::getPath).toList());
    }

    @Test
    void replaceNotifiesChangeListeners() {
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        TrainsetDesigns.addChangeListener(calls::incrementAndGet);
        TrainsetDesigns.replace(Map.of(ResourceLocation.fromNamespaceAndPath("railbound", "coach_standard"), TestDesigns.sample()));
        assertEquals(1, calls.get());
        TrainsetDesigns.replace(Map.of());
        assertEquals(2, calls.get());
    }

    @Test
    void allIsUnmodifiable() {
        TrainsetDesigns.replace(Map.of());
        assertThrows(UnsupportedOperationException.class,
                () -> TrainsetDesigns.all().put(ResourceLocation.fromNamespaceAndPath("x", "y"), ParsedDesign.of(TestDesigns.sample())));
    }
}
