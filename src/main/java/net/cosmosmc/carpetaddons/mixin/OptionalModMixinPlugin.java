package net.cosmosmc.carpetaddons.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Applies the mixins of an optional integration only when the mod it integrates with is installed, so these
 * mods stay optional dependencies. The mod id is the last segment of the mixin config's package, e.g.
 * {@code ...mixin.axiom} is only applied with Axiom, {@code ...mixin.litematica} only with Litematica.
 */
public class OptionalModMixinPlugin implements IMixinConfigPlugin {
    private boolean modLoaded;

    @Override
    public void onLoad(String mixinPackage) {
        String modId = mixinPackage.substring(mixinPackage.lastIndexOf('.') + 1);
        modLoaded = FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return modLoaded;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
