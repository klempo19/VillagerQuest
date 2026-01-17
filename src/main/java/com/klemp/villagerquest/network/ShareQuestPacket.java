package com.klemp.villagerquest.network;

import com.klemp.villagerquest.quest.PlayerQuestManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class ShareQuestPacket {
    private final UUID villagerUUID;

    public ShareQuestPacket(UUID villagerUUID) {
        this.villagerUUID = villagerUUID;
    }

    public ShareQuestPacket(FriendlyByteBuf buf) {
        this.villagerUUID = buf.readUUID();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(villagerUUID);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                ServerLevel level = (ServerLevel) sender.level();
                PlayerQuestManager playerQuestManager = PlayerQuestManager.get(level);
                
                // Check if sender has this quest active
                UUID senderQuest = playerQuestManager.getActiveQuestVillager(sender.getUUID());
                if (senderQuest == null || !senderQuest.equals(villagerUUID)) {
                    sender.sendSystemMessage(Component.literal("§cYou don't have an active quest from this villager!"));
                    return;
                }
                
                // Find nearby players within 10 blocks
                AABB searchArea = sender.getBoundingBox().inflate(10.0);
                List<ServerPlayer> nearbyPlayers = level.getEntitiesOfClass(
                    ServerPlayer.class, 
                    searchArea,
                    p -> !p.getUUID().equals(sender.getUUID())
                );
                
                if (nearbyPlayers.isEmpty()) {
                    sender.sendSystemMessage(Component.literal("§cNo players nearby to share quest with!"));
                    return;
                }
                
                boolean sharedWithSomeone = false;
                for (ServerPlayer target : nearbyPlayers) {
                    if (!playerQuestManager.hasActiveQuest(target.getUUID())) {
                        // Share quest with this player
                        if (playerQuestManager.shareQuest(sender.getUUID(), target.getUUID())) {
                            target.sendSystemMessage(Component.literal(
                                "§a" + sender.getName().getString() + " shared a quest with you!"));
                            sharedWithSomeone = true;
                        }
                    }
                }
                
                if (sharedWithSomeone) {
                    sender.sendSystemMessage(Component.literal("§aQuest shared with nearby players!"));
                } else {
                    sender.sendSystemMessage(Component.literal("§cNo eligible players nearby (they may already have quests)."));
                }
            }
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}