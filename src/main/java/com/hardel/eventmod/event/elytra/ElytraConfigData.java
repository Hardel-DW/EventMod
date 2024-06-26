package com.hardel.eventmod.event.elytra;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hardel.eventmod.event.EventData;
import com.hardel.eventmod.utils.BlockUtils;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ElytraConfigData(
        String variant,
        Text failMessage,
        List<CheckpointData> checkpoints,
        WinCondition winCondition,
        BlockPos start,
        BlockPos end
) {
    private static final List<ElytraConfigData> instances = new ArrayList<>();
    private static final String key = "elytra";

    public static ElytraConfigData.Builder builder() {
        return new ElytraConfigData.Builder();
    }

    public static List<ElytraConfigData> getInstance() {
        if (instances.isEmpty()) {
            loadConfigData();
        }

        return instances;
    }

    private static void loadConfigData() {
        JsonArray loadedConfigs = EventData.loadConfigEventData(key);
        List<ElytraConfigData> configs = new ArrayList<>();

        for (JsonElement element : loadedConfigs) {
            JsonObject configData = element.getAsJsonObject();

            String variant = configData.get("variant").getAsString();
            Text failMessage = Text.Serialization.fromJson(configData.get("fail_message").getAsString(), DynamicRegistryManager.EMPTY);
            List<CheckpointData> checkpoints = CheckpointData.fromJsonArray(configData.getAsJsonArray("checkpoints"));
            WinCondition winCondition = WinCondition.valueOf(configData.get("win_condition").getAsString());
            JsonArray startArray = configData.getAsJsonArray("start");
            BlockPos start = new BlockPos(startArray.get(0).getAsInt(), startArray.get(1).getAsInt(), startArray.get(2).getAsInt());
            JsonArray endArray = configData.getAsJsonArray("end");
            BlockPos end = new BlockPos(endArray.get(0).getAsInt(), endArray.get(1).getAsInt(), endArray.get(2).getAsInt());

            configs.add(ElytraConfigData.builder().variant(variant).failMessage(failMessage).checkpoints(checkpoints).winCondition(winCondition).start(start).end(end).build());
        }

        instances.clear();
        instances.addAll(configs);
    }

    private static void saveConfigData() {
        JsonArray configArray = new JsonArray();
        for (ElytraConfigData config : instances) {
            JsonObject configData = new JsonObject();
            configData.addProperty("variant", config.variant());
            configData.addProperty("fail_message", Text.Serialization.toJsonString(config.failMessage(), DynamicRegistryManager.EMPTY));
            configData.add("checkpoints", CheckpointData.toJsonArray(config.checkpoints()));
            configData.addProperty("win_condition", config.winCondition().name());
            configData.add("start", BlockUtils.posToJson(config.start()));
            configData.add("end", BlockUtils.posToJson(config.end()));

            configArray.add(configData);
        }

        EventData.saveConfigEventData(key, configArray);
    }

    public static boolean addNewConfig(ElytraConfigData config) {
        if (getInstance().stream().noneMatch(finderConfig -> finderConfig.variant().equals(config.variant()))) {
            instances.add(config);
            saveConfigData();
            return true;
        }

        return false;
    }

    public static boolean removeExistingConfig(String variant) {
        boolean removed = getInstance().removeIf(config -> config.variant().equals(variant));
        if (removed) {
            saveConfigData();
        }

        return removed;
    }

    public static boolean modifyExistingConfig(String variant, ElytraConfigData newConfig) {
        Optional<ElytraConfigData> config = getInstance().stream()
                .filter(configData -> configData.variant().equals(variant))
                .findFirst();

        if (config.isPresent()) {
            ElytraConfigData currentConfig = config.get();
            ElytraConfigData updatedConfig = new ElytraConfigData.Builder().from(currentConfig).from(newConfig).build();

            instances.set(instances.indexOf(currentConfig), updatedConfig);
            saveConfigData();
            return true;
        }

        return false;
    }

    public static boolean addCheckpoint(String variant, CheckpointData checkpoint) {
        Optional<ElytraConfigData> config = getInstance().stream()
                .filter(configData -> configData.variant().equals(variant))
                .findFirst();

        if (config.isPresent()) {
            ElytraConfigData currentConfig = config.get();

            List<CheckpointData> checkpoints = new ArrayList<>(currentConfig.checkpoints());
            if (checkpoints.stream().anyMatch(checkpointData -> Objects.equals(checkpointData.index(), checkpoint.index()))) {
                return false;
            }

            checkpoints.add(checkpoint);
            ElytraConfigData updatedConfig = new ElytraConfigData.Builder().from(currentConfig).checkpoints(checkpoints).build();

            instances.set(instances.indexOf(currentConfig), updatedConfig);
            saveConfigData();
            return true;
        }

        return false;
    }

    public static boolean removeCheckpoint(String variant, int index) {
        Optional<ElytraConfigData> config = getInstance().stream()
                .filter(configData -> configData.variant().equals(variant))
                .findFirst();

        if (config.isPresent()) {
            ElytraConfigData currentConfig = config.get();
            List<CheckpointData> checkpoints = new ArrayList<>(currentConfig.checkpoints());
            checkpoints.removeIf(checkpoint -> checkpoint.index() == index);

            ElytraConfigData updatedConfig = new ElytraConfigData.Builder().from(currentConfig).checkpoints(checkpoints).build();
            instances.set(instances.indexOf(currentConfig), updatedConfig);
            saveConfigData();
            return true;
        }

        return false;
    }

    public static boolean modifyCheckpoint(String variant, int index, CheckpointData newCheckpoint) {
        Optional<ElytraConfigData> config = getInstance().stream().filter(configData -> configData.variant().equals(variant)).findFirst();

        if (config.isPresent()) {
            ElytraConfigData currentConfig = config.get();
            List<CheckpointData> checkpoints = new ArrayList<>(currentConfig.checkpoints());
            Optional<CheckpointData> checkpointToUpdate = checkpoints.stream().filter(checkpoint -> checkpoint.index() == index).findFirst();

            if (checkpointToUpdate.isPresent()) {
                CheckpointData currentCheckpoint = checkpointToUpdate.get();
                CheckpointData updatedCheckpoint = new CheckpointData.Builder().index(index).from(currentCheckpoint).from(newCheckpoint).build();

                checkpoints.set(checkpoints.indexOf(currentCheckpoint), updatedCheckpoint);
                ElytraConfigData updatedConfig = new ElytraConfigData.Builder().from(currentConfig).checkpoints(checkpoints).build();

                instances.set(instances.indexOf(currentConfig), updatedConfig);
                saveConfigData();
                return true;
            }

            return false;
        }

        return false;
    }

    public static class Builder {
        private String variant;
        private Text failMessage;
        private List<CheckpointData> checkpoints;
        private WinCondition winCondition;
        private BlockPos start;
        private BlockPos end;

        public ElytraConfigData.Builder withDefaults(String variant) {
            this.start = BlockPos.ORIGIN;
            this.end = BlockPos.ORIGIN;
            this.variant = variant;
            this.failMessage = Text.of("Is not the correct portal");
            this.checkpoints = new ArrayList<>();
            this.winCondition = WinCondition.FIRST_TO_FINISH;
            return this;
        }

        public ElytraConfigData.Builder from(ElytraConfigData config) {
            this.variant = Optional.ofNullable(config.variant()).orElse(this.variant);
            this.failMessage = Optional.ofNullable(config.failMessage()).orElse(this.failMessage);
            this.checkpoints = Optional.ofNullable(config.checkpoints()).orElse(this.checkpoints);
            this.winCondition = Optional.ofNullable(config.winCondition()).orElse(this.winCondition);
            this.start = Optional.ofNullable(config.start()).orElse(this.start);
            this.end = Optional.ofNullable(config.end()).orElse(this.end);
            return this;
        }

        public ElytraConfigData.Builder variant(String variant) {
            this.variant = variant;
            return this;
        }

        public ElytraConfigData.Builder failMessage(Text failMessage) {
            this.failMessage = failMessage;
            return this;
        }

        public ElytraConfigData.Builder checkpoints(List<CheckpointData> checkpoints) {
            this.checkpoints = checkpoints;
            return this;
        }

        public ElytraConfigData.Builder winCondition(WinCondition winCondition) {
            this.winCondition = winCondition;
            return this;
        }

        public ElytraConfigData.Builder start(BlockPos start) {
            this.start = start;
            return this;
        }

        public ElytraConfigData.Builder end(BlockPos end) {
            this.end = end;
            return this;
        }

        public ElytraConfigData build() {
            return new ElytraConfigData(variant, failMessage, checkpoints, winCondition, start, end);
        }
    }
}