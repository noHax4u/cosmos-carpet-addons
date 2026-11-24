package net.cosmosmc.carpetaddons.mixin.servux;

import fi.dy.masa.servux.schematic.LitematicaSchematic;
import fi.dy.masa.servux.util.data.tag.CompoundData;
import fi.dy.masa.servux.util.data.tag.util.DataTypeUtils;
import net.cosmosmc.carpetaddons.lightmanipulator.schematic.SchematicRegionLight;
import net.cosmosmc.carpetaddons.lightmanipulator.schematic.ServuxLightHolder;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

/** Reads the light data ({@link SchematicRegionLight}) of schematics sent to Servux for pasting. */
@Mixin(value = LitematicaSchematic.class, remap = false)
public abstract class ServuxLitematicaSchematicMixin implements ServuxLightHolder.Schematic {
    @Unique
    private final Map<String, SchematicRegionLight> cca$light = new HashMap<>();

    @Override
    public @Nullable SchematicRegionLight cca$getRegionLight(String regionName) {
        return cca$light.get(regionName);
    }

    @Inject(method = "readSubRegionsFromData", at = @At("HEAD"))
    private void cca$readLight(CompoundData regions, int version, int minecraftDataVersion, boolean enableFixers, CallbackInfo ci) {
        cca$light.clear();
        for (String regionName : regions.getKeys()) {
            if (!regions.contains(regionName, 10)) {
                continue;
            }
            CompoundData regionTag = regions.getCompound(regionName);
            BlockPos size = DataTypeUtils.readBlockPos(regionTag.getCompound("Size"));
            if (size == null || !regionTag.contains(SchematicRegionLight.TAG, 10)) {
                continue;
            }
            CompoundData tag = regionTag.getCompound(SchematicRegionLight.TAG);
            byte[] sky = tag.contains("Sky", 7) ? tag.getByteArray("Sky") : null;
            SchematicRegionLight light = SchematicRegionLight.fromArrays(Math.abs(size.getX()), Math.abs(size.getY()), Math.abs(size.getZ()),
                    tag.getByteArray("Block"), sky);
            if (light != null) {
                cca$light.put(regionName, light);
            }
        }
    }
}
