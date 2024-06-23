package com.hardel.eventmod.event.type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hardel.eventmod.event.EventData;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public record FinderConfigData(List<FinderConfig> finderConfigs) {
    /**
     * Get the config data from the file
     *
     * @param event the event to get the data from
     * @return the event data
     */
    public static FinderConfigData getConfigData(String event) {
        JsonArray finderArray = EventData.loadConfigEventData(event);
        List<FinderConfig> configs = new ArrayList<>();

        for (JsonElement element : finderArray) {
            JsonObject configData = element.getAsJsonObject();
            UUID uuid = UUID.fromString(configData.get("uuid").getAsString());
            String variant = configData.get("variant").getAsString();
            Text foundMessage = Text.Serialization.fromJson(configData.get("found_message").getAsString(), DynamicRegistryManager.EMPTY);
            Text alreadyFoundMessage = Text.Serialization.fromJson(configData.get("already_found").getAsString(), DynamicRegistryManager.EMPTY);
            String reward = configData.get("reward").getAsString();
            Identifier sound = Identifier.of(configData.get("sound").getAsString());

            configs.add(
                    FinderConfig.builder()
                            .uuid(uuid)
                            .variant(variant)
                            .foundMessage(foundMessage)
                            .alreadyFoundMessage(alreadyFoundMessage)
                            .reward(reward)
                            .sound(sound)
                            .build()
            );
        }

        return new FinderConfigData(configs);
    }

    /**
     * Save finder configurations to JSON data
     */
    public void saveConfigData() {
        JsonArray finderArray = new JsonArray();
        for (FinderConfig config : finderConfigs) {
            JsonObject configData = new JsonObject();
            configData.addProperty("uuid", config.uuid().toString());
            configData.addProperty("variant", config.variant());
            configData.addProperty("found_message", Text.Serialization.toJsonString(config.foundMessage(), DynamicRegistryManager.EMPTY));
            configData.addProperty("already_found", Text.Serialization.toJsonString(config.alreadyFoundMessage(), DynamicRegistryManager.EMPTY));
            configData.addProperty("reward", config.reward());
            configData.addProperty("sound", config.sound().toString());
            finderArray.add(configData);
        }

        EventData.saveConfigEventData(Finder.key, finderArray);
    }


    /**
     * Add a new finder configuration
     *
     * @param config the finder configuration to add
     */
    public boolean addFinderConfig(FinderConfig config) {
        if (finderConfigs.stream().noneMatch(finderConfig -> finderConfig.variant().equals(config.variant()))) {
            finderConfigs.add(config);

            saveConfigData();
            return true;
        }

        return false;
    }

    /**
     * Modify the configuration of a finder.
     *
     * @param variant   the variant of the block
     * @param newConfig a partial FinderConfig containing the new values to set
     * @return true if the configuration is modified, false if the variant is not found
     */
    public boolean modifyFinderConfig(String variant, FinderConfig newConfig) {
        for (FinderConfig config : finderConfigs) {
            if (config.variant().equals(variant)) {
                FinderConfig updatedConfig = FinderConfig.builder()
                        .uuid(config.uuid())
                        .variant(config.variant())
                        .foundMessage(newConfig.foundMessage() != null ? newConfig.foundMessage() : config.foundMessage())
                        .alreadyFoundMessage(newConfig.alreadyFoundMessage() != null ? newConfig.alreadyFoundMessage() : config.alreadyFoundMessage())
                        .reward(newConfig.reward() != null ? newConfig.reward() : config.reward())
                        .sound(newConfig.sound() != null ? newConfig.sound() : config.sound())
                        .particle(newConfig.particle() != null ? newConfig.particle() : config.particle())
                        .build();

                finderConfigs.set(finderConfigs.indexOf(config), updatedConfig);

                saveConfigData();
                return true;
            }
        }

        return false;
    }

    /**
     * Remove a finder configuration
     *
     * @param variant the variant of the block
     * @return true if the variant is removed, false if the variant is not found
     */
    public boolean removeFinderConfig(String variant) {
        for (FinderConfig config : finderConfigs) {
            if (config.variant().equals(variant)) {
                finderConfigs.remove(config);

                saveConfigData();
                return true;
            }
        }

        return false;
    }
}
