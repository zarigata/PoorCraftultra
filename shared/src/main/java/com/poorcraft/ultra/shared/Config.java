package com.poorcraft.ultra.shared;

import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;

import java.util.List;

public final class Config {

    private static volatile Config INSTANCE;

    private final com.typesafe.config.Config config;

    private Config() {
        try {
            com.typesafe.config.Config loaded = ConfigFactory.load("application");
            if (loaded == null) {
                throw new ConfigLoadException("Failed to load application.conf from classpath");
            }
            this.config = loaded.withFallback(ConfigFactory.defaultReference()).resolve();
        } catch (ConfigException e) {
            throw new ConfigLoadException("Unable to load configuration", e);
        }
    }

    public static Config getInstance() {
        if (INSTANCE == null) {
            synchronized (Config.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Config();
                }
            }
        }
        return INSTANCE;
    }

    public static Config load() {
        return getInstance();
    }

    public String getString(String path) {
        try {
            return config.getString(path);
        } catch (ConfigException e) {
            throw new ConfigLoadException("Failed to get string for path: " + path, e);
        }
    }

    public int getInt(String path) {
        try {
            return config.getInt(path);
        } catch (ConfigException e) {
            throw new ConfigLoadException("Failed to get int for path: " + path, e);
        }
    }

    public boolean getBoolean(String path) {
        try {
            return config.getBoolean(path);
        } catch (ConfigException e) {
            throw new ConfigLoadException("Failed to get boolean for path: " + path, e);
        }
    }

    public double getDouble(String path) {
        try {
            return config.getDouble(path);
        } catch (ConfigException e) {
            throw new ConfigLoadException("Failed to get double for path: " + path, e);
        }
    }

    public List<String> getStringList(String path) {
        try {
            return config.getStringList(path);
        } catch (ConfigException e) {
            throw new ConfigLoadException("Failed to get string list for path: " + path, e);
        }
    }

    public boolean hasPath(String path) {
        return config.hasPath(path);
    }

    public com.typesafe.config.Config getConfig(String path) {
        try {
            return config.getConfig(path);
        } catch (ConfigException e) {
            throw new ConfigLoadException("Failed to get config for path: " + path, e);
        }
    }

    public com.typesafe.config.Config getRawConfig() {
        return config;
    }
}
