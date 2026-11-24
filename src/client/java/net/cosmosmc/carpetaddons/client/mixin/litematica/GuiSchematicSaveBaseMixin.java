package net.cosmosmc.carpetaddons.client.mixin.litematica;

import fi.dy.masa.litematica.gui.GuiSchematicSave;
import fi.dy.masa.litematica.gui.GuiSchematicSaveBase;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.widgets.WidgetCheckBox;
import fi.dy.masa.malilib.util.StringUtils;
import net.cosmosmc.carpetaddons.client.litematica.LitematicaLightConfigs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds an "include light levels" checkbox next to the save button when creating a schematic from a selection.
 */
@Mixin(value = GuiSchematicSaveBase.class, remap = false)
public abstract class GuiSchematicSaveBaseMixin {
    @Shadow
    @Final
    protected LitematicaSchematic schematic;

    @Inject(method = "initGui", at = @At("TAIL"))
    private void cca$addIncludeLightCheckbox(CallbackInfo ci) {
        if (!((Object) this instanceof GuiSchematicSave) || schematic != null) {
            return;
        }
        GuiBase gui = (GuiBase) (Object) this;
        // the save button is at (10, 54) and as wide as its label + 10
        String saveLabel = StringUtils.translate(GuiSchematicSaveBase.ButtonType.SAVE.getLabelKey());
        int x = 10 + gui.getStringWidth(saveLabel) + 10 + 8;

        WidgetCheckBox checkbox = new WidgetCheckBox(x, 59, Icons.CHECKBOX_UNSELECTED, Icons.CHECKBOX_SELECTED,
                StringUtils.translate("cosmos-carpet-addons.litematica.save.include_light"),
                StringUtils.translate("cosmos-carpet-addons.litematica.save.include_light.hover"));
        checkbox.setChecked(LitematicaLightConfigs.SAVE_INCLUDE_LIGHT.getBooleanValue(), false);
        checkbox.setListener(widget -> {
            LitematicaLightConfigs.SAVE_INCLUDE_LIGHT.setBooleanValue(widget.isChecked());
            LitematicaLightConfigs.save();
        });
        gui.addWidget(checkbox);
    }
}
