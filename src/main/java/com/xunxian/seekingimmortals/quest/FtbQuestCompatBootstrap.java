package com.xunxian.seekingimmortals.quest;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.compat.ModCompat;

import java.lang.reflect.InvocationTargetException;

/** Keeps optional FTB API types outside the common mod entrypoint. */
public final class FtbQuestCompatBootstrap {
    private static final String HOOK_CLASS =
            "com.xunxian.seekingimmortals.quest.FtbCustomTaskHooks";

    private FtbQuestCompatBootstrap() {}

    public static void registerIfPresent() {
        if (!ModCompat.FTB_QUESTS_LOADED) {
            return;
        }
        try {
            Class<?> hooks = Class.forName(HOOK_CLASS, true, FtbQuestCompatBootstrap.class.getClassLoader());
            hooks.getMethod("register").invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException exception) {
            SeekingImmortalsMod.LOGGER.error("FTB Quests is present but its quest bridge could not be loaded", exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            SeekingImmortalsMod.LOGGER.error("FTB Quests bridge registration failed", cause);
        } catch (LinkageError error) {
            SeekingImmortalsMod.LOGGER.error("FTB Quests API is incompatible; native quests remain available", error);
        }
    }
}
