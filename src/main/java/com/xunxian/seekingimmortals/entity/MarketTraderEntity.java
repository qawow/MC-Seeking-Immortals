package com.xunxian.seekingimmortals.entity;

import com.xunxian.seekingimmortals.npc.NamedNpcRegistry;
import com.xunxian.seekingimmortals.npc.NpcDialogueApi;
import com.xunxian.seekingimmortals.shop.ShopService;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Locale;

/**
 * M12 market trader: stall leash + trading hours + optional named-NPC dialogue / shop.
 * Shop shelves remain owned by M05 {@link ShopService}.
 */
public class MarketTraderEntity extends CultivatorNpcEntity {
    private static final String TAG_STALL = "MarketStallPos";
    private static final String TAG_NAMED_NPC = "NamedNpcId";
    private static final String TAG_REGION = "RegionId";
    private static final String TAG_SHOP = "ShopId";
    private static final String TAG_DIALOGUE_TREE = "DialogueTreeId";

    private net.minecraft.core.BlockPos stallPos;
    private String namedNpcId = "";
    private String regionId = "";
    private String shopId = ShopService.MARKET_HERBAL_STALL;
    private String dialogueTreeId = "";

    public MarketTraderEntity(EntityType<? extends MarketTraderEntity> type, Level level) {
        super(type, level, VisualRole.TRADER);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomStrollGoal(this, 0.45D) {
            @Override
            public boolean canUse() {
                return isTradingHours() && super.canUse();
            }
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (stallPos == null) {
            stallPos = blockPosition().immutable();
        }
        if (stallPos != null) {
            double dist = distanceToSqr(stallPos.getX() + 0.5D, stallPos.getY(), stallPos.getZ() + 0.5D);
            double returnDistanceSqr = isTradingHours() ? 36.0D : 4.0D;
            if (dist > returnDistanceSqr && tickCount % 20 == 0 && getNavigation().isDone()) {
                getNavigation().moveTo(stallPos.getX() + 0.5D, stallPos.getY(), stallPos.getZ() + 0.5D, 0.6D);
            }
        }
    }

    /** Market hours: morning to dusk (0-13000). */
    public boolean isTradingHours() {
        long dayTime = level().getDayTime() % 24000L;
        return dayTime < 13000L;
    }

    public boolean openFor(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        // Prefer dialogue tree when bound; shop action nodes open shelves server-side.
        if (!namedNpcId.isBlank() || !dialogueTreeId.isBlank()) {
            String npc = namedNpcId.isBlank() ? "market_vendor" : namedNpcId;
            if (NpcDialogueApi.startDialogue(player, npc, dialogueTreeId, this)) {
                return true;
            }
        }
        String shop = shopId == null || shopId.isBlank() ? ShopService.MARKET_HERBAL_STALL : shopId;
        ShopService.openMarket(player, shop, this);
        return true;
    }

    public void applyNamedNpc(NamedNpcRegistry.NamedNpc npc) {
        if (npc == null) {
            return;
        }
        setNamedNpcId(npc.id());
        setRegionId(npc.regionId());
        setDialogueTreeId(npc.dialogueTreeId());
        if (!npc.shopId().isBlank()) {
            setShopId(npc.shopId());
        }
        setCustomName(PlayerDisplayText.isSafe(npc.display())
                ? Component.literal(npc.display().trim())
                : Component.literal("无名商人"));
        setCustomNameVisible(true);
    }

    public String getNamedNpcId() {
        return namedNpcId;
    }

    public void setNamedNpcId(String namedNpcId) {
        this.namedNpcId = namedNpcId == null ? "" : namedNpcId.trim().toLowerCase(Locale.ROOT);
        if (!this.namedNpcId.isBlank()) {
            NamedNpcRegistry.find(this.namedNpcId).ifPresent(npc -> {
                if (dialogueTreeId.isBlank()) {
                    dialogueTreeId = npc.dialogueTreeId();
                }
                if (shopId == null || shopId.isBlank() || ShopService.MARKET_HERBAL_STALL.equals(shopId)) {
                    if (!npc.shopId().isBlank()) {
                        shopId = npc.shopId();
                    }
                }
                if (regionId.isBlank()) {
                    regionId = npc.regionId();
                }
            });
        }
    }

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId == null ? "" : regionId.trim().toLowerCase(Locale.ROOT);
    }

    public String getShopId() {
        return shopId;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId == null || shopId.isBlank()
                ? ShopService.MARKET_HERBAL_STALL
                : shopId.trim().toLowerCase(Locale.ROOT);
    }

    public String getDialogueTreeId() {
        return dialogueTreeId;
    }

    public void setDialogueTreeId(String dialogueTreeId) {
        this.dialogueTreeId = dialogueTreeId == null ? "" : dialogueTreeId.trim().toLowerCase(Locale.ROOT);
    }

    public net.minecraft.core.BlockPos getStallPos() {
        return stallPos;
    }

    public void setStallPos(net.minecraft.core.BlockPos stallPos) {
        this.stallPos = stallPos == null ? null : stallPos.immutable();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (stallPos != null) {
            tag.putLong(TAG_STALL, stallPos.asLong());
        }
        tag.putString(TAG_NAMED_NPC, namedNpcId);
        tag.putString(TAG_REGION, regionId);
        tag.putString(TAG_SHOP, shopId == null ? "" : shopId);
        tag.putString(TAG_DIALOGUE_TREE, dialogueTreeId);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_STALL)) {
            stallPos = net.minecraft.core.BlockPos.of(tag.getLong(TAG_STALL));
        }
        setNamedNpcId(tag.getString(TAG_NAMED_NPC));
        setRegionId(tag.getString(TAG_REGION));
        if (tag.contains(TAG_SHOP)) {
            setShopId(tag.getString(TAG_SHOP));
        }
        setDialogueTreeId(tag.getString(TAG_DIALOGUE_TREE));
    }
}
