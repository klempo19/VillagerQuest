package com.klemp.villagerquest.handler;

import com.klemp.villagerquest.quest.MarkerManager;
import com.klemp.villagerquest.quest.QuestManager;
import com.klemp.villagerquest.quest.VillagerQuest;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.server.level.ServerLevel;

public class QuestValidationHandler {
    
    @SubscribeEvent
    public void onVillagerDeath(LivingDeathEvent event) {
        // Clean up quest markers if the quest-giving villager dies
        if (event.getEntity() instanceof Villager villager) {
            if (villager.level() instanceof ServerLevel serverLevel) {
                QuestManager questManager = QuestManager.get(serverLevel);
                
                if (questManager.hasActiveQuest(villager.getUUID())) {
                    MarkerManager.removeMarkersForQuest(villager.getUUID(), serverLevel);
                    questManager.removeQuest(villager.getUUID());
                }
            }
        }
    }
}