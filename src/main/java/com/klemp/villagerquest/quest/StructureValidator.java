package com.klemp.villagerquest.quest;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class StructureValidator {

    public static class ValidationResult {
        private final boolean valid;
        private final List<String> failureReasons;

        public ValidationResult(boolean valid, List<String> failureReasons) {
            this.valid = valid;
            this.failureReasons = failureReasons;
        }

        public boolean isValid() { return valid; }
        public List<String> getFailureReasons() { return failureReasons; }
    }

    public static boolean validateStructure(VillagerQuest quest, ServerLevel level) {
        return validateStructureWithReasons(quest, level).isValid();
    }

    public static ValidationResult validateStructureWithReasons(VillagerQuest quest, ServerLevel level) {
        List<String> reasons = new ArrayList<>();
        
        // Find the actual built structure within the quest area
        StructureBounds actualStructure = findActualStructure(quest, level);
        
        if (actualStructure == null) {
            reasons.add("No structure found in the quest area");
            return new ValidationResult(false, reasons);
        }
        
        // Find the required furniture first (bed or workstation)
        BlockPos furniturePos = findRequiredFurniture(quest, actualStructure, level);
        if (furniturePos == null) {
            String required = quest.getQuestType() == VillagerQuest.QuestType.PERSONAL_RESIDENCE 
                ? "bed" : "workstation";
            reasons.add("Missing required " + required);
            return new ValidationResult(false, reasons);
        }
        
        // Use flood fill to check if structure is enclosed
        if (!isEnclosedUsingFloodFill(actualStructure, furniturePos, level)) {
            reasons.add("Structure is not fully enclosed - air leaks to outside");
        }
        
        Map<Block, Integer> missingBlocks = getMissingBlocks(quest, actualStructure, level);
        if (!missingBlocks.isEmpty()) {
            for (Map.Entry<Block, Integer> entry : missingBlocks.entrySet()) {
                reasons.add("Missing " + entry.getValue() + "x " + 
                    entry.getKey().getName().getString());
            }
        }
        
        return new ValidationResult(reasons.isEmpty(), reasons);
    }

    private static class StructureBounds {
        BlockPos min;
        BlockPos max;
        
        StructureBounds(BlockPos min, BlockPos max) {
            this.min = min;
            this.max = max;
        }
    }

    private static StructureBounds findActualStructure(VillagerQuest quest, ServerLevel level) {
        BlockPos questMin = quest.getMinPos();
        BlockPos questMax = quest.getMaxPos();
        
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        
        boolean foundAnyBlock = false;
        
        // Scan the quest area for non-air blocks
        for (int x = questMin.getX(); x <= questMax.getX(); x++) {
            for (int y = questMin.getY(); y <= questMax.getY(); y++) {
                for (int z = questMin.getZ(); z <= questMax.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    
                    if (!state.isAir() && state.getBlock() != Blocks.WATER && state.getBlock() != Blocks.LAVA) {
                        foundAnyBlock = true;
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        minZ = Math.min(minZ, z);
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                        maxZ = Math.max(maxZ, z);
                    }
                }
            }
        }
        
        if (!foundAnyBlock) {
            return null;
        }
        
        return new StructureBounds(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
    }

    /**
     * Flood Fill Algorithm to check if structure is enclosed.
     * Starts from the furniture (bed/workstation) and expands through all air blocks.
     * If it reaches outside the structure bounds, the structure is not enclosed.
     */
    private static boolean isEnclosedUsingFloodFill(StructureBounds bounds, BlockPos startPos, ServerLevel level) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(startPos);
        visited.add(startPos);
        
        // Expand bounds slightly to account for walls
        BlockPos min = bounds.min.offset(-1, -1, -1);
        BlockPos max = bounds.max.offset(1, 1, 1);
        
        // Additional safety - expand even more to detect leaks
        BlockPos outerMin = min.offset(-2, -2, -2);
        BlockPos outerMax = max.offset(2, 2, 2);
        
        int maxIterations = 10000; // Safety limit to prevent infinite loops
        int iterations = 0;
        
        while (!queue.isEmpty() && iterations < maxIterations) {
            iterations++;
            BlockPos current = queue.poll();
            
            // Check if we've leaked way outside the structure bounds
            if (current.getX() < outerMin.getX() || current.getX() > outerMax.getX() ||
                current.getY() < outerMin.getY() || current.getY() > outerMax.getY() ||
                current.getZ() < outerMin.getZ() || current.getZ() > outerMax.getZ()) {
                // We've escaped far outside! Structure is definitely not enclosed
                return false;
            }
            
            // Check all 6 adjacent blocks (up, down, north, south, east, west)
            BlockPos[] neighbors = {
                current.above(),
                current.below(),
                current.north(),
                current.south(),
                current.east(),
                current.west()
            };
            
            for (BlockPos neighbor : neighbors) {
                if (visited.contains(neighbor)) {
                    continue;
                }
                
                BlockState state = level.getBlockState(neighbor);
                
                // If it's air or truly passable (not doors!), we can move through it
                if (isPassableForFloodFill(state, neighbor, level)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        
        // If we got here without escaping, the structure is enclosed
        return true;
    }

    /**
     * Checks if a block is passable for flood fill purposes.
     * IMPORTANT: Doors should NOT be passable in flood fill!
     * The air can leak through door hitboxes even when closed.
     */
    private static boolean isPassableForFloodFill(BlockState state, BlockPos pos, ServerLevel level) {
        Block block = state.getBlock();
        
        // Air is passable
        if (state.isAir()) {
            return true;
        }
        
        // IMPORTANT: Doors and trapdoors should NOT be passable for flood fill
        // They are part of the wall structure, not openings
        if (block instanceof DoorBlock || block instanceof TrapDoorBlock) {
            return false; // Changed from true to false!
        }
        
        // Small decorative blocks that shouldn't block air flow
        if (block == Blocks.TORCH || block == Blocks.WALL_TORCH ||
            block == Blocks.REDSTONE_TORCH || block == Blocks.REDSTONE_WALL_TORCH ||
            block == Blocks.SOUL_TORCH || block == Blocks.SOUL_WALL_TORCH) {
            return true;
        }
        
        // Pressure plates, buttons, signs - these are thin and shouldn't block
        String blockName = block.getName().getString().toLowerCase();
        if (blockName.contains("pressure") || blockName.contains("button") || 
            blockName.contains("sign") || blockName.contains("banner")) {
            return true;
        }
        
        // Carpets - only if they're truly thin (height < 0.1)
        if (blockName.contains("carpet")) {
            return true;
        }
        
        return false;
    }

    private static BlockPos findRequiredFurniture(VillagerQuest quest, StructureBounds bounds, ServerLevel level) {
        BlockPos min = bounds.min;
        BlockPos max = bounds.max;
        
        // Check what's required based on quest type
        if (quest.getQuestType() == VillagerQuest.QuestType.PERSONAL_RESIDENCE) {
            // Look for any bed
            for (int x = min.getX(); x <= max.getX(); x++) {
                for (int y = min.getY(); y <= max.getY(); y++) {
                    for (int z = min.getZ(); z <= max.getZ(); z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        Block block = level.getBlockState(pos).getBlock();
                        String blockName = block.getName().getString().toLowerCase();
                        
                        if (blockName.contains("bed")) {
                            return pos;
                        }
                    }
                }
            }
        } else {
            // Look for workstation
            for (var entry : quest.getRequiredBlocks().entrySet()) {
                Block requiredBlock = entry.getKey();
                String blockName = requiredBlock.getName().getString().toLowerCase();
                
                // Check if this is a workstation block
                if (isWorkstationBlock(blockName)) {
                    for (int x = min.getX(); x <= max.getX(); x++) {
                        for (int y = min.getY(); y <= max.getY(); y++) {
                            for (int z = min.getZ(); z <= max.getZ(); z++) {
                                BlockPos pos = new BlockPos(x, y, z);
                                if (level.getBlockState(pos).getBlock() == requiredBlock) {
                                    return pos;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return null;
    }

    private static Map<Block, Integer> getMissingBlocks(VillagerQuest quest, StructureBounds bounds, ServerLevel level) {
        BlockPos min = bounds.min;
        BlockPos max = bounds.max;
        
        Map<Block, Integer> foundBlocks = new HashMap<>();
        
        // Count all blocks in the actual structure
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    Block block = level.getBlockState(pos).getBlock();
                    
                    if (block != Blocks.AIR) {
                        foundBlocks.put(block, foundBlocks.getOrDefault(block, 0) + 1);
                    }
                }
            }
        }
        
        // Calculate missing blocks
        Map<Block, Integer> missingBlocks = new HashMap<>();
        for (Map.Entry<Block, Integer> requirement : quest.getRequiredBlocks().entrySet()) {
            Block requiredBlock = requirement.getKey();
            int requiredCount = requirement.getValue();
            int foundCount = foundBlocks.getOrDefault(requiredBlock, 0);
            
            if (foundCount < requiredCount) {
                missingBlocks.put(requiredBlock, requiredCount - foundCount);
            }
        }
        
        return missingBlocks;
    }

    private static boolean isWorkstationBlock(String blockName) {
        return blockName.contains("table") || blockName.contains("furnace") || 
               blockName.contains("stand") || blockName.contains("composter") ||
               blockName.contains("barrel") || blockName.contains("loom") ||
               blockName.contains("grindstone") || blockName.contains("lectern") ||
               blockName.contains("cauldron") || blockName.contains("stonecutter");
    }
}
