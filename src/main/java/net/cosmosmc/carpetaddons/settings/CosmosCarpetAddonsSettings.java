package net.cosmosmc.carpetaddons.settings;

import carpet.api.settings.CarpetRule;
import carpet.api.settings.Rule;
import carpet.api.settings.Validator;
import carpet.utils.Messenger;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.Nullable;

import static carpet.api.settings.RuleCategory.*;
import static net.cosmosmc.carpetaddons.settings.RuleCategory.*;

public class CosmosCarpetAddonsSettings {
    private static class SyncTimeValidator extends Validator<Integer> {
        @Override
        public Integer validate(@Nullable CommandSourceStack source, CarpetRule<Integer> changingRule, Integer newValue, String userInput) {
            if (0 > newValue || 10 < newValue) {
                int v = Math.clamp(newValue, 0, 10);
                Messenger.m(source, "r " + newValue + " is out of range (0 - 10). Clamping to " + v);
                return v;
            }

            return newValue;
        }
    }

    @Rule(
            categories = {BUGFIX, CCA},
            options = {"0", "2", "4", "10"},
            strict = false,
            validators = { SyncTimeValidator.class }
    )
    public static int ghostProjectileFix = 0;

    @Rule(
            categories = {BUGFIX, CCA},
            options = {"0", "2", "4", "10"},
            strict = false,
            validators = { SyncTimeValidator.class }
    )
    public static int ghostEntitiesFix = 0;
}
