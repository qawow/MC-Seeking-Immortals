package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.SkillType;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/** Server-authoritative snapshot of every cultivation skill. */
public record SyncSkillDataPacket(List<SkillData> skills) {
    public static final int MAX_SKILLS = 512;
    public static final int MAX_SKILL_TYPE_LENGTH = 64;

    public static SyncSkillDataPacket from(PlayerCultivation cultivation) {
        Objects.requireNonNull(cultivation, "cultivation");
        List<SkillData> entries = new ArrayList<>(SkillType.values().length);
        for (SkillType type : SkillType.values()) {
            CultivationSkill skill = cultivation.getSkill(type);
            boolean unlocked = skill != null && skill.isUnlocked();
            entries.add(new SkillData(
                    type.name(),
                    unlocked,
                    unlocked ? Math.max(0, skill.getLevel()) : 0,
                    unlocked ? Math.max(0, skill.getExperience()) : 0,
                    unlocked ? Math.max(0, skill.getProficiency()) : 0));
        }
        return new SyncSkillDataPacket(List.copyOf(entries));
    }

    public static void send(ServerPlayer player, PlayerCultivation cultivation) {
        Objects.requireNonNull(player, "player");
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), from(cultivation));
    }

    public static void encode(SyncSkillDataPacket packet, FriendlyByteBuf buffer) {
        List<SkillData> entries = Objects.requireNonNull(packet.skills, "skills");
        requireCount(entries.size());
        buffer.writeVarInt(entries.size());
        Set<String> seen = new HashSet<>();
        for (SkillData entry : entries) {
            if (entry == null) {
                throw new IllegalArgumentException("skill entry cannot be null");
            }
            String skillType = requireSkillTypeName(entry.skillType);
            if (!seen.add(skillType)) {
                throw new IllegalArgumentException("duplicate skill type " + skillType);
            }
            requireNonNegative(entry.level, "level");
            requireNonNegative(entry.experience, "experience");
            requireNonNegative(entry.proficiency, "proficiency");
            buffer.writeUtf(skillType, MAX_SKILL_TYPE_LENGTH);
            buffer.writeBoolean(entry.unlocked);
            buffer.writeVarInt(entry.level);
            buffer.writeVarInt(entry.experience);
            buffer.writeVarInt(entry.proficiency);
        }
    }

    public static SyncSkillDataPacket decode(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_SKILLS) {
            throw new DecoderException("skill count " + count + " exceeds " + MAX_SKILLS);
        }
        List<SkillData> entries = new ArrayList<>(count);
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < count; i++) {
            String skillType = buffer.readUtf(MAX_SKILL_TYPE_LENGTH);
            if (!isSkillTypeName(skillType)) {
                throw new DecoderException("invalid skill type " + skillType);
            }
            if (!seen.add(skillType)) {
                throw new DecoderException("duplicate skill type " + skillType);
            }
            boolean unlocked = buffer.readBoolean();
            int level = readNonNegative(buffer, "level");
            int experience = readNonNegative(buffer, "experience");
            int proficiency = readNonNegative(buffer, "proficiency");
            entries.add(new SkillData(skillType, unlocked, level, experience, proficiency));
        }
        return new SyncSkillDataPacket(List.copyOf(entries));
    }

    public static void handle(SyncSkillDataPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.xunxian.seekingimmortals.client.ClientSkillData.set(packet)));
        context.setPacketHandled(true);
    }

    private static void requireCount(int count) {
        if (count < 0 || count > MAX_SKILLS) {
            throw new IllegalArgumentException("skill count " + count + " exceeds " + MAX_SKILLS);
        }
    }

    private static String requireSkillTypeName(String skillType) {
        if (!isSkillTypeName(skillType)) {
            throw new IllegalArgumentException("invalid skill type " + skillType);
        }
        return skillType;
    }

    private static boolean isSkillTypeName(String skillType) {
        if (skillType == null || skillType.isBlank() || skillType.length() > MAX_SKILL_TYPE_LENGTH) {
            return false;
        }
        try {
            SkillType.valueOf(skillType);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static void requireNonNegative(int value, String label) {
        if (value < 0) {
            throw new IllegalArgumentException(label + " cannot be negative");
        }
    }

    private static int readNonNegative(FriendlyByteBuf buffer, String label) {
        int value = buffer.readVarInt();
        if (value < 0) {
            throw new DecoderException(label + " cannot be negative");
        }
        return value;
    }

    public record SkillData(String skillType, boolean unlocked, int level, int experience, int proficiency) {}
}
