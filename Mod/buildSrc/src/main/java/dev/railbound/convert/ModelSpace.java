package dev.railbound.convert;

/**
 * Blockbench model units to design block space. The model is centred on x = 0 and z = 0, y = 0 is the floor
 * (top of layer 0), and 16 units are one block. Design block space has cell (x, y, z) at [x, x+1) etc., with
 * x running -1..1 across, layer 0 at the bogeys, and z = 0 at the front.
 */
public record ModelSpace(int length) {

    public double[] toBlock(double mx, double my, double mz) {
        return new double[] {(mx + 8) / 16, my / 16 + 1, (mz + 8 * length) / 16};
    }
}
