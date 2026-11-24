package net.cosmosmc.carpetaddons.mixin.servux;

import fi.dy.masa.servux.schematic.LitematicaSchematic;
import fi.dy.masa.servux.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.servux.schematic.placement.SchematicPlacement;
import fi.dy.masa.servux.schematic.placement.SubRegionPlacement;
import fi.dy.masa.servux.util.PasteLayerBehavior;
import fi.dy.masa.servux.util.ReplaceBehavior;
import fi.dy.masa.servux.util.SchematicPlacingUtils;
import fi.dy.masa.servux.util.data.tag.CompoundData;
import fi.dy.masa.servux.util.position.IntBoundingBox;
import fi.dy.masa.servux.util.position.LayerRange;
import fi.dy.masa.servux.util.position.PositionUtils;
import net.cosmosmc.carpetaddons.lightmanipulator.schematic.SchematicLightPaste;
import net.cosmosmc.carpetaddons.lightmanipulator.schematic.SchematicRegionLight;
import net.cosmosmc.carpetaddons.lightmanipulator.schematic.ServuxLightHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
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
 * After Servux pasted a sub-region's blocks within a chunk, write the schematic's light levels there
 * (if the client asked for it and the light level manipulator is active).
 */
@Mixin(value = SchematicPlacingUtils.class, remap = false)
public abstract class ServuxSchematicPlacingUtilsMixin {
    @Inject(method = "placeBlocksWithinChunk", at = @At("RETURN"))
    private static void cca$pasteLight(Level world, ChunkPos chunkPos, String regionName, LitematicaBlockStateContainer container,
                                       Map<BlockPos, CompoundData> blockEntityMap, BlockPos origin, SchematicPlacement schematicPlacement,
                                       SubRegionPlacement placement, Map<BlockPos, ScheduledTick<Block>> scheduledBlockTicks,
                                       Map<BlockPos, ScheduledTick<Fluid>> scheduledFluidTicks, ReplaceBehavior replace,
                                       PasteLayerBehavior layerBehavior, LayerRange layerRange, boolean notifyNeighbors,
                                       CallbackInfoReturnable<Boolean> cir) {
        if (!(world instanceof ServerLevel level) || !cir.getReturnValueZ() || container == null || placement == null || !placement.isEnabled()
                || !((ServuxLightHolder.Placement) schematicPlacement).cca$pastesLight() || !SchematicLightPaste.canPasteLight()) {
            return;
        }
        LitematicaSchematic schematic = schematicPlacement.getSchematic();
        SchematicRegionLight light = schematic == null ? null : ((ServuxLightHolder.Schematic) schematic).cca$getRegionLight(regionName);
        Vec3i regionSize = schematic == null ? null : schematic.getAreaSize(regionName);
        IntBoundingBox box = schematicPlacement.getBoxWithinChunkForRegion(regionName, chunkPos.x(), chunkPos.z());
        if (light == null || regionSize == null || box == null) {
            return;
        }

        // reverse of the position transform in placeBlocksWithinChunk
        BlockPos regionPos = placement.getPos();
        BlockPos posEndRel = new BlockPos(PositionUtils.getRelativeEndPositionFromAreaSize(regionSize)).offset(regionPos);
        BlockPos containerOffset = PositionUtils.getMinCorner(regionPos, posEndRel).subtract(regionPos);
        BlockPos regionOrigin = PositionUtils.getTransformedBlockPos(regionPos, schematicPlacement.getMirror(), schematicPlacement.getRotation()).offset(origin);

        SchematicLightPaste.paste(level, box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ(), light,
                pos -> {
                    BlockPos rel = pos.subtract(regionOrigin);
                    rel = PositionUtils.getReverseTransformedBlockPos(rel, placement.getMirror(), placement.getRotation());
                    rel = PositionUtils.getReverseTransformedBlockPos(rel, schematicPlacement.getMirror(), schematicPlacement.getRotation());
                    return rel.subtract(containerOffset);
                },
                (pos, rel) -> {
                    BlockState expected = container.get(rel.getX(), rel.getY(), rel.getZ());
                    return expected.getBlock() != Blocks.STRUCTURE_VOID
                            && SchematicPlacingUtils.shouldPasteBlock(pos, layerBehavior, layerRange)
                            && level.getBlockState(pos).is(expected.getBlock());
                });
    }
}
