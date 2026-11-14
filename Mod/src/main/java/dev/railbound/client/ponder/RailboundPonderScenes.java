package dev.railbound.client.ponder;

import dev.railbound.registry.RailboundItems;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Which ponder scene belongs to which item, and which schematic (assets/railbound/ponder/NAME.nbt) each one stands on. */
public final class RailboundPonderScenes {
    public static final String PLACE_TRAIN = "place_train";

    /** Every schematic the scenes below use, so a test can check each file exists and is sound. */
    public static final List<String> SCHEMATICS = List.of(PLACE_TRAIN);

    private RailboundPonderScenes() {}

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.addStoryBoard(RailboundItems.TRAINSET.getId(), PLACE_TRAIN, TrainsetScenes::placeTrain);
    }
}
