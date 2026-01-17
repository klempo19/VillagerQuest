package com.klemp.villagerquest.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;

public class WanderingQuestVillager extends Villager {
    private static final int DESPAWN_TIME = 24000; // 20 minutes
    private int ticksExisted = 0;
    private boolean hasOfferedQuest = false;

    public WanderingQuestVillager(EntityType<? extends Villager> type, Level level) {
        super(type, level);
        this.setVillagerData(this.getVillagerData()
            .setProfession(VillagerProfession.NONE)
            .setType(VillagerType.PLAINS));
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 0.5D));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.35D));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        
        if (!this.level().isClientSide) {
            ticksExisted++;
            
            if (ticksExisted >= DESPAWN_TIME) {
                this.discard();
            }
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, 
                                       MobSpawnType reason, @Nullable SpawnGroupData spawnData, 
                                       @Nullable CompoundTag dataTag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
        
        this.setCustomName(Component.literal("§6Wandering Builder"));
        this.setCustomNameVisible(true);
        
        return data;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("TicksExisted", this.ticksExisted);
        tag.putBoolean("HasOfferedQuest", this.hasOfferedQuest);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.ticksExisted = tag.getInt("TicksExisted");
        this.hasOfferedQuest = tag.getBoolean("HasOfferedQuest");
    }

    public boolean hasOfferedQuest() {
        return hasOfferedQuest;
    }

    public void setOfferedQuest(boolean offered) {
        this.hasOfferedQuest = offered;
    }

    @Override
    public boolean canRestock() {
        return false;
    }
}