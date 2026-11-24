package net.cosmosmc.carpetaddons.mixin;

import net.cosmosmc.carpetaddons.lightmanipulator.LightManipulator;
import net.cosmosmc.carpetaddons.lightmanipulator.PortalItems;
import net.cosmosmc.carpetaddons.settings.CosmosCarpetAddonsSettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    /**
     * While the light level manipulator is active, "placing" a light item doesn't place a light block,
     * it only sets the light level of that single position (sky light if the item has a glint override).
     * Also places nether portal items.
     */
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void cca$manipulateLight(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult portalResult = PortalItems.place(context);
        if (portalResult != null) {
            cir.setReturnValue(portalResult);
            return;
        }

        if (!CosmosCarpetAddonsSettings.lightLevelManipulator || !context.getItemInHand().is(Items.LIGHT)) {
            return;
        }

        if (context.getLevel() instanceof ServerLevel serverLevel) {
            LightManipulator.placeLightItem(serverLevel, context.getClickedPos(), context.getItemInHand());
        }
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
