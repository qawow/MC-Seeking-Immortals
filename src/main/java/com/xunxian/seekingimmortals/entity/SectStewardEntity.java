package com.xunxian.seekingimmortals.entity;

import com.xunxian.seekingimmortals.npc.NamedNpcRegistry;
import com.xunxian.seekingimmortals.npc.NpcDialogueApi;
import com.xunxian.seekingimmortals.sect.SectContributionService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Locale;

/**
 * M12 sect steward: station leash + day/night schedule + named-NPC dialogue entry.
 * Sect business still delegates to {@link SectContributionService} (M08).
 */
public class SectStewardEntity extends Villager {
    public static final String NPC_TYPE_RECRUITER = "recruiter";
    public static final String NPC_TYPE_CONTRIBUTION = "contribution_clerk";
    public static final String NPC_TYPE_DIALOGUE = "dialogue";

    private static final String TAG_SECT_ID = "SectId";
    private static final String TAG_NPC_TYPE = "SectNpcType";
    private static final String TAG_HOME = "SectHomePos";
    private static final String TAG_NAMED_NPC = "NamedNpcId";
    private static final String TAG_REGION = "RegionId";
    private static final String TAG_DIALOGUE_TREE = "DialogueTreeId";
    private static final String TAG_SHOP = "ShopId";

    private String sectId = SectContributionService.SECT_ID;
    private String npcType = NPC_TYPE_RECRUITER;
    private String namedNpcId = "";
    private String regionId = "";
    private String dialogueTreeId = "";
    private String shopId = "";
    private net.minecraft.core.BlockPos homePos;

    public SectStewardEntity(EntityType<? extends SectStewardEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Station duty: look at visitors; light stroll only while on duty and near home.
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.55D) {
            @Override
            public boolean canUse() {
                return homePos != null && isOnDuty() && super.canUse();
            }
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (homePos == null) {
            homePos = blockPosition().immutable();
        }
        // Soft leash: return to office when too far, or after hours.
        if (homePos != null) {
            double dist = distanceToSqr(homePos.getX() + 0.5D, homePos.getY(), homePos.getZ() + 0.5D);
            if (dist > 64.0D || !isOnDuty()) {
                getNavigation().moveTo(homePos.getX() + 0.5D, homePos.getY(), homePos.getZ() + 0.5D, 0.7D);
            }
        }
    }

    /**
     * Daytime duty window (vanilla day 0-12000). After hours the steward stays near home.
     */
    public boolean isOnDuty() {
        long dayTime = level().getDayTime() % 24000L;
        return dayTime < 12000L;
    }

    public boolean openDialogue(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        if (!namedNpcId.isBlank()) {
            return NpcDialogueApi.startDialogue(player, namedNpcId, dialogueTreeId);
        }
        // Resolve a named NPC for this sect/role when possible.
        String resolved = resolveNamedNpcId();
        if (!resolved.isBlank()) {
            setNamedNpcId(resolved);
            return NpcDialogueApi.startDialogue(player, resolved, dialogueTreeId);
        }
        return false;
    }

    private String resolveNamedNpcId() {
        if (!namedNpcId.isBlank()) {
            return namedNpcId;
        }
        for (NamedNpcRegistry.NamedNpc npc : NamedNpcRegistry.bySect(sectId)) {
            if ("outer_deacon".equals(npc.role())
                    || "patrol_captain".equals(npc.role())
                    || npc.role().contains("deacon")
                    || npc.archetype().contains("contribution")) {
                return npc.id();
            }
        }
        // Known seed for Huangfeng contribution clerk.
        if (sectId.contains("huangfeng") && NamedNpcRegistry.isKnown("npc_huangfeng_contribution")) {
            return "npc_huangfeng_contribution";
        }
        return "";
    }

    public void applyNamedNpc(NamedNpcRegistry.NamedNpc npc) {
        if (npc == null) {
            return;
        }
        setNamedNpcId(npc.id());
        setSectId(npc.sectId().isBlank() ? npc.factionId() : npc.sectId());
        setRegionId(npc.regionId());
        setDialogueTreeId(npc.dialogueTreeId());
        setShopId(npc.shopId());
        if (npc.archetype().contains("contribution") || "outer_deacon".equals(npc.role())) {
            setNpcType(NPC_TYPE_CONTRIBUTION);
        } else {
            setNpcType(NPC_TYPE_DIALOGUE);
        }
        setCustomName(Component.literal(npc.display()));
        setCustomNameVisible(true);
    }

    public String getSectId() {
        return sectId;
    }

    public void setSectId(String sectId) {
        this.sectId = normalize(sectId, SectContributionService.SECT_ID);
    }

    public String getNpcType() {
        return npcType;
    }

    public void setNpcType(String npcType) {
        this.npcType = normalize(npcType, NPC_TYPE_RECRUITER);
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
                if (shopId.isBlank()) {
                    shopId = npc.shopId();
                }
                if (regionId.isBlank()) {
                    regionId = npc.regionId();
                }
                if (sectId.isBlank() || SectContributionService.SECT_ID.equals(sectId)) {
                    if (!npc.sectId().isBlank()) {
                        sectId = npc.sectId();
                    } else if (!npc.factionId().isBlank()) {
                        sectId = npc.factionId();
                    }
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

    public String getDialogueTreeId() {
        return dialogueTreeId;
    }

    public void setDialogueTreeId(String dialogueTreeId) {
        this.dialogueTreeId = dialogueTreeId == null ? "" : dialogueTreeId.trim().toLowerCase(Locale.ROOT);
    }

    public String getShopId() {
        return shopId;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId == null ? "" : shopId.trim().toLowerCase(Locale.ROOT);
    }

    public net.minecraft.core.BlockPos getHomePos() {
        return homePos;
    }

    public void setHomePos(net.minecraft.core.BlockPos homePos) {
        this.homePos = homePos == null ? null : homePos.immutable();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(TAG_SECT_ID, sectId);
        tag.putString(TAG_NPC_TYPE, npcType);
        tag.putString(TAG_NAMED_NPC, namedNpcId);
        tag.putString(TAG_REGION, regionId);
        tag.putString(TAG_DIALOGUE_TREE, dialogueTreeId);
        tag.putString(TAG_SHOP, shopId);
        if (homePos != null) {
            tag.putLong(TAG_HOME, homePos.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSectId(tag.getString(TAG_SECT_ID));
        setNpcType(tag.getString(TAG_NPC_TYPE));
        setNamedNpcId(tag.getString(TAG_NAMED_NPC));
        setRegionId(tag.getString(TAG_REGION));
        setDialogueTreeId(tag.getString(TAG_DIALOGUE_TREE));
        setShopId(tag.getString(TAG_SHOP));
        if (tag.contains(TAG_HOME)) {
            homePos = net.minecraft.core.BlockPos.of(tag.getLong(TAG_HOME));
        }
    }

    private static String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? fallback : normalized;
    }
}
