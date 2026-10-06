package dev.railbound.client;

import com.simibubi.create.AllItems;
import dev.railbound.network.UncouplePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;

/**
 * Right-clicking a joined knuckle coupler with Create's wrench asks the server to uncouple the train there. Couplers are
 * drawn, not blocks, so the click is aimed at them here: the nearest one along the player's look within reach, unless
 * a block is in the way.
 */
public final class CouplerWrench {
    private CouplerWrench() {}

    public static void onInteract(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND || player == null || mc.level == null
                || !AllItems.WRENCH.isIn(player.getMainHandItem())) {
            return;
        }
        float partialTick = mc.getTimer().getGameTimeDeltaPartialTick(true);
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 reach = eye.add(player.getViewVector(partialTick).scale(player.blockInteractionRange()));
        Optional<CouplingRenderer.Pick> pick = CouplingRenderer.pick(mc.level, eye, reach, partialTick);
        if (pick.isEmpty()) {
            return;
        }
        HitResult hit = mc.hitResult;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK && hit.getLocation().distanceTo(eye) < pick.get().distance()) {
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(true);
        PacketDistributor.sendToServer(new UncouplePayload(pick.get().train(), pick.get().gap()));
    }
}
