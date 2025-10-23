package com.poorcraftultra.rendering;

import com.poorcraftultra.world.block.Block;
import com.poorcraftultra.world.block.BlockFace;
import com.poorcraftultra.world.block.BlockRegistry;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

/**
 * Manages a texture atlas for block textures.
 * <p>
 * The atlas is a single 2D texture containing multiple 32×32 tile textures
 * packed in a grid. Each texture is assigned a position in the atlas, and
 * UV coordinates are calculated for rendering.
 * <p>
 * Phase 7 adds biome-specific textures to support diverse terrain generation.
 * <p>
 * <b>Atlas Layout:</b>
 * <ul>
 *   <li>Tile size: 32×32 pixels</li>
 *   <li>Packing: left-to-right, top-to-bottom</li>
 *   <li>Texture filtering: GL_NEAREST (pixelated look)</li>
 *   <li>Texture wrapping: GL_CLAMP_TO_EDGE</li>
 * </ul>
 */
public class TextureAtlas {
    private int textureId;
    private final int atlasWidth;
    private final int atlasHeight;
    private final int tileSize;
    private final int tilesPerRow;
    private final Map<String, AtlasPosition> texturePositions;

    /**
     * Creates a TextureAtlas with the specified dimensions.
     *
     * @param atlasWidth  atlas width in pixels
     * @param atlasHeight atlas height in pixels
     * @param tileSize    size of each tile in pixels (e.g., 32)
     */
    public TextureAtlas(int atlasWidth, int atlasHeight, int tileSize) {
        this.atlasWidth = atlasWidth;
        this.atlasHeight = atlasHeight;
        this.tileSize = tileSize;
        this.tilesPerRow = atlasWidth / tileSize;
        this.texturePositions = new HashMap<>();
        this.textureId = -1;
    }

    /**
     * Loads textures from the provided map and builds the atlas.
     *
     * @param textureFiles map of texture name to resource path
     * @throws RuntimeException if texture loading fails
     */
    public void load(Map<String, String> textureFiles) {
        // Create atlas buffer
        ByteBuffer atlasBuffer = BufferUtils.createByteBuffer(atlasWidth * atlasHeight * 4); // RGBA

        int currentTile = 0;
        int tilesPerColumn = atlasHeight / tileSize;
        int maxTiles = tilesPerRow * tilesPerColumn;

        // Load each texture and copy to atlas
        for (Map.Entry<String, String> entry : textureFiles.entrySet()) {
            String textureName = entry.getKey();
            String resourcePath = entry.getValue();

            if (currentTile >= maxTiles) {
                throw new RuntimeException("Atlas is full! Cannot fit texture: " + textureName);
            }

            // Load image using STB
            ByteBuffer imageData = loadImageFromResource(resourcePath);
            boolean isPlaceholder = false;
            if (imageData == null) {
                System.err.println("Warning: Failed to load texture: " + resourcePath + ", using placeholder");
                imageData = createPlaceholderTexture();
                isPlaceholder = true;
            }

            // Calculate tile position in atlas
            int tileX = currentTile % tilesPerRow;
            int tileY = currentTile / tilesPerRow;

            // Copy pixel data to atlas buffer
            copyToAtlas(imageData, atlasBuffer, tileX, tileY);

            // Free STB image buffer immediately after copying (only for STB-loaded images, not placeholders)
            if (!isPlaceholder) {
                STBImage.stbi_image_free(imageData);
            }

            // Calculate normalized UV coordinates
            float u0 = (float) (tileX * tileSize) / atlasWidth;
            float v0 = (float) (tileY * tileSize) / atlasHeight;
            float u1 = (float) ((tileX + 1) * tileSize) / atlasWidth;
            float v1 = (float) ((tileY + 1) * tileSize) / atlasHeight;

            // Store atlas position
            texturePositions.put(textureName, new AtlasPosition(tileX, tileY, u0, v0, u1, v1));

            currentTile++;
        }

        // Generate OpenGL texture
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);

        // Upload atlas to GPU
        atlasBuffer.flip();
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, atlasWidth, atlasHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, atlasBuffer);

        // Set texture parameters for pixelated look
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glBindTexture(GL_TEXTURE_2D, 0);

        System.out.println("Texture atlas created: " + atlasWidth + "x" + atlasHeight + 
                           " (" + texturePositions.size() + " textures)");
    }

    /**
     * Loads an image from resources using STB.
     *
     * @param resourcePath path to the resource
     * @return ByteBuffer containing RGBA pixel data, or null if loading fails
     */
    private ByteBuffer loadImageFromResource(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("Resource not found: " + resourcePath);
                return null;
            }

            byte[] imageBytes = is.readAllBytes();
            ByteBuffer imageBuffer = BufferUtils.createByteBuffer(imageBytes.length);
            imageBuffer.put(imageBytes);
            imageBuffer.flip();

            IntBuffer width = BufferUtils.createIntBuffer(1);
            IntBuffer height = BufferUtils.createIntBuffer(1);
            IntBuffer channels = BufferUtils.createIntBuffer(1);

            ByteBuffer image = STBImage.stbi_load_from_memory(imageBuffer, width, height, channels, 4);

            if (image == null) {
                System.err.println("STB failed to load image: " + resourcePath + " - " + STBImage.stbi_failure_reason());
                return null;
            }

            // Validate dimensions
            if (width.get(0) != tileSize || height.get(0) != tileSize) {
                System.err.println("Warning: Texture " + resourcePath + " is " + width.get(0) + "x" + height.get(0) + 
                                   ", expected " + tileSize + "x" + tileSize);
            }

            return image;

        } catch (IOException e) {
            System.err.println("Failed to read resource: " + resourcePath);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a placeholder texture (magenta checkerboard pattern).
     *
     * @return ByteBuffer containing placeholder texture data
     */
    private ByteBuffer createPlaceholderTexture() {
        ByteBuffer buffer = BufferUtils.createByteBuffer(tileSize * tileSize * 4);
        for (int y = 0; y < tileSize; y++) {
            for (int x = 0; x < tileSize; x++) {
                boolean checker = ((x / 4) + (y / 4)) % 2 == 0;
                buffer.put((byte) (checker ? 255 : 0));   // R
                buffer.put((byte) 0);                      // G
                buffer.put((byte) (checker ? 255 : 0));   // B
                buffer.put((byte) 255);                    // A
            }
        }
        buffer.flip();
        return buffer;
    }

    /**
     * Copies a tile texture to the atlas buffer.
     *
     * @param source      source texture data (32×32 RGBA)
     * @param destination atlas buffer
     * @param tileX       tile X coordinate in atlas
     * @param tileY       tile Y coordinate in atlas
     */
    private void copyToAtlas(ByteBuffer source, ByteBuffer destination, int tileX, int tileY) {
        int pixelX = tileX * tileSize;
        int pixelY = tileY * tileSize;

        for (int y = 0; y < tileSize; y++) {
            for (int x = 0; x < tileSize; x++) {
                int srcIndex = (y * tileSize + x) * 4;
                int dstIndex = ((pixelY + y) * atlasWidth + (pixelX + x)) * 4;

                if (srcIndex + 3 < source.capacity() && dstIndex + 3 < destination.capacity()) {
                    destination.put(dstIndex, source.get(srcIndex));         // R
                    destination.put(dstIndex + 1, source.get(srcIndex + 1)); // G
                    destination.put(dstIndex + 2, source.get(srcIndex + 2)); // B
                    destination.put(dstIndex + 3, source.get(srcIndex + 3)); // A
                }
            }
        }
    }

    /**
     * Gets the UV coordinates for a texture.
     *
     * @param textureName the texture name
     * @return the atlas position with UV coordinates
     * @throws IllegalArgumentException if texture not found
     */
    public AtlasPosition getUVCoordinates(String textureName) {
        AtlasPosition pos = texturePositions.get(textureName);
        if (pos == null) {
            throw new IllegalArgumentException("Texture not found in atlas: " + textureName);
        }
        return pos;
    }

    /**
     * Updates block texture references with atlas coordinates.
     *
     * @param registry the block registry
     */
    public void updateBlockTextures(BlockRegistry registry) {
        for (Block block : registry.getAllBlocks()) {
            for (BlockFace face : BlockFace.values()) {
                Block.TextureReference texRef = block.getTextureReference(face);
                if (texRef.textureName != null && !texRef.textureName.isEmpty()) {
                    AtlasPosition pos = texturePositions.get(texRef.textureName);
                    if (pos != null) {
                        block.setAtlasCoordinates(face, pos.x, pos.y);
                    }
                }
            }
        }
    }

    /**
     * Binds the texture atlas to GL_TEXTURE0.
     */
    public void bind() {
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    /**
     * Unbinds the texture atlas.
     */
    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    /**
     * @return the OpenGL texture ID
     */
    public int getTextureId() {
        return textureId;
    }

    /**
     * Cleans up OpenGL resources.
     */
    public void cleanup() {
        if (textureId != -1) {
            glDeleteTextures(textureId);
            textureId = -1;
        }
    }

    /**
     * Creates a default texture atlas with textures from resources/textures/.
     *
     * @return a loaded TextureAtlas
     */
    public static TextureAtlas createDefault() {
        TextureAtlas atlas = new TextureAtlas(512, 512, 32);

        Map<String, String> textureFiles = new LinkedHashMap<>();
        // Phase 6 textures
        textureFiles.put("stone", "/textures/stone.png");
        textureFiles.put("grass_top", "/textures/grass_top.png");
        textureFiles.put("grass_side", "/textures/grass_side.png");
        textureFiles.put("dirt", "/textures/dirt.png");
        textureFiles.put("sand", "/textures/sand.png");
        textureFiles.put("glass", "/textures/glass.png");
        
        // Phase 7 biome textures
        textureFiles.put("snow", "/textures/snow.png");
        textureFiles.put("jungle_grass_top", "/textures/jungle_grass_top.png");
        textureFiles.put("jungle_grass_side", "/textures/jungle_grass_side.png");
        textureFiles.put("sandstone", "/textures/sandstone.png");
        textureFiles.put("mountain_stone", "/textures/mountain_stone.png");
        textureFiles.put("desert_sand", "/textures/desert_sand.png");
        textureFiles.put("ice", "/textures/ice.png");
        textureFiles.put("cactus_side", "/textures/cactus_side.png");
        textureFiles.put("cactus_top", "/textures/cactus_top.png");
        textureFiles.put("jungle_log_side", "/textures/jungle_log_side.png");
        textureFiles.put("jungle_log_top", "/textures/jungle_log_top.png");

        atlas.load(textureFiles);
        return atlas;
    }

    /**
     * Represents a texture's position in the atlas with UV coordinates.
     */
    public static class AtlasPosition {
        public final int x;
        public final int y;
        public final float u0;
        public final float v0;
        public final float u1;
        public final float v1;

        public AtlasPosition(int x, int y, float u0, float v0, float u1, float v1) {
            this.x = x;
            this.y = y;
            this.u0 = u0;
            this.v0 = v0;
            this.u1 = u1;
            this.v1 = v1;
        }

        @Override
        public String toString() {
            return "AtlasPosition{" +
                    "tile=(" + x + "," + y + ")" +
                    ", uv=[" + u0 + "," + v0 + "," + u1 + "," + v1 + "]" +
                    '}';
        }
    }
}
