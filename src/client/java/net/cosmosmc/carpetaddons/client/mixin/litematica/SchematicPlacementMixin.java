package net.cosmosmc.carpetaddons.client.mixin.litematica;

import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import net.cosmosmc.carpetaddons.client.litematica.LitematicaLightConfigs;
import net.cosmosmc.carpetaddons.lightmanipulator.schematic.ServuxLightHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tells Servux whether to paste the schematic's light levels, through the placement data Litematica sends it.
 * Servux ignores the extra key unless this mod is installed on the server.
 */
@Mixin(value = SchematicPlacement.class, remap = false)
public abstract class SchematicPlacementMixin {
    

    @Inject(method = "toData", at = @At("RETURN"))
    private void cca$addPasteLightFlag(boolean withSchematic, CallbackInfoReturnable<CompoundData> cir) {
        cir.getReturnValue().putBoolean(ServuxLightHolder.PASTE_LIGHT_KEY, LitematicaLightConfigs.PASTE_INCLUDE_LIGHT.getBooleanValue());
    }
}
