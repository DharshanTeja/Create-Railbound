package dev.railbound.trainset.design;

import java.util.Optional;

public record HiddenPart(PartType type, Optional<FrameShape> shape) {

    public static Optional<HiddenPart> parse(String text) {
        String[] bits = text.split(":", -1);
        if (bits.length > 2) {
            return Optional.empty();
        }
        Optional<PartType> type = PartType.byId(bits[0]);
        if (type.isEmpty()) {
            return Optional.empty();
        }
        if (type.get() == PartType.FRAME) {
            if (bits.length != 2) {
                return Optional.empty();
            }
            return FrameShape.byId(bits[1]).map(shape -> new HiddenPart(PartType.FRAME, Optional.of(shape)));
        }
        return bits.length == 1 ? Optional.of(new HiddenPart(type.get(), Optional.empty())) : Optional.empty();
    }
}
