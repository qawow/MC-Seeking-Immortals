package com.xunxian.seekingimmortals.client.ui;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Client-local persistence for the selected {@link UiTheme}.
 *
 * <p>Stores a single {@code theme=<id>} line in
 * {@code config/seeking_immortals-ui-theme.properties}. Purely cosmetic and
 * client-side; never synced or trusted by the server. All I/O is lazy and
 * failure-tolerant so headless tests and broken filesystems degrade to the
 * 云笈墨卷 baseline instead of crashing.</p>
 */
public final class UiThemeConfig {
    private static final String FILE_NAME = "seeking_immortals-ui-theme.properties";
    private static final String KEY = "theme";
    private static volatile boolean loaded;

    private UiThemeConfig() {
    }

    /** Loads the persisted theme (once) and activates it. Safe to call repeatedly. */
    public static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        try {
            Path file = configFile();
            if (file == null || !Files.isRegularFile(file)) {
                return;
            }
            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(file)) {
                props.load(in);
            }
            UiTheme.setActive(UiTheme.byId(props.getProperty(KEY)));
        } catch (Exception e) {
            // 配置损坏时保持云笈墨卷基线，不影响进入游戏。
        }
    }

    /** Activates {@code theme} and persists the choice for future sessions. */
    public static void select(UiTheme theme) {
        UiTheme safe = theme == null ? UiTheme.INKSCROLL : theme;
        UiTheme.setActive(safe);
        try {
            Path file = configFile();
            if (file == null) {
                return;
            }
            Files.createDirectories(file.getParent());
            Properties props = new Properties();
            props.setProperty(KEY, safe.id());
            try (OutputStream out = Files.newOutputStream(file)) {
                props.store(out, "seeking_immortals UI theme (client-only cosmetic choice)");
            }
        } catch (IOException e) {
            // 写入失败只影响下次启动的记忆，本次会话仍已切换。
        }
    }

    private static Path configFile() {
        try {
            return net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
        } catch (Throwable t) {
            // 无 FML 环境（单元测试等）时退回工作目录下的 config/。
            try {
                return Paths.get("config", FILE_NAME);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
