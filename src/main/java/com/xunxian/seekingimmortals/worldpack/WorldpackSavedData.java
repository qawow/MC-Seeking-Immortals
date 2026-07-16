package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WorldpackSavedData extends SavedData {
    private static final String DATA_NAME = SeekingImmortalsMod.MODID + "_worldpack";
    private final Map<String, Anchor> anchors = new HashMap<>();
    private final Map<String, EventRoll> dailyEvents = new HashMap<>();

    public static WorldpackSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                WorldpackSavedData::load,
                WorldpackSavedData::new,
                DATA_NAME);
    }

    public static WorldpackSavedData load(CompoundTag tag) {
        WorldpackSavedData data = new WorldpackSavedData();
        ListTag anchorList = tag.getList("Anchors", 10);
        for (int i = 0; i < anchorList.size(); i++) {
            CompoundTag anchorTag = anchorList.getCompound(i);
            Anchor anchor = Anchor.load(anchorTag);
            if (!anchor.id().isBlank() && !anchor.dimension().isBlank()) {
                data.anchors.put(anchor.id(), anchor);
            }
        }
        ListTag eventList = tag.getList("DailyEvents", 10);
        for (int i = 0; i < eventList.size(); i++) {
            CompoundTag eventTag = eventList.getCompound(i);
            EventRoll roll = EventRoll.load(eventTag);
            if (!roll.regionId().isBlank()) {
                data.dailyEvents.put(roll.regionId(), roll);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag anchorList = new ListTag();
        anchors.values().stream().sorted((a, b) -> a.id().compareTo(b.id())).forEach(anchor -> anchorList.add(anchor.save()));
        tag.put("Anchors", anchorList);

        ListTag eventList = new ListTag();
        dailyEvents.values().stream().sorted((a, b) -> a.regionId().compareTo(b.regionId())).forEach(roll -> eventList.add(roll.save()));
        tag.put("DailyEvents", eventList);
        return tag;
    }

    public Optional<Anchor> getAnchor(String id) {
        return Optional.ofNullable(anchors.get(id == null ? "" : id));
    }

    public boolean hasAnchor(String id) {
        return getAnchor(id).isPresent();
    }

    public void setAnchor(String id, String dimension, double x, double y, double z, float yRot, float xRot) {
        if (id == null || id.isBlank() || dimension == null || dimension.isBlank()) {
            return;
        }
        anchors.put(id.trim(), new Anchor(id.trim(), dimension, x, y, z, yRot, xRot));
        setDirty();
    }

    public void ensureStarterAnchor(ServerLevel level, WorldpackDataService.Snapshot snapshot) {
        snapshot.findRegion(WorldpackGameplayService.DEFAULT_REGION_ID).ifPresent(region -> {
            if (hasAnchor(region.travelAnchor())) {
                return;
            }
            var spawn = level.getSharedSpawnPos();
            setAnchor(region.travelAnchor(), level.dimension().location().toString(),
                    spawn.getX() + 0.5D, spawn.getY() + 1.0D, spawn.getZ() + 0.5D, 0.0F, 0.0F);
        });
    }

    public Optional<EventRoll> peekDailyEvent(String regionId) {
        return Optional.ofNullable(dailyEvents.get(regionId == null ? "" : regionId));
    }

    public EventRoll getOrRollDailyEvent(String regionId, WorldpackDataService.Snapshot snapshot, long gameTime, RandomSource random) {
        return getOrRollDailyEvent(regionId, snapshot, gameTime, random, snapshot.eventsForRegion(regionId));
    }

    /**
     * M06: allow expanded multi-region candidates from text_material daily_random_events.
     */
    public EventRoll getOrRollDailyEvent(String regionId, WorldpackDataService.Snapshot snapshot, long gameTime,
                                         RandomSource random, List<WorldpackDataService.DailyEvent> candidateOverride) {
        EventRoll current = dailyEvents.get(regionId);
        if (current != null && current.isActive(gameTime)
                && (snapshot.findDailyEvent(current.eventId()).isPresent()
                || containsEvent(candidateOverride, current.eventId()))) {
            return current;
        }
        List<WorldpackDataService.DailyEvent> candidates = candidateOverride == null || candidateOverride.isEmpty()
                ? snapshot.eventsForRegion(regionId)
                : candidateOverride;
        if (candidates.isEmpty()) {
            EventRoll empty = new EventRoll(regionId, "", gameTime + 24000L);
            dailyEvents.put(regionId, empty);
            setDirty();
            return empty;
        }
        WorldpackDataService.DailyEvent event = chooseWeighted(candidates, random);
        long duration = event.durationTicks() > 0 ? event.durationTicks() : 24000L;
        EventRoll rolled = new EventRoll(regionId, event.id(), gameTime + duration);
        dailyEvents.put(regionId, rolled);
        setDirty();
        return rolled;
    }

    private static boolean containsEvent(List<WorldpackDataService.DailyEvent> candidates, String eventId) {
        if (candidates == null || eventId == null || eventId.isBlank()) {
            return false;
        }
        for (WorldpackDataService.DailyEvent event : candidates) {
            if (eventId.equals(event.id())) {
                return true;
            }
        }
        return false;
    }

    static WorldpackDataService.DailyEvent chooseWeighted(List<WorldpackDataService.DailyEvent> events, RandomSource random) {
        int total = events.stream().mapToInt(event -> Math.max(1, event.weight())).sum();
        int pick = random.nextInt(Math.max(1, total));
        int cursor = 0;
        for (WorldpackDataService.DailyEvent event : events) {
            cursor += Math.max(1, event.weight());
            if (pick < cursor) {
                return event;
            }
        }
        return events.get(events.size() - 1);
    }

    public record Anchor(String id, String dimension, double x, double y, double z, float yRot, float xRot) {
        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Id", id);
            tag.putString("Dimension", dimension);
            tag.putDouble("X", x);
            tag.putDouble("Y", y);
            tag.putDouble("Z", z);
            tag.putFloat("YRot", yRot);
            tag.putFloat("XRot", xRot);
            return tag;
        }

        private static Anchor load(CompoundTag tag) {
            return new Anchor(
                    tag.getString("Id"),
                    tag.getString("Dimension"),
                    tag.getDouble("X"),
                    tag.contains("Y") ? tag.getDouble("Y") : 64.0D,
                    tag.getDouble("Z"),
                    tag.getFloat("YRot"),
                    tag.getFloat("XRot"));
        }
    }

    public record EventRoll(String regionId, String eventId, long untilTick) {
        public boolean isActive(long gameTime) {
            return !eventId.isBlank() && gameTime < untilTick;
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("RegionId", regionId);
            tag.putString("EventId", eventId);
            tag.putLong("UntilTick", untilTick);
            return tag;
        }

        private static EventRoll load(CompoundTag tag) {
            return new EventRoll(tag.getString("RegionId"), tag.getString("EventId"), Math.max(0L, tag.getLong("UntilTick")));
        }
    }
}
