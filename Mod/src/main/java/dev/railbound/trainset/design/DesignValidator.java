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

    private static void validateHold(List<LayoutCell> cells, PartType type, int capacity, String field, List<String> errors) {
        long holds = cells.stream().filter(c -> c.part().type() == type).count();
        if (capacity > 0 && holds != 1) {
            errors.add("\"cargo\" " + field + " needs exactly 1 " + type.id() + " cell, found " + holds);
        } else if (capacity == 0 && holds > 0) {
            errors.add("the layout has a " + type.id() + " cell but no \"cargo\" " + field + " to hold");
        }
    }

    private static void validateLayoutContents(TrainsetDesign design, List<String> errors) {
        List<LayoutCell> cells = LayoutParser.parse(design);

        long anchors = cells.stream().filter(c -> c.part().type() == PartType.ANCHOR).count();
        if (anchors != 1) {
            errors.add("must have exactly 1 anchor, found " + anchors);
        }

        // a goods wagon's whole load sits in one hold cell (items) or one tank cell (fluid)
        validateHold(cells, PartType.CARGO_ITEM, design.cargo().itemSlots(), "item_slots", errors);
        validateHold(cells, PartType.CARGO_FLUID, design.cargo().fluidMb(), "fluid_mb", errors);

        Set<BlockPos> doorCells = cells.stream()
                .filter(c -> c.part().type() == PartType.DOOR)
                .map(LayoutCell::pos)
                .collect(Collectors.toSet());
        int length = design.size().length();
        for (DoorSpec door : design.doors()) {
            if (!doorCells.contains(door.pos())) {
                errors.add("door '" + door.part() + "' at " + door.pos().toShortString() + " is not a door cell");
            }
            // A leaf slides into the wall beside its doorway: along the side wall, or across the end wall.
            boolean endDoor = door.pos().getX() == 0;
            if (endDoor && !door.slide().map(DoorSlideDirection::sideways).orElse(false)) {
                errors.add("end door '" + door.part() + "' must slide left or right (\"slide\": \"left\" or \"right\")");
            } else if (!endDoor && door.slide().map(DoorSlideDirection::sideways).orElse(false)) {
                errors.add("side door '" + door.part() + "' must slide front or rear, not into the carriage");
            }
        }

        // Doors are two cells tall (lower and upper half) and open outwards: through a side, or through an end.
        for (BlockPos door : doorCells) {
            if (doorCells.contains(door.below())) {
                continue;
            }
            int height = 1;
            while (doorCells.contains(door.above(height))) {
                height++;
            }
            if (height != 2) {
                errors.add("door at " + door.toShortString() + " must be exactly two cells tall, found " + height);
            }
            if (door.getX() == 0 && door.getZ() != 0 && door.getZ() != length - 1) {
                errors.add("door at " + door.toShortString() + " must be on a side column, or on the centre column at a carriage end");
            }
        }

        // Steps are the climbable cell you board through, so they only make sense under a door or an open doorway.
        Set<BlockPos> allCells = cells.stream().map(LayoutCell::pos).collect(Collectors.toSet());
        cells.stream().filter(c -> c.part().type() == PartType.STEP).map(LayoutCell::pos)
                .filter(step -> !doorCells.contains(step.above()) && allCells.contains(step.above()))
                .forEach(step -> errors.add("step at " + step.toShortString()
                        + " needs a door or an open doorway (air) directly above it"));

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

        validateLoco(design, cells, errors);
        design.coupler().flatMap(CouplerSpec::gangway).ifPresent(g -> {
            if (g.top() <= g.bottom()) {
                errors.add("gangway top must be above its bottom");
            }
            if (g.halfWidth() <= 0 || g.halfWidth() > 8 * design.size().width()) {
                errors.add("gangway must fit within the carriage width (half_width up to " + 8 * design.size().width() + ")");
            }
        });

        if (!cells.isEmpty()) {
            Set<BlockPos> reachable = BlockGraph.connected(cells.get(0).pos(), cellPositions::contains, cellPositions.size() + 1);
            int cutOff = cellPositions.size() - reachable.size();
            if (cutOff > 0) {
                errors.add("layout cells are not connected: " + cutOff + " cell(s) are cut off from the rest");
            }
        }
    }

    /** Locos: speeds, the driver's controls with their conductor seat, and for steam the bunker and water tanks. */
    private static void validateLoco(TrainsetDesign design, List<LayoutCell> cells, List<String> errors) {
        boolean locomotive = design.category() == TrainsetCategory.LOCOMOTIVE;
        if (locomotive && design.performance().isEmpty()) {
            errors.add("a locomotive needs a \"performance\" section (top_speed, curve_speed, acceleration)");
        }
        design.performance().ifPresent(p -> {
            if (p.topSpeed() <= 0) {
                errors.add("performance top_speed must be above 0");
            }
            if (p.curveSpeed() <= 0) {
                errors.add("performance curve_speed must be above 0");
            } else if (p.curveSpeed() > p.topSpeed()) {
                errors.add("performance curve_speed must not be above top_speed");
            }
            if (p.acceleration() <= 0) {
                errors.add("performance acceleration must be above 0");
            }
        });

        Set<BlockPos> seats = cells.stream().filter(c -> c.part().type() == PartType.SEAT)
                .map(LayoutCell::pos).collect(Collectors.toSet());
        List<BlockPos> controls = cells.stream().filter(c -> c.part().type() == PartType.CONTROLS)
                .map(LayoutCell::pos).toList();
        if (!controls.isEmpty() && !design.category().drivable()) {
            errors.add("only locomotives and multiple units may carry Train Controls");
        }
        if (locomotive && controls.isEmpty()) {
            errors.add("a locomotive needs Train Controls (a \"controls\" cell)");
        }
        // A seated mob only runs a schedule from the seat its controls face: right in front of it, or across one open
        // cell such as a doorway.
        Set<BlockPos> occupied = cells.stream().map(LayoutCell::pos).collect(Collectors.toSet());
        for (BlockPos pos : controls) {
            if (!hasConductorSeat(pos, seats, occupied)) {
                errors.add("controls at " + pos.toShortString() + " needs a seat directly ahead or behind (the conductor seat)");
            }
        }

        if (design.power() == PowerType.STEAM) {
            if (design.steam().isEmpty()) {
                errors.add("a steam locomotive needs a \"steam\" section (water, bunker_slots)");
            }
            long bunkers = cells.stream().filter(c -> c.part().type() == PartType.BUNKER).count();
            if (bunkers != 1) {
                errors.add("a steam locomotive needs exactly 1 coal bunker, found " + bunkers);
            }
            if (cells.stream().noneMatch(c -> c.part().type() == PartType.WATER_TANK)) {
                errors.add("a steam locomotive needs at least 1 water tank");
            }
        }
        design.drive().ifPresent(d -> {
            if (d.axles().isEmpty()) {
                errors.add("drive needs at least one axle");
            } else if (d.mainAxle() < 0 || d.mainAxle() >= d.axles().size()) {
                errors.add("drive main_axle must pick one of the " + d.axles().size() + " axles (0 to "
                        + (d.axles().size() - 1) + "), got " + d.mainAxle());
            }
            if (d.wheelRadius() <= 0) {
                errors.add("drive wheel_radius must be above 0");
            }
            if (d.crankRadius() <= 0) {
                errors.add("drive crank_radius must be above 0");
            } else if (d.rodLength() <= d.crankRadius() + Math.abs(d.crossheadY() - d.axleY())) {
                errors.add("drive rod_length is too short to reach round the crank from the crosshead line");
            }
        });
        if (design.power() != PowerType.STEAM && cells.stream().anyMatch(c -> c.part().type() == PartType.FIREBOX)) {
            errors.add("firebox cells belong on a steam locomotive");
        }
        design.steam().ifPresent(s -> {
            if (s.water() <= 0) {
                errors.add("steam water must be above 0");
            }
            if (s.bunkerSlots() < 1 || s.bunkerSlots() > 9) {
                errors.add("steam bunker_slots must be 1 to 9, got " + s.bunkerSlots());
            }
        });
    }

    /** A seat right ahead of or behind controls, or across one open cell. */
    private static boolean hasConductorSeat(BlockPos controls, Set<BlockPos> seats, Set<BlockPos> occupied) {
        for (net.minecraft.core.Direction way : new net.minecraft.core.Direction[] {net.minecraft.core.Direction.NORTH,
                net.minecraft.core.Direction.SOUTH}) {
            if (seats.contains(controls.relative(way))
                    || !occupied.contains(controls.relative(way)) && seats.contains(controls.relative(way, 2))) {
                return true;
            }
        }
        return false;
    }
}
