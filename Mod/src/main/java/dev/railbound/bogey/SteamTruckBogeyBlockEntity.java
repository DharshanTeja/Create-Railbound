package dev.railbound.bogey;

import com.simibubi.create.content.trains.bogey.BogeyStyle;
import com.simibubi.create.content.trains.bogey.StandardBogeyBlockEntity;
import dev.railbound.registry.RailboundBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Block entity of {@link SteamTruckBogeyBlock}: a Create bogey whose default style is the Railbound steam truck. */
public class SteamTruckBogeyBlockEntity extends StandardBogeyBlockEntity {

    public SteamTruckBogeyBlockEntity(BlockPos pos, BlockState state) {
        super(RailboundBlockEntities.STEAM_TRUCK_BOGEY.get(), pos, state);
    }

    @Override
    public BogeyStyle getDefaultStyle() {
        return RailboundBogeyStyles.STEAM_TRUCK;
    }
}
