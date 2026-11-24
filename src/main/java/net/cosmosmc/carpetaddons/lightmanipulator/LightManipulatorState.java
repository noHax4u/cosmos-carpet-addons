package net.cosmosmc.carpetaddons.lightmanipulator;

import net.cosmosmc.carpetaddons.settings.CosmosCarpetAddonsSettings;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.lighting.LevelLightEngine;

public final class LightManipulatorState {
    /** Set on the client from {@link net.cosmosmc.carpetaddons.lightmanipulator.network.ManipulatorStatePayload}. */
    public static volatile boolean clientActive = false;

    private LightManipulatorState() {
    }

    /**
     * Whether block changes should be ignored by {@code engine}: the server's light engine follows the carpet rule,
     * client light engines follow what the server told them.
     */
    public static boolean ignoresLightUpdates(LevelLightEngine engine) {
        return engine instanceof ThreadedLevelLightEngine ? CosmosCarpetAddonsSettings.lightLevelManipulator : clientActive;
    }
}
