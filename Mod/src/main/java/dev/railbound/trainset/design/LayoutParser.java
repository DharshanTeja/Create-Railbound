package dev.railbound.trainset.design;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LayoutParser {
    private LayoutParser() {}

    /** Converts the text layout into cells. Throws IllegalStateException if the layout is invalid — validate first. */
    public static List<LayoutCell> parse(TrainsetDesign design) {
        LayoutSpec layout = design.layout();
        int half = design.size().width() / 2;

        Map<String, HiddenPart> palette = new HashMap<>();
        layout.palette().forEach((key, value) -> palette.put(key, HiddenPart.parse(value)
                .orElseThrow(() -> new IllegalStateException("Invalid palette entry '" + key + "' -> '" + value + "'"))));

        List<LayoutCell> cells = new ArrayList<>();
        for (int y = 0; y < layout.layers().size(); y++) {
            List<String> rows = layout.layers().get(y);
            for (int z = 0; z < rows.size(); z++) {
                String row = rows.get(z);
                for (int i = 0; i < row.length(); i++) {
                    String symbol = String.valueOf(row.charAt(i));
                    HiddenPart part = palette.get(symbol);
                    if (part == null) {
                        throw new IllegalStateException("Character '" + symbol + "' is not in the palette");
                    }
                    if (part.type() != PartType.AIR) {
                        cells.add(new LayoutCell(new BlockPos(i - half, y, z), part));
                    }
                }
            }
        }
        return List.copyOf(cells);
    }
}
