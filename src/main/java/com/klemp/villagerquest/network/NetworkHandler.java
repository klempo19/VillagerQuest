package com.klemp.villagerquest.network;

import com.klemp.villagerquest.VillagerQuestMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(VillagerQuestMod.MOD_ID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register() {
        // Client to Server packets
        INSTANCE.messageBuilder(QuestResponsePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(QuestResponsePacket::new)
            .encoder(QuestResponsePacket::toBytes)
            .consumerMainThread(QuestResponsePacket::handle)
            .add();

        INSTANCE.messageBuilder(CheckQuestCompletionPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(CheckQuestCompletionPacket::new)
            .encoder(CheckQuestCompletionPacket::toBytes)
            .consumerMainThread(CheckQuestCompletionPacket::handle)
            .add();

        INSTANCE.messageBuilder(ShareQuestPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(ShareQuestPacket::new)
            .encoder(ShareQuestPacket::toBytes)
            .consumerMainThread(ShareQuestPacket::handle)
            .add();

        INSTANCE.messageBuilder(CancelQuestPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(CancelQuestPacket::new)
            .encoder(CancelQuestPacket::toBytes)
            .consumerMainThread(CancelQuestPacket::handle)
            .add();

        INSTANCE.messageBuilder(SubmitDeliveryPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(SubmitDeliveryPacket::new)
            .encoder(SubmitDeliveryPacket::toBytes)
            .consumerMainThread(SubmitDeliveryPacket::handle)
            .add();

        // Server to Client packets
        INSTANCE.messageBuilder(OpenQuestOfferPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(OpenQuestOfferPacket::new)
            .encoder(OpenQuestOfferPacket::toBytes)
            .consumerMainThread(OpenQuestOfferPacket::handle)
            .add();

        INSTANCE.messageBuilder(QuestResultPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(QuestResultPacket::new)
            .encoder(QuestResultPacket::toBytes)
            .consumerMainThread(QuestResultPacket::handle)
            .add();

        INSTANCE.messageBuilder(SyncQuestMarkersPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(SyncQuestMarkersPacket::new)
            .encoder(SyncQuestMarkersPacket::toBytes)
            .consumerMainThread(SyncQuestMarkersPacket::handle)
            .add();

        INSTANCE.messageBuilder(OpenDeliveryScreenPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(OpenDeliveryScreenPacket::new)
            .encoder(OpenDeliveryScreenPacket::toBytes)
            .consumerMainThread(OpenDeliveryScreenPacket::handle)
            .add();
    }
}
