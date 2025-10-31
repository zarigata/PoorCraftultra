package com.poorcraft.ultra.ui;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Label;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Development gallery for cycling through generated block textures.
 */
public final class GalleryState extends BaseAppState {

    private static final Logger logger = Logger.getLogger(GalleryState.class.getName());

    private static final float CYCLE_INTERVAL_SECONDS = 2.0f;
    private static final float TEXTURE_DISPLAY_SIZE = 256f;
    private static final int ATLAS_GRID = 8;

    private Texture2D atlasTexture;
    private Map<String, Map<String, Integer>> atlasMapping = Collections.emptyMap();
    private List<AtlasEntry> atlasEntries = Collections.emptyList();
    private int currentIndex;
    private float cycleTimer;

    private Geometry textureQuad;
    private Material textureMaterial;
    private Label infoLabel;

    @Override
    protected void initialize(Application app) {
        ensureGuiInitialized(app);
        loadAssets(app);
        if (atlasTexture == null || atlasEntries.isEmpty()) {
            setEnabled(false);
            return;
        }
        setupScene((SimpleApplication) app);
        logger.info(() -> "Gallery initialized with " + atlasEntries.size() + " atlas entries");
    }

    @Override
    protected void cleanup(Application app) {
        if (textureQuad != null) {
            textureQuad.removeFromParent();
            textureQuad = null;
        }
        if (infoLabel != null) {
            infoLabel.removeFromParent();
            infoLabel = null;
        }
        atlasTexture = null;
        textureMaterial = null;
    }

    @Override
    protected void onEnable() {
        if (textureQuad != null) {
            textureQuad.setCullHint(Spatial.CullHint.Never);
        }
        if (infoLabel != null) {
            infoLabel.setCullHint(Spatial.CullHint.Never);
        }
    }

    @Override
    protected void onDisable() {
        if (textureQuad != null) {
            textureQuad.setCullHint(Spatial.CullHint.Always);
        }
        if (infoLabel != null) {
            infoLabel.setCullHint(Spatial.CullHint.Always);
        }
    }

    @Override
    public void update(float tpf) {
        if (!isEnabled() || atlasEntries.isEmpty()) {
            return;
        }
        cycleTimer += tpf;
        if (cycleTimer < CYCLE_INTERVAL_SECONDS) {
            return;
        }
        cycleTimer = 0f;
        currentIndex = (currentIndex + 1) % atlasEntries.size();
        applyCurrentTexture();
    }

    private void ensureGuiInitialized(Application app) {
        if (!GuiGlobals.isInitialized()) {
            GuiGlobals.initialize(app.getAssetManager(), app.getInputManager(), app.getAudioRenderer(),
                    ((SimpleApplication) app).getGuiViewPort());
        }
    }

    private void loadAssets(Application app) {
        SimpleApplication simpleApplication = (SimpleApplication) app;
        simpleApplication.getAssetManager().registerLocator("assets", FileLocator.class);

        final String atlasTexturePath = "textures/blocks_atlas.png";
        try {
            atlasTexture = (Texture2D) simpleApplication.getAssetManager().loadTexture(atlasTexturePath);
        } catch (RuntimeException exception) {
            logger.log(Level.SEVERE, "Atlas texture not found. Run :tools:assets:generate first.", exception);
            atlasTexture = null;
        }

        final Path mappingPath = Paths.get("assets", "textures", "blocks_atlas.json");
        if (Files.exists(mappingPath)) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Map<String, Integer>> mapping = mapper.readValue(
                        Files.readString(mappingPath),
                        new TypeReference<Map<String, Map<String, Integer>>>() {
                        });
                atlasMapping = new LinkedHashMap<>(mapping);
                atlasEntries = flattenAtlasEntries(atlasMapping);
                currentIndex = 0;
            } catch (IOException exception) {
                logger.log(Level.SEVERE, "Failed to load blocks_atlas.json", exception);
                atlasMapping = Collections.emptyMap();
                atlasEntries = Collections.emptyList();
            }
        } else {
            logger.warning("Atlas mapping not found. Run :tools:assets:generate first.");
        }
    }

    private void setupScene(SimpleApplication app) {
        Quad quad = new Quad(TEXTURE_DISPLAY_SIZE, TEXTURE_DISPLAY_SIZE);
        quad.setBuffer(
                com.jme3.scene.VertexBuffer.Type.TexCoord,
                2,
                BufferUtils.createFloatBuffer(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f));

        textureMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        textureMaterial.setTexture("ColorMap", atlasTexture);

        textureQuad = new Geometry("GalleryQuad", quad);
        textureQuad.setMaterial(textureMaterial);
        centerQuad(app);
        app.getGuiNode().attachChild(textureQuad);

        infoLabel = new Label("");
        infoLabel.setFontSize(20f);
        infoLabel.setLocalTranslation(calculateLabelTranslation(app));
        app.getGuiNode().attachChild(infoLabel);

        applyCurrentTexture();
    }

    private void applyCurrentTexture() {
        if (textureQuad == null || atlasEntries.isEmpty()) {
            return;
        }
        AtlasEntry entry = atlasEntries.get(currentIndex);
        int atlasIndex = entry.getAtlasIndex();
        updateTextureUVs(atlasIndex);
        if (infoLabel != null) {
            infoLabel.setText(formatEntryLabel(entry));
        }
        logger.fine(() -> "Cycling to texture: " + entry.getBlockName() +
                (entry.getFaceName() != null ? " (" + entry.getFaceName() + ")" : "") +
                " (#" + atlasIndex + ")");
    }

    private void updateTextureUVs(int atlasIndex) {
        if (textureQuad == null) {
            return;
        }
        Quad quad = (Quad) textureQuad.getMesh();
        float tileSize = 1f / ATLAS_GRID;
        int row = atlasIndex / ATLAS_GRID;
        int col = atlasIndex % ATLAS_GRID;

        float u0 = col * tileSize;
        float v0 = 1f - (row + 1) * tileSize;
        float u1 = u0 + tileSize;
        float v1 = 1f - row * tileSize;

        quad.setBuffer(
                com.jme3.scene.VertexBuffer.Type.TexCoord,
                2,
                BufferUtils.createFloatBuffer(
                        u0, v1,
                        u1, v1,
                        u0, v0,
                        u1, v0));
        quad.updateBound();
    }

    private void centerQuad(SimpleApplication app) {
        if (textureQuad == null) {
            return;
        }
        float width = app.getCamera().getWidth();
        float height = app.getCamera().getHeight();
        float x = (width - TEXTURE_DISPLAY_SIZE) / 2f;
        float y = (height - TEXTURE_DISPLAY_SIZE) / 2f;
        textureQuad.setLocalTranslation(new Vector3f(x, y, 0f));
    }

    private Vector3f calculateLabelTranslation(SimpleApplication app) {
        float width = app.getCamera().getWidth();
        float x = width / 2f;
        float y = 40f;
        return new Vector3f(x, y, 0f);
    }

    private String formatEntryLabel(AtlasEntry entry) {
        String face = entry.getFaceName();
        if (face != null) {
            return String.format("Block: %s (%s) - Index: %d", entry.getBlockName(), face, entry.getAtlasIndex());
        }
        return String.format("Block: %s - Index: %d", entry.getBlockName(), entry.getAtlasIndex());
    }

    private static List<AtlasEntry> flattenAtlasEntries(Map<String, Map<String, Integer>> mapping) {
        if (mapping.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> blocks = new ArrayList<>(mapping.keySet());
        Collections.sort(blocks);

        List<AtlasEntry> entries = new ArrayList<>();
        for (String block : blocks) {
            Map<String, Integer> faces = mapping.get(block);
            if (faces == null || faces.isEmpty()) {
                continue;
            }
            List<Map.Entry<String, Integer>> sortedFaces = new ArrayList<>(faces.entrySet());
            sortedFaces.sort(Map.Entry.comparingByKey());
            for (Map.Entry<String, Integer> faceEntry : sortedFaces) {
                entries.add(new AtlasEntry(block, faceEntry.getKey(), faceEntry.getValue()));
            }
        }
        return entries;
    }

    private static final class AtlasEntry {
        private final String blockName;
        private final String faceName;
        private final int atlasIndex;

        private AtlasEntry(String blockName, String faceName, int atlasIndex) {
            this.blockName = blockName;
            this.faceName = faceName;
            this.atlasIndex = atlasIndex;
        }

        private String getBlockName() {
            return blockName;
        }

        private String getFaceName() {
            return faceName;
        }

        private int getAtlasIndex() {
            return atlasIndex;
        }
    }
}
