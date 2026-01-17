package com.klemp.villagerquest.item;

import com.klemp.villagerquest.quest.PlayerQuestManager;
import com.klemp.villagerquest.quest.QuestManager;
import com.klemp.villagerquest.quest.VillagerQuest;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

public class QuestBookItem extends Item {
    
    public QuestBookItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            ServerLevel serverLevel = (ServerLevel) level;
            PlayerQuestManager playerQuestManager = PlayerQuestManager.get(serverLevel);
            
            if (playerQuestManager.hasActiveQuest(player.getUUID())) {
                UUID villagerUUID = playerQuestManager.getActiveQuestVillager(player.getUUID());
                QuestManager questManager = QuestManager.get(serverLevel);
                VillagerQuest quest = questManager.getQuestForVillager(villagerUUID);
                
                if (quest != null) {
                    displayQuestInfo(serverPlayer, quest, serverLevel);
                    
                    // Find and show direction to villager
                    Villager villager = findVillager(serverLevel, villagerUUID);
                    if (villager != null) {
                        BlockPos villagerPos = villager.blockPosition();
                        BlockPos playerPos = player.blockPosition();
                        
                        int distance = (int) Math.sqrt(playerPos.distSqr(villagerPos));
                        String direction = getDirectionToPos(playerPos, villagerPos);
                        
                        player.sendSystemMessage(Component.literal("§6Quest Villager: §e" + 
                            distance + " blocks " + direction));
                        player.sendSystemMessage(Component.literal("§7Location: §f" + 
                            villagerPos.getX() + ", " + villagerPos.getY() + ", " + villagerPos.getZ()));
                    } else {
                        player.sendSystemMessage(Component.literal("§c⚠ Cannot locate quest villager!"));
                    }
                }
            } else {
                player.sendSystemMessage(Component.literal("§7You don't have an active quest."));
            }
        }
        
        return InteractionResultHolder.success(stack);
    }

    private void displayQuestInfo(ServerPlayer player, VillagerQuest quest, ServerLevel level) {
        player.sendSystemMessage(Component.literal("§6§l=== Quest Details ==="));
        
        String questTypeName = quest.getQuestType() == VillagerQuest.QuestType.PERSONAL_RESIDENCE ? 
            "Personal Residence" : 
            (quest.getQuestType() == VillagerQuest.QuestType.WORKPLACE ? "Workplace" : "Delivery");
        
        player.sendSystemMessage(Component.literal("§eType: §f" + questTypeName));
        
        if (!quest.isDeliveryQuest()) {
            var min = quest.getMinPos();
            var max = quest.getMaxPos();
            int width = max.getX() - min.getX() + 1;
            int length = max.getZ() - min.getZ() + 1;
            int height = max.getY() - min.getY() + 1;
            
            player.sendSystemMessage(Component.literal("§eDimensions: §f" + width + "x" + length + "x" + height + " blocks"));
            player.sendSystemMessage(Component.literal("§eBuild Area: §f" + min.getX() + ", " + min.getY() + ", " + min.getZ()));
            
            int distanceToBuild = (int) Math.sqrt(player.blockPosition().distSqr(min));
            player.sendSystemMessage(Component.literal("§eDistance to build site: §f" + distanceToBuild + " blocks"));
        }
        
        player.sendSystemMessage(Component.literal("§eRequired Materials:"));
        for (var entry : quest.getRequiredBlocks().entrySet()) {
            String blockName = entry.getKey().getName().getString();
            player.sendSystemMessage(Component.literal("  §f• " + entry.getValue() + "x §7" + blockName));
        }
        
        player.sendSystemMessage(Component.literal("§eReward: §a" + quest.getRewardEmeralds() + " Emeralds"));
        player.sendSystemMessage(Component.literal("§6§l=================="));
    }

    private String getDirectionToPos(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        
        // Determine primary direction
        String ns = dz > 0 ? "South" : "North";
        String ew = dx > 0 ? "East" : "West";
        
        // If one direction is much stronger, only show that
        if (Math.abs(dx) > Math.abs(dz) * 2) {
            return ew;
        } else if (Math.abs(dz) > Math.abs(dx) * 2) {
            return ns;
        } else {
            // Show both directions
            return ns + "-" + ew;
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

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Right-click to view quest").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7and locate villager").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§6✓ Active Quest").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Give it enchanted glint
    }
}
