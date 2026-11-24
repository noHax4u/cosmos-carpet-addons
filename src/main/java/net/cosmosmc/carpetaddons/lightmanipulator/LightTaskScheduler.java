package net.cosmosmc.carpetaddons.lightmanipulator;

/**
 * Implemented by {@link net.minecraft.server.level.ThreadedLevelLightEngine} through a mixin.
 */
public interface LightTaskScheduler {
    /**
     * Runs {@code task} on the light engine thread, before the next batch of light updates is processed,
     * and {@code afterUpdate} after that batch, once the changes are visible to readers (e.g. light packets).
     */
    void cca$submitLightTask(int chunkX, int chunkZ, Runnable task, Runnable afterUpdate);
}
