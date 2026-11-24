package net.cosmosmc.carpetaddons.lightmanipulator;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.cosmosmc.carpetaddons.settings.CosmosCarpetAddonsSettings;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.lighting.LightEngine;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * {@code /lightmanipulator}, available while the {@code lightLevelManipulator} rule is enabled.
 * <pre>
 * /lightmanipulator give (block|sky) [level] [targets]
 * /lightmanipulator give portal (x|z) [blockLight] [skyLight] [targets]     (-1 = keep the current level)
 * /lightmanipulator fill &lt;from&gt; &lt;to&gt; (block|sky) &lt;level&gt;
 * /lightmanipulator generate &lt;from&gt; &lt;to&gt; (block|sky) [normalized|raw|offset|center] &lt;expression&gt;
 * </pre>
 */
public final class LightManipulatorCommand {
    public static final int MAX_VOLUME = 1 << 24;

    private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType(
            (max, count) -> Component.literal("Too many positions in the specified area (maximum " + max + ", specified " + count + ")")
    );
    private static final SimpleCommandExceptionType ERROR_NOT_LOADED = new SimpleCommandExceptionType(
            Component.literal("The specified area is not fully loaded")
    );
    private static final SimpleCommandExceptionType ERROR_NO_SKYLIGHT = new SimpleCommandExceptionType(
            Component.literal("This dimension has no sky light")
    );
    private static final DynamicCommandExceptionType ERROR_EXPRESSION = new DynamicCommandExceptionType(
            message -> Component.literal("Invalid expression: " + message)
    );

    private LightManipulatorCommand() {
    }

    /**
     * How the {@code x}, {@code y} and {@code z} variables of an expression are computed, similar to WorldEdit's //generate.
     */
    public enum CoordinateMode {
        /** -1..1 across the area, 0 at its center (WorldEdit default) */
        NORMALIZED,
        /** world coordinates (WorldEdit -r) */
        RAW,
        /** relative to the command's execution position (WorldEdit -o) */
        OFFSET,
        /** relative to the center of the area, unscaled (WorldEdit -c) */
        CENTER;

        String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var command = literal("lightmanipulator")
                .requires(source -> CosmosCarpetAddonsSettings.lightLevelManipulator
                        && Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source));

        var give = literal("give");
        var fillTo = argument("to", BlockPosArgument.blockPos());
        var generateTo = argument("to", BlockPosArgument.blockPos());

        for (LightLayer layer : List.of(LightLayer.BLOCK, LightLayer.SKY)) {
            String name = layerName(layer);

            give.then(literal(name)
                    .executes(c -> give(c.getSource(), layer, 0, List.of(c.getSource().getPlayerOrException())))
                    .then(argument("level", IntegerArgumentType.integer(0, LightEngine.MAX_LEVEL))
                            .executes(c -> give(c.getSource(), layer, IntegerArgumentType.getInteger(c, "level"),
                                    List.of(c.getSource().getPlayerOrException())))
                            .then(argument("targets", EntityArgument.players())
                                    .executes(c -> give(c.getSource(), layer, IntegerArgumentType.getInteger(c, "level"),
                                            EntityArgument.getPlayers(c, "targets"))))));

            fillTo.then(literal(name)
                    .then(argument("level", IntegerArgumentType.integer(0, LightEngine.MAX_LEVEL))
                            .executes(c -> fill(c, layer, IntegerArgumentType.getInteger(c, "level")))));

            var generateLayer = literal(name).then(expressionArgument(layer, CoordinateMode.NORMALIZED));
            for (CoordinateMode mode : CoordinateMode.values()) {
                generateLayer.then(literal(mode.id()).then(expressionArgument(layer, mode)));
            }
            generateTo.then(generateLayer);
        }

        var portal = literal("portal");
        for (Direction.Axis axis : List.of(Direction.Axis.X, Direction.Axis.Z)) {
            portal.then(literal(axis.getSerializedName())
                    .executes(c -> givePortal(c, axis, 0, PortalItems.KEEP, false))
                    .then(argument("blockLight", IntegerArgumentType.integer(PortalItems.KEEP, LightEngine.MAX_LEVEL))
                            .executes(c -> givePortal(c, axis, IntegerArgumentType.getInteger(c, "blockLight"), PortalItems.KEEP, false))
                            .then(argument("skyLight", IntegerArgumentType.integer(PortalItems.KEEP, LightEngine.MAX_LEVEL))
                                    .executes(c -> givePortal(c, axis, IntegerArgumentType.getInteger(c, "blockLight"),
                                            IntegerArgumentType.getInteger(c, "skyLight"), false))
                                    .then(argument("targets", EntityArgument.players())
                                            .executes(c -> givePortal(c, axis, IntegerArgumentType.getInteger(c, "blockLight"),
                                                    IntegerArgumentType.getInteger(c, "skyLight"), true))))));
        }
        give.then(portal);

        command.then(give);
        command.then(literal("fill").then(argument("from", BlockPosArgument.blockPos()).then(fillTo)));
        command.then(literal("generate").then(argument("from", BlockPosArgument.blockPos()).then(generateTo)));

        dispatcher.register(command);
    }

    private static ArgumentBuilder<CommandSourceStack, ?> expressionArgument(LightLayer layer, CoordinateMode mode) {
        return argument("expression", StringArgumentType.greedyString())
                .executes(c -> generate(c, layer, mode, StringArgumentType.getString(c, "expression")));
    }

    private static String layerName(LightLayer layer) {
        return layer == LightLayer.SKY ? "sky" : "block";
    }

    private static int give(CommandSourceStack source, LightLayer layer, int level, Collection<ServerPlayer> targets) {
        return giveItems(source, () -> LightManipulator.createLightItem(layer, level), layerName(layer) + " light " + level + " item", targets);
    }

    private static int givePortal(CommandContext<CommandSourceStack> c, Direction.Axis axis, int blockLight, int skyLight, boolean hasTargets) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = hasTargets ? EntityArgument.getPlayers(c, "targets") : List.of(c.getSource().getPlayerOrException());
        return giveItems(c.getSource(), () -> PortalItems.create(axis, blockLight, skyLight),
                "nether portal item (" + axis.getSerializedName() + ")", targets);
    }

    private static int giveItems(CommandSourceStack source, Supplier<ItemStack> factory, String what, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            ItemStack stack = factory.get();
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            player.containerMenu.broadcastChanges();
        }

        if (targets.size() == 1) {
            String name = targets.iterator().next().getName().getString();
            source.sendSuccess(() -> Component.literal("Gave a " + what + " to " + name), true);
        } else {
            source.sendSuccess(() -> Component.literal("Gave a " + what + " to " + targets.size() + " players"), true);
        }
        return targets.size();
    }

    private static BoundingBox area(CommandContext<CommandSourceStack> c, LightLayer layer) throws CommandSyntaxException {
        ServerLevel level = c.getSource().getLevel();
        if (!LightManipulator.hasLayer(level, layer)) {
            throw ERROR_NO_SKYLIGHT.create();
        }

        BoundingBox box = BoundingBox.fromCorners(
                BlockPosArgument.getLoadedBlockPos(c, "from"),
                BlockPosArgument.getLoadedBlockPos(c, "to")
        );

        long volume = (long) box.getXSpan() * box.getYSpan() * box.getZSpan();
        if (volume > MAX_VOLUME) {
            throw ERROR_AREA_TOO_LARGE.create(MAX_VOLUME, volume);
        }

        for (int chunkX = SectionPos.blockToSectionCoord(box.minX()); chunkX <= SectionPos.blockToSectionCoord(box.maxX()); chunkX++) {
            for (int chunkZ = SectionPos.blockToSectionCoord(box.minZ()); chunkZ <= SectionPos.blockToSectionCoord(box.maxZ()); chunkZ++) {
                if (!level.getChunkSource().hasChunk(chunkX, chunkZ)) {
                    throw ERROR_NOT_LOADED.create();
                }
            }
        }
        return box;
    }

    private static int fill(CommandContext<CommandSourceStack> c, LightLayer layer, int lightLevel) throws CommandSyntaxException {
        BoundingBox box = area(c, layer);
        run(c.getSource(), layer, box, (x, y, z, current) -> lightLevel);
        return volume(box);
    }

    private static int generate(CommandContext<CommandSourceStack> c, LightLayer layer, CoordinateMode mode, String source) throws CommandSyntaxException {
        BoundingBox box = area(c, layer);

        LightExpression expression;
        try {
            expression = LightExpression.parse(source);
        } catch (LightExpression.ParseException e) {
            throw ERROR_EXPRESSION.create(e.getMessage());
        }

        double centerX = (box.minX() + box.maxX()) / 2.0;
        double centerY = (box.minY() + box.maxY()) / 2.0;
        double centerZ = (box.minZ() + box.maxZ()) / 2.0;

        double offsetX, offsetY, offsetZ;
        double scaleX = 1, scaleY = 1, scaleZ = 1;
        switch (mode) {
            case RAW -> {
                offsetX = offsetY = offsetZ = 0;
            }
            case OFFSET -> {
                BlockPos origin = BlockPos.containing(c.getSource().getPosition());
                offsetX = origin.getX();
                offsetY = origin.getY();
                offsetZ = origin.getZ();
            }
            case CENTER -> {
                offsetX = centerX;
                offsetY = centerY;
                offsetZ = centerZ;
            }
            default -> {
                offsetX = centerX;
                offsetY = centerY;
                offsetZ = centerZ;
                scaleX = Math.max(box.maxX() - centerX, 1);
                scaleY = Math.max(box.maxY() - centerY, 1);
                scaleZ = Math.max(box.maxZ() - centerZ, 1);
            }
        }

        double ox = offsetX, oy = offsetY, oz = offsetZ, sx = scaleX, sy = scaleY, sz = scaleZ;
        run(c.getSource(), layer, box, (x, y, z, current) -> {
            double[] vars = new double[LightExpression.VAR_COUNT];
            vars[LightExpression.VAR_X] = (x - ox) / sx;
            vars[LightExpression.VAR_Y] = (y - oy) / sy;
            vars[LightExpression.VAR_Z] = (z - oz) / sz;
            vars[LightExpression.VAR_CUR] = current;
            double result = expression.evaluate(vars);
            if (Double.isNaN(result) || result < 0) {
                return -1;
            }
            return (int) Math.round(Math.min(result, LightEngine.MAX_LEVEL));
        });
        return volume(box);
    }

    private static int volume(BoundingBox box) {
        return box.getXSpan() * box.getYSpan() * box.getZSpan();
    }

    private static void run(CommandSourceStack source, LightLayer layer, BoundingBox box, LightFunction function) {
        MinecraftServer server = source.getServer();
        String name = layerName(layer);
        source.sendSuccess(() -> Component.literal("Setting " + name + " light of " + volume(box) + " positions..."), false);

        LightManipulator.apply(source.getLevel(), layer, box, function).whenComplete((result, error) -> server.execute(() -> {
            if (error != null) {
                source.sendFailure(Component.literal("Failed to set " + name + " light: " + error.getMessage()));
                return;
            }
            StringBuilder message = new StringBuilder("Changed the " + name + " light of " + result.changed() + " positions");
            if (result.skipped() > 0) {
                message.append(" (").append(result.skipped()).append(" positions skipped: no light data stored there)");
            }
            source.sendSuccess(() -> Component.literal(message.toString()), true);
        }));
    }
}
