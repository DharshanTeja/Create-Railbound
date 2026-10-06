package dev.railbound.bogey;

import com.simibubi.create.content.trains.bogey.BogeySizes;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import com.simibubi.create.content.trains.bogey.StandardBogeyBlock;
import com.simibubi.create.content.trains.bogey.StandardBogeyBlockEntity;
import dev.railbound.registry.RailboundBlockEntities;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Create's small bogey under a goods wagon: the Railbound diamond-frame freight truck, a full-height bogey block. */
public class FreightBogeyBlock extends StandardBogeyBlock {

    public FreightBogeyBlock(Properties properties) {
        super(properties, BogeySizes.SMALL);
    }

    @Override
    public BogeyStyle getDefaultStyle() {
        return RailboundBogeyStyles.FREIGHT;
    }

    @Override
    public BlockEntityType<? extends StandardBogeyBlockEntity> getBlockEntityType() {
        return RailboundBlockEntities.FREIGHT_BOGEY.get();
    }
}
