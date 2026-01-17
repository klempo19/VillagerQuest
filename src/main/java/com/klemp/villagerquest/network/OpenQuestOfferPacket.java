package com.klemp.villagerquest.network;

import com.klemp.villagerquest.client.screen.QuestOfferScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class OpenQuestOfferPacket {
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

    public OpenQuestOfferPacket(UUID villagerUUID, String questType, 
                               Map<Block, Integer> requiredBlocks, int reward,
                               int width, int length, int height,
                               boolean questTaken, boolean playerHasQuest,
                               boolean canShare, List<String> partyMembers) {
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

    public OpenQuestOfferPacket(FriendlyByteBuf buf) {
        this.villagerUUID = buf.readUUID();
        this.questType = buf.readUtf();
        
        int blockCount = buf.readInt();
        this.requiredBlocks = new HashMap<>();
        for (int i = 0; i < blockCount; i++) {
            Block block = buf.readRegistryIdUnsafe(ForgeRegistries.BLOCKS);
            int count = buf.readInt();
            requiredBlocks.put(block, count);
        }
        
        this.reward = buf.readInt();
        this.width = buf.readInt();
        this.length = buf.readInt();
        this.height = buf.readInt();
        this.questTaken = buf.readBoolean();
        this.playerHasQuest = buf.readBoolean();
        this.canShare = buf.readBoolean();
        
        int partySize = buf.readInt();
        this.partyMembers = new ArrayList<>();
        for (int i = 0; i < partySize; i++) {
            partyMembers.add(buf.readUtf());
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(villagerUUID);
        buf.writeUtf(questType);
        
        buf.writeInt(requiredBlocks.size());
        for (Map.Entry<Block, Integer> entry : requiredBlocks.entrySet()) {
            buf.writeRegistryIdUnsafe(ForgeRegistries.BLOCKS, entry.getKey());
            buf.writeInt(entry.getValue());
        }
        
        buf.writeInt(reward);
        buf.writeInt(width);
        buf.writeInt(length);
        buf.writeInt(height);
        buf.writeBoolean(questTaken);
        buf.writeBoolean(playerHasQuest);
        buf.writeBoolean(canShare);
        
        buf.writeInt(partyMembers.size());
        for (String member : partyMembers) {
            buf.writeUtf(member);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> 
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Minecraft.getInstance().setScreen(new QuestOfferScreen(
                    villagerUUID, questType, requiredBlocks, reward, 
                    width, length, height, questTaken, playerHasQuest, 
                    canShare, partyMembers
                ));
            })
        );
        ctx.get().setPacketHandled(true);
        return true;
    }
}