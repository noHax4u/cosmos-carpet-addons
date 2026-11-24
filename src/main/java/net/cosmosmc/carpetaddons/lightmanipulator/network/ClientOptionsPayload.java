package net.cosmosmc.carpetaddons.lightmanipulator.network;

import net.cosmosmc.carpetaddons.CosmosCarpetAddons;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client -> server: client options the server needs to know about.
 *
 * @param axiomNoUpdatesSkipsLight Axiom's "no updates" mode also skips light updates for this player's edits
 */
public record ClientOptionsPayload(boolean axiomNoUpdatesSkipsLight) implements CustomPacketPayload {
    public static final Type<ClientOptionsPayload> TYPE = new Type<>(CosmosCarpetAddons.id("client_options"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientOptionsPayload> CODEC =
            ByteBufCodecs.BOOL.map(ClientOptionsPayload::new, ClientOptionsPayload::axiomNoUpdatesSkipsLight).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
