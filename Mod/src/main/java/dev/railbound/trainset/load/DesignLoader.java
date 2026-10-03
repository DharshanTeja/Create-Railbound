package dev.railbound.trainset.load;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.railbound.trainset.design.DesignValidator;
import dev.railbound.trainset.design.TrainsetDesign;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public final class DesignLoader {
    private DesignLoader() {}

    public record LoadResult(Map<ResourceLocation, TrainsetDesign> designs, List<String> errors) {}

    public static LoadResult load(Map<ResourceLocation, JsonElement> jsons) {
        Map<ResourceLocation, TrainsetDesign> designs = new TreeMap<>();
        List<String> errors = new ArrayList<>();

        new TreeMap<>(jsons).forEach((id, json) -> {
            DataResult<TrainsetDesign> decoded = TrainsetDesign.CODEC.parse(JsonOps.INSTANCE, json);
            Optional<TrainsetDesign> design = decoded.result();
            if (design.isEmpty()) {
                errors.add(id + ": " + decoded.error().map(DataResult.Error::message).orElse("could not be decoded"));
                return;
            }
            List<String> problems = DesignValidator.validate(design.get());
            if (!problems.isEmpty()) {
                problems.forEach(problem -> errors.add(id + ": " + problem));
                return;
            }
            designs.put(id, design.get());
        });
        return new LoadResult(designs, errors);
    }
}
