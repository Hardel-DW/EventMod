package com.hardel.eventmod.utils;

import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public class ParticleUtils {
    public static void spawnParticles(ServerPlayerEntity player, BlockPos pos, SimpleParticleType particle) {
        player.networkHandler.sendPacket(new ParticleS2CPacket(particle, true, pos.getX(), pos.getY(), pos.getZ(), 0.75f, 0.75f, 0.75f, 0.25f, 100));
    }
}
