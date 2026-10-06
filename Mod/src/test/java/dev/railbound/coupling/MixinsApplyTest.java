package dev.railbound.coupling;

import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.Train;
import dev.railbound.mixin.CarriageBogeyAccessor;
import dev.railbound.mixin.TrainAccessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Loads the Create classes our mixins change, so a mixin that no longer fits Create fails here, not when a player's
 * world loads its first train. (NeoForge's unit tests run with the mod and its mixins loaded.)
 */
class MixinsApplyTest {
    @Test
    void theTrainAndBogeyAccessorsAreApplied() {
        assertTrue(TrainAccessor.class.isAssignableFrom(Train.class));
        assertTrue(CarriageBogeyAccessor.class.isAssignableFrom(CarriageBogey.class));
    }

    @Test
    void everyClassOurCarriageMixinsChangeLoads() {
        for (String name : new String[] {
                "com.simibubi.create.content.trains.entity.Carriage",
                "com.simibubi.create.content.trains.entity.Carriage$DimensionalCarriageEntity",
                "com.simibubi.create.content.trains.entity.CarriageBogey",
                "com.simibubi.create.content.trains.entity.CarriageContraptionEntity",
                "com.simibubi.create.content.trains.entity.Train",
                "com.simibubi.create.content.trains.bogey.AbstractBogeyBlockEntity",
                "com.simibubi.create.content.contraptions.AbstractContraptionEntity",
                "com.simibubi.create.content.trains.entity.CarriageSounds",
                "com.simibubi.create.content.trains.entity.CarriageContraption",
                "com.simibubi.create.content.trains.station.StationBlockEntity",
                "com.simibubi.create.content.trains.station.StationScreen"}) {
            assertDoesNotThrow(() -> Class.forName(name, true, getClass().getClassLoader()), name);
        }
    }
}
