package dev.railbound.trainset.item;

import com.simibubi.create.content.trains.track.ITrackBlock;
import dev.railbound.registry.RailboundComponents;
import dev.railbound.registry.RailboundItems;
import dev.railbound.trainset.design.ParsedDesign;
import dev.railbound.trainset.load.TrainsetDesigns;
import dev.railbound.trainset.placement.TrainsetPlacement;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
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
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (context.getLevel().isClientSide) {
            // Claim track clicks so the client does not also use the off-hand item; the server decides.
            return context.getLevel().getBlockState(context.getClickedPos()).getBlock() instanceof ITrackBlock
                    ? InteractionResult.SUCCESS
                    : InteractionResult.PASS;
        }
        return TrainsetPlacement.tryPlace(stack, context);
    }

    @Override
    public Component getName(ItemStack stack) {
        return TrainsetNames.displayName(designId(stack), id -> TrainsetDesigns.get(id).map(ParsedDesign::design));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        TrainsetDesigns.get(designId(stack)).ifPresent(parsed -> tooltip.addAll(TrainsetNames.tooltip(parsed)));
    }
}
