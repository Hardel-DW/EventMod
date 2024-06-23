package com.hardel.eventmod.command;

import com.hardel.eventmod.event.EventData;
import com.hardel.eventmod.event.type.Finder;
import com.hardel.eventmod.event.type.FinderConfig;
import com.hardel.eventmod.event.type.FinderConfigData;
import com.hardel.eventmod.event.type.PlayerFinderData;
import com.hardel.eventmod.utils.BlockUtils;
import com.mojang.brigadier.CommandDispatcher;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class CommandEvent {
    public static final SuggestionProvider<ServerCommandSource> SUGGESTION_PROVIDER = (context, builder) -> {
        ReloadableRegistries.Lookup lookup = context.getSource().getServer().getReloadableRegistries();
        return CommandSource.suggestIdentifiers(lookup.getIds(RegistryKeys.LOOT_TABLE), builder);
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
                .then(literal("finder")
                        .then(literal("get")
                                .then(argument("target", EntityArgumentType.player())
                                        .then(argument("variant", StringArgumentType.word()).suggests((context, builder) -> suggestEventVariants(builder, Finder.key))
                                                .executes(context -> executeGetFinderCount(context.getSource(), EntityArgumentType.getPlayer(context, "target"), StringArgumentType.getString(context, "variant")))
                                        ).executes(context -> executeGetFinderCount(context.getSource(), EntityArgumentType.getPlayer(context, "target"), null))
                                )
                        )
                        .then(literal("reset")
                                .then(argument("target", EntityArgumentType.player())
                                        .then(argument("variant", StringArgumentType.word()).suggests((context, builder) -> suggestEventVariants(builder, Finder.key))
                                                .executes(context -> executeResetFinder(context.getSource(), EntityArgumentType.getPlayer(context, "target"), StringArgumentType.getString(context, "variant")))
                                        ).executes(context -> executeResetFinder(context.getSource(), EntityArgumentType.getPlayer(context, "target"), null))
                                )
                        )
                        .then(literal("config")
                                .then(argument("variant", StringArgumentType.word()).suggests((context, builder) -> suggestEventVariants(builder, Finder.key))
                                        .then(literal("found_message")
                                                .then(argument("found_message", TextArgumentType.text(commandRegistryAccess))
                                                        .executes(context -> executeFinderConfigModification(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "variant"),
                                                                FinderConfig.builder().foundMessage(TextArgumentType.getTextArgument(context, "found_message"))
                                                        ))
                                                )
                                        )
                                        .then(literal("already")
                                                .then(argument("already_found_message", TextArgumentType.text(commandRegistryAccess))
                                                        .executes(context -> executeFinderConfigModification(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "variant"),
                                                                FinderConfig.builder().alreadyFoundMessage(TextArgumentType.getTextArgument(context, "already_found_message"))
                                                        ))
                                                )
                                        )
                                        .then(literal("reward")
                                                .then(argument("reward", RegistryEntryArgumentType.lootTable(commandRegistryAccess))
                                                        .suggests(SUGGESTION_PROVIDER).executes(context -> executeFinderConfigModification(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "variant"),
                                                                FinderConfig.builder().reward(RegistryEntryArgumentType.getLootTable(context, "reward").getIdAsString())
                                                        ))
                                                )
                                        )
                                        .then(literal("sound")
                                                .then(argument("sound", IdentifierArgumentType.identifier())
                                                        .suggests(SuggestionProviders.AVAILABLE_SOUNDS)
                                                        .executes(context -> executeFinderConfigModification(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "variant"),
                                                                FinderConfig.builder().sound(IdentifierArgumentType.getIdentifier(context, "sound"))
                                                        ))
                                                )
                                        )
                                )
                        )
                        .then(literal("create")
                                .then(argument("name", StringArgumentType.word())
                                        .then(argument("uuid", BlockPosArgumentType.blockPos())
                                                .then(argument("reward", RegistryEntryArgumentType.lootTable(commandRegistryAccess))
                                                        .suggests(SUGGESTION_PROVIDER).executes(context -> executeCreateFinder(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "name"),
                                                                BlockPosArgumentType.getLoadedBlockPos(context, "uuid"),
                                                                RegistryEntryArgumentType.getLootTable(context, "reward")
                                                        ))
                                                )
                                        )
                                )
                        )
                        .then(literal("remove")
                                .then(argument("variant", StringArgumentType.word()).suggests((context, builder) -> suggestEventVariants(builder, Finder.key))
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

    private static int executeGetFinderCount(ServerCommandSource source, ServerPlayerEntity targetPlayer, String variant) {
        if (targetPlayer == null) {
            source.sendError(Text.of("Player not found"));
            return 0;
        }

        PlayerFinderData playerFinderData = PlayerFinderData.getPlayerData(targetPlayer);
        int dirtDestroyedCount = playerFinderData.getFoundBlocksCountByVariant(variant);

        source.sendFeedback(() -> Text.of(targetPlayer.getName().getString() + " has destroyed " + dirtDestroyedCount + " dirt blocks"), false);

        return dirtDestroyedCount;
    }

    private static int executeResetFinder(ServerCommandSource source, ServerPlayerEntity targetPlayer, String variant) {
        if (targetPlayer == null) {
            source.sendError(Text.of("Player not found"));
            return 0;
        }

        PlayerFinderData.getPlayerData(targetPlayer).resetByVariant(variant, targetPlayer.getUuid());
        source.sendFeedback(() -> Text.of("Reset dirt destroyed count for " + targetPlayer.getName().getString()), false);
        return 1;
    }

    private static int executeCreateFinder(ServerCommandSource source, String name, BlockPos block, RegistryEntry<LootTable> reward) {
        UUID uuid = BlockUtils.getHeadUuid(source.getWorld(), block);
        FinderConfig config = FinderConfig.builder()
                .uuid(uuid)
                .variant(name)
                .reward(reward.getIdAsString())
                .build();

        boolean isCreated = FinderConfigData.getConfigData(Finder.key).addFinderConfig(config);

        source.sendFeedback(() -> Text.of("Created new finder config: " + name), false);
        return isCreated ? 1 : 0;
    }

    private static int executeRemoveFinder(ServerCommandSource source, String name) {
        boolean isRemoved = FinderConfigData
                .getConfigData(Finder.key)
                .removeFinderConfig(name);

        source.sendFeedback(() -> Text.of("Removed finder config: " + name), false);
        return isRemoved ? 1 : 0;
    }

    private static int executeFinderConfigModification(ServerCommandSource source, String name, FinderConfig.Builder builder) {
        boolean isUpdated = FinderConfigData
                .getConfigData(Finder.key)
                .modifyFinderConfig(name, builder.build());

        source.sendFeedback(() -> Text.of("Modified finder config: " + name), false);
        return isUpdated ? 1 : 0;
    }

}
