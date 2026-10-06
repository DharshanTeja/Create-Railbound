package dev.railbound.cargo;

/**
 * A big hold seen through a window of a few rows that scrolls: which hold slot each shown place holds. Rows are
 * {@code columns} slots wide; the last row may be part full, its other places empty.
 */
public record ScrollWindow(int slots, int columns, int visibleRows) {
    public int rows() {
        return (slots + columns - 1) / columns;
    }

    /** The furthest the window scrolls down: its last row shows the hold's last row. */
    public int maxRow() {
        return Math.max(0, rows() - visibleRows);
    }

    public int clampRow(int row) {
        return Math.max(0, Math.min(maxRow(), row));
    }

    /** The hold slot at shown place {@code shown} (0 at the top left) with the window scrolled to {@code row}, or -1. */
    public int slotAt(int row, int shown) {
        int slot = row * columns + shown;
        return slot < slots ? slot : -1;
    }
}
