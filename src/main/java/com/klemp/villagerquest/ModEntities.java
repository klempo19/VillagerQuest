package com.klemp.villagerquest;

import com.klemp.villagerquest.entity.WanderingQuestVillager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = 
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, VillagerQuestMod.MOD_ID);

    public static final RegistryObject<EntityType<WanderingQuestVillager>> WANDERING_QUEST_VILLAGER = 
        ENTITIES.register("wandering_quest_villager", () -> 
            EntityType.Builder.of((EntityType.EntityFactory<WanderingQuestVillager>) WanderingQuestVillager::new, MobCategory.CREATURE)
                .sized(0.6F, 1.95F)
                .clientTrackingRange(10)
                .build("wandering_quest_villager"));
}