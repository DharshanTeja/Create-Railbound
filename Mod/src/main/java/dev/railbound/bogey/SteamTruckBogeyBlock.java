package dev.railbound.bogey;

import com.simibubi.create.content.trains.bogey.BogeySizes;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import com.simibubi.create.content.trains.bogey.StandardBogeyBlock;
import com.simibubi.create.content.trains.bogey.StandardBogeyBlockEntity;
import dev.railbound.registry.RailboundBlockEntities;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Create's small bogey under a steam loco: the loco's carrying truck, a full-height bogey block. */
public class SteamTruckBogeyBlock extends StandardBogeyBlock {

    public SteamTruckBogeyBlock(Properties properties) {
        super(properties, BogeySizes.SMALL);
    }

    @Override
    public BogeyStyle getDefaultStyle() {
        return RailboundBogeyStyles.STEAM_TRUCK;
    }

    @Override
    public BlockEntityType<? extends StandardBogeyBlockEntity> getBlockEntityType() {
        return RailboundBlockEntities.STEAM_TRUCK_BOGEY.get();
    }
}
