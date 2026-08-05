package com.xunxian.seekingimmortals.beast;

import com.xunxian.seekingimmortals.item.InventoryDeliveryService;
import com.xunxian.seekingimmortals.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.UUID;

/**
 * Y-B: live-carrier state for authored capture-only beasts (阴芝马).
 *
 * <p>Authored rules this enforces: 「活的才是丹。死的，只是一堆打折的肉」——a live carrier is a
 * distinct, non-stackable object bound to one capture instance and one secret-realm session, and
 * a carrier that dies or times out in transit degrades to inferior material instead of vanishing.</p>
 *
 * <p>All state lives in item NBT so the carrier survives relog; the instance UUID prevents a
 * duplicated stack from being submitted twice.</p>
 */
public final class LiveCaptureCarrierService {
    public static final String TAG_ROOT = "SeekingImmortalsLiveCapture";
    public static final String TAG_BEAST = "Beast";
    public static final String TAG_UUID = "CaptureUuid";
    public static final String TAG_SESSION = "SourceSession";
    public static final String TAG_STATE = "LifeState";
    public static final String TAG_CAPTURED_AT = "CapturedAt";
    public static final String TAG_TIER = "Tier";
    /** Transit window before a live carrier degrades on its own (20 real minutes). */
    public static final int LIVE_TIMEOUT_TICKS = 20 * 60 * 20;

    public enum LifeState {
        LIVE,
        DEGRADED;

        static LifeState parse(String raw) {
            return "degraded".equalsIgnoreCase(raw == null ? "" : raw.trim()) ? DEGRADED : LIVE;
        }

        String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private LiveCaptureCarrierService() {}

    public static boolean isCarrier(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.hasTag()
                && stack.getTag().contains(TAG_ROOT)
                && !root(stack).getString(TAG_BEAST).isBlank();
    }

    public static String beastId(ItemStack stack) {
        return isCarrier(stack) ? root(stack).getString(TAG_BEAST) : "";
    }

    public static String captureUuid(ItemStack stack) {
        return isCarrier(stack) ? root(stack).getString(TAG_UUID) : "";
    }

    public static String sourceSession(ItemStack stack) {
        return isCarrier(stack) ? root(stack).getString(TAG_SESSION) : "";
    }

    public static LifeState lifeState(ItemStack stack) {
        return isCarrier(stack) ? LifeState.parse(root(stack).getString(TAG_STATE)) : LifeState.DEGRADED;
    }

    public static boolean isLive(ItemStack stack) {
        return lifeState(stack) == LifeState.LIVE;
    }

    public static int tier(ItemStack stack) {
        return isCarrier(stack) ? BeastTierService.clampTier(root(stack).getInt(TAG_TIER)) : 1;
    }

    public static long capturedAt(ItemStack stack) {
        return isCarrier(stack) ? root(stack).getLong(TAG_CAPTURED_AT) : 0L;
    }

    /**
     * Stamps a fresh live carrier. The stack must already be single (carrier items are
     * {@code stacksTo(1)}); a new instance UUID is minted per capture so two carriers can never
     * settle the same capture twice.
     */
    public static ItemStack createLive(String beastId, int tier, String sessionId, long gameTime) {
        ItemStack stack = new ItemStack(ModItems.LIVE_BEAST_CARRIER.get(), 1);
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_BEAST, normalize(beastId));
        tag.putString(TAG_UUID, UUID.randomUUID().toString());
        tag.putString(TAG_SESSION, sessionId == null ? "" : sessionId);
        tag.putString(TAG_STATE, LifeState.LIVE.key());
        tag.putLong(TAG_CAPTURED_AT, gameTime);
        tag.putInt(TAG_TIER, BeastTierService.clampTier(tier));
        stack.getOrCreateTag().put(TAG_ROOT, tag);
        return stack;
    }

    /** Marks a carrier dead in transit; the beast id and instance stay for the inferior payout. */
    public static boolean degrade(ItemStack stack) {
        if (!isCarrier(stack) || !isLive(stack)) {
            return false;
        }
        CompoundTag tag = root(stack).copy();
        tag.putString(TAG_STATE, LifeState.DEGRADED.key());
        stack.getOrCreateTag().put(TAG_ROOT, tag);
        return true;
    }

    /**
     * Transit timeout check, called from the player tick.
     * A carrier past {@link #LIVE_TIMEOUT_TICKS} degrades rather than being silently deleted, so
     * the player still gets the failure-branch payout the author specified.
     */
    public static boolean tickTransit(ServerPlayer player, ItemStack stack, long gameTime) {
        if (player == null || !isCarrier(stack) || !isLive(stack)) {
            return false;
        }
        long captured = capturedAt(stack);
        if (captured <= 0L || gameTime - captured < LIVE_TIMEOUT_TICKS) {
            return false;
        }
        if (!degrade(stack)) {
            return false;
        }
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.capture.carrier_timeout",
                BeastDisplay.of(beastId(stack))), true);
        return true;
    }

    /**
     * Degrades every live carrier the player holds; used for the authored death branch
     * 「运出途中死，只剩劣材」 so dying in transit never yields full-price material.
     */
    public static int degradeAllCarried(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        int degraded = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (isCarrier(stack) && isLive(stack) && degrade(stack)) {
                degraded++;
            }
        }
        if (degraded > 0) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.capture.carrier_died", degraded), true);
        }
        return degraded;
    }

    /**
     * Kill hook: an authored capture-only beast that dies pays inferior material only.
     * Resolves the beast id from the entity the same way the loot path does.
     */
    public static boolean onCaptureOnlyKilled(ServerPlayer killer,
                                              net.minecraft.world.entity.LivingEntity dead) {
        if (killer == null || dead == null) {
            return false;
        }
        String beastId = dead instanceof com.xunxian.seekingimmortals.entity.CultivationBeastEntity beast
                ? beast.getBeastId()
                : dead.getPersistentData().getString("seeking_immortals_beast_id");
        if (beastId == null || beastId.isBlank()
                || !BeastBestiaryService.isCaptureOnlyBeast(beastId)) {
            return false;
        }
        grantKillMaterial(killer, beastId);
        return true;
    }

    /**
     * Kill payout for an authored capture-only beast: inferior material only, never a live carrier.
     * Enforces 「击杀降材料品质」 / 「杀光=失败向」.
     */
    public static void grantKillMaterial(ServerPlayer player, String beastId) {
        if (player == null) {
            return;
        }
        ItemStack material = new ItemStack(ModItems.SPIRIT_BEAST_BONE.get(), 1);
        InventoryDeliveryService.giveOrEnqueue(player, material, "capture_kill_material");
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.capture.kill_inferior", BeastDisplay.of(beastId)), true);
    }

    private static CompoundTag root(ItemStack stack) {
        return stack.getOrCreateTag().getCompound(TAG_ROOT);
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    /** Display helper kept local so tooltips and messages resolve ids the same way. */
    static final class BeastDisplay {
        private BeastDisplay() {}

        static Component of(String beastId) {
            return BeastBestiaryService.find(beastId)
                    .map(BeastBestiaryService.BeastEntry::display)
                    .map(display -> com.xunxian.seekingimmortals.util.PlayerDisplayText
                            .safeLiteral(display, "text.seeking_immortals.unknown_item"))
                    .orElseGet(() -> Component.translatable("text.seeking_immortals.unknown_item"));
        }
    }
}
