package dev.railbound.steam;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Breaking any block of the five-block crane brings the rest down, each block dropping its loot: only the base may
 * drop the crane, or one crane comes back as five.
 */
class WaterCraneLootTest {
    @Test
    void onlyTheBaseDropsTheCrane() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/data/railbound/loot_table/blocks/water_crane.json")) {
            assertNotNull(in);
            JsonObject table = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject pool = table.getAsJsonArray("pools").get(0).getAsJsonObject();
            JsonObject onlyTheBase = null;
            for (var condition : pool.getAsJsonArray("conditions")) {
                JsonObject c = condition.getAsJsonObject();
                if (c.get("condition").getAsString().equals("minecraft:block_state_property")) {
                    onlyTheBase = c;
                }
            }
            assertNotNull(onlyTheBase, "the pool is limited to one section");
            assertEquals("railbound:water_crane", onlyTheBase.get("block").getAsString());
            assertEquals("0", onlyTheBase.getAsJsonObject("properties").get("section").getAsString());
        }
    }
}
