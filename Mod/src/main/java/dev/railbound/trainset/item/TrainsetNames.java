package dev.railbound.trainset.item;

import dev.railbound.trainset.design.ParsedDesign;
import dev.railbound.trainset.design.TrainsetDesign;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class TrainsetNames {
    private TrainsetNames() {}

    public static Component displayName(@Nullable ResourceLocation designId,
                                        Function<ResourceLocation, Optional<TrainsetDesign>> lookup) {
        if (designId == null) {
            return Component.translatable("item.railbound.trainset.empty");
        }
        return lookup.apply(designId)
                .<Component>map(design -> Component.translatable(design.name()))
                .orElseGet(() -> Component.translatable("item.railbound.trainset.unknown", designId.toString()));
    }

    public static List<Component> tooltip(ParsedDesign parsed) {
        TrainsetDesign design = parsed.design();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("tooltip.railbound.category." + design.category().getSerializedName())
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.railbound.length", design.size().length())
                .withStyle(ChatFormatting.GRAY));
        if (parsed.seatCount() > 0) {
            lines.add(Component.translatable("tooltip.railbound.seats", parsed.seatCount()).withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }
}
