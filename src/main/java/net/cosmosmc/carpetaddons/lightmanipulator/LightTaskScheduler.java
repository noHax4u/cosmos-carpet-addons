package net.cosmosmc.carpetaddons.lightmanipulator;

/**
 * Implemented by {@link net.minecraft.server.level.ThreadedLevelLightEngine} through a mixin.
 */
public interface LightTaskScheduler {
    /**
     * Runs {@code task} on the light engine thread, before the next batch of light updates is processed.
     * Any light level changes done by the task are synced to clients afterward.
     */
    void cca$submitLightTask(int chunkX, int chunkZ, Runnable task);
}
