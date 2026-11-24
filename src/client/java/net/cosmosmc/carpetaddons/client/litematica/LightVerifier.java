package net.cosmosmc.carpetaddons.client.litematica;

import net.cosmosmc.carpetaddons.lightmanipulator.schematic.SchematicRegionLight;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Light level part of a Litematica schematic verifier: compares the light levels stored in the schematic
 * ({@link SchematicRegionLight}) with the client's world.
 */
public final class LightVerifier {
    public record LightMismatch(BlockPos pos, int expectedBlock, int expectedSky, int foundBlock, int foundSky) {
        public String describe() {
            String sky = expectedSky >= 0 ? ", sky %d (found %d)".formatted(expectedSky, foundSky) : "";
            return "%d %d %d: block %d (found %d)%s".formatted(pos.getX(), pos.getY(), pos.getZ(), expectedBlock, foundBlock, sky);
        }

        /** @return the categories this mismatch belongs to: one per light layer that differs */
        public List<Category> categories() {
            List<Category> list = new ArrayList<>(2);
            if (expectedBlock != foundBlock) list.add(new Category(LightLayer.BLOCK, expectedBlock, foundBlock));
            if (expectedSky >= 0 && expectedSky != foundSky) list.add(new Category(LightLayer.SKY, expectedSky, foundSky));
            return list;
        }
    }

    /** A kind of light mismatch, e.g. "block light: expected 1, found 15". */
    public record Category(LightLayer layer, int expected, int found) {
    }

    /** How often known mismatches are re-checked, in real time (Litematica runs the verifier only every few ticks). */
    private static final long RECHECK_INTERVAL_MS = 1000;
    /** The highlighted positions are re-sorted when the player moved this far (squared distance). */
    private static final double RESORT_DISTANCE_SQ = 4 * 4;

    private final Long2ObjectMap<LightMismatch> mismatches = new Long2ObjectOpenHashMap<>();
    /** Ignored categories, kept when re-verifying (like Litematica's ignored block mismatches). */
    private final Set<Category> ignored = new HashSet<>();
    /** Categories selected for highlighting; empty highlights all (non-ignored) mismatches. */
    private final Set<Category> selected = new HashSet<>();
    private List<BlockPos> closest = List.of();
    private boolean closestDirty = false;
    private @Nullable BlockPos closestCenter = null;
    private int checkedPositions = 0;
    private boolean schematicHasLight = false;
    private long lastRecheck = 0;

    public void clear() {
        mismatches.clear();
        selected.clear();
        closest = List.of();
        closestDirty = false;
        checkedPositions = 0;
        schematicHasLight = false;
    }

    public boolean schematicHasLight() {
        return schematicHasLight;
    }

    public int checkedPositions() {
        return checkedPositions;
    }

    private boolean isActive(LightMismatch mismatch) {
        for (Category category : mismatch.categories()) {
            if (!ignored.contains(category)) return true;
        }
        return false;
    }

    private boolean isHighlighted(LightMismatch mismatch) {
        for (Category category : mismatch.categories()) {
            if (!ignored.contains(category) && (selected.isEmpty() || selected.contains(category))) return true;
        }
        return false;
    }

    /** @return the number of mismatches that are not (fully) ignored */
    public int mismatchCount() {
        int count = 0;
        for (LightMismatch mismatch : mismatches.values()) {
            if (isActive(mismatch)) count++;
        }
        return count;
    }

    /** @return the mismatch at {@code pos} if it is not ignored, or null */
    public @Nullable LightMismatch getMismatch(BlockPos pos) {
        LightMismatch mismatch = mismatches.get(pos.asLong());
        return mismatch != null && isActive(mismatch) ? mismatch : null;
    }

    /** @return the non-ignored categories with their number of positions, most frequent first */
    public List<Map.Entry<Category, Integer>> categoryCounts() {
        Map<Category, Integer> counts = new HashMap<>();
        for (LightMismatch mismatch : mismatches.values()) {
            for (Category category : mismatch.categories()) {
                if (!ignored.contains(category)) counts.merge(category, 1, Integer::sum);
            }
        }
        List<Map.Entry<Category, Integer>> list = new ArrayList<>(counts.entrySet());
        list.sort(Map.Entry.<Category, Integer>comparingByValue().reversed()
                .thenComparing(e -> e.getKey().layer())
                .thenComparingInt(e -> e.getKey().expected())
                .thenComparingInt(e -> e.getKey().found()));
        return list;
    }

    public boolean isSelected(Category category) {
        return selected.contains(category);
    }

    public void toggleSelected(Category category) {
        if (!selected.remove(category)) selected.add(category);
        closestDirty = true;
    }

    public void ignore(Category category) {
        ignored.add(category);
        selected.remove(category);
        closestDirty = true;
    }

    public int ignoredCount() {
        return ignored.size();
    }

    public void resetIgnored() {
        ignored.clear();
        closestDirty = true;
    }

    /**
     * @return the mismatches to highlight (not ignored, and in a selected category if any are selected), closest to the
     * player first. Updated right away when mismatches change (and when the player moved a bit).
     */
    public List<BlockPos> closestMismatches() {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        BlockPos center = player == null ? BlockPos.ZERO : player.blockPosition();
        if (closestDirty || closestCenter == null || closestCenter.distSqr(center) > RESORT_DISTANCE_SQ) {
            closest = mismatches.values().stream()
                    .filter(this::isHighlighted)
                    .map(LightMismatch::pos)
                    .sorted(Comparator.comparingDouble(pos -> pos.distSqr(center)))
                    .limit(LitematicaLightConfigs.LIGHT_OVERLAY_MAX_POSITIONS.getIntegerValue())
                    .toList();
            closestCenter = center;
            closestDirty = false;
        }
        return closest;
    }

    /** @return up to {@code max} non-ignored mismatches, closest to {@code center} first */
    public List<LightMismatch> closestMismatchDetails(@Nullable BlockPos center, int max) {
        return mismatches.values().stream()
                .filter(this::isActive)
                .sorted(Comparator.comparingDouble(m -> center == null ? 0 : m.pos().distSqr(center)))
                .limit(max)
                .toList();
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
        LitematicaRegionMapping mapping = regionName == null ? null : LitematicaRegionMapping.of(schematicPlacement, regionName);
        if (mapping == null) {
            return;
        }
        schematicHasLight = true;
        boolean checkSky = mapping.light().hasSky() && world.dimensionType().hasSkyLight() && LitematicaLightConfigs.VERIFY_SKY_LIGHT.getBooleanValue();
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
            LightMismatch mismatch = new LightMismatch(pos, expectedBlock, expectedSky, foundBlock, foundSky);
            if (!mismatch.equals(mismatches.put(pos.asLong(), mismatch))) {
                closestDirty = true;
            }
        } else if (mismatches.remove(pos.asLong()) != null) {
            closestDirty = true;
        }
    }

    /**
     * Re-checks the known mismatches against the placement's current position, rotation and mirror: the placement
     * may have been moved since they were found. Positions it no longer covers are dropped.
     */
    private void recheck(ClientLevel world, SchematicPlacement schematicPlacement) {
        List<LitematicaRegionMapping> mappings = new ArrayList<>();
        LitematicaSchematic schematic = schematicPlacement.getSchematic();
        if (schematic != null) {
            for (String regionName : schematic.getAreaSizes().keySet()) {
                LitematicaRegionMapping mapping = LitematicaRegionMapping.of(schematicPlacement, regionName);
                if (mapping != null) {
                    mappings.add(mapping);
                }
            }
        }
        boolean checkSky = world.dimensionType().hasSkyLight() && LitematicaLightConfigs.VERIFY_SKY_LIGHT.getBooleanValue();

        for (LightMismatch mismatch : new ArrayList<>(mismatches.values())) {
            BlockPos pos = mismatch.pos();
            LitematicaRegionMapping covering = null;
            BlockPos rel = null;
            for (LitematicaRegionMapping mapping : mappings) {
                rel = mapping.toRegion(pos);
                if (mapping.light().contains(rel.getX(), rel.getY(), rel.getZ())) {
                    covering = mapping;
                    break;
                }
            }
            if (covering == null) {
                mismatches.remove(pos.asLong());
                closestDirty = true;
            } else if (world.hasChunkAt(pos)) {
                int expectedSky = checkSky && covering.light().hasSky() ? covering.light().getSky(rel.getX(), rel.getY(), rel.getZ()) : -1;
                check(world, pos, covering.light().getBlock(rel.getX(), rel.getY(), rel.getZ()), expectedSky);
            }
        }
    }

    /**
     * Called whenever Litematica runs the verifier task: once it has finished, re-checks the known mismatches
     * about once a second, so fixed ones (and ones the moved placement no longer covers) disappear.
     */
    public void tick(@Nullable ClientLevel world, @Nullable SchematicPlacement schematicPlacement, boolean finished) {
        long now = System.currentTimeMillis();
        if (world == null || now - lastRecheck < RECHECK_INTERVAL_MS) {
            return;
        }
        lastRecheck = now;
        if (schematicPlacement == null) {
            if (!mismatches.isEmpty()) {
                mismatches.clear();
                closestDirty = true;
            }
        } else if (finished && !mismatches.isEmpty()) {
            recheck(world, schematicPlacement);
        }
    }
}
