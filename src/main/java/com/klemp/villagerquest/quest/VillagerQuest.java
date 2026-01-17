package com.klemp.villagerquest.quest;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VillagerQuest {
    private UUID villagerUUID;
    private BlockPos corner1;
    private BlockPos corner2;
    private QuestType questType;
    private Map<Block, Integer> requiredBlocks;
    private int rewardEmeralds;
    private boolean isActive;
    private boolean isCompleted;
    private UUID targetVillagerUUID; // For delivery quests
    private boolean validationInProgress;

    public enum QuestType {
        PERSONAL_RESIDENCE,
        WORKPLACE,
        DELIVERY
    }

    public VillagerQuest(UUID villagerUUID, BlockPos corner1, BlockPos corner2, 
                        QuestType questType, Map<Block, Integer> requiredBlocks, int reward) {
        this(villagerUUID, corner1, corner2, questType, requiredBlocks, reward, null);
    }

    public VillagerQuest(UUID villagerUUID, BlockPos corner1, BlockPos corner2, 
                        QuestType questType, Map<Block, Integer> requiredBlocks, int reward,
                        UUID targetVillagerUUID) {
        this.villagerUUID = villagerUUID;
        this.corner1 = corner1;
        this.corner2 = corner2;
        this.questType = questType;
        this.requiredBlocks = requiredBlocks;
        this.rewardEmeralds = reward;
        this.isActive = true;
        this.isCompleted = false;
        this.targetVillagerUUID = targetVillagerUUID;
        this.validationInProgress = false;
    }

    // Getters
    public UUID getVillagerUUID() { return villagerUUID; }
    public BlockPos getCorner1() { return corner1; }
    public BlockPos getCorner2() { return corner2; }
    public QuestType getQuestType() { return questType; }
    public Map<Block, Integer> getRequiredBlocks() { return requiredBlocks; }
    public int getRewardEmeralds() { return rewardEmeralds; }
    public boolean isActive() { return isActive; }
    public boolean isCompleted() { return isCompleted; }
    public UUID getTargetVillagerUUID() { return targetVillagerUUID; }
    public boolean isValidationInProgress() { return validationInProgress; }

    // Setters
    public void setCompleted(boolean completed) { this.isCompleted = completed; }
    public void setActive(boolean active) { this.isActive = active; }
    public void setValidationInProgress(boolean inProgress) { this.validationInProgress = inProgress; }

    public boolean isDeliveryQuest() {
        return questType == QuestType.DELIVERY;
    }

    // NBT Serialization for saving/loading
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("VillagerUUID", villagerUUID);
        tag.put("Corner1", NbtUtils.writeBlockPos(corner1));
        tag.put("Corner2", NbtUtils.writeBlockPos(corner2));
        tag.putString("QuestType", questType.name());
        tag.putInt("Reward", rewardEmeralds);
        tag.putBoolean("Active", isActive);
        tag.putBoolean("Completed", isCompleted);
        tag.putBoolean("ValidationInProgress", validationInProgress);

        if (targetVillagerUUID != null) {
            tag.putUUID("TargetVillager", targetVillagerUUID);
        }

        // Save required blocks
        CompoundTag blocksTag = new CompoundTag();
        int index = 0;
        for (Map.Entry<Block, Integer> entry : requiredBlocks.entrySet()) {
            CompoundTag blockEntry = new CompoundTag();
            blockEntry.putString("Block", ForgeRegistries.BLOCKS.getKey(entry.getKey()).toString());
            blockEntry.putInt("Count", entry.getValue());
            blocksTag.put("Block" + index, blockEntry);
            index++;
        }
        tag.put("RequiredBlocks", blocksTag);
        tag.putInt("BlockCount", index);

        return tag;
    }

    public static VillagerQuest deserializeNBT(CompoundTag tag) {
        UUID villagerUUID = tag.getUUID("VillagerUUID");
        BlockPos corner1 = NbtUtils.readBlockPos(tag.getCompound("Corner1"));
        BlockPos corner2 = NbtUtils.readBlockPos(tag.getCompound("Corner2"));
        QuestType questType = QuestType.valueOf(tag.getString("QuestType"));
        int reward = tag.getInt("Reward");

        UUID targetVillagerUUID = tag.contains("TargetVillager") ? tag.getUUID("TargetVillager") : null;

        // Load required blocks
        Map<Block, Integer> requiredBlocks = new HashMap<>();
        CompoundTag blocksTag = tag.getCompound("RequiredBlocks");
        int blockCount = tag.getInt("BlockCount");
        for (int i = 0; i < blockCount; i++) {
            CompoundTag blockEntry = blocksTag.getCompound("Block" + i);
            Block block = ForgeRegistries.BLOCKS.getValue(
                new net.minecraft.resources.ResourceLocation(blockEntry.getString("Block"))
            );
            int count = blockEntry.getInt("Count");
            if (block != null && block != Blocks.AIR) {
                requiredBlocks.put(block, count);
            }
        }

        VillagerQuest quest = new VillagerQuest(villagerUUID, corner1, corner2, questType, requiredBlocks, reward, targetVillagerUUID);
        quest.setActive(tag.getBoolean("Active"));
        quest.setCompleted(tag.getBoolean("Completed"));
        quest.setValidationInProgress(tag.getBoolean("ValidationInProgress"));
        return quest;
    }

    // Calculate the build area
    public BlockPos getMinPos() {
        return new BlockPos(
            Math.min(corner1.getX(), corner2.getX()),
            Math.min(corner1.getY(), corner2.getY()),
            Math.min(corner1.getZ(), corner2.getZ())
        );
    }

    public BlockPos getMaxPos() {
        return new BlockPos(
            Math.max(corner1.getX(), corner2.getX()),
            Math.max(corner1.getY(), corner2.getY()),
            Math.max(corner1.getZ(), corner2.getZ())
        );
    }
}
