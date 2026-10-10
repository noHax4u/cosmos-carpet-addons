package net.cosmosmc.carpetaddons.logging.TNTCart;

import carpet.logging.Logger;
import net.cosmosmc.carpetaddons.CosmosCarpetAddons;
import net.cosmosmc.carpetaddons.logging.Loggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

// Logging data gets created in mixin.MinecartTNTDebugger and mixin.VehicleEntityDebugger
public class TNTCartLogger extends Logger {
    private static final TNTCartLogger INSTANCE = new TNTCartLogger("TNTCart", "chat", new String[]{"chat", "sound", "full"}, true);

    public static boolean active; // probably move all the active states to the registrar

    Field subscriberField;

    public static TNTCartLogger getInstance() {
        return INSTANCE;
    }

    public TNTCartLogger(String logName, String def, String[] options, boolean strictOptions) { // TODO: clean ts up
        Field loggerField = null;
        try {
            loggerField = TNTCartLogger.class.getField("active");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        super(loggerField, logName, def, options, strictOptions);

        try {
            subscriberField = Logger.class.getDeclaredField("subscribedOnlinePlayers");
            subscriberField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            CosmosCarpetAddons.LOGGER.error("Cannot access logger subscribers. Unable to play logging sounds");
        }
    }

    private static boolean logChatForOption(String option) {
        return !option.equals("sound");
    }

    public static void log(String fmt, Object ...args) {
        INSTANCE.log(new Loggers.FormattedMessage(TNTCartLogger::logChatForOption, fmt, args));
    }

    public static void playSound(MinecartTNT source, SoundEvent event) {
        INSTANCE.playSoundImpl(source, event);
    }

    private void playSoundImpl(MinecartTNT source, SoundEvent event) {
        for (Map.Entry<String,String> en : getSubscribers()) {
            if (!en.getValue().equals("chat")) {
                ServerPlayer player = playerFromName(en.getKey());
                Loggers.playSoundForPlayer(player, SoundSource.UI, source.position(), event, 1f, 0.5f);
            }
        }
    }

    private Set<Map.Entry<String, String>> getSubscribers() {
        try {
            @SuppressWarnings("unchecked")
            var val = (Map<String, String>) subscriberField.get(this);
            return val.entrySet();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
