package dev.railbound.registry;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Every block we register needs a blockstate, or it shows Minecraft's magenta "missing model" cube in game. */
class BlockAssetsTest {
    @Test
    void everyBlockHasABlockstate() {
        List<String> missing = RailboundBlocks.BLOCKS.getEntries().stream()
                .map(entry -> entry.getId().getPath())
                .filter(id -> getClass().getResource("/assets/railbound/blockstates/" + id + ".json") == null)
                .toList();
        assertEquals(List.of(), missing);
    }
}
