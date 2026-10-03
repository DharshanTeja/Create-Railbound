package dev.railbound.client;

import dev.railbound.Railbound;
import dev.railbound.trainset.item.TrainsetItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * The trainset item keeps its flat icon (item/trainset) and switches, per stack, to the 3D model
 * (item/trainset_3d, drawn by {@link TrainsetItemRenderer}) when the stack's design has a model.
 */
public final class TrainsetItemModels {
    public static final ModelResourceLocation THREE_D = ModelResourceLocation.standalone(Railbound.rl("item/trainset_3d"));
    private static final ModelResourceLocation ITEM = ModelResourceLocation.inventory(Railbound.rl("trainset"));

    private TrainsetItemModels() {}

    public static void registerAdditional(ModelEvent.RegisterAdditional event) {
        event.register(THREE_D);
    }

    public static void wrap(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        BakedModel flat = models.get(ITEM);
        BakedModel threeD = models.get(THREE_D);
        if (flat == null || threeD == null) {
            return;
        }
        ItemOverrides overrides = new ItemOverrides() {
            @Override
            public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level,
                                      @Nullable LivingEntity entity, int seed) {
                return CarriageDrawer.hasModel(TrainsetItem.designId(stack)) ? threeD : flat;
            }
        };
        models.put(ITEM, new BakedModelWrapper<>(flat) {
            @Override
            public ItemOverrides getOverrides() {
                return overrides;
            }
        });
    }
}
