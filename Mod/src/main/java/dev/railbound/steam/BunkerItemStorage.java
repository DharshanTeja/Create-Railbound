package dev.railbound.steam;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.api.contraption.storage.SyncedMountedStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.api.contraption.storage.item.WrapperMountedItemStorage;
import com.simibubi.create.content.contraptions.Contraption;
import dev.railbound.registry.RailboundStorageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * A steam loco's bunker on a train: the coal and the boiler, ticked with the train (see {@link LocoSteam}) even
 * where no player is near. Synced to clients for the cab gauges. Interfaces can load coal but never unload it.
 */
public class BunkerItemStorage extends WrapperMountedItemStorage<CoalSlots> implements SyncedMountedStorage {
    public static final MapCodec<BunkerItemStorage> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("design").forGetter(s -> Optional.ofNullable(s.designId)),
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("coal").forGetter(s -> s.wrapped.contents()),
            Codec.INT.fieldOf("slots").forGetter(s -> s.wrapped.usable()),
            Codec.DOUBLE.fieldOf("pressure").forGetter(s -> s.pressure),
            Codec.DOUBLE.fieldOf("fire").forGetter(s -> s.fire),
            Codec.DOUBLE.fieldOf("water_fraction").forGetter(s -> s.waterFraction),
            Codec.BOOL.fieldOf("plug_melted").forGetter(s -> s.plugMelted)
    ).apply(i, (design, coal, slots, pressure, fire, fraction, plug) ->
            new BunkerItemStorage(design.orElse(null), CoalSlots.of(coal, slots), pressure, fire, fraction, plug)));

    @Nullable
    private final ResourceLocation designId;
    private double pressure;
    private double fire;
    /** The part of a millibucket the boiler has used but the integer tank has not yet given up. */
    private double waterFraction;
    private boolean plugMelted;
    private double pull;
    private boolean dirty;

    private BunkerItemStorage(@Nullable ResourceLocation designId, CoalSlots coal, double pressure, double fire,
                              double waterFraction, boolean plugMelted) {
        this(RailboundStorageTypes.BUNKER_ITEMS.get(), designId, coal, pressure, fire, waterFraction, plugMelted);
    }

    private BunkerItemStorage(MountedItemStorageType<?> type, @Nullable ResourceLocation designId, CoalSlots coal,
                              double pressure, double fire, double waterFraction, boolean plugMelted) {
        super(type, coal);
        this.designId = designId;
        this.pressure = pressure;
        this.fire = fire;
        this.waterFraction = waterFraction;
        this.plugMelted = plugMelted;
        coal.onChange(() -> dirty = true);
    }

    public static BunkerItemStorage of(BunkerBlockEntity bunker) {
        Boiler.Snapshot boiler = bunker.boiler();
        return new BunkerItemStorage(bunker.designId(), CoalSlots.of(bunker.coal().contents(), bunker.coal().usable()),
                boiler.pressure(), boiler.fire(), 0, boiler.plugMelted());
    }

    @Nullable
    public ResourceLocation designId() {
        return designId;
    }

    /** One tick of the boiler at the given effort, burning coal from these slots and water from the tank. */
    public void tick(double effort, BunkerFluidStorage water) {
        Boiler boiler = Boiler.restore(water.capacity(),
                new Boiler.Snapshot(pressure, fire, water.amount() + waterFraction, plugMelted));
        boiler.tick(effort, wrapped::take);
        Boiler.Snapshot after = boiler.snapshot();
        boolean changed = Math.abs(after.pressure() - pressure) > 1e-3 || after.plugMelted() != plugMelted
                || (after.fire() > 0) != (fire > 0);
        pressure = after.pressure();
        fire = after.fire();
        plugMelted = after.plugMelted();
        int whole = (int) Math.floor(after.water());
        waterFraction = after.water() - whole;
        water.setAmount(whole);
        pull = boiler.pull();
        dirty |= changed;
    }

    /** How hard this loco can pull after its last tick, see {@link Boiler#pull()}. */
    public double pull() {
        return pull;
    }

    public double pressure() {
        return pressure;
    }

    public boolean fireLit() {
        return fire > 0;
    }

    public boolean plugMelted() {
        return plugMelted;
    }

    public int lumps() {
        return wrapped.lumps();
    }

    /** Interfaces and hoppers may load coal, but must never take it out of the bunker. */
    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    /** On a train: coal in hand goes straight in; otherwise the bunker screen, reading this storage and its tank. */
    @Override
    public boolean handleInteraction(ServerPlayer player, Contraption contraption, StructureBlockInfo info) {
        ItemStack held = player.getMainHandItem();
        if (CoalSlots.isCoal(held)) {
            ItemStack rest = wrapped.feed(held.copy());
            if (!player.getAbilities().instabuild) {
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, rest);
            }
            return true;
        }
        if (!(contraption.getStorage().getFluids().storages.get(info.pos()) instanceof BunkerFluidStorage tank)) {
            return false;
        }
        Vec3 centre = Vec3.atCenterOf(info.pos());
        int slots = wrapped.usable();
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new BunkerMenu(id, inventory, wrapped, slots,
                SteamGauges.data(() -> SteamGauges.of(this, tank)),
                who -> contraption.entity.isAlive() && who.distanceToSqr(contraption.entity.toGlobalVector(centre, 0)) < 64),
                info.state().getBlock().getName()), buf -> buf.writeVarInt(slots));
        return true;
    }

    /** Players at the bunker may shovel coal back out. */
    @Override
    protected IItemHandlerModifiable getHandlerForMenu(StructureBlockInfo info, Contraption contraption) {
        return wrapped;
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (be instanceof BunkerBlockEntity bunker) {
            bunker.restoreFromTrain(wrapped, pressure, fire, plugMelted);
        }
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void markClean() {
        dirty = false;
    }

    @Override
    public void afterSync(Contraption contraption, BlockPos localPos) {
    }

    /** Lets {@link LocoSteam} find a train's bunkers among its storages. */
    public static List<BunkerItemStorage> in(Iterable<?> storages) {
        List<BunkerItemStorage> bunkers = new java.util.ArrayList<>();
        for (Object storage : storages) {
            if (storage instanceof BunkerItemStorage bunker) {
                bunkers.add(bunker);
            }
        }
        return bunkers;
    }
}
