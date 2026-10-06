package dev.railbound.client;

import dev.railbound.cargo.TankMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/** A tank wagon's tank: the fluid gauge, a slot to pour containers in and one to fill them, and the inventory. */
public class TankScreen extends AbstractContainerScreen<TankMenu> {
    public TankScreen(TankMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = TankMenu.WIDTH;
        imageHeight = TankMenu.HEIGHT;
        inventoryLabelY = TankMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1E1F21);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF3B3E42);
        gauge(graphics, x + TankMenu.GAUGE_X, y + TankMenu.GAUGE_Y);
        slotFrame(graphics, x + TankMenu.SLOT_X, y + TankMenu.POUR_Y);
        slotFrame(graphics, x + TankMenu.SLOT_X, y + TankMenu.FILL_Y);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                slotFrame(graphics, x + 8 + col * 18, y + TankMenu.INVENTORY_Y + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            slotFrame(graphics, x + 8 + col * 18, y + TankMenu.INVENTORY_Y + 58);
        }
    }

    /** The fluid's own texture filling the gauge from the bottom, tinted as the fluid is in the world. */
    private void gauge(GuiGraphics graphics, int x, int y) {
        int w = TankMenu.GAUGE_WIDTH, h = TankMenu.GAUGE_HEIGHT;
        graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF17181A);
        graphics.fill(x, y, x + w, y + h, 0xFF2A2C2F);
        FluidStack fluid = menu.fluid();
        int capacity = Math.max(1, menu.capacity());
        int filled = fluid.isEmpty() ? 0 : Math.max(1, (int) ((long) h * menu.amount() / capacity));
        if (filled > 0) {
            IClientFluidTypeExtensions look = IClientFluidTypeExtensions.of(fluid.getFluid());
            TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(look.getStillTexture(fluid));
            int tint = look.getTintColor(fluid);
            float r = (tint >> 16 & 0xFF) / 255f, g = (tint >> 8 & 0xFF) / 255f, b = (tint & 0xFF) / 255f;
            for (int ty = y + h - filled; ty < y + h; ty += 16) {
                for (int tx = x; tx < x + w; tx += 16) {
                    int tw = Math.min(16, x + w - tx), th = Math.min(16, y + h - ty);
                    graphics.blit(tx, ty, 0, tw, th, sprite, r, g, b, 1);
                }
            }
        }
    }

    private static void slotFrame(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF17181A);
        graphics.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        FluidStack fluid = menu.fluid();
        Component name = fluid.isEmpty() ? Component.translatable("railbound.cargo.empty") : fluid.getHoverName();
        Component amount = Component.translatable("railbound.cargo.amount",
                String.format("%.1f", menu.amount() / 1000.0), menu.capacity() / 1000);
        // what is in the tank and how much, on one line under the gauge
        Component line = name.copy().append(Component.literal("  ")).append(amount);
        graphics.drawString(font, font.plainSubstrByWidth(line.getString(), imageWidth - 16), TankMenu.GAUGE_X,
                TankMenu.GAUGE_Y + TankMenu.GAUGE_HEIGHT + 3, 0xFFE0E0E0, false);
        Component pour = Component.translatable("railbound.cargo.pour"), fill = Component.translatable("railbound.cargo.fill");
        graphics.drawString(font, pour, TankMenu.SLOT_X - 4 - font.width(pour), TankMenu.POUR_Y + 4, 0xFFE0E0E0, false);
        graphics.drawString(font, fill, TankMenu.SLOT_X - 4 - font.width(fill), TankMenu.FILL_Y + 4, 0xFFE0E0E0, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
