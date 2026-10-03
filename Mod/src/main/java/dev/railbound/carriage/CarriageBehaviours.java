package dev.railbound.carriage;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.actors.seat.SeatInteractionBehaviour;
import com.simibubi.create.content.contraptions.actors.seat.SeatMovementBehaviour;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorMovementBehaviour;
import dev.railbound.registry.RailboundBlocks;

/**
 * Hooks our interior parts into Create's moving-train behaviour, as Create does for its own seats and doors.
 * Doors open at stations but snap rather than slide, since the slide animation needs Create's door block entity.
 */
public final class CarriageBehaviours {
    private CarriageBehaviours() {}

    public static void register() {
        MovingInteractionBehaviour.REGISTRY.register(RailboundBlocks.SEAT.get(), new SeatInteractionBehaviour());
        MovementBehaviour.REGISTRY.register(RailboundBlocks.SEAT.get(), new SeatMovementBehaviour());
        MovementBehaviour.REGISTRY.register(RailboundBlocks.DOOR.get(), new SlidingDoorMovementBehaviour());
        MovingInteractionBehaviour.REGISTRY.register(RailboundBlocks.DOOR.get(), new DoorPartMovingInteraction());
    }
}
