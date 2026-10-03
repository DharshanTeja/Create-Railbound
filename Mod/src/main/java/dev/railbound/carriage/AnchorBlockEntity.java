package dev.railbound.carriage;

import dev.railbound.registry.RailboundBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class AnchorBlockEntity extends BlockEntity {
    private static final String DESIGN_KEY = "Design";
    @Nullable
    private ResourceLocation designId;

    public AnchorBlockEntity(BlockPos pos, BlockState state) {
        super(RailboundBlockEntities.ANCHOR.get(), pos, state);
    }

    @Nullable
    public ResourceLocation designId() {
        return designId;
    }

    public void setDesignId(ResourceLocation designId) {
        this.designId = designId;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (designId != null) {
            tag.putString(DESIGN_KEY, designId.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        designId = tag.contains(DESIGN_KEY) ? ResourceLocation.tryParse(tag.getString(DESIGN_KEY)) : null;
    }
}
