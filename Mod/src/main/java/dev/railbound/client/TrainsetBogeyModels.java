package dev.railbound.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.railbound.Railbound;

/**
 * A bogey style's converted parts (models/bogey/<style>/frame and /wheels): the frame is drawn from Create's bogey
 * origin (bottom of the track block); the wheelset is centred on its axle and drawn at both axles.
 */
public record TrainsetBogeyModels(PartialModel frame, PartialModel wheels, float axleOffset) {
    /** Axles sit 12/16 of a block above the bogey origin (Create's axle height). */
    public static final float AXLE_HEIGHT = 12 / 16f;

    /** Axles one block either side of the centre, as Create's own bogeys. */
    public static final TrainsetBogeyModels COACH = of("coach", 1);
    /** Axles 14 px either side, clear of the loco's driving wheels; must match the art (the converter checks it is even). */
    public static final TrainsetBogeyModels STEAM_TRUCK = of("steam_truck", 14 / 16f);

    private static TrainsetBogeyModels of(String style, float axleOffset) {
        return new TrainsetBogeyModels(PartialModel.of(Railbound.rl("bogey/" + style + "/frame")),
                PartialModel.of(Railbound.rl("bogey/" + style + "/wheels")), axleOffset);
    }
}
