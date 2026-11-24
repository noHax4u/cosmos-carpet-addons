package net.cosmosmc.carpetaddons.lightmanipulator.schematic;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.cosmosmc.carpetaddons.lightmanipulator.LightManipulator;
import net.cosmosmc.carpetaddons.lightmanipulator.network.SectionLight;
import net.cosmosmc.carpetaddons.settings.CosmosCarpetAddonsSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;

/**
 * Writes a pasted schematic sub-region's stored light levels into the world. Shared by Litematica's direct paste
 * (singleplayer) and Servux's paste (servers).
 * <p>
 * Only works while the light level manipulator is active: otherwise the light updates caused by the pasted blocks
 * would recalculate the light right away.
 */
public final class SchematicLightPaste {
    private SchematicLightPaste() {
    }

    /** Maps a world position back to a position inside the region's light data. */
    @FunctionalInterface
    public interface WorldToRegion {
        BlockPos toRegion(BlockPos worldPos);
    }

    /** Whether the schematic's block for a region position was actually pasted at a world position. */
    @FunctionalInterface
    public interface PastedCheck {
        boolean wasPasted(BlockPos worldPos, BlockPos regionPos);
    }

    public static boolean canPasteLight() {
        return CosmosCarpetAddonsSettings.lightLevelManipulator;
    }

    /**
     * Writes the light of all positions in the given world box that belong to the region and were pasted.
     * Must be called on the server thread after the blocks were placed.
     */
    public static void paste(ServerLevel level, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                             SchematicRegionLight light, WorldToRegion mapping, PastedCheck pasted) {
        if (!canPasteLight()) {
            return;
        }
        boolean hasSky = light.hasSky() && LightManipulator.hasLayer(level, LightLayer.SKY);
        Long2ObjectMap<SectionLight> sections = new Long2ObjectLinkedOpenHashMap<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    pos.set(x, y, z);
                    BlockPos rel = mapping.toRegion(pos);
                    if (!light.contains(rel.getX(), rel.getY(), rel.getZ()) || !pasted.wasPasted(pos, rel)) {
                        continue;
                    }
                    long section = SectionPos.asLong(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(y), SectionPos.blockToSectionCoord(z));
                    sections.computeIfAbsent(section, s -> SectionLight.create(s, hasSky))
                            .set(x, y, z, light.getBlock(rel.getX(), rel.getY(), rel.getZ()), hasSky ? light.getSky(rel.getX(), rel.getY(), rel.getZ()) : 0);
                }
            }
        }

        for (SectionLight section : sections.values()) {
            LightManipulator.applySectionLight(level, section);
        }
    }
}
