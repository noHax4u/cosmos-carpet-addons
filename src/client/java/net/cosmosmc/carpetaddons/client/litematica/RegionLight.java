package net.cosmosmc.carpetaddons.client.litematica;

import fi.dy.masa.malilib.util.data.tag.CompoundData;
import org.jspecify.annotations.Nullable;

/**
 * Block and sky light levels of one schematic sub-region, indexed like Litematica's block state container
 * (x, y, z relative to the region's minimum corner).
 * <p>
 * Stored in the region's compound as {@value #TAG}. Litematica only reads the keys it knows,
 * so unmodified Litematica clients load these schematics normally and just ignore the light data.
 */
public final class RegionLight {
    public static final String TAG = "cca:Light";
    private static final int FORMAT_VERSION = 1;

    private final int sizeX, sizeY, sizeZ;
    private final byte[] block;
    private final byte @Nullable [] sky;

    public RegionLight(int sizeX, int sizeY, int sizeZ, boolean hasSky) {
        this(sizeX, sizeY, sizeZ, new byte[nibbleArraySize(sizeX, sizeY, sizeZ)], hasSky ? new byte[nibbleArraySize(sizeX, sizeY, sizeZ)] : null);
    }

    private RegionLight(int sizeX, int sizeY, int sizeZ, byte[] block, byte @Nullable [] sky) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.block = block;
        this.sky = sky;
    }

    private static int nibbleArraySize(int sizeX, int sizeY, int sizeZ) {
        return (int) (((long) sizeX * sizeY * sizeZ + 1) / 2);
    }

    public boolean contains(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < sizeX && y < sizeY && z < sizeZ;
    }

    public boolean hasSky() {
        return sky != null;
    }

    private int index(int x, int y, int z) {
        return (y * sizeZ + z) * sizeX + x;
    }

    private static int get(byte[] data, int index) {
        return (data[index >> 1] >> ((index & 1) << 2)) & 15;
    }

    private static void set(byte[] data, int index, int value) {
        int shift = (index & 1) << 2;
        data[index >> 1] = (byte) ((data[index >> 1] & ~(15 << shift)) | ((value & 15) << shift));
    }

    public int getBlock(int x, int y, int z) {
        return get(block, index(x, y, z));
    }

    /** @return the sky light, or -1 if this region has no sky light data */
    public int getSky(int x, int y, int z) {
        return sky == null ? -1 : get(sky, index(x, y, z));
    }

    public void set(int x, int y, int z, int blockLight, int skyLight) {
        int index = index(x, y, z);
        set(block, index, blockLight);
        if (sky != null) {
            set(sky, index, skyLight);
        }
    }

    public CompoundData write() {
        CompoundData tag = new CompoundData();
        tag.putInt("Version", FORMAT_VERSION);
        tag.putByteArray("Block", block);
        if (sky != null) {
            tag.putByteArray("Sky", sky);
        }
        return tag;
    }

    /**
     * @return the light data of a region compound, or null if it has none or it doesn't match the region's size
     */
    public static @Nullable RegionLight read(CompoundData regionTag, int sizeX, int sizeY, int sizeZ) {
        if (!regionTag.contains(TAG, 10)) {
            return null;
        }
        CompoundData tag = regionTag.getCompound(TAG);
        int expected = nibbleArraySize(sizeX, sizeY, sizeZ);
        byte[] block = tag.getByteArray("Block");
        byte[] sky = tag.contains("Sky", 7) ? tag.getByteArray("Sky") : null;
        if (block == null || block.length != expected || (sky != null && sky.length != expected)) {
            return null;
        }
        return new RegionLight(sizeX, sizeY, sizeZ, block, sky);
    }
}
