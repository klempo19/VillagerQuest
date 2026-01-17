package com.klemp.villagerquest.network;

import com.klemp.villagerquest.client.screen.QuestCompleteScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class QuestResultPacket {
    private final boolean successful;
    private final int reward;
    private final List<String> failureReasons;

    public QuestResultPacket(boolean successful, int reward, List<String> failureReasons) {
        this.successful = successful;
        this.reward = reward;
        this.failureReasons = failureReasons != null ? failureReasons : new ArrayList<>();
    }

    public QuestResultPacket(FriendlyByteBuf buf) {
        this.successful = buf.readBoolean();
        this.reward = buf.readInt();
        
        int reasonCount = buf.readInt();
        this.failureReasons = new ArrayList<>();
        for (int i = 0; i < reasonCount; i++) {
            failureReasons.add(buf.readUtf());
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(successful);
        buf.writeInt(reward);
        
        buf.writeInt(failureReasons.size());
        for (String reason : failureReasons) {
            buf.writeUtf(reason);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> 
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Minecraft.getInstance().setScreen(new QuestCompleteScreen(
                    successful, reward, failureReasons
                ));
            })
        );
        ctx.get().setPacketHandled(true);
        return true;
    }
}