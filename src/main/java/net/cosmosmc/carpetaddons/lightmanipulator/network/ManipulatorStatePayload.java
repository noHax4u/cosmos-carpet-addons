package net.cosmosmc.carpetaddons.lightmanipulator.network;

import net.cosmosmc.carpetaddons.CosmosCarpetAddons;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client: whether the light level manipulator is active on the server.
 * Clients with this mod stop relighting block changes locally while it is.
 */
public record ManipulatorStatePayload(boolean active) implements CustomPacketPayload {
    public static final Type<ManipulatorStatePayload> TYPE = new Type<>(CosmosCarpetAddons.id("light_manipulator_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ManipulatorStatePayload> CODEC =
            ByteBufCodecs.BOOL.map(ManipulatorStatePayload::new, ManipulatorStatePayload::active).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
