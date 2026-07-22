package com.xunxian.seekingimmortals.entity;

import com.xunxian.seekingimmortals.npc.NamedNpcRegistry;
import com.xunxian.seekingimmortals.npc.NpcDialogueApi;
import com.xunxian.seekingimmortals.quest.QuestService;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Locale;

/** Generic named story NPC for roles that are neither merchants nor stewards. */
public class QuestNpcEntity extends CultivatorNpcEntity {
    private static final String TAG_HOME = "QuestNpcHomePos";
    private static final String TAG_NAMED_NPC = "NamedNpcId";
    private static final String TAG_REGION = "RegionId";
    private static final String TAG_DIALOGUE_TREE = "DialogueTreeId";

    private BlockPos homePos;
    private String namedNpcId = "";
    private String regionId = "";
    private String dialogueTreeId = "";

    public QuestNpcEntity(EntityType<? extends QuestNpcEntity> type, Level level) {
        super(type, level, VisualRole.QUEST);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.55D));
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
        double dist = distanceToSqr(homePos.getX() + 0.5D, homePos.getY(), homePos.getZ() + 0.5D);
        if (dist > 100.0D && tickCount % 20 == 0 && getNavigation().isDone()) {
            getNavigation().moveTo(homePos.getX() + 0.5D, homePos.getY(), homePos.getZ() + 0.5D, 0.75D);
        }
    }

    public boolean openFor(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        if (QuestService.handleNamedNpcInteraction(player, this)) {
            return true;
        }
        if (!namedNpcId.isBlank() && NpcDialogueApi.startDialogue(player, namedNpcId, dialogueTreeId, this)) {
            return true;
        }
        return false;
    }

    public void applyNamedNpc(NamedNpcRegistry.NamedNpc npc) {
        if (npc == null) {
            return;
        }
        setNamedNpcId(npc.id());
        setRegionId(npc.regionId());
        setDialogueTreeId(npc.dialogueTreeId());
        setCustomName(PlayerDisplayText.isSafe(npc.display())
                ? Component.literal(npc.display().trim())
                : Component.translatable("entity.seeking_immortals.quest_npc"));
        setCustomNameVisible(true);
    }

    public void setStoryIdentity(String displayName) {
        String display = displayName == null ? "" : displayName.trim();
        setCustomName(PlayerDisplayText.isSafe(display)
                ? Component.literal(display)
                : Component.translatable("entity.seeking_immortals.quest_npc"));
        setCustomNameVisible(true);
    }

    @Override
    public String getNamedNpcId() {
        return namedNpcId;
    }

    public void setNamedNpcId(String namedNpcId) {
        this.namedNpcId = normalize(namedNpcId);
    }

    @Override
    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = normalize(regionId);
    }

    public void setDialogueTreeId(String dialogueTreeId) {
        this.dialogueTreeId = normalize(dialogueTreeId);
    }

    public void setHomePos(BlockPos homePos) {
        this.homePos = homePos == null ? null : homePos.immutable();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (homePos != null) {
            tag.putLong(TAG_HOME, homePos.asLong());
        }
        tag.putString(TAG_NAMED_NPC, namedNpcId);
        tag.putString(TAG_REGION, regionId);
        tag.putString(TAG_DIALOGUE_TREE, dialogueTreeId);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_HOME)) {
            homePos = BlockPos.of(tag.getLong(TAG_HOME));
        }
        setNamedNpcId(tag.getString(TAG_NAMED_NPC));
        setRegionId(tag.getString(TAG_REGION));
        setDialogueTreeId(tag.getString(TAG_DIALOGUE_TREE));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
