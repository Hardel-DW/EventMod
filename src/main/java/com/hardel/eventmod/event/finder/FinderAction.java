package com.hardel.eventmod.event.finder;

import com.hardel.eventmod.utils.BlockUtils;
import com.hardel.eventmod.utils.LootTableUtils;
import com.hardel.eventmod.utils.ParticleUtils;
import com.hardel.eventmod.utils.PlaySoundUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class FinderAction {
    public static ActionResult onBlockUse(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        if (hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
        }

        Block block = world.getBlockState(hitResult.getBlockPos()).getBlock();
        if (block == Blocks.PLAYER_HEAD || block == Blocks.PLAYER_WALL_HEAD) {
            UUID headOwnerUuid = BlockUtils.getHeadUuid(world, hitResult.getBlockPos());
            List<FinderConfigData> configs = FinderConfigData.getInstance();

            for (FinderConfigData config : configs) {
                if (Objects.equals(headOwnerUuid, config.uuid())) {
                    if (player instanceof ServerPlayerEntity) {
                        boolean isNewBlock = FinderPlayerData.tryAddNewEntry(hitResult.getBlockPos(), config.variant(), player.getUuid());

                        if (isNewBlock) {
                            player.sendMessage(config.foundMessage(), true);
                            LootTableUtils.spawnLootTable(player, Identifier.of(config.reward()));
                            PlaySoundUtils.playSound(config.sound(), (ServerPlayerEntity) player, 1, 1);
                            ParticleUtils.spawnParticles((ServerPlayerEntity) player, hitResult.getBlockPos(), ParticleTypes.HAPPY_VILLAGER);
                        } else {
                            player.sendMessage(config.alreadyFoundMessage(), true);
                        }
                    }

                    return ActionResult.SUCCESS;
                }
            }
        }

        return ActionResult.PASS;
    }
}
