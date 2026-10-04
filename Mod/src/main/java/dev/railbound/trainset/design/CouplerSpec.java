package dev.railbound.trainset.design;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * Railbound knuckle couplers at both carriage ends, at this height (model pixels), and for carriages people walk
 * through, the gangway bellows. Between two carriages with couplers (ours, or Create-built ones with coupler blocks)
 * the knuckles are drawn instead of Create's chain; between two with gangways, the bellows too.
 */
public record CouplerSpec(double height, Optional<GangwaySpec> gangway) {
    public static final Codec<CouplerSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.fieldOf("height").forGetter(CouplerSpec::height),
            GangwaySpec.CODEC.optionalFieldOf("gangway").forGetter(CouplerSpec::gangway)
    ).apply(i, CouplerSpec::new));
}
