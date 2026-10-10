package net.cosmosmc.carpetaddons.mixin;

import net.cosmosmc.carpetaddons.logging.TNTCart.TNTCartLogger;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(VehicleEntity.class)
public abstract class VehicleEntityDebugger extends Entity {
    public VehicleEntityDebugger(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow
    public abstract float getDamage();

    private static String LogCategory = "Vehicle";

    @Inject(method="hurtServer", at=@At(
        value="INVOKE",
        target="Lnet/minecraft/world/entity/vehicle/VehicleEntity;shouldSourceDestroy(Lnet/minecraft/world/damagesource/DamageSource;)Z",
        shift=At.Shift.BY, by=-5
    ), locals= LocalCapture.CAPTURE_FAILEXCEPTION)
    void maybeDestroy(ServerLevel serverLevel, DamageSource damageSource, float f, CallbackInfoReturnable<Boolean> cir, boolean bl) {
        if (!((VehicleEntity)(Object) this instanceof MinecartTNT))
            return;

        TNTCartLogger.log("damage §d{}§r, destroy? damage > 40 = {}", this.getDamage(), this.getDamage() > 40.0F);
    }
}
