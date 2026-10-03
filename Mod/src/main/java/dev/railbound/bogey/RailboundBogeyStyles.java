package dev.railbound.bogey;

import com.simibubi.create.content.trains.bogey.BogeySizes;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import dev.railbound.Railbound;
import dev.railbound.client.TrainsetBogeyModels;
import dev.railbound.client.TrainsetBogeyRenderer;
import dev.railbound.client.TrainsetBogeyVisual;
import dev.railbound.registry.RailboundBlocks;
import net.minecraft.network.chat.Component;

/**
 * Railbound's own bogey styles. Their blocks are Create bogeys (they run on Create track and assemble like any
 * Create bogey) with Railbound looks and heights. Designs pick a style by id in their {@code bogeys} list.
 */
public final class RailboundBogeyStyles {
    public static BogeyStyle COACH;

    private RailboundBogeyStyles() {}

    /** Called during mod construction, after Create has registered its blocks and styles. */
    public static void register() {
        COACH = new BogeyStyle.Builder(Railbound.rl("coach"), Railbound.rl("railbound"))
                .displayName(Component.translatable("railbound.bogey.style.coach"))
                .size(BogeySizes.SMALL, RailboundBlocks.COACH_BOGEY, () -> () -> new BogeyStyle.SizeRenderer(
                        new TrainsetBogeyRenderer(TrainsetBogeyModels.COACH),
                        (ctx, partialTick, inContraption) -> new TrainsetBogeyVisual(ctx, TrainsetBogeyModels.COACH)))
                .build();
    }
}
