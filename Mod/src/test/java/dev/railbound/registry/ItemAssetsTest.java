package dev.railbound.registry;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every plain item we register needs a model, a texture and a name, or it shows as a magenta cube with a raw key. */
class ItemAssetsTest {
    /** Drawn by their own renderer, or sharing their block's model. */
    private static final Set<String> SKIPPED = Set.of("trainset", "water_crane", "coupler");

    private static List<String> plainItems() {
        return RailboundItems.ITEMS.getEntries().stream()
                .map(entry -> entry.getId().getPath())
                .filter(id -> !SKIPPED.contains(id))
                .toList();
    }

    private static JsonObject json(String path) throws IOException {
        try (InputStream in = ItemAssetsTest.class.getResourceAsStream(path)) {
            assertTrue(in != null, "missing resource " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test
    void thereAreCraftingPartItems() {
        assertTrue(plainItems().containsAll(List.of(
                "steam_engine_unit", "coach_bogey_unit", "freight_bogey_unit", "steam_truck_unit",
                "anti_climber", "carriage_shell", "wagon_underframe")));
    }

    @Test
    void onlyPartsHaveUnfinishedStandIns() {
        // trainsets are laid out in mechanical crafters, so they never exist half-built
        Set<String> parts = Set.of("steam_engine_unit", "coach_bogey_unit", "freight_bogey_unit", "steam_truck_unit",
                "anti_climber", "carriage_shell", "wagon_underframe");
        assertEquals(parts, Set.copyOf(RailboundItems.UNFINISHED));
    }

    @Test
    void everyPlainItemHasAModel() {
        List<String> missing = plainItems().stream()
                .filter(id -> getClass().getResource("/assets/railbound/models/item/" + id + ".json") == null)
                .toList();
        assertEquals(List.of(), missing);
    }

    @Test
    void everyModelPointsAtARealTexture() throws IOException {
        for (String id : plainItems()) {
            String texture = json("/assets/railbound/models/item/" + id + ".json")
                    .getAsJsonObject("textures").get("layer0").getAsString();
            assertTrue(texture.startsWith("railbound:item/"), id + " texture " + texture);
            String file = "/assets/railbound/textures/" + texture.substring("railbound:".length()) + ".png";
            assertTrue(getClass().getResource(file) != null, id + " is missing " + file);
        }
    }

    @Test
    void everyPlainItemHasAName() throws IOException {
        JsonObject lang = json("/assets/railbound/lang/en_us.json");
        List<String> missing = plainItems().stream()
                .filter(id -> !lang.has("item.railbound." + id))
                .toList();
        assertEquals(List.of(), missing);
    }
}
