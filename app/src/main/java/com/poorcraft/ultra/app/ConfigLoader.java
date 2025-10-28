package com.poorcraft.ultra.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poorcraft.ultra.shared.config.Config;
import com.poorcraft.ultra.shared.util.Logger;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class ConfigLoader {

    private static final org.slf4j.Logger LOG = Logger.getLogger(ConfigLoader.class);
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final Yaml YAML_MAPPER = new Yaml(new Constructor(Config.class, new LoaderOptions()));

    private ConfigLoader() {
    }

    public static Config load(Path defaultsYaml, Path overridesYaml, Path overridesJson) {
        Objects.requireNonNull(defaultsYaml, "defaultsYaml");

        Config defaults = loadYaml(defaultsYaml);
        if (defaults == null) {
            LOG.warn("Default configuration not found at {} – using built-in defaults", defaultsYaml);
            defaults = Config.createDefault();
        }

        Config merged = defaults;

        Config yamlOverrides = loadYaml(overridesYaml);
        if (yamlOverrides != null) {
            merged = mergeConfigs(merged, yamlOverrides);
        }

        Config jsonOverrides = loadJson(overridesJson);
        if (jsonOverrides != null) {
            merged = mergeConfigs(merged, jsonOverrides);
        }

        return merged;
    }

    public static Config loadYaml(Path path) {
        if (path == null || path.toString().isBlank()) {
            return null;
        }
        if (Files.notExists(path)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Config config = YAML_MAPPER.load(reader);
            if (config == null) {
                LOG.warn("YAML configuration at {} was empty", path);
            }
            return config;
        } catch (IOException ex) {
            LOG.error("Failed to read YAML configuration from {}", path, ex);
            return null;
        } catch (Exception ex) {
            LOG.error("Failed to parse YAML configuration from {}", path, ex);
            return null;
        }
    }

    public static Config loadJson(Path path) {
        if (path == null || path.toString().isBlank()) {
            return null;
        }
        if (Files.notExists(path)) {
            return null;
        }
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            if (content.isBlank()) {
                LOG.warn("JSON configuration at {} was empty", path);
                return null;
            }
            Config config = JSON_MAPPER.readValue(content, Config.class);
            if (config == null) {
                LOG.warn("JSON configuration at {} produced null config", path);
            }
            return config;
        } catch (IOException ex) {
            LOG.error("Failed to read JSON configuration from {}", path, ex);
            return null;
        }
    }

    public static Config mergeConfigs(Config base, Config overrides) {
        if (base == null) {
            base = Config.createDefault();
        }
        if (overrides == null) {
            return base;
        }

        ObjectNode baseNode = JSON_MAPPER.valueToTree(base);
        ObjectNode overridesNode = JSON_MAPPER.valueToTree(overrides);
        mergeNode(baseNode, overridesNode);
        try {
            return JSON_MAPPER.treeToValue(baseNode, Config.class);
        } catch (JsonProcessingException ex) {
            LOG.error("Failed to merge configuration objects", ex);
            return base;
        }
    }

    private static void mergeNode(ObjectNode base, JsonNode override) {
        override.fieldNames().forEachRemaining(field -> {
            JsonNode overrideValue = override.get(field);
            JsonNode baseValue = base.get(field);
            if (overrideValue == null || overrideValue.isNull()) {
                return;
            }
            if (baseValue != null && baseValue.isObject() && overrideValue.isObject()) {
                mergeNode((ObjectNode) baseValue, overrideValue);
            } else {
                base.set(field, overrideValue);
            }
        });
    }
}
