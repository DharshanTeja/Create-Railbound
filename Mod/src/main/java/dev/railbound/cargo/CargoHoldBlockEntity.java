package dev.railbound.cargo;

import dev.railbound.registry.RailboundBlockEntities;
import dev.railbound.trainset.load.TrainsetDesigns;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * A parked goods wagon's hold: its whole load, as many stacks as its design's {@code item_slots} (160 for the box van,
 * like a 2x2x2 Create vault). On a train the same load travels in {@link HoldItemStorage}.
 */
public class CargoHoldBlockEntity extends BlockEntity {
    public static final int DEFAULT_SLOTS = 160;

    @Nullable
    private ResourceLocation designId;
    private final ItemStackHandler items = new ItemStackHandler(DEFAULT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public CargoHoldBlockEntity(BlockPos pos, BlockState state) {
        super(RailboundBlockEntities.CARGO_HOLD.get(), pos, state);
    }

    /** Placement tells the hold which design it belongs to: that sets how many stacks it holds. */
    public void setDesignId(ResourceLocation designId) {
        this.designId = designId;
        if (isEmpty()) {
            items.setSize(slotsOf(designId));
        }
        setChanged();
    }

    public static int slotsOf(@Nullable ResourceLocation designId) {
        return Optional.ofNullable(designId).flatMap(TrainsetDesigns::get)
                .map(design -> design.design().cargo().itemSlots())
                .filter(slots -> slots > 0)
                .orElse(DEFAULT_SLOTS);
    }

    public ItemStackHandler items() {
        return items;
    }

    public boolean isEmpty() {
        for (int slot = 0; slot < items.getSlots(); slot++) {
            if (!items.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Written back by the mounted hold when the train is disassembled. */
    void restoreFromTrain(IItemHandler load) {
        items.setSize(load.getSlots());
        for (int slot = 0; slot < load.getSlots(); slot++) {
            items.setStackInSlot(slot, load.getStackInSlot(slot).copy());
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (designId != null) {
            tag.putString("Design", designId.toString());
        }
        tag.put("Items", items.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        designId = tag.contains("Design") ? ResourceLocation.tryParse(tag.getString("Design")) : null;
        items.deserializeNBT(registries, tag.getCompound("Items"));
    }
}
