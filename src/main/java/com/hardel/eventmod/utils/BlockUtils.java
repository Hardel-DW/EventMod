package com.hardel.eventmod.utils;

import net.minecraft.nbt.NbtCompound;
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
}
