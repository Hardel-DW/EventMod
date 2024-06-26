package com.hardel.eventmod.utils;

import com.google.gson.JsonArray;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Objects;
import java.util.UUID;

public class BlockUtils {
    public static UUID getHeadUuid(World world, BlockPos blockPos) {
        NbtCompound nbt = Objects.requireNonNull(world.getBlockEntity(blockPos)).createComponentlessNbt(null);
        NbtCompound profile = nbt.getCompound("profile");
        int[] uuid = profile.getIntArray("id");
        return new UUID(((long) uuid[0] << 32) | uuid[1], ((long) uuid[2] << 32) | uuid[3]);
    }

    public static boolean isPlayerInZone(ServerPlayerEntity player, BlockPos pos1, BlockPos pos2) {
        int playerX = toBlockCoord(player.getX());
        int playerY = toBlockCoord(player.getY());
        int playerZ = toBlockCoord(player.getZ());

        boolean isInX = isBetween(playerX, pos1.getX(), pos2.getX());
        boolean isInY = isBetween(playerY, pos1.getY(), pos2.getY());
        boolean isInZ = isBetween(playerZ, pos1.getZ(), pos2.getZ());
        return isInX && isInY && isInZ;
    }

    private static boolean isBetween(int value, int min, int max) {
        return value >= Math.min(min, max) && value <= Math.max(min, max);
    }

    private static int toBlockCoord(double coordinate) {
        return coordinate < 0 ? (int) coordinate - 1 : (int) coordinate;
    }

    public static JsonArray posToJson(BlockPos pos) {
        JsonArray position = new JsonArray();
        position.add(pos.getX());
        position.add(pos.getY());
        position.add(pos.getZ());
        return position;
    }
}
