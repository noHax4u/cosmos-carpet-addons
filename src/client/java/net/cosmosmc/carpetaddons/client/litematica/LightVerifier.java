package net.cosmosmc.carpetaddons.client.litematica;

import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.util.position.IntBoundingBox;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Light level part of a Litematica schematic verifier: compares the light levels stored in the schematic
 * ({@link RegionLight}) with the client's world.
 */
public final class LightVerifier {
    public record LightMismatch(BlockPos pos, int expectedBlock, int expectedSky, int foundBlock, int foundSky) {
        public String describe() {
            String sky = expectedSky >= 0 ? ", sky %d (found %d)".formatted(expectedSky, foundSky) : "";
            return "%d %d %d: block %d (found %d)%s".formatted(pos.getX(), pos.getY(), pos.getZ(), expectedBlock, foundBlock, sky);
        }
    }

    private static final int RECHECK_INTERVAL_TICKS = 20;

    private final Long2ObjectMap<LightMismatch> mismatches = new Long2ObjectOpenHashMap<>();
    private List<BlockPos> closest = List.of();
    private int checkedPositions = 0;
    private boolean schematicHasLight = false;
    private int ticks = 0;

    public void clear() {
        mismatches.clear();
        closest = List.of();
        checkedPositions = 0;
        schematicHasLight = false;
    }

    public boolean schematicHasLight() {
        return schematicHasLight;
    }

    public int mismatchCount() {
        return mismatches.size();
    }

    public int checkedPositions() {
        return checkedPositions;
    }

    public List<BlockPos> closestMismatches() {
        return closest;
    }

    public List<LightMismatch> closestMismatchDetails(int max) {
        List<LightMismatch> list = new ArrayList<>();
        for (BlockPos pos : closest) {
            LightMismatch mismatch = mismatches.get(pos.asLong());
            if (mismatch != null) {
                list.add(mismatch);
                if (list.size() >= max) break;
            }
        }
        return list;
    }

    /**
     * Maps world positions back to positions inside one sub-region of a placed schematic, reversing
     * Litematica's {@code SchematicPlacingUtils.placeBlocksWithinChunk}.
     */
    private record RegionMapping(RegionLight light, SchematicPlacement schematicPlacement, SubRegionPlacement placement,
                                 BlockPos regionOrigin, BlockPos containerOffset) {
        static @Nullable RegionMapping of(SchematicPlacement schematicPlacement, String regionName) {
            LitematicaSchematic schematic = schematicPlacement.getSchematic();
            RegionLight light = schematic == null ? null : ((LightSchematic) schematic).cca$getRegionLight(regionName);
            SubRegionPlacement placement = schematicPlacement.getRelativeSubRegionPlacement(regionName);
            Vec3i regionSize = schematic == null ? null : schematic.getAreaSizeAsVec3i(regionName);
            if (light == null || placement == null || !placement.isEnabled() || regionSize == null) {
                return null;
            }
            BlockPos regionPos = placement.getPos();
            BlockPos posEndRel = new BlockPos(PositionUtils.getRelativeEndPositionFromAreaSize(regionSize)).offset(regionPos);
            BlockPos posMinRel = PositionUtils.getMinCorner(regionPos, posEndRel);
            BlockPos regionPosTransformed = PositionUtils.getTransformedBlockPos(regionPos, schematicPlacement.getMirror(), schematicPlacement.getRotation());
            return new RegionMapping(light, schematicPlacement, placement, regionPosTransformed.offset(schematicPlacement.getOrigin()), posMinRel.subtract(regionPos));
        }

        /** @return the position inside the region's light data */
        BlockPos toRegion(BlockPos worldPos) {
            BlockPos rel = worldPos.subtract(regionOrigin);
            rel = PositionUtils.getReverseTransformedBlockPos(rel, placement.getMirror(), placement.getRotation());
            rel = PositionUtils.getReverseTransformedBlockPos(rel, schematicPlacement.getMirror(), schematicPlacement.getRotation());
            return rel.subtract(containerOffset);
        }
    }

    /**
     * Called for every box Litematica verifies (one sub-region within one chunk).
     */
    public void verifyBox(ClientLevel world, SchematicPlacement schematicPlacement, ChunkPos chunkPos, IntBoundingBox box) {
        String regionName = null;
        for (Map.Entry<String, IntBoundingBox> entry : schematicPlacement.getBoxesWithinChunk(chunkPos.x(), chunkPos.z()).entrySet()) {
            if (entry.getValue().equals(box)) {
                regionName = entry.getKey();
                break;
            }
        }
        RegionMapping mapping = regionName == null ? null : RegionMapping.of(schematicPlacement, regionName);
        if (mapping == null) {
            return;
        }
        schematicHasLight = true;
        boolean checkSky = mapping.light().hasSky() && world.dimensionType().hasSkyLight();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int y = box.minY(); y <= box.maxY(); y++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int x = box.minX(); x <= box.maxX(); x++) {
                    pos.set(x, y, z);
                    BlockPos rel = mapping.toRegion(pos);
                    if (!mapping.light().contains(rel.getX(), rel.getY(), rel.getZ())) {
                        continue;
                    }
                    checkedPositions++;
                    int expectedBlock = mapping.light().getBlock(rel.getX(), rel.getY(), rel.getZ());
                    int expectedSky = checkSky ? mapping.light().getSky(rel.getX(), rel.getY(), rel.getZ()) : -1;
                    check(world, pos.immutable(), expectedBlock, expectedSky);
                }
            }
        }
    }

    private void check(ClientLevel world, BlockPos pos, int expectedBlock, int expectedSky) {
        int foundBlock = world.getBrightness(LightLayer.BLOCK, pos);
        int foundSky = expectedSky >= 0 ? world.getBrightness(LightLayer.SKY, pos) : -1;
        if (foundBlock != expectedBlock || foundSky != expectedSky) {
            mismatches.put(pos.asLong(), new LightMismatch(pos, expectedBlock, expectedSky, foundBlock, foundSky));
        } else {
            mismatches.remove(pos.asLong());
        }
    }

    /**
     * Re-checks the known mismatches against the placement's current position, rotation and mirror: the placement
     * may have been moved since they were found. Positions it no longer covers are dropped.
     */
    private void recheck(ClientLevel world, SchematicPlacement schematicPlacement) {
        List<RegionMapping> mappings = new ArrayList<>();
        LitematicaSchematic schematic = schematicPlacement.getSchematic();
        if (schematic != null) {
            for (String regionName : schematic.getAreaSizes().keySet()) {
                RegionMapping mapping = RegionMapping.of(schematicPlacement, regionName);
                if (mapping != null) {
                    mappings.add(mapping);
                }
            }
        }
        boolean checkSky = world.dimensionType().hasSkyLight();

        for (LightMismatch mismatch : new ArrayList<>(mismatches.values())) {
            BlockPos pos = mismatch.pos();
            RegionMapping covering = null;
            BlockPos rel = null;
            for (RegionMapping mapping : mappings) {
                rel = mapping.toRegion(pos);
                if (mapping.light().contains(rel.getX(), rel.getY(), rel.getZ())) {
                    covering = mapping;
                    break;
                }
            }
            if (covering == null) {
                mismatches.remove(pos.asLong());
            } else if (world.hasChunkAt(pos)) {
                int expectedSky = checkSky && covering.light().hasSky() ? covering.light().getSky(rel.getX(), rel.getY(), rel.getZ()) : -1;
                check(world, pos, covering.light().getBlock(rel.getX(), rel.getY(), rel.getZ()), expectedSky);
            }
        }
    }

    /**
     * Called every client tick while the verifier exists: once it has finished, re-checks the known
     * mismatches (so fixed ones disappear) and updates the closest positions to highlight.
     */
    public void tick(ClientLevel world, @Nullable SchematicPlacement schematicPlacement, boolean finished, @Nullable BlockPos center, int maxHighlighted) {
        if (++ticks % RECHECK_INTERVAL_TICKS != 0 || world == null) {
            return;
        }
        if (schematicPlacement == null) {
            mismatches.clear();
        } else if (finished && !mismatches.isEmpty()) {
            recheck(world, schematicPlacement);
        }
        if (center != null) {
            closest = mismatches.values().stream()
                    .map(LightMismatch::pos)
                    .sorted(Comparator.comparingDouble(pos -> pos.distSqr(center)))
                    .limit(maxHighlighted)
                    .toList();
        }
    }
}
