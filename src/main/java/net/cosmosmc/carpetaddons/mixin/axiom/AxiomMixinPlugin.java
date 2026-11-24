package net.cosmosmc.carpetaddons.mixin.axiom;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Only applies the Axiom integration mixins when Axiom is installed, so Axiom stays an optional dependency.
 * Used by both the common and the client Axiom mixin configs.
 */
public class AxiomMixinPlugin implements IMixinConfigPlugin {
    public static final String AXIOM_MOD_ID = "axiom";

    private boolean axiomLoaded;

    @Override
    public void onLoad(String mixinPackage) {
        axiomLoaded = FabricLoader.getInstance().isModLoaded(AXIOM_MOD_ID);
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return axiomLoaded;
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
