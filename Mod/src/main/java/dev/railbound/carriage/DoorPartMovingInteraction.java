package dev.railbound.carriage;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.SimpleBlockMovingInteraction;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

/** Right-clicking a door on an assembled train opens or closes both halves, like Create's own train doors. */
public class DoorPartMovingInteraction extends SimpleBlockMovingInteraction {

    @Override
    protected BlockState handle(Player player, Contraption contraption, BlockPos pos, BlockState currentState) {
        if (!(currentState.getBlock() instanceof DoorPartBlock)) {
            return currentState;
        }
        BlockPos otherPos = currentState.getValue(DoorPartBlock.HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        StructureBlockInfo other = contraption.getBlocks().get(otherPos);
        if (other != null && other.state().getBlock() instanceof DoorPartBlock) {
            setContraptionBlockData(contraption.entity, otherPos,
                    new StructureBlockInfo(other.pos(), other.state().cycle(DoorPartBlock.OPEN), other.nbt()));
        }
        boolean opening = !currentState.getValue(DoorPartBlock.OPEN);
        if (player != null) {
            playSound(player, opening ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE,
                    player.level().random.nextFloat() * 0.1F + 0.9F);
        }
        return currentState.cycle(DoorPartBlock.OPEN);
    }

    @Override
    protected boolean updateColliders() {
        return true;
    }
}
