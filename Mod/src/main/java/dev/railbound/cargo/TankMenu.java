package dev.railbound.cargo;

import dev.railbound.registry.RailboundMenus;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * A tank wagon's tank: a slot to pour full buckets (or any fluid container) in, a slot to fill empty ones from the
 * tank, and a gauge of what it holds. Each container is handled one at a time; what comes back goes to the
 * player's inventory.
 */
public class TankMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176;
    public static final int HEIGHT = 166;
    /** The two container slots stand one above the other right of the gauge, each with its label to its left. */
    public static final int SLOT_X = 150;
    public static final int POUR_Y = 22;
    public static final int FILL_Y = 48;
    public static final int GAUGE_X = 8;
    public static final int GAUGE_Y = 18;
    public static final int GAUGE_WIDTH = 96;
    public static final int GAUGE_HEIGHT = 52;
    public static final int INVENTORY_Y = 84;
    private static final int POUR = 0;
    private static final int FILL = 1;

    @Nullable
    private final IFluidHandler tank;
    private final Player player;
    private final SimpleContainer inputs = new SimpleContainer(2);
    private final ContainerData data;
    private final Predicate<Player> stillValid;

    /** Server side: the real tank. */
    public TankMenu(int id, Inventory inventory, IFluidHandler tank, Predicate<Player> stillValid) {
        this(id, inventory, tank, gauge(tank), stillValid);
    }

    /** Client side: the gauge follows by sync. */
    public TankMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, null, new SimpleContainerData(5), p -> true);
    }

    private TankMenu(int id, Inventory inventory, @Nullable IFluidHandler tank, ContainerData data, Predicate<Player> stillValid) {
        super(RailboundMenus.CARGO_TANK.get(), id);
        this.tank = tank;
        this.player = inventory.player;
        this.data = data;
        this.stillValid = stillValid;
        addSlot(new Slot(inputs, POUR, SLOT_X, POUR_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return FluidUtil.getFluidContained(stack).isPresent();
            }
        });
        addSlot(new Slot(inputs, FILL, SLOT_X, FILL_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return FluidUtil.getFluidHandler(stack.copyWithCount(1)).isPresent() && FluidUtil.getFluidContained(stack).isEmpty();
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, INVENTORY_Y + 58));
        }
        addDataSlots(data);
    }

    /** The tank as menu data: amount and capacity in two halves each (data travels as shorts), and the fluid. */
    private static ContainerData gauge(IFluidHandler tank) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                FluidStack fluid = tank.getFluidInTank(0);
                return switch (index) {
                    case 0 -> CargoRules.split(fluid.getAmount())[0];
                    case 1 -> CargoRules.split(fluid.getAmount())[1];
                    case 2 -> CargoRules.split(tank.getTankCapacity(0))[0];
                    case 3 -> CargoRules.split(tank.getTankCapacity(0))[1];
                    default -> fluid.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(fluid.getFluid());
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 5;
            }
        };
    }

    public int amount() {
        return CargoRules.join(data.get(0), data.get(1));
    }

    public int capacity() {
        return CargoRules.join(data.get(2), data.get(3));
    }

    /** The fluid in the tank, or empty. */
    public FluidStack fluid() {
        int id = data.get(4);
        if (id < 0 || amount() == 0) {
            return FluidStack.EMPTY;
        }
        Fluid fluid = BuiltInRegistries.FLUID.byId(id);
        return new FluidStack(fluid, amount());
    }

    /** Server: each tick, pour or fill one container from the slots, handing back what comes out. */
    @Override
    public void broadcastChanges() {
        if (tank != null) {
            work(POUR, CargoRules.pour(inputs.getItem(POUR), tank));
            work(FILL, CargoRules.draw(inputs.getItem(FILL), tank));
        }
        super.broadcastChanges();
    }

    private void work(int slot, CargoRules.Poured result) {
        if (inputs.getItem(slot).isEmpty() || result.giveBack().isEmpty()) {
            return;
        }
        inputs.setItem(slot, result.left());
        player.getInventory().placeItemBackInInventory(result.giveBack());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        clearContainer(player, inputs);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid.test(player);
    }

    /** Shift-click: a container to the slot it belongs in, or back to the inventory. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            int target = FluidUtil.getFluidContained(stack).isPresent() ? POUR : FILL;
            if (!slots.get(target).mayPlace(stack) || !moveItemStackTo(stack, target, target + 1, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }
}
