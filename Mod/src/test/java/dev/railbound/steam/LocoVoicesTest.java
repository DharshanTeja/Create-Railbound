package dev.railbound.steam;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocoVoicesTest {
    private static final UUID TRAIN = UUID.randomUUID();
    private static final Vec3 FRONT = new Vec3(100, 66, 0);
    private static final Vec3 BACK = new Vec3(60, 66, 0);

    @BeforeEach
    void forget() {
        LocoVoices.clear();
    }

    @Test
    void aTrainWhoseLocosWereHeardThisTickHasItsOwnVoices() {
        assertFalse(LocoVoices.hasLoco(TRAIN, 10));
        LocoVoices.heard(TRAIN, 1, FRONT, 10);
        assertTrue(LocoVoices.hasLoco(TRAIN, 10));
        assertFalse(LocoVoices.hasLoco(UUID.randomUUID(), 10), "another train");
    }

    @Test
    void theWhistleSoundsFromTheLocoNearestTheListener() {
        LocoVoices.heard(TRAIN, 1, FRONT, 10);
        LocoVoices.heard(TRAIN, 2, BACK, 10);
        assertEquals(Optional.of(FRONT), LocoVoices.nearestWhistle(TRAIN, new Vec3(95, 66, 2), 10), "riding the front loco");
        assertEquals(Optional.of(BACK), LocoVoices.nearestWhistle(TRAIN, new Vec3(58, 66, 2), 10), "riding the back loco");
    }

    @Test
    void aLocoNoLongerHeardFallsSilent() {
        // uncoupled, unloaded or gone: its last place is forgotten after a few ticks
        LocoVoices.heard(TRAIN, 1, FRONT, 10);
        LocoVoices.heard(TRAIN, 2, BACK, 20);
        assertEquals(Optional.of(BACK), LocoVoices.nearestWhistle(TRAIN, FRONT, 20));
        assertFalse(LocoVoices.hasLoco(TRAIN, 40));
        assertEquals(Optional.empty(), LocoVoices.nearestWhistle(TRAIN, FRONT, 40));
    }

    @Test
    void aLocoHeardAgainMovesItsWhistle() {
        LocoVoices.heard(TRAIN, 1, FRONT, 10);
        LocoVoices.heard(TRAIN, 1, BACK, 11);
        assertEquals(Optional.of(BACK), LocoVoices.nearestWhistle(TRAIN, FRONT, 11));
    }
}
