package com.poorcraft.ultra.engine;

import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.poorcraft.ultra.shared.Constants;
import com.poorcraft.ultra.shared.config.Config;
import com.poorcraft.ultra.shared.util.Logger;

public class PoorcraftApp extends SimpleApplication {

    private static final org.slf4j.Logger LOG = Logger.getLogger(PoorcraftApp.class);

    private final Config config;
    private final boolean devMode;

    public PoorcraftApp(Config config, boolean devMode) {
        this.config = config;
        this.devMode = devMode;
    }

    @Override
    public void simpleInitApp() {
        LOG.info("Initializing {} engine", Constants.GAME_NAME);
        initScene();
        cam.setLocation(new Vector3f(3, 3, 6));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
        flyCam.setMoveSpeed(10f);
        LOG.info("jME initialized");
    }

    private void initScene() {
        Box box = new Box(1, 1, 1);
        Geometry geometry = new Geometry("phase0-box", box);
        geometry.setMaterial(EngineMaterials.createBasicMaterial(assetManager, ColorRGBA.Blue));
        rootNode.attachChild(geometry);

        AmbientLight light = new AmbientLight();
        light.setColor(ColorRGBA.White.mult(1.3f));
        rootNode.addLight(light);
    }

    public boolean isDevMode() {
        return devMode;
    }
}
