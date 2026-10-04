package dev.railbound.registry;

import dev.railbound.bogey.SteamTruckBogeyBlockEntity;
import dev.railbound.steam.BunkerBlockEntity;
import dev.railbound.steam.WaterCraneBlockEntity;
import dev.railbound.Railbound;
import dev.railbound.bogey.CoachBogeyBlockEntity;
import dev.railbound.carriage.AnchorBlockEntity;
import dev.railbound.carriage.DoorPartBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RailboundBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Railbound.MOD_ID);

    @SuppressWarnings("DataFlowIssue") // the data fixer type is unused for mod block entities
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AnchorBlockEntity>> ANCHOR =
            BLOCK_ENTITIES.register("carriage_anchor",
                    () -> BlockEntityType.Builder.of(AnchorBlockEntity::new, RailboundBlocks.ANCHOR.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DoorPartBlockEntity>> DOOR =
            BLOCK_ENTITIES.register("carriage_door",
                    () -> BlockEntityType.Builder.of(DoorPartBlockEntity::new, RailboundBlocks.DOOR.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoachBogeyBlockEntity>> COACH_BOGEY =
            BLOCK_ENTITIES.register("coach_bogey",
                    () -> BlockEntityType.Builder.of(CoachBogeyBlockEntity::new, RailboundBlocks.COACH_BOGEY.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamTruckBogeyBlockEntity>> STEAM_TRUCK_BOGEY =
            BLOCK_ENTITIES.register("steam_truck_bogey",
                    () -> BlockEntityType.Builder.of(SteamTruckBogeyBlockEntity::new, RailboundBlocks.STEAM_TRUCK_BOGEY.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BunkerBlockEntity>> BUNKER =
            BLOCK_ENTITIES.register("loco_bunker",
                    () -> BlockEntityType.Builder.of(BunkerBlockEntity::new, RailboundBlocks.BUNKER.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WaterCraneBlockEntity>> WATER_CRANE =
            BLOCK_ENTITIES.register("water_crane",
                    () -> BlockEntityType.Builder.of(WaterCraneBlockEntity::new, RailboundBlocks.WATER_CRANE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.railbound.carriage.CouplerBlockEntity>> COUPLER =
            BLOCK_ENTITIES.register("coupler",
                    () -> BlockEntityType.Builder.of(dev.railbound.carriage.CouplerBlockEntity::new, RailboundBlocks.COUPLER.get()).build(null));

    private RailboundBlockEntities() {}
}
