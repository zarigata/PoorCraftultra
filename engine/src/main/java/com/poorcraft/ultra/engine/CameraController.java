package com.poorcraft.ultra.engine;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsRayTestResult;
import com.jme3.input.InputManager;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.poorcraft.ultra.engine.api.InputControllable;
import com.poorcraft.ultra.engine.api.InputMappings;
import com.poorcraft.ultra.engine.PhysicsManager;
import com.poorcraft.ultra.shared.Logger;
import com.poorcraft.ultra.ui.MainMenuState;

import java.util.ArrayList;
import java.util.List;

public final class CameraController extends BaseAppState implements ActionListener, AnalogListener, InputControllable {

    private static final Logger logger = Logger.getLogger(CameraController.class);

    public static final float MOVEMENT_SPEED = 5.0f;
    public static final float MOUSE_SENSITIVITY = 0.5f;
    public static final float MAX_PITCH = FastMath.HALF_PI - 0.01f;
    private static final float EYE_HEIGHT = 1.6f;
    private static final float RAY_START_OFFSET = 3.0f;
    private static final float RAY_END_OFFSET = 10.0f;

    private Camera cam;
    private InputManager inputManager;
    private final Vector3f walkDirection = new Vector3f();

    private boolean moveForward;
    private boolean moveBackward;
    private boolean moveLeft;
    private boolean moveRight;
    private boolean moveUp;
    private boolean moveDown;

    private float yaw;
    private float pitch;

    private boolean inputEnabled = true;

    @Override
    protected void initialize(Application app) {
        cam = app.getCamera();
        inputManager = app.getInputManager();

        Vector3f direction = cam.getDirection();
        yaw = FastMath.atan2(direction.x, direction.z);
        pitch = FastMath.asin(FastMath.clamp(direction.y, -1f, 1f));

        inputEnabled = true;

        logger.info("Camera controller initialized");
    }

    @Override
    protected void cleanup(Application app) {
        clearMovementFlags();
        logger.info("Camera controller cleaned up");
    }

    @Override
    protected void onEnable() {
        inputEnabled = true;
        if (inputManager != null) {
            inputManager.setCursorVisible(false);
            // jME relies on relative mouse deltas; there is no programmatic cursor centering here.
        }
    }

    @Override
    protected void onDisable() {
        inputEnabled = false;
        clearMovementFlags();
    }

    @Override
    public void update(float tpf) {
        if (!inputEnabled || cam == null) {
            return;
        }

        AppStateManager manager = getStateManager();
        if (manager != null) {
            MainMenuState menuState = manager.getState(MainMenuState.class);
            if (menuState != null && menuState.isEnabled()) {
                return;
            }
        }

        walkDirection.set(0f, 0f, 0f);

        if (moveForward) {
            walkDirection.addLocal(cam.getDirection());
        }
        if (moveBackward) {
            walkDirection.subtractLocal(cam.getDirection());
        }
        if (moveLeft) {
            walkDirection.addLocal(cam.getLeft());
        }
        if (moveRight) {
            walkDirection.subtractLocal(cam.getLeft());
        }
        if (moveUp) {
            walkDirection.addLocal(Vector3f.UNIT_Y);
        }
        if (moveDown) {
            walkDirection.subtractLocal(Vector3f.UNIT_Y);
        }

        Vector3f currentLocation = cam.getLocation().clone();
        Vector3f desiredLocation = currentLocation;

        Vector3f movement = null;
        if (walkDirection.lengthSquared() > 0f) {
            movement = walkDirection.normalize().mult(MOVEMENT_SPEED * tpf);
            desiredLocation = currentLocation.add(movement);
        } else {
            desiredLocation = currentLocation.clone();
        }

        desiredLocation = applyGroundCollision(currentLocation, desiredLocation, movement);

        if (movement != null || !desiredLocation.equals(currentLocation)) {
            cam.setLocation(desiredLocation);
        }

        applyRotation();
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        if (!inputEnabled) {
            return;
        }

        switch (name) {
            case InputMappings.MOUSE_X_POS -> yaw += value * MOUSE_SENSITIVITY;
            case InputMappings.MOUSE_X_NEG -> yaw -= value * MOUSE_SENSITIVITY;
            case InputMappings.MOUSE_Y_POS -> pitch -= value * MOUSE_SENSITIVITY;
            case InputMappings.MOUSE_Y_NEG -> pitch += value * MOUSE_SENSITIVITY;
            default -> {
                return;
            }
        }

        pitch = FastMath.clamp(pitch, -MAX_PITCH, MAX_PITCH);

        applyRotation();
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!inputEnabled) {
            return;
        }

        switch (name) {
            case InputMappings.MOVE_FORWARD -> moveForward = isPressed;
            case InputMappings.MOVE_BACKWARD -> moveBackward = isPressed;
            case InputMappings.MOVE_LEFT -> moveLeft = isPressed;
            case InputMappings.MOVE_RIGHT -> moveRight = isPressed;
            case InputMappings.MOVE_UP -> moveUp = isPressed;
            case InputMappings.MOVE_DOWN -> moveDown = isPressed;
            default -> {
            }
        }
    }

    @Override
    public void setInputEnabled(boolean enabled) {
        inputEnabled = enabled;
        if (!enabled) {
            clearMovementFlags();
        }
    }

    private void clearMovementFlags() {
        moveForward = false;
        moveBackward = false;
        moveLeft = false;
        moveRight = false;
        moveUp = false;
        moveDown = false;
    }

    private void applyRotation() {
        if (cam == null) {
            return;
        }
        Quaternion rotation = new Quaternion();
        rotation.fromAngles(pitch, yaw, 0f);
        cam.setRotation(rotation);
    }

    private Vector3f applyGroundCollision(Vector3f currentLocation, Vector3f desiredLocation, Vector3f movement) {
        if (!PhysicsManager.isInitializedManager()) {
            return desiredLocation;
        }

        PhysicsSpace space;
        try {
            space = PhysicsManager.getInstance().getPhysicsSpace();
        } catch (IllegalStateException exception) {
            return desiredLocation;
        }

        float baseY = Math.max(currentLocation.y, desiredLocation.y);
        Vector3f rayStart = new Vector3f(desiredLocation.x, baseY + RAY_START_OFFSET, desiredLocation.z);
        Vector3f rayEnd = new Vector3f(desiredLocation.x, baseY - RAY_END_OFFSET, desiredLocation.z);

        List<PhysicsRayTestResult> results = new ArrayList<>();
        space.rayTest(rayStart, rayEnd, results);
        if (results.isEmpty()) {
            return desiredLocation;
        }

        PhysicsRayTestResult closestResult = null;
        float closestFraction = Float.POSITIVE_INFINITY;
        for (PhysicsRayTestResult result : results) {
            float fraction = result.getHitFraction();
            if (fraction < closestFraction) {
                closestFraction = fraction;
                closestResult = result;
            }
        }

        if (closestResult == null || !Float.isFinite(closestFraction)) {
            return desiredLocation;
        }

        Vector3f direction = rayEnd.subtract(rayStart);
        Vector3f hitPoint = rayStart.add(direction.mult(closestFraction));
        float minimumY = hitPoint.y + EYE_HEIGHT;

        if (desiredLocation.y < minimumY) {
            if (movement != null && movement.y < 0f) {
                movement.y = 0f;
            }
            if (walkDirection.y < 0f) {
                walkDirection.y = 0f;
            }
            return new Vector3f(desiredLocation.x, minimumY, desiredLocation.z);
        }

        return desiredLocation;
    }
}
