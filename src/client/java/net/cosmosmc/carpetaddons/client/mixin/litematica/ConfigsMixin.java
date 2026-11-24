package net.cosmosmc.carpetaddons.client.mixin.litematica;

import fi.dy.masa.litematica.config.Configs;
import net.cosmosmc.carpetaddons.client.litematica.LitematicaLightConfigs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Loads and saves the light level options together with Litematica's config. */
@Mixin(value = Configs.class, remap = false)
public abstract class ConfigsMixin {
    @Inject(method = "loadFromFile", at = @At("TAIL"))
    private static void cca$loadLightConfigs(CallbackInfo ci) {
        LitematicaLightConfigs.load();
    }

    @Inject(method = "saveToFile", at = @At("TAIL"))
    private static void cca$saveLightConfigs(CallbackInfo ci) {
        LitematicaLightConfigs.save();
    }
}
