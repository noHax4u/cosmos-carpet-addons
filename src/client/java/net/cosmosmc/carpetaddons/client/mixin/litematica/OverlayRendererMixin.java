package net.cosmosmc.carpetaddons.client.mixin.litematica;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.malilib.util.data.Color4f;
import net.cosmosmc.carpetaddons.client.litematica.LightMarkers;
import net.cosmosmc.carpetaddons.client.litematica.LitematicaLightConfigs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Litematica takes each marker's color from its mismatch type ({@code entry.type().getColor()}). Light markers
 * have no type of their own, so remember which entry is being drawn and use the configured color for light markers.
 */
@Mixin(value = OverlayRenderer.class, remap = false)
public abstract class OverlayRendererMixin {
    @Unique
    private SchematicVerifier.MismatchRenderPos cca$currentEntry;

    @WrapOperation(
            method = "renderSchematicMismatches",
            at = @At(value = "INVOKE", target = "Lfi/dy/masa/litematica/schematic/verifier/SchematicVerifier$MismatchRenderPos;type()Lfi/dy/masa/litematica/schematic/verifier/SchematicVerifier$MismatchType;")
    )
    private SchematicVerifier.MismatchType cca$rememberEntry(SchematicVerifier.MismatchRenderPos entry, Operation<SchematicVerifier.MismatchType> original) {
        cca$currentEntry = entry;
        return original.call(entry);
    }

    @WrapOperation(
            method = "renderSchematicMismatches",
            at = @At(value = "INVOKE", target = "Lfi/dy/masa/litematica/schematic/verifier/SchematicVerifier$MismatchType;getColor()Lfi/dy/masa/malilib/util/data/Color4f;")
    )
    private Color4f cca$lightMarkerColor(SchematicVerifier.MismatchType type, Operation<Color4f> original) {
        if (cca$currentEntry != null && LightMarkers.isLightMarker(cca$currentEntry)) {
            return LitematicaLightConfigs.LIGHT_OVERLAY_COLOR.getColor();
        }
        return original.call(type);
    }
}
