package net.cosmosmc.carpetaddons.mixin;

import net.cosmosmc.carpetaddons.CosmosCarpetAddons;
import net.cosmosmc.carpetaddons.logging.TNTCart.TNTCartLogger;
import net.cosmosmc.carpetaddons.logging.TNTCart.TNTCartPrimeLogger;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static java.lang.Math.sqrt;

@Mixin(MinecartTNT.class)
public abstract class MinecartTNTDebugger {
    private int tick_count = 0;
    private boolean didExplode = false;
    private boolean didDrop    = false;

    public String _id() {
        return String.format("@%04X", this.hashCode() & 0xffff);
    }
    private MinecartTNT self() {
        return (MinecartTNT) (Object)this;
    }

    @Inject(
        method="<init>",
        at = @At("TAIL")
    )
    private void init(EntityType type, Level level, CallbackInfo ci) {
        TNTCartLogger.log("§3{}§r <init>", _id());
        self().setCustomName(Component.literal(_id()));
        self().setCustomNameVisible(true);
    }

    @Inject(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target= "Lnet/minecraft/world/entity/vehicle/minecart/MinecartTNT;explode(Lnet/minecraft/world/damagesource/DamageSource;D)V"
        )
    )
    void beforeFuseExplosion(CallbackInfo ci) {
        TNTCartLogger.log("§3{}§r explosion!", _id());
        didExplode = true;
    }

    @Inject(
        method="causeFallDamage",
        at = @At(
            value="INVOKE",
            target= "Lnet/minecraft/world/entity/vehicle/minecart/MinecartTNT;explode(Lnet/minecraft/world/damagesource/DamageSource;D)V"
        )
    )
    void beforeExplodeFromFalldamage(double d, float f, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        TNTCartLogger.log("*§3{}§r fell from a high place*", _id());
    }

    @Inject(method="tick", at = @At("HEAD"))
    void onTick(CallbackInfo ci) {
        TNTCartLogger.log("§3{}§r begin tick #{}", _id(), ++tick_count);
    }

    @Inject(method="getDropItem", at = @At("HEAD"))
    void dropAsItem(CallbackInfoReturnable<Item> cir) {
        TNTCartLogger.log("§3{}§r dropping", _id());
        didDrop = true;

        // no more renamed items
        self().setCustomName(null);
    }

    @Inject(method="destroy", at = @At("HEAD"))
    void startDestroy(ServerLevel serverLevel, DamageSource damageSource, CallbackInfo ci) {
        double d = self().getDeltaMovement().horizontalDistanceSqr();
        String action = d >= 0.01 ? "explode" : "drop";
        TNTCartLogger.log("d² = §6{}§r (§6{}§r) -> {}", d, sqrt(d), action);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    void afterTick(CallbackInfo ci) {
        if (!self().isRemoved())
            return;

        if (didExplode && didDrop) {
            TNTCartLogger.log("§a{} SUCCESS", _id());
            TNTCartLogger.playSound(self(), SoundEvents.NOTE_BLOCK_BELL.value());
        }
        else {
            TNTCartLogger.log("§c{} FAILED", _id());
            TNTCartLogger.playSound(self(), SoundEvents.NOTE_BLOCK_TRUMPET_OXIDIZED.value());
        }
    }

    @Inject(method="hurtServer", at = @At("HEAD"))
    void onHurtServer(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        Entity entity = source.getDirectEntity();
        if (entity != null && MinecartTNT.damageSourceIgnitesTnt(source)) {
            TNTCartPrimeLogger.log(_id(), entity);
        }
    }
}
