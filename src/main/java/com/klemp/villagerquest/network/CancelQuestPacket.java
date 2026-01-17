package com.klemp.villagerquest.network;

import com.klemp.villagerquest.quest.MarkerManager;
import com.klemp.villagerquest.quest.PlayerQuestManager;
import com.klemp.villagerquest.quest.QuestManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class CancelQuestPacket {
    private final UUID villagerUUID;

    public CancelQuestPacket(UUID villagerUUID) {
        this.villagerUUID = villagerUUID;
    }

    public CancelQuestPacket(FriendlyByteBuf buf) {
        this.villagerUUID = buf.readUUID();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(villagerUUID);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ServerLevel level = (ServerLevel) player.level();
                QuestManager questManager = QuestManager.get(level);
                PlayerQuestManager playerQuestManager = PlayerQuestManager.get(level);
                
                // Check if player has this quest
                UUID playerQuestVillager = playerQuestManager.getActiveQuestVillager(player.getUUID());
                if (playerQuestVillager != null && playerQuestVillager.equals(villagerUUID)) {
                    // Remove markers
                    MarkerManager.removeMarkersForQuest(villagerUUID, level);
                    
                    // Remove quest from all party members
                    var partyMembers = playerQuestManager.getQuestParty(player.getUUID());
                    for (UUID memberId : partyMembers) {
                        ServerPlayer member = level.getServer().getPlayerList().getPlayer(memberId);
                        if (member != null) {
                            member.sendSystemMessage(Component.literal("§cQuest has been cancelled."));
                        }
                        playerQuestManager.completeQuest(memberId);
                    }
                    
                    // Remove the quest from quest manager
                    questManager.removeQuest(villagerUUID);
                    
                    player.sendSystemMessage(Component.literal("§7Quest cancelled."));
                }
            }
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}
