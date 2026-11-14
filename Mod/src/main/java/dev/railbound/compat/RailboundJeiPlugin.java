package dev.railbound.compat;

import dev.railbound.Railbound;
import dev.railbound.registry.RailboundComponents;
import dev.railbound.registry.RailboundItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Every trainset is the one item {@code railbound:trainset} told apart by its design. Without this JEI sees them as a
 * single ingredient, lists only the first design and piles every trainset recipe under it.
 */
@JeiPlugin
public class RailboundJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = Railbound.rl("jei");

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(RailboundItems.TRAINSET.get(), new ISubtypeInterpreter<ItemStack>() {
            @Override
            public Object getSubtypeData(ItemStack stack, UidContext context) {
                return stack.get(RailboundComponents.TRAINSET_DESIGN.get());
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                ResourceLocation design = stack.get(RailboundComponents.TRAINSET_DESIGN.get());
                return design == null ? "" : design.toString();
            }
        });
    }
}
