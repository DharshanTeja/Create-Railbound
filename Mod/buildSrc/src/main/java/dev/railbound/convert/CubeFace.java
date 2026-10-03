package dev.railbound.convert;

/**
 * One textured cube face. Corners are top-left, top-right, bottom-right, bottom-left as seen from outside
 * (counter-clockwise when walked in reverse), in model units; uvs are the matching texture pixels.
 */
public record CubeFace(String direction, double[][] corners, double[][] uvs, int texture) {
}
