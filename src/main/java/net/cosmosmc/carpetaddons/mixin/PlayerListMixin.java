package net.cosmosmc.carpetaddons.mixin;

import net.cosmosmc.carpetaddons.settings.CosmosCarpetAddonsSettings;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @ModifyVariable(
            method = "sendPlayerPermissionLevel(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/permissions/LevelBasedPermissionSet;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private LevelBasedPermissionSet advertiseFullRangeItemSearch(LevelBasedPermissionSet permissions) {
        // Only change the permission indication sent to the client. Server-side
        // permissions and the command tree continue to use the player's real level.
        // Vanilla sends this indication on login, respawn, and permission changes.
        if (CosmosCarpetAddonsSettings.fullrangecfinditem && !permissions.hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            return LevelBasedPermissionSet.GAMEMASTER;
        }
        return permissions;
    }
}
