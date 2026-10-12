package net.cosmosmc.carpetaddons.logging;

import carpet.logging.Logger;
import carpet.logging.LoggerRegistry;
import net.cosmosmc.carpetaddons.CosmosCarpetAddons;
import net.cosmosmc.carpetaddons.logging.TNTCart.TNTCartLogger;
import net.cosmosmc.carpetaddons.logging.TNTCart.TNTCartPrimeLogger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.text.Normalizer;
import java.util.Map;
import java.util.function.Function;

public class Loggers {
    public static void register() {
        register(TNTCartLogger.getInstance());
        register(TNTCartPrimeLogger.getInstance());
    }

    private static void register(Logger logger) {
        LoggerRegistry.registerLogger(logger.getLogName(), logger);
    }

    public static void playSoundForPlayer(ServerPlayer serverPlayer, SoundSource source, Vec3 origin, SoundEvent sound, float volume, float pitch) {
        serverPlayer.connection.send(
                new ClientboundSoundPacket(
                        Holder.direct(sound),
                        source,
                        origin.x(),
                        origin.y(),
                        origin.z(),
                        volume,
                        pitch,
                        0
                )
        );    }

    public static class FormattedMessage implements Logger.lMessage {
        private String msg;
        private Function<String, Boolean> predicate;

        public static boolean alwaysSend(String ignored) {
            return true;
        }

        public FormattedMessage(String format, Object ...args) {
            this(FormattedMessage::alwaysSend, format, args);
        }
        public FormattedMessage(Function<String, Boolean> predicate, String format, Object... args) {
            msg = FormattedMessage.format(format, args);
            this.predicate = predicate;
        }

        @Override
        public Component[] get(String playerOption, Player player) {
            if (predicate.apply(playerOption))
                return new MutableComponent[]{MutableComponent.create(PlainTextContents.create(msg))};
            else
                return new MutableComponent[0];
        }

        private static String format(String fmt, Object ...args) {
            StringBuffer buffer = new StringBuffer(fmt);

            for (final Object arg: args) {
                final int index = buffer.indexOf("{");
                if (index == -1) {
                    CosmosCarpetAddons.LOGGER.error("index out of range: {}. Message: {}. args: {}", index, fmt, args);
                    break;
                }
                buffer.replace(index, index + 2, arg.toString());
            }
            return buffer.toString();
        }
    }
}
