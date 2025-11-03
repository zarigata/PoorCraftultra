package com.poorcraft.ultra.blocks;

import java.util.EnumMap;
import java.util.Map;

/**
 * Immutable data class for block properties.
 */
public class BlockDefinition {
    
    private final short id;
    private final String name;
    private final Map<BlockFace, String> textures;
    private final float hardness;
    private final short drops;
    private final boolean transparent;
    private final boolean solid;
    private final int lightEmission;
    private final Map<String, String> stateProperties;
    
    private BlockDefinition(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.textures = new EnumMap<>(builder.textures);
        this.hardness = builder.hardness;
        this.drops = builder.drops;
        this.transparent = builder.transparent;
        this.solid = builder.solid;
        this.lightEmission = builder.lightEmission;
        this.stateProperties = new java.util.HashMap<>(builder.stateProperties);
        
        // Validation
        if (id > 0 && name.isEmpty()) {
            throw new IllegalArgumentException("Non-air blocks must have a name");
        }
        if (id > 0 && textures.isEmpty()) {
            throw new IllegalArgumentException("Non-air blocks must have at least one texture");
        }
    }
    
    public short getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public float getHardness() {
        return hardness;
    }
    
    public short getDrops() {
        return drops;
    }
    
    public boolean isTransparent() {
        return transparent;
    }
    
    public boolean isSolid() {
        return solid;
    }
    
    public boolean isAir() {
        return id == 0;
    }
    
    public int getLightEmission() {
        return lightEmission;
    }
    
    /**
     * Returns texture filename for face (with fallback to default if face not specified).
     */
    public String getTexture(BlockFace face) {
        String texture = textures.get(face);
        if (texture != null) {
            return texture;
        }
        // Fallback: try to find any texture
        if (!textures.isEmpty()) {
            return textures.values().iterator().next();
        }
        return "stone_01.png"; // Ultimate fallback
    }
    
    /**
     * Returns state property value by key (null if not found).
     */
    public String getStateProperty(String key) {
        return stateProperties.get(key);
    }
    
    /**
     * Returns all state properties.
     */
    public Map<String, String> getStateProperties() {
        return new java.util.HashMap<>(stateProperties);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private short id;
        private String name = "";
        private Map<BlockFace, String> textures = new EnumMap<>(BlockFace.class);
        private float hardness = 1.0f;
        private short drops;
        private boolean transparent = false;
        private boolean solid = true;
        private int lightEmission = 0;
        private Map<String, String> stateProperties = new java.util.HashMap<>();
        
        public Builder id(int id) {
            this.id = (short) id;
            this.drops = (short) id; // Default: drops self
            return this;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder texture(String texture) {
            // Set all faces to same texture
            for (BlockFace face : BlockFace.values()) {
                this.textures.put(face, texture);
            }
            return this;
        }
        
        public Builder texture(BlockFace face, String texture) {
            this.textures.put(face, texture);
            return this;
        }
        
        public Builder topBottomSide(String top, String bottom, String side) {
            this.textures.put(BlockFace.UP, top);
            this.textures.put(BlockFace.DOWN, bottom);
            this.textures.put(BlockFace.NORTH, side);
            this.textures.put(BlockFace.SOUTH, side);
            this.textures.put(BlockFace.EAST, side);
            this.textures.put(BlockFace.WEST, side);
            return this;
        }
        
        public Builder hardness(float hardness) {
            this.hardness = hardness;
            return this;
        }
        
        public Builder drops(int drops) {
            this.drops = (short) drops;
            return this;
        }
        
        public Builder transparent(boolean transparent) {
            this.transparent = transparent;
            return this;
        }
        
        public Builder solid(boolean solid) {
            this.solid = solid;
            return this;
        }
        
        public Builder lightEmission(int lightEmission) {
            this.lightEmission = lightEmission;
            return this;
        }
        
        public Builder stateProperty(String key, String value) {
            this.stateProperties.put(key, value);
            return this;
        }
        
        public Builder stateProperties(Map<String, String> properties) {
            this.stateProperties.putAll(properties);
            return this;
        }
        
        public BlockDefinition build() {
            return new BlockDefinition(this);
        }
    }
}
