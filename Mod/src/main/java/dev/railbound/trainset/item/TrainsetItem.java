package dev.railbound.trainset.item;

import dev.railbound.registry.RailboundComponents;
import dev.railbound.registry.RailboundItems;
import dev.railbound.trainset.load.TrainsetDesigns;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TrainsetItem extends Item {

    public TrainsetItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(ResourceLocation designId) {
        ItemStack stack = new ItemStack(RailboundItems.TRAINSET.get());
        stack.set(RailboundComponents.TRAINSET_DESIGN.get(), designId);
        return stack;
    }

    @Nullable
    public static ResourceLocation designId(ItemStack stack) {
        return stack.get(RailboundComponents.TRAINSET_DESIGN.get());
    }

    @Override
    public Component getName(ItemStack stack) {
        return TrainsetNames.displayName(designId(stack), TrainsetDesigns::get);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation id = designId(stack);
        if (id != null) {
            TrainsetDesigns.get(id).ifPresent(design -> tooltip.addAll(TrainsetNames.tooltip(design)));
        }
    }
}
