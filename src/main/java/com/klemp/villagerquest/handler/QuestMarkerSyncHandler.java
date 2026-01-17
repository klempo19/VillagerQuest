package com.klemp.villagerquest.handler;

import com.klemp.villagerquest.network.NetworkHandler;
import com.klemp.villagerquest.network.SyncQuestMarkersPacket;
import com.klemp.villagerquest.quest.QuestManager;
import com.klemp.villagerquest.quest.VillagerQuestStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class QuestMarkerSyncHandler {
    private int tickCounter = 0;
    private static final int SYNC_INTERVAL = 100; // Sync every 5 seconds

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        tickCounter++;
        if (tickCounter < SYNC_INTERVAL) return;
        tickCounter = 0;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            VillagerQuestStatus statusManager = VillagerQuestStatus.get(level);
            statusManager.updateQuestMarkers(level);
            
            // Sync to all players in this level
            syncQuestMarkersToPlayers(level);
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = (ServerLevel) player.level();
            syncQuestMarkersToPlayer(level, player);
        }
    }

    @SubscribeEvent
    public void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = (ServerLevel) player.level();
            syncQuestMarkersToPlayer(level, player);
        }
    }

    private void syncQuestMarkersToPlayers(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            syncQuestMarkersToPlayer(level, player);
        }
    }

    private void syncQuestMarkersToPlayer(ServerLevel level, ServerPlayer player) {
        VillagerQuestStatus statusManager = VillagerQuestStatus.get(level);
        QuestManager questManager = QuestManager.get(level);

        Set<UUID> availableQuests = new HashSet<>();
        Set<UUID> activeQuests = new HashSet<>();

        // Get all villagers in loaded chunks near player
        for (Villager villager : level.getEntitiesOfClass(
                Villager.class, 
                player.getBoundingBox().inflate(128))) {
            
            UUID villagerUUID = villager.getUUID();
            
            if (questManager.hasActiveQuest(villagerUUID)) {
                activeQuests.add(villagerUUID);
            } else if (statusManager.hasQuestMarker(villagerUUID) && 
                       statusManager.canOfferQuest(villagerUUID)) {
                availableQuests.add(villagerUUID);
            }
        }

        NetworkHandler.INSTANCE.send(
            PacketDistributor.PLAYER.with(() -> player),
            new SyncQuestMarkersPacket(availableQuests, activeQuests)
        );
    }
}
