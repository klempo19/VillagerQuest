package com.klemp.villagerquest.handler;

import com.klemp.villagerquest.ModEntities;
import com.klemp.villagerquest.entity.WanderingQuestVillager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Random;

public class WanderingVillagerSpawner {
    private static final Random RANDOM = new Random();
    private static final int CHECK_INTERVAL = 1200; // Check every minute
    private static final double SPAWN_CHANCE = 0.01; // 1% chance per check
    private static final int VILLAGE_SEARCH_RADIUS = 128; // Blocks
    private static final int SPAWN_DISTANCE = 48; // Blocks from player
    
    private int tickCounter = 0;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        tickCounter++;
        if (tickCounter < CHECK_INTERVAL) return;
        tickCounter = 0;

        // Check each player
        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (level.dimension() != ServerLevel.OVERWORLD) continue; // Only spawn in overworld
            
            for (ServerPlayer player : level.players()) {
                // Skip if player has active quest
                if (hasActiveQuest(player, level)) continue;
                
                // Skip if near village
                if (isNearVillage(player, level)) continue;
                
                // Random chance to spawn
                if (RANDOM.nextDouble() < SPAWN_CHANCE) {
                    spawnWanderingVillager(player, level);
                }
            }
        }
    }

    private boolean hasActiveQuest(ServerPlayer player, ServerLevel level) {
        // Check if player has an active quest
        com.klemp.villagerquest.quest.PlayerQuestManager playerQuestManager = 
            com.klemp.villagerquest.quest.PlayerQuestManager.get(level);
        return playerQuestManager.hasActiveQuest(player.getUUID());
    }

    private boolean isNearVillage(ServerPlayer player, ServerLevel level) {
        // Check for villagers nearby (indicates a village)
        return !level.getEntitiesOfClass(
            Villager.class,
            player.getBoundingBox().inflate(VILLAGE_SEARCH_RADIUS),
            v -> !(v instanceof WanderingQuestVillager)
        ).isEmpty();
    }

    private void spawnWanderingVillager(ServerPlayer player, ServerLevel level) {
        // Find spawn position around player
        BlockPos playerPos = player.blockPosition();
        
        for (int attempt = 0; attempt < 10; attempt++) {
            // Random angle and distance
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            int distance = SPAWN_DISTANCE + RANDOM.nextInt(32);
            
            int x = playerPos.getX() + (int)(Math.cos(angle) * distance);
            int z = playerPos.getZ() + (int)(Math.sin(angle) * distance);
            
            BlockPos spawnPos = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(x, 0, z));
            
            // Check if position is valid
            if (!level.getBlockState(spawnPos).isAir()) continue;
            if (!level.getBlockState(spawnPos.below()).isSolid()) continue;
            
            // Spawn the wandering villager
            WanderingQuestVillager wanderer = ModEntities.WANDERING_QUEST_VILLAGER.get().create(level);
            if (wanderer != null) {
                wanderer.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 
                    RANDOM.nextFloat() * 360.0F, 0.0F);
                wanderer.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), 
                    MobSpawnType.EVENT, null, null);
                level.addFreshEntity(wanderer);
                
                // Notify player
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§6A wandering builder has appeared nearby..."));
                break;
            }
        }
    }
}