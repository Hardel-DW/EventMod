package com.hardel.eventmod.event;

import com.google.gson.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EventData {
    public static final Logger LOGGER = LogManager.getLogger(EventData.class);
    private static final String EVENTS_DIR_PATH = "events";
    private static final String CONFIG_DIR_PATH = EVENTS_DIR_PATH + "/config";
    private static final String PLAYERS_DIR_PATH = EVENTS_DIR_PATH + "/players";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Object fileLock = new Object();

    /**
     * Read data from a json file
     *
     * @param path the path to read the data from
     * @return containing the data from the file
     */
    private static JsonArray readJsonFile(Path path) {
        if (Files.exists(path)) {
            try {
                return GSON.fromJson(Files.readString(path), JsonArray.class);
            } catch (IOException e) {
                LOGGER.error("Failed to read data from file: {}", path, e);
            }
        }

        return new JsonArray();
    }

    /**
     * Write data to a json file
     *
     * @param path the path to write the data to
     * @param data the data to write
     */
    private static void writeJsonFile(Path path, JsonArray data) {
        try {
            Files.writeString(path, GSON.toJson(data));
        } catch (IOException e) {
            LOGGER.error("Failed to write data to file: {}", path, e);
        }
    }

    /**
     * Create directories if they don't exist
     *
     * @param path the path to create directories for
     */
    private static void createDirectories(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            LOGGER.error("Failed to create directories for path: {}", path, e);
        }
    }

    /**
     * Load the config data for a specific event
     *
     * @param event the event to load config data for
     * @return containing the config data for the event
     */
    public static JsonArray loadConfigEventData(String event) {
        synchronized (fileLock) {
            Path path = Paths.get(CONFIG_DIR_PATH, event + ".json");
            createDirectories(path.getParent());
            JsonArray configData = readJsonFile(path);

            return !configData.isEmpty() ? configData : new JsonArray();
        }
    }

    /**
     * Save the config data for a specific event
     *
     * @param event the event to save config data for
     * @param data  containing the config data for the event
     */
    public static void saveConfigEventData(String event, JsonArray data) {
        synchronized (fileLock) {
            Path path = Paths.get(CONFIG_DIR_PATH, event + ".json");
            createDirectories(path.getParent());
            writeJsonFile(path, data);
        }
    }

    /**
     * Load data from players for a specific event
     *
     * @param playerUuid UUID of the player
     * @param event      The event to load data for
     * @return containing the player data for the event
     */
    public static JsonArray loadPlayerEventData(UUID playerUuid, String event) {
        synchronized (fileLock) {
            Path path = Paths.get(PLAYERS_DIR_PATH, event, playerUuid.toString() + ".json");
            createDirectories(path.getParent());
            JsonElement playerData = readJsonFile(path);

            if (playerData.isJsonArray()) {
                return playerData.getAsJsonArray();
            } else {
                return new JsonArray();
            }
        }
    }

    /**
     * Force load data from every file in the players directory for a specific event
     */
    public static List<UUID> forceLoadAllPlayerEventData(String event) {
        synchronized (fileLock) {
            Path path = Paths.get(PLAYERS_DIR_PATH, event);
            createDirectories(path);
            List<UUID> playerData = new ArrayList<>();

            File[] files = path.toFile().listFiles();
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    if (fileName.endsWith(".json")) {
                        playerData.add(UUID.fromString(fileName.substring(0, fileName.length() - 5)));
                    }
                }
            }

            return playerData;
        }
    }

    /**
     * Save the player data to the event file
     *
     * @param playerUuid of the player
     * @param event      the event to save data for
     * @param eventData  containing the player data for the specific event
     */
    public static void savePlayerEventData(UUID playerUuid, String event, JsonArray eventData) {
        synchronized (fileLock) {
            Path path = Paths.get(PLAYERS_DIR_PATH, event, playerUuid.toString() + ".json");
            createDirectories(path.getParent());
            writeJsonFile(path, eventData);
        }
    }

    /**
     * Get all the variant names from the event file
     *
     * @param event the event to get the variant names for
     * @return containing the variant names
     */
    public static List<String> getAllVariantNameFromEvent(String event) {
        JsonArray finderConfig = loadConfigEventData(event);

        List<String> variantNames = new ArrayList<>();
        for (JsonElement finder : finderConfig) {
            JsonObject finderObj = finder.getAsJsonObject();
            String variantName = finderObj.get("variant").getAsString();
            variantNames.add(variantName);
        }

        return variantNames;
    }
}
