package net.cosmosmc.carpetaddons.client.mixin;

import net.cosmosmc.carpetaddons.lightmanipulator.PortalItems;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemModelResolver.class)
public abstract class ItemModelResolverMixin {
    /**
     * Renders nether portal items (purple stained glass panes with our custom data) as actual nether portals.
     */
    @ModifyVariable(method = "appendItemLayers", at = @At("STORE"), ordinal = 0)
    private Identifier cca$renderPortalItems(Identifier modelId, ItemStackRenderState output, ItemStack item) {
        Identifier portalModel = PortalItems.modelFor(item);
        return portalModel != null ? portalModel : modelId;
    }
}
