package net.cosmosmc.carpetaddons.lightmanipulator.network;

import net.cosmosmc.carpetaddons.CosmosCarpetAddons;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Client -> server: light levels to write into sections that an Axiom builder tool is about to change,
 * copied from the tool's source positions. Applied once Axiom has applied the matching block buffer.
 */
public record SourceLightPayload(ResourceKey<Level> dimension, List<SectionLight> sections) implements CustomPacketPayload {
    public static final Type<SourceLightPayload> TYPE = new Type<>(CosmosCarpetAddons.id("source_light"));

    /** Serverbound custom payloads are limited to 32 KiB, so the client splits its data into chunks of this many sections. */
    public static final int MAX_SECTIONS_PER_PAYLOAD = 6;

    public static final StreamCodec<RegistryFriendlyByteBuf, SourceLightPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeResourceKey(payload.dimension());
                buf.writeVarInt(payload.sections().size());
                for (SectionLight section : payload.sections()) {
                    section.write(buf);
                }
            },
            buf -> {
                ResourceKey<Level> dimension = buf.readResourceKey(Registries.DIMENSION);
                int count = buf.readVarInt();
                if (count < 0 || count > MAX_SECTIONS_PER_PAYLOAD) {
                    throw new IllegalArgumentException("Too many sections in source light payload: " + count);
                }
                List<SectionLight> sections = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    sections.add(SectionLight.read(buf));
                }
                return new SourceLightPayload(dimension, sections);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
