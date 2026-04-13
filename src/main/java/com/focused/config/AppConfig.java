package com.focused.config;

import java.io.IOException;
import java.nio.file.*;
import java.util.Properties;

/**
 * AppConfig — owns everything related to settings and persistence.
 * Saves a simple .properties file next to the running JAR.
 * This is portable — the user can carry the folder anywhere.
 *
 * Rules for this class:
 *   - Never import JavaFX (config is not UI)
 *   - Never import JNA (config is not OS)
 *   - Only reads/writes to disk
 */
public class AppConfig {

    private static final String FILE_NAME = "focused.properties";

    private final Path      configPath;
    private final Properties props;

    // Constructor

    public AppConfig() {
        // Resolve config file next to the running JAR (portable mode)
        this.configPath = resolveConfigPath();
        this.props      = new Properties();
        load();
    }

    // Public

    public String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(props.getProperty(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public void set(String key, String value) {
        props.setProperty(key, value);
        save();
    }

    public void set(String key, int value) {
        set(key, String.valueOf(value));
    }

    // Keys (constants so nobody typos a string)

    public static final String KEY_LAST_WINDOW   = "last.window";
    public static final String KEY_LAST_DURATION = "last.duration.seconds";

    // Internal

    private void load() {
        if (Files.exists(configPath)) {
            try (var in = Files.newInputStream(configPath)) {
                props.load(in);
            } catch (IOException e) {
                System.err.println("[AppConfig] Could not load config: " + e.getMessage());
            }
        }
    }

    private void save() {
        try (var out = Files.newOutputStream(configPath,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            props.store(out, "Focused app config — do not edit manually");
        } catch (IOException e) {
            System.err.println("[AppConfig] Could not save config: " + e.getMessage());
        }
    }

    private static Path resolveConfigPath() {
        try {
            // Get the directory of the running JAR
            Path jarDir = Path.of(
                AppConfig.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
            ).getParent();
            return jarDir.resolve(FILE_NAME);
        } catch (Exception e) {
            // Fallback: current working directory (works fine in dev)
            return Path.of(FILE_NAME);
        }
    }
}
