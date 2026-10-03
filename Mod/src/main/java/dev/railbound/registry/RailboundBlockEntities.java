package dev.railbound.registry;

import dev.railbound.Railbound;
import dev.railbound.carriage.AnchorBlockEntity;
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

    private RailboundBlockEntities() {}
}
