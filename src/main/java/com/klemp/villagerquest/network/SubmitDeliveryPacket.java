package com.klemp.villagerquest.network;

import com.klemp.villagerquest.ModItems;
import com.klemp.villagerquest.quest.PlayerQuestManager;
import com.klemp.villagerquest.quest.QuestManager;
import com.klemp.villagerquest.quest.VillagerQuest;
import com.klemp.villagerquest.quest.VillagerQuestStatus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;
import java.util.function.Supplier;

public class SubmitDeliveryPacket {
    private final UUID villagerUUID;
    private final List<ItemStack> deliveredItems;

    public SubmitDeliveryPacket(UUID villagerUUID, List<ItemStack> deliveredItems) {
        this.villagerUUID = villagerUUID;
        this.deliveredItems = deliveredItems;
    }

    public SubmitDeliveryPacket(FriendlyByteBuf buf) {
        this.villagerUUID = buf.readUUID();
        
        int itemCount = buf.readInt();
        this.deliveredItems = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            deliveredItems.add(buf.readItem());
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(villagerUUID);
        
        buf.writeInt(deliveredItems.size());
        for (ItemStack stack : deliveredItems) {
            buf.writeItem(stack);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ServerLevel level = (ServerLevel) player.level();
                QuestManager questManager = QuestManager.get(level);
                PlayerQuestManager playerQuestManager = PlayerQuestManager.get(level);
                VillagerQuestStatus statusManager = VillagerQuestStatus.get(level);
                
                VillagerQuest quest = questManager.getQuestForVillager(villagerUUID);
                
                if (quest == null || !quest.isDeliveryQuest()) {
                    player.sendSystemMessage(Component.literal("§cInvalid delivery quest!"));
                    return;
                }
                
                // Check if player has this quest
                UUID playerQuestVillager = playerQuestManager.getActiveQuestVillager(player.getUUID());
                if (playerQuestVillager == null || !playerQuestVillager.equals(villagerUUID)) {
                    player.sendSystemMessage(Component.literal("§cYou don't have this quest!"));
                    return;
                }
                
                // Count delivered items by type
                Map<net.minecraft.world.item.Item, Integer> deliveredCount = new HashMap<>();
                for (ItemStack stack : deliveredItems) {
                    if (!stack.isEmpty()) {
                        deliveredCount.put(stack.getItem(), 
                            deliveredCount.getOrDefault(stack.getItem(), 0) + stack.getCount());
                    }
                }
                
                // Check if all required items are delivered
                List<String> missing = new ArrayList<>();
                for (Map.Entry<Block, Integer> requirement : quest.getRequiredBlocks().entrySet()) {
                    net.minecraft.world.item.Item requiredItem = requirement.getKey().asItem();
                    int requiredCount = requirement.getValue();
                    int delivered = deliveredCount.getOrDefault(requiredItem, 0);
                    
                    if (delivered < requiredCount) {
                        missing.add((requiredCount - delivered) + "x " + 
                            requirement.getKey().getName().getString());
                    }
                }
                
                if (!missing.isEmpty()) {
                    // Not enough items
                    NetworkHandler.INSTANCE.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new QuestResultPacket(false, 0, missing)
                    );
                    return;
                }
                
                // Success! Remove items from player inventory
                for (Map.Entry<Block, Integer> requirement : quest.getRequiredBlocks().entrySet()) {
                    net.minecraft.world.item.Item requiredItem = requirement.getKey().asItem();
                    int requiredCount = requirement.getValue();
                    
                    // Remove from inventory
                    int remaining = requiredCount;
                    for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                        ItemStack slotStack = player.getInventory().getItem(slot);
                        if (!slotStack.isEmpty() && slotStack.getItem() == requiredItem) {
                            int toRemove = Math.min(remaining, slotStack.getCount());
                            slotStack.shrink(toRemove);
                            remaining -= toRemove;
                            
                            if (remaining <= 0) break;
                        }
                    }
                }
                
                // Complete quest
                questManager.completeQuest(villagerUUID);
                statusManager.setQuestCompleted(villagerUUID);
                
                // Get all players in the party
                Set<UUID> partyMembers = playerQuestManager.getPlayersOnQuest(villagerUUID);
                
                // Reward all party members
                for (UUID memberUUID : partyMembers) {
                    ServerPlayer member = level.getServer().getPlayerList().getPlayer(memberUUID);
                    if (member != null) {
                        // Remove quest book from inventory
                        removeQuestBookFromInventory(member);
                        
                        // Give reward
                        ItemStack emeralds = new ItemStack(Items.EMERALD, quest.getRewardEmeralds());
                        if (!member.getInventory().add(emeralds)) {
                            member.drop(emeralds, false);
                        }
                        
                        // Send success packet
                        NetworkHandler.INSTANCE.send(
                            PacketDistributor.PLAYER.with(() -> member),
                            new QuestResultPacket(true, quest.getRewardEmeralds(), null)
                        );
                    }
                    
                    // Remove quest from player
                    playerQuestManager.completeQuest(memberUUID);
                }
            }
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
    
    private void removeQuestBookFromInventory(ServerPlayer player) {
        // Search for quest book in inventory and remove it
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == ModItems.QUEST_BOOK.get()) {
                player.getInventory().removeItem(i, 1);
                return; // Only remove one quest book
            }
        }
    }
}
