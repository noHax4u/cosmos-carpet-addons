package net.cosmosmc.carpetaddons.client.mixin.litematica;

import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.malilib.util.position.IntBoundingBox;
import net.cosmosmc.carpetaddons.client.litematica.LightVerifier;
import net.cosmosmc.carpetaddons.client.litematica.LightVerifierHolder;
import net.cosmosmc.carpetaddons.client.litematica.LightMarkers;
import net.cosmosmc.carpetaddons.client.litematica.LitematicaLightConfigs;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Verifies light levels alongside the blocks, for schematics that contain light data.
 */
@Mixin(value = SchematicVerifier.class, remap = false)
public abstract class SchematicVerifierMixin implements LightVerifierHolder {
    @Shadow
    private ClientLevel worldClient;
    @Shadow
    private SchematicPlacement schematicPlacement;

    @Shadow
    public abstract boolean isFinished();

    @Unique
    private final LightVerifier cca$lightVerifier = new LightVerifier();

    @Override
    public LightVerifier cca$getLightVerifier() {
        return cca$lightVerifier;
    }

    @Inject(method = "verifyChunk", at = @At("RETURN"))
    private void cca$verifyLight(ChunkAccess chunkClient, ChunkAccess chunkSchematic, IntBoundingBox box, CallbackInfoReturnable<Boolean> cir) {
        if (worldClient != null && schematicPlacement != null && LitematicaLightConfigs.VERIFY_LIGHT.getBooleanValue()) {
            cca$lightVerifier.verifyBox(worldClient, schematicPlacement, chunkClient.getPos(), box);
        }
    }

    @Inject(method = "clearData", at = @At("TAIL"))
    private void cca$clearLight(CallbackInfo ci) {
        cca$lightVerifier.clear();
    }

    @Inject(method = "execute", at = @At("TAIL"))
    private void cca$tickLight(ProfilerFiller profiler, CallbackInfoReturnable<Boolean> cir) {
        cca$lightVerifier.tick(worldClient, schematicPlacement, isFinished());
    }

    /**
     * Highlights light mismatches with Litematica's own mismatch overlay, in the configured color.
     * Positions that already show a block mismatch marker are skipped unless configured otherwise,
     * since overlapping markers render badly.
     */
    @Inject(method = "getSelectedMismatchPositionsForRender", at = @At("RETURN"), cancellable = true)
    private void cca$renderLightMismatches(CallbackInfoReturnable<List<SchematicVerifier.MismatchRenderPos>> cir) {
        List<BlockPos> lightMismatches = cca$lightVerifier.closestMismatches();
        if (!LitematicaLightConfigs.LIGHT_OVERLAY_ENABLED.getBooleanValue() || lightMismatches.isEmpty()) {
            return;
        }
        List<SchematicVerifier.MismatchRenderPos> list = new ArrayList<>(cir.getReturnValue());
        boolean overOthers = LitematicaLightConfigs.LIGHT_OVERLAY_OVER_OTHER_MARKERS.getBooleanValue();
        Set<BlockPos> marked = new HashSet<>();
        if (!overOthers) {
            for (SchematicVerifier.MismatchRenderPos entry : list) {
                marked.add(entry.pos());
            }
        }
        for (SchematicVerifier.MismatchRenderPos entry : LightMarkers.entriesFor(lightMismatches)) {
            if (overOthers || !marked.contains(entry.pos())) {
                list.add(entry);
            }
        }
        cir.setReturnValue(list);
    }
}
