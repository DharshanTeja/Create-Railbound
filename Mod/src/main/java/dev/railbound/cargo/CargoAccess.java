package dev.railbound.cargo;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import dev.railbound.carriage.CarriageFootprint;
import dev.railbound.carriage.CarriagePart;
import dev.railbound.carriage.CarriageRemover;
import dev.railbound.registry.RailboundBlocks;
import dev.railbound.trainset.design.LayoutCell;
import dev.railbound.trainset.design.PartType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * How players and machines reach a goods wagon's load. Sneak with an empty hand on any part of the wagon to open it:
 * parked, or on a stopped train. Parked, hoppers, funnels and pipes against any part of the wagon put in and take out.
 * A loaded wagon cannot be picked up or broken until it is emptied.
 */
public final class CargoAccess {
    private CargoAccess() {}

    // --- finding the hold ---

    /** The hold or tank block of the parked wagon that the part at pos belongs to. */
    public static Optional<BlockEntity> parkedHold(Level level, BlockPos partPos) {
        return CarriageRemover.findCarriage(level, partPos).flatMap(found -> {
            for (LayoutCell cell : found.design().cells()) {
                PartType type = cell.part().type();
                if (type == PartType.CARGO_ITEM || type == PartType.CARGO_FLUID) {
                    BlockPos pos = CarriageFootprint.worldPos(found.design(), found.anchor(), found.facing(), cell.pos());
                    return Optional.ofNullable(level.getBlockEntity(pos));
                }
            }
            return Optional.empty();
        });
    }

    /** Where a train carriage's hold or tank block is, if it has one. */
    public static Optional<BlockPos> holdOnTrain(Contraption contraption) {
        for (Map.Entry<BlockPos, StructureBlockInfo> block : contraption.getBlocks().entrySet()) {
            Block type = block.getValue().state().getBlock();
            if (type instanceof CargoHoldBlock || type instanceof CargoTankBlock) {
                return Optional.of(block.getKey());
            }
        }
        return Optional.empty();
    }

    public static boolean loaded(BlockEntity hold) {
        return hold instanceof CargoHoldBlockEntity items && !items.isEmpty()
                || hold instanceof CargoTankBlockEntity tank && !tank.isEmpty();
    }

    // --- opening ---

    private static void openParked(ServerPlayer player, BlockEntity hold, Predicate<Player> stillValid, Component title) {
        if (hold instanceof CargoHoldBlockEntity parked) {
            openHold(player, parked.items(), stillValid, title);
        } else if (hold instanceof CargoTankBlockEntity parked) {
            openTank(player, parked.tank(), stillValid, title);
        }
    }

    private static void openHold(ServerPlayer player, IItemHandlerModifiable items,
                                 Predicate<Player> stillValid, Component title) {
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new HoldMenu(id, inventory, items, stillValid), title),
                buf -> buf.writeVarInt(items.getSlots()));
    }

    private static void openTank(ServerPlayer player, IFluidHandler tank, Predicate<Player> stillValid, Component title) {
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new TankMenu(id, inventory, tank, stillValid), title));
    }

    /** A parked wagon: sneak with an empty hand on any of its parts. */
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (event.getHand() != InteractionHand.MAIN_HAND || !player.isShiftKeyDown() || !player.getMainHandItem().isEmpty()
                || !CarriagePart.is(event.getLevel().getBlockState(event.getPos()))) {
            return;
        }
        Level level = event.getLevel();
        if (level.isClientSide) {
            return;
        }
        parkedHold(level, event.getPos()).ifPresent(hold -> {
            Vec3 centre = Vec3.atCenterOf(hold.getBlockPos());
            Component title = hold.getBlockState().getBlock().getName();
            openParked((ServerPlayer) player, hold,
                    p -> !hold.isRemoved() && p.distanceToSqr(centre) < CargoRules.REACH * CargoRules.REACH, title);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        });
    }

    /** A wagon on a train, opened through its hold or tank block once the train has stopped. */
    public static void openOnTrain(ServerPlayer player, AbstractContraptionEntity entity, BlockPos holdPos) {
        Contraption contraption = entity.getContraption();
        if (contraption == null) {
            return;
        }
        if (entity instanceof CarriageContraptionEntity carriage && carriage.getCarriage() != null
                && !CargoRules.canOpenOnTrain(carriage.getCarriage().train.speed)) {
            player.displayClientMessage(Component.translatable("railbound.cargo.stop_first"), true);
            return;
        }
        Vec3 local = Vec3.atCenterOf(holdPos);
        Predicate<Player> stillValid = p -> CargoRules.stillOpenOnTrain(entity.isAlive(), trainSpeed(entity),
                p.distanceToSqr(entity.toGlobalVector(local, 0)));
        StructureBlockInfo info = contraption.getBlocks().get(holdPos);
        Component title = info == null ? Component.empty() : info.state().getBlock().getName();
        if (contraption.getStorage().getAllItemStorages().get(holdPos) instanceof HoldItemStorage items) {
            openHold(player, items.load(), stillValid, title);
        } else if (contraption.getStorage().getFluids().storages.get(holdPos) instanceof TankFluidStorage tank) {
            openTank(player, tank.load(), stillValid, title);
        }
    }

    /** The speed of the train a carriage is in (0 for a contraption that is not a train carriage). */
    private static double trainSpeed(AbstractContraptionEntity entity) {
        return entity instanceof CarriageContraptionEntity carriage && carriage.getCarriage() != null
                && carriage.getCarriage().train != null ? carriage.getCarriage().train.speed : 0;
    }

    // --- refusing to pick up a loaded wagon ---

    /** Sneak-wrenching or breaking a loaded wagon (or a bogey under it) is refused until it is emptied. */
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide) {
            return;
        }
        Optional<BlockPos> part = partAt(level, event.getPos());
        if (part.isEmpty() || parkedHold(level, part.get()).filter(CargoAccess::loaded).isEmpty()) {
            return;
        }
        event.setCanceled(true);
        if (event.getPlayer() != null) {
            event.getPlayer().displayClientMessage(Component.translatable("railbound.cargo.not_empty")
                    .withStyle(ChatFormatting.RED), true);
        }
    }

    private static Optional<BlockPos> partAt(Level level, BlockPos pos) {
        if (CarriagePart.is(level.getBlockState(pos))) {
            return Optional.of(pos);
        }
        if (level.getBlockState(pos).getBlock() instanceof AbstractBogeyBlock<?>) {
            for (Direction direction : Direction.values()) {
                if (CarriagePart.is(level.getBlockState(pos.relative(direction)))) {
                    return Optional.of(pos.relative(direction));
                }
            }
        }
        return Optional.empty();
    }

    // --- hoppers, funnels and pipes on a parked wagon ---

    /** Any part of a parked goods wagon reaches its load, in and out. */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        Block[] parts = {RailboundBlocks.FRAME.get(), RailboundBlocks.PLACEHOLDER.get(), RailboundBlocks.CARGO_HOLD.get(),
                RailboundBlocks.CARGO_TANK.get()};
        event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, be, side) -> items(level, pos, be), parts);
        event.registerBlock(Capabilities.FluidHandler.BLOCK, (level, pos, state, be, side) -> fluid(level, pos, be), parts);
    }

    @Nullable
    private static IItemHandler items(Level level, BlockPos pos, @Nullable BlockEntity be) {
        BlockEntity hold = be instanceof CargoHoldBlockEntity ? be : parkedHold(level, pos).orElse(null);
        return hold instanceof CargoHoldBlockEntity items ? items.items() : null;
    }

    @Nullable
    private static IFluidHandler fluid(Level level, BlockPos pos, @Nullable BlockEntity be) {
        BlockEntity hold = be instanceof CargoTankBlockEntity ? be : parkedHold(level, pos).orElse(null);
        return hold instanceof CargoTankBlockEntity tank ? tank.tank() : null;
    }
}
