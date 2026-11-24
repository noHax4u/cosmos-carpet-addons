package net.cosmosmc.carpetaddons.lightmanipulator.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.chunk.DataLayer;

import java.util.BitSet;

/**
 * Light levels for (some of) the positions of one section.
 *
 * @param sectionPos {@link net.minecraft.core.SectionPos#asLong} of the section
 * @param mask       which positions are set, indexed by {@link #index(int, int, int)}
 * @param block      block light, or null to keep it
 * @param sky        sky light, or null to keep it
 */
public record SectionLight(long sectionPos, BitSet mask, DataLayer block, DataLayer sky) {
    public static final int SIZE = 16 * 16 * 16;

    public static SectionLight create(long sectionPos, boolean hasSky) {
        return new SectionLight(sectionPos, new BitSet(SIZE), new DataLayer(), hasSky ? new DataLayer() : null);
    }

    /** Same index order as {@link DataLayer}. */
    public static int index(int x, int y, int z) {
        return (y & 15) << 8 | (z & 15) << 4 | (x & 15);
    }

    public void set(int x, int y, int z, int blockLight, int skyLight) {
        mask.set(index(x, y, z));
        block.set(x & 15, y & 15, z & 15, blockLight);
        if (sky != null) {
            sky.set(x & 15, y & 15, z & 15, skyLight);
        }
    }

    void write(FriendlyByteBuf buf) {
        buf.writeLong(sectionPos);
        long[] words = mask.toLongArray();
        buf.writeVarInt(words.length);
        for (long word : words) {
            buf.writeLong(word);
        }
        buf.writeByteArray(block.getData());
        buf.writeBoolean(sky != null);
        if (sky != null) {
            buf.writeByteArray(sky.getData());
        }
    }

    static SectionLight read(FriendlyByteBuf buf) {
        long sectionPos = buf.readLong();
        int wordCount = buf.readVarInt();
        if (wordCount < 0 || wordCount > SIZE / 64) {
            throw new IllegalArgumentException("Invalid source light mask size: " + wordCount);
        }
        long[] words = new long[wordCount];
        for (int i = 0; i < wordCount; i++) {
            words[i] = buf.readLong();
        }
        DataLayer block = new DataLayer(buf.readByteArray(DataLayer.SIZE));
        DataLayer sky = buf.readBoolean() ? new DataLayer(buf.readByteArray(DataLayer.SIZE)) : null;
        return new SectionLight(sectionPos, BitSet.valueOf(words), block, sky);
    }
}
