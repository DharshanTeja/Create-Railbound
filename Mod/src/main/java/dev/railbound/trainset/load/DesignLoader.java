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
import java.util.function.Function;

public final class DesignLoader {
    public static final int MAX_MESSAGE_LENGTH = 300;

    private DesignLoader() {}

    public record LoadResult(Map<ResourceLocation, TrainsetDesign> designs, List<String> errors) {}

    public static LoadResult load(Map<ResourceLocation, JsonElement> jsons) {
        return load(jsons, DesignValidator::validate);
    }

    static LoadResult load(Map<ResourceLocation, JsonElement> jsons, Function<TrainsetDesign, List<String>> validator) {
        Map<ResourceLocation, TrainsetDesign> designs = new TreeMap<>();
        List<String> errors = new ArrayList<>();

        new TreeMap<>(jsons).forEach((id, json) -> {
            try {
                DataResult<TrainsetDesign> decoded = TrainsetDesign.CODEC.parse(JsonOps.INSTANCE, json);
                Optional<TrainsetDesign> design = decoded.result();
                if (design.isEmpty()) {
                    errors.add(id + ": " + shorten(decoded.error().map(DataResult.Error::message).orElse("could not be decoded")));
                    return;
                }
                List<String> problems = validator.apply(design.get());
                if (!problems.isEmpty()) {
                    problems.forEach(problem -> errors.add(id + ": " + problem));
                    return;
                }
                designs.put(id, design.get());
            } catch (RuntimeException e) {
                errors.add(id + ": internal error: " + shorten(String.valueOf(e)));
            }
        });
        return new LoadResult(designs, errors);
    }

    static String shorten(String message) {
        return message.length() <= MAX_MESSAGE_LENGTH ? message : message.substring(0, MAX_MESSAGE_LENGTH) + "…";
    }
}
