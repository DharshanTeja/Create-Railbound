package dev.railbound.client;

import dev.railbound.steam.BunkerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The coal bunker: its slots, the boiler and tank gauges, and the player's inventory. */
public class BunkerScreen extends AbstractContainerScreen<BunkerMenu> {

    public BunkerScreen(BunkerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = BunkerMenu.WIDTH;
        imageHeight = BunkerMenu.HEIGHT;
        titleLabelX = BunkerMenu.INVENTORY_X;
        inventoryLabelY = BunkerMenu.INVENTORY_Y - 11;
        inventoryLabelX = BunkerMenu.INVENTORY_X;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1E1F21);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF3B3E42);
        for (int i = 0; i < menu.coalSlots(); i++) {
            slotFrame(graphics, x + BunkerMenu.SLOT_X + i * 18, y + BunkerMenu.SLOT_Y);
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                slotFrame(graphics, x + BunkerMenu.INVENTORY_X + col * 18, y + BunkerMenu.INVENTORY_Y + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            slotFrame(graphics, x + BunkerMenu.INVENTORY_X + col * 18, y + BunkerMenu.INVENTORY_Y + 58);
        }
        SteamGaugeDrawing.draw(graphics, font, x + 8, y + 42, menu.gauges(), SteamGaugeDrawing.SCREEN_BAR);
    }

    private static void slotFrame(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF17181A);
        graphics.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
