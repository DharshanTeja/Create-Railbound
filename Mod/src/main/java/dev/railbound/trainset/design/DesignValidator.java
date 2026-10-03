package dev.railbound.trainset.design;

import dev.railbound.util.BlockGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class DesignValidator {
    public static final int REQUIRED_BOGEYS = 2;
    public static final int MIN_BOGEY_SPACING = 3;

    private DesignValidator() {}

    public static List<String> validate(TrainsetDesign design) {
        List<String> errors = new ArrayList<>();
        CarriageSize size = design.size();

        if (design.name().isBlank()) {
            errors.add("name must not be empty");
        }
        if (size.width() != 1 && size.width() != 3) {
            errors.add("width must be 1 or 3, got " + size.width());
        }

        List<BogeySpec> bogeys = design.bogeys();
        if (bogeys.size() != REQUIRED_BOGEYS) {
            errors.add("must have exactly 2 bogeys, found " + bogeys.size());
        }
        for (BogeySpec bogey : bogeys) {
            if (bogey.z() < 0 || bogey.z() >= size.length()) {
                errors.add("bogey z=" + bogey.z() + " is outside carriage length " + size.length());
            }
        }
        if (bogeys.size() == REQUIRED_BOGEYS) {
            int spacing = Math.abs(bogeys.get(0).z() - bogeys.get(1).z());
            if (spacing == 0) {
                errors.add("bogeys must be at different positions");
            } else if (spacing < MIN_BOGEY_SPACING) {
                // Create refuses to assemble bogeys closer than this (StationBlockEntity "bogeys_too_close").
                errors.add("bogeys must be at least 3 blocks apart, got " + spacing);
            }
        }

        List<String> layoutErrors = validateLayoutStructure(design);
        errors.addAll(layoutErrors);
        if (layoutErrors.isEmpty()) {
            validateLayoutContents(design, errors);
        }
        return errors;
    }

    private static List<String> validateLayoutStructure(TrainsetDesign design) {
        List<String> errors = new ArrayList<>();
        LayoutSpec layout = design.layout();
        CarriageSize size = design.size();

        for (Map.Entry<String, String> entry : layout.palette().entrySet()) {
            if (entry.getKey().length() != 1) {
                errors.add("palette key '" + entry.getKey() + "' must be a single character");
            } else if (HiddenPart.parse(entry.getValue()).isEmpty()) {
                errors.add("palette '" + entry.getKey() + "': unknown part '" + entry.getValue() + "'");
            }
        }

        if (layout.layers().size() != size.height()) {
            errors.add("layout has " + layout.layers().size() + " layers, expected height " + size.height());
        }
        for (int y = 0; y < layout.layers().size(); y++) {
            List<String> rows = layout.layers().get(y);
            if (rows.size() != size.length()) {
                errors.add("layer " + y + " has " + rows.size() + " rows, expected length " + size.length());
                continue;
            }
            for (int z = 0; z < rows.size(); z++) {
                String row = rows.get(z);
                if (row.length() != size.width()) {
                    errors.add("layer " + y + " row " + z + " has " + row.length() + " cells, expected width " + size.width());
                    continue;
                }
                for (int i = 0; i < row.length(); i++) {
                    String symbol = String.valueOf(row.charAt(i));
                    if (!layout.palette().containsKey(symbol)) {
                        errors.add("layer " + y + " row " + z + ": character '" + symbol + "' is not in the palette");
                    }
                }
            }
        }
        return errors;
    }

    private static void validateLayoutContents(TrainsetDesign design, List<String> errors) {
        List<LayoutCell> cells = LayoutParser.parse(design);

        long anchors = cells.stream().filter(c -> c.part().type() == PartType.ANCHOR).count();
        if (anchors != 1) {
            errors.add("must have exactly 1 anchor, found " + anchors);
        }

        Set<BlockPos> doorCells = cells.stream()
                .filter(c -> c.part().type() == PartType.DOOR)
                .map(LayoutCell::pos)
                .collect(Collectors.toSet());
        for (DoorSpec door : design.doors()) {
            if (!doorCells.contains(door.pos())) {
                errors.add("door '" + door.part() + "' at " + door.pos().toShortString() + " is not a door cell");
            }
        }

        // Layer 0 sits at bogey height: the bogey fills its own cell, and something beside or above must hold it.
        Set<BlockPos> cellPositions = cells.stream().map(LayoutCell::pos).collect(Collectors.toSet());
        for (BogeySpec bogey : design.bogeys()) {
            if (bogey.z() < 0 || bogey.z() >= design.size().length()) {
                continue;
            }
            BlockPos bogeyCell = new BlockPos(0, 0, bogey.z());
            if (cellPositions.contains(bogeyCell)) {
                errors.add("bogey z=" + bogey.z() + " needs layer 0 at x=0 to be air: the bogey goes there");
            } else if (Arrays.stream(Direction.values()).noneMatch(d -> cellPositions.contains(bogeyCell.relative(d)))) {
                errors.add("bogey z=" + bogey.z() + " is not next to any carriage part");
            }
        }

        if (!cells.isEmpty()) {
            Set<BlockPos> reachable = BlockGraph.connected(cells.get(0).pos(), cellPositions::contains, cellPositions.size() + 1);
            int cutOff = cellPositions.size() - reachable.size();
            if (cutOff > 0) {
                errors.add("layout cells are not connected: " + cutOff + " cell(s) are cut off from the rest");
            }
        }
    }
}
