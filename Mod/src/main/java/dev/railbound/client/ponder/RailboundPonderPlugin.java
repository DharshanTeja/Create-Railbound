package dev.railbound.client.ponder;

import dev.railbound.Railbound;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/** Adds Railbound's scenes to Create's Ponder (hold W over one of our items). */
public class RailboundPonderPlugin implements PonderPlugin {
    @Override
    public String getModId() {
        return Railbound.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        RailboundPonderScenes.register(helper);
    }
}
