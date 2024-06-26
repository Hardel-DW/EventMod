package com.hardel.eventmod.event.finder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hardel.eventmod.EventMod;
import com.hardel.eventmod.event.EventData;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class FinderPlayerData {
    private static final Map<UUID, List<FinderPlayerData>> instances = new HashMap<>();
    private final BlockPos pos;
    private final String variant;

    private static List<FinderPlayerData> getInstances(UUID playerUuid) {
        if (!instances.containsKey(playerUuid)) {
            loadPlayerData(playerUuid);
        }

        instances.computeIfAbsent(playerUuid, k -> new ArrayList<>());
        return instances.get(playerUuid);
    }

    private FinderPlayerData(BlockPos pos, String variant) {
        this.pos = pos;
        this.variant = variant;
    }

    private static void loadPlayerData(UUID playerUuid) {
        JsonArray loadedConfigs = EventData.loadPlayerEventData(playerUuid, EventMod.finderKey);
        List<FinderPlayerData> data = new ArrayList<>();

        for (JsonElement element : loadedConfigs) {
            JsonObject blockData = element.getAsJsonObject();
            String variant = blockData.get("variant").getAsString();
            JsonArray position = blockData.getAsJsonArray("position");

            BlockPos pos = new BlockPos(position.get(0).getAsInt(), position.get(1).getAsInt(), position.get(2).getAsInt());
            data.add(new FinderPlayerData(pos, variant));
        }

        instances.put(playerUuid, data);
    }

    private static void savePlayerData(UUID playerUuid) {
        List<FinderPlayerData> instance = instances.get(playerUuid);
        if (instance == null) {
            instance = new ArrayList<>();
        }

        JsonArray data = new JsonArray();
        for (FinderPlayerData entry : instance) {
            if (entry.pos == null || entry.variant == null) {
                continue;
            }

            JsonObject object = new JsonObject();
            JsonArray position = new JsonArray();
            position.add(entry.pos.getX());
            position.add(entry.pos.getY());
            position.add(entry.pos.getZ());

            object.add("position", position);
            object.addProperty("variant", entry.variant);
            data.add(object);
        }

        EventData.savePlayerEventData(playerUuid, EventMod.finderKey, data);
        loadPlayerData(playerUuid);
    }

    /**
     * Add a block found by the player
     *
     * @param pos     the position of the block
     * @param variant the variant of the block
     * @return true if the block is added, false if the block is already found
     */
    public static boolean tryAddNewEntry(BlockPos pos, String variant, UUID playerUuid) {
        List<FinderPlayerData> instance = getInstances(playerUuid);
        FinderPlayerData finderBlock = new FinderPlayerData(pos, variant);

        if (!instance.contains(finderBlock)) {
            instance.add(finderBlock);
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
    public static int getFoundBlocksCountByVariant(String variant, UUID playerUuid) {
        List<FinderPlayerData> instance = getInstances(playerUuid);
        if (instance == null) {
            return 0;
        }

        return variant == null ? instance.size() : (int) instance.stream().filter(block -> Objects.equals(block.variant, variant)).count();
    }

    /**
     * Reset the found blocks by variant, and if variant is null, reset all found blocks
     *
     * @param variant the variant to reset
     */
    public static void resetByVariant(String variant, UUID playerUuid) {
        List<FinderPlayerData> instance = instances.computeIfAbsent(playerUuid, k -> new ArrayList<>());
        instance.removeIf(block -> variant == null || Objects.equals(block.variant, variant));
        savePlayerData(playerUuid);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FinderPlayerData that = (FinderPlayerData) o;
        return Objects.equals(pos, that.pos) && Objects.equals(variant, that.variant);
    }
}