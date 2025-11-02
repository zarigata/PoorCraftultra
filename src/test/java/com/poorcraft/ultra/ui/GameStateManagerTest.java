package com.poorcraft.ultra.ui;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.InputManager;
import com.jme3.input.TouchInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.InputListener;
import com.jme3.input.controls.Trigger;
import com.poorcraft.ultra.app.ClientConfig;
import com.poorcraft.ultra.app.ServiceHub;
import com.poorcraft.ultra.voxel.ChunkManager;
import com.poorcraft.ultra.world.WorldSaveManager;
import com.poorcraft.ultra.ui.TestInputDevices.StubKeyInput;
import com.poorcraft.ultra.ui.TestInputDevices.StubMouseInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameStateManagerTest {

    private RecordingInputManager inputManager;
    private TestApp application;
    private RecordingAppStateManager stateManager;
    private ServiceHub serviceHub;
    private RecordingChunkManager chunkManager;
    private WorldSaveManager worldSaveManager;
    private GameStateManager manager;

    private StubState mainMenuState;
    private StubState inGameState;
    private StubState pauseMenuState;

    @BeforeEach
    void setUp() throws Exception {
        inputManager = new RecordingInputManager();
        application = new TestApp(inputManager);
        stateManager = application.getRecordingStateManager();

        serviceHub = new ServiceHub(ClientConfig.defaults());
        chunkManager = new RecordingChunkManager();
        worldSaveManager = new WorldSaveManager();
        serviceHub.register(ChunkManager.class, chunkManager);
        serviceHub.register(WorldSaveManager.class, worldSaveManager);

        manager = new GameStateManager();
        serviceHub.register(GameStateManager.class, manager);

        manager.init(application, serviceHub);

        // Replace the initialized states with our stubs
        mainMenuState = replaceState("mainMenuState", new StubState("MainMenu"), true);
        inGameState = replaceState("inGameState", new StubState("InGame"), false);
        pauseMenuState = replaceState("pauseMenuState", new StubState("PauseMenu"), false);

        stateManager.clearHistory();
        inputManager.resetHistory();
    }

    @Test
    void initialisesMainMenuAndShowsCursor() {
        assertEquals(GameStateManager.GameState.MAIN_MENU, manager.getCurrentState());
        assertTrue(mainMenuState.isAttachedFlag());
        assertFalse(inGameState.isAttachedFlag());
        assertFalse(pauseMenuState.isAttachedFlag());
        assertTrue(inputManager.lastCursorVisible);
    }

    @Test
    void startGameTransitionsAndHidesCursor() {
        manager.startGame();

        assertEquals(GameStateManager.GameState.IN_GAME, manager.getCurrentState());
        assertFalse(mainMenuState.isAttachedFlag());
        assertTrue(inGameState.isAttachedFlag());
        assertTrue(inGameState.isEnabledFlag());
        assertEquals(1, inGameState.getEnableCallCount());
        assertFalse(inputManager.lastCursorVisible);
    }

    @Test
    void pauseGameAttachesPauseStateAndShowsCursor() {
        manager.startGame();
        manager.pauseGame();

        assertEquals(GameStateManager.GameState.PAUSED, manager.getCurrentState());
        assertTrue(pauseMenuState.isAttachedFlag());
        assertFalse(inGameState.isEnabledFlag());
        assertEquals(1, inGameState.getDisableCallCount());
        assertTrue(inputManager.lastCursorVisible);
    }

    @Test
    void resumeGameDetachesPauseStateAndHidesCursor() {
        manager.startGame();
        manager.pauseGame();
        manager.resumeGame();

        assertEquals(GameStateManager.GameState.IN_GAME, manager.getCurrentState());
        assertFalse(pauseMenuState.isAttachedFlag());
        assertTrue(inGameState.isEnabledFlag());
        assertEquals(2, inGameState.getEnableCallCount());
        assertFalse(inputManager.lastCursorVisible);
    }

    @Test
    void exitToMainMenuDetachesGameplayStatesAndShowsCursor() {
        manager.startGame();
        manager.pauseGame();
        manager.exitToMainMenu();

        assertEquals(GameStateManager.GameState.MAIN_MENU, manager.getCurrentState());
        assertTrue(mainMenuState.isAttachedFlag());
        assertFalse(inGameState.isAttachedFlag());
        assertFalse(pauseMenuState.isAttachedFlag());
        assertTrue(inputManager.lastCursorVisible);
        assertTrue(chunkManager.wasSaveAllCalled());
        assertTrue(worldSaveManager.isSavedOnShutdown());
    }

    private StubState replaceState(String fieldName, StubState replacement, boolean attach) throws Exception {
        Field field = GameStateManager.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        BaseAppState existing = (BaseAppState) field.get(manager);
        if (existing != null) {
            stateManager.removeSilently(existing);
        }
        field.set(manager, replacement);
        if (attach) {
            stateManager.attachSilently(replacement, true);
        }
        return replacement;
    }

    private static final class TestApp extends SimpleApplication {
        private final RecordingAppStateManager recordingStateManager;
        private final RecordingInputManager inputManager;

        TestApp(RecordingInputManager inputManager) {
            this.inputManager = inputManager;
            this.recordingStateManager = new RecordingAppStateManager(this);
        }

        @Override
        public void simpleInitApp() {
            // no-op
        }

        @Override
        public AppStateManager getStateManager() {
            return recordingStateManager;
        }

        @Override
        public InputManager getInputManager() {
            return inputManager;
        }

        RecordingAppStateManager getRecordingStateManager() {
            return recordingStateManager;
        }
    }

    private static final class RecordingInputManager extends InputManager {
        private boolean lastCursorVisible = true;
        private final Set<String> mappings = new HashSet<>();
        private final Set<String> actionMappingsWithListeners = new HashSet<>();
        private final Set<String> analogMappingsWithListeners = new HashSet<>();

        RecordingInputManager() {
            super(new StubMouseInput(), new StubKeyInput(), null, (TouchInput) null);
        }

        @Override
        public void setCursorVisible(boolean visible) {
            lastCursorVisible = visible;
        }

        @Override
        public void addMapping(String mappingName, Trigger... triggers) {
            mappings.add(mappingName);
        }

        @Override
        public void deleteMapping(String mappingName) {
            mappings.remove(mappingName);
            actionMappingsWithListeners.remove(mappingName);
            analogMappingsWithListeners.remove(mappingName);
        }

        @Override
        public boolean hasMapping(String mappingName) {
            return mappings.contains(mappingName);
        }

        @Override
        public void addListener(InputListener listener, String... mappingNames) {
            if (listener instanceof ActionListener) {
                actionMappingsWithListeners.addAll(Arrays.asList(mappingNames));
            }
            if (listener instanceof AnalogListener) {
                analogMappingsWithListeners.addAll(Arrays.asList(mappingNames));
            }
        }

        @Override
        public void removeListener(InputListener listener) {
            if (listener instanceof ActionListener) {
                actionMappingsWithListeners.clear();
            }
            if (listener instanceof AnalogListener) {
                analogMappingsWithListeners.clear();
            }
        }

        void resetHistory() {
            actionMappingsWithListeners.clear();
            analogMappingsWithListeners.clear();
        }
    }

    private static final class RecordingAppStateManager extends AppStateManager {
        private final SimpleApplication application;
        private final Set<BaseAppState> activeStates = new LinkedHashSet<>();
        private final Set<BaseAppState> attachHistory = new LinkedHashSet<>();
        private final Set<BaseAppState> detachHistory = new LinkedHashSet<>();
        private final Set<BaseAppState> enableHistory = new LinkedHashSet<>();
        private final Set<BaseAppState> disableHistory = new LinkedHashSet<>();

        RecordingAppStateManager(SimpleApplication application) {
            super(application);
            this.application = application;
        }

        @Override
        public boolean attach(AppState state) {
            if (!(state instanceof BaseAppState base)) {
                return false;
            }
            if (!activeStates.add(base)) {
                return false;
            }
            attachHistory.add(base);
            if (base instanceof StubState stub) {
                stub.setManager(this);
            }
            base.initialize(this, application);
            return true;
        }

        @Override
        public boolean detach(AppState state) {
            if (!(state instanceof BaseAppState base)) {
                return false;
            }
            if (!activeStates.remove(base)) {
                return false;
            }
            detachHistory.add(base);
            if (base instanceof StubState stub) {
                stub.setManager(this);
            }
            if (base.isEnabled()) {
                base.setEnabled(false);
            }
            base.cleanup();
            return true;
        }

        @Override
        public boolean hasState(AppState state) {
            return state instanceof BaseAppState base && activeStates.contains(base);
        }

        void recordEnabled(BaseAppState state) {
            enableHistory.add(state);
        }

        void recordDisabled(BaseAppState state) {
            disableHistory.add(state);
        }

        void clearHistory() {
            attachHistory.clear();
            detachHistory.clear();
            enableHistory.clear();
            disableHistory.clear();
        }

        void removeSilently(BaseAppState state) {
            detach(state);
        }

        void attachSilently(StubState state, boolean enable) {
            activeStates.add(state);
            state.setManager(this);
            state.initialize(this, application);
            if (enable) {
                state.setEnabled(true);
            }
        }
    }

    private static final class StubState extends BaseAppState {
        private final String name;
        private boolean attached;
        private boolean enabled;
        private int enableCalls;
        private int disableCalls;
        private RecordingAppStateManager manager;

        StubState(String name) {
            this.name = name;
        }

        void setManager(RecordingAppStateManager manager) {
            this.manager = manager;
        }

        @Override
        protected void initialize(Application app) {
            attached = true;
        }

        @Override
        protected void cleanup(Application app) {
            attached = false;
            enabled = false;
        }

        @Override
        protected void onEnable() {
            enabled = true;
            enableCalls++;
            if (manager != null) {
                manager.recordEnabled(this);
            }
        }

        @Override
        protected void onDisable() {
            enabled = false;
            disableCalls++;
            if (manager != null) {
                manager.recordDisabled(this);
            }
        }

        boolean isAttachedFlag() {
            return attached;
        }

        boolean isEnabledFlag() {
            return enabled;
        }

        int getEnableCallCount() {
            return enableCalls;
        }

        int getDisableCallCount() {
            return disableCalls;
        }

        @Override
        public String toString() {
            return "StubState{" + name + '}';
        }
    }

    private static final class RecordingChunkManager extends ChunkManager {
        private boolean saveAllCalled;

        @Override
        public void saveAll() {
            saveAllCalled = true;
        }

        boolean wasSaveAllCalled() {
            return saveAllCalled;
        }
    }
}
