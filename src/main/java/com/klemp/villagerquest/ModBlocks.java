package com.klemp.villagerquest;

import com.klemp.villagerquest.block.QuestMarkerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = 
        DeferredRegister.create(ForgeRegistries.BLOCKS, VillagerQuestMod.MOD_ID);

    public static final RegistryObject<Block> QUEST_MARKER = BLOCKS.register("quest_marker",
        QuestMarkerBlock::new);
}