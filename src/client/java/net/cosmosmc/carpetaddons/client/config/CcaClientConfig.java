package net.cosmosmc.carpetaddons.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.cosmosmc.carpetaddons.CosmosCarpetAddons;
import net.cosmosmc.carpetaddons.lightmanipulator.network.ClientOptionsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Client options of Cosmos Carpet Addons, stored in {@code config/cosmos-carpet-addons-client.json} and editable
 * through Mod Menu. (The Litematica related options are in Litematica's own config screen.)
 */
public final class CcaClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve(CosmosCarpetAddons.MOD_ID + "-client.json");

    private static CcaClientConfig instance = new CcaClientConfig();

    /** Show the light level at the targeted position while holding a light item or nether portal item. */
    public boolean showLightLevelWhenHoldingLightItem = true;
    /** Axiom's "no updates" mode also skips light updates for your edits (needs this mod on the server). */
    public boolean axiomNoUpdatesSkipsLight = false;

    public static CcaClientConfig get() {
        return instance;
    }

    public static void load() {
        if (Files.isReadable(FILE)) {
            try {
                CcaClientConfig loaded = GSON.fromJson(Files.readString(FILE), CcaClientConfig.class);
                if (loaded != null) {
                    instance = loaded;
                }
            } catch (Exception e) {
                CosmosCarpetAddons.LOGGER.warn("Failed to read {}", FILE, e);
            }
        }
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(instance));
        } catch (IOException e) {
            CosmosCarpetAddons.LOGGER.warn("Failed to write {}", FILE, e);
        }
        sendToServer();
    }

    /** Tells the server about the options it needs, if it has this mod. */
    public static void sendToServer() {
        if (Minecraft.getInstance().getConnection() != null && ClientPlayNetworking.canSend(ClientOptionsPayload.TYPE)) {
            ClientPlayNetworking.send(new ClientOptionsPayload(instance.axiomNoUpdatesSkipsLight));
        }
    }
}
