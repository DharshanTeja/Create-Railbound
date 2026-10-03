package dev.railbound.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.railbound.Railbound;

/**
 * A bogey style's converted parts (models/bogey/<style>/frame and /wheels): the frame is drawn from Create's bogey
 * origin (bottom of the track block); the wheelset is centred on its axle and drawn at both axles.
 */
public record TrainsetBogeyModels(PartialModel frame, PartialModel wheels) {
    /** Axles sit 12/16 of a block above the bogey origin, one block either side of the centre (Create's geometry). */
    public static final float AXLE_HEIGHT = 12 / 16f;
    public static final float AXLE_OFFSET = 1;

    public static final TrainsetBogeyModels COACH = of("coach");

    private static TrainsetBogeyModels of(String style) {
        return new TrainsetBogeyModels(PartialModel.of(Railbound.rl("bogey/" + style + "/frame")),
                PartialModel.of(Railbound.rl("bogey/" + style + "/wheels")));
    }
}
