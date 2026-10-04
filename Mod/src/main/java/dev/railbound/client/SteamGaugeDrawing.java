package dev.railbound.client;

import dev.railbound.steam.SteamGauges;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/**
 * Draws a loco's gauges in tidy columns: labels, bars, figures, then a status line. Every column is measured from
 * its text first, so nothing overlaps whatever the language or figures.
 */
final class SteamGaugeDrawing {
    static final int HUD_BAR = 96;
    static final int SCREEN_BAR = 64;
    private static final int ROW = 12;
    private static final int GAP = 6;
    private static final int TEXT = 0xFFE6E6E6;
    private static final int DIM = 0xFF9A9A9A;

    private SteamGaugeDrawing() {}

    /** Width and height the gauges will take with this bar width. */
    static int[] size(Font font, SteamGauges g, int bar) {
        int width = labelColumn(font) + bar + GAP + valueColumn(font, g);
        width = Math.max(width, font.width(status(g)) + 10);
        return new int[] {width, ROW * 3 + 10};
    }

    /** Draws the gauges with their top-left corner at (x, y). */
    static void draw(GuiGraphics graphics, Font font, int x, int y, SteamGauges g, int bar) {
        int bx = x + labelColumn(font);
        int vx = bx + bar + GAP;

        row(graphics, font, x, y, Component.translatable("railbound.gauge.pressure"));
        bar(graphics, bx, y + 1, bar, g.pressure(), g.canMove() ? 0xFF6FBF4A : 0xFFD9A441);
        int mark = bx + (int) Math.round(bar * SteamGauges.movingPressure());
        graphics.fill(mark, y - 1, mark + 1, y + 9, 0xFFFF5040);
        graphics.drawString(font, pressure(g), vx, y, TEXT, true);

        int wy = y + ROW;
        row(graphics, font, x, wy, Component.translatable("railbound.gauge.water"));
        bar(graphics, bx, wy + 1, bar, g.capacity() <= 0 ? 0 : (double) g.water() / g.capacity(), 0xFF3F76E4);
        graphics.drawString(font, water(g), vx, wy, TEXT, true);

        int cy = wy + ROW;
        row(graphics, font, x, cy, Component.translatable("railbound.gauge.coal"));
        graphics.drawString(font, Component.translatable("railbound.gauge.lumps", g.lumps()), bx, cy, TEXT, true);

        int sy = cy + ROW + 2;
        int colour = statusColour(g);
        graphics.fill(x, sy + 1, x + 6, sy + 7, colour);
        graphics.drawString(font, status(g), x + 10, sy, colour, true);
    }

    private static void row(GuiGraphics graphics, Font font, int x, int y, Component label) {
        graphics.drawString(font, label, x, y, DIM, true);
    }

    private static int labelColumn(Font font) {
        int widest = 0;
        for (String key : List.of("railbound.gauge.pressure", "railbound.gauge.water", "railbound.gauge.coal")) {
            widest = Math.max(widest, font.width(Component.translatable(key)));
        }
        return widest + GAP;
    }

    private static int valueColumn(Font font, SteamGauges g) {
        // measured on the widest figures the gauges can show, so the box does not jump as they change
        SteamGauges widest = new SteamGauges(1, g.capacity(), g.capacity(), 0, true, false);
        return Math.max(font.width(pressure(widest)), font.width(water(widest)));
    }

    private static String pressure(SteamGauges g) {
        return (int) Math.round(g.pressure() * 100) + "%";
    }

    /** Buckets to two places (Create counts water in millibuckets), so the level is seen falling as the loco runs. */
    private static String water(SteamGauges g) {
        return String.format(Locale.ROOT, "%.2f / %.2f B", g.water() / 1000.0, g.capacity() / 1000.0);
    }

    private static Component status(SteamGauges g) {
        if (g.plugMelted()) {
            return Component.translatable("railbound.gauge.plug_melted");
        }
        if (!g.fireLit()) {
            return Component.translatable("railbound.gauge.fire_out");
        }
        return Component.translatable(g.canMove() ? "railbound.gauge.ready" : "railbound.gauge.raising_steam");
    }

    private static int statusColour(SteamGauges g) {
        if (g.plugMelted()) {
            return 0xFFFF5A48;
        }
        if (!g.fireLit()) {
            return DIM;
        }
        return g.canMove() ? 0xFF7FD45A : 0xFFFFA940;
    }

    private static void bar(GuiGraphics graphics, int x, int y, int width, double fraction, int colour) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + 8, 0xFF0C0C0C);
        graphics.fill(x, y, x + width, y + 7, 0xFF353535);
        int filled = (int) Math.round(width * Math.max(0, Math.min(1, fraction)));
        graphics.fill(x, y, x + filled, y + 7, colour);
        graphics.fill(x, y, x + filled, y + 1, (colour & 0x00FFFFFF) | 0x60FFFFFF);
    }
}
