package net.cosmosmc.carpetaddons.client.litematica;

import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.util.PositionUtils;
import net.cosmosmc.carpetaddons.lightmanipulator.schematic.SchematicRegionLight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import org.jspecify.annotations.Nullable;

/**
 * Maps world positions back to positions inside one sub-region of a placed schematic, reversing
 * Litematica's {@code SchematicPlacingUtils.placeBlocksWithinChunk}.
 */
public record LitematicaRegionMapping(SchematicRegionLight light, SchematicPlacement schematicPlacement, SubRegionPlacement placement,
                                      BlockPos regionOrigin, BlockPos containerOffset) {
    /**
     * @return the mapping for an enabled sub-region with light data, or null
     */
    public static @Nullable LitematicaRegionMapping of(SchematicPlacement schematicPlacement, String regionName) {
        LitematicaSchematic schematic = schematicPlacement.getSchematic();
        SchematicRegionLight light = schematic == null ? null : ((LightSchematic) schematic).cca$getRegionLight(regionName);
        SubRegionPlacement placement = schematicPlacement.getRelativeSubRegionPlacement(regionName);
        Vec3i regionSize = schematic == null ? null : schematic.getAreaSizeAsVec3i(regionName);
        if (light == null || placement == null || !placement.isEnabled() || regionSize == null) {
            return null;
        }
        BlockPos regionPos = placement.getPos();
        BlockPos posEndRel = new BlockPos(PositionUtils.getRelativeEndPositionFromAreaSize(regionSize)).offset(regionPos);
        BlockPos posMinRel = PositionUtils.getMinCorner(regionPos, posEndRel);
        BlockPos regionPosTransformed = PositionUtils.getTransformedBlockPos(regionPos, schematicPlacement.getMirror(), schematicPlacement.getRotation());
        return new LitematicaRegionMapping(light, schematicPlacement, placement, regionPosTransformed.offset(schematicPlacement.getOrigin()), posMinRel.subtract(regionPos));
    }

    /** @return the position inside the region (its block state container and light data) */
    public BlockPos toRegion(BlockPos worldPos) {
        BlockPos rel = worldPos.subtract(regionOrigin);
        rel = PositionUtils.getReverseTransformedBlockPos(rel, placement.getMirror(), placement.getRotation());
        rel = PositionUtils.getReverseTransformedBlockPos(rel, schematicPlacement.getMirror(), schematicPlacement.getRotation());
        return rel.subtract(containerOffset);
    }
}
