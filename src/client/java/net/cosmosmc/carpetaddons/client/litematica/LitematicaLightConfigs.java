package net.cosmosmc.carpetaddons.client.litematica;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.data.json.JsonUtils;
import net.cosmosmc.carpetaddons.CosmosCarpetAddons;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Light level options shown in Litematica's config screen (in its Generic, Info Overlays and Colors tabs).
 * They are stored in their own file next to Litematica's, and loaded and saved together with it.
 */
public final class LitematicaLightConfigs {
    private static final String PREFIX = "cosmos-carpet-addons.litematica.config";
    private static final String FILE_NAME = CosmosCarpetAddons.MOD_ID + "-litematica.json";

    // Generic
    public static final ConfigBoolean SAVE_INCLUDE_LIGHT = (ConfigBoolean) new ConfigBoolean("schematicSaveIncludeLight", false).apply(PREFIX);
    public static final ConfigBoolean PASTE_INCLUDE_LIGHT = (ConfigBoolean) new ConfigBoolean("schematicPasteIncludeLight", true).apply(PREFIX);
    public static final ConfigBoolean VERIFY_LIGHT = (ConfigBoolean) new ConfigBoolean("verifierCheckLightLevels", true).apply(PREFIX);
    public static final ConfigBoolean VERIFY_SKY_LIGHT = (ConfigBoolean) new ConfigBoolean("verifierCheckSkyLight", true).apply(PREFIX);

    // Info Overlays
    public static final ConfigBoolean LIGHT_OVERLAY_ENABLED = (ConfigBoolean) new ConfigBoolean("verifierLightOverlayEnabled", false).apply(PREFIX);
    public static final ConfigBoolean LIGHT_OVERLAY_OVER_OTHER_MARKERS = (ConfigBoolean) new ConfigBoolean("verifierLightOverlayOverOtherMarkers", false).apply(PREFIX);
    public static final ConfigBoolean LIGHT_INFO_THROUGH_BLOCKS = (ConfigBoolean) new ConfigBoolean("verifierLightInfoThroughBlocks", false).apply(PREFIX);
    public static final ConfigInteger LIGHT_OVERLAY_MAX_POSITIONS = (ConfigInteger) new ConfigInteger("verifierLightOverlayMaxPositions", 1000, 1, 1000000).apply(PREFIX);

    // Colors
    public static final ConfigColor LIGHT_OVERLAY_COLOR = new ConfigColor("schematicOverlayColorWrongLight", "#FF9966FF").apply(PREFIX);

    public static final List<IConfigBase> GENERIC = ImmutableList.of(SAVE_INCLUDE_LIGHT, PASTE_INCLUDE_LIGHT, VERIFY_LIGHT, VERIFY_SKY_LIGHT);
    public static final List<IConfigBase> INFO_OVERLAYS = ImmutableList.of(LIGHT_OVERLAY_ENABLED, LIGHT_OVERLAY_OVER_OTHER_MARKERS, LIGHT_INFO_THROUGH_BLOCKS, LIGHT_OVERLAY_MAX_POSITIONS);
    public static final List<IConfigBase> COLORS = ImmutableList.of(LIGHT_OVERLAY_COLOR);

    private LitematicaLightConfigs() {
    }

    public static void load() {
        Path file = FileUtils.getConfigDirectory().resolve(FILE_NAME);
        if (!Files.isReadable(file)) {
            return;
        }
        JsonElement element = JsonUtils.parseJsonFile(file);
        if (element != null && element.isJsonObject()) {
            JsonObject root = element.getAsJsonObject();
            ConfigUtils.readConfigBase(root, "Generic", GENERIC);
            ConfigUtils.readConfigBase(root, "InfoOverlays", INFO_OVERLAYS);
            ConfigUtils.readConfigBase(root, "Colors", COLORS);
        }
    }

    public static void save() {
        Path dir = FileUtils.getConfigDirectory();
        if (!Files.isDirectory(dir)) {
            return;
        }
        JsonObject root = new JsonObject();
        ConfigUtils.writeConfigBase(root, "Generic", GENERIC);
        ConfigUtils.writeConfigBase(root, "InfoOverlays", INFO_OVERLAYS);
        ConfigUtils.writeConfigBase(root, "Colors", COLORS);
        JsonUtils.writeJsonToFile(root, dir.resolve(FILE_NAME));
    }
}
