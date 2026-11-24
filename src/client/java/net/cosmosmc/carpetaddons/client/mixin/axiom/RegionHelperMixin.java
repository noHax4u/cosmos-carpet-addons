package net.cosmosmc.carpetaddons.client.mixin.axiom;

import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.utils.RegionHelper;
import com.moulberry.axiom.world_modification.undo.AdditionalUndoOperation;
import it.unimi.dsi.fastutil.longs.Long2LongFunction;
import net.cosmosmc.carpetaddons.client.axiom.AxiomLightCapture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stack and extrude pass a destination -> source function (for block entity data); use it for light too.
 */
@Mixin(value = RegionHelper.class, remap = false)
public abstract class RegionHelperMixin {
    @Inject(
            method = "pushBlockRegionChangeWithNBT(Lcom/moulberry/axiom/render/regions/ChunkedBlockRegion;Ljava/lang/String;ILit/unimi/dsi/fastutil/longs/Long2LongFunction;Lcom/moulberry/axiom/world_modification/undo/AdditionalUndoOperation;)V",
            at = @At("HEAD")
    )
    private static void cca$captureSourceLight(ChunkedBlockRegion region, String description, int modifiers, Long2LongFunction nbtLocationFunction,
                                               AdditionalUndoOperation undo, CallbackInfo ci) {
        AxiomLightCapture.captureMapped(region, nbtLocationFunction);
    }
}
