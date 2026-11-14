package dev.railbound.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.railbound.client.ponder.RailboundPonderScenes;
import dev.railbound.registry.RailboundBlocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ponder scenes are code and a schematic, and the text is checked here since a typo only shows in game. */
class PonderScenesTest {
    private static final Path SOURCE_DIR = sourceDir();
    private static final Pattern TITLE = Pattern.compile("scene\\.title\\(\"([a-z0-9_]+)\",\\s*\"((?:[^\"\\\\]|\\\\.)*)\"\\)");
    private static final Pattern TEXT = Pattern.compile("\\.text\\(\"((?:[^\"\\\\]|\\\\.)*)\"\\)");

    /** The scene sources, found from wherever the test runs (the working folder is not always the module's). */
    private static Path sourceDir() {
        Path relative = Path.of("src/main/java/dev/railbound/client/ponder");
        java.util.List<Path> starts = new java.util.ArrayList<>();
        starts.add(Path.of("").toAbsolutePath());
        try {
            starts.add(Path.of(PonderScenesTest.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
        } catch (Exception ignored) {
            // fall back to the working folder alone
        }
        for (Path start : starts) {
            for (Path dir = start; dir != null; dir = dir.getParent()) {
                if (Files.isDirectory(dir.resolve(relative))) return dir.resolve(relative);
                if (Files.isDirectory(dir.resolve("Mod").resolve(relative))) return dir.resolve("Mod").resolve(relative);
            }
        }
        throw new IllegalStateException("cannot find " + relative + " from " + starts);
    }

    private static JsonObject lang() throws IOException {
        try (InputStream in = PonderScenesTest.class.getResourceAsStream("/assets/railbound/lang/en_us.json")) {
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /** Every scene's header and numbered texts, in the order the scene code shows them (what Ponder's datagen would write). */
    private static Map<String, String> sceneLang() throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        try (var files = Files.list(SOURCE_DIR)) {
            for (Path file : files.filter(f -> f.toString().endsWith("Scenes.java")).toList()) {
                String source = Files.readString(file);
                Matcher title = TITLE.matcher(source);
                while (title.find()) {
                    String id = title.group(1);
                    out.put("railbound.ponder." + id + ".header", unescape(title.group(2)));
                    int end = source.indexOf("scene.title(", title.end());
                    String body = source.substring(title.end(), end < 0 ? source.length() : end);
                    Matcher text = TEXT.matcher(body);
                    int n = 0;
                    while (text.find()) {
                        out.put("railbound.ponder." + id + ".text_" + ++n, unescape(text.group(1)));
                    }
                }
            }
        }
        return out;
    }

    private static String unescape(String s) {
        return s.replace("\\\"", "\"").replace("\\n", "\n");
    }

    @Test
    void everySceneTextIsInTheLangFile() throws IOException {
        JsonObject lang = lang();
        Map<String, String> expected = sceneLang();
        assertFalse(expected.isEmpty(), "no scenes found in " + SOURCE_DIR);
        for (var entry : expected.entrySet()) {
            assertTrue(lang.has(entry.getKey()), "lang is missing " + entry.getKey());
            assertEquals(entry.getValue(), lang.get(entry.getKey()).getAsString(), entry.getKey());
        }
    }

    @Test
    void theLangFileHasNoStalePonderText() throws IOException {
        Set<String> expected = sceneLang().keySet();
        for (String key : lang().keySet()) {
            if (key.startsWith("railbound.ponder.") && !key.contains(".tag.")) {
                assertTrue(expected.contains(key), "lang has " + key + " but no scene shows it");
            }
        }
    }

    @Test
    void everySceneHasAValidSchematic() throws IOException {
        Set<String> ours = new HashSet<>();
        RailboundBlocks.BLOCKS.getEntries().forEach(e -> ours.add(e.getId().toString()));
        assertFalse(RailboundPonderScenes.SCHEMATICS.isEmpty());
        for (String name : RailboundPonderScenes.SCHEMATICS) {
            String path = "/assets/railbound/ponder/" + name + ".nbt";
            try (InputStream in = getClass().getResourceAsStream(path)) {
                assertTrue(in != null, "missing schematic " + path);
                CompoundTag root = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
                assertTrue(root.contains("size") && root.contains("palette") && root.contains("blocks"), name + " is not a structure");
                ListTag palette = root.getList("palette", 10);
                for (int i = 0; i < palette.size(); i++) {
                    String block = palette.getCompound(i).getString("Name");
                    if (block.startsWith("railbound:")) {
                        assertTrue(ours.contains(block), name + " uses unknown block " + block);
                    }
                }
                ListTag blocks = root.getList("blocks", 10);
                assertTrue(blocks.size() > 0, name + " is empty");
                ListTag size = root.getList("size", 3);
                for (int i = 0; i < blocks.size(); i++) {
                    ListTag pos = blocks.getCompound(i).getList("pos", 3);
                    assertTrue(blocks.getCompound(i).getInt("state") < palette.size(), name + ": bad palette index");
                    for (int axis = 0; axis < 3; axis++) {
                        assertTrue(pos.getInt(axis) >= 0 && pos.getInt(axis) < size.getInt(axis),
                                name + ": block outside the size at " + pos);
                    }
                }
            }
        }
    }
}
