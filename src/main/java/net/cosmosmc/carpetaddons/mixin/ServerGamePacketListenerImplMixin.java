package net.cosmosmc.carpetaddons.mixin;

import net.cosmosmc.carpetaddons.settings.CosmosCarpetAddonsSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundTagQueryPacket;
import net.minecraft.network.protocol.game.ServerboundBlockEntityTagQueryPacket;
import net.minecraft.network.protocol.game.ServerboundEntityTagQueryPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    // Inject after vanilla's thread check, before its operator permission check.
    @Inject(method = "handleBlockEntityTagQuery", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/permissions/PermissionSet;hasPermission(Lnet/minecraft/server/permissions/Permission;)Z"
    ), cancellable = true)
    private void queryContainerInventory(ServerboundBlockEntityTagQueryPacket packet, CallbackInfo ci) {
        if (!CosmosCarpetAddonsSettings.fullrangecfinditem || player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            return;
        }

        player.connection.send(new ClientboundTagQueryPacket(packet.getTransactionId(), getBlockInventory(packet.getPos())));
        ci.cancel();
    }

    @Inject(method = "handleEntityTagQuery", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/permissions/PermissionSet;hasPermission(Lnet/minecraft/server/permissions/Permission;)Z"
    ), cancellable = true)
    private void queryEntityInventory(ServerboundEntityTagQueryPacket packet, CallbackInfo ci) {
        if (!CosmosCarpetAddonsSettings.fullrangecfinditem || player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            return;
        }

        player.connection.send(new ClientboundTagQueryPacket(packet.getTransactionId(), getEntityInventory(packet.getEntityId())));
        ci.cancel();
    }

    @Unique
    private @Nullable CompoundTag getBlockInventory(BlockPos pos) {
        if (!isTracked(pos)) {
            return null;
        }

        // Never load or generate a chunk to answer an inventory query.
        LevelChunk chunk = player.level().getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        BlockEntity blockEntity = chunk == null ? null : chunk.getBlockEntity(pos);
        if (!(blockEntity instanceof Container)) {
            return null;
        }

        return inventoryTags(blockEntity.saveWithoutMetadata(player.registryAccess()), ContainerHelper.TAG_ITEMS, DecoratedPotBlockEntity.TAG_ITEM);
    }

    @Unique
    private @Nullable CompoundTag getEntityInventory(int entityId) {
        Entity entity = player.level().getEntity(entityId);
        if (entity != player && (!(entity instanceof ContainerEntity || entity instanceof AbstractChestedHorse) || !isTracked(entity.blockPosition()))) {
            return null;
        }

        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, player.registryAccess());
        entity.saveWithoutId(output);
        // Ender chests refer to the querying player's inventory, never another player's.
        return inventoryTags(output.buildResult(), entity == player ? "EnderItems" : ContainerHelper.TAG_ITEMS);
    }

    @Unique
    private boolean isTracked(BlockPos pos) {
        return player.level().getChunkSource().chunkMap.isChunkTracked(player, pos.getX() >> 4, pos.getZ() >> 4);
    }

    @Unique
    private static CompoundTag inventoryTags(CompoundTag source, String... keys) {
        CompoundTag result = new CompoundTag();
        for (String key : keys) {
            Tag value = source.get(key);
            if (value != null) {
                result.put(key, value);
            }
        }
        return result;
    }
}
