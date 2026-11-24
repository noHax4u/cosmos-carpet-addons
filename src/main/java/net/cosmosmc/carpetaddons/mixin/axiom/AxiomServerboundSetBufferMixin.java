package net.cosmosmc.carpetaddons.mixin.axiom;

import com.moulberry.axiom.packets.AxiomServerboundSetBuffer;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.world_modification.BlockBuffer;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.cosmosmc.carpetaddons.lightmanipulator.network.LightNetworking;
import net.cosmosmc.carpetaddons.settings.CosmosCarpetAddonsSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Once Axiom applied a block buffer, write the source light the player's client sent for those sections.
 * (Axiom's own relighting is suppressed by LevelLightEngineMixin while the manipulator is active.)
 */
@Mixin(value = AxiomServerboundSetBuffer.class, remap = false)
public abstract class AxiomServerboundSetBufferMixin {
    @Inject(method = "applyBlockBufferServer", at = @At("RETURN"))
    private static void cca$applySourceLight(BlockBuffer buffer, ServerLevel level, ChunkedBooleanRegion selection, ServerPlayer source, CallbackInfo ci) {
        if (source == null || !CosmosCarpetAddonsSettings.lightLevelManipulator) {
            return;
        }
        // Axiom keys its buffer sections with BlockPos.asLong(sectionX, sectionY, sectionZ)
        LongIterator iterator = buffer.keySet().iterator();
        while (iterator.hasNext()) {
            long key = iterator.nextLong();
            long sectionPos = SectionPos.asLong(BlockPos.getX(key), BlockPos.getY(key), BlockPos.getZ(key));
            LightNetworking.consumeSourceLight(level, source, sectionPos);
        }
    }
}
