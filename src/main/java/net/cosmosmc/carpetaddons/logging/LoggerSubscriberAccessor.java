package net.cosmosmc.carpetaddons.logging;

import carpet.logging.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(Logger.class)
public interface LoggerSubscriberAccessor {
    @Accessor("subscribedOnlinePlayers")
    Map<String, String> cosmosCarpetAddons$getSubscribedOnlinePlayer();
}
