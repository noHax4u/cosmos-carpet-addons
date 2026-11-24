package net.cosmosmc.carpetaddons.mixin;

import net.cosmosmc.carpetaddons.lightmanipulator.LightTaskScheduler;
import net.cosmosmc.carpetaddons.settings.CosmosCarpetAddonsSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThreadedLevelLightEngine.class)
public abstract class ThreadedLevelLightEngineMixin implements LightTaskScheduler {
    @Shadow
    private void addTask(int chunkX, int chunkZ, ThreadedLevelLightEngine.TaskType type, Runnable runnable) {
    }

    /**
     * While the light level manipulator is active, block changes no longer trigger light updates.
     * Chunk generation lighting is not affected.
     */
    @Inject(method = "checkBlock", at = @At("HEAD"), cancellable = true)
    private void cca$ignoreLightUpdates(BlockPos pos, CallbackInfo ci) {
        if (CosmosCarpetAddonsSettings.lightLevelManipulator) {
            ci.cancel();
        }
    }

    @Override
    public void cca$submitLightTask(int chunkX, int chunkZ, Runnable task) {
        addTask(chunkX, chunkZ, ThreadedLevelLightEngine.TaskType.PRE_UPDATE, task);
    }
}
