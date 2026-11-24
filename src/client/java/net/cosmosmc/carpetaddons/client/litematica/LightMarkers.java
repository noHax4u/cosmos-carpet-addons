package net.cosmosmc.carpetaddons.client.litematica;

import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * The marker entries added to Litematica's verifier overlay for light mismatches. They reuse an existing
 * mismatch type, so they are recognised by identity to give them their own color.
 */
public final class LightMarkers {
    private static Set<SchematicVerifier.MismatchRenderPos> current = Set.of();
    private static List<SchematicVerifier.MismatchRenderPos> entries = List.of();
    private static List<BlockPos> source = null;

    private LightMarkers() {
    }

    /** @return marker entries for {@code positions}, reused as long as the positions list doesn't change */
    public static List<SchematicVerifier.MismatchRenderPos> entriesFor(List<BlockPos> positions) {
        if (positions != source) {
            List<SchematicVerifier.MismatchRenderPos> list = new ArrayList<>(positions.size());
            Set<SchematicVerifier.MismatchRenderPos> set = Collections.newSetFromMap(new IdentityHashMap<>());
            for (BlockPos pos : positions) {
                var entry = new SchematicVerifier.MismatchRenderPos(SchematicVerifier.MismatchType.DIFF_BLOCK, pos);
                list.add(entry);
                set.add(entry);
            }
            entries = list;
            current = set;
            source = positions;
        }
        return entries;
    }

    public static boolean isLightMarker(SchematicVerifier.MismatchRenderPos entry) {
        return current.contains(entry);
    }
}
