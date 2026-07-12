package com.xunxian.seekingimmortals.catalog;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.worldpack.WorldpackGameplayService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.Locale;
import java.util.Optional;

/**
 * Applies text-material manuals_catalog entries as honest runtime unlocks:
 * temporary insight buffs + chat notes. Full method/forge gates remain deferred.
 */
public final class ManualCatalogService {
    private ManualCatalogService() {}

    public static boolean study(ServerPlayer player, String manualId) {
        Optional<TextMaterialCatalogService.ManualEntry> optional =
                TextMaterialCatalogService.builtin().findManual(manualId);
        if (optional.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.manual.unknown", manualId), false);
            return false;
        }
        TextMaterialCatalogService.ManualEntry manual = optional.get();
        boolean[] ok = {false};
        CultivationHelper.get(player).ifPresent(cultivation -> {
            if (!manual.realmMin().isBlank() && !WorldpackGameplayService.meetsMinRealm(cultivation.getRealm(), manual.realmMin())) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.manual.realm_too_low",
                        manual.display(), manual.realmMin()), false);
                return;
            }
            applyInsight(player, manual);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.manual.studied",
                    manual.display(), manual.type()), true);
            if (!manual.note().isBlank()) {
                player.displayClientMessage(Component.literal(manual.note()), false);
            }
            if (!manual.unlocks().isEmpty()) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.manual.unlocks",
                        String.join(", ", manual.unlocks())), false);
            }
            ok[0] = true;
        });
        return ok[0];
    }

    public static int manualCount() {
        return TextMaterialCatalogService.builtin().manuals().size();
    }

    public static int methodCount() {
        return TextMaterialCatalogService.builtin().methods().size();
    }

    private static void applyInsight(ServerPlayer player, TextMaterialCatalogService.ManualEntry manual) {
        String type = manual.type() == null ? "" : manual.type().toLowerCase(Locale.ROOT);
        int duration = 20 * 60 * 5;
        if (type.contains("refinement") || type.contains("forge")) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, duration, 0));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0));
        } else if (type.contains("puppet")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 0));
        } else if (type.contains("ghost") || type.contains("cultivation")) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 0));
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, duration, 0));
            player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, Math.min(duration, 20 * 90), 0));
        }
    }
}
