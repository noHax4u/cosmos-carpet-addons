package net.cosmosmc.carpetaddons.client.mixin.litematica;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.selection.AreaSelection;
import net.cosmosmc.carpetaddons.client.litematica.LightSchematic;
import net.cosmosmc.carpetaddons.client.litematica.LitematicaLightOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Marks schematics created from a selection to also capture light levels when the checkbox is set.
 */
@Mixin(targets = "fi.dy.masa.litematica.gui.GuiSchematicSave$ButtonListener", remap = false)
public abstract class GuiSchematicSaveButtonListenerMixin {
    @WrapOperation(
            method = "actionPerformedWithButton",
            at = @At(
                    value = "INVOKE",
                    target = "Lfi/dy/masa/litematica/schematic/LitematicaSchematic;createEmptySchematic(Lfi/dy/masa/litematica/selection/AreaSelection;Ljava/lang/String;)Lfi/dy/masa/litematica/schematic/LitematicaSchematic;"
            )
    )
    private LitematicaSchematic cca$captureLight(AreaSelection area, String author, Operation<LitematicaSchematic> original) {
        LitematicaSchematic schematic = original.call(area, author);
        if (schematic != null) {
            ((LightSchematic) schematic).cca$setCapturesLight(LitematicaLightOptions.includeLightWhenSaving);
        }
        return schematic;
    }
}
