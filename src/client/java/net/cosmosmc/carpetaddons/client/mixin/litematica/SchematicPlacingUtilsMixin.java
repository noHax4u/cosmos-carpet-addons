package net.cosmosmc.carpetaddons.client.mixin.litematica;

import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.util.PasteLayerBehavior;
import fi.dy.masa.litematica.util.ReplaceBehavior;
import fi.dy.masa.litematica.util.SchematicPlacingUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.position.IntBoundingBox;
import net.cosmosmc.carpetaddons.client.litematica.LitematicaLightConfigs;
import net.cosmosmc.carpetaddons.client.litematica.LitematicaRegionMapping;
import net.cosmosmc.carpetaddons.lightmanipulator.schematic.SchematicLightPaste;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.ScheduledTick;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * Singleplayer paste: Litematica places the blocks into the integrated server's world directly.
 * Afterwards, write the schematic's light levels for the pasted region part.
 */
@Mixin(value = SchematicPlacingUtils.class, remap = false)
public abstract class SchematicPlacingUtilsMixin {
    @Inject(method = "placeBlocksWithinChunk", at = @At("RETURN"))
    private static void cca$pasteLight(Level world, ChunkPos chunkPos, String regionName, LitematicaBlockStateContainer container,
                                       Map<BlockPos, CompoundData> blockEntityMap, BlockPos origin, SchematicPlacement schematicPlacement,
                                       SubRegionPlacement placement, Map<BlockPos, ScheduledTick<Block>> scheduledBlockTicks,
                                       Map<BlockPos, ScheduledTick<Fluid>> scheduledFluidTicks, ReplaceBehavior replace,
                                       PasteLayerBehavior layerBehavior, boolean notifyNeighbors, CallbackInfoReturnable<Boolean> cir) {
        // only real pastes into the integrated server's world, not the schematic preview world
        if (!(world instanceof ServerLevel level) || !cir.getReturnValueZ() || container == null
                || !LitematicaLightConfigs.PASTE_INCLUDE_LIGHT.getBooleanValue() || !SchematicLightPaste.canPasteLight()) {
            return;
        }
        LitematicaRegionMapping mapping = LitematicaRegionMapping.of(schematicPlacement, regionName);
        IntBoundingBox box = schematicPlacement.getBoxWithinChunkForRegion(regionName, chunkPos.x(), chunkPos.z());
        if (mapping == null || box == null) {
            return;
        }
        SchematicLightPaste.paste(level, box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ(),
                mapping.light(), mapping::toRegion, (pos, rel) -> {
                    BlockState expected = container.get(rel.getX(), rel.getY(), rel.getZ());
                    return expected.getBlock() != Blocks.STRUCTURE_VOID
                            && SchematicPlacingUtils.shouldPasteBlock(pos, layerBehavior)
                            && level.getBlockState(pos).is(expected.getBlock());
                });
    }
}
