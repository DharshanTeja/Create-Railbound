package dev.railbound.steam;

import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.Contraption;
import dev.railbound.carriage.CarriageFootprint;
import dev.railbound.carriage.CarriageRemover;
import dev.railbound.trainset.design.PartType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Optional;

/**
 * How a steam loco's firebox, tanks and bunker reach each other: on a standing loco through its design's bunker cell,
 * on a train through the carriage's mounted storages.
 */
public final class LocoAccess {
    private LocoAccess() {}

    /** The bunker of the standing loco the part at partPos belongs to. */
    public static Optional<BunkerBlockEntity> bunkerOf(Level level, BlockPos partPos) {
        return CarriageRemover.findCarriage(level, partPos).flatMap(found -> found.design().cells().stream()
                        .filter(cell -> cell.part().type() == PartType.BUNKER)
                        .map(cell -> CarriageFootprint.worldPos(found.design(), found.anchor(), found.facing(), cell.pos()))
                        .findFirst())
                .flatMap(pos -> level.getBlockEntity(pos) instanceof BunkerBlockEntity bunker
                        ? Optional.of(bunker) : Optional.empty());
    }

    /** Puts held coal into a standing loco's bunker, with the shovel sound at soundPos. */
    public static ItemInteractionResult feedCoal(Level level, BlockPos soundPos, BunkerBlockEntity bunker, Player player,
                                                 InteractionHand hand, ItemStack stack) {
        if (!level.isClientSide) {
            ItemStack rest = bunker.coal().feed(stack.copy());
            if (rest.getCount() != stack.getCount()) {
                if (!player.getAbilities().instabuild) {
                    player.setItemInHand(hand, rest);
                }
                level.playSound(null, soundPos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.4f, 0.8f);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Opens a standing loco's bunker screen. */
    public static void openBunker(ServerPlayer player, BunkerBlockEntity bunker, Component title) {
        int slots = bunker.coal().usable();
        BlockPos pos = bunker.getBlockPos();
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new BunkerMenu(id, inventory, bunker.coal(), slots,
                SteamGauges.data(() -> SteamGauges.of(bunker)),
                who -> !bunker.isRemoved() && who.distanceToSqr(pos.getCenter()) < 64), title),
                buf -> buf.writeVarInt(slots));
    }

    /** On a train: the bunker storage and where it sits in the contraption. */
    public static Optional<Map.Entry<BlockPos, BunkerItemStorage>> bunkerOn(Contraption contraption) {
        for (Map.Entry<BlockPos, MountedItemStorage> entry : contraption.getStorage().getAllItemStorages().entrySet()) {
            if (entry.getValue() instanceof BunkerItemStorage bunker) {
                return Optional.of(Map.entry(entry.getKey(), bunker));
            }
        }
        return Optional.empty();
    }

    /** On a train: the loco's water. */
    public static Optional<BunkerFluidStorage> tankOn(Contraption contraption) {
        for (MountedFluidStorage storage : contraption.getStorage().getFluids().storages.values()) {
            if (storage instanceof BunkerFluidStorage tank) {
                return Optional.of(tank);
            }
        }
        return Optional.empty();
    }
}
