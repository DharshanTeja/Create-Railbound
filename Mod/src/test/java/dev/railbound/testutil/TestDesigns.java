package dev.railbound.testutil;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.railbound.trainset.design.BogeySpec;
import dev.railbound.trainset.design.CarriageSize;
import dev.railbound.trainset.design.DoorSpec;
import dev.railbound.trainset.design.LayoutSpec;
import dev.railbound.trainset.design.TrainsetDesign;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

public final class TestDesigns {
    public static final String SAMPLE_PATH = "/data/railbound/railbound/trainsets/coach_standard.json";

    private TestDesigns() {}

    public static JsonElement sampleJson() {
        try (InputStream in = Objects.requireNonNull(TestDesigns.class.getResourceAsStream(SAMPLE_PATH), SAMPLE_PATH)) {
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static TrainsetDesign sample() {
        return TrainsetDesign.CODEC.parse(JsonOps.INSTANCE, sampleJson()).getOrThrow();
    }

    public static TrainsetDesign parse(String json) {
        return TrainsetDesign.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    public static TrainsetDesign withBogeys(TrainsetDesign d, List<BogeySpec> bogeys) {
        return new TrainsetDesign(d.name(), d.category(), d.size(), bogeys, d.power(), d.cargo(), d.doors(), d.layout(), d.performance(), d.steam(), d.drive());
    }

    public static TrainsetDesign withSize(TrainsetDesign d, CarriageSize size) {
        return new TrainsetDesign(d.name(), d.category(), size, d.bogeys(), d.power(), d.cargo(), d.doors(), d.layout(), d.performance(), d.steam(), d.drive());
    }

    public static TrainsetDesign withLayout(TrainsetDesign d, LayoutSpec layout) {
        return new TrainsetDesign(d.name(), d.category(), d.size(), d.bogeys(), d.power(), d.cargo(), d.doors(), layout, d.performance(), d.steam(), d.drive());
    }

    public static TrainsetDesign withDoors(TrainsetDesign d, List<DoorSpec> doors) {
        return new TrainsetDesign(d.name(), d.category(), d.size(), d.bogeys(), d.power(), d.cargo(), doors, d.layout(), d.performance(), d.steam(), d.drive());
    }

    public static TrainsetDesign withName(TrainsetDesign d, String name) {
        return new TrainsetDesign(name, d.category(), d.size(), d.bogeys(), d.power(), d.cargo(), d.doors(), d.layout(), d.performance(), d.steam(), d.drive());
    }
}
