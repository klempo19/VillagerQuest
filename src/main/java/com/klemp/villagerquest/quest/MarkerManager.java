package com.klemp.villagerquest.quest;

import com.klemp.villagerquest.ModBlocks;
import com.klemp.villagerquest.VillagerQuestMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

public class MarkerManager {
    private static final Map<UUID, List<BlockPos>> questMarkers = new HashMap<>();

    public static void placeMarkersForQuest(VillagerQuest quest, ServerLevel level) {
        BlockPos min = quest.getMinPos();
        BlockPos max = quest.getMaxPos();
        
        List<BlockPos> markers = new ArrayList<>();
        
        // Place markers at the four corners at ground level
        BlockPos[] corners = {
            new BlockPos(min.getX(), min.getY(), min.getZ()),
            new BlockPos(max.getX(), min.getY(), min.getZ()),
            new BlockPos(min.getX(), min.getY(), max.getZ()),
            new BlockPos(max.getX(), min.getY(), max.getZ())
        };
        
        BlockState markerState = ModBlocks.QUEST_MARKER.get().defaultBlockState();
        
        for (BlockPos corner : corners) {
            // Find the ground level at this corner
            BlockPos groundPos = level.getHeightmapPos(
                net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, corner);
            
            // Place marker
            level.setBlock(groundPos, markerState, 3);
            markers.add(groundPos);
            VillagerQuestMod.LOGGER.info("Placed quest marker at: {}", groundPos);
        }
        
        questMarkers.put(quest.getVillagerUUID(), markers);
        VillagerQuestMod.LOGGER.info("Stored {} markers for villager {}", markers.size(), quest.getVillagerUUID());
    }

    public static void removeMarkersForQuest(UUID villagerUUID, ServerLevel level) {
        VillagerQuestMod.LOGGER.info("Attempting to remove markers for villager: {}", villagerUUID);
        
        List<BlockPos> markers = questMarkers.get(villagerUUID);
        if (markers != null) {
            VillagerQuestMod.LOGGER.info("Found {} markers to remove", markers.size());
            
            for (BlockPos pos : markers) {
                BlockState state = level.getBlockState(pos);
                VillagerQuestMod.LOGGER.info("Checking block at {}: {}", pos, state.getBlock().getName().getString());
                
                if (state.getBlock() == ModBlocks.QUEST_MARKER.get()) {
                    level.removeBlock(pos, false);
                    VillagerQuestMod.LOGGER.info("Removed marker at: {}", pos);
                } else {
                    VillagerQuestMod.LOGGER.warn("Block at {} is not a quest marker: {}", pos, state.getBlock().getName().getString());
                }
            }
            questMarkers.remove(villagerUUID);
            VillagerQuestMod.LOGGER.info("Cleared marker list for villager {}", villagerUUID);
        } else {
            VillagerQuestMod.LOGGER.warn("No markers found for villager: {}", villagerUUID);
            
            // Debug: Print all stored marker UUIDs
            VillagerQuestMod.LOGGER.info("Current stored marker UUIDs: {}", questMarkers.keySet());
        }
    }

    public static boolean hasMarkers(UUID villagerUUID) {
        return questMarkers.containsKey(villagerUUID);
    }
    
    // Add method to force clean all markers (for debugging)
    public static void cleanAllMarkers(ServerLevel level) {
        VillagerQuestMod.LOGGER.info("Force cleaning all quest markers");
        
        for (Map.Entry<UUID, List<BlockPos>> entry : questMarkers.entrySet()) {
            for (BlockPos pos : entry.getValue()) {
                if (level.getBlockState(pos).getBlock() == ModBlocks.QUEST_MARKER.get()) {
                    level.removeBlock(pos, false);
                }
            }
        }
        questMarkers.clear();
        VillagerQuestMod.LOGGER.info("All markers cleaned");
    }
}
