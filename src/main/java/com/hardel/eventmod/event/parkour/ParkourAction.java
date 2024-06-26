package com.hardel.eventmod.event.parkour;

import com.hardel.eventmod.utils.BlockUtils;
import com.hardel.eventmod.utils.ParticleUtils;
import com.hardel.eventmod.utils.PlaySoundUtils;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

import java.util.List;

public class ParkourAction {
    public static void onTick(MinecraftServer server) {
        List<ParkourConfigData> instances = ParkourConfigData.getInstance();

        server.getPlayerManager().getPlayerList().forEach(player -> {
            for (ParkourConfigData config : instances) {
                List<CheckpointData> checkpoints = config.checkpoints();

                for (CheckpointData checkpoint : checkpoints) {
                    if (BlockUtils.isPlayerInZone(player, checkpoint.start(), checkpoint.end())) {
                        CheckpointAction response = switch (checkpoint.type()) {
                            case START ->
                                    ParkourPlayerData.startCheckpoint(config.variant(), player.getUuid(), server.getTicks(), checkpoint.index());
                            case END ->
                                    ParkourPlayerData.finishCheckpoint(config.variant(), player, server.getTicks(), checkpoint.index());
                            case CHECKPOINT ->
                                    ParkourPlayerData.updateCheckpoints(config.variant(), player.getUuid(), checkpoint.index());
                        };

                        switch (response) {
                            case GOOD_PATH -> {
                                player.sendMessage(checkpoint.successMessage(), true);
                                PlaySoundUtils.playSound(checkpoint.sound(), player, 1, 1);
                                ParticleUtils.spawnParticles(player, player.getBlockPos(), ParticleTypes.HAPPY_VILLAGER);
                            }
                            case NOT_GOOD_PATH -> player.sendMessage(config.failMessage(), true);
                        }
                    }

                    ParkourPlayerData instance = ParkourPlayerData.getInstance(player.getUuid(), config.variant());
                    if (instance.isParticipating() == null) {
                        continue;
                    }

                    if (!(BlockUtils.isPlayerInZone(player, config.start(), config.end())) && instance.isParticipating()) {
                        ParkourPlayerData.setParticipating(config.variant(), player.getUuid(), false);
                    }

                    if (instance.isParticipating() && !instance.isFinished()) {
                        int second = (server.getTicks() - instance.startTicks()) / 20;
                        player.sendMessage(Text.of("Temps écoulé: " + second + " secondes"), true);
                    }
                }
            }
        });
    }
}