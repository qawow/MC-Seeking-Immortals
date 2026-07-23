package com.xunxian.seekingimmortals.visual;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.TribulationRulesCatalog;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.entity.CultivationBeastEntity;
import com.xunxian.seekingimmortals.entity.CultivatorNpcEntity;
import com.xunxian.seekingimmortals.entity.SpiritBoatEntity;
import com.xunxian.seekingimmortals.network.VisualEventPacket;
import com.xunxian.seekingimmortals.worldpack.BossEncounterService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime consumers for the authored visual namespaces.
 *
 * <p>This subscriber is deliberately cosmetic: it only describes an event to
 * nearby clients after the authoritative Forge event has already happened.
 * Rate limits keep broad tick hooks from becoming an alternate particle
 * system, while the packet lifecycle keeps long-lived anchors deterministic.</p>
 */
@Mod.EventBusSubscriber(modid = SeekingImmortalsMod.MODID)
public final class VisualDomainRuntimeEvents {
    private static final int ENTITY_REFRESH_TICKS = 20;
    private static final int PLAYER_CONTEXT_REFRESH_TICKS = 40;
    private static final int MAX_TRACKED_KEYS = 4096;
    private static final int MAX_BLOCK_EVENTS_PER_TICK = 32;
    private static final Map<String, Long> LAST_EMIT = new ConcurrentHashMap<>();
    private static final Map<String, BlockEventBudget> BLOCK_EVENT_BUDGETS = new ConcurrentHashMap<>();

    private VisualDomainRuntimeEvents() {}

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getLevel().isClientSide()) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity instanceof LivingEntity living && living.isDeadOrDying()) {
            return;
        }
        if (entity instanceof Mob mob && BossEncounterService.isBossMob(mob)
                && !BossEncounterService.bossIdOf(mob).isBlank()) {
            startEntity(level, mob, "boss", BossEncounterService.bossIdOf(mob), "SPAWN", 80, 3);
        } else if (entity instanceof CultivationBeastEntity beast) {
            startEntity(level, beast, "beast", beast.getBeastId(), "SPAWN", 60, 2);
        } else if (entity instanceof CultivatorNpcEntity npc) {
            String id = npc.getNamedNpcId();
            if (!id.isBlank()) {
                startEntity(level, npc, "npc", id, "SPAWN", 80, 1);
            }
        } else if (entity instanceof SpiritBoatEntity boat) {
            startEntity(level, boat, "vehicle", boat.vehicleId(), "DOCKED", 80, 1);
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getLevel().isClientSide()) {
            return;
        }
        Entity entity = event.getEntity();
        // Death already emitted its terminal STOP; leaving the level must not
        // replay the same dissipate event a second time.
        if (entity instanceof LivingEntity living && living.isDeadOrDying()) {
            return;
        }
        if (entity instanceof Mob mob && BossEncounterService.isBossMob(mob)
                && !BossEncounterService.bossIdOf(mob).isBlank()) {
            stopEntity(level, mob, "boss", BossEncounterService.bossIdOf(mob), "DESPAWN", 3);
        } else if (entity instanceof CultivationBeastEntity beast) {
            stopEntity(level, beast, "beast", beast.getBeastId(), "DESPAWN", 2);
        } else if (entity instanceof CultivatorNpcEntity npc && !npc.getNamedNpcId().isBlank()) {
            stopEntity(level, npc, "npc", npc.getNamedNpcId(), "DESPAWN", 1);
        } else if (entity instanceof SpiritBoatEntity boat) {
            stopEntity(level, boat, "vehicle", boat.vehicleId(), "DOCK", 1);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || entity.level().isClientSide()) {
            return;
        }
        if (entity.isDeadOrDying()) {
            return;
        }
        long gameTime = level.getGameTime();

        if (entity instanceof Mob mob && BossEncounterService.isBossMob(mob)
                && !BossEncounterService.bossIdOf(mob).isBlank()
                && due(level, mob, "boss", BossEncounterService.bossIdOf(mob), ENTITY_REFRESH_TICKS)) {
            String bossId = BossEncounterService.bossIdOf(mob);
            VisualEventDispatcher.entity(level, "boss", bossId,
                    VisualEventPacket.Lifecycle.UPDATE, bossTrigger(mob), mob,
                    VisualEventDispatcher.entityKey("boss", mob, bossId), 45, 0,
                    1.25D, 28, gameTime ^ mob.getUUID().getLeastSignificantBits(), 3);
        } else if (entity instanceof CultivationBeastEntity beast
                && due(level, beast, "beast", beast.getBeastId(), ENTITY_REFRESH_TICKS)) {
            VisualEventDispatcher.entity(level, "beast", beast.getBeastId(),
                    VisualEventPacket.Lifecycle.UPDATE, beast.isAggressive() ? "AGGRO" : "IDLE",
                    beast, VisualEventDispatcher.entityKey("beast", beast, beast.getBeastId()),
                    45, 0, 1.0D, beast.isAggressive() ? 22 : 8,
                    gameTime ^ beast.getUUID().getLeastSignificantBits(), beast.isAggressive() ? 2 : 0);
        } else if (entity instanceof CultivatorNpcEntity npc && !npc.getNamedNpcId().isBlank()
                && due(level, npc, "npc", npc.getNamedNpcId(), ENTITY_REFRESH_TICKS * 2)) {
            VisualEventDispatcher.entity(level, "npc", npc.getNamedNpcId(),
                    VisualEventPacket.Lifecycle.UPDATE, "IDLE", npc,
                    VisualEventDispatcher.entityKey("npc", npc, npc.getNamedNpcId()),
                    60, 0, 0.82D, 7, gameTime ^ npc.getUUID().getLeastSignificantBits(), 0);
        }

        if (entity instanceof ServerPlayer player && player.tickCount % PLAYER_CONTEXT_REFRESH_TICKS == 0) {
            emitPlayerContext(level, player);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (entity instanceof Mob mob && BossEncounterService.isBossMob(mob)
                && !BossEncounterService.bossIdOf(mob).isBlank()) {
            String bossId = BossEncounterService.bossIdOf(mob);
            stopEntity(level, mob, "boss", bossId, "DEATH", 3);
            VisualEventDispatcher.entity(level, "boss", bossId,
                    VisualEventPacket.Lifecycle.EVENT, "DEATH", mob, "", 0, 0,
                    1.5D, 36, level.getGameTime() ^ mob.getId(), 3);
        } else if (entity instanceof CultivationBeastEntity beast) {
            stopEntity(level, beast, "beast", beast.getBeastId(), "DEATH", 2);
            VisualEventDispatcher.entity(level, "beast", beast.getBeastId(),
                    VisualEventPacket.Lifecycle.EVENT, "DEATH", beast, "", 0, 0,
                    1.25D, 28, level.getGameTime() ^ beast.getId(), 2);
        } else if (entity instanceof CultivatorNpcEntity npc && !npc.getNamedNpcId().isBlank()) {
            stopEntity(level, npc, "npc", npc.getNamedNpcId(), "DEATH", 1);
            VisualEventDispatcher.entity(level, "npc", npc.getNamedNpcId(),
                    VisualEventPacket.Lifecycle.EVENT, "DEATH", npc, "", 0, 0,
                    1.0D, 20, level.getGameTime() ^ npc.getId(), 1);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null) {
            return;
        }
        String id = key.getPath();
        String domain = itemDomain(id);
        CompoundTag tag = stack.getTag();
        String methodId = firstNonBlank(tag == null ? "" : tag.getString("MethodId"),
                tag == null ? "" : tag.getString("method_id"));
        if (!methodId.isBlank() && hasProfile("method", methodId)) {
            VisualEventDispatcher.entity(level, "method", methodId,
                    VisualEventPacket.Lifecycle.EVENT, "TRAIN_PULSE", player, "", 0, 0,
                    0.95D, 18, level.getGameTime() ^ methodId.hashCode(), 1);
        }
        if (domain.isBlank() || "pill".equals(domain) || "consumable".equals(domain)
                || "artifact".equals(domain) || !due(level, player, domain, id, 4)) {
            return;
        }
        VisualEventDispatcher.entity(level, domain, id, VisualEventPacket.Lifecycle.EVENT,
                "USE", player, "", 0, 0, 0.9D, 18,
                level.getGameTime() ^ stack.getItem().hashCode(), 1);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)
                || !(event.getTarget() instanceof CultivatorNpcEntity npc)
                || npc.getNamedNpcId().isBlank()) {
            return;
        }
        VisualEventDispatcher.entity(level, "npc", npc.getNamedNpcId(),
                VisualEventPacket.Lifecycle.EVENT, "TALK", npc, "", 0, 0,
                0.95D, 16, level.getGameTime() ^ player.getId(), 1);
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        dispatchBlock(level, event.getPlacedBlock(), event.getPos(),
                VisualEventPacket.Lifecycle.START, "DEPLOY", 40, 1);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        dispatchBlock(level, event.getState(), event.getPos(),
                VisualEventPacket.Lifecycle.EVENT, "DISMANTLED", 0, 2);
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        emitPlayerContext(level, player);
    }

    private static void startEntity(ServerLevel level, Entity entity, String domain, String id,
                                    String trigger, int duration, int priority) {
        if (id == null || id.isBlank() || !hasProfile(domain, id)) {
            return;
        }
        VisualEventDispatcher.entity(level, domain, id, VisualEventPacket.Lifecycle.START,
                trigger, entity, VisualEventDispatcher.entityKey(domain, entity, id),
                duration, 0, 1.0D, 18, level.getGameTime() ^ entity.getUUID().getLeastSignificantBits(), priority);
    }

    private static void stopEntity(ServerLevel level, Entity entity, String domain, String id,
                                   String trigger, int priority) {
        if (id == null || id.isBlank() || !hasProfile(domain, id)) {
            return;
        }
        VisualEventDispatcher.entity(level, domain, id, VisualEventPacket.Lifecycle.STOP,
                trigger, entity, VisualEventDispatcher.entityKey(domain, entity, id),
                1, 0, 1.0D, 12, level.getGameTime() ^ entity.getUUID().getMostSignificantBits(), priority);
    }

    private static void emitPlayerContext(ServerLevel level, ServerPlayer player) {
        String dimension = player.level().dimension().location().getPath();
        String realm = secretRealmId(dimension);
        String zone = player.getPersistentData().getString("seeking_immortals_secret_realm_layer");
        if (zone.isBlank()) {
            zone = player.getPersistentData().getString("SecretRealmLayer");
        }
        if (!realm.isBlank()) {
            VisualEventDispatcher.entity(level, "realm", realm,
                    VisualEventPacket.Lifecycle.UPDATE, "AMBIENT", player,
                    VisualEventDispatcher.entityKey("realm", player, realm), 60, 0,
                    1.0D, 8, level.getGameTime() ^ realm.hashCode(), 0);
        }
        if (!zone.isBlank()) {
            VisualEventDispatcher.entity(level, "zone", zone,
                    VisualEventPacket.Lifecycle.UPDATE, "AMBIENT", player,
                    VisualEventDispatcher.entityKey("zone", player, zone), 60, 0,
                    0.9D, 10, level.getGameTime() ^ zone.hashCode(), 0);
        }
        CultivationHelper.get(player).ifPresent(cultivation -> {
            if (cultivation.isTribulationActive()) {
                TribulationRulesCatalog.builtin().forRealm(cultivation.getTribulationTargetRealm())
                        .ifPresent(rule -> VisualEventDispatcher.entity(level, "tribulation", rule.id(),
                                VisualEventPacket.Lifecycle.UPDATE, "WAVE", player,
                                VisualEventDispatcher.entityKey("tribulation", player, rule.id()), 60, 0,
                                1.1D, 24, level.getGameTime()
                                        ^ cultivation.getTribulationCurrentStrike(), 2));
            }
        });
    }

    private static void dispatchBlock(ServerLevel level, BlockState state, net.minecraft.core.BlockPos pos,
                                      VisualEventPacket.Lifecycle lifecycle, String trigger,
                                      int duration, int priority) {
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (key == null) {
            return;
        }
        String id = key.getPath();
        String domain = id.contains("formation") || id.contains("array") ? "formation" : "structure";
        if (!hasProfile(domain, id) || !claimBlockEvent(level)) {
            return;
        }
        VisualEventDispatcher.block(level, domain, id, lifecycle, trigger, pos,
                VisualEventDispatcher.blockKey(domain, level, pos, id), duration, 0,
                1.0D, lifecycle == VisualEventPacket.Lifecycle.EVENT ? 24 : 14,
                level.getGameTime() ^ pos.asLong(), priority);
    }

    private static String itemDomain(String id) {
        String lower = id == null ? "" : id.toLowerCase(java.util.Locale.ROOT);
        for (String candidate : new String[] {"pill", "consumable", "artifact", "herb", "material",
                "talisman", "currency", "manual"}) {
            if (hasProfile(candidate, lower)) {
                return candidate;
            }
        }
        if (lower.contains("pill") || lower.endsWith("_dan") || lower.contains("powder")) {
            return "pill";
        }
        if (lower.contains("talisman") || lower.contains("符")) {
            return "talisman";
        }
        if (lower.contains("manual") || lower.contains("scripture") || lower.contains("jade_slip")) {
            return "manual";
        }
        if (lower.contains("stone") || lower.contains("jade") || lower.contains("coin")
                || lower.contains("currency")) {
            return "currency";
        }
        if (lower.contains("herb") || lower.contains("grass") || lower.contains("flower")) {
            return "herb";
        }
        if (lower.contains("artifact") || lower.contains("sword") || lower.contains("mirror")) {
            return "artifact";
        }
        return "consumable";
    }

    static boolean hasProfile(String domain, String id) {
        try {
            return AuthoredVisualCatalog.resolve(domain + ":" + id).isPresent();
        } catch (RuntimeException ignored) {
            // A domain may be absent in a legacy resource pack; heuristics below
            // still provide a bounded generic event for that item.
            return false;
        }
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : (second == null ? "" : second);
    }

    private static String secretRealmId(String dimension) {
        String value = dimension == null ? "" : dimension.toLowerCase(java.util.Locale.ROOT);
        if (value.contains("blood") || value.contains("forbidden")) return "blood_forbidden";
        if (value.contains("xutian") || value.contains("void_palace")) return "void_palace";
        if (value.contains("zhuimo") || value.contains("fallen_demon")) return "fallen_demon_valley";
        if (value.contains("kunwu")) return "kunwu_mountain";
        if (value.contains("yinyang") || value.contains("yin_mountain")) return "yin_mountain_catacomb";
        if (value.contains("nether") || value.contains("minghe")) return "nether_river_land";
        if (value.contains("chaotic") || value.contains("star_sea")) return "chaotic_sea_abyss";
        return "";
    }

    private static String bossTrigger(Mob mob) {
        if (mob == null || mob.getMaxHealth() <= 0.0F) {
            return "P1";
        }
        return bossTrigger(mob.getHealth() / mob.getMaxHealth(), mob.getTarget() != null, mob.tickCount);
    }

    static String bossTrigger(float healthRatio, boolean hasTarget, int tickCount) {
        if (hasTarget && Math.floorMod(tickCount / ENTITY_REFRESH_TICKS, 2) == 1) {
            return "ATTACK";
        }
        if (healthRatio <= 0.33F) {
            return "P3";
        }
        if (healthRatio <= 0.66F) {
            return "P2";
        }
        return "P1";
    }

    private static boolean due(ServerLevel level, Entity entity, String domain, String id, int interval) {
        if (id == null || id.isBlank()) return false;
        long now = level.getGameTime();
        String key = domain + ':' + id + ':' + entity.getUUID();
        Long previous = LAST_EMIT.get(key);
        boolean due = emissionDue(previous, now, interval);
        if (due) {
            LAST_EMIT.put(key, now);
        }
        if (LAST_EMIT.size() > MAX_TRACKED_KEYS) {
            LAST_EMIT.entrySet().removeIf(entry -> now - entry.getValue() > 1200L);
        }
        return due;
    }

    static boolean emissionDue(Long previous, long now, int interval) {
        return previous == null || now < previous || now - previous >= Math.max(1, interval);
    }

    private static boolean claimBlockEvent(ServerLevel level) {
        String dimension = level.dimension().location().toString();
        BlockEventBudget budget = BLOCK_EVENT_BUDGETS.computeIfAbsent(dimension,
                ignored -> new BlockEventBudget());
        return budget.claim(level.getGameTime());
    }

    private static final class BlockEventBudget {
        private long tick = Long.MIN_VALUE;
        private int used;

        private synchronized boolean claim(long currentTick) {
            if (tick != currentTick) {
                tick = currentTick;
                used = 0;
            }
            if (used >= MAX_BLOCK_EVENTS_PER_TICK) {
                return false;
            }
            used++;
            return true;
        }
    }
}
