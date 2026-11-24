package net.cosmosmc.carpetaddons.lightmanipulator;

@FunctionalInterface
public interface LightFunction {
    /**
     * @param current the light level currently stored at this position
     * @return the new light level (0..15), or a negative value to leave the position unchanged
     */
    int compute(int x, int y, int z, int current);
}
