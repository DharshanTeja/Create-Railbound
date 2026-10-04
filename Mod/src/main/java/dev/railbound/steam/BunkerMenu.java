package dev.railbound.steam;

import dev.railbound.registry.RailboundMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.Predicate;

/** A loco's coal bunker: its coal slots, with the boiler and tank gauges alongside. */
public class BunkerMenu extends AbstractContainerMenu {
    /** Wide enough for the gauges beside their bars; the inventory sits centred under them. */
    public static final int WIDTH = 222;
    public static final int HEIGHT = 186;
    public static final int INVENTORY_X = (WIDTH - 9 * 18) / 2;
    public static final int SLOT_X = INVENTORY_X;
    public static final int SLOT_Y = 20;
    public static final int INVENTORY_Y = 104;

    private final int coalSlots;
    private final ContainerData data;
    private final Predicate<Player> stillValid;

    /** Server side: the real bunker. */
    public BunkerMenu(int id, Inventory inventory, IItemHandler coal, int coalSlots, ContainerData data,
                      Predicate<Player> stillValid) {
        super(RailboundMenus.BUNKER.get(), id);
        this.coalSlots = coalSlots;
        this.data = data;
        this.stillValid = stillValid;
        for (int i = 0; i < coalSlots; i++) {
            addSlot(new SlotItemHandler(coal, i, SLOT_X + i * 18, SLOT_Y));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 58));
        }
        addDataSlots(data);
    }

    /** Client side: the slot count comes with the opening packet; contents and gauges follow by sync. */
    public BunkerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, new CoalSlots(buf.readVarInt()), new SimpleContainerData(SteamGauges.DATA_SLOTS));
    }

    private BunkerMenu(int id, Inventory inventory, CoalSlots coal, ContainerData data) {
        this(id, inventory, coal, coal.usable(), data, p -> true);
    }

    public int coalSlots() {
        return coalSlots;
    }

    public SteamGauges gauges() {
        return SteamGauges.read(data);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid.test(player);
    }

    /** Shift-click: coal between the bunker and the inventory. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean fromBunker = index < coalSlots;
        if (fromBunker ? !moveItemStackTo(stack, coalSlots, slots.size(), true)
                : !CoalSlots.isCoal(stack) || !moveItemStackTo(stack, 0, coalSlots, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }
}
