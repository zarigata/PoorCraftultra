package com.poorcraftultra.world.block;

import java.util.Objects;

/**
 * Represents a block type definition with properties and texture references.
 * <p>
 * Each block has:
 * <ul>
 *   <li>A unique byte ID (0-255)</li>
 *   <li>A string identifier (e.g., "stone", "grass")</li>
 *   <li>A display name for UI</li>
 *   <li>Behavioral properties (solid, transparent, etc.)</li>
 *   <li>Texture references for each of the 6 faces</li>
 * </ul>
 * <p>
 * Blocks are immutable after registration, except for atlas coordinates which are
 * set once during texture atlas initialization.
 */
public class Block {
    private final byte id;
    private final String name;
    private final String displayName;
    private final BlockProperties properties;
    private final TextureReference[] faceTextures;

    /**
     * Creates a Block with different textures for each face.
     *
     * @param id          unique block ID (0-255)
     * @param name        identifier (e.g., "stone")
     * @param displayName user-friendly name
     * @param properties  block properties
     * @param textureNames texture names for each face [TOP, BOTTOM, NORTH, SOUTH, EAST, WEST]
     */
    public Block(byte id, String name, String displayName, BlockProperties properties, String[] textureNames) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.properties = properties;
        this.faceTextures = new TextureReference[6];

        if (textureNames.length != 6) {
            throw new IllegalArgumentException("Must provide exactly 6 texture names (one per face)");
        }

        for (int i = 0; i < 6; i++) {
            this.faceTextures[i] = new TextureReference(textureNames[i]);
        }
    }

    /**
     * Creates a Block with the same texture on all faces.
     *
     * @param id          unique block ID (0-255)
     * @param name        identifier (e.g., "stone")
     * @param displayName user-friendly name
     * @param properties  block properties
     * @param textureName texture name for all faces
     */
    public Block(byte id, String name, String displayName, BlockProperties properties, String textureName) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.properties = properties;
        this.faceTextures = new TextureReference[6];

        for (int i = 0; i < 6; i++) {
            this.faceTextures[i] = new TextureReference(textureName);
        }
    }

    /**
     * @return the unique block ID
     */
    public byte getId() {
        return id;
    }

    /**
     * @return the block identifier (e.g., "stone")
     */
    public String getName() {
        return name;
    }

    /**
     * @return the user-friendly display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @return the block properties
     */
    public BlockProperties getProperties() {
        return properties;
    }

    /**
     * Gets the texture reference for a specific face.
     *
     * @param face the block face
     * @return the texture reference for that face
     */
    public TextureReference getTextureReference(BlockFace face) {
        return faceTextures[face.ordinal()];
    }

    /**
     * Delegates to properties.isSolid().
     *
     * @return true if the block is solid
     */
    public boolean isSolid() {
        return properties.isSolid();
    }

    /**
     * Delegates to properties.isTransparent().
     *
     * @return true if the block is transparent
     */
    public boolean isTransparent() {
        return properties.isTransparent();
    }

    /**
     * Delegates to properties.isLightEmitting().
     *
     * @return true if the block emits light
     */
    public boolean isLightEmitting() {
        return properties.isLightEmitting();
    }

    /**
     * Delegates to properties.hasGravity().
     *
     * @return true if the block is affected by gravity
     */
    public boolean hasGravity() {
        return properties.hasGravity();
    }

    /**
     * Sets the atlas coordinates for a specific face.
     * Called by TextureAtlas during initialization.
     *
     * @param face the block face
     * @param x    atlas X coordinate (tile coordinate, not pixels)
     * @param y    atlas Y coordinate (tile coordinate, not pixels)
     */
    public void setAtlasCoordinates(BlockFace face, int x, int y) {
        faceTextures[face.ordinal()].atlasX = x;
        faceTextures[face.ordinal()].atlasY = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Block block = (Block) o;
        return id == block.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Block{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                '}';
    }

    /**
     * Represents a reference to a texture in the texture atlas.
     * Contains the texture name and atlas coordinates (set during atlas building).
     */
    public static class TextureReference {
        public final String textureName;
        public int atlasX;
        public int atlasY;

        public TextureReference(String textureName) {
            this.textureName = textureName;
            this.atlasX = -1;
            this.atlasY = -1;
        }

        @Override
        public String toString() {
            return "TextureReference{" +
                    "textureName='" + textureName + '\'' +
                    ", atlasX=" + atlasX +
                    ", atlasY=" + atlasY +
                    '}';
        }
    }
}
