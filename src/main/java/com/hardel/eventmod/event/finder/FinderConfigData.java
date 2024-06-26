package com.hardel.eventmod.event.finder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hardel.eventmod.EventMod;
import com.hardel.eventmod.event.EventData;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record FinderConfigData(
        UUID uuid,
        String variant,
        Text foundMessage,
        Text alreadyFoundMessage,
        String reward,
        Identifier sound,
        SimpleParticleType particle
) {
    private static final List<FinderConfigData> instances = new ArrayList<>();

    public static Builder builder() {
        return new Builder();
    }

    public static List<FinderConfigData> getInstance() {
        if (instances.isEmpty()) {
            loadConfigData();
        }

        return instances;
    }

    private static void loadConfigData() {
        JsonArray loadedConfigs = EventData.loadConfigEventData(EventMod.finderKey);
        List<FinderConfigData> configs = new ArrayList<>();

        for (JsonElement element : loadedConfigs) {
            JsonObject configData = element.getAsJsonObject();
            UUID uuid = UUID.fromString(configData.get("uuid").getAsString());
            String variant = configData.get("variant").getAsString();
            Text foundMessage = Text.Serialization.fromJson(configData.get("found_message").getAsString(), DynamicRegistryManager.EMPTY);
            Text alreadyFoundMessage = Text.Serialization.fromJson(configData.get("already_found").getAsString(), DynamicRegistryManager.EMPTY);
            String reward = configData.get("reward").getAsString();
            Identifier sound = Identifier.of(configData.get("sound").getAsString());

            configs.add(
                    FinderConfigData.builder()
                            .withDefaults(variant, uuid)
                            .foundMessage(foundMessage)
                            .alreadyFoundMessage(alreadyFoundMessage)
                            .reward(reward)
                            .sound(sound)
                            .build()
            );
        }

        instances.clear();
        instances.addAll(configs);
    }

    private static void saveConfigData() {
        JsonArray finderArray = new JsonArray();
        for (FinderConfigData config : instances) {
            JsonObject configData = new JsonObject();
            configData.addProperty("uuid", config.uuid().toString());
            configData.addProperty("variant", config.variant());
            configData.addProperty("found_message", Text.Serialization.toJsonString(config.foundMessage(), DynamicRegistryManager.EMPTY));
            configData.addProperty("already_found", Text.Serialization.toJsonString(config.alreadyFoundMessage(), DynamicRegistryManager.EMPTY));
            configData.addProperty("reward", config.reward());
            configData.addProperty("sound", config.sound().toString());
            finderArray.add(configData);
        }

        EventData.saveConfigEventData(EventMod.finderKey, finderArray);
    }

    public static boolean addNewConfig(FinderConfigData config) {
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

    public static boolean modifyExistingConfig(String variant, FinderConfigData newConfig) {
        Optional<FinderConfigData> configToUpdate = getInstance().stream()
                .filter(config -> config.variant().equals(variant))
                .findFirst();

        if (configToUpdate.isPresent()) {
            FinderConfigData currentConfig = configToUpdate.get();
            FinderConfigData updatedConfig = new FinderConfigData.Builder().from(currentConfig).from(newConfig).build();

            instances.set(instances.indexOf(currentConfig), updatedConfig);
            saveConfigData();
            return true;
        }

        return false;
    }

    public static class Builder {
        private UUID uuid;
        private String variant;
        private Text foundMessage;
        private Text alreadyFoundMessage;
        private String reward;
        private Identifier sound;
        private final SimpleParticleType particle = ParticleTypes.HAPPY_VILLAGER;

        public FinderConfigData.Builder withDefaults(String variant, UUID uuid) {
            this.uuid = uuid;
            this.variant = variant;
            this.foundMessage = Text.of("You found a new one!");
            this.alreadyFoundMessage = Text.of("You already found this one!");
            this.reward = "nyanmod:nyantite";
            this.sound = Identifier.of("entity.experience_orb.pickup");
            return this;
        }

        public FinderConfigData.Builder from(FinderConfigData config) {
            this.uuid = Optional.ofNullable(config.uuid()).orElse(this.uuid);
            this.variant = Optional.ofNullable(config.variant()).orElse(this.variant);
            this.foundMessage = Optional.ofNullable(config.foundMessage()).orElse(this.foundMessage);
            this.alreadyFoundMessage = Optional.ofNullable(config.alreadyFoundMessage()).orElse(this.alreadyFoundMessage);
            this.reward = Optional.ofNullable(config.reward()).orElse(this.reward);
            this.sound = Optional.ofNullable(config.sound()).orElse(this.sound);
            return this;
        }

        public FinderConfigData.Builder foundMessage(Text foundMessage) {
            this.foundMessage = foundMessage;
            return this;
        }

        public FinderConfigData.Builder alreadyFoundMessage(Text alreadyFoundMessage) {
            this.alreadyFoundMessage = alreadyFoundMessage;
            return this;
        }

        public FinderConfigData.Builder reward(String reward) {
            this.reward = reward;
            return this;
        }

        public FinderConfigData.Builder sound(Identifier sound) {
            this.sound = sound;
            return this;
        }

        public FinderConfigData build() {
            return new FinderConfigData(uuid, variant, foundMessage, alreadyFoundMessage, reward, sound, particle);
        }
    }
}