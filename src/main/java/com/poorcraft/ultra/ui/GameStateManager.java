package com.poorcraft.ultra.ui;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppStateManager;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.InputManager;
import com.poorcraft.ultra.app.ServiceHub;
import com.poorcraft.ultra.voxel.ChunkManager;
import com.poorcraft.ultra.world.WorldSaveManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralises game state transitions between main menu, in-game, and pause states.
 */
public class GameStateManager {
    private static final Logger logger = LoggerFactory.getLogger(GameStateManager.class);

    public enum GameState {
        MAIN_MENU,
        IN_GAME,
        PAUSED
    }

    private SimpleApplication application;
    private AppStateManager stateManager;
    private InputManager inputManager;
    private ServiceHub serviceHub;

    private GameState currentState = GameState.MAIN_MENU;

    private MainMenuState mainMenuState;
    private InGameState inGameState;
    private PauseMenuState pauseMenuState;

    public void init(SimpleApplication app, ServiceHub hub) {
        this.application = app;
        this.stateManager = app.getStateManager();
        this.inputManager = app.getInputManager();
        this.serviceHub = hub;

        mainMenuState = new MainMenuState(serviceHub);
        inGameState = new InGameState(serviceHub);
        pauseMenuState = new PauseMenuState(serviceHub);

        attachState(mainMenuState);
        currentState = GameState.MAIN_MENU;
        logger.info("GameStateManager initialised - current state MAIN_MENU");
    }

    public void enterMainMenu() {
        switch (currentState) {
            case IN_GAME -> detachState(inGameState);
            case PAUSED -> {
                detachState(pauseMenuState);
                detachState(inGameState);
            }
            default -> {
            }
        }

        attachState(mainMenuState);
        showCursor(true);
        GameState previousState = currentState;
        currentState = GameState.MAIN_MENU;
        logger.info("State transition: {} -> MAIN_MENU", previousState);
    }

    public void startGame() {
        if (currentState != GameState.MAIN_MENU) {
            logger.error("startGame() called from invalid state: {}. Expected MAIN_MENU.", currentState);
            return;
        }

        detachState(mainMenuState);

        attachState(inGameState);
        enableState(inGameState);
        showCursor(false);
        GameState previousState = currentState;
        currentState = GameState.IN_GAME;
        logger.info("State transition: {} -> IN_GAME", previousState);
    }

    public void pauseGame() {
        if (currentState != GameState.IN_GAME) {
            return;
        }
        disableState(inGameState);
        attachState(pauseMenuState);
        showCursor(true);
        GameState previousState = currentState;
        currentState = GameState.PAUSED;
        logger.info("State transition: {} -> PAUSED", previousState);
    }

    public void resumeGame() {
        if (currentState != GameState.PAUSED) {
            return;
        }
        detachState(pauseMenuState);
        enableState(inGameState);
        showCursor(false);
        GameState previousState = currentState;
        currentState = GameState.IN_GAME;
        logger.info("State transition: {} -> IN_GAME (resumed)", previousState);
    }

    public void exitToMainMenu() {
        if (serviceHub != null) {
            try {
                if (serviceHub.has(WorldSaveManager.class) && serviceHub.has(ChunkManager.class)) {
                    WorldSaveManager saveManager = serviceHub.get(WorldSaveManager.class);
                    ChunkManager chunkManager = serviceHub.get(ChunkManager.class);
                    saveManager.saveAll(chunkManager);
                    saveManager.markSavedOnShutdown();
                }
            } catch (Exception e) {
                logger.error("Failed to persist world state before returning to main menu", e);
            }
        }

        detachState(pauseMenuState);
        detachState(inGameState);
        attachState(mainMenuState);
        showCursor(true);
        GameState previousState = currentState;
        currentState = GameState.MAIN_MENU;
        logger.info("State transition: {} -> MAIN_MENU (exit)", previousState);
    }

    public GameState getCurrentState() {
        return currentState;
    }

    private void attachState(BaseAppState state) {
        if (!stateManager.hasState(state)) {
            logger.debug("Attaching AppState: {}", state.getClass().getSimpleName());
            stateManager.attach(state);
        }
    }

    private void detachState(BaseAppState state) {
        if (stateManager.hasState(state)) {
            logger.debug("Detaching AppState: {}", state.getClass().getSimpleName());
            stateManager.detach(state);
        }
    }

    private void enableState(BaseAppState state) {
        if (stateManager.hasState(state)) {
            state.setEnabled(true);
        }
    }

    private void disableState(BaseAppState state) {
        if (stateManager.hasState(state)) {
            state.setEnabled(false);
        }
    }

    private void showCursor(boolean visible) {
        if (inputManager != null) {
            logger.debug("Setting cursor visible: {}", visible);
            inputManager.setCursorVisible(visible);
        }
    }
}
