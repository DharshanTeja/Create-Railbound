package dev.railbound.cargo;

import dev.railbound.registry.RailboundMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * A goods wagon's hold seen six rows at a time, scrolling through all its stacks (see {@link ScrollWindow}). On the
 * server the shown slots are the hold's slots for the current scroll; on the client they just show what the server
 * sends, so the two never disagree. The client scrolls with a menu button click carrying the row.
 */
public class HoldMenu extends AbstractContainerMenu {
    public static final int COLUMNS = 9;
    public static final int ROWS = 6;
    public static final int SHOWN = COLUMNS * ROWS;
    public static final int WIDTH = 194;
    public static final int HEIGHT = 222;
    public static final int SLOT_X = 8;
    public static final int SLOT_Y = 18;
    public static final int INVENTORY_Y = SLOT_Y + ROWS * 18 + 14;

    private final ScrollWindow window;
    @Nullable
    private final IItemHandlerModifiable hold;
    private final Predicate<Player> stillValid;
    private int row;

    /** Server side: the real hold. */
    public HoldMenu(int id, Inventory inventory, IItemHandlerModifiable hold, Predicate<Player> stillValid) {
        this(id, inventory, hold.getSlots(), hold, stillValid);
    }

    /** Client side: the hold's size comes with the opening packet; its contents follow by sync. */
    public HoldMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readVarInt(), null, p -> true);
    }

    private HoldMenu(int id, Inventory inventory, int slots, @Nullable IItemHandlerModifiable hold, Predicate<Player> stillValid) {
        super(RailboundMenus.CARGO_HOLD.get(), id);
        this.window = new ScrollWindow(slots, COLUMNS, ROWS);
        this.hold = hold;
        this.stillValid = stillValid;
        Container shown = new SimpleContainer(SHOWN);
        for (int i = 0; i < SHOWN; i++) {
            int x = SLOT_X + (i % COLUMNS) * 18, y = SLOT_Y + (i / COLUMNS) * 18;
            addSlot(hold == null ? new ShownSlot(shown, i, x, y) : new HoldSlot(shown, i, x, y));
        }
        for (int r = 0; r < 3; r++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + r * 9 + 9, SLOT_X + col * 18, INVENTORY_Y + r * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, SLOT_X + col * 18, INVENTORY_Y + 58));
        }
    }

    public ScrollWindow window() {
        return window;
    }

    public int row() {
        return row;
    }

    /** Scrolls the client's view; the screen tells the server with a menu button click carrying the row. */
    public void scrollTo(int row) {
        this.row = window.clampRow(row);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        scrollTo(id);
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid.test(player);
    }

    /** Shift-click: a stack from the hold to the inventory, or from the inventory into the hold wherever it fits. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (hold == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem().copy();
        ItemStack original = stack.copy();
        if (index < SHOWN) {
            if (!moveItemStackTo(stack, SHOWN, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            stack = ItemHandlerHelper.insertItemStacked(hold, stack, false);
            if (stack.getCount() == original.getCount()) {
                return ItemStack.EMPTY;
            }
        }
        slot.set(stack);
        return original;
    }

    /** Server: one of the shown places, standing for the hold slot the window shows there. */
    private class HoldSlot extends Slot {
        HoldSlot(Container shown, int index, int x, int y) {
            super(shown, index, x, y);
        }

        private int holdSlot() {
            return window.slotAt(row, getContainerSlot());
        }

        @Override
        public ItemStack getItem() {
            int slot = holdSlot();
            return slot < 0 ? ItemStack.EMPTY : hold.getStackInSlot(slot);
        }

        @Override
        public void set(ItemStack stack) {
            int slot = holdSlot();
            if (slot >= 0) {
                hold.setStackInSlot(slot, stack);
            }
        }

        @Override
        public ItemStack remove(int amount) {
            int slot = holdSlot();
            if (slot < 0) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = hold.getStackInSlot(slot);
            ItemStack taken = stack.split(amount);
            hold.setStackInSlot(slot, stack);
            return taken;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            int slot = holdSlot();
            return slot >= 0 && hold.isItemValid(slot, stack);
        }

        @Override
        public int getMaxStackSize() {
            int slot = holdSlot();
            return slot < 0 ? 0 : hold.getSlotLimit(slot);
        }

        @Override
        public void setChanged() {
        }
    }

    /** Client: a shown place; past the end of the hold nothing goes in. */
    private class ShownSlot extends Slot {
        ShownSlot(Container shown, int index, int x, int y) {
            super(shown, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return window.slotAt(row, getContainerSlot()) >= 0;
        }
    }
}
