package com.klemp.villagerquest.handler;

import com.klemp.villagerquest.VillagerQuestMod;
import com.klemp.villagerquest.network.NetworkHandler;
import com.klemp.villagerquest.network.OpenQuestOfferPacket;
import com.klemp.villagerquest.quest.PlayerQuestManager;
import com.klemp.villagerquest.quest.QuestGenerator;
import com.klemp.villagerquest.quest.QuestManager;
import com.klemp.villagerquest.quest.VillagerQuest;
import com.klemp.villagerquest.quest.VillagerQuestStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class VillagerInteractionHandler {

    @SubscribeEvent
    public void onVillagerInteract(PlayerInteractEvent.EntityInteract event) {
        // Debug log
        VillagerQuestMod.LOGGER.info("Entity interact event triggered");
        
        if (!(event.getTarget() instanceof Villager villager)) {
            VillagerQuestMod.LOGGER.info("Target is not a villager");
            return;
        }
        
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            VillagerQuestMod.LOGGER.info("Not on server side");
            return;
        }
        
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            VillagerQuestMod.LOGGER.info("Entity is not a player");
            return;
        }

        VillagerQuestMod.LOGGER.info("Player {} interacting with villager, sneaking: {}", 
            player.getName().getString(), player.isShiftKeyDown());

        if (player.isShiftKeyDown()) {
            VillagerQuestMod.LOGGER.info("Player is shift-clicking!");
            event.setCanceled(true);
            
            QuestManager questManager = QuestManager.get(serverLevel);
            PlayerQuestManager playerQuestManager = PlayerQuestManager.get(serverLevel);
            VillagerQuestStatus statusManager = VillagerQuestStatus.get(serverLevel);
            
            UUID playerQuestVillager = playerQuestManager.getActiveQuestVillager(player.getUUID());
            boolean playerHasThisQuest = playerQuestVillager != null && 
                                        playerQuestVillager.equals(villager.getUUID());
            
            // Check if player has THIS quest active
            if (playerHasThisQuest) {
                VillagerQuestMod.LOGGER.info("Player has this villager's quest active");
                VillagerQuest quest = questManager.getQuestForVillager(villager.getUUID());
                
                if (quest == null) {
                    VillagerQuestMod.LOGGER.warn("Quest is null but player has it assigned!");
                    return;
                }
                
                if (quest.isCompleted()) {
                    VillagerQuestMod.LOGGER.info("Quest already completed");
                    return;
                }
                
                // Show turn-in screen with validation option
                BlockPos min = quest.getMinPos();
                BlockPos max = quest.getMaxPos();
                int width = max.getX() - min.getX() + 1;
                int length = max.getZ() - min.getZ() + 1;
                int height = max.getY() - min.getY() + 1;
                
                Set<UUID> partyMembers = playerQuestManager.getQuestParty(player.getUUID());
                List<String> memberNames = new ArrayList<>();
                for (UUID memberId : partyMembers) {
                    ServerPlayer member = serverLevel.getServer().getPlayerList().getPlayer(memberId);
                    if (member != null && !member.getUUID().equals(player.getUUID())) {
                        memberNames.add(member.getName().getString());
                    }
                }
                
                VillagerQuestMod.LOGGER.info("Opening quest turn-in screen");
                NetworkHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new OpenQuestOfferPacket(
                        villager.getUUID(),
                        quest.getQuestType().name(),
                        quest.getRequiredBlocks(),
                        quest.getRewardEmeralds(),
                        width, length, height,
                        true, true, true, memberNames
                    )
                );
                return;
            }
            
            // Check if this villager has an active quest (taken by someone)
            if (questManager.hasActiveQuest(villager.getUUID())) {
                VillagerQuestMod.LOGGER.info("Villager has active quest but not by this player");
                player.sendSystemMessage(Component.literal("§cThis villager's quest has already been taken by another player."));
                return;
            }
            
            // Check if villager can offer quest
            if (!statusManager.canOfferQuest(villager.getUUID())) {
                VillagerQuestMod.LOGGER.info("Villager is on cooldown");
                long remainingTime = statusManager.getTimeUntilNextQuest(villager.getUUID());
                long minutes = (remainingTime / 1200) + 1;
                player.sendSystemMessage(Component.literal("§7This villager has no quests available. Try again in " + minutes + " minutes."));
                return;
            }
            
            boolean playerHasAnyQuest = playerQuestManager.hasActiveQuest(player.getUUID());
            
            if (playerHasAnyQuest) {
                VillagerQuestMod.LOGGER.info("Player already has a quest");
                player.sendSystemMessage(Component.literal("§cYou already have an active quest! Complete it before accepting another."));
                return;
            }
            
            VillagerQuestMod.LOGGER.info("Generating new quest for villager");
            VillagerQuest newQuest = QuestGenerator.generateQuest(villager, serverLevel);
            if (newQuest != null) {
                questManager.addQuest(newQuest);
                
                BlockPos min = newQuest.getMinPos();
                BlockPos max = newQuest.getMaxPos();
                int width = max.getX() - min.getX() + 1;
                int length = max.getZ() - min.getZ() + 1;
                int height = max.getY() - min.getY() + 1;
                
                VillagerQuestMod.LOGGER.info("Opening quest offer screen");
                NetworkHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new OpenQuestOfferPacket(
                        villager.getUUID(),
                        newQuest.getQuestType().name(),
                        newQuest.getRequiredBlocks(),
                        newQuest.getRewardEmeralds(),
                        width, length, height,
                        false, playerHasAnyQuest, false, new ArrayList<>()
                    )
                );
            } else {
                VillagerQuestMod.LOGGER.warn("Failed to generate quest!");
            }
        }
    }
}
