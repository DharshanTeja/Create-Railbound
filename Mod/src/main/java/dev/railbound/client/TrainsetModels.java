package dev.railbound.client;

import dev.railbound.Railbound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.Optional;

/**
 * Trainset models live at {@code assets/<ns>/models/trainset/<design>/<part>.json} (made by the converter).
 * Every one found in the loaded resource packs is registered, so resource packs can add designs too.
 */
public final class TrainsetModels {
    public static final String BODY = "body";
    public static final String LAMPS = "lamps";
    private static final String FOLDER = "models/trainset";

    private TrainsetModels() {}

    public static void register(ModelEvent.RegisterAdditional event) {
        int count = 0;
        for (ResourceLocation file : Minecraft.getInstance().getResourceManager()
                .listResources(FOLDER, path -> path.getPath().endsWith(".json")).keySet()) {
            String path = file.getPath().substring("models/".length(), file.getPath().length() - ".json".length());
            event.register(ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(file.getNamespace(), path)));
            count++;
        }
        Railbound.LOGGER.info("Registered {} trainset model part(s)", count);
    }

    public static ModelResourceLocation location(ResourceLocation design, String part) {
        return ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath(design.getNamespace(), "trainset/" + design.getPath() + "/" + part));
    }

    /** The baked part, or empty when the design has no such part (or no model at all). */
    public static Optional<BakedModel> get(ResourceLocation design, String part) {
        ModelManager models = Minecraft.getInstance().getModelManager();
        BakedModel model = models.getModel(location(design, part));
        return model == models.getMissingModel() ? Optional.empty() : Optional.of(model);
    }
}
