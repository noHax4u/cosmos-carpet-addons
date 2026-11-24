package net.cosmosmc.carpetaddons.client.mixin.litematica;

import com.google.common.collect.ImmutableMap;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.util.DataTypeUtils;
import fi.dy.masa.malilib.util.position.IntBoundingBox;
import net.cosmosmc.carpetaddons.client.litematica.LightSchematic;
import net.cosmosmc.carpetaddons.lightmanipulator.schematic.SchematicRegionLight;
import net.cosmosmc.carpetaddons.client.litematica.LightNbt;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

/**
 * Captures, saves and loads per-region light levels ({@link SchematicRegionLight}) for Litematica schematics.
 */
@Mixin(value = LitematicaSchematic.class, remap = false)
public abstract class LitematicaSchematicMixin implements LightSchematic {
    @Unique
    private final Map<String, SchematicRegionLight> cca$light = new HashMap<>();
    @Unique
    private boolean cca$captureLight;

    @Override
    public @Nullable SchematicRegionLight cca$getRegionLight(String regionName) {
        return cca$light.get(regionName);
    }

    @Override
    public void cca$putRegionLight(String regionName, SchematicRegionLight light) {
        cca$light.put(regionName, light);
    }

    @Override
    public boolean cca$capturesLight() {
        return cca$captureLight;
    }

    @Override
    public void cca$setCapturesLight(boolean capture) {
        cca$captureLight = capture;
    }

    /** The chunk-wise save task: copy the light levels of the volumes Litematica just took the blocks from. */
    @Inject(method = "takeBlocksFromWorldWithinChunk", at = @At("TAIL"))
    private void cca$takeLightFromWorld(Level world, ImmutableMap<String, IntBoundingBox> volumes, ImmutableMap<String, Box> boxes,
                                        LitematicaSchematic.SchematicSaveInfo info, CallbackInfo ci) {
        if (!cca$captureLight || info.fromSchematicWorld()) {
            return;
        }
        boolean hasSky = world.dimensionType().hasSkyLight();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        volumes.forEach((regionName, bb) -> {
            Box box = boxes.get(regionName);
            if (box == null || box.getPos1() == null || box.getPos2() == null) {
                return;
            }
            BlockPos min = PositionUtils.getMinCorner(box.getPos1(), box.getPos2());
            BlockPos size = box.getSize();
            SchematicRegionLight light = cca$light.computeIfAbsent(regionName,
                    name -> new SchematicRegionLight(Math.abs(size.getX()), Math.abs(size.getY()), Math.abs(size.getZ()), hasSky));

            for (int y = bb.minY(); y <= bb.maxY(); y++) {
                for (int z = bb.minZ(); z <= bb.maxZ(); z++) {
                    for (int x = bb.minX(); x <= bb.maxX(); x++) {
                        int rx = x - min.getX(), ry = y - min.getY(), rz = z - min.getZ();
                        if (light.contains(rx, ry, rz)) {
                            pos.set(x, y, z);
                            light.set(rx, ry, rz, world.getBrightness(LightLayer.BLOCK, pos), hasSky ? world.getBrightness(LightLayer.SKY, pos) : 0);
                        }
                    }
                }
            }
        });
    }

    @Inject(method = "writeSubRegionsToData", at = @At("RETURN"))
    private void cca$writeLight(CallbackInfoReturnable<CompoundData> cir) {
        CompoundData regions = cir.getReturnValue();
        cca$light.forEach((regionName, light) -> {
            if (regions.contains(regionName, 10)) {
                // getCompound returns the stored compound, so this adds the light data to the written region
                regions.getCompound(regionName).put(SchematicRegionLight.TAG, LightNbt.write(light));
            }
        });
    }

    @Inject(method = "readSubRegionsFromData", at = @At("HEAD"))
    private void cca$readLight(CompoundData regions, int version, int minecraftDataVersion, CallbackInfo ci) {
        cca$light.clear();
        for (String regionName : regions.getKeys()) {
            if (!regions.contains(regionName, 10)) {
                continue;
            }
            CompoundData regionTag = regions.getCompound(regionName);
            BlockPos size = DataTypeUtils.readBlockPos(regionTag.getCompound("Size"));
            if (size == null) {
                continue;
            }
            SchematicRegionLight light = LightNbt.read(regionTag, Math.abs(size.getX()), Math.abs(size.getY()), Math.abs(size.getZ()));
            if (light != null) {
                cca$light.put(regionName, light);
            }
        }
    }
}
