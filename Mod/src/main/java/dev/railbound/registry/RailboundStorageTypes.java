package dev.railbound.registry;

import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorageType;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.api.registry.CreateRegistries;
import dev.railbound.Railbound;
import dev.railbound.steam.BunkerBlockEntity;
import dev.railbound.steam.BunkerFluidStorage;
import dev.railbound.steam.BunkerItemStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

/** How a steam loco's bunker travels on a train: its coal and boiler as item storage, its water as fluid storage. */
public final class RailboundStorageTypes {
    public static final DeferredRegister<MountedItemStorageType<?>> ITEM_TYPES =
            DeferredRegister.create(CreateRegistries.MOUNTED_ITEM_STORAGE_TYPE, Railbound.MOD_ID);
    public static final DeferredRegister<MountedFluidStorageType<?>> FLUID_TYPES =
            DeferredRegister.create(CreateRegistries.MOUNTED_FLUID_STORAGE_TYPE, Railbound.MOD_ID);

    public static final DeferredHolder<MountedItemStorageType<?>, MountedItemStorageType<BunkerItemStorage>> BUNKER_ITEMS =
            ITEM_TYPES.register("bunker", () -> new MountedItemStorageType<>(BunkerItemStorage.CODEC) {
                @Override
                public @Nullable BunkerItemStorage mount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
                    return be instanceof BunkerBlockEntity bunker ? BunkerItemStorage.of(bunker) : null;
                }
            });
    public static final DeferredHolder<MountedFluidStorageType<?>, MountedFluidStorageType<BunkerFluidStorage>> BUNKER_WATER =
            FLUID_TYPES.register("bunker", () -> new MountedFluidStorageType<>(BunkerFluidStorage.CODEC) {
                @Override
                public @Nullable BunkerFluidStorage mount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
                    return be instanceof BunkerBlockEntity bunker ? BunkerFluidStorage.of(bunker) : null;
                }
            });

    private RailboundStorageTypes() {}

    /** Tells Create which block these storages belong to; call once the blocks are registered. */
    public static void linkToBlocks() {
        MountedItemStorageType.REGISTRY.register(RailboundBlocks.BUNKER.get(), BUNKER_ITEMS.get());
        MountedFluidStorageType.REGISTRY.register(RailboundBlocks.BUNKER.get(), BUNKER_WATER.get());
    }
}
