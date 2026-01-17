package com.klemp.villagerquest.network;

import com.klemp.villagerquest.client.screen.DeliveryTurnInScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class OpenDeliveryScreenPacket {
    private final UUID villagerUUID;
    private final Map<Block, Integer> requiredItems;
    private final int reward;

    public OpenDeliveryScreenPacket(UUID villagerUUID, Map<Block, Integer> requiredItems, int reward) {
        this.villagerUUID = villagerUUID;
        this.requiredItems = requiredItems;
        this.reward = reward;
    }

    public OpenDeliveryScreenPacket(FriendlyByteBuf buf) {
        this.villagerUUID = buf.readUUID();
        
        int itemCount = buf.readInt();
        this.requiredItems = new HashMap<>();
        for (int i = 0; i < itemCount; i++) {
            Block block = buf.readRegistryIdUnsafe(ForgeRegistries.BLOCKS);
            int count = buf.readInt();
            requiredItems.put(block, count);
        }
        
        this.reward = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(villagerUUID);
        
        buf.writeInt(requiredItems.size());
        for (Map.Entry<Block, Integer> entry : requiredItems.entrySet()) {
            buf.writeRegistryIdUnsafe(ForgeRegistries.BLOCKS, entry.getKey());
            buf.writeInt(entry.getValue());
        }
        
        buf.writeInt(reward);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> 
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Minecraft mc = Minecraft.getInstance();
                mc.setScreen(new DeliveryTurnInScreen(
                    villagerUUID, 
                    requiredItems, 
                    reward, 
                    mc.player.getInventory()
                ));
            })
        );
        ctx.get().setPacketHandled(true);
        return true;
    }
}
