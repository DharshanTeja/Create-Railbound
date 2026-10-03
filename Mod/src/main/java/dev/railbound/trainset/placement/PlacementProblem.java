package dev.railbound.trainset.placement;

public enum PlacementProblem {
    DOES_NOT_FIT("does_not_fit"),
    TOO_CLOSE("too_close"),
    TOUCHING("touching");

    private final String key;

    PlacementProblem(String key) {
        this.key = key;
    }

    /** Suffix of the translation key railbound.placement.<key>. */
    public String key() {
        return key;
    }
}
