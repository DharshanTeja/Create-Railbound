package dev.railbound.client;

import dev.railbound.cargo.HoldMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** A goods wagon's hold: six rows of its stacks at a time, scrolled with the mouse wheel or by dragging the bar. */
public class HoldScreen extends AbstractContainerScreen<HoldMenu> {
    private static final int BAR_X = HoldMenu.SLOT_X + HoldMenu.COLUMNS * 18 + 4;
    private static final int BAR_WIDTH = 12;
    private static final int BAR_HEIGHT = HoldMenu.ROWS * 18;
    private static final int THUMB_HEIGHT = 15;
    private boolean dragging;

    public HoldScreen(HoldMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = HoldMenu.WIDTH;
        imageHeight = HoldMenu.HEIGHT;
        inventoryLabelY = HoldMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1E1F21);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF3B3E42);
        for (int i = 0; i < HoldMenu.SHOWN; i++) {
            boolean inHold = menu.window().slotAt(menu.row(), i) >= 0;
            slotFrame(graphics, x + HoldMenu.SLOT_X + (i % HoldMenu.COLUMNS) * 18, y + HoldMenu.SLOT_Y + (i / HoldMenu.COLUMNS) * 18, inHold);
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                slotFrame(graphics, x + HoldMenu.SLOT_X + col * 18, y + HoldMenu.INVENTORY_Y + row * 18, true);
            }
        }
        for (int col = 0; col < 9; col++) {
            slotFrame(graphics, x + HoldMenu.SLOT_X + col * 18, y + HoldMenu.INVENTORY_Y + 58, true);
        }
        // scroll bar: a track the height of the shown rows, the thumb where the window is in the hold
        int barX = x + BAR_X, barY = y + HoldMenu.SLOT_Y - 1;
        graphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, 0xFF17181A);
        int max = menu.window().maxRow();
        int thumbY = barY + (max == 0 ? 0 : (BAR_HEIGHT - THUMB_HEIGHT) * menu.row() / max);
        graphics.fill(barX + 1, thumbY + 1, barX + BAR_WIDTH - 1, thumbY + THUMB_HEIGHT - 1, max == 0 ? 0xFF5A5D62 : 0xFFB8BCC2);
    }

    private static void slotFrame(GuiGraphics graphics, int x, int y, boolean inHold) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF17181A);
        graphics.fill(x, y, x + 16, y + 16, inHold ? 0xFF8B8B8B : 0xFF2A2C2F);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollTo(menu.row() - (int) Math.signum(scrollY));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (onBar(mouseX, mouseY)) {
            dragging = true;
            scrollToMouse(mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            scrollToMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean onBar(double mouseX, double mouseY) {
        double x = mouseX - leftPos - BAR_X, y = mouseY - topPos - HoldMenu.SLOT_Y;
        return x >= 0 && x < BAR_WIDTH && y >= 0 && y < BAR_HEIGHT;
    }

    private void scrollToMouse(double mouseY) {
        double share = (mouseY - topPos - HoldMenu.SLOT_Y - THUMB_HEIGHT / 2.0) / (BAR_HEIGHT - THUMB_HEIGHT);
        scrollTo((int) Math.round(Math.max(0, Math.min(1, share)) * menu.window().maxRow()));
    }

    /** Scrolls here and tells the server, which shows the same rows in the slots it sends. */
    private void scrollTo(int row) {
        int clamped = menu.window().clampRow(row);
        if (clamped != menu.row() && minecraft != null && minecraft.gameMode != null) {
            menu.scrollTo(clamped);
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, clamped);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
