package dev.railbound.trainset.design;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/** Integer codecs that reject fractional JSON numbers instead of silently truncating them. */
public final class StrictInt {
    public static final Codec<Integer> CODEC = Codec.DOUBLE.flatXmap(StrictInt::toInt, i -> DataResult.success(i.doubleValue()));

    public static final Codec<Integer> POSITIVE = CODEC.validate(i -> i > 0
            ? DataResult.success(i)
            : DataResult.error(() -> "value must be positive, got " + i));

    public static final Codec<Integer> NON_NEGATIVE = CODEC.validate(i -> i >= 0
            ? DataResult.success(i)
            : DataResult.error(() -> "value must not be negative, got " + i));

    private StrictInt() {}

    private static DataResult<Integer> toInt(Double value) {
        double d = value;
        if (d != Math.rint(d) || d < Integer.MIN_VALUE || d > Integer.MAX_VALUE) {
            return DataResult.error(() -> "expected a whole number, got " + value);
        }
        return DataResult.success((int) d);
    }
}
