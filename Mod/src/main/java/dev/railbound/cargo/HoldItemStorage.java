package dev.railbound.cargo;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.contraption.storage.item.WrapperMountedItemStorage;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.foundation.codec.CreateCodecs;
import dev.railbound.registry.RailboundStorageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * A goods wagon's hold on a train. Create counts it as the train's cargo, so Portable Storage Interfaces load and
 * unload it and schedules and observers see it. Players open it through {@link CargoMovingInteraction}.
 */
public class HoldItemStorage extends WrapperMountedItemStorage<ItemStackHandler> {
    public static final MapCodec<HoldItemStorage> CODEC = CreateCodecs.ITEM_STACK_HANDLER
            .xmap(HoldItemStorage::new, storage -> storage.wrapped).fieldOf("items");

    private HoldItemStorage(ItemStackHandler items) {
        super(RailboundStorageTypes.HOLD_ITEMS.get(), items);
    }

    public static HoldItemStorage of(CargoHoldBlockEntity hold) {
        return new HoldItemStorage(copyToItemStackHandler(hold.items()));
    }

    /** The hold itself, for the hold screen. */
    public ItemStackHandler load() {
        return wrapped;
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (be instanceof CargoHoldBlockEntity hold) {
            hold.restoreFromTrain(wrapped);
        }
    }

    /** Opened by the wagon's own click handler, which also checks the train is stopped. */
    @Override
    public boolean handleInteraction(ServerPlayer player, Contraption contraption, StructureBlockInfo info) {
        return false;
    }
}
