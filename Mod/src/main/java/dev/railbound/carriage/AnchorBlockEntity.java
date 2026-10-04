package dev.railbound.carriage;

import dev.railbound.steam.DriveGear;
import dev.railbound.steam.Exhaust;
import dev.railbound.registry.RailboundBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class AnchorBlockEntity extends BlockEntity {
    private static final String DESIGN_KEY = "Design";
    @Nullable
    private ResourceLocation designId;

    /** Client only: a loco's driving-wheel angle (radians) this tick and last, advanced as its train rolls. */
    private double driveAngle;
    private double prevDriveAngle;

    public AnchorBlockEntity(BlockPos pos, BlockState state) {
        super(RailboundBlockEntities.ANCHOR.get(), pos, state);
    }

    /** Rolls the driving wheels a distance (blocks, positive towards the carriage front). */
    public void roll(double distance, double wheelRadiusPixels) {
        prevDriveAngle = driveAngle;
        driveAngle = DriveGear.rolled(driveAngle, distance, wheelRadiusPixels) % (Math.PI * 2);
        if (Math.abs(driveAngle - prevDriveAngle) > Math.PI) {
            prevDriveAngle = driveAngle;   // wrapped round: do not sweep back through a whole turn
        }
    }

    /** The driving-wheel angle between the last two ticks. */
    public double driveAngle(float partialTick) {
        return prevDriveAngle + (driveAngle - prevDriveAngle) * partialTick;
    }

    /** Client only: how sharply the loco is turning (radians per block, + right), smoothed, and last tick's heading. */
    private double curvature;
    @Nullable
    private net.minecraft.world.phys.Vec3 lastForward;

    /** Learns the curve from how the heading turned over the distance rolled; keeps it while standing on a curve. */
    public void noteHeading(net.minecraft.world.phys.Vec3 forward, double distance) {
        if (lastForward != null && Math.abs(distance) > 0.02) {
            double measured = Math.max(-0.5, Math.min(0.5, DriveGear.curvature(lastForward, forward, distance)));
            curvature += (measured - curvature) * 0.25;
        }
        lastForward = forward;
    }

    public double curvature() {
        return curvature;
    }

    /** Client only: this tick's and last tick's speed, and the coal last seen, to tell accelerating and firing apart. */
    private double speed;
    private double speedBefore;
    private int lastLumps = -1;
    private int ticksStopped = Exhaust.LONG_STOPPED;
    private int darkTicks;

    /**
     * Notes this tick's speed (blocks per tick) and coal (lumps). Returns true while the smoke is dark after firing,
     * which starts when the coal count drops.
     */
    public boolean noteSpeedAndCoal(double speed, int lumps) {
        speedBefore = this.speed;
        this.speed = speed;
        if (speed >= 0.01) {
            ticksStopped = -1;
        } else if (ticksStopped < Exhaust.LONG_STOPPED) {
            ticksStopped++;
        }
        if (lastLumps >= 0 && lumps < lastLumps) {
            darkTicks = 40;
        }
        lastLumps = lumps;
        if (darkTicks > 0) {
            darkTicks--;
            return true;
        }
        return false;
    }

    /** Ticks since the loco came to a stand: -1 while it moves, {@link Exhaust#LONG_STOPPED} if it never has. */
    public int ticksStopped() {
        return ticksStopped;
    }

    /** The speed noted the tick before the latest one. */
    public double lastSpeed() {
        return speedBefore;
    }

    /** The driving-wheel angle at the last tick and the one before, for counting chuffs. */
    public double driveAngle() {
        return driveAngle;
    }

    public double prevDriveAngle() {
        return prevDriveAngle;
    }

    @Nullable
    public ResourceLocation designId() {
        return designId;
    }

    public void setDesignId(ResourceLocation designId) {
        this.designId = designId;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Clients (and Create's moving-train copy) need the design to draw the carriage. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
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
