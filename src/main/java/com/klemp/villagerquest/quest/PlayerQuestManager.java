package com.klemp.villagerquest.quest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.*;

public class PlayerQuestManager extends SavedData {
    private static final String DATA_NAME = "villagerquest_players";
    
    // Maps player UUID to their active quest's villager UUID
    private Map<UUID, UUID> playerActiveQuests = new HashMap<>();
    
    // Maps player UUID to list of player UUIDs they're sharing quest with
    private Map<UUID, Set<UUID>> questParties = new HashMap<>();

    public PlayerQuestManager() {
        super();
    }

    public PlayerQuestManager(CompoundTag tag) {
        this.load(tag);
    }

    public static PlayerQuestManager get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(PlayerQuestManager::new, PlayerQuestManager::new, DATA_NAME);
    }

    // Check if player has an active quest
    public boolean hasActiveQuest(UUID playerUUID) {
        return playerActiveQuests.containsKey(playerUUID);
    }

    // Get the villager UUID for a player's active quest
    public UUID getActiveQuestVillager(UUID playerUUID) {
        return playerActiveQuests.get(playerUUID);
    }

    // Assign a quest to a player
    public void assignQuest(UUID playerUUID, UUID villagerUUID) {
        playerActiveQuests.put(playerUUID, villagerUUID);
        setDirty();
    }

    // Complete/remove a quest for a player
    public void completeQuest(UUID playerUUID) {
        playerActiveQuests.remove(playerUUID);
        questParties.remove(playerUUID); // Remove any party they were leading
        
        // Remove player from any party they were in
        questParties.values().forEach(party -> party.remove(playerUUID));
        setDirty();
    }

    // Share quest with another player
    public boolean shareQuest(UUID ownerUUID, UUID targetUUID) {
        if (!hasActiveQuest(ownerUUID)) return false;
        if (hasActiveQuest(targetUUID)) return false; // Target already has a quest
        
        UUID villagerUUID = playerActiveQuests.get(ownerUUID);
        
        // Add target to owner's party
        questParties.computeIfAbsent(ownerUUID, k -> new HashSet<>()).add(targetUUID);
        
        // Assign same quest to target
        playerActiveQuests.put(targetUUID, villagerUUID);
        
        setDirty();
        return true;
    }

    // Get all players working on the same quest
    public Set<UUID> getQuestParty(UUID playerUUID) {
        Set<UUID> party = new HashSet<>();
        party.add(playerUUID); // Include the player themselves
        
        // If they're the party leader, add their party members
        if (questParties.containsKey(playerUUID)) {
            party.addAll(questParties.get(playerUUID));
        } else {
            // Check if they're in someone else's party
            for (Map.Entry<UUID, Set<UUID>> entry : questParties.entrySet()) {
                if (entry.getValue().contains(playerUUID)) {
                    party.add(entry.getKey()); // Add party leader
                    party.addAll(entry.getValue()); // Add all party members
                    break;
                }
            }
        }
        
        return party;
    }

    // Get all players working on a specific villager's quest
    public Set<UUID> getPlayersOnQuest(UUID villagerUUID) {
        Set<UUID> players = new HashSet<>();
        for (Map.Entry<UUID, UUID> entry : playerActiveQuests.entrySet()) {
            if (entry.getValue().equals(villagerUUID)) {
                players.add(entry.getKey());
            }
        }
        return players;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        // Save player active quests
        ListTag questList = new ListTag();
        for (Map.Entry<UUID, UUID> entry : playerActiveQuests.entrySet()) {
            CompoundTag questTag = new CompoundTag();
            questTag.putUUID("Player", entry.getKey());
            questTag.putUUID("Villager", entry.getValue());
            questList.add(questTag);
        }
        tag.put("ActiveQuests", questList);

        // Save quest parties
        ListTag partyList = new ListTag();
        for (Map.Entry<UUID, Set<UUID>> entry : questParties.entrySet()) {
            CompoundTag partyTag = new CompoundTag();
            partyTag.putUUID("Leader", entry.getKey());
            
            ListTag memberList = new ListTag();
            for (UUID member : entry.getValue()) {
                CompoundTag memberTag = new CompoundTag();
                memberTag.putUUID("Member", member);
                memberList.add(memberTag);
            }
            partyTag.put("Members", memberList);
            partyList.add(partyTag);
        }
        tag.put("Parties", partyList);

        return tag;
    }

    public void load(CompoundTag tag) {
        playerActiveQuests.clear();
        questParties.clear();

        // Load player active quests
        ListTag questList = tag.getList("ActiveQuests", Tag.TAG_COMPOUND);
        for (int i = 0; i < questList.size(); i++) {
            CompoundTag questTag = questList.getCompound(i);
            playerActiveQuests.put(
                questTag.getUUID("Player"),
                questTag.getUUID("Villager")
            );
        }

        // Load quest parties
        ListTag partyList = tag.getList("Parties", Tag.TAG_COMPOUND);
        for (int i = 0; i < partyList.size(); i++) {
            CompoundTag partyTag = partyList.getCompound(i);
            UUID leader = partyTag.getUUID("Leader");
            
            Set<UUID> members = new HashSet<>();
            ListTag memberList = partyTag.getList("Members", Tag.TAG_COMPOUND);
            for (int j = 0; j < memberList.size(); j++) {
                CompoundTag memberTag = memberList.getCompound(j);
                members.add(memberTag.getUUID("Member"));
            }
            
            questParties.put(leader, members);
        }
    }
}