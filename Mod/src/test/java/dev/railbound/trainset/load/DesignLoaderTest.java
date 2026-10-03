package dev.railbound.trainset.load;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import dev.railbound.testutil.TestDesigns;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DesignLoaderTest {

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("railbound", path);
    }

    @Test
    void loadsValidDesign() {
        DesignLoader.LoadResult result = DesignLoader.load(Map.of(id("coach_standard"), TestDesigns.sampleJson()));
        assertEquals(Set.of(id("coach_standard")), result.designs().keySet());
        assertEquals(List.of(), result.errors());
    }

    @Test
    void skipsUndecodableDesignAndKeepsOthers() {
        Map<ResourceLocation, JsonElement> input = new HashMap<>();
        input.put(id("coach_standard"), TestDesigns.sampleJson());
        input.put(id("bad_category"), JsonParser.parseString("""
                {"name":"n","category":"spaceship","size":{"length":2,"width":1,"height":1},
                 "bogeys":[{"z":0},{"z":1}],"layout":{"palette":{},"layers":[]}}"""));
        DesignLoader.LoadResult result = DesignLoader.load(input);
        assertEquals(Set.of(id("coach_standard")), result.designs().keySet());
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().get(0).startsWith("railbound:bad_category: "));
    }

    @Test
    void skipsInvalidDesignWithAllProblems() {
        JsonElement oneBogey = JsonParser.parseString("""
                {"name":"n","category":"box_car","size":{"length":2,"width":1,"height":1},
                 "bogeys":[{"z":0}],
                 "layout":{"palette":{"#":"frame:floor"},"layers":[["#","#"]]}}""");
        DesignLoader.LoadResult result = DesignLoader.load(Map.of(id("broken"), oneBogey));
        assertTrue(result.designs().isEmpty());
        assertTrue(result.errors().contains("railbound:broken: must have exactly 2 bogeys, found 1"));
        assertTrue(result.errors().contains("railbound:broken: must have exactly 1 anchor, found 0"));
    }

    @Test
    void nonObjectJsonIsSkippedNotThrown() {
        DesignLoader.LoadResult result = DesignLoader.load(Map.of(id("weird"), new JsonPrimitive("not a design")));
        assertTrue(result.designs().isEmpty());
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().get(0).startsWith("railbound:weird: "));
    }

    @Test
    void errorsAreInIdOrder() {
        Map<ResourceLocation, JsonElement> input = new HashMap<>();
        input.put(id("zeta"), new JsonPrimitive(1));
        input.put(id("alpha"), new JsonPrimitive(1));
        List<String> errors = DesignLoader.load(input).errors();
        assertTrue(errors.get(0).startsWith("railbound:alpha"));
        assertTrue(errors.get(1).startsWith("railbound:zeta"));
    }
}
