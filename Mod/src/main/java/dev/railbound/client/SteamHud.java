package dev.railbound.client;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsHandler;
import dev.railbound.steam.BunkerBlockEntity;
import dev.railbound.steam.WaterTankBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import dev.railbound.steam.SteamGauges;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;

import java.util.Optional;

/** The cab gauges, shown while driving or riding a steam loco, or looking at one (its firebox or tank when it stands). */
public final class SteamHud {
    private SteamHud() {}

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        AbstractContraptionEntity entity = ControlsHandler.getContraption();
        if (entity == null && mc.player.getVehicle() instanceof AbstractContraptionEntity riding) {
            entity = riding;
        }
        if (entity == null && mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof AbstractContraptionEntity looked) {
            entity = looked;
        }
        Optional<SteamGauges> gauges = entity != null && entity.getContraption() != null ? gaugesOf(entity) : standingLoco(mc);
        gauges.ifPresent(g -> draw(graphics, mc, g));
    }

    /** The gauges of a standing loco whose firebox or water tank the player is looking at. */
    private static Optional<SteamGauges> standingLoco(Minecraft mc) {
        if (!(mc.hitResult instanceof BlockHitResult hit) || mc.level == null) {
            return Optional.empty();
        }
        BlockPos pos = hit.getBlockPos();
        if (mc.level.getBlockEntity(pos) instanceof BunkerBlockEntity bunker) {
            return Optional.of(SteamGauges.of(bunker));
        }
        if (mc.level.getBlockState(pos).getBlock() instanceof WaterTankBlock
                || mc.level.getBlockState(pos).getBlock() instanceof dev.railbound.steam.FireboxBlock) {
            return WaterTankBlock.bunkerOf(mc.level, pos).map(SteamGauges::of);
        }
        return Optional.empty();
    }

    private static void draw(GuiGraphics graphics, Minecraft mc, SteamGauges gauges) {
        int[] size = SteamGaugeDrawing.size(mc.font, gauges, SteamGaugeDrawing.HUD_BAR);
        int pad = 6, x = 8 + pad, y = graphics.guiHeight() - 40 - size[1] - pad;
        graphics.fill(x - pad, y - pad, x + size[0] + pad, y + size[1] + pad - 2, 0xB0101214);
        graphics.fill(x - pad, y - pad, x + size[0] + pad, y - pad + 1, 0x40FFFFFF);
        SteamGaugeDrawing.draw(graphics, mc.font, x, y, gauges, SteamGaugeDrawing.HUD_BAR);
    }

    /** Which contraptions are steam locos (they carry a bunker), found once each. */
    private static final java.util.Map<com.simibubi.create.content.contraptions.Contraption, Boolean> LOCOS =
            new java.util.WeakHashMap<>();

    private static Optional<SteamGauges> gaugesOf(AbstractContraptionEntity entity) {
        // only for a carriage that is itself a steam loco, and only what the server sent for it: Create's client copy
        // of train storage is shared between trains, so reading that would show a loco's gauges on other trains
        boolean loco = LOCOS.computeIfAbsent(entity.getContraption(), contraption -> contraption.getBlocks().values().stream()
                .anyMatch(info -> info.state().getBlock() instanceof dev.railbound.steam.BunkerBlock));
        return loco ? dev.railbound.steam.LocoGaugeCache.get(entity.getId()) : Optional.empty();
    }
}
