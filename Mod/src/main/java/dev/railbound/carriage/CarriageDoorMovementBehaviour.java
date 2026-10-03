package dev.railbound.carriage;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorMovementBehaviour;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

/** Create's station door logic, plus the slide animation of our own door block entity. */
public class CarriageDoorMovementBehaviour extends SlidingDoorMovementBehaviour {

    @Override
    public void tick(MovementContext context) {
        super.tick(context);
        if (!context.world.isClientSide()) {
            return;
        }
        StructureBlockInfo info = context.contraption.getBlocks().get(context.localPos);
        if (info == null || !info.state().hasProperty(DoorPartBlock.OPEN)) {
            return;
        }
        if (context.contraption.getBlockEntityClientSide(context.localPos) instanceof DoorPartBlockEntity door
                && door.animate(info.state().getValue(DoorPartBlock.OPEN))) {
            context.world.playLocalSound(context.position.x, context.position.y, context.position.z,
                    SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.25f, 1, false);
        }
    }
}
