package dev.railbound.steam;

import dev.railbound.registry.RailboundBlockEntities;
import dev.railbound.trainset.design.SteamSpec;
import dev.railbound.trainset.load.TrainsetDesigns;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * A standing steam loco's coal bunker. It keeps the coal, the water and the banked boiler while the loco stands as
 * blocks; on a train the same state travels in {@link BunkerItemStorage} and {@link BunkerFluidStorage}.
 */
public class BunkerBlockEntity extends BlockEntity {
    public static final SteamSpec DEFAULT_STEAM = new SteamSpec(16000, 2);

    @Nullable
    private ResourceLocation designId;
    private final CoalSlots coal = new CoalSlots(DEFAULT_STEAM.bunkerSlots());
    private int water;
    private double pressure;
    private double fire;
    private boolean plugMelted;

    public BunkerBlockEntity(BlockPos pos, BlockState state) {
        super(RailboundBlockEntities.BUNKER.get(), pos, state);
        coal.onChange(this::setChanged);
    }

    /** Placement tells the bunker which design it belongs to: that sets its slots and tank size. */
    public void setDesignId(ResourceLocation designId) {
        this.designId = designId;
        coal.setUsable(steam().bunkerSlots());
        setChanged();
    }

    @Nullable
    public ResourceLocation designId() {
        return designId;
    }

    public SteamSpec steam() {
        return steamOf(designId);
    }

    public static SteamSpec steamOf(@Nullable ResourceLocation designId) {
        return Optional.ofNullable(designId).flatMap(TrainsetDesigns::get)
                .flatMap(design -> design.design().steam())
                .orElse(DEFAULT_STEAM);
    }

    public CoalSlots coal() {
        return coal;
    }

    public int water() {
        return water;
    }

    public IItemHandler coalIn() {
        return coal.insertOnly();
    }

    public IFluidHandler waterIn() {
        return new WaterTank(() -> water, () -> steam().water(), amount -> {
            water = amount;
            setChanged();
        });
    }

    public Boiler.Snapshot boiler() {
        return new Boiler.Snapshot(pressure, fire, water, plugMelted);
    }

    /** Written back by the mounted coal storage when the train is disassembled. */
    void restoreFromTrain(CoalSlots coal, double pressure, double fire, boolean plugMelted) {
        for (int slot = 0; slot < CoalSlots.MAX_SLOTS; slot++) {
            this.coal.setStackInSlot(slot, coal.getStackInSlot(slot).copy());
        }
        this.coal.setUsable(coal.usable());
        this.pressure = pressure;
        this.fire = fire;
        this.plugMelted = plugMelted;
        setChanged();
    }

    /** Written back by the mounted water storage when the train is disassembled. */
    void restoreWater(int water) {
        this.water = water;
        setChanged();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (designId != null) {
            tag.putString("Design", designId.toString());
        }
        tag.put("Coal", coal.serializeNBT(registries));
        tag.putInt("Slots", coal.usable());
        tag.putInt("Water", water);
        tag.putDouble("Pressure", pressure);
        tag.putDouble("Fire", fire);
        tag.putBoolean("PlugMelted", plugMelted);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        designId = tag.contains("Design") ? ResourceLocation.tryParse(tag.getString("Design")) : null;
        coal.deserializeNBT(registries, tag.getCompound("Coal"));
        coal.setUsable(tag.contains("Slots") ? tag.getInt("Slots") : DEFAULT_STEAM.bunkerSlots());
        water = tag.getInt("Water");
        pressure = tag.getDouble("Pressure");
        fire = tag.getDouble("Fire");
        plugMelted = tag.getBoolean("PlugMelted");
    }
}
