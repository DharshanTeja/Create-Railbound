package dev.railbound.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The buffer is a plain model block the Anti-Climber item places: its files have to agree with each other. */
class BufferBlockAssetsTest {
    private static JsonObject json(String path) throws IOException {
        try (InputStream in = BufferBlockAssetsTest.class.getResourceAsStream(path)) {
            assertTrue(in != null, "missing resource " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test
    void theAntiClimberItemPlacesTheBufferBlock() {
        assertTrue(RailboundItems.ANTI_CLIMBER.get() instanceof net.minecraft.world.item.BlockItem,
                "the part you craft is the block you place");
        assertEquals(RailboundBlocks.BUFFER.get(), ((net.minecraft.world.item.BlockItem) RailboundItems.ANTI_CLIMBER.get()).getBlock());
    }

    @Test
    void everyFacingHasAModelTurnedTheRightWay() throws IOException {
        JsonObject variants = json("/assets/railbound/blockstates/buffer.json").getAsJsonObject("variants");
        assertEquals(4, variants.size());
        assertEquals(0, variants.getAsJsonObject("facing=south").has("y") ? variants.getAsJsonObject("facing=south").get("y").getAsInt() : 0);
        assertEquals(90, variants.getAsJsonObject("facing=west").get("y").getAsInt());
        assertEquals(180, variants.getAsJsonObject("facing=north").get("y").getAsInt());
        assertEquals(270, variants.getAsJsonObject("facing=east").get("y").getAsInt());
        for (String key : variants.keySet()) {
            assertEquals("railbound:block/buffer", variants.getAsJsonObject(key).get("model").getAsString());
        }
    }

    @Test
    void theModelUsesARealTextureAndStaysInsideTheBlock() throws IOException {
        JsonObject model = json("/assets/railbound/models/block/buffer.json");
        String texture = model.getAsJsonObject("textures").get("buffer").getAsString();
        assertEquals("railbound:block/buffer", texture);
        assertTrue(getClass().getResource("/assets/railbound/textures/block/buffer.png") != null, "texture file");
        JsonArray elements = model.getAsJsonArray("elements");
        assertTrue(elements.size() >= 10, "a detailed model, not a box");
        for (JsonElement e : elements) {
            JsonObject element = e.getAsJsonObject();
            for (String corner : new String[] {"from", "to"}) {
                for (JsonElement c : element.getAsJsonArray(corner)) {
                    assertTrue(c.getAsDouble() >= -0.001 && c.getAsDouble() <= 16.001, "inside the 16 px cell: " + c);
                }
            }
            for (String face : element.getAsJsonObject("faces").keySet()) {
                assertEquals("#buffer", element.getAsJsonObject("faces").getAsJsonObject(face).get("texture").getAsString());
            }
        }
    }

    @Test
    void breakingItDropsTheAntiClimber() throws IOException {
        JsonObject loot = json("/data/railbound/loot_table/blocks/buffer.json");
        String dropped = loot.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject()
                .get("name").getAsString();
        assertEquals("railbound:anti_climber", dropped);
    }

    @Test
    void aPickaxeBreaksIt() throws IOException {
        JsonArray values = json("/data/minecraft/tags/block/mineable/pickaxe.json").getAsJsonArray("values");
        assertTrue(values.toString().contains("railbound:buffer"), values.toString());
    }

    @Test
    void itHasAName() throws IOException {
        JsonObject lang = json("/assets/railbound/lang/en_us.json");
        assertTrue(lang.has("block.railbound.buffer"), "the placed item takes its block's name");
    }
}
