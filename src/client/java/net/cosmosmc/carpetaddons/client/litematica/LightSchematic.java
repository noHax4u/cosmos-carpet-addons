package net.cosmosmc.carpetaddons.client.litematica;

import net.cosmosmc.carpetaddons.lightmanipulator.schematic.SchematicRegionLight;
import org.jspecify.annotations.Nullable;

/**
 * Implemented by Litematica's {@code LitematicaSchematic} through a mixin.
 */
public interface LightSchematic {
    @Nullable SchematicRegionLight cca$getRegionLight(String regionName);

    void cca$putRegionLight(String regionName, SchematicRegionLight light);

    /** Whether light levels are captured when blocks are taken from the world (set by the save screen option). */
    boolean cca$capturesLight();

    void cca$setCapturesLight(boolean capture);
}
