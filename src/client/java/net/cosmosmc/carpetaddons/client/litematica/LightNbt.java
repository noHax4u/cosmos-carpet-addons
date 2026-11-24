package net.cosmosmc.carpetaddons.client.litematica;

import fi.dy.masa.malilib.util.data.tag.CompoundData;
import net.cosmosmc.carpetaddons.lightmanipulator.schematic.SchematicRegionLight;
import org.jspecify.annotations.Nullable;

/**
 * Reads and writes {@link SchematicRegionLight} with MaLiLib's NBT classes (used by Litematica).
 */
public final class LightNbt {
    private LightNbt() {
    }

    public static CompoundData write(SchematicRegionLight light) {
        CompoundData tag = new CompoundData();
        tag.putInt("Version", SchematicRegionLight.FORMAT_VERSION);
        tag.putByteArray("Block", light.blockData());
        if (light.skyData() != null) {
            tag.putByteArray("Sky", light.skyData());
        }
        return tag;
    }

    /**
     * @return the light data of a region compound, or null if it has none or it doesn't match the region's size
     */
    public static @Nullable SchematicRegionLight read(CompoundData regionTag, int sizeX, int sizeY, int sizeZ) {
        if (!regionTag.contains(SchematicRegionLight.TAG, 10)) {
            return null;
        }
        CompoundData tag = regionTag.getCompound(SchematicRegionLight.TAG);
        byte[] sky = tag.contains("Sky", 7) ? tag.getByteArray("Sky") : null;
        return SchematicRegionLight.fromArrays(sizeX, sizeY, sizeZ, tag.getByteArray("Block"), sky);
    }
}
