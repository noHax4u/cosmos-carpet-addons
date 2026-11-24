package net.cosmosmc.carpetaddons.client.config;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** Config screen for the client options, opened from Mod Menu. */
public class CcaConfigScreen extends Screen {
    private final @Nullable Screen parent;

    public CcaConfigScreen(@Nullable Screen parent) {
        super(Component.translatable("cosmos-carpet-addons.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        CcaClientConfig config = CcaClientConfig.get();
        int x = width / 2 - 155;
        int y = height / 6;

        addRenderableWidget(CycleButton.onOffBuilder(config.showLightLevelWhenHoldingLightItem)
                .withTooltip(value -> Tooltip.create(Component.translatable("cosmos-carpet-addons.config.showLightLevelWhenHoldingLightItem.tooltip")))
                .create(x, y, 310, 20, Component.translatable("cosmos-carpet-addons.config.showLightLevelWhenHoldingLightItem"),
                        (button, value) -> config.showLightLevelWhenHoldingLightItem = value));
        y += 24;
        addRenderableWidget(CycleButton.onOffBuilder(config.axiomNoUpdatesSkipsLight)
                .withTooltip(value -> Tooltip.create(Component.translatable("cosmos-carpet-addons.config.axiomNoUpdatesSkipsLight.tooltip")))
                .create(x, y, 310, 20, Component.translatable("cosmos-carpet-addons.config.axiomNoUpdatesSkipsLight"),
                        (button, value) -> config.axiomNoUpdatesSkipsLight = value));

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(width / 2 - 100, height - 28, 200, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.centeredText(font, title, width / 2, 15, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        CcaClientConfig.save();
        minecraft.gui.setScreen(parent);
    }
}
