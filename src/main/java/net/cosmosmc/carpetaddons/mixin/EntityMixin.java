package net.cosmosmc.carpetaddons.mixin;

import net.cosmosmc.carpetaddons.CosUtils;
import net.cosmosmc.carpetaddons.CosmosCarpetAddons;
import net.cosmosmc.carpetaddons.settings.CosmosCarpetAddonsSettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Entity.class)
abstract public class EntityMixin {
    @Unique
    private int dirty = 0;

    @Shadow
    private Level level;

    @Inject(method = "tick", at = @At("TAIL"))
    void afterTick(CallbackInfo ci) {
        if (dirty > 0 && this.level instanceof ServerLevel serverLevel) {
            CosmosCarpetAddons.LOGGER.info("updaete {}", this);
            CosUtils.forceUpdateEntity(serverLevel, (Entity) (Object) this);
            --dirty;
        }
    }

    @Inject(method = "move", at = @At("TAIL"))
    void afterMove(MoverType moverType, Vec3 delta, CallbackInfo ci) {
        if (!(this.level instanceof ServerLevel))
            return;

        ServerLevel serverLevel = (ServerLevel) this.level;

        if (moverType != MoverType.PISTON)
            return;

        if ((Entity) (Object) this instanceof Projectile) {
            dirty = Math.max (CosmosCarpetAddonsSettings.ghostProjectileFix,
                    CosmosCarpetAddonsSettings.ghostEntitiesFix);
        }
        else
            dirty = CosmosCarpetAddonsSettings.ghostEntitiesFix;

        if (dirty > 0) {
            CosmosCarpetAddons.LOGGER.info("updaete {}", this);
            CosUtils.forceUpdateEntity(serverLevel, (Entity) (Object) this);
        }
    }
}
