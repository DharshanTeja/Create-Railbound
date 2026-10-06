package dev.railbound.client;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsHandler;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsInputPacket;
import com.simibubi.create.content.contraptions.sync.ContraptionInteractionPacket;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.foundation.utility.ControlsUtil;
import dev.railbound.carriage.CabControlsBlock;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.List;
import java.util.Optional;

/**
 * Driving a loco from its cab: sitting down in a driver's seat takes the cab controls that seat faces, as clicking them
 * would, and standing up lets go. Create holds Shift while you drive, so Shift first lets go of the controls and then
 * stands you up as usual. Runs before Create's own controls handler each tick.
 */
public final class CabDriving {
    /** The carriage entity whose seat we last handled, and the controls that seat took (null: none). */
    private static int seatedOn = -1;
    private static BlockPos taken;

    private CabDriving() {}

    public static void tick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            seatedOn = -1;
            taken = null;
            return;
        }
        Entity vehicle = player.getVehicle();
        AbstractContraptionEntity driving = ControlsHandler.getContraption();
        if (seatedOn != -1 && (vehicle == null || vehicle.getId() != seatedOn)) {
            // stood up or got off: let go of the controls the seat took
            if (taken != null && driving != null && driving.getId() == seatedOn) {
                release(driving);
            }
            seatedOn = -1;
            taken = null;
        }
        if (!(vehicle instanceof CarriageContraptionEntity carriage) || carriage.getContraption() == null) {
            return;
        }
        if (taken != null && driving == carriage && ControlsUtil.isActuallyPressed(mc.options.keyShift)) {
            release(carriage);
            taken = null;
            return;
        }
        if (seatedOn == carriage.getId()) {
            return;
        }
        seatedOn = carriage.getId();
        Contraption contraption = carriage.getContraption();
        Integer seatIndex = contraption.getSeatMapping().get(player.getUUID());
        if (seatIndex == null || seatIndex < 0 || seatIndex >= contraption.getSeats().size() || driving != null) {
            return;
        }
        BlockPos seat = contraption.getSeats().get(seatIndex);
        Optional<BlockPos> controls = dev.railbound.carriage.CabSeating.controlsFor(seat,
                pos -> cabControlsFacing(contraption, pos), pos -> !contraption.getBlocks().containsKey(pos));
        if (controls.isPresent() && carriage.handlePlayerInteraction(player, controls.get(), Direction.UP, InteractionHand.MAIN_HAND)) {
            CatnipServices.NETWORK.sendToServer(new ContraptionInteractionPacket(carriage, InteractionHand.MAIN_HAND, controls.get(), Direction.UP));
            taken = controls.get();
        }
    }

    private static Optional<Direction> cabControlsFacing(Contraption contraption, BlockPos pos) {
        StructureBlockInfo info = contraption.getBlocks().get(pos);
        return info != null && info.state().getBlock() instanceof CabControlsBlock
                ? Optional.of(info.state().getValue(ControlsBlock.FACING)) : Optional.empty();
    }

    /** Lets go of the controls as Create does when Esc is pressed. */
    private static void release(AbstractContraptionEntity entity) {
        BlockPos pos = ControlsHandler.getControlsPos();
        ControlsHandler.stopControlling();
        if (pos != null) {
            CatnipServices.NETWORK.sendToServer(new ControlsInputPacket(List.of(), false, entity.getId(), pos, true));
        }
    }
}
