package net.cosmosmc.carpetaddons.client;

import net.cosmosmc.carpetaddons.CosmosCarpetAddons;
import net.cosmosmc.carpetaddons.client.config.CcaClientConfig;
import net.cosmosmc.carpetaddons.lightmanipulator.LightManipulator;
import net.cosmosmc.carpetaddons.lightmanipulator.PortalItems;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

/**
 * While holding a light item (or nether portal item), shows the light level at the position the item would be placed:
 * block light for normal light items, sky light for light items with an enchantment glint override, both for portals.
 */
public final class LightLevelHud {
    private LightLevelHud() {
    }

    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, CosmosCarpetAddons.id("light_level"), LightLevelHud::render);
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Component text = text();
        if (text != null) {
            graphics.centeredText(Minecraft.getInstance().font, text, graphics.guiWidth() / 2, graphics.guiHeight() / 2 + 10, 0xFFFFFF55);
        }
    }

    private static @Nullable Component text() {
        Minecraft mc = Minecraft.getInstance();
        if (!CcaClientConfig.get().showLightLevelWhenHoldingLightItem || mc.player == null || mc.level == null
                || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = mc.player.getItemInHand(hand);
            boolean light = stack.is(Items.LIGHT);
            boolean portal = PortalItems.get(stack) != null;
            if (!light && !portal) {
                continue;
            }
            // where using the item would place it (the targeted block if it is replaceable, otherwise next to it)
            BlockPos pos = new BlockPlaceContext(mc.player, hand, stack, hit).getClickedPos();
            int block = mc.level.getBrightness(LightLayer.BLOCK, pos);
            int sky = mc.level.getBrightness(LightLayer.SKY, pos);
            if (portal) {
                return Component.translatable("cosmos-carpet-addons.hud.light_level.both", block, sky);
            }
            return LightManipulator.isSkyLightItem(stack)
                    ? Component.translatable("cosmos-carpet-addons.hud.light_level.sky", sky)
                    : Component.translatable("cosmos-carpet-addons.hud.light_level.block", block);
        }
        return null;
    }
}
