package com.klemp.villagerquest.network;

import com.klemp.villagerquest.client.VillagerRenderHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class SyncQuestMarkersPacket {
    private final Set<UUID> availableQuests;
    private final Set<UUID> activeQuests;

    public SyncQuestMarkersPacket(Set<UUID> availableQuests, Set<UUID> activeQuests) {
        this.availableQuests = availableQuests != null ? availableQuests : new HashSet<>();
        this.activeQuests = activeQuests != null ? activeQuests : new HashSet<>();
    }

    public SyncQuestMarkersPacket(FriendlyByteBuf buf) {
        int availableCount = buf.readInt();
        this.availableQuests = new HashSet<>();
        for (int i = 0; i < availableCount; i++) {
            availableQuests.add(buf.readUUID());
        }

        int activeCount = buf.readInt();
        this.activeQuests = new HashSet<>();
        for (int i = 0; i < activeCount; i++) {
            activeQuests.add(buf.readUUID());
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(availableQuests.size());
        for (UUID uuid : availableQuests) {
            buf.writeUUID(uuid);
        }

        buf.writeInt(activeQuests.size());
        for (UUID uuid : activeQuests) {
            buf.writeUUID(uuid);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> 
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                VillagerRenderHandler.setQuestMarkers(availableQuests);
                VillagerRenderHandler.setActiveQuests(activeQuests);
            })
        );
        ctx.get().setPacketHandled(true);
        return true;
    }
}
