package net.cosmosmc.carpetaddons.client.mixin.axiom;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.moulberry.axiom.buildertools.CloneBuilderTool;
import com.moulberry.axiom.clipboard.SelectionBuffer;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.utils.IntMatrix;
import com.moulberry.axiom.world_modification.CompressedBlockEntity;
import com.moulberry.axiom.world_modification.undo.AdditionalUndoOperation;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.cosmosmc.carpetaddons.client.axiom.AxiomLightCapture;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The clone tool's region holds positions relative to the selection center, transformed by {@code transformMatrix}.
 */
@Mixin(value = CloneBuilderTool.class, remap = false)
public abstract class CloneBuilderToolMixin {
    @Shadow
    @Final
    private IntMatrix transformMatrix;

    @Shadow
    private SelectionBuffer selectionBuffer;

    @WrapOperation(
            method = "apply",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/moulberry/axiom/utils/RegionHelper;pushBlockRegionChangeOffset(Lcom/moulberry/axiom/render/regions/ChunkedBlockRegion;Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;Lnet/minecraft/core/BlockPos;Ljava/lang/String;ILcom/moulberry/axiom/world_modification/undo/AdditionalUndoOperation;)V"
            )
    )
    private void cca$captureSourceLight(ChunkedBlockRegion region, Long2ObjectMap<CompressedBlockEntity> blockEntities, BlockPos offset,
                                        String description, int modifiers, AdditionalUndoOperation undo, Operation<Void> original) {
        if (selectionBuffer != null) {
            AxiomLightCapture.captureTransformed(region, offset, transformMatrix, selectionBuffer.center());
        }
        original.call(region, blockEntities, offset, description, modifiers, undo);
    }
}
