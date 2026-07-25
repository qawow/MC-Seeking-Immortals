package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.network.VisualEventPacket;
import com.xunxian.seekingimmortals.visual.VisualTimelineEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.LinkedHashMap;
import java.util.Map;

/** Distinct client route for authored MODEL_ANIMATION state changes. */
@OnlyIn(Dist.CLIENT)
public final class ClientModelAnimationRuntime {
    private static final int MAX_STATES = 128;
    private static final Map<String, String> LAST_STATES = new LinkedHashMap<>();

    private ClientModelAnimationRuntime() {}

    public static void trigger(ClientLevel level, VisualEventPacket packet, VisualTimelineEvent event) {
        if (level == null || packet == null || event == null) {
            return;
        }
        LAST_STATES.put(packet.profileKey().toString(), event.state());
        while (LAST_STATES.size() > MAX_STATES) {
            LAST_STATES.remove(LAST_STATES.keySet().iterator().next());
        }
        Entity anchor = packet.anchorType() == VisualEventPacket.AnchorType.ENTITY
                ? level.getEntity(packet.entityId()) : Minecraft.getInstance().player;
        if (anchor instanceof LivingEntity living) {
            living.swing(InteractionHand.MAIN_HAND, true);
        }
    }

    public static String lastState(String profileKey) {
        return LAST_STATES.getOrDefault(profileKey == null ? "" : profileKey, "");
    }

    public static void reset() {
        LAST_STATES.clear();
    }
}
