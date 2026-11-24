package net.cosmosmc.carpetaddons.lightmanipulator.schematic;

import org.jspecify.annotations.Nullable;

/**
 * Implemented by Servux's {@code LitematicaSchematic} (light data per sub-region) and
 * {@code SchematicPlacement} (whether to paste it) through mixins.
 */
public interface ServuxLightHolder {
    interface Schematic {
        @Nullable SchematicRegionLight cca$getRegionLight(String regionName);
    }

    interface Placement {
        boolean cca$pastesLight();

        void cca$setPastesLight(boolean pasteLight);
    }

    /** Key in the placement data Litematica sends to Servux, added by this mod's Litematica integration. */
    String PASTE_LIGHT_KEY = "cca:PasteLight";
}
