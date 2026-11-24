package net.cosmosmc.carpetaddons.client.mixin.litematica;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiConfigs;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;
import net.cosmosmc.carpetaddons.client.litematica.LitematicaLightConfigs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/** Shows the light level options at the end of Litematica's Generic, Info Overlays, Colors and All tabs. */
@Mixin(value = GuiConfigs.class, remap = false)
public abstract class GuiConfigsMixin {
    @Inject(method = "getConfigs", at = @At("RETURN"), cancellable = true)
    private void cca$addLightConfigs(CallbackInfoReturnable<List<ConfigOptionWrapper>> cir) {
        List<IConfigBase> extra = switch (DataManager.getConfigGuiTab()) {
            case GENERIC -> LitematicaLightConfigs.GENERIC;
            case INFO_OVERLAYS -> LitematicaLightConfigs.INFO_OVERLAYS;
            case COLORS -> LitematicaLightConfigs.COLORS;
            default -> List.of(); // the All tab goes through getAllConfigs
        };
        if (!extra.isEmpty()) {
            List<ConfigOptionWrapper> list = new ArrayList<>(cir.getReturnValue());
            list.addAll(ConfigOptionWrapper.createFor(extra));
            cir.setReturnValue(list);
        }
    }

    @Inject(method = "getAllConfigs", at = @At("RETURN"), cancellable = true)
    private void cca$addAllLightConfigs(CallbackInfoReturnable<List<ConfigOptionWrapper>> cir) {
        List<ConfigOptionWrapper> list = new ArrayList<>(cir.getReturnValue());
        list.addAll(ConfigOptionWrapper.createFor(LitematicaLightConfigs.GENERIC));
        list.addAll(ConfigOptionWrapper.createFor(LitematicaLightConfigs.INFO_OVERLAYS));
        list.addAll(ConfigOptionWrapper.createFor(LitematicaLightConfigs.COLORS));
        cir.setReturnValue(list);
    }
}
