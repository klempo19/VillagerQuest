package com.klemp.villagerquest.network;

import com.klemp.villagerquest.item.QuestBookItem;
import com.klemp.villagerquest.ModItems;
import com.klemp.villagerquest.quest.MarkerManager;
import com.klemp.villagerquest.quest.PlayerQuestManager;
import com.klemp.villagerquest.quest.QuestManager;
import com.klemp.villagerquest.quest.VillagerQuest;
import com.klemp.villagerquest.quest.VillagerQuestStatus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class QuestResponsePacket {
    private final UUID villagerUUID;
    private final boolean accepted;

    public QuestResponsePacket(UUID villagerUUID, boolean accepted) {
        this.villagerUUID = villagerUUID;
        this.accepted = accepted;
    }

    public QuestResponsePacket(FriendlyByteBuf buf) {
        this.villagerUUID = buf.readUUID();
        this.accepted = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(villagerUUID);
        buf.writeBoolean(accepted);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ServerLevel level = (ServerLevel) player.level();
                QuestManager questManager = QuestManager.get(level);
                PlayerQuestManager playerQuestManager = PlayerQuestManager.get(level);
                VillagerQuestStatus statusManager = VillagerQuestStatus.get(level);
                
                if (accepted) {
                    // Check if player already has a quest
                    if (playerQuestManager.hasActiveQuest(player.getUUID())) {
                        questManager.removeQuest(villagerUUID);
                        return; // Don't send message, screen already shows this
                    }
                    
                    // Check if quest is already taken
                    Set<UUID> questPlayers = playerQuestManager.getPlayersOnQuest(villagerUUID);
                    if (!questPlayers.isEmpty()) {
                        return; // Don't send message, screen already shows this
                    }
                    
                    // Activate the quest and place markers
                    VillagerQuest quest = questManager.getQuestForVillager(villagerUUID);
                    if (quest != null) {
                        playerQuestManager.assignQuest(player.getUUID(), villagerUUID);
                        
                        // Give player quest book
                        ItemStack questBook = new ItemStack(ModItems.QUEST_BOOK.get());
                        if (!player.getInventory().add(questBook)) {
                            player.drop(questBook, false);
                        }
                        
                        // Only place markers for building quests, not delivery
                        if (!quest.isDeliveryQuest()) {
                            MarkerManager.placeMarkersForQuest(quest, level);
                            player.sendSystemMessage(Component.literal("§a✓ Quest accepted! Check your Quest Book for details."));
                        } else {
                            player.sendSystemMessage(Component.literal("§a✓ Delivery quest accepted! Check your Quest Book."));
                        }
                        
                        // Mark that this villager has offered a quest
                        statusManager.setQuestOffered(villagerUUID);
                    }
                } else {
                    // Remove the quest
                    questManager.removeQuest(villagerUUID);
                    // Don't send message, user just closed the screen
                }
            }
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}
