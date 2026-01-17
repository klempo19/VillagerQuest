package com.klemp.villagerquest;

import com.klemp.villagerquest.config.QuestConfig;
import com.klemp.villagerquest.handler.QuestMarkerSyncHandler;
import com.klemp.villagerquest.handler.QuestValidationHandler;
import com.klemp.villagerquest.handler.VillagerInteractionHandler;
import com.klemp.villagerquest.handler.WanderingVillagerSpawner;
import com.klemp.villagerquest.network.NetworkHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(VillagerQuestMod.MOD_ID)
public class VillagerQuestMod {
    public static final String MOD_ID = "villagerquest";
    public static final Logger LOGGER = LogManager.getLogger();

    public VillagerQuestMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // Register config
        QuestConfig.register();
        
        // Register mod content
        ModItems.ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        
        // Register common setup
        modEventBus.addListener(this::commonSetup);
        
        // Register event handlers
        MinecraftForge.EVENT_BUS.register(new VillagerInteractionHandler());
        MinecraftForge.EVENT_BUS.register(new QuestValidationHandler());
        MinecraftForge.EVENT_BUS.register(new WanderingVillagerSpawner());
        MinecraftForge.EVENT_BUS.register(new QuestMarkerSyncHandler());
        
        LOGGER.info("Villager Quest Mod initialized!");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::register);
    }
}
