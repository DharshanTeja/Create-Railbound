package dev.railbound.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks our recipe files against what we register, so a typo shows up here and not as a missing item in game. */
class RecipeDataTest {
    private static final Path RECIPES = onClasspath("/data/railbound/recipe");
    private static final Path TRAINSETS = onClasspath("/data/railbound/railbound/trainsets");

    /** The folder as a Path, whether the resources are loose files or packed into a jar. */
    private static Path onClasspath(String folder) {
        try {
            URI uri = Objects.requireNonNull(RecipeDataTest.class.getResource(folder), folder).toURI();
            if (uri.getScheme().equals("jar")) {
                try {
                    FileSystems.newFileSystem(uri, Map.of());
                } catch (FileSystemAlreadyExistsException ignored) {
                    // already open for a folder we asked for earlier
                }
            }
            return Paths.get(uri);
        } catch (URISyntaxException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<Path> files(Path dir) throws IOException {
        List<Path> out = new ArrayList<>();
        if (!Files.isDirectory(dir)) return out;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path file : stream) {
                if (file.getFileName().toString().endsWith(".json")) out.add(file);
            }
        }
        return out;
    }

    private static JsonObject read(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static String name(Path file) {
        String n = file.getFileName().toString();
        return n.substring(0, n.length() - ".json".length());
    }

    private static Set<String> ourItems() {
        Set<String> out = new HashSet<>();
        RailboundItems.ITEMS.getEntries().forEach(entry -> out.add("railbound:" + entry.getId().getPath()));
        return out;
    }

    /** Every item id an ingredient names, whatever its shape (item, array of alternatives, tag ignored). */
    private static void collectItems(JsonElement element, List<String> out) {
        if (element.isJsonArray()) {
            for (JsonElement e : element.getAsJsonArray()) collectItems(e, out);
        } else if (element.isJsonObject() && element.getAsJsonObject().has("item")) {
            out.add(element.getAsJsonObject().get("item").getAsString());
        }
    }

    /** The trainset design a recipe makes, or null when it makes something else. */
    private static String designMade(JsonObject recipe) {
        List<JsonElement> stacks = new ArrayList<>();
        if (recipe.has("results")) recipe.getAsJsonArray("results").forEach(stacks::add);
        if (recipe.has("result")) stacks.add(recipe.get("result"));
        for (JsonElement stack : stacks) {
            JsonObject s = stack.getAsJsonObject();
            if (s.get("id").getAsString().equals("railbound:trainset")) {
                return s.getAsJsonObject("components").get("railbound:trainset_design").getAsString();
            }
        }
        return null;
    }

    @Test
    void everyTrainsetDesignCanBeCrafted() throws IOException {
        Set<String> crafted = new HashSet<>();
        for (Path recipe : files(RECIPES)) {
            String design = designMade(read(recipe));
            if (design != null) crafted.add(design);
        }
        Set<String> designs = new HashSet<>();
        for (Path design : files(TRAINSETS)) designs.add("railbound:" + name(design));
        assertFalse(designs.isEmpty());
        assertEquals(designs, crafted);
    }

    /** Parts come off a sequenced assembly; the finished train is laid out part by part in mechanical crafters. */
    @Test
    void everyTrainsetIsMechanicallyCrafted() throws IOException {
        for (Path recipe : files(RECIPES)) {
            JsonObject json = read(recipe);
            if (designMade(json) == null) continue;
            assertEquals("create:mechanical_crafting", json.get("type").getAsString(), name(recipe) + " must be mechanically crafted");
        }
    }

    @Test
    void mechanicalCraftingGridsAreWellFormed() throws IOException {
        Set<String> ours = ourItems();
        int seen = 0;
        for (Path recipe : files(RECIPES)) {
            JsonObject json = read(recipe);
            if (!json.get("type").getAsString().equals("create:mechanical_crafting")) continue;
            seen++;
            String file = name(recipe);
            JsonArray rows = json.getAsJsonArray("pattern");
            int width = rows.get(0).getAsString().length();
            assertTrue(rows.size() <= 9 && width <= 9, file + ": a crafter array is at most 9 x 9");
            Set<Character> used = new HashSet<>();
            for (JsonElement row : rows) {
                assertEquals(width, row.getAsString().length(), file + ": rows must be the same length");
                for (char c : row.getAsString().toCharArray()) if (c != ' ') used.add(c);
            }
            Set<Character> keys = new HashSet<>();
            JsonObject key = json.getAsJsonObject("key");
            for (String k : key.keySet()) keys.add(k.charAt(0));
            assertEquals(keys, used, file + ": every key must be used and every letter keyed");

            List<String> named = new ArrayList<>();
            for (String k : key.keySet()) collectItems(key.get(k), named);
            for (String id : named) {
                if (id.startsWith("railbound:")) assertTrue(ours.contains(id), file + ": unknown item " + id);
            }
            if (designMade(json) != null) {
                assertTrue(named.stream().anyMatch(id -> id.startsWith("railbound:")), file + ": a train is built from our parts");
            }
        }
        assertTrue(seen >= 4, "expected the four trainsets to be mechanical crafts");
    }

    @Test
    void everyPartItemHasARecipeMakingIt() throws IOException {
        Set<String> parts = Set.of("steam_engine_unit", "coach_bogey_unit", "freight_bogey_unit", "steam_truck_unit",
                "anti_climber", "carriage_shell", "wagon_underframe");
        Set<String> made = new HashSet<>();
        for (Path recipe : files(RECIPES)) {
            JsonObject json = read(recipe);
            JsonElement results = json.get("results");
            if (results != null) {
                for (JsonElement r : results.getAsJsonArray()) made.add(r.getAsJsonObject().get("id").getAsString());
            } else if (json.has("result")) {
                made.add(json.getAsJsonObject("result").get("id").getAsString());
            }
        }
        for (String part : parts) assertTrue(made.contains("railbound:" + part), part + " has no recipe");
    }

    @Test
    void sequencedAssembliesAreWellFormed() throws IOException {
        Set<String> ours = ourItems();
        for (Path recipe : files(RECIPES)) {
            JsonObject json = read(recipe);
            if (!json.get("type").getAsString().equals("create:sequenced_assembly")) continue;
            String file = name(recipe);

            String transitional = json.getAsJsonObject("transitional_item").get("id").getAsString();
            assertTrue(ours.contains(transitional), file + ": transitional " + transitional + " is not registered");

            JsonArray sequence = json.getAsJsonArray("sequence");
            assertTrue(sequence.size() >= 2, file + ": a sequence needs at least two steps");
            for (JsonElement step : sequence) {
                JsonObject s = step.getAsJsonObject();
                String type = s.get("type").getAsString();
                assertTrue(type.equals("create:deploying") || type.equals("create:pressing"), file + ": step " + type);
                assertEquals(transitional, s.getAsJsonArray("results").get(0).getAsJsonObject().get("id").getAsString(),
                        file + ": every step must keep the transitional item");
                assertEquals(transitional, s.getAsJsonArray("ingredients").get(0).getAsJsonObject().get("item").getAsString(),
                        file + ": every step must start from the transitional item");
            }

            List<String> named = new ArrayList<>();
            collectItems(json.get("ingredient"), named);
            for (JsonElement step : sequence) collectItems(step.getAsJsonObject().get("ingredients"), named);
            for (String id : named) {
                if (id.startsWith("railbound:")) assertTrue(ours.contains(id), file + ": unknown item " + id);
            }
        }
    }

    @Test
    void waterCraneHasACraftingRecipe() throws IOException {
        JsonObject json = read(RECIPES.resolve("water_crane.json"));
        assertEquals("railbound:water_crane", json.getAsJsonObject("result").get("id").getAsString());
    }

    @Test
    void couplerHasACraftingRecipe() throws IOException {
        JsonObject json = read(RECIPES.resolve("coupler.json"));
        assertEquals("railbound:coupler", json.getAsJsonObject("result").get("id").getAsString());
    }
}
