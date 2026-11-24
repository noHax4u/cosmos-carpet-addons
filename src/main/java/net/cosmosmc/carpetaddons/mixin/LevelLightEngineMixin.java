package net.cosmosmc.carpetaddons.mixin;

import net.cosmosmc.carpetaddons.lightmanipulator.LightManipulatorState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelLightEngine.class)
public abstract class LevelLightEngineMixin {
    /**
     * Server: also catches mods that bypass {@code ThreadedLevelLightEngine.checkBlock} and call
     * {@code LevelLightEngine.checkBlock} on the light thread directly (Axiom does this for its relighting).
     * Client: stops relighting block changes locally while the server's light level manipulator is active.
     */
    @Inject(method = "checkBlock", at = @At("HEAD"), cancellable = true)
    private void cca$ignoreLightUpdates(BlockPos pos, CallbackInfo ci) {
        if (LightManipulatorState.ignoresLightUpdates((LevelLightEngine) (Object) this)) {
            ci.cancel();
        }
    }
}
