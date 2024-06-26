package com.hardel.eventmod.event.elytra;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hardel.eventmod.event.EventData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;

public record ElytraPlayerData(
        Integer checkpoints,
        String variant,
        Boolean isFinished,
        Integer startTicks,
        Integer finishTicks,
        Boolean isParticipating
) {
    private static final Map<UUID, List<ElytraPlayerData>> instances = new HashMap<>();
    private static final String key = "elytra";

    public static ElytraPlayerData getInstance(UUID playerUuid, String variant) {
        return getInstances(playerUuid).stream()
                .filter(data -> data.variant.equals(variant))
                .findFirst()
                .orElse(new ElytraPlayerData.Builder(variant).build());
    }

    private static List<ElytraPlayerData> getInstances(UUID playerUuid) {
        if (!instances.containsKey(playerUuid)) {
            loadPlayerData(playerUuid);
        }

        instances.computeIfAbsent(playerUuid, k -> new ArrayList<>());
        return instances.get(playerUuid);
    }

    public static Builder builder(String variant) {
        return new Builder(variant);
    }

    private static void loadPlayerData(UUID playerUuid) {
        JsonArray loadedConfigs = EventData.loadPlayerEventData(playerUuid, key);
        List<ElytraPlayerData> data = new ArrayList<>();

        for (JsonElement element : loadedConfigs) {
            JsonObject entry = element.getAsJsonObject();
            String variant = entry.get("variant").getAsString();
            Boolean isFinished = Optional.of(entry.get("isFinished").getAsBoolean()).orElse(null);
            Integer checkpoints = Optional.of(entry.get("checkpoints").getAsInt()).orElse(null);
            Integer startTicks = Optional.of(entry.get("startTicks").getAsInt()).orElse(null);
            Integer finishTicks = Optional.of(entry.get("finishTicks").getAsInt()).orElse(null);
            Boolean isParticipating = Optional.of(entry.get("isParticipating").getAsBoolean()).orElse(null);

            data.add(new ElytraPlayerData(checkpoints, variant, isFinished, startTicks, finishTicks, isParticipating));
        }

        instances.put(playerUuid, data);
    }

    private static void savePlayerData(UUID playerUuid) {
        List<ElytraPlayerData> instance = instances.get(playerUuid);
        if (instance == null) {
            instance = new ArrayList<>();
        }

        JsonArray data = new JsonArray();
        for (ElytraPlayerData entry : instance) {
            if (entry.variant == null) {
                continue;
            }

            JsonObject object = new JsonObject();
            object.addProperty("variant", entry.variant);
            object.addProperty("checkpoints", entry.checkpoints);
            object.addProperty("isFinished", entry.isFinished);
            object.addProperty("startTicks", entry.startTicks);
            object.addProperty("finishTicks", entry.finishTicks);
            object.addProperty("isParticipating", entry.isParticipating);
            data.add(object);
        }

        EventData.savePlayerEventData(playerUuid, key, data);
    }


    /**
     * Get the finish status, start ticks and finish ticks of the player, and checkpoints
     *
     * @param playerUuid the player's UUID
     * @return the finish status of the player
     */
    public static Text getPlayerDetail(String variant, UUID playerUuid) {
        List<ElytraPlayerData> instance = getInstances(playerUuid);
        for (ElytraPlayerData data : instance) {
            if (data.variant.equals(variant)) {
                return Text.of("Checkpoints: " + data.checkpoints + ", Finished: " + data.isFinished + ", Start: " + data.startTicks + ", Finish: " + data.finishTicks);
            }
        }

        return Text.of("The participant has not played this variant yet.");
    }

    /**
     * Get Ranking between all players
     *
     * @param variant the variant of the player
     * @param howMany the number of players to get
     */
    public static List<UUID> getRanking(String variant, int howMany) {
        // Dirty code, but it works
        List<UUID> players = EventData.forceLoadAllPlayerEventData(key);
        for (UUID playerUuid : players) {
            getInstances(playerUuid);
        }

        List<UUID> ranking = new ArrayList<>();

        // Get all players has played the variant, and sort by finish status
        List<UUID> playerUuids = new ArrayList<>(instances.keySet());
        playerUuids.removeIf(uuid -> getInstances(uuid).stream().noneMatch(data -> data.variant.equals(variant)));

        // Check by finish status true, and the
        // Get the config from the variant
        ElytraConfigData config = ElytraConfigData.getInstance().stream().filter(c -> c.variant().equals(variant)).findFirst().orElse(null);
        WinCondition winCondition = config != null ? config.winCondition() : WinCondition.FIRST_TO_FINISH;

        // For FIRST_TO_FINISH is the player with status isFinished true and lower endTicks
        // For FASTEST_TIME is the player with status isFinished true and lower time between startTicks and finishTicks
        playerUuids.sort((uuid1, uuid2) -> {
            int compare = 0;
            List<ElytraPlayerData> instance1 = getInstances(uuid1);
            List<ElytraPlayerData> instance2 = getInstances(uuid2);
            ElytraPlayerData data1 = instance1.stream().filter(data -> data.variant.equals(variant)).findFirst().orElse(null);
            ElytraPlayerData data2 = instance2.stream().filter(data -> data.variant.equals(variant)).findFirst().orElse(null);

            if (data1 == null || data2 == null) {
                return 0;
            }

            if (winCondition == WinCondition.FIRST_TO_FINISH) {
                compare = Boolean.compare(data1.isFinished, data2.isFinished);
                if (compare == 0) {
                    compare = Integer.compare(data1.finishTicks, data2.finishTicks);
                }
            } else if (winCondition == WinCondition.FASTEST_TIME) {
                compare = Boolean.compare(data1.isFinished, data2.isFinished);
                if (compare == 0) {
                    compare = Integer.compare(data1.finishTicks - data1.startTicks, data2.finishTicks - data2.startTicks);
                }
            }

            return compare;
        });

        for (int i = 0; i < howMany && i < playerUuids.size(); i++) {
            ranking.add(playerUuids.get(i));
        }

        return ranking;
    }

    /**
     * Reset the data by variant
     *
     * @param variant the variant to reset
     */
    public static void resetByVariant(String variant, UUID playerUuid) {
        List<ElytraPlayerData> instance = instances.computeIfAbsent(playerUuid, k -> new ArrayList<>());
        instance.removeIf(data -> variant == null || Objects.equals(data.variant, variant));

        instances.put(playerUuid, instance);
        savePlayerData(playerUuid);
    }

    /**
     * Update the start ticks of the player
     */
    private static void updatePlayer(String variant, UUID playerUuid, ElytraPlayerData updatedData) {
        List<ElytraPlayerData> instance = getInstances(playerUuid);
        instance.removeIf(data -> data.variant.equals(variant));
        instance.add(updatedData);

        instances.put(playerUuid, instance);
        savePlayerData(playerUuid);
    }

    /**
     * Update the checkpoints of the player
     *
     * @param checkpoints the new checkpoints
     * @param playerUuid  the player's UUID
     */
    public static CheckpointAction updateCheckpoints(String variant, UUID playerUuid, int checkpoints) {
        ElytraPlayerData instance = getInstance(playerUuid, variant);
        if (instance.checkpoints() == null || instance.checkpoints() == checkpoints || instance.isFinished) {
            return CheckpointAction.SAME;
        }

        // Get the nearest index of the checkpoints
        Optional<Integer> nearestIndex = ElytraConfigData.getInstance().stream()
                .filter(c -> c.variant().equals(variant))
                .findFirst()
                .map(ElytraConfigData::checkpoints)
                .orElse(new ArrayList<>())
                .stream()
                .map(CheckpointData::index)
                .filter(index -> index > instance.checkpoints())
                .min(Comparator.naturalOrder());

        if (nearestIndex.isEmpty()) {
            return CheckpointAction.SAME;
        }

        if (checkpoints == nearestIndex.get()) {
            updatePlayer(variant, playerUuid, builder(variant).from(instance).checkpoints(checkpoints).build());
            return CheckpointAction.GOOD_PATH;
        }

        return CheckpointAction.NOT_GOOD_PATH;
    }

    public static boolean setParticipating(String variant, UUID playerUuid, boolean isParticipating) {
        ElytraPlayerData instance = getInstance(playerUuid, variant);
        if (instance.isParticipating() == isParticipating) {
            return false;
        }

        updatePlayer(variant, playerUuid, builder(variant).from(instance).isParticipating(isParticipating).build());
        return true;
    }

    /**
     * Start the player with the variant, check if not already finished
     *
     * @param variant    the variant of the player
     * @param playerUuid the player's UUID
     * @param ticks      the start ticks
     * @return Allow to start the parkour
     */
    public static CheckpointAction startCheckpoint(String variant, UUID playerUuid, int ticks, int checkpoints) {
        ElytraPlayerData instance = getInstance(playerUuid, variant);

        if (instance.checkpoints == null || instance.isFinished || !instance.isParticipating) {
            updatePlayer(variant, playerUuid, builder(variant).from(instance).isParticipating(true).isFinished(false).finishTicks(-1).checkpoints(checkpoints).startTicks(ticks).build());
            return CheckpointAction.GOOD_PATH;
        }

        return CheckpointAction.SAME;
    }

    /**
     * Finish the player with the variant, check if not already finished
     *
     * @param variant the variant of the player
     * @param player  the player's UUID
     * @param ticks   the finish ticks
     * @return Finish the parkour
     */
    public static CheckpointAction finishCheckpoint(String variant, ServerPlayerEntity player, int ticks, int checkpoint) {
        ElytraPlayerData instance = getInstance(player.getUuid(), variant);

        boolean hasTakenPenultimateCheckpoint = false;
        List<ElytraConfigData> configs = ElytraConfigData.getInstance();

        for (ElytraConfigData config : configs) {
            if (config.variant().equals(variant)) {
                List<CheckpointData> checkpoints = new ArrayList<>(config.checkpoints());
                checkpoints.sort(Comparator.comparingInt(CheckpointData::index));
                if (checkpoints.size() >= 2) {
                    int penultimateIndex = checkpoints.get(checkpoints.size() - 2).index();
                    if (instance.checkpoints != null && penultimateIndex == instance.checkpoints) {
                        hasTakenPenultimateCheckpoint = true;
                        break;
                    }
                }
            }
        }

        if (instance.checkpoints == null || instance.isFinished) {
            return CheckpointAction.SAME;
        }

        if (instance.checkpoints >= 0 && !hasTakenPenultimateCheckpoint) {
            return CheckpointAction.NOT_GOOD_PATH;
        }

        int second = (ticks - instance.startTicks()) / 20;
        player.sendMessage(Text.of("You have finished the parkour, with a time of " + second + " seconds"), false);
        updatePlayer(variant, player.getUuid(), builder(variant).from(instance).isFinished(true).checkpoints(checkpoint).finishTicks(ticks).build());
        return CheckpointAction.GOOD_PATH;
    }

    public static class Builder {
        private final String variant;
        private Boolean isFinished;
        private Integer checkpoints;
        private Integer startTicks;
        private Integer finishTicks;
        private Boolean isParticipating;

        public ElytraPlayerData.Builder from(ElytraPlayerData data) {
            this.checkpoints = Optional.ofNullable(data.checkpoints()).orElse(this.checkpoints);
            this.isFinished = Optional.ofNullable(data.isFinished()).orElse(this.isFinished);
            this.startTicks = Optional.ofNullable(data.startTicks()).orElse(this.startTicks);
            this.finishTicks = Optional.ofNullable(data.finishTicks()).orElse(this.finishTicks);
            this.isParticipating = Optional.ofNullable(data.isParticipating()).orElse(this.isParticipating);
            return this;
        }

        public Builder(String variant) {
            this.variant = variant;
        }

        public ElytraPlayerData.Builder checkpoints(int checkpoints) {
            this.checkpoints = checkpoints;
            return this;
        }

        public ElytraPlayerData.Builder isFinished(boolean isFinished) {
            this.isFinished = isFinished;
            return this;
        }

        public ElytraPlayerData.Builder startTicks(int startTicks) {
            this.startTicks = startTicks;
            return this;
        }

        public ElytraPlayerData.Builder finishTicks(int finishTicks) {
            this.finishTicks = finishTicks;
            return this;
        }

        public ElytraPlayerData.Builder isParticipating(boolean isParticipating) {
            this.isParticipating = isParticipating;
            return this;
        }

        public ElytraPlayerData build() {
            return new ElytraPlayerData(checkpoints, variant, isFinished, startTicks, finishTicks, isParticipating);
        }
    }
}