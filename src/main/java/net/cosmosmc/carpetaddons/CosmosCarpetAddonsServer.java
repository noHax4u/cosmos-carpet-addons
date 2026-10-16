package net.cosmosmc.carpetaddons;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.google.gson.reflect.TypeToken;
import net.cosmosmc.carpetaddons.logging.Loggers;
import net.cosmosmc.carpetaddons.settings.CosmosCarpetAddonsSettings;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

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

    @Override
    public void onGameStarted() {
        CarpetServer.settingsManager.parseSettingsClass(CosmosCarpetAddonsSettings.class);
        CarpetServer.settingsManager.registerRuleObserver((source, rule, value) -> {
            if (source != null && rule.name().equals("fullrangecfinditem")) {
                var playerList = source.getServer().getPlayerList();
                playerList.getPlayers().forEach(playerList::sendPlayerPermissionLevel);
            }
        });
    }

    // https://github.com/gnembon/fabric-carpet/wiki/Migrating-to-the-new-settings-system#register-your-language-file
    @Override
    public Map<String, String> canHasTranslations(String lang) {
        InputStream langFile = CosmosCarpetAddonsServer.class.getClassLoader().getResourceAsStream("assets/%s/lang/%s.json".formatted(CosmosCarpetAddons.MOD_ID, lang));
        if (langFile == null) {
            // we don't have that language
            return Collections.emptyMap();
        }
        String jsonData;
        try {
            jsonData = IOUtils.toString(langFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return Collections.emptyMap();
        }
        Gson gson = new GsonBuilder().setStrictness(Strictness.LENIENT).create(); // lenient allows for comments
        return gson.fromJson(jsonData, new TypeToken<Map<String, String>>() {}.getType());
    }
}
