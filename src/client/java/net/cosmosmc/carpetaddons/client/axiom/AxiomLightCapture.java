package net.cosmosmc.carpetaddons.client.axiom;

import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.utils.IntMatrix;
import it.unimi.dsi.fastutil.longs.Long2LongFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.cosmosmc.carpetaddons.lightmanipulator.LightManipulatorState;
import net.cosmosmc.carpetaddons.lightmanipulator.network.SectionLight;
import net.cosmosmc.carpetaddons.lightmanipulator.network.SourceLightPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Copies the light levels of an Axiom builder tool's source positions to its destination positions.
 * <p>
 * The light is read from the client's world right before Axiom sends its block buffer and is sent to the server
 * first, which writes it once Axiom applied the matching sections. Only active while the server's light level
 * manipulator is enabled.
 */
public final class AxiomLightCapture {
    private AxiomLightCapture() {
    }

    @FunctionalInterface
    private interface SourceMapper {
        /** @return the source position of a region entry, as {@link BlockPos#asLong} */
        long source(int x, int y, int z);
    }

    private static boolean enabled() {
        return LightManipulatorState.clientActive && ClientPlayNetworking.canSend(SourceLightPayload.TYPE);
    }

    /**
     * For tools whose region holds destination positions and that provide a destination -> source function
     * (stack, extrude).
     */
    public static void captureMapped(ChunkedBlockRegion region, Long2LongFunction destinationToSource) {
        if (destinationToSource == null || !enabled()) {
            return;
        }
        capture(region, 0, 0, 0, (x, y, z) -> destinationToSource.get(BlockPos.asLong(x, y, z)));
    }

    /**
     * For tools whose region holds positions relative to the source selection's {@code center}, transformed by
     * {@code transform}, and placed at {@code offset} (clone, move).
     */
    public static void captureTransformed(ChunkedBlockRegion region, BlockPos offset, IntMatrix transform, BlockPos center) {
        if (center == null || !enabled()) {
            return;
        }
        IntMatrix inverse = transform.copy();
        inverse.invert();
        capture(region, offset.getX(), offset.getY(), offset.getZ(), (x, y, z) -> BlockPos.asLong(
                center.getX() + inverse.transformX(x, y, z),
                center.getY() + inverse.transformY(x, y, z),
                center.getZ() + inverse.transformZ(x, y, z)
        ));
    }

    private static void capture(ChunkedBlockRegion region, int offsetX, int offsetY, int offsetZ, SourceMapper mapper) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || region.isEmpty()) {
            return;
        }
        boolean hasSky = level.dimensionType().hasSkyLight();
        Long2ObjectMap<SectionLight> sections = new Long2ObjectLinkedOpenHashMap<>();
        BlockPos.MutableBlockPos source = new BlockPos.MutableBlockPos();

        region.forEachEntry((x, y, z, state) -> {
            source.set(mapper.source(x, y, z));
            int destX = x + offsetX, destY = y + offsetY, destZ = z + offsetZ;
            long sectionPos = SectionPos.asLong(SectionPos.blockToSectionCoord(destX), SectionPos.blockToSectionCoord(destY), SectionPos.blockToSectionCoord(destZ));
            sections.computeIfAbsent(sectionPos, pos -> SectionLight.create(pos, hasSky))
                    .set(destX, destY, destZ, level.getBrightness(LightLayer.BLOCK, source), hasSky ? level.getBrightness(LightLayer.SKY, source) : 0);
        });

        List<SectionLight> batch = new ArrayList<>(SourceLightPayload.MAX_SECTIONS_PER_PAYLOAD);
        for (SectionLight section : sections.values()) {
            batch.add(section);
            if (batch.size() == SourceLightPayload.MAX_SECTIONS_PER_PAYLOAD) {
                ClientPlayNetworking.send(new SourceLightPayload(level.dimension(), batch));
                batch = new ArrayList<>(SourceLightPayload.MAX_SECTIONS_PER_PAYLOAD);
            }
        }
        if (!batch.isEmpty()) {
            ClientPlayNetworking.send(new SourceLightPayload(level.dimension(), batch));
        }
    }
}
