package com.xunxian.seekingimmortals.registry;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Wave56 dialogue / presentation sound events.
 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, SeekingImmortalsMod.MODID);

    public static final RegistryObject<SoundEvent> DIALOGUE_GREETING = register("dialogue_greeting");
    public static final RegistryObject<SoundEvent> DIALOGUE_ADVANCE = register("dialogue_advance");
    public static final RegistryObject<SoundEvent> DIALOGUE_BRANCH = register("dialogue_branch");
    public static final RegistryObject<SoundEvent> DIALOGUE_NPC_MO_LAO = register("dialogue_npc_mo_lao");
    public static final RegistryObject<SoundEvent> DIALOGUE_NPC_GUIDE = register("dialogue_npc_guide");

    private ModSounds() {}

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(
                new ResourceLocation(SeekingImmortalsMod.MODID, name)));
    }

    public static void register(IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }
}
