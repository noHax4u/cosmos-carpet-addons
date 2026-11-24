package net.cosmosmc.carpetaddons.client.litematica;

public final class LitematicaLightOptions {
    /** State of the "include light levels" checkbox on Litematica's save schematic screen. */
    public static boolean includeLightWhenSaving = false;

    /** Whether light mismatches found by the schematic verifier are highlighted in the world. */
    public static boolean highlightLightMismatches = true;

    private LitematicaLightOptions() {
    }
}
