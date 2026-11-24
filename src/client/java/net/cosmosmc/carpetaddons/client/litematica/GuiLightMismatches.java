package net.cosmosmc.carpetaddons.client.litematica;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.LightLayer;

import java.util.List;
import java.util.Map;

/**
 * The "Wrong Light Levels" tab of Litematica's schematic verifier: lists the light mismatch categories
 * (e.g. "block light: expected 1, found 15") with their counts. Categories can be selected for highlighting
 * (none selected highlights all) and ignored.
 */
public class GuiLightMismatches extends GuiBase {
    private static final int ROW_HEIGHT = 22;
    private static final int TEXT_COLOR = 0xFFF0F0F0;

    private final LightVerifier verifier;
    private int page = 0;

    public GuiLightMismatches(LightVerifier verifier, Screen parent) {
        this.verifier = verifier;
        this.title = StringUtils.translate("cosmos-carpet-addons.litematica.light_tab.title");
        setParent(parent);
    }

    @Override
    public String getTitleString() {
        return t("title");
    }

    private static String t(String key, Object... args) {
        return StringUtils.translate("cosmos-carpet-addons.litematica.light_tab." + key, args);
    }

    private int addButton(int x, int y, String label, String hover, Runnable action) {
        int width = getStringWidth(label) + 20;
        ButtonGeneric button = hover == null ? new ButtonGeneric(x, y, width, 20, label) : new ButtonGeneric(x, y, width, 20, label, hover);
        addButton(button, (b, mouseButton) -> {
            action.run();
            initGui();
        });
        return x + width + 4;
    }

    @Override
    public void initGui() {
        super.initGui();
        clearButtons();
        clearWidgets();

        int x = 12;
        int y = 20;
        x = addButton(x, y, t("back"), null, () -> GuiBase.openGui(getParent()));
        boolean overlay = LitematicaLightConfigs.LIGHT_OVERLAY_ENABLED.getBooleanValue();
        x = addButton(x, y, t(overlay ? "highlight_on" : "highlight_off"), t("highlight.hover"), () -> {
            LitematicaLightConfigs.LIGHT_OVERLAY_ENABLED.toggleBooleanValue();
            LitematicaLightConfigs.save();
        });
        if (verifier.ignoredCount() > 0) {
            addButton(x, y, t("reset_ignored", verifier.ignoredCount()), null, verifier::resetIgnored);
        }

        List<Map.Entry<LightVerifier.Category, Integer>> categories = verifier.categoryCounts();
        y += 26;
        addLabel(12, y, 400, 12, TEXT_COLOR, t("summary", verifier.mismatchCount(), verifier.checkedPositions()));
        y += 16;
        if (categories.isEmpty()) {
            addLabel(12, y, 400, 12, TEXT_COLOR, t(verifier.schematicHasLight() ? "none" : "no_data"));
            return;
        }

        int rowsPerPage = Math.max(1, (getScreenHeight() - y - 34) / ROW_HEIGHT);
        int pages = (categories.size() + rowsPerPage - 1) / rowsPerPage;
        page = Math.min(page, pages - 1);
        int first = page * rowsPerPage;

        for (int i = first; i < Math.min(categories.size(), first + rowsPerPage); i++) {
            LightVerifier.Category category = categories.get(i).getKey();
            int count = categories.get(i).getValue();
            String layer = t(category.layer() == LightLayer.SKY ? "sky" : "block");
            String color = category.layer() == LightLayer.SKY ? GuiBase.TXT_AQUA : GuiBase.TXT_GOLD;
            addLabel(12, y + 5, 300, 12, TEXT_COLOR,
                    t("row", color + layer + GuiBase.TXT_RST, category.expected(), category.found(), count));

            boolean isSelected = verifier.isSelected(category);
            int bx = getScreenWidth() - 12;
            String ignoreLabel = t("ignore");
            bx -= getStringWidth(ignoreLabel) + 20;
            addButton(bx, y, ignoreLabel, t("ignore.hover"), () -> verifier.ignore(category));
            String selectLabel = t(isSelected ? "selected" : "select");
            bx -= getStringWidth(selectLabel) + 24;
            addButton(bx, y, (isSelected ? GuiBase.TXT_GREEN : "") + selectLabel + GuiBase.TXT_RST, t("select.hover"),
                    () -> verifier.toggleSelected(category));
            y += ROW_HEIGHT;
        }

        if (pages > 1) {
            int by = getScreenHeight() - 28;
            int bx = addButton(12, by, "<", null, () -> page = Math.max(0, page - 1));
            bx = addButton(bx, by, ">", null, () -> page = Math.min(pages - 1, page + 1));
            addLabel(bx + 4, by + 5, 100, 12, TEXT_COLOR, t("page", page + 1, pages));
        }
    }
}
