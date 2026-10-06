package dev.railbound.cargo;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

/** A goods wagon's hold or tank on a train: clicking it (or sneaking with an empty hand on the wagon) opens it. */
public class CargoMovingInteraction extends MovingInteractionBehaviour {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        if (player instanceof ServerPlayer serverPlayer) {
            CargoAccess.openOnTrain(serverPlayer, contraptionEntity, localPos);
        }
        return true;
    }
}
