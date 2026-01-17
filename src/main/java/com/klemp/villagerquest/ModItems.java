package com.klemp.villagerquest;

import com.klemp.villagerquest.item.QuestBookItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = 
        DeferredRegister.create(ForgeRegistries.ITEMS, VillagerQuestMod.MOD_ID);

    // Quest Book item
    public static final RegistryObject<Item> QUEST_BOOK = ITEMS.register("quest_book",
        QuestBookItem::new);
    
    // Block items
    public static final RegistryObject<Item> QUEST_MARKER = ITEMS.register("quest_marker",
        () -> new BlockItem(ModBlocks.QUEST_MARKER.get(), new Item.Properties()));
}
