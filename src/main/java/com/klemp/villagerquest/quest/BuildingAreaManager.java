package com.klemp.villagerquest.quest;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.*;

public class BuildingAreaManager extends SavedData {
    private static final String DATA_NAME = "villagerquest_buildings";
    
    // Stores all registered building areas
    private List<BuildingArea> buildingAreas = new ArrayList<>();

    public static class BuildingArea {
        private final BlockPos min;
        private final BlockPos max;
        private final UUID villagerUUID;
        private final long timestamp;

        public BuildingArea(BlockPos min, BlockPos max, UUID villagerUUID) {
            this.min = min;
            this.max = max;
            this.villagerUUID = villagerUUID;
            this.timestamp = System.currentTimeMillis();
        }

        public BuildingArea(BlockPos min, BlockPos max, UUID villagerUUID, long timestamp) {
            this.min = min;
            this.max = max;
            this.villagerUUID = villagerUUID;
            this.timestamp = timestamp;
        }

        public BlockPos getMin() { return min; }
        public BlockPos getMax() { return max; }
        public UUID getVillagerUUID() { return villagerUUID; }
        public long getTimestamp() { return timestamp; }

        public boolean overlaps(BlockPos otherMin, BlockPos otherMax) {
            // Add 3 block buffer zone around each building
            int buffer = 3;
            BlockPos expandedMin = min.offset(-buffer, -buffer, -buffer);
            BlockPos expandedMax = max.offset(buffer, buffer, buffer);

            return !(otherMax.getX() < expandedMin.getX() || otherMin.getX() > expandedMax.getX() ||
                     otherMax.getY() < expandedMin.getY() || otherMin.getY() > expandedMax.getY() ||
                     otherMax.getZ() < expandedMin.getZ() || otherMin.getZ() > expandedMax.getZ());
        }
    }

    public BuildingAreaManager() {
        super();
    }

    public BuildingAreaManager(CompoundTag tag) {
        this.load(tag);
    }

    public static BuildingAreaManager get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(BuildingAreaManager::new, BuildingAreaManager::new, DATA_NAME);
    }

    public boolean registerBuildingArea(BlockPos corner1, BlockPos corner2) {
        BlockPos min = new BlockPos(
            Math.min(corner1.getX(), corner2.getX()),
            Math.min(corner1.getY(), corner2.getY()),
            Math.min(corner1.getZ(), corner2.getZ())
        );
        BlockPos max = new BlockPos(
            Math.max(corner1.getX(), corner2.getX()),
            Math.max(corner1.getY(), corner2.getY()),
            Math.max(corner1.getZ(), corner2.getZ())
        );

        // Check for overlaps
        if (wouldOverlap(min, max)) {
            return false;
        }

        buildingAreas.add(new BuildingArea(min, max, UUID.randomUUID()));
        setDirty();
        return true;
    }

    public boolean wouldOverlap(BlockPos min, BlockPos max) {
        for (BuildingArea area : buildingAreas) {
            if (area.overlaps(min, max)) {
                return true;
            }
        }
        return false;
    }

    public List<BuildingArea> getBuildingAreas() {
        return new ArrayList<>(buildingAreas);
    }

    public Optional<BuildingArea> findAreaForUpgrade(BlockPos near, int maxDistance) {
        // Find nearest building area for potential upgrade quests
        BuildingArea nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (BuildingArea area : buildingAreas) {
            double dist = Math.sqrt(area.getMin().distSqr(near));
            if (dist < maxDistance && dist < nearestDist) {
                nearest = area;
                nearestDist = dist;
            }
        }

        return Optional.ofNullable(nearest);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag areaList = new ListTag();
        for (BuildingArea area : buildingAreas) {
            CompoundTag areaTag = new CompoundTag();
            areaTag.put("Min", NbtUtils.writeBlockPos(area.getMin()));
            areaTag.put("Max", NbtUtils.writeBlockPos(area.getMax()));
            areaTag.putUUID("Villager", area.getVillagerUUID());
            areaTag.putLong("Timestamp", area.getTimestamp());
            areaList.add(areaTag);
        }
        tag.put("Areas", areaList);
        return tag;
    }

    public void load(CompoundTag tag) {
        buildingAreas.clear();
        
        ListTag areaList = tag.getList("Areas", Tag.TAG_COMPOUND);
        for (int i = 0; i < areaList.size(); i++) {
            CompoundTag areaTag = areaList.getCompound(i);
            BlockPos min = NbtUtils.readBlockPos(areaTag.getCompound("Min"));
            BlockPos max = NbtUtils.readBlockPos(areaTag.getCompound("Max"));
            UUID villagerUUID = areaTag.getUUID("Villager");
            long timestamp = areaTag.getLong("Timestamp");
            buildingAreas.add(new BuildingArea(min, max, villagerUUID, timestamp));
        }
    }
}
