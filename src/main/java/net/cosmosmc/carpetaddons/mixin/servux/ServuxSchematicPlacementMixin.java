package net.cosmosmc.carpetaddons.mixin.servux;

import fi.dy.masa.servux.schematic.LitematicaSchematic;
import fi.dy.masa.servux.schematic.placement.SchematicPlacement;
import fi.dy.masa.servux.util.data.tag.CompoundData;
import net.cosmosmc.carpetaddons.lightmanipulator.schematic.ServuxLightHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Remembers whether the client asked to paste the schematic's light levels. Clients without this mod don't send
 * the flag, so their pastes never change light.
 */
@Mixin(value = SchematicPlacement.class, remap = false)
public abstract class ServuxSchematicPlacementMixin implements ServuxLightHolder.Placement {
    @Unique
    private boolean cca$pasteLight;

    @Override
    public boolean cca$pastesLight() {
        return cca$pasteLight;
    }

    @Override
    public void cca$setPastesLight(boolean pasteLight) {
        cca$pasteLight = pasteLight;
    }

    @Unique
    private static void cca$readFlag(SchematicPlacement placement, CompoundData tags) {
        if (placement != null) {
            ((ServuxLightHolder.Placement) placement).cca$setPastesLight(tags.getBooleanOrDefault(ServuxLightHolder.PASTE_LIGHT_KEY, false));
        }
    }

    @Inject(method = "createFromData(Lfi/dy/masa/servux/util/data/tag/CompoundData;)Lfi/dy/masa/servux/schematic/placement/SchematicPlacement;", at = @At("RETURN"))
    private static void cca$readPasteLight(CompoundData tags, CallbackInfoReturnable<SchematicPlacement> cir) {
        cca$readFlag(cir.getReturnValue(), tags);
    }

    @Inject(method = "createFromData(Lfi/dy/masa/servux/schematic/LitematicaSchematic;Lfi/dy/masa/servux/util/data/tag/CompoundData;)Lfi/dy/masa/servux/schematic/placement/SchematicPlacement;", at = @At("RETURN"))
    private static void cca$readPasteLight(LitematicaSchematic schematic, CompoundData tags, CallbackInfoReturnable<SchematicPlacement> cir) {
        cca$readFlag(cir.getReturnValue(), tags);
    }
}
