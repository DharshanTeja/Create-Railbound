package dev.railbound.bogey;

import com.simibubi.create.content.trains.bogey.BogeyStyle;
import com.simibubi.create.content.trains.bogey.StandardBogeyBlockEntity;
import dev.railbound.registry.RailboundBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Block entity of {@link CoachBogeyBlock}: a Create bogey whose default style is the Railbound coach bogey. */
public class CoachBogeyBlockEntity extends StandardBogeyBlockEntity {

    public CoachBogeyBlockEntity(BlockPos pos, BlockState state) {
        super(RailboundBlockEntities.COACH_BOGEY.get(), pos, state);
    }

    @Override
    public BogeyStyle getDefaultStyle() {
        return RailboundBogeyStyles.COACH;
    }
}
