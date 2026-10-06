package dev.railbound.bogey;

import com.simibubi.create.content.trains.bogey.BogeyStyle;
import com.simibubi.create.content.trains.bogey.StandardBogeyBlockEntity;
import dev.railbound.registry.RailboundBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Block entity of {@link FreightBogeyBlock}: a Create bogey whose default style is the Railbound freight truck. */
public class FreightBogeyBlockEntity extends StandardBogeyBlockEntity {

    public FreightBogeyBlockEntity(BlockPos pos, BlockState state) {
        super(RailboundBlockEntities.FREIGHT_BOGEY.get(), pos, state);
    }

    @Override
    public BogeyStyle getDefaultStyle() {
        return RailboundBogeyStyles.FREIGHT;
    }
}
