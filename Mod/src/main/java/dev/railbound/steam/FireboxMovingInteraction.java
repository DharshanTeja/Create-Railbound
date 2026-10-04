package dev.railbound.steam;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

/** The firebox door on a train: coal in hand goes into the loco's bunker, an empty hand opens the bunker screen. */
public class FireboxMovingInteraction extends MovingInteractionBehaviour {

    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        Contraption contraption = contraptionEntity.getContraption();
        if (player instanceof ServerPlayer serverPlayer && contraption != null) {
            LocoAccess.bunkerOn(contraption).ifPresent(entry -> entry.getValue()
                    .handleInteraction(serverPlayer, contraption, contraption.getBlocks().get(entry.getKey())));
        }
        return true;
    }
}
