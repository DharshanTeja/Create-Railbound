package dev.railbound.carriage;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.world.item.ItemStack;

/**
 * Create's Train Controls behaviour on a moving train, less its lever desk: the loco's model draws the control stand.
 * The driver lets go when the train stops being one.
 */
public class CabControlsMovementBehaviour implements MovementBehaviour {
    @Override
    public ItemStack canBeDisabledVia(MovementContext context) {
        return null;
    }

    @Override
    public void stopMoving(MovementContext context) {
        context.contraption.entity.stopControlling(context.localPos);
        MovementBehaviour.super.stopMoving(context);
    }
}
