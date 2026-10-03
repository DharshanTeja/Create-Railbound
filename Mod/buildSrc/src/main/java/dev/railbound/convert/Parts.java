package dev.railbound.convert;

import java.util.List;
import java.util.Optional;

/**
 * Which exported part a cube belongs to, from its group path: `door_*` groups are sliding door leaves, `lamps`
 * renders full-bright, groups starting with `_` or `ref_` are previews and references (never exported), and
 * everything else is the body.
 */
public final class Parts {
    public static final String BODY = "body";
    public static final String LAMPS = "lamps";

    private Parts() {}

    public static Optional<String> partFor(List<String> groupPath) {
        String part = BODY;
        for (String group : groupPath) {
            if (group.startsWith("_") || group.startsWith("ref_")) {
                return Optional.empty();
            }
            if (group.startsWith("door_") || group.equals(LAMPS)) {
                part = group;
            }
        }
        return Optional.of(part);
    }
}
