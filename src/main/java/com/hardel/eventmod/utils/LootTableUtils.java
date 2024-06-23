package com.hardel.eventmod.utils;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

public class LootTableUtils {
    public static void spawnLootTable(PlayerEntity player, Identifier lootTableId) {
        LootTable lootTable = Objects.requireNonNull(player.getServer()).getReloadableRegistries().getRegistryManager().get(RegistryKeys.LOOT_TABLE).get(lootTableId);
        DefaultedList<ItemStack> lootStack = DefaultedList.of();

        assert lootTable != null;
        lootTable.generateLoot(new LootContextParameterSet.Builder((ServerWorld) player.getWorld()).build(LootContextTypes.EMPTY), lootStack::add);
        ItemScatterer.spawn(player.getWorld(), BlockPos.ofFloored(new Vec3d(player.getBlockPos().getX(), player.getBlockPos().getY() + 1, player.getBlockPos().getZ())), lootStack);
    }
}
