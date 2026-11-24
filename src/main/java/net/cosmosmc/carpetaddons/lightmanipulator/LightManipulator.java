package net.cosmosmc.carpetaddons.lightmanipulator;

import net.cosmosmc.carpetaddons.CosmosCarpetAddons;
import net.cosmosmc.carpetaddons.lightmanipulator.network.SectionLight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.level.lighting.LightEngine;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Writes light levels directly into the light engine's storage, without propagating them.
 * <p>
 * All writes happen on the light engine thread. Changed sections are synced to clients by the
 * light engine itself once the task has run.
 */
public final class LightManipulator {
    private LightManipulator() {
    }

    public record Result(int changed, int skipped) {
        static final Result EMPTY = new Result(0, 0);

        Result plus(Result other) {
            return new Result(changed + other.changed, skipped + other.skipped);
        }
    }

    public static boolean hasLayer(ServerLevel level, LightLayer layer) {
        return engineFor(level.getChunkSource().getLightEngine(), layer) != null;
    }

    private static @Nullable LightEngine<?, ?> engineFor(ThreadedLevelLightEngine lightEngine, LightLayer layer) {
        return layer == LightLayer.BLOCK ? lightEngine.blockEngine : lightEngine.skyEngine;
    }

    /**
     * Sets the light level of every position in {@code box} according to {@code function}.
     * Positions in sections without light storage (e.g. far above the terrain) are skipped.
     *
     * @return a future that completes (on the light thread) once all chunks have been processed
     */
    public static CompletableFuture<Result> apply(ServerLevel level, LightLayer layer, BoundingBox box, LightFunction function) {
        ThreadedLevelLightEngine lightEngine = level.getChunkSource().getLightEngine();
        LightEngine<?, ?> engine = engineFor(lightEngine, layer);
        if (engine == null) {
            return CompletableFuture.completedFuture(Result.EMPTY);
        }

        LayerLightSectionStorage<?> storage = engine.storage;
        LightTaskScheduler scheduler = (LightTaskScheduler) lightEngine;
        List<CompletableFuture<Result>> futures = new ArrayList<>();

        int minChunkX = SectionPos.blockToSectionCoord(box.minX());
        int maxChunkX = SectionPos.blockToSectionCoord(box.maxX());
        int minChunkZ = SectionPos.blockToSectionCoord(box.minZ());
        int maxChunkZ = SectionPos.blockToSectionCoord(box.maxZ());

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                CompletableFuture<Result> future = new CompletableFuture<>();
                futures.add(future);
                int cx = chunkX, cz = chunkZ;
                BitSet changedSections = new BitSet();
                Result[] result = new Result[1];
                scheduler.cca$submitLightTask(cx, cz, () -> {
                    try {
                        result[0] = applyToChunk(storage, box, cx, cz, function, lightEngine.getMinLightSection(), changedSections);
                    } catch (Throwable t) {
                        CosmosCarpetAddons.LOGGER.error("Failed to manipulate light in chunk [{}, {}]", cx, cz, t);
                        future.completeExceptionally(t);
                    }
                }, () -> {
                    // runs after the light engine published the changes
                    if (result[0] == null) {
                        return;
                    }
                    if (!changedSections.isEmpty()) {
                        level.getServer().execute(() -> sendLightUpdate(level, new ChunkPos(cx, cz), layer, changedSections));
                    }
                    future.complete(result[0]);
                });
            }
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(ignored -> {
            Result total = Result.EMPTY;
            for (CompletableFuture<Result> future : futures) {
                total = total.plus(future.join());
            }
            return total;
        });
    }

    /**
     * Vanilla only sends light updates to players on the edge of their view distance, since clients light
     * block changes themselves. Manipulated light has to be sent to everyone tracking the chunk.
     */
    private static void sendLightUpdate(ServerLevel level, ChunkPos pos, LightLayer layer, BitSet changedSections) {
        List<ServerPlayer> players = level.getChunkSource().chunkMap.getPlayers(pos, false);
        if (players.isEmpty()) {
            return;
        }
        BitSet empty = new BitSet();
        var packet = new ClientboundLightUpdatePacket(pos, level.getChunkSource().getLightEngine(),
                layer == LightLayer.SKY ? changedSections : empty,
                layer == LightLayer.BLOCK ? changedSections : empty);
        for (ServerPlayer player : players) {
            player.connection.send(packet);
        }
    }

    private static Result applyToChunk(LayerLightSectionStorage<?> storage, BoundingBox box, int chunkX, int chunkZ, LightFunction function,
                                       int minLightSection, BitSet changedSections) {
        int minX = Math.max(box.minX(), SectionPos.sectionToBlockCoord(chunkX));
        int maxX = Math.min(box.maxX(), SectionPos.sectionToBlockCoord(chunkX, 15));
        int minZ = Math.max(box.minZ(), SectionPos.sectionToBlockCoord(chunkZ));
        int maxZ = Math.min(box.maxZ(), SectionPos.sectionToBlockCoord(chunkZ, 15));
        int columnArea = (maxX - minX + 1) * (maxZ - minZ + 1);

        int changed = 0;
        int skipped = 0;

        for (int sectionY = SectionPos.blockToSectionCoord(box.minY()); sectionY <= SectionPos.blockToSectionCoord(box.maxY()); sectionY++) {
            int minY = Math.max(box.minY(), SectionPos.sectionToBlockCoord(sectionY));
            int maxY = Math.min(box.maxY(), SectionPos.sectionToBlockCoord(sectionY, 15));

            if (!storage.storingLightForSection(SectionPos.asLong(chunkX, sectionY, chunkZ))) {
                skipped += columnArea * (maxY - minY + 1);
                continue;
            }

            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int x = minX; x <= maxX; x++) {
                        long node = BlockPos.asLong(x, y, z);
                        int current = storage.getStoredLevel(node);
                        int target = function.compute(x, y, z, current);
                        if (target < 0) {
                            continue;
                        }
                        target = Math.min(target, LightEngine.MAX_LEVEL);
                        if (target != current) {
                            storage.setStoredLevel(node, target);
                            changedSections.set(sectionY - minLightSection);
                            changed++;
                        }
                    }
                }
            }
        }

        return new Result(changed, skipped);
    }

    /**
     * Writes the masked block and sky light levels of {@code section}.
     */
    public static void applySectionLight(ServerLevel level, SectionLight section) {
        SectionPos pos = SectionPos.of(section.sectionPos());
        BoundingBox box = new BoundingBox(pos.minBlockX(), pos.minBlockY(), pos.minBlockZ(), pos.maxBlockX(), pos.maxBlockY(), pos.maxBlockZ());
        BitSet mask = section.mask();

        apply(level, LightLayer.BLOCK, box, (x, y, z, current) ->
                mask.get(SectionLight.index(x, y, z)) ? section.block().get(x & 15, y & 15, z & 15) : -1);
        if (section.sky() != null && hasLayer(level, LightLayer.SKY)) {
            apply(level, LightLayer.SKY, box, (x, y, z, current) ->
                    mask.get(SectionLight.index(x, y, z)) ? section.sky().get(x & 15, y & 15, z & 15) : -1);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Light items

    public static boolean isSkyLightItem(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
    }

    public static int lightLevelOf(ItemStack stack) {
        BlockItemStateProperties properties = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        Integer level = properties.get(LightBlock.LEVEL);
        return level == null ? LightBlock.MAX_LEVEL : level;
    }

    public static ItemStack createLightItem(LightLayer layer, int level) {
        ItemStack stack = LightBlock.setLightOnStack(new ItemStack(Items.LIGHT), level);
        if (layer == LightLayer.SKY) {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            stack.set(DataComponents.ITEM_NAME, Component.literal("Sky Light " + level));
        } else {
            stack.set(DataComponents.ITEM_NAME, Component.literal("Block Light " + level));
        }
        return stack;
    }

    /**
     * Called instead of placing a light block while the light level manipulator is active.
     */
    public static void placeLightItem(ServerLevel level, BlockPos pos, ItemStack stack) {
        LightLayer layer = isSkyLightItem(stack) ? LightLayer.SKY : LightLayer.BLOCK;
        int lightLevel = lightLevelOf(stack);
        apply(level, layer, BoundingBox.fromCorners(pos, pos), (x, y, z, current) -> lightLevel);
    }
}
