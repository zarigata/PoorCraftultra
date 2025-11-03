package com.poorcraft.ultra.voxel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.poorcraft.ultra.voxel.ChunkConstants.*;

/**
 * Texture atlas builder and UV coordinate provider.
 */
public class TextureAtlas {
    
    private static final Logger logger = LoggerFactory.getLogger(TextureAtlas.class);
    
    private Texture2D atlasTexture;
    private final Map<String, Integer> textureIndices = new HashMap<>();
    private final int tileSize = ATLAS_TILE_SIZE;
    private final int gridSize = ATLAS_GRID_SIZE;
    private static final float UV_EPSILON = 0.001f;
    
    /**
     * Builds texture atlas from block textures.
     */
    public void build(AssetManager assetManager) {
        logger.info("Building texture atlas...");
        
        try {
            // Load manifest
            List<String> textureFiles = loadManifest(assetManager);
            
            logger.info("Building texture atlas: {} textures", textureFiles.size());
            
            // Create atlas image
            BufferedImage atlasImage = new BufferedImage(ATLAS_SIZE, ATLAS_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = atlasImage.createGraphics();
            
            // Load and copy each texture
            int index = 0;
            for (String textureFile : textureFiles) {
                if (index >= gridSize * gridSize) {
                    logger.warn("Too many textures for atlas (max {}), skipping: {}", gridSize * gridSize, textureFile);
                    break;
                }
                
                try {
                    // Load texture
                    Texture texture = assetManager.loadTexture("blocks/" + textureFile);
                    Image image = texture.getImage();
                    
                    // Convert to BufferedImage
                    BufferedImage texImage = convertToBufferedImage(image);
                    
                    // Compute grid position
                    int tileX = index % gridSize;
                    int tileY = index / gridSize;
                    
                    // Copy to atlas
                    g.drawImage(texImage, tileX * tileSize, tileY * tileSize, tileSize, tileSize, null);
                    
                    // Store index
                    textureIndices.put(textureFile, index);
                    logger.debug("Atlas tile {}: {}", index, textureFile);
                    
                    index++;
                } catch (Exception e) {
                    logger.warn("Failed to load texture: {}", textureFile, e);
                }
            }
            
            g.dispose();
            
            // Convert to jME Texture2D
            atlasTexture = convertToTexture2D(atlasImage);
            atlasTexture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
            atlasTexture.setMagFilter(Texture.MagFilter.Nearest);
            atlasTexture.setWrap(Texture.WrapMode.EdgeClamp);
            
            logger.info("Texture atlas built: {}x{}, {} tiles", ATLAS_SIZE, ATLAS_SIZE, textureIndices.size());
            
        } catch (Exception e) {
            logger.error("Failed to build texture atlas", e);
            throw new RuntimeException("Failed to build texture atlas", e);
        }
    }
    
    /**
     * Returns UV coordinates for quad (4 corners × 2 coords).
     */
    public float[] getUVs(String textureName) {
        Integer index = textureIndices.get(textureName);
        if (index == null) {
            logger.warn("Texture not found in atlas: {}, using fallback", textureName);
            index = textureIndices.getOrDefault("stone_01.png", 0);
        }
        
        int tileX = index % gridSize;
        int tileY = index / gridSize;
        
        float u0 = (float) tileX / gridSize + UV_EPSILON;
        float u1 = (float) (tileX + 1) / gridSize - UV_EPSILON;
        float v0 = (float) tileY / gridSize + UV_EPSILON;
        float v1 = (float) (tileY + 1) / gridSize - UV_EPSILON;
        
        // Return UVs for 4 corners: bottom-left, bottom-right, top-right, top-left
        return new float[] {
            u0, v1,  // bottom-left
            u1, v1,  // bottom-right
            u1, v0,  // top-right
            u0, v0   // top-left
        };
    }
    
    public Texture2D getAtlasTexture() {
        return atlasTexture;
    }
    
    private BufferedImage convertToBufferedImage(Image jmeImage) {
        int width = jmeImage.getWidth();
        int height = jmeImage.getHeight();
        
        // Verify image format
        if (jmeImage.getFormat() != Image.Format.RGBA8) {
            logger.warn("Unexpected image format: {}. Expected RGBA8. Attempting conversion anyway.", 
                jmeImage.getFormat());
        }
        
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        ByteBuffer data = jmeImage.getData(0);
        data.rewind();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = data.get() & 0xFF;
                int g = data.get() & 0xFF;
                int b = data.get() & 0xFF;
                int a = data.get() & 0xFF;
                
                int argb = (a << 24) | (r << 16) | (g << 8) | b;
                bufferedImage.setRGB(x, y, argb);
            }
        }
        
        return bufferedImage;
    }
    
    private Texture2D convertToTexture2D(BufferedImage bufferedImage) throws IOException {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        
        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = bufferedImage.getRGB(x, y);
                buffer.put((byte) ((argb >> 16) & 0xFF)); // R
                buffer.put((byte) ((argb >> 8) & 0xFF));  // G
                buffer.put((byte) (argb & 0xFF));         // B
                buffer.put((byte) ((argb >> 24) & 0xFF)); // A
            }
        }
        
        buffer.flip();
        
        Image image = new Image(Image.Format.RGBA8, width, height, buffer, ColorSpace.sRGB);
        return new Texture2D(image);
    }
    
    /**
     * Loads manifest via AssetManager for packaged build compatibility.
     */
    private List<String> loadManifest(AssetManager assetManager) throws IOException {
        AssetKey<Object> key = new AssetKey<>("blocks/manifest.json");
        AssetInfo assetInfo = assetManager.locateAsset(key);
        
        if (assetInfo == null) {
            throw new RuntimeException("Manifest not found: blocks/manifest.json. " +
                "Ensure the manifest exists in the assets directory.");
        }
        
        List<String> textureFiles = new ArrayList<>();
        
        try (InputStream inputStream = assetInfo.openStream()) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode manifest = mapper.readTree(inputStream);
            
            JsonNode textures = manifest.get("textures");
            
            if (textures != null && textures.isArray()) {
                for (JsonNode texture : textures) {
                    String name = texture.get("name").asText();
                    textureFiles.add(name);
                }
            }
            
            logger.info("Loaded manifest via AssetManager: {} texture entries found", textureFiles.size());
        }
        
        return textureFiles;
    }
}
