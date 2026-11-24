package net.cosmosmc.carpetaddons.logging.TNTCart;

import carpet.logging.Logger;
import net.cosmosmc.carpetaddons.logging.Loggers;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;

public class TNTCartPrimeLogger extends Logger {
    private static final TNTCartPrimeLogger INSTANCE = new TNTCartPrimeLogger("TNTCartPriming", "", new String[]{}, true);

    public static boolean active; // probably move all the active states to the registrar

    public static TNTCartPrimeLogger getInstance() {
        return INSTANCE;
    }

    public TNTCartPrimeLogger(String logName, String def, String[] options, boolean strictOptions) { // TODO: clean ts up
        Field loggerField = null;
        try {
            loggerField = TNTCartPrimeLogger.class.getField("active");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        super(loggerField, logName, def, options, strictOptions);
    }

    public static void log(String cartId, Entity source) {
        INSTANCE.log(new Loggers.FormattedMessage("§3{}§r got hit by §6{} §r(id {})§r", cartId, source.getClass().getSimpleName(), source.getId()));
    }
}
