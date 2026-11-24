package net.cosmosmc.carpetaddons.mixin.axiom;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.moulberry.axiom.packets.AxiomServerboundSetBlock;
import net.cosmosmc.carpetaddons.lightmanipulator.network.LightNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Axiom's "no updates" mode places blocks without neighbour updates through {@code setWithoutUpdates}, which still
 * asks the light engine to relight the block. Players who enabled the client option skip that light update too.
 */
@Mixin(value = AxiomServerboundSetBlock.class, remap = false)
public abstract class AxiomServerboundSetBlockMixin {
    @Shadow
    @Final
    private boolean updateNeighbors;
    @Shadow
    @Final
    private int reason;

    /** Set while handling a "no updates" edit of a player who enabled the option (packets are handled on the server thread). */
    @Unique
    private static boolean cca$skipLightUpdates;

    @Inject(method = "handle", at = @At("HEAD"))
    private void cca$beginHandle(MinecraftServer server, ServerPlayer player, CallbackInfo ci) {
        // only Axiom's "no updates" capability, not other edits that also skip neighbour updates (e.g. replace mode)
        cca$skipLightUpdates = !updateNeighbors && (reason & AxiomServerboundSetBlock.REASON_NOUPDATES) != 0
                && LightNetworking.axiomNoUpdatesSkipsLight(player);
    }

    @Inject(method = "handle", at = @At("RETURN"))
    private void cca$endHandle(MinecraftServer server, ServerPlayer player, CallbackInfo ci) {
        cca$skipLightUpdates = false;
    }

    @WrapOperation(
            method = "setWithoutUpdates",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ThreadedLevelLightEngine;checkBlock(Lnet/minecraft/core/BlockPos;)V")
    )
    private static void cca$skipLightUpdate(ThreadedLevelLightEngine engine, BlockPos pos, Operation<Void> original) {
        if (!cca$skipLightUpdates) {
            original.call(engine, pos);
        }
    }
}
