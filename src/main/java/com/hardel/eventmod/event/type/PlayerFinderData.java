package com.hardel.eventmod.event.type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hardel.eventmod.event.EventData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record PlayerFinderData(List<FinderBlock> foundBlocks) {
    private record FinderBlock(BlockPos pos, String variant) {
    }

    /**
     * Get the player data from the file
     *
     * @param targetPlayer the player to get the data from
     * @return the player data
     */
    public static PlayerFinderData getPlayerData(PlayerEntity targetPlayer) {
        JsonArray finderArray = EventData.loadPlayerEventData(targetPlayer.getUuid(), Finder.key);
        List<FinderBlock> blocks = new ArrayList<>();

        for (JsonElement element : finderArray) {
            JsonObject blockData = element.getAsJsonObject();
            String variant = blockData.get("variant").getAsString();
            JsonArray position = blockData.getAsJsonArray("position");

            BlockPos pos = new BlockPos(position.get(0).getAsInt(), position.get(1).getAsInt(), position.get(2).getAsInt());
            blocks.add(new FinderBlock(pos, variant));
        }

        return new PlayerFinderData(blocks);
    }

    /**
     * Save the player data to the file
     */
    public void savePlayerData(UUID playerUuid) {
        JsonArray finderArray = new JsonArray();

        for (FinderBlock block : foundBlocks) {
            JsonObject blockData = new JsonObject();
            JsonArray position = new JsonArray();
            position.add(block.pos().getX());
            position.add(block.pos().getY());
            position.add(block.pos().getZ());

            blockData.add("position", position);
            blockData.addProperty("variant", block.variant());
            finderArray.add(blockData);
        }

        EventData.savePlayerEventData(playerUuid, Finder.key, finderArray);
    }

    /**
     * Add a block found by the player
     *
     * @param pos     the position of the block
     * @param variant the variant of the block
     * @return true if the block is added, false if the block is already found
     */
    public boolean tryAddNewEntry(BlockPos pos, String variant, UUID playerUuid) {
        FinderBlock finderBlock = new FinderBlock(pos, variant);
        if (!foundBlocks.contains(finderBlock)) {
            foundBlocks.add(finderBlock);
            savePlayerData(playerUuid);
            return true;
        }

        return false;
    }

    /**
     * Get the count of found blocks by variant, and if variant is null, return the total count of found blocks
     *
     * @param variant the variant to count
     * @return the count of found blocks
     */
    public int getFoundBlocksCountByVariant(String variant) {
        return variant == null ? foundBlocks.size() : (int) foundBlocks.stream().filter(block -> Objects.equals(block.variant(), variant)).count();
    }

    /**
     * Reset the found blocks by variant, and if variant is null, reset all found blocks
     *
     * @param variant the variant to reset
     */
    public void resetByVariant(String variant, UUID playerUuid) {
        foundBlocks.removeIf(block -> variant == null || Objects.equals(block.variant(), variant));
        savePlayerData(playerUuid);
    }
}