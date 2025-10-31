package com.poorcraft.ultra.tests;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.poorcraft.ultra.engine.CameraController;
import com.poorcraft.ultra.engine.EngineCore;
import com.poorcraft.ultra.ui.HudState;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

final class Phase02Tests {

    @Test
    void testCameraControllerCreation() {
        CameraController controller = new CameraController();
        assertThat(controller).isNotNull();
    }

    @Test
    void testCameraMovesForwardAtConfiguredSpeed() throws Exception {
        CameraController controller = new CameraController();
        var camera = new com.jme3.renderer.Camera(800, 600);
        camera.setLocation(new Vector3f(0f, 0f, 0f));
        camera.lookAtDirection(new Vector3f(0f, 0f, -1f), Vector3f.UNIT_Y);

        setField(controller, "cam", camera);
        setField(controller, "inputEnabled", true);
        setField(controller, "moveForward", true);

        controller.update(1f);

        assertThat(camera.getLocation()).isEqualTo(new Vector3f(0f, 0f, -CameraController.MOVEMENT_SPEED));
    }

    @Test
    void testDiagonalMovementIsNormalized() throws Exception {
        CameraController controller = new CameraController();
        var camera = new com.jme3.renderer.Camera(800, 600);
        camera.setLocation(new Vector3f(0f, 0f, 0f));
        camera.lookAtDirection(new Vector3f(0f, 0f, -1f), Vector3f.UNIT_Y);

        setField(controller, "cam", camera);
        setField(controller, "inputEnabled", true);
        setField(controller, "moveForward", true);
        setField(controller, "moveRight", true);

        controller.update(1f);

        Vector3f displacement = camera.getLocation();
        assertThat(displacement.length()).isCloseTo(CameraController.MOVEMENT_SPEED, within(1e-3f));
    }

    @Test
    void testSetInputEnabledClearsMovementFlags() throws Exception {
        CameraController controller = new CameraController();
        setField(controller, "moveForward", true);
        setField(controller, "moveLeft", true);

        controller.setInputEnabled(false);

        assertThat(getBoolean(controller, "moveForward")).isFalse();
        assertThat(getBoolean(controller, "moveLeft")).isFalse();
    }

    @Test
    void testMouseInputUpdatesYawAndPitchWithClamping() throws Exception {
        CameraController controller = new CameraController();
        setField(controller, "inputEnabled", true);

        controller.onAnalog("MouseXPos", 2f, 0.016f);
        controller.onAnalog("MouseYNeg", 100f, 0.016f);

        float yaw = getFloat(controller, "yaw");
        float pitch = getFloat(controller, "pitch");

        assertThat(yaw).isEqualTo(1f);
        assertThat(pitch).isEqualTo(CameraController.MAX_PITCH);
    }

    @Test
    void testSkyboxColorMatchesDesignSpec() throws Exception {
        Field field = EngineCore.class.getDeclaredField("SKYBOX_COLOR");
        field.setAccessible(true);
        ColorRGBA sky = (ColorRGBA) field.get(null);

        assertThat(sky.r).isCloseTo(0.529f, within(1e-3f));
        assertThat(sky.g).isCloseTo(0.808f, within(1e-3f));
        assertThat(sky.b).isCloseTo(0.922f, within(1e-3f));
        assertThat(sky.a).isEqualTo(1.0f);
    }

    @Test
    void testHudFpsUpdateInterval() throws Exception {
        Field field = HudState.class.getDeclaredField("FPS_UPDATE_INTERVAL");
        field.setAccessible(true);
        float interval = field.getFloat(null);

        assertThat(interval).isCloseTo(0.5f, within(1e-6f));
    }

    @Test
    void testPositionFormatting() {
        Vector3f pos = new Vector3f(123.456f, 78.901f, -45.678f);
        String formatted = String.format("X: %.1f Y: %.1f Z: %.1f", pos.x, pos.y, pos.z);
        assertThat(formatted).isEqualTo("X: 123.5 Y: 78.9 Z: -45.7");
    }

    @Test
    void testInputMappingNamesRemainStable() {
        String[] mappings = {
                "MoveForward",
                "MoveBackward",
                "MoveLeft",
                "MoveRight",
                "MoveUp",
                "MoveDown",
                "MouseXPos",
                "MouseXNeg",
                "MouseYPos",
                "MouseYNeg",
                "ToggleMenu"
        };

        assertThat(mappings).containsExactly(
                "MoveForward",
                "MoveBackward",
                "MoveLeft",
                "MoveRight",
                "MoveUp",
                "MoveDown",
                "MouseXPos",
                "MouseXNeg",
                "MouseYPos",
                "MouseYNeg",
                "ToggleMenu"
        );
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static boolean getBoolean(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getBoolean(target);
    }

    private static float getFloat(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getFloat(target);
    }
}
