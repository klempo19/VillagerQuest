package com.klemp.villagerquest.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class QuestCompleteScreen extends Screen {
    private final boolean successful;
    private final int reward;
    private final List<String> failureReasons;
    
    private static final int WINDOW_WIDTH = 250;
    private static final int WINDOW_HEIGHT = 180;

    public QuestCompleteScreen(boolean successful, int reward, List<String> failureReasons) {
        super(Component.literal("Quest Check"));
        this.successful = successful;
        this.reward = reward;
        this.failureReasons = failureReasons != null ? failureReasons : new ArrayList<>();
    }

    @Override
    protected void init() {
        super.init();
        
        // Use Minecraft window width and height for centering
        int screenWidth = this.minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = this.minecraft.getWindow().getGuiScaledHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        
        this.addRenderableWidget(Button.builder(
            Component.literal("Close"),
            button -> this.onClose())
            .bounds(centerX - 40, centerY + 70, 80, 20)
            .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        
        // Use Minecraft window dimensions
        int screenWidth = this.minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = this.minecraft.getWindow().getGuiScaledHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        int left = centerX - WINDOW_WIDTH / 2;
        int top = centerY - WINDOW_HEIGHT / 2;
        
        int bgColor = successful ? 0xC0104010 : 0xC0401010;
        graphics.fill(left, top, left + WINDOW_WIDTH, top + WINDOW_HEIGHT, bgColor);
        
        int borderColor = successful ? 0xFF00FF00 : 0xFFFF0000;
        graphics.fill(left, top, left + WINDOW_WIDTH, top + 2, borderColor);
        graphics.fill(left, top + WINDOW_HEIGHT - 2, left + WINDOW_WIDTH, top + WINDOW_HEIGHT, borderColor);
        graphics.fill(left, top, left + 2, top + WINDOW_HEIGHT, borderColor);
        graphics.fill(left + WINDOW_WIDTH - 2, top, left + WINDOW_WIDTH, top + WINDOW_HEIGHT, borderColor);
        
        if (successful) {
            graphics.drawCenteredString(this.font, "§a§l✓ QUEST COMPLETED!", centerX, top + 15, 0xFFFFFF);
            graphics.drawCenteredString(this.font, "§eThank you for your help!", centerX, top + 35, 0xFFFFFF);
            
            graphics.drawCenteredString(this.font, "§6Reward Received:", centerX, top + 60, 0xFFFFFF);
            graphics.drawCenteredString(this.font, "§a§l" + reward + " Emeralds", centerX, top + 75, 0xFFFFFF);
            
            graphics.drawCenteredString(this.font, "§7The villager is very pleased!", centerX, top + 100, 0xFFFFFF);
            
        } else {
            graphics.drawCenteredString(this.font, "§c§l✗ Not Yet Complete", centerX, top + 15, 0xFFFFFF);
            graphics.drawCenteredString(this.font, "§eThe structure needs more work...", centerX, top + 35, 0xFFFFFF);
            
            graphics.drawString(this.font, "§7Issues found:", left + 10, top + 55, 0xFFFFFF);
            
            int yOffset = 70;
            for (String reason : failureReasons) {
                if (yOffset > top + 130) break;
                
                if (this.font.width(reason) > WINDOW_WIDTH - 30) {
                    List<String> wrapped = wrapText(reason, WINDOW_WIDTH - 30);
                    for (String line : wrapped) {
                        graphics.drawString(this.font, "§c• " + line, left + 15, top + yOffset, 0xFFFFFF);
                        yOffset += 12;
                    }
                } else {
                    graphics.drawString(this.font, "§c• " + reason, left + 15, top + yOffset, 0xFFFFFF);
                    yOffset += 12;
                }
            }
        }
    }
    
    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            if (this.font.width(currentLine + " " + word) <= maxWidth) {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    lines.add(word);
                }
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}