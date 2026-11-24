package net.cosmosmc.carpetaddons.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.cosmosmc.carpetaddons.client.config.CcaConfigScreen;

/** Mod Menu entry point (only loaded when Mod Menu is installed): opens the client config screen. */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return CcaConfigScreen::new;
    }
}
