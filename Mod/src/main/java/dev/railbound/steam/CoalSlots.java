package dev.railbound.steam;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A coal bunker's slots. Only the coal family goes in, and only the design's number of slots is open. The fireman
 * takes coal from here; from outside (interfaces, hoppers, pipes) coal can only be put in, never taken out.
 */
public class CoalSlots extends ItemStackHandler {
    public static final int MAX_SLOTS = 9;
    private int usable;
    private Runnable onChange = () -> {};

    public CoalSlots(int usable) {
        super(MAX_SLOTS);
        setUsable(usable);
    }

    public static CoalSlots of(List<ItemStack> stacks, int usable) {
        CoalSlots slots = new CoalSlots(usable);
        for (int i = 0; i < Math.min(MAX_SLOTS, stacks.size()); i++) {
            slots.stacks.set(i, stacks.get(i).copy());
        }
        return slots;
    }

    public void setUsable(int usable) {
        this.usable = Math.max(1, Math.min(MAX_SLOTS, usable));
    }

    public int usable() {
        return usable;
    }

    public void onChange(Runnable onChange) {
        this.onChange = onChange;
    }

    public List<ItemStack> contents() {
        List<ItemStack> copy = new ArrayList<>();
        stacks.forEach(stack -> copy.add(stack.copy()));
        return copy;
    }

    public static boolean isCoal(ItemStack stack) {
        return CoalFuel.accepts(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return slot < usable && isCoal(stack);
    }

    @Override
    protected void onContentsChanged(int slot) {
        onChange.run();
    }

    /** The fireman's shovel: one item from the first filled slot, worth this many lumps of coal (0 when empty). */
    public double take() {
        for (int slot = 0; slot < getSlots(); slot++) {
            ItemStack one = extractItem(slot, 1, false);
            if (!one.isEmpty()) {
                return CoalFuel.value(BuiltInRegistries.ITEM.getKey(one.getItem()));
            }
        }
        return 0;
    }

    /** Puts a held stack of coal into the bunker; returns what did not fit. */
    public ItemStack feed(ItemStack held) {
        ItemStack rest = held;
        for (int slot = 0; slot < usable && !rest.isEmpty(); slot++) {
            rest = insertItem(slot, rest, false);
        }
        return rest;
    }

    /** Lumps of coal left in the bunker. */
    public int lumps() {
        int lumps = 0;
        for (ItemStack stack : stacks) {
            lumps += stack.getCount() * CoalFuel.value(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }
        return lumps;
    }

    /** The bunker as seen from outside: coal goes in, nothing comes out. */
    public IItemHandler insertOnly() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return CoalSlots.this.getSlots();
            }

            @Override
            public @NotNull ItemStack getStackInSlot(int slot) {
                return CoalSlots.this.getStackInSlot(slot);
            }

            @Override
            public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                return CoalSlots.this.insertItem(slot, stack, simulate);
            }

            @Override
            public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return CoalSlots.this.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return CoalSlots.this.isItemValid(slot, stack);
            }
        };
    }
}
