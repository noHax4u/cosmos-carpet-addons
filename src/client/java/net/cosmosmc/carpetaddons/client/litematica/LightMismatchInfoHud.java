package net.cosmosmc.carpetaddons.client.litematica;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.util.RayTraceUtils;
import net.cosmosmc.carpetaddons.CosmosCarpetAddons;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows the expected and found light levels while looking at a light mismatch marker of the schematic verifier.
 */
public final class LightMismatchInfoHud {
    private LightMismatchInfoHud() {
    }

    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, CosmosCarpetAddons.id("light_mismatch_info"), LightMismatchInfoHud::render);
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        List<Component> lines = lines();
        if (lines.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        int width = 0;
        for (Component line : lines) {
            width = Math.max(width, mc.font.width(line));
        }
        int centerX = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() / 2 + 22;
        // dark background so the text stays readable on bright blocks
        graphics.fill(centerX - width / 2 - 3, y - 3, centerX + width / 2 + 3, y + lines.size() * 10 + 1, 0xA0000000);
        for (Component line : lines) {
            graphics.centeredText(mc.font, line, centerX, y, 0xFFFFFFFF);
            y += 10;
        }
    }

    private static List<Component> lines() {
        Minecraft mc = Minecraft.getInstance();
        if (!LitematicaLightConfigs.LIGHT_OVERLAY_ENABLED.getBooleanValue() || mc.getCameraEntity() == null) {
            return List.of();
        }
        SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();
        if (placement == null || !placement.hasVerifier()) {
            return List.of();
        }
        LightVerifier verifier = ((LightVerifierHolder) placement.getSchematicVerifier()).cca$getLightVerifier();
        BlockHitResult hit = RayTraceUtils.traceToPositions(verifier.closestMismatches(), mc.getCameraEntity(), 128.0);
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return List.of();
        }
        LightVerifier.LightMismatch mismatch = verifier.getMismatch(hit.getBlockPos());
        if (mismatch == null) {
            return List.of();
        }
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("cosmos-carpet-addons.litematica.info.title").withStyle(ChatFormatting.BOLD));
        // the wrong light layers in gold, matching ones in gray
        boolean blockWrong = mismatch.expectedBlock() != mismatch.foundBlock();
        lines.add(Component.translatable("cosmos-carpet-addons.litematica.info.block", mismatch.expectedBlock(), mismatch.foundBlock())
                .withStyle(blockWrong ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        if (mismatch.expectedSky() >= 0) {
            boolean skyWrong = mismatch.expectedSky() != mismatch.foundSky();
            lines.add(Component.translatable("cosmos-carpet-addons.litematica.info.sky", mismatch.expectedSky(), mismatch.foundSky())
                    .withStyle(skyWrong ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        }
        return lines;
    }
}
