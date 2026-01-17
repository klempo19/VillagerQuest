package com.klemp.villagerquest.network;

import com.klemp.villagerquest.ModItems;
import com.klemp.villagerquest.quest.MarkerManager;
import com.klemp.villagerquest.quest.PlayerQuestManager;
import com.klemp.villagerquest.quest.QuestManager;
import com.klemp.villagerquest.quest.StructureValidator;
import com.klemp.villagerquest.quest.VillagerQuest;
import com.klemp.villagerquest.quest.VillagerQuestStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class CheckQuestCompletionPacket {
    private final UUID villagerUUID;

    public CheckQuestCompletionPacket(UUID villagerUUID) {
        this.villagerUUID = villagerUUID;
    }

    public CheckQuestCompletionPacket(FriendlyByteBuf buf) {
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
                VillagerQuestStatus statusManager = VillagerQuestStatus.get(level);
                
                VillagerQuest quest = questManager.getQuestForVillager(villagerUUID);
                
                if (quest != null && quest.isActive() && !quest.isCompleted()) {
                    if (quest.isValidationInProgress()) {
                        player.sendSystemMessage(Component.literal("§eQuest validation is already in progress..."));
                        return;
                    }

                    // Check if it's a delivery quest - open delivery screen instead
                    if (quest.isDeliveryQuest()) {
                        NetworkHandler.INSTANCE.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new OpenDeliveryScreenPacket(villagerUUID, quest.getRequiredBlocks(), quest.getRewardEmeralds())
                        );
                        return;
                    }

                    // Building quest validation
                    StructureValidator.ValidationResult result = 
                        StructureValidator.validateStructureWithReasons(quest, level);
                    
                    if (result.isValid()) {
                        // Find the villager
                        Villager villager = findVillager(level, villagerUUID);
                        if (villager == null) {
                            player.sendSystemMessage(Component.literal("§cThe quest-giving villager could not be found!"));
                            return;
                        }

                        // Mark validation as in progress
                        quest.setValidationInProgress(true);
                        
                        // Save villager's original position
                        BlockPos originalPos = villager.blockPosition();
                        
                        player.sendSystemMessage(Component.literal("§eThe villager is checking the building..."));
                        
                        // Start async pathfinding check
                        level.getServer().execute(() -> {
                            validateVillagerAccess(quest, villager, originalPos, level, questManager, 
                                playerQuestManager, statusManager, player);
                        });
                    } else {
                        // Send failure packet with reasons
                        NetworkHandler.INSTANCE.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new QuestResultPacket(false, 0, result.getFailureReasons())
                        );
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
        return true;
    }

    private void validateVillagerAccess(VillagerQuest quest, Villager villager, BlockPos originalPos,
                                       ServerLevel level, QuestManager questManager, 
                                       PlayerQuestManager playerQuestManager,
                                       VillagerQuestStatus statusManager, ServerPlayer player) {
        BlockPos targetPos;
        
        // Find the bed or workstation
        if (quest.getQuestType() == VillagerQuest.QuestType.PERSONAL_RESIDENCE) {
            targetPos = findBedInArea(quest, level);
        } else {
            targetPos = findWorkstationInArea(quest, level);
        }

        if (targetPos == null) {
            quest.setValidationInProgress(false);
            player.sendSystemMessage(Component.literal("§cCould not find required furniture!"));
            return;
        }

        // Check if villager can path to target
        PathNavigation navigation = villager.getNavigation();
        Path pathToTarget = navigation.createPath(targetPos, 1);
        
        if (pathToTarget == null || !pathToTarget.canReach()) {
            quest.setValidationInProgress(false);
            NetworkHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new QuestResultPacket(false, 0, java.util.List.of("The villager cannot reach the required furniture!"))
            );
            return;
        }

        // Check if villager can path back to original position
        Path pathBack = navigation.createPath(originalPos, 1);
        
        if (pathBack == null || !pathBack.canReach()) {
            quest.setValidationInProgress(false);
            NetworkHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new QuestResultPacket(false, 0, java.util.List.of("The villager cannot return from the building!"))
            );
            return;
        }

        // All checks passed - complete quest
        quest.setValidationInProgress(false);
        completeQuest(quest, level, questManager, playerQuestManager, statusManager, player);
    }

    private void completeQuest(VillagerQuest quest, ServerLevel level, QuestManager questManager,
                              PlayerQuestManager playerQuestManager, VillagerQuestStatus statusManager,
                              ServerPlayer player) {
        // Remove markers BEFORE completing quest
        MarkerManager.removeMarkersForQuest(quest.getVillagerUUID(), level);
        
        // Now complete the quest
        questManager.completeQuest(quest.getVillagerUUID());
        statusManager.setQuestCompleted(quest.getVillagerUUID());
        
        // Get all players in the party
        Set<UUID> partyMembers = playerQuestManager.getPlayersOnQuest(quest.getVillagerUUID());
        
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
                
                // Send success packet to this player
                NetworkHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> member),
                    new QuestResultPacket(true, quest.getRewardEmeralds(), null)
                );
            }
            
            // Remove quest from player
            playerQuestManager.completeQuest(memberUUID);
        }
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

    private Villager findVillager(ServerLevel level, UUID villagerUUID) {
        for (Villager villager : level.getEntitiesOfClass(Villager.class, 
                new net.minecraft.world.phys.AABB(-30000000, -1000, -30000000, 30000000, 1000, 30000000))) {
            if (villager.getUUID().equals(villagerUUID)) {
                return villager;
            }
        }
        return null;
    }

    private BlockPos findBedInArea(VillagerQuest quest, ServerLevel level) {
        BlockPos min = quest.getMinPos();
        BlockPos max = quest.getMaxPos();
        
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getBlockState(pos).getBlock().getName().getString().toLowerCase().contains("bed")) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    private BlockPos findWorkstationInArea(VillagerQuest quest, ServerLevel level) {
        BlockPos min = quest.getMinPos();
        BlockPos max = quest.getMaxPos();
        
        for (var entry : quest.getRequiredBlocks().entrySet()) {
            Block requiredBlock = entry.getKey();
            String blockName = requiredBlock.getName().getString().toLowerCase();
            
            // Check if this is likely a workstation
            if (blockName.contains("table") || blockName.contains("furnace") || 
                blockName.contains("stand") || blockName.contains("composter") ||
                blockName.contains("barrel") || blockName.contains("loom") ||
                blockName.contains("grindstone") || blockName.contains("lectern") ||
                blockName.contains("cauldron") || blockName.contains("stonecutter")) {
                
                for (int x = min.getX(); x <= max.getX(); x++) {
                    for (int y = min.getY(); y <= max.getY(); y++) {
                        for (int z = min.getZ(); z <= max.getZ(); z++) {
                            BlockPos pos = new BlockPos(x, y, z);
                            if (level.getBlockState(pos).getBlock() == requiredBlock) {
                                return pos;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}
