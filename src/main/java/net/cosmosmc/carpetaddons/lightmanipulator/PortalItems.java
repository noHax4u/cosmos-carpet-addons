package net.cosmosmc.carpetaddons.lightmanipulator;

import net.cosmosmc.carpetaddons.CosmosCarpetAddons;
import net.cosmosmc.carpetaddons.settings.CosmosCarpetAddonsSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;

/**
 * Nether portal "items": a purple stained glass pane that carries a portal axis and light levels in its custom data.
 * Placing one places a nether portal block (without the frame check) and, while the light level manipulator
 * is active, sets that position's block and/or sky light.
 * <p>
 * Clients with this mod installed render the actual portal model (see the client's ItemModelResolverMixin),
 * vanilla clients see a purple stained glass pane.
 */
public final class PortalItems {
    public static final String TAG = CosmosCarpetAddons.MOD_ID + ":nether_portal";
    public static final int KEEP = -1;

    public static final Identifier MODEL_X = CosmosCarpetAddons.id("nether_portal_x");
    public static final Identifier MODEL_Z = CosmosCarpetAddons.id("nether_portal_z");

    private PortalItems() {
    }

    public record PortalData(Direction.Axis axis, int blockLight, int skyLight) {
    }

    public static ItemStack create(Direction.Axis axis, int blockLight, int skyLight) {
        ItemStack stack = new ItemStack(Items.STAINED_GLASS_PANE.pick(DyeColor.PURPLE));
        CompoundTag data = new CompoundTag();
        data.putString("axis", axis.getSerializedName());
        if (blockLight >= 0) data.putInt("block", blockLight);
        if (skyLight >= 0) data.putInt("sky", skyLight);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(TAG, data));

        stack.set(DataComponents.ITEM_NAME, Component.literal("Nether Portal (" + axis.getSerializedName().toUpperCase() + ", "
                + describe("block", blockLight) + ", " + describe("sky", skyLight) + ")"));
        return stack;
    }

    private static String describe(String layer, int level) {
        return layer + " " + (level >= 0 ? Integer.toString(level) : "keep");
    }

    public static @Nullable PortalData get(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || customData.isEmpty() || !stack.is(Items.STAINED_GLASS_PANE.pick(DyeColor.PURPLE))) {
            return null;
        }
        CompoundTag data = customData.copyTag().getCompound(TAG).orElse(null);
        if (data == null) {
            return null;
        }
        Direction.Axis axis = "z".equals(data.getStringOr("axis", "x")) ? Direction.Axis.Z : Direction.Axis.X;
        return new PortalData(axis, data.getIntOr("block", KEEP), data.getIntOr("sky", KEEP));
    }

    /**
     * @return the model to render instead of the item's own, or null for normal items
     */
    public static @Nullable Identifier modelFor(ItemStack stack) {
        PortalData data = get(stack);
        if (data == null) return null;
        return data.axis() == Direction.Axis.Z ? MODEL_Z : MODEL_X;
    }

    /**
     * @return the result of placing a portal item, or null if {@code context} doesn't hold one
     */
    public static @Nullable InteractionResult place(BlockPlaceContext context) {
        ItemStack stack = context.getItemInHand();
        PortalData data = get(stack);
        if (data == null) {
            return null;
        }
        if (!(context.getLevel() instanceof ServerLevel level)) {
            // don't predict a glass pane on the client
            return InteractionResult.SUCCESS;
        }
        if (!context.canPlace()) {
            return InteractionResult.FAIL;
        }

        BlockPos pos = context.getClickedPos();
        var state = Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, data.axis());
        // UPDATE_KNOWN_SHAPE: don't update neighbours, so the portal (and its neighbours) don't check for a frame
        if (!level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)) {
            return InteractionResult.FAIL;
        }

        if (CosmosCarpetAddonsSettings.lightLevelManipulator) {
            BoundingBox box = BoundingBox.fromCorners(pos, pos);
            if (data.blockLight() >= 0) {
                LightManipulator.apply(level, LightLayer.BLOCK, box, (x, y, z, current) -> data.blockLight());
            }
            if (data.skyLight() >= 0 && LightManipulator.hasLayer(level, LightLayer.SKY)) {
                LightManipulator.apply(level, LightLayer.SKY, box, (x, y, z, current) -> data.skyLight());
            }
        }

        Player player = context.getPlayer();
        level.playSound(player, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }
}
