package com.hardel.eventmod.utils;

import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class PlaySoundUtils {
    public static void playSound(Identifier soundId, ServerPlayerEntity player, float volume, float pitch) {
        RegistryEntry<SoundEvent> registryEntry = RegistryEntry.of(SoundEvent.of(soundId));
        player.networkHandler.sendPacket(new PlaySoundS2CPacket(registryEntry, SoundCategory.MASTER, player.getX(), player.getY(), player.getZ(), volume, pitch, 0));
    }
}
