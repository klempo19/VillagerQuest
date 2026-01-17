package com.klemp.villagerquest.quest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.*;

public class QuestManager extends SavedData {
    private static final String DATA_NAME = "villagerquest_data";
    
    private Map<UUID, VillagerQuest> activeQuests = new HashMap<>();
    private Map<UUID, VillagerQuest> villagerToQuest = new HashMap<>();

    public QuestManager() {
        super();
    }

    public QuestManager(CompoundTag tag) {
        this.load(tag);
    }

    public static QuestManager get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(QuestManager::new, QuestManager::new, DATA_NAME);
    }

    public void addQuest(VillagerQuest quest) {
        UUID questId = UUID.randomUUID();
        activeQuests.put(questId, quest);
        villagerToQuest.put(quest.getVillagerUUID(), quest);
        setDirty();
    }

    public VillagerQuest getQuestForVillager(UUID villagerUUID) {
        return villagerToQuest.get(villagerUUID);
    }

    public boolean hasActiveQuest(UUID villagerUUID) {
        VillagerQuest quest = villagerToQuest.get(villagerUUID);
        return quest != null && quest.isActive() && !quest.isCompleted();
    }

    public void completeQuest(UUID villagerUUID) {
        VillagerQuest quest = villagerToQuest.get(villagerUUID);
        if (quest != null) {
            quest.setCompleted(true);
            quest.setActive(false);
            setDirty();
        }
    }

    public void removeQuest(UUID villagerUUID) {
        VillagerQuest quest = villagerToQuest.remove(villagerUUID);
        if (quest != null) {
            activeQuests.values().remove(quest);
            setDirty();
        }
    }

    public Collection<VillagerQuest> getAllActiveQuests() {
        return activeQuests.values();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag questList = new ListTag();
        for (VillagerQuest quest : activeQuests.values()) {
            questList.add(quest.serializeNBT());
        }
        tag.put("Quests", questList);
        return tag;
    }

    public void load(CompoundTag tag) {
        activeQuests.clear();
        villagerToQuest.clear();
        
        ListTag questList = tag.getList("Quests", Tag.TAG_COMPOUND);
        for (int i = 0; i < questList.size(); i++) {
            CompoundTag questTag = questList.getCompound(i);
            VillagerQuest quest = VillagerQuest.deserializeNBT(questTag);
            UUID questId = UUID.randomUUID();
            activeQuests.put(questId, quest);
            villagerToQuest.put(quest.getVillagerUUID(), quest);
        }
    }
}