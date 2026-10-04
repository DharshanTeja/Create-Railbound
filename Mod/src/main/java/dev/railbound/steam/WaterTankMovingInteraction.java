package dev.railbound.steam;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** A water bucket on a loco's tank filler on a train pours into the loco's tank. */
public class WaterTankMovingInteraction extends MovingInteractionBehaviour {

    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        ItemStack held = player.getItemInHand(activeHand);
        if (!held.is(Items.WATER_BUCKET) || contraptionEntity.getContraption() == null) {
            return false;
        }
        if (!player.level().isClientSide) {
            LocoAccess.tankOn(contraptionEntity.getContraption()).ifPresent(tank -> {
                if (WaterTankBlock.pourBucket(tank)) {
                    if (!player.getAbilities().instabuild) {
                        player.setItemInHand(activeHand, new ItemStack(Items.BUCKET));
                    }
                    player.level().playSound(null, player.blockPosition(), SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1, 1);
                }
            });
        }
        return true;
    }
}
