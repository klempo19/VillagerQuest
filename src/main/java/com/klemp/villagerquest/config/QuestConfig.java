package com.klemp.villagerquest.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class QuestConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // Quest Generation Settings
    public static final ForgeConfigSpec.IntValue MIN_BUILD_WIDTH;
    public static final ForgeConfigSpec.IntValue MAX_BUILD_WIDTH;
    public static final ForgeConfigSpec.IntValue MIN_BUILD_HEIGHT;
    public static final ForgeConfigSpec.IntValue MAX_BUILD_HEIGHT;
    public static final ForgeConfigSpec.IntValue MIN_REQUIRED_BLOCKS;
    public static final ForgeConfigSpec.IntValue MAX_REQUIRED_BLOCKS;
    
    // Quest Rewards
    public static final ForgeConfigSpec.IntValue MIN_EMERALD_REWARD;
    public static final ForgeConfigSpec.IntValue MAX_EMERALD_REWARD;
    
    // Quest Cooldown
    public static final ForgeConfigSpec.IntValue QUEST_COOLDOWN_MINUTES;
    
    // Quest Types
    public static final ForgeConfigSpec.DoubleValue DELIVERY_QUEST_CHANCE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BUILDING_QUESTS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DELIVERY_QUESTS;
    
    // Building Requirements
    public static final ForgeConfigSpec.IntValue BUILDING_BUFFER_ZONE;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_FULL_ENCLOSURE;
    
    // Wandering Villager
    public static final ForgeConfigSpec.IntValue WANDERING_VILLAGER_SPAWN_CHANCE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_WANDERING_VILLAGERS;

    static {
        BUILDER.push("Quest Generation");
        
        MIN_BUILD_WIDTH = BUILDER
            .comment("Minimum width/length for building quests (in blocks)")
            .defineInRange("minBuildWidth", 4, 3, 20);
        
        MAX_BUILD_WIDTH = BUILDER
            .comment("Maximum width/length for building quests (in blocks)")
            .defineInRange("maxBuildWidth", 15, 5, 50);
        
        MIN_BUILD_HEIGHT = BUILDER
            .comment("Minimum height for building quests (in blocks)")
            .defineInRange("minBuildHeight", 5, 3, 20);
        
        MAX_BUILD_HEIGHT = BUILDER
            .comment("Maximum height for building quests (in blocks)")
            .defineInRange("maxBuildHeight", 10, 5, 30);
        
        MIN_REQUIRED_BLOCKS = BUILDER
            .comment("Minimum number of blocks required for quests")
            .defineInRange("minRequiredBlocks", 20, 10, 100);
        
        MAX_REQUIRED_BLOCKS = BUILDER
            .comment("Maximum number of blocks required for quests")
            .defineInRange("maxRequiredBlocks", 50, 20, 200);
        
        BUILDER.pop();
        
        BUILDER.push("Quest Rewards");
        
        MIN_EMERALD_REWARD = BUILDER
            .comment("Minimum emerald reward for completing quests")
            .defineInRange("minEmeraldReward", 5, 1, 64);
        
        MAX_EMERALD_REWARD = BUILDER
            .comment("Maximum emerald reward for completing quests")
            .defineInRange("maxEmeraldReward", 64, 10, 128);
        
        BUILDER.pop();
        
        BUILDER.push("Quest Cooldown");
        
        QUEST_COOLDOWN_MINUTES = BUILDER
            .comment("Time in minutes before a villager can offer another quest")
            .defineInRange("questCooldownMinutes", 60, 5, 300);
        
        BUILDER.pop();
        
        BUILDER.push("Quest Types");
        
        DELIVERY_QUEST_CHANCE = BUILDER
            .comment("Chance (0.0 to 1.0) for a quest to be a delivery quest instead of building")
            .defineInRange("deliveryQuestChance", 0.3, 0.0, 1.0);
        
        ENABLE_BUILDING_QUESTS = BUILDER
            .comment("Enable building quests")
            .define("enableBuildingQuests", true);
        
        ENABLE_DELIVERY_QUESTS = BUILDER
            .comment("Enable delivery quests")
            .define("enableDeliveryQuests", true);
        
        BUILDER.pop();
        
        BUILDER.push("Building Requirements");
        
        BUILDING_BUFFER_ZONE = BUILDER
            .comment("Buffer zone (in blocks) between quest buildings to prevent overlap")
            .defineInRange("buildingBufferZone", 3, 0, 10);
        
        REQUIRE_FULL_ENCLOSURE = BUILDER
            .comment("Require buildings to be fully enclosed (floor, walls, ceiling)")
            .define("requireFullEnclosure", true);
        
        BUILDER.pop();
        
        BUILDER.push("Wandering Villagers");
        
        ENABLE_WANDERING_VILLAGERS = BUILDER
            .comment("Enable wandering quest villagers to spawn")
            .define("enableWanderingVillagers", true);
        
        WANDERING_VILLAGER_SPAWN_CHANCE = BUILDER
            .comment("Chance (1 in X checks) for a wandering villager to spawn per player")
            .defineInRange("wanderingVillagerSpawnChance", 100, 10, 1000);
        
        BUILDER.pop();
        
        SPEC = BUILDER.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "villagerquest-common.toml");
    }
}
