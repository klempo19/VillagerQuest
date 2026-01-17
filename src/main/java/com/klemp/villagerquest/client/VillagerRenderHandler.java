package com.klemp.villagerquest.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class VillagerRenderHandler {
    // Store which villagers have quest markers (exclamation mark) or active quests (hourglass)
    private static final Set<UUID> villagersWithQuestMarkers = new HashSet<>();
    private static final Set<UUID> villagersWithActiveQuests = new HashSet<>();

    public static void setQuestMarkers(Set<UUID> markers) {
        villagersWithQuestMarkers.clear();
        villagersWithQuestMarkers.addAll(markers);
    }

    public static void setActiveQuests(Set<UUID> active) {
        villagersWithActiveQuests.clear();
        villagersWithActiveQuests.addAll(active);
    }

    @SubscribeEvent
    public static void onRenderVillager(RenderLivingEvent.Post<Villager, ?> event) {
        if (!(event.getEntity() instanceof Villager villager)) return;

        UUID villagerUUID = villager.getUUID();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();
        int packedLight = event.getPackedLight();

        poseStack.pushPose();
        
        // Position above villager's head
        poseStack.translate(0, villager.getBbHeight() + 0.5, 0);
        poseStack.scale(0.025F, 0.025F, 0.025F);
        
        // Make it always face the player
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-1.0F, -1.0F, 1.0F);

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        
        if (villagersWithActiveQuests.contains(villagerUUID)) {
            // Draw hourglass symbol (⏳) for active quest - using ! as fallback
            font.drawInBatch("!", -2, -4, 0xFFFFAA00, false, 
                poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, packedLight);
        } else if (villagersWithQuestMarkers.contains(villagerUUID)) {
            // Draw exclamation mark (!) for available quest
            font.drawInBatch("!", -2, -4, 0xFFFFFF00, false, 
                poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, packedLight);
        }

        poseStack.popPose();
    }
}
