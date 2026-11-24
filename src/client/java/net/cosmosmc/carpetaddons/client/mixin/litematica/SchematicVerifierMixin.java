package net.cosmosmc.carpetaddons.client.mixin.litematica;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.malilib.util.position.IntBoundingBox;
import net.cosmosmc.carpetaddons.client.litematica.LightVerifier;
import net.cosmosmc.carpetaddons.client.litematica.LightVerifierHolder;
import net.cosmosmc.carpetaddons.client.litematica.LitematicaLightOptions;
import net.minecraft.client.Minecraft;
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
import java.util.List;

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
        if (worldClient != null && schematicPlacement != null) {
            cca$lightVerifier.verifyBox(worldClient, schematicPlacement, chunkClient.getPos(), box);
        }
    }

    @Inject(method = "clearData", at = @At("TAIL"))
    private void cca$clearLight(CallbackInfo ci) {
        cca$lightVerifier.clear();
    }

    @Inject(method = "execute", at = @At("TAIL"))
    private void cca$tickLight(ProfilerFiller profiler, CallbackInfoReturnable<Boolean> cir) {
        var player = Minecraft.getInstance().player;
        cca$lightVerifier.tick(worldClient, isFinished(), player == null ? null : player.blockPosition(),
                Configs.InfoOverlays.VERIFIER_ERROR_HILIGHT_MAX_POSITIONS.getIntegerValue());
    }

    /**
     * Highlights light mismatches with Litematica's own mismatch overlay. There is no light mismatch type,
     * so they use the "different block" color.
     */
    @Inject(method = "getSelectedMismatchPositionsForRender", at = @At("RETURN"), cancellable = true)
    private void cca$renderLightMismatches(CallbackInfoReturnable<List<SchematicVerifier.MismatchRenderPos>> cir) {
        List<BlockPos> lightMismatches = cca$lightVerifier.closestMismatches();
        if (!LitematicaLightOptions.highlightLightMismatches || lightMismatches.isEmpty()) {
            return;
        }
        List<SchematicVerifier.MismatchRenderPos> list = new ArrayList<>(cir.getReturnValue());
        for (BlockPos pos : lightMismatches) {
            list.add(new SchematicVerifier.MismatchRenderPos(SchematicVerifier.MismatchType.DIFF_BLOCK, pos));
        }
        cir.setReturnValue(list);
    }
}
