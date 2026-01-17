package com.klemp.villagerquest.client.screen;

import com.klemp.villagerquest.network.CancelQuestPacket;
import com.klemp.villagerquest.network.CheckQuestCompletionPacket;
import com.klemp.villagerquest.network.NetworkHandler;
import com.klemp.villagerquest.network.QuestResponsePacket;
import com.klemp.villagerquest.network.ShareQuestPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class QuestOfferScreen extends Screen {
    private final UUID villagerUUID;
    private final String questType;
    private final Map<Block, Integer> requiredBlocks;
    private final int reward;
    private final int width;
    private final int length;
    private final int height;
    private final boolean questTaken;
    private final boolean playerHasQuest;
    private final boolean canShare;
    private final List<String> partyMembers;
    
    private static final int WINDOW_WIDTH = 270;
    private static final int WINDOW_HEIGHT = 240;

    public QuestOfferScreen(UUID villagerUUID, String questType, 
                           Map<Block, Integer> requiredBlocks, int reward,
                           int width, int length, int height,
                           boolean questTaken, boolean playerHasQuest,
                           boolean canShare, List<String> partyMembers) {
        super(Component.literal("Villager Quest"));
        this.villagerUUID = villagerUUID;
        this.questType = questType;
        this.requiredBlocks = requiredBlocks;
        this.reward = reward;
        this.width = width;
        this.length = length;
        this.height = height;
        this.questTaken = questTaken;
        this.playerHasQuest = playerHasQuest;
        this.canShare = canShare;
        this.partyMembers = partyMembers != null ? partyMembers : new ArrayList<>();
    }

    @Override
    protected void init() {
        super.init();
        
        int screenWidth = this.minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = this.minecraft.getWindow().getGuiScaledHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        
        if (questTaken && playerHasQuest && canShare) {
            // Player has this quest - show Turn In, Share, and Cancel buttons
            this.addRenderableWidget(Button.builder(
                Component.literal("Turn In Quest"),
                button -> {
                    NetworkHandler.INSTANCE.sendToServer(new CheckQuestCompletionPacket(villagerUUID));
                    this.onClose();
                })
                .bounds(centerX - 100, centerY + 60, 90, 20)
                .build());
            
            this.addRenderableWidget(Button.builder(
                Component.literal("Share Quest"),
                button -> {
                    NetworkHandler.INSTANCE.sendToServer(new ShareQuestPacket(villagerUUID));
                    this.onClose();
                })
                .bounds(centerX + 10, centerY + 60, 90, 20)
                .build());
            
            this.addRenderableWidget(Button.builder(
                Component.literal("Cancel Quest"),
                button -> {
                    NetworkHandler.INSTANCE.sendToServer(new CancelQuestPacket(villagerUUID));
                    this.onClose();
                })
                .bounds(centerX - 100, centerY + 85, 90, 20)
                .build());
                
            this.addRenderableWidget(Button.builder(
                Component.literal("Close"),
                button -> this.onClose())
                .bounds(centerX + 10, centerY + 85, 90, 20)
                .build());
                
        } else if (questTaken && !playerHasQuest) {
            // Quest taken by someone else
            this.addRenderableWidget(Button.builder(
                Component.literal("Close"),
                button -> this.onClose())
                .bounds(centerX - 40, centerY + 95, 80, 20)
                .build());
                
        } else if (!playerHasQuest && !questTaken) {
            // New quest offer
            this.addRenderableWidget(Button.builder(
                Component.literal("Accept Quest"),
                button -> {
                    NetworkHandler.INSTANCE.sendToServer(new QuestResponsePacket(villagerUUID, true));
                    this.onClose();
                })
                .bounds(centerX - 100, centerY + 95, 90, 20)
                .build());
            
            this.addRenderableWidget(Button.builder(
                Component.literal("Reject"),
                button -> {
                    NetworkHandler.INSTANCE.sendToServer(new QuestResponsePacket(villagerUUID, false));
                    this.onClose();
                })
                .bounds(centerX + 10, centerY + 95, 90, 20)
                .build());
                
        } else {
            // Default close button
            this.addRenderableWidget(Button.builder(
                Component.literal("Close"),
                button -> this.onClose())
                .bounds(centerX - 40, centerY + 95, 80, 20)
                .build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        
        int screenWidth = this.minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = this.minecraft.getWindow().getGuiScaledHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        int left = centerX - WINDOW_WIDTH / 2;
        int top = centerY - WINDOW_HEIGHT / 2;
        
        graphics.fill(left, top, left + WINDOW_WIDTH, top + WINDOW_HEIGHT, 0xC0101010);
        graphics.fill(left, top, left + WINDOW_WIDTH, top + 2, 0xFF8B8B8B);
        graphics.fill(left, top + WINDOW_HEIGHT - 2, left + WINDOW_WIDTH, top + WINDOW_HEIGHT, 0xFF8B8B8B);
        graphics.fill(left, top, left + 2, top + WINDOW_HEIGHT, 0xFF8B8B8B);
        graphics.fill(left + WINDOW_WIDTH - 2, top, left + WINDOW_WIDTH, top + WINDOW_HEIGHT, 0xFF8B8B8B);
        
        if (questTaken && playerHasQuest && canShare) {
            graphics.drawCenteredString(this.font, "§6§lYour Active Quest", centerX, top + 10, 0xFFFFFF);
        } else {
            graphics.drawCenteredString(this.font, "§6§lVillager's Request", centerX, top + 10, 0xFFFFFF);
        }
        
        if (questTaken && !playerHasQuest) {
            graphics.drawCenteredString(this.font, "§c§lQuest Already Taken!", centerX, top + 35, 0xFFFFFF);
            graphics.drawCenteredString(this.font, "§7Another player is working on", centerX, top + 50, 0xFFFFFF);
            graphics.drawCenteredString(this.font, "§7this villager's request.", centerX, top + 62, 0xFFFFFF);
            return;
        } else if (playerHasQuest && !canShare && !questTaken) {
            graphics.drawCenteredString(this.font, "§c§lYou already have an active quest!", centerX, top + 35, 0xFFFFFF);
            graphics.drawCenteredString(this.font, "§7Complete your current quest", centerX, top + 50, 0xFFFFFF);
            graphics.drawCenteredString(this.font, "§7before accepting a new one.", centerX, top + 62, 0xFFFFFF);
            return;
        }
        
        String typeText;
        if (questType.equals("DELIVERY")) {
            typeText = "I need items delivered!";
        } else if (questType.equals("PERSONAL_RESIDENCE")) {
            typeText = "I need a personal home built!";
        } else {
            typeText = "I need a workplace built!";
        }
        graphics.drawCenteredString(this.font, "§e" + typeText, centerX, top + 28, 0xFFFFFF);
        
        if (!questType.equals("DELIVERY")) {
            graphics.drawString(this.font, "§7Dimensions:", left + 10, top + 45, 0xFFFFFF);
            graphics.drawString(this.font, "§f" + width + "x" + length + "x" + height + " blocks", 
                left + 80, top + 45, 0xFFFFFF);
        }
        
        graphics.drawString(this.font, questType.equals("DELIVERY") ? "§7Required Items:" : "§7Required Materials:", 
            left + 10, top + 60, 0xFFFFFF);
        
        int yOffset = 73;
        for (Map.Entry<Block, Integer> entry : requiredBlocks.entrySet()) {
            String blockName = entry.getKey().getName().getString();
            if (blockName.length() > 25) {
                blockName = blockName.substring(0, 22) + "...";
            }
            String text = "§f• " + entry.getValue() + "x §7" + blockName;
            graphics.drawString(this.font, text, left + 15, top + yOffset, 0xFFFFFF);
            yOffset += 12;
            
            if (yOffset > top + 130) {
                graphics.drawString(this.font, "§7  ... and more", left + 15, top + yOffset, 0xFFFFFF);
                break;
            }
        }
        
        if (!partyMembers.isEmpty()) {
            graphics.drawString(this.font, "§aParty Members:", left + 10, top + 145, 0xFFFFFF);
            int partyY = 158;
            for (String member : partyMembers) {
                if (partyY > top + 175) {
                    graphics.drawString(this.font, "§7  +" + (partyMembers.size() - 2) + " more", 
                        left + 15, top + partyY, 0xFFFFFF);
                    break;
                }
                graphics.drawString(this.font, "§7• §f" + member, left + 15, top + partyY, 0xFFFFFF);
                partyY += 12;
            }
        } else if (!questType.equals("DELIVERY")) {
            graphics.drawCenteredString(this.font, "§8Structure must be fully enclosed", 
                centerX, top + 157, 0xFFFFFF);
            graphics.drawCenteredString(this.font, "§8(doors and trapdoors allowed)", 
                centerX, top + 167, 0xFFFFFF);
        }
        
        graphics.drawCenteredString(this.font, "§6Reward: §a" + reward + " Emeralds", 
            centerX, top + 182, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
