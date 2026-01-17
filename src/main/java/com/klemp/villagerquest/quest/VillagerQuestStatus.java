package com.klemp.villagerquest.quest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VillagerQuestStatus extends SavedData {
    private static final String DATA_NAME = "villagerquest_status";
    private static final long QUEST_COOLDOWN = 72000; // 60 minutes (72000 ticks)
    
    // Maps villager UUID to when they last offered/completed a quest
    private Map<UUID, Long> lastQuestTime = new HashMap<>();
    
    // Maps villager UUID to whether they have a quest marker (available quest)
    private Map<UUID, Boolean> hasQuestMarker = new HashMap<>();

    public VillagerQuestStatus() {
        super();
    }

    public VillagerQuestStatus(CompoundTag tag) {
        this.load(tag);
    }

    public static VillagerQuestStatus get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(VillagerQuestStatus::new, VillagerQuestStatus::new, DATA_NAME);
    }

    public boolean canOfferQuest(UUID villagerUUID) {
        if (!lastQuestTime.containsKey(villagerUUID)) {
            return true;
        }
        
        long currentTime = System.currentTimeMillis();
        long lastTime = lastQuestTime.get(villagerUUID);
        return (currentTime - lastTime) >= (QUEST_COOLDOWN * 50); // Convert ticks to milliseconds
    }

    public long getTimeUntilNextQuest(UUID villagerUUID) {
        if (!lastQuestTime.containsKey(villagerUUID)) {
            return 0;
        }
        
        long currentTime = System.currentTimeMillis();
        long lastTime = lastQuestTime.get(villagerUUID);
        long elapsed = currentTime - lastTime;
        long cooldownMs = QUEST_COOLDOWN * 50;
        
        return Math.max(0, (cooldownMs - elapsed) / 50); // Return in ticks
    }

    public void setQuestOffered(UUID villagerUUID) {
        lastQuestTime.put(villagerUUID, System.currentTimeMillis());
        hasQuestMarker.put(villagerUUID, false); // Remove marker when quest is taken
        setDirty();
    }

    public void setQuestCompleted(UUID villagerUUID) {
        lastQuestTime.put(villagerUUID, System.currentTimeMillis());
        hasQuestMarker.put(villagerUUID, false);
        setDirty();
    }

    public boolean hasQuestMarker(UUID villagerUUID) {
        return hasQuestMarker.getOrDefault(villagerUUID, false);
    }

    public void updateQuestMarkers(ServerLevel level) {
        // This should be called periodically to update which villagers have quest markers
        long currentTime = System.currentTimeMillis();
        
        for (Map.Entry<UUID, Long> entry : lastQuestTime.entrySet()) {
            UUID villagerUUID = entry.getKey();
            long lastTime = entry.getValue();
            long elapsed = currentTime - lastTime;
            long cooldownMs = QUEST_COOLDOWN * 50;
            
            if (elapsed >= cooldownMs) {
                hasQuestMarker.put(villagerUUID, true);
            }
        }
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag timeList = new ListTag();
        for (Map.Entry<UUID, Long> entry : lastQuestTime.entrySet()) {
            CompoundTag timeTag = new CompoundTag();
            timeTag.putUUID("Villager", entry.getKey());
            timeTag.putLong("Time", entry.getValue());
            timeList.add(timeTag);
        }
        tag.put("LastQuestTimes", timeList);

        ListTag markerList = new ListTag();
        for (Map.Entry<UUID, Boolean> entry : hasQuestMarker.entrySet()) {
            CompoundTag markerTag = new CompoundTag();
            markerTag.putUUID("Villager", entry.getKey());
            markerTag.putBoolean("HasMarker", entry.getValue());
            markerList.add(markerTag);
        }
        tag.put("QuestMarkers", markerList);

        return tag;
    }

    public void load(CompoundTag tag) {
        lastQuestTime.clear();
        hasQuestMarker.clear();

        ListTag timeList = tag.getList("LastQuestTimes", Tag.TAG_COMPOUND);
        for (int i = 0; i < timeList.size(); i++) {
            CompoundTag timeTag = timeList.getCompound(i);
            lastQuestTime.put(timeTag.getUUID("Villager"), timeTag.getLong("Time"));
        }

        ListTag markerList = tag.getList("QuestMarkers", Tag.TAG_COMPOUND);
        for (int i = 0; i < markerList.size(); i++) {
            CompoundTag markerTag = markerList.getCompound(i);
            hasQuestMarker.put(markerTag.getUUID("Villager"), markerTag.getBoolean("HasMarker"));
        }
    }
}
