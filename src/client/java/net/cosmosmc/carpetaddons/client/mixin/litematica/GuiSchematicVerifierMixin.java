package net.cosmosmc.carpetaddons.client.mixin.litematica;

import fi.dy.masa.litematica.gui.GuiSchematicVerifier;
import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu.ButtonType;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.util.StringUtils;
import net.cosmosmc.carpetaddons.client.litematica.LightVerifier;
import net.cosmosmc.carpetaddons.client.litematica.LightVerifierHolder;
import net.cosmosmc.carpetaddons.client.litematica.LitematicaLightOptions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds a light mismatch button next to the main menu button: shows the count, lists the closest mismatches
 * on hover and toggles highlighting them in the world.
 */
@Mixin(value = GuiSchematicVerifier.class, remap = false)
public abstract class GuiSchematicVerifierMixin {
    @Shadow
    @Final
    private SchematicVerifier verifier;

    @Inject(method = "initGui", at = @At("TAIL"))
    private void cca$addLightButton(CallbackInfo ci) {
        GuiBase gui = (GuiBase) (Object) this;
        LightVerifier light = ((LightVerifierHolder) verifier).cca$getLightVerifier();

        String label;
        List<String> hover = new ArrayList<>();
        if (!light.schematicHasLight()) {
            label = StringUtils.translate("cosmos-carpet-addons.litematica.verifier.no_light");
            hover.add(StringUtils.translate("cosmos-carpet-addons.litematica.verifier.no_light.hover"));
        } else {
            String state = StringUtils.translate(LitematicaLightOptions.highlightLightMismatches
                    ? "cosmos-carpet-addons.litematica.verifier.highlight_on" : "cosmos-carpet-addons.litematica.verifier.highlight_off");
            label = StringUtils.translate("cosmos-carpet-addons.litematica.verifier.light", light.mismatchCount(), state);
            hover.add(StringUtils.translate("cosmos-carpet-addons.litematica.verifier.light.hover", light.checkedPositions()));
            for (LightVerifier.LightMismatch mismatch : light.closestMismatchDetails(10)) {
                hover.add(GuiBase.TXT_GOLD + mismatch.describe() + GuiBase.TXT_RST);
            }
        }

        // same position as the main menu button, shifted left
        String mainMenuLabel = StringUtils.translate(ButtonType.MAIN_MENU.getLabelKey());
        int mainMenuX = gui.getScreenWidth() - (gui.getStringWidth(mainMenuLabel) + 20) - 10;
        int width = gui.getStringWidth(label) + 20;
        ButtonGeneric button = new ButtonGeneric(mainMenuX - width - 4, gui.getScreenHeight() - 36, width, 20, label, hover.toArray(String[]::new));
        gui.addButton(button, (b, mouseButton) -> {
            if (light.schematicHasLight()) {
                LitematicaLightOptions.highlightLightMismatches = !LitematicaLightOptions.highlightLightMismatches;
                gui.initGui();
            }
        });
    }
}
