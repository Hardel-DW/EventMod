package com.hardel.eventmod.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.hardel.eventmod.event.EventData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JSONUtils {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LogManager.getLogger(EventData.class);

    /**
     * Read data from a json file
     *
     * @param path the path to read the data from
     * @return containing the data from the file
     */
    public static JsonArray readJsonFile(Path path) {
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
    public static void writeJsonFile(Path path, JsonArray data) {
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
    public static void createDirectories(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            LOGGER.error("Failed to create directories for path: {}", path, e);
        }
    }

}
