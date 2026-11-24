package net.cosmosmc.carpetaddons.lightmanipulator.network;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.cosmosmc.carpetaddons.lightmanipulator.LightManipulator;
import net.cosmosmc.carpetaddons.settings.CosmosCarpetAddonsSettings;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Networking for the light level manipulator. Everything here works without Axiom; Axiom only
 * triggers {@link #consumeSourceLight} through an optional mixin.
 */
public final class LightNetworking {
    /** Pending source light is dropped if no matching Axiom buffer arrives within this many ticks. */
    private static final int PENDING_TIMEOUT_TICKS = 20 * 60;
    /** Upper bound of pending sections per player (~4.6 KiB each). */
    private static final int MAX_PENDING_SECTIONS = 1 << 14;

    private record Pending(ResourceKey<Level> dimension, Long2ObjectMap<SectionLight> sections, Long2ObjectMap<Integer> receivedAt) {
    }

    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private static int tick = 0;

    private LightNetworking() {
    }

    public static void init() {
        PayloadTypeRegistry.clientboundPlay().register(ManipulatorStatePayload.TYPE, ManipulatorStatePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SourceLightPayload.TYPE, SourceLightPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SourceLightPayload.TYPE, (payload, context) -> receiveSourceLight(context.player(), payload));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendState(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> PENDING.remove(handler.getPlayer().getUUID()));
        ServerTickEvents.END_SERVER_TICK.register(server -> expirePending());
    }

    public static void sendState(ServerPlayer player) {
        if (ServerPlayNetworking.canSend(player, ManipulatorStatePayload.TYPE)) {
            ServerPlayNetworking.send(player, new ManipulatorStatePayload(CosmosCarpetAddonsSettings.lightLevelManipulator));
        }
    }

    public static void broadcastState(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(LightNetworking::sendState);
        if (!CosmosCarpetAddonsSettings.lightLevelManipulator) {
            PENDING.clear();
        }
    }

    private static void receiveSourceLight(ServerPlayer player, SourceLightPayload payload) {
        // same requirements as /lightmanipulator
        if (!CosmosCarpetAddonsSettings.lightLevelManipulator
                || !Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(player.createCommandSourceStack())) {
            return;
        }

        Pending pending = PENDING.get(player.getUUID());
        if (pending == null || !pending.dimension().equals(payload.dimension())) {
            pending = new Pending(payload.dimension(), new Long2ObjectOpenHashMap<>(), new Long2ObjectOpenHashMap<>());
            PENDING.put(player.getUUID(), pending);
        }
        for (SectionLight section : payload.sections()) {
            if (pending.sections().size() >= MAX_PENDING_SECTIONS && !pending.sections().containsKey(section.sectionPos())) {
                return;
            }
            pending.sections().put(section.sectionPos(), section);
            pending.receivedAt().put(section.sectionPos(), (Integer) tick);
        }
    }

    private static void expirePending() {
        tick++;
        if (tick % 20 != 0 || PENDING.isEmpty()) {
            return;
        }
        PENDING.values().removeIf(pending -> {
            pending.receivedAt().long2ObjectEntrySet().removeIf(entry -> {
                boolean expired = tick - entry.getValue() > PENDING_TIMEOUT_TICKS;
                if (expired) {
                    pending.sections().remove(entry.getLongKey());
                }
                return expired;
            });
            return pending.sections().isEmpty();
        });
    }

    /**
     * Called after Axiom applied a block buffer section sent by {@code player}: writes the pending source light
     * for that section, if any.
     */
    public static void consumeSourceLight(ServerLevel level, @Nullable ServerPlayer player, long sectionPos) {
        if (player == null || !CosmosCarpetAddonsSettings.lightLevelManipulator) {
            return;
        }
        Pending pending = PENDING.get(player.getUUID());
        if (pending == null || !pending.dimension().equals(level.dimension())) {
            return;
        }
        SectionLight section = pending.sections().remove(sectionPos);
        pending.receivedAt().remove(sectionPos);
        if (section != null) {
            LightManipulator.applySectionLight(level, section);
        }
    }
}
