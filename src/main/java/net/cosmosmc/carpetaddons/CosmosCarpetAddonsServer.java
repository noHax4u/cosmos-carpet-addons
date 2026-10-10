package net.cosmosmc.carpetaddons;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import net.cosmosmc.carpetaddons.logging.Loggers;

public class CosmosCarpetAddonsServer implements CarpetExtension {
    private static CosmosCarpetAddonsServer INSTANCE = new CosmosCarpetAddonsServer();
    public static CosmosCarpetAddonsServer getInstance() {
        return INSTANCE;
    }

    public static void init() {
        CarpetServer.manageExtension(INSTANCE);
    }

    @Override
    public void registerLoggers() {
        Loggers.register();
    }
}
