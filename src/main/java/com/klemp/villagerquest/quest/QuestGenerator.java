package com.klemp.villagerquest.quest;

import com.klemp.villagerquest.config.QuestConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.*;

public class QuestGenerator {

    private static final Random RANDOM = new Random();

    public static VillagerQuest generateQuest(Villager villager, ServerLevel level) {
        // Check config for quest type chances
        if (!QuestConfig.ENABLE_BUILDING_QUESTS.get() && !QuestConfig.ENABLE_DELIVERY_QUESTS.get()) {
            return null; // Both disabled
        }
        
        boolean shouldBeDelivery = false;
        if (QuestConfig.ENABLE_DELIVERY_QUESTS.get() && QuestConfig.ENABLE_BUILDING_QUESTS.get()) {
            // Both enabled, use chance
            shouldBeDelivery = RANDOM.nextDouble() < QuestConfig.DELIVERY_QUEST_CHANCE.get();
        } else if (QuestConfig.ENABLE_DELIVERY_QUESTS.get()) {
            shouldBeDelivery = true;
        }
        
        if (shouldBeDelivery) {
            return generateDeliveryQuest(villager, level);
        }
        
        // Otherwise generate building quest
        return generateBuildingQuest(villager, level);
    }

    private static VillagerQuest generateBuildingQuest(Villager villager, ServerLevel level) {
        BlockPos villagerPos = villager.blockPosition();
        BuildingAreaManager areaManager = BuildingAreaManager.get(level);
        
        // Determine quest type based on profession
        VillagerQuest.QuestType questType;
        if (villager.getVillagerData().getProfession() == VillagerProfession.NONE || 
            villager.getVillagerData().getProfession() == VillagerProfession.NITWIT) {
            questType = VillagerQuest.QuestType.PERSONAL_RESIDENCE;
        } else {
            // 50% chance for either type for employed villagers
            questType = RANDOM.nextBoolean() ? 
                VillagerQuest.QuestType.PERSONAL_RESIDENCE : 
                VillagerQuest.QuestType.WORKPLACE;
        }

        // Find a suitable build location near the villager
        BlockPos buildLocation = findSuitableBuildLocation(villagerPos, level, areaManager);
        if (buildLocation == null) {
            return null; // No suitable location found
        }

        // Generate random structure dimensions using config values
        int minSize = QuestConfig.MIN_BUILD_WIDTH.get();
        int maxSize = QuestConfig.MAX_BUILD_WIDTH.get();
        int minHeight = QuestConfig.MIN_BUILD_HEIGHT.get();
        int maxHeight = QuestConfig.MAX_BUILD_HEIGHT.get();
        
        int width = minSize + RANDOM.nextInt(maxSize - minSize + 1);
        int length = Math.max(minSize + 1, minSize + RANDOM.nextInt(maxSize - minSize + 1));
        int height = minHeight + RANDOM.nextInt(maxHeight - minHeight + 1);

        // Calculate corners
        BlockPos corner1 = buildLocation;
        BlockPos corner2 = buildLocation.offset(width - 1, height - 1, length - 1);

        // Register this area
        if (!areaManager.registerBuildingArea(corner1, corner2)) {
            return null; // Area overlaps with existing building
        }

        // Generate required blocks
        Map<Block, Integer> requiredBlocks = generateRequiredBlocks(questType, width, length, height, villager);

        // Calculate reward based on complexity
        int reward = calculateReward(width, length, height, requiredBlocks);

        return new VillagerQuest(villager.getUUID(), corner1, corner2, questType, requiredBlocks, reward);
    }

    private static VillagerQuest generateDeliveryQuest(Villager sourceVillager, ServerLevel level) {
        // Delivery quest is TO the same villager that gives it
        // Generate delivery items based on source profession
        Map<Block, Integer> deliveryItems = generateDeliveryItems(sourceVillager);
        if (deliveryItems.isEmpty()) {
            return null;
        }

        int reward = calculateDeliveryReward(deliveryItems);

        // Use villager's own position as target (they want the items themselves)
        return new VillagerQuest(
            sourceVillager.getUUID(),
            sourceVillager.blockPosition(),
            sourceVillager.blockPosition(),
            VillagerQuest.QuestType.DELIVERY,
            deliveryItems,
            reward,
            sourceVillager.getUUID() // Target is the same villager
        );
    }

    private static Map<Block, Integer> generateDeliveryItems(Villager villager) {
        Map<Block, Integer> items = new HashMap<>();
        VillagerProfession profession = villager.getVillagerData().getProfession();

        if (profession == VillagerProfession.FARMER) {
            Block[] farmItems = {Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS};
            Block item = farmItems[RANDOM.nextInt(farmItems.length)];
            items.put(item, 16 + RANDOM.nextInt(33)); // 16-48 items
        } else if (profession == VillagerProfession.FISHERMAN) {
            // Use kelp or dried kelp block as placeholder for fish delivery
            items.put(Blocks.DRIED_KELP_BLOCK, 8 + RANDOM.nextInt(17)); // 8-24 blocks
        } else if (profession == VillagerProfession.SHEPHERD) {
            Block[] woolColors = {Blocks.WHITE_WOOL, Blocks.BROWN_WOOL, Blocks.LIGHT_GRAY_WOOL, Blocks.GRAY_WOOL};
            Block wool = woolColors[RANDOM.nextInt(woolColors.length)];
            items.put(wool, 16 + RANDOM.nextInt(33));
        } else if (profession == VillagerProfession.FLETCHER) {
            // Use oak logs for fletcher (to make arrows/bows)
            items.put(Blocks.OAK_LOG, 16 + RANDOM.nextInt(17));
        } else if (profession == VillagerProfession.MASON) {
            Block[] stoneTypes = {Blocks.STONE, Blocks.COBBLESTONE, Blocks.ANDESITE, Blocks.DIORITE};
            Block stone = stoneTypes[RANDOM.nextInt(stoneTypes.length)];
            items.put(stone, 32 + RANDOM.nextInt(33));
        } else if (profession == VillagerProfession.CLERIC) {
            items.put(Blocks.GLOWSTONE, 8 + RANDOM.nextInt(9)); // 8-16 glowstone
        } else if (profession == VillagerProfession.LIBRARIAN) {
            items.put(Blocks.BOOKSHELF, 4 + RANDOM.nextInt(5)); // 4-8 bookshelves
        } else {
            // Generic items for other professions
            items.put(Blocks.OAK_LOG, 8 + RANDOM.nextInt(17));
        }

        return items;
    }

    private static int calculateDeliveryReward(Map<Block, Integer> items) {
        int totalItems = items.values().stream().mapToInt(Integer::intValue).sum();
        return Math.max(3, Math.min(16, totalItems / 4));
    }

    private static BlockPos findSuitableBuildLocation(BlockPos center, ServerLevel level, BuildingAreaManager areaManager) {
        int searchRadius = 10;
        int minSize = QuestConfig.MIN_BUILD_WIDTH.get();
        
        // Try to find a flat area near the villager that doesn't overlap
        for (int attempt = 0; attempt < 20; attempt++) {
            int xOffset = RANDOM.nextInt(searchRadius * 2) - searchRadius;
            int zOffset = RANDOM.nextInt(searchRadius * 2) - searchRadius;
            BlockPos testPos = center.offset(xOffset, 0, zOffset);
            
            // Find ground level
            testPos = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, testPos);
            
            // Check if area is relatively flat and not occupied
            int testWidth = minSize + 5;
            int testLength = minSize + 5;
            int testHeight = QuestConfig.MIN_BUILD_HEIGHT.get() + 5;
            
            if (isAreaSuitable(testPos, level, testWidth, testLength)) {
                // Check if this area would overlap with existing buildings
                BlockPos testCorner2 = testPos.offset(testWidth - 1, testHeight - 1, testLength - 1);
                if (!areaManager.wouldOverlap(testPos, testCorner2)) {
                    return testPos;
                }
            }
        }
        return null;
    }

    private static boolean isAreaSuitable(BlockPos pos, ServerLevel level, int width, int length) {
        // Check if the area is flat enough and not heavily obstructed
        int baseY = pos.getY();
        int obstructedBlocks = 0;
        
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                BlockPos checkPos = pos.offset(x, 0, z);
                int heightDiff = Math.abs(level.getHeightmapPos(
                    net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, checkPos).getY() - baseY);
                
                if (heightDiff > 2) return false; // Too steep
                
                // Check if there are blocks above ground
                for (int y = 1; y <= 5; y++) {
                    if (!level.getBlockState(checkPos.above(y)).isAir()) {
                        obstructedBlocks++;
                    }
                }
            }
        }
        
        // Area is suitable if less than 20% obstructed
        return obstructedBlocks < (width * length * 5 * 0.2);
    }

    private static Map<Block, Integer> generateRequiredBlocks(
            VillagerQuest.QuestType questType, int width, int length, int height, Villager villager) {
        
        Map<Block, Integer> requiredBlocks = new HashMap<>();
        
        // Common building blocks (choose 1-2 types)
        List<Block> buildingBlocks = Arrays.asList(
            Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.STONE_BRICKS, 
            Blocks.COBBLESTONE, Blocks.BRICKS, Blocks.DARK_OAK_PLANKS
        );
        
        int numBlockTypes = 1 + RANDOM.nextInt(2); // 1-2 different block types
        int minBlocks = QuestConfig.MIN_REQUIRED_BLOCKS.get();
        int maxBlocks = QuestConfig.MAX_REQUIRED_BLOCKS.get();
        int totalBlocks = minBlocks + RANDOM.nextInt(maxBlocks - minBlocks + 1);
        
        int blocksPerType = totalBlocks / numBlockTypes;
        int remainingBlocks = totalBlocks;
        
        for (int i = 0; i < numBlockTypes && remainingBlocks > 0; i++) {
            Block block = buildingBlocks.get(RANDOM.nextInt(buildingBlocks.size()));
            int amount = Math.min(blocksPerType, remainingBlocks);
            requiredBlocks.put(block, amount);
            remainingBlocks -= amount;
        }
        
        // Add quest-specific requirements
        if (questType == VillagerQuest.QuestType.PERSONAL_RESIDENCE) {
            requiredBlocks.put(Blocks.RED_BED, 1);
        } else if (questType == VillagerQuest.QuestType.WORKPLACE) {
            // Get profession-specific workstation
            Block workstation = getWorkstationForProfession(villager.getVillagerData().getProfession());
            requiredBlocks.put(workstation, 1);
        }
        
        return requiredBlocks;
    }

    private static Block getWorkstationForProfession(VillagerProfession profession) {
        if (profession == VillagerProfession.ARMORER) return Blocks.BLAST_FURNACE;
        if (profession == VillagerProfession.BUTCHER) return Blocks.SMOKER;
        if (profession == VillagerProfession.CARTOGRAPHER) return Blocks.CARTOGRAPHY_TABLE;
        if (profession == VillagerProfession.CLERIC) return Blocks.BREWING_STAND;
        if (profession == VillagerProfession.FARMER) return Blocks.COMPOSTER;
        if (profession == VillagerProfession.FISHERMAN) return Blocks.BARREL;
        if (profession == VillagerProfession.FLETCHER) return Blocks.FLETCHING_TABLE;
        if (profession == VillagerProfession.LEATHERWORKER) return Blocks.CAULDRON;
        if (profession == VillagerProfession.LIBRARIAN) return Blocks.LECTERN;
        if (profession == VillagerProfession.MASON) return Blocks.STONECUTTER;
        if (profession == VillagerProfession.SHEPHERD) return Blocks.LOOM;
        if (profession == VillagerProfession.TOOLSMITH) return Blocks.SMITHING_TABLE;
        if (profession == VillagerProfession.WEAPONSMITH) return Blocks.GRINDSTONE;
        return Blocks.CRAFTING_TABLE; // Default
    }

    private static int calculateReward(int width, int length, int height, Map<Block, Integer> requiredBlocks) {
        // Base reward on structure volume
        int volume = width * length * height;
        int baseReward = volume / 10;
        
        // Add bonus for required blocks
        int blockBonus = requiredBlocks.values().stream().mapToInt(Integer::intValue).sum() / 20;
        
        return Math.max(5, Math.min(64, baseReward + blockBonus)); // Between 5-64 emeralds
    }
}
