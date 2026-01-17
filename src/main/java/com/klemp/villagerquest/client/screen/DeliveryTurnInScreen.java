package com.klemp.villagerquest.client.screen;

import com.klemp.villagerquest.network.NetworkHandler;
import com.klemp.villagerquest.network.SubmitDeliveryPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.*;

public class DeliveryTurnInScreen extends Screen {
    private final UUID villagerUUID;
    private final Map<Block, Integer> requiredItems;
    private final int reward;
    private final Inventory playerInventory;
    
    // Delivery slots (items player places here)
    private final Map<Integer, ItemStack> deliverySlots = new HashMap<>();
    private static final int MAX_SLOTS = 9; // 3x3 grid
    
    private static final int WINDOW_WIDTH = 176; // Standard inventory width
    private static final int WINDOW_HEIGHT = 140; // Smaller height
    
    private int slotHoverIndex = -1;
    private int leftPos;
    private int topPos;

    public DeliveryTurnInScreen(UUID villagerUUID, Map<Block, Integer> requiredItems, 
                               int reward, Inventory playerInventory) {
        super(Component.literal("Deliver Items"));
        this.villagerUUID = villagerUUID;
        this.requiredItems = requiredItems;
        this.reward = reward;
        this.playerInventory = playerInventory;
        
        // Initialize empty delivery slots
        for (int i = 0; i < MAX_SLOTS; i++) {
            deliverySlots.put(i, ItemStack.EMPTY);
        }
    }

    @Override
    protected void init() {
        super.init();
        
        // Center the window but leave room for hotbar at bottom
        this.leftPos = (this.width - WINDOW_WIDTH) / 2;
        this.topPos = (this.height - WINDOW_HEIGHT) / 2 - 20; // Shifted up for hotbar
        
        // Submit button
        this.addRenderableWidget(Button.builder(
            Component.literal("Submit"),
            button -> {
                // Collect all items from delivery slots
                List<ItemStack> items = new ArrayList<>();
                for (ItemStack stack : deliverySlots.values()) {
                    if (!stack.isEmpty()) {
                        items.add(stack.copy());
                    }
                }
                NetworkHandler.INSTANCE.sendToServer(new SubmitDeliveryPacket(villagerUUID, items));
                this.onClose();
            })
            .bounds(leftPos + 10, topPos + WINDOW_HEIGHT - 24, 70, 20)
            .build());
        
        // Cancel button
        this.addRenderableWidget(Button.builder(
            Component.literal("Cancel"),
            button -> this.onClose())
            .bounds(leftPos + WINDOW_WIDTH - 80, topPos + WINDOW_HEIGHT - 24, 70, 20)
            .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        
        // Background panel
        graphics.fill(leftPos, topPos, leftPos + WINDOW_WIDTH, topPos + WINDOW_HEIGHT, 0xC0101010);
        
        // Border
        graphics.fill(leftPos, topPos, leftPos + WINDOW_WIDTH, topPos + 2, 0xFF8B8B8B);
        graphics.fill(leftPos, topPos + WINDOW_HEIGHT - 2, leftPos + WINDOW_WIDTH, topPos + WINDOW_HEIGHT, 0xFF8B8B8B);
        graphics.fill(leftPos, topPos, leftPos + 2, topPos + WINDOW_HEIGHT, 0xFF8B8B8B);
        graphics.fill(leftPos + WINDOW_WIDTH - 2, topPos, leftPos + WINDOW_WIDTH, topPos + WINDOW_HEIGHT, 0xFF8B8B8B);
        
        // Title
        graphics.drawString(this.font, "§6Deliver Items", leftPos + 8, topPos + 6, 0xFFFFFF);
        
        // Required items list (compact)
        int yOffset = 20;
        int itemCount = 0;
        for (Map.Entry<Block, Integer> entry : requiredItems.entrySet()) {
            if (itemCount >= 2) break; // Only show first 2 items to save space
            
            String blockName = entry.getKey().getName().getString();
            if (blockName.length() > 15) {
                blockName = blockName.substring(0, 12) + "...";
            }
            
            int delivered = countItemInSlots(entry.getKey().asItem());
            int required = entry.getValue();
            
            String color = delivered >= required ? "§a" : "§f";
            String text = color + delivered + "/" + required + " §7" + blockName;
            graphics.drawString(this.font, text, leftPos + 10, topPos + yOffset, 0xFFFFFF);
            yOffset += 10;
            itemCount++;
        }
        
        if (requiredItems.size() > 2) {
            graphics.drawString(this.font, "§7+" + (requiredItems.size() - 2) + " more...", 
                leftPos + 10, topPos + yOffset, 0xFFFFFF);
        }
        
        // Delivery slots area
        graphics.drawString(this.font, "§ePlaced Items:", leftPos + 10, topPos + 55, 0xFFFFFF);
        
        // Draw 3x3 grid of slots - centered
        int slotStartX = leftPos + (WINDOW_WIDTH / 2) - 27; // Center the 3x3 grid (54 pixels wide)
        int slotStartY = topPos + 68;
        
        slotHoverIndex = -1;
        
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotIndex = row * 3 + col;
                int slotX = slotStartX + col * 18;
                int slotY = slotStartY + row * 18;
                
                // Check if mouse is over this slot
                boolean hovered = mouseX >= slotX && mouseX < slotX + 16 &&
                                 mouseY >= slotY && mouseY < slotY + 16;
                
                if (hovered) {
                    slotHoverIndex = slotIndex;
                }
                
                // Slot background
                int slotColor = hovered ? 0xFFAAAAAA : 0xFF8B8B8B;
                graphics.fill(slotX, slotY, slotX + 16, slotY + 16, slotColor);
                graphics.fill(slotX, slotY, slotX + 16, slotY + 1, 0xFF555555);
                graphics.fill(slotX, slotY, slotX + 1, slotY + 16, 0xFF555555);
                
                // Render item in slot
                ItemStack stack = deliverySlots.get(slotIndex);
                if (!stack.isEmpty()) {
                    graphics.renderItem(stack, slotX, slotY);
                    graphics.renderItemDecorations(this.font, stack, slotX, slotY);
                }
            }
        }
        
        // Reward (compact)
        graphics.drawString(this.font, "§6Reward: §a" + reward + " Emeralds", 
            leftPos + WINDOW_WIDTH - 85, topPos + 6, 0xFFFFFF);
        
        // Render player's hotbar at bottom (mimic vanilla inventory)
        renderPlayerHotbar(graphics);
        
        // Render item tooltip if hovering (but not if carrying an item)
        ItemStack carriedStack = this.minecraft.player.containerMenu.getCarried();
        if (slotHoverIndex >= 0 && carriedStack.isEmpty()) {
            ItemStack stack = deliverySlots.get(slotHoverIndex);
            if (!stack.isEmpty()) {
                graphics.renderTooltip(this.font, stack, mouseX, mouseY);
            }
        }
        
        // IMPORTANT: Render carried item on cursor (drawn last so it's on top)
        if (!carriedStack.isEmpty()) {
            graphics.renderItem(carriedStack, mouseX - 8, mouseY - 8);
            graphics.renderItemDecorations(this.font, carriedStack, mouseX - 8, mouseY - 8);
        }
    }

    private void renderPlayerHotbar(GuiGraphics graphics) {
        // Render player's hotbar at the bottom of screen
        int hotbarY = this.height - 22;
        int hotbarX = (this.width - 182) / 2;
        
        // Semi-transparent background
        graphics.fill(hotbarX - 1, hotbarY - 1, hotbarX + 183, hotbarY + 23, 0x80000000);
        
        // Render 9 hotbar slots
        for (int i = 0; i < 9; i++) {
            int slotX = hotbarX + i * 20 + 3;
            ItemStack stack = playerInventory.getItem(i);
            
            // Slot background
            graphics.fill(slotX, hotbarY, slotX + 16, hotbarY + 16, 0xFF8B8B8B);
            graphics.fill(slotX, hotbarY, slotX + 16, hotbarY + 1, 0xFF555555);
            graphics.fill(slotX, hotbarY, slotX + 1, hotbarY + 16, 0xFF555555);
            
            // Highlight selected slot
            if (i == playerInventory.selected) {
                graphics.fill(slotX - 1, hotbarY - 1, slotX + 17, hotbarY + 17, 0x80FFFFFF);
            }
            
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, slotX, hotbarY);
                graphics.renderItemDecorations(this.font, stack, slotX, hotbarY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check if clicking on hotbar
        int hotbarY = this.height - 22;
        int hotbarX = (this.width - 182) / 2;
        
        if (mouseY >= hotbarY && mouseY < hotbarY + 16) {
            for (int i = 0; i < 9; i++) {
                int slotX = hotbarX + i * 20 + 3;
                if (mouseX >= slotX && mouseX < slotX + 16) {
                    // Clicked on hotbar slot i
                    ItemStack hotbarStack = playerInventory.getItem(i);
                    ItemStack cursorStack = this.minecraft.player.containerMenu.getCarried();
                    
                    if (button == 0) { // Left click
                        if (!hotbarStack.isEmpty() && cursorStack.isEmpty()) {
                            // Pick up from hotbar
                            this.minecraft.player.containerMenu.setCarried(hotbarStack.copy());
                            playerInventory.setItem(i, ItemStack.EMPTY);
                            return true;
                        } else if (cursorStack.isEmpty() && hotbarStack.isEmpty()) {
                            // Do nothing, both empty
                            return true;
                        } else if (!cursorStack.isEmpty() && hotbarStack.isEmpty()) {
                            // Place into hotbar
                            playerInventory.setItem(i, cursorStack.copy());
                            this.minecraft.player.containerMenu.setCarried(ItemStack.EMPTY);
                            return true;
                        } else if (!cursorStack.isEmpty() && !hotbarStack.isEmpty()) {
                            // Swap
                            ItemStack temp = hotbarStack.copy();
                            playerInventory.setItem(i, cursorStack.copy());
                            this.minecraft.player.containerMenu.setCarried(temp);
                            return true;
                        }
                    } else if (button == 1 && !hotbarStack.isEmpty()) { // Right click
                        // Pick up from hotbar
                        this.minecraft.player.containerMenu.setCarried(hotbarStack.copy());
                        playerInventory.setItem(i, ItemStack.EMPTY);
                        return true;
                    }
                }
            }
        }
        
        // Handle delivery slot clicks
        if (slotHoverIndex >= 0 && slotHoverIndex < MAX_SLOTS) {
            ItemStack currentStack = deliverySlots.get(slotHoverIndex);
            ItemStack cursorStack = this.minecraft.player.containerMenu.getCarried();
            
            if (button == 0) { // Left click
                if (!cursorStack.isEmpty() && currentStack.isEmpty()) {
                    // Place item in slot
                    deliverySlots.put(slotHoverIndex, cursorStack.copy());
                    this.minecraft.player.containerMenu.setCarried(ItemStack.EMPTY);
                    return true;
                } else if (cursorStack.isEmpty() && !currentStack.isEmpty()) {
                    // Take item from slot
                    this.minecraft.player.containerMenu.setCarried(currentStack.copy());
                    deliverySlots.put(slotHoverIndex, ItemStack.EMPTY);
                    return true;
                } else if (!cursorStack.isEmpty() && !currentStack.isEmpty()) {
                    // Swap items
                    ItemStack temp = currentStack.copy();
                    deliverySlots.put(slotHoverIndex, cursorStack.copy());
                    this.minecraft.player.containerMenu.setCarried(temp);
                    return true;
                }
            } else if (button == 1 && !currentStack.isEmpty()) { // Right click - remove item
                this.minecraft.player.containerMenu.setCarried(currentStack.copy());
                deliverySlots.put(slotHoverIndex, ItemStack.EMPTY);
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int countItemInSlots(net.minecraft.world.item.Item item) {
        int count = 0;
        for (ItemStack stack : deliverySlots.values()) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Don't pause game
    }
}
