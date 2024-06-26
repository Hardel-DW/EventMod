package com.hardel.eventmod.event.parkour;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hardel.eventmod.utils.BlockUtils;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record CheckpointData(
        BlockPos start,
        BlockPos end,
        BlockPos respawn,
        Integer index,
        PortalType type,
        Identifier sound,
        Text successMessage,
        SimpleParticleType particle
) {

    public static CheckpointData.Builder builder() {
        return new CheckpointData.Builder();
    }

    /**
     * Converts a list of checkpoints to a JSON Object containing for each entry, key index in Integer, start key in JsonArray, and end key in JsonArray.
     *
     * @param checkpoints the list of checkpoints
     * @return the JSON Object containing the checkpoints
     */
    public static JsonArray toJsonArray(List<CheckpointData> checkpoints) {
        JsonArray checkpointsObject = new JsonArray();
        for (CheckpointData checkpoint : checkpoints) {
            JsonObject checkpointObject = new JsonObject();
            checkpointObject.add("start", BlockUtils.posToJson(checkpoint.start()));
            checkpointObject.add("end", BlockUtils.posToJson(checkpoint.end()));
            checkpointObject.add("respawn", BlockUtils.posToJson(checkpoint.respawn()));
            checkpointObject.addProperty("index", checkpoint.index());
            checkpointObject.addProperty("type", checkpoint.type().name());
            checkpointObject.addProperty("sound", checkpoint.sound().toString());
            checkpointObject.addProperty("success_message", Text.Serialization.toJsonString(checkpoint.successMessage, DynamicRegistryManager.EMPTY));

            checkpointsObject.add(checkpointObject);
        }

        return checkpointsObject;
    }

    /**
     * Converts a JSON Object containing checkpoints to a list of checkpoints.
     *
     * @param checkpointsArray the JSON Object containing the checkpoints
     * @return the list of checkpoints
     */
    public static List<CheckpointData> fromJsonArray(JsonArray checkpointsArray) {
        List<CheckpointData> checkpoints = new ArrayList<>();
        for (JsonElement checkpointElement : checkpointsArray) {
            JsonObject checkpointObject = checkpointElement.getAsJsonObject();

            JsonArray startArray = checkpointObject.getAsJsonArray("start");
            BlockPos start = new BlockPos(startArray.get(0).getAsInt(), startArray.get(1).getAsInt(), startArray.get(2).getAsInt());

            JsonArray endArray = checkpointObject.getAsJsonArray("end");
            BlockPos end = new BlockPos(endArray.get(0).getAsInt(), endArray.get(1).getAsInt(), endArray.get(2).getAsInt());

            JsonArray respawnArray = checkpointObject.getAsJsonArray("respawn");
            BlockPos respawn = new BlockPos(respawnArray.get(0).getAsInt(), respawnArray.get(1).getAsInt(), respawnArray.get(2).getAsInt());

            int index = checkpointObject.get("index").getAsInt();
            PortalType type = PortalType.valueOf(checkpointObject.get("type").getAsString());
            Identifier sound = Identifier.of(checkpointObject.get("sound").getAsString());

            Text successMessage = Text.Serialization.fromJson(checkpointObject.get("success_message").getAsString(), DynamicRegistryManager.EMPTY);

            checkpoints.add(builder().start(start).end(end).respawn(respawn).index(index).type(type).sound(sound).successMessage(successMessage).build());
        }

        return checkpoints;
    }

    public static class Builder {
        private BlockPos start;
        private BlockPos end;
        private BlockPos respawn;
        private Integer index;
        private PortalType type;
        private Identifier sound;
        private Text successMessage;
        private final SimpleParticleType particle = ParticleTypes.FIREWORK;

        /**
         * Copies the values from the given checkpoint to the builder.
         *
         * @param checkpoint the checkpoint to copy from
         * @return the builder
         */
        public Builder from(CheckpointData checkpoint) {
            this.start = Optional.ofNullable(checkpoint.start()).orElse(this.start);
            this.end = Optional.ofNullable(checkpoint.end()).orElse(this.end);
            this.respawn = Optional.ofNullable(checkpoint.respawn()).orElse(this.respawn);
            this.index = Optional.ofNullable(checkpoint.index()).orElse(this.index);
            this.type = Optional.ofNullable(checkpoint.type()).orElse(this.type);
            this.sound = Optional.ofNullable(checkpoint.sound()).orElse(this.sound);
            this.successMessage = Optional.ofNullable(checkpoint.successMessage).orElse(this.successMessage);
            return this;
        }

        public Builder withDefaults() {
            this.start = BlockPos.ORIGIN;
            this.end = BlockPos.ORIGIN;
            this.respawn = BlockPos.ORIGIN;
            this.index = 0;
            this.type = PortalType.CHECKPOINT;
            this.sound = Identifier.of("entity.experience_orb.pickup");
            this.successMessage = Text.of("Continue to the next checkpoint!");
            return this;
        }

        public Builder respawn(BlockPos respawn) {
            this.respawn = respawn;
            return this;
        }

        public Builder start(BlockPos start) {
            this.start = start;
            return this;
        }

        public Builder end(BlockPos end) {
            this.end = end;
            return this;
        }

        public Builder index(int index) {
            this.index = index;
            return this;
        }

        public Builder type(PortalType type) {
            this.type = type;
            return this;
        }

        public Builder sound(Identifier sound) {
            this.sound = sound;
            return this;
        }

        public Builder successMessage(Text successMessage) {
            this.successMessage = successMessage;
            return this;
        }

        public CheckpointData build() {
            return new CheckpointData(start, end, respawn, index, type, sound, successMessage, particle);
        }
    }
}
