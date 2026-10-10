package net.cosmosmc.carpetaddons;

import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.List;

public class CosUtils {
    public static void forceUpdateEntity(ServerLevel serverLevel, Entity entity) {
        var motionPacket  = new ClientboundSetEntityMotionPacket(entity.getId(), entity.getDeltaMovement());
        var positionPacket = ClientboundEntityPositionSyncPacket.of(entity);
        serverLevel.getChunkSource().sendToTrackingPlayers(entity, new ClientboundBundlePacket(
                List.of(motionPacket, positionPacket)
        ));
    }
}
