package com.hardel.eventmod.command;

import com.hardel.eventmod.event.EventData;
import com.hardel.eventmod.event.elytra.*;
import com.hardel.eventmod.event.finder.FinderConfigData;
import com.hardel.eventmod.event.finder.FinderPlayerData;
import com.hardel.eventmod.utils.BlockUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.*;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.ReloadableRegistries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class CommandEvent {
    private static final String finderKey = "finder";
    private static final String ElytraKey = "elytra";

    public static final SuggestionProvider<ServerCommandSource> SUGGESTION_PROVIDER = (context, builder) -> {
        ReloadableRegistries.Lookup lookup = context.getSource().getServer().getReloadableRegistries();
        return CommandSource.suggestIdentifiers(lookup.getIds(RegistryKeys.LOOT_TABLE), builder);
    };

    public static final SuggestionProvider<ServerCommandSource> SUGGESTION_PROVIDER_WIN_CONDITION = (context, builder) -> {
        for (WinCondition element : WinCondition.values()) {
            builder.suggest(element.name());
        }

        return builder.buildFuture();
    };

    public static final SuggestionProvider<ServerCommandSource> SUGGESTION_PROVIDER_PORTAL_TYPE = (context, builder) -> {
        for (PortalType type : PortalType.values()) {
            builder.suggest(type.name());
        }

        return builder.buildFuture();
    };

    public static CompletableFuture<Suggestions> suggestEventVariants(SuggestionsBuilder builder, String event) {
        List<String> eventVariants = EventData.getAllVariantNameFromEvent(event);

        for (String variant : eventVariants) {
            builder.suggest(variant);
        }

        return builder.buildFuture();
    }


    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess) {
        dispatcher.register(literal("event")
                .requires(source -> source.hasPermissionLevel(2))
                .then(literal(ElytraKey)
                        .then(literal("config")
                                .then(argument("variant", StringArgumentType.word()).suggests((context, builder) -> suggestEventVariants(builder, ElytraKey))
                                        .then(literal("fail_message")
                                                .then(argument("fail_message", TextArgumentType.text(commandRegistryAccess))
                                                        .executes(context -> executeElytraConfigModification(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "variant"),
                                                                ElytraConfigData.builder().failMessage(TextArgumentType.getTextArgument(context, "fail_message"))
                                                        ))
                                                )
                                        )
                                        .then(literal("win_condition")
                                                .then(argument("win_condition", StringArgumentType.word()).suggests(SUGGESTION_PROVIDER_WIN_CONDITION)
                                                        .executes(context -> executeElytraConfigModification(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "variant"),
                                                                ElytraConfigData.builder().winCondition(WinCondition.valueOf(StringArgumentType.getString(context, "win_condition")))
                                                        ))
                                                )
                                        )
                                        .then(literal("start")
                                                .then(argument("start", BlockPosArgumentType.blockPos())
                                                        .executes(context -> executeElytraConfigModification(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "variant"),
                                                                ElytraConfigData.builder().start(BlockPosArgumentType.getLoadedBlockPos(context, "start"))
                                                        ))
                                                )
                                        )
                                        .then(literal("end")
                                                .then(argument("end", BlockPosArgumentType.blockPos())
                                                        .executes(context -> executeElytraConfigModification(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "variant"),
                                                                ElytraConfigData.builder().end(BlockPosArgumentType.getLoadedBlockPos(context, "end"))
                                                        ))
                                                )
                                        )
                                )
                        )
                        .then(literal("portal")
                                .then(argument("variant", StringArgumentType.word()).suggests((context, builder) -> suggestEventVariants(builder, ElytraKey))
                                        .then(literal("create")
                                                .then(argument("start", BlockPosArgumentType.blockPos())
                                                        .then(argument("end", BlockPosArgumentType.blockPos())
                                                                .then(argument("index", IntegerArgumentType.integer(0, 2000))
                                                                        .executes(context -> executeCreateElytraCheckpoint(
                                                                                context.getSource(),
                                                                                StringArgumentType.getString(context, "variant"),
                                                                                BlockPosArgumentType.getLoadedBlockPos(context, "start"),
                                                                                BlockPosArgumentType.getLoadedBlockPos(context, "end"),
                                                                                IntegerArgumentType.getInteger(context, "index"),
                                                                                PortalType.CHECKPOINT
                                                                        ))
                                                                        .then(argument("type", StringArgumentType.word()).suggests(SUGGESTION_PROVIDER_PORTAL_TYPE)
                                                                                .executes(context -> executeCreateElytraCheckpoint(
                                                                                        context.getSource(),
                                                                                        StringArgumentType.getString(context, "variant"),
                                                                                        BlockPosArgumentType.getLoadedBlockPos(context, "start"),
                                                                                        BlockPosArgumentType.getLoadedBlockPos(context, "end"),
                                                                                        IntegerArgumentType.getInteger(context, "index"),
                                                                                        PortalType.valueOf(StringArgumentType.getString(context, "type"))
                                                                                ))
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                        .then(literal("remove")
                                                .then(argument("index", IntegerArgumentType.integer())
                                                        .executes(context -> executeRemoveElytraCheckpoint(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "variant"),
                                                                IntegerArgumentType.getInteger(context, "index")
                                                        ))
                                                )
                                        )
                                        .then(literal("modify")
                                                .then(argument("index", IntegerArgumentType.integer())
                                                        .then(literal("end")
                                                                .then(argument("end", BlockPosArgumentType.blockPos())
                                                                        .executes(context -> executeElytraCheckpointConfigModification(
                                                                                context.getSource(),
                                                                                StringArgumentType.getString(context, "variant"),
                                                                                IntegerArgumentType.getInteger(context, "index"),
                                                                                CheckpointData.builder()
                                                                                        .end(BlockPosArgumentType.getLoadedBlockPos(context, "end"))
                                                                                        .build()
                                                                        ))
                                                                )
                                                        )
                                                        .then(literal("start")
                                                                .then(argument("start", BlockPosArgumentType.blockPos())
                                                                        .executes(context -> executeElytraCheckpointConfigModification(
                                                                                context.getSource(),
                                                                                StringArgumentType.getString(context, "variant"),
                                                                                IntegerArgumentType.getInteger(context, "index"),
                                                                                CheckpointData.builder()
                                                                                        .start(BlockPosArgumentType.getLoadedBlockPos(context, "start"))
                                                                                        .build()
                                                                        ))
                                                                )
                                                        )
                                                        .then(literal("sound")
                                                                .then(argument("sound", IdentifierArgumentType.identifier())
                                                                        .suggests(SuggestionProviders.AVAILABLE_SOUNDS)
                                                                        .executes(context -> executeElytraCheckpointConfigModification(
                                                                                context.getSource(),
                                                                                StringArgumentType.getString(context, "variant"),
                                                                                IntegerArgumentType.getInteger(context, "index"),
                                                                                CheckpointData.builder()
                                                                                        .sound(IdentifierArgumentType.getIdentifier(context, "sound"))
                                                                                        .build()
                                                                        ))
                                                                )
                                                        )
                                                        .then(literal("type")
                                                                .then(argument("type", StringArgumentType.word()).suggests(SUGGESTION_PROVIDER_PORTAL_TYPE)
                                                                        .executes(context -> executeElytraCheckpointConfigModification(
                                                                                context.getSource(),
                                                                                StringArgumentType.getString(context, "variant"),
                                                                                IntegerArgumentType.getInteger(context, "index"),
                                                                                CheckpointData.builder()
                                                                                        .type(PortalType.valueOf(StringArgumentType.getString(context, "type")))
                                                                                        .build()
                                                                        ))
                                                                )
                                                        )
                                                        .then(literal("message")
                                                                .then(argument("success_message", TextArgumentType.text(commandRegistryAccess))
                                                                        .executes(context -> executeElytraCheckpointConfigModification(
                                                                                context.getSource(),
                                                                                StringArgumentType.getString(context, "variant"),
                                                                                IntegerArgumentType.getInteger(context, "index"),
                                                                                CheckpointData.builder()
                                                                                        .successMessage(TextArgumentType.getTextArgument(context, "success_message"))
                                                                                        .build()
                                                                        ))
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(literal("create")
                                .then(argument("name", StringArgumentType.word())
                                        .then(argument("start", BlockPosArgumentType.blockPos())
                                                .then(argument("end", BlockPosArgumentType.blockPos())
                                                        .executes(context -> executeCreateElytra(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "name"),
                                                                BlockPosArgumentType.getLoadedBlockPos(context, "start"),
                                                                BlockPosArgumentType.getLoadedBlockPos(context, "end")
                                                        ))
                                                )
                                        )
                                )
                        )
                        .then(literal("delete")
                                .then(argument("variant", StringArgumentType.word()).suggests((context, builder) -> suggestEventVariants(builder, ElytraKey))
                                        .executes(context -> executeRemoveElytra(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "variant")
                                        ))
                                )
                        )
                        .then(literal("players")
                                .then(literal("rank")
                                        .then(argument("variant", StringArgumentType.word()).suggests((context, builder) -> suggestEventVariants(builder, ElytraKey))
                                                .then(argument("howMany", IntegerArgumentType.integer())
                                                        .executes(context -> executeElytraRanking(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "variant"),
                                                                IntegerArgumentType.getInteger(context, "howMany")
                                                        ))
                                                )
                                        )
                                )
                                .then(literal("join")
                                        .then(argument("target", EntityArgumentType.player())
                                                .then(argument("variant", StringArgumentType.word()).suggests((context, builder) -> suggestEventVariants(builder, ElytraKey))
                                                        .executes(context -> executeGetElytraParticipating(context.getSource(), EntityArgumentType.getPlayer(context, "target"), StringArgumentType.getString(context, "variant"), true))
                                                )
                                        )
                                )
                                .then(literal("leave")
                                        .then(argument("target", EntityArgumentType.player())
                                                .then(argument("variant", StringArgumentType.word()).suggests((context, builder) -> suggestEventVariants(builder, ElytraKey))
                                                        .executes(context -> executeGetElytraParticipating(context.getSource(), EntityArgumentType.getPlayer(context, "target"), StringArgumentType.getString(context, "variant"), false))
                                                )
                                        )
                                )
                                .then(literal("get")
                                        .then(argument("target", EntityArgumentType.player())
                                                .then(argument("variant", StringArgumentType.word()).suggests((context, builder) -> suggestEventVariants(builder, ElytraKey))
                                                        .executes(context -> executeGetElytraCheckpoint(context.getSource(), EntityArgumentType.getPlayer(context, "target"), StringArgumentType.getString(context, "variant")))
                                                ).executes(context -> executeGetElytraCheckpoint(context.getSource(), EntityArgumentType.getPlayer(context, "target"), null))
                                        )
                                )
                                .then(literal("reset")
                                        .then(argument("target", EntityArgumentType.player())
                                                .then(argument("variant", StringArgumentType.word()).suggests((context, builder) -> suggestEventVariants(builder, ElytraKey))
                                                        .executes(context -> executeResetElytraCheckpoint(context.getSource(), EntityArgumentType.getPlayer(context, "target"), StringArgumentType.getString(context, "variant")))
                                                ).executes(context -> executeResetElytraCheckpoint(context.getSource(), EntityArgumentType.getPlayer(context, "target"), null))
                                        )
                                )
                        )
                )
                .then(literal(finderKey)
                        .then(literal("get")
                                .then(argument("target", EntityArgumentType.player())
                                        .then(argument("variant", StringArgumentType.word()).suggests((context, builder) -> suggestEventVariants(builder, finderKey))
                                                .executes(context -> executeGetFinderCount(context.getSource(), EntityArgumentType.getPlayer(context, "target"), StringArgumentType.getString(context, "variant")))
                                        ).executes(context -> executeGetFinderCount(context.getSource(), EntityArgumentType.getPlayer(context, "target"), null))
                                )
                        )
                        .then(literal("reset")
                                .then(argument("target", EntityArgumentType.player())
                                        .then(argument("variant", StringArgumentType.word()).suggests((context, builder) -> suggestEventVariants(builder, finderKey))
                                                .executes(context -> executeResetFinder(context.getSource(), EntityArgumentType.getPlayer(context, "target"), StringArgumentType.getString(context, "variant")))
                                        ).executes(context -> executeResetFinder(context.getSource(), EntityArgumentType.getPlayer(context, "target"), null))
                                )
                        )
                        .then(literal("config")
                                .then(argument("variant", StringArgumentType.word()).suggests((context, builder) -> suggestEventVariants(builder, finderKey))
                                        .then(literal("found_message")
                                                .then(argument("found_message", TextArgumentType.text(commandRegistryAccess))
                                                        .executes(context -> executeFinderConfigModification(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "variant"),
                                                                FinderConfigData.builder().foundMessage(TextArgumentType.getTextArgument(context, "found_message"))
                                                        ))
                                                )
                                        )
                                        .then(literal("already_found_message")
                                                .then(argument("already_found_message", TextArgumentType.text(commandRegistryAccess))
                                                        .executes(context -> executeFinderConfigModification(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "variant"),
                                                                FinderConfigData.builder().alreadyFoundMessage(TextArgumentType.getTextArgument(context, "already_found_message"))
                                                        ))
                                                )
                                        )
                                        .then(literal("reward")
                                                .then(argument("reward", RegistryEntryArgumentType.lootTable(commandRegistryAccess))
                                                        .suggests(SUGGESTION_PROVIDER).executes(context -> executeFinderConfigModification(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "variant"),
                                                                FinderConfigData.builder().reward(RegistryEntryArgumentType.getLootTable(context, "reward").getIdAsString())
                                                        ))
                                                )
                                        )
                                        .then(literal("sound")
                                                .then(argument("sound", IdentifierArgumentType.identifier())
                                                        .suggests(SuggestionProviders.AVAILABLE_SOUNDS)
                                                        .executes(context -> executeFinderConfigModification(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "variant"),
                                                                FinderConfigData.builder().sound(IdentifierArgumentType.getIdentifier(context, "sound"))
                                                        ))
                                                )
                                        )

                                )
                        )
                        .then(literal("create")
                                .then(argument("variant", StringArgumentType.word())
                                        .then(argument("uuid", BlockPosArgumentType.blockPos())
                                                .then(argument("reward", RegistryEntryArgumentType.lootTable(commandRegistryAccess))
                                                        .suggests(SUGGESTION_PROVIDER).executes(context -> executeCreateFinder(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "variant"),
                                                                BlockPosArgumentType.getLoadedBlockPos(context, "uuid"),
                                                                RegistryEntryArgumentType.getLootTable(context, "reward")
                                                        ))
                                                )
                                        )
                                )
                        )
                        .then(literal("delete")
                                .then(argument("variant", StringArgumentType.word()).suggests((context, builder) -> suggestEventVariants(builder, finderKey))
                                        .executes(context -> executeRemoveFinder(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "variant")
                                                )
                                        )
                                )
                        )
                )
        );
    }

    // Finder Event
    private static int executeGetFinderCount(ServerCommandSource source, ServerPlayerEntity targetPlayer, String variant) {
        int dirtDestroyedCount = FinderPlayerData.getFoundBlocksCountByVariant(variant, targetPlayer.getUuid());
        source.sendFeedback(() -> Text.of(targetPlayer.getName().getString() + " has destroyed " + dirtDestroyedCount + " dirt blocks"), false);
        return dirtDestroyedCount;
    }

    private static int executeResetFinder(ServerCommandSource source, ServerPlayerEntity targetPlayer, String variant) {
        FinderPlayerData.resetByVariant(variant, targetPlayer.getUuid());
        source.sendFeedback(() -> Text.of("Reset dirt destroyed count for " + targetPlayer.getName().getString()), false);
        return 1;
    }

    private static int executeCreateFinder(ServerCommandSource source, String variant, BlockPos block, RegistryEntry<LootTable> reward) {
        UUID uuid = BlockUtils.getHeadUuid(source.getWorld(), block);
        FinderConfigData config = FinderConfigData.builder().withDefaults(variant, uuid).reward(reward.getIdAsString()).build();

        boolean isCreated = FinderConfigData.addNewConfig(config);
        source.sendFeedback(() -> Text.of("Created new finder config: " + variant), false);
        return isCreated ? 1 : 0;
    }

    private static int executeRemoveFinder(ServerCommandSource source, String name) {
        boolean isRemoved = FinderConfigData.removeExistingConfig(name);
        source.sendFeedback(() -> Text.of("Removed finder config: " + name), false);
        return isRemoved ? 1 : 0;
    }

    private static int executeFinderConfigModification(ServerCommandSource source, String variant, FinderConfigData.Builder builder) {
        boolean isUpdated = FinderConfigData.modifyExistingConfig(variant, builder.build());
        source.sendFeedback(() -> Text.of("Modified finder config: " + variant), false);
        return isUpdated ? 1 : 0;
    }

    // Elytra Event Player
    private static int executeGetElytraCheckpoint(ServerCommandSource source, ServerPlayerEntity targetPlayer, String variant) {
        Text text = ElytraPlayerData.getPlayerDetail(variant, targetPlayer.getUuid());
        source.sendFeedback(() -> Text.of(text), false);
        return 1;
    }

    private static int executeResetElytraCheckpoint(ServerCommandSource source, ServerPlayerEntity targetPlayer, String variant) {
        ElytraPlayerData.resetByVariant(variant, targetPlayer.getUuid());
        source.sendFeedback(() -> Text.of("Reset checkpoint count for " + targetPlayer.getName().getString()), false);
        return 1;
    }

    private static int executeGetElytraParticipating(ServerCommandSource source, ServerPlayerEntity targetPlayer, String variant, boolean isJoin) {
        boolean isParticipating = ElytraPlayerData.setParticipating(variant, targetPlayer.getUuid(), isJoin);
        source.sendFeedback(() -> Text.of((isParticipating ? "Joined" : "Left") + " elytra event: " + variant), false);
        return isParticipating ? 1 : 0;
    }

    // Elytra Config
    private static int executeCreateElytra(ServerCommandSource source, String name, BlockPos start, BlockPos end) {
        boolean isCreated = ElytraConfigData.addNewConfig(ElytraConfigData.builder().withDefaults(name).start(start).end(end).build());
        source.sendFeedback(() -> Text.of((isCreated ? "Created" : "Failed to create") + " new elytra config: " + name), false);
        return isCreated ? 1 : 0;
    }

    private static int executeElytraConfigModification(ServerCommandSource source, String name, ElytraConfigData.Builder builder) {
        boolean isUpdated = ElytraConfigData.modifyExistingConfig(name, builder.build());
        source.sendFeedback(() -> Text.of(isUpdated ? "Modified" : "Failed to modify" + " elytra config: " + name), false);
        return isUpdated ? 1 : 0;
    }

    private static int executeRemoveElytra(ServerCommandSource source, String name) {
        boolean isRemoved = ElytraConfigData.removeExistingConfig(name);
        source.sendFeedback(() -> Text.of(isRemoved ? "Removed" : "Failed to remove" + " elytra config: " + name), false);
        return isRemoved ? 1 : 0;
    }

    // Elytra Checkpoint
    private static int executeCreateElytraCheckpoint(ServerCommandSource source, String name, BlockPos start, BlockPos end, int index, PortalType type) {
        boolean isCreated = ElytraConfigData.addCheckpoint(name, CheckpointData.builder().withDefaults().start(start).end(end).index(index).type(type).build());
        if (isCreated) {
            source.sendFeedback(() -> Text.of("Ceated new elytra checkpoint: " + name), false);
        } else {
            source.sendFeedback(() -> Text.of("The index already exist, please change the index or use the modify command"), false);
        }

        return isCreated ? 1 : 0;
    }

    private static int executeRemoveElytraCheckpoint(ServerCommandSource source, String name, int index) {
        boolean isRemoved = ElytraConfigData.removeCheckpoint(name, index);
        source.sendFeedback(() -> Text.of(isRemoved ? "Removed" : "Failed to remove" + " elytra checkpoint: " + name), false);
        return isRemoved ? 1 : 0;
    }

    private static int executeElytraCheckpointConfigModification(ServerCommandSource source, String name, int index, CheckpointData checkpointData) {
        boolean isUpdated = ElytraConfigData.modifyCheckpoint(name, index, checkpointData);
        source.sendFeedback(() -> Text.of(isUpdated ? "Modified" : "Failed to modify" + " elytra checkpoint: " + name), false);
        return isUpdated ? 1 : 0;
    }

    // Elytra ranking
    private static int executeElytraRanking(ServerCommandSource source, String variant, int howMany) {
        List<UUID> topRanking = ElytraPlayerData.getRanking(variant, howMany);
        for (int i = 0; i < topRanking.size(); i++) {
            UUID player = topRanking.get(i);


            ElytraPlayerData data = ElytraPlayerData.getInstance(player, variant);
            String name = Objects.requireNonNull(source.getServer().getPlayerManager().getPlayer(player)).getName().getString();
            int second = (data.finishTicks() - data.startTicks()) / 20;
            int finalI = i;

            source.sendFeedback(() -> Text.of("Position " + (finalI + 1) + ", in " + second + " seconds, for player " + name), false);
        }

        return topRanking.size();
    }
}
