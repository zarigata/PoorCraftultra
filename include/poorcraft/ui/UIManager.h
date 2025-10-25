#pragma once

#include "poorcraft/core/GameState.h"
#include "poorcraft/rendering/Renderer.h"

#include <SDL2/SDL.h>

#include <chrono>
#include <memory>

namespace poorcraft::core {
class Input;
class Timer;
class Inventory;
}

namespace poorcraft::world {
class ChunkManager;
}

namespace poorcraft::ui {
class MainMenuUI;
class PauseMenuUI;
class SettingsUI;
class HUD;

class UIManager {
public:
    UIManager(
        core::GameStateManager& gameStateManager,
        rendering::Renderer& renderer,
        core::Input& input,
        core::Timer& timer,
        core::Inventory& inventory,
        world::ChunkManager& chunkManager);

    void initialize();
    void shutdown();

    void processEvent(const SDL_Event& event);
    void render();

    bool wantsCaptureMouse() const;
    bool wantsCaptureKeyboard() const;

private:
    void handleStateChange(core::GameState previous, core::GameState current);
    void updateLoadingState();

    core::GameStateManager& m_gameStateManager;
    rendering::Renderer& m_renderer;
    core::Input& m_input;
    core::Timer& m_timer;
    core::Inventory& m_inventory;
    world::ChunkManager& m_chunkManager;

    std::unique_ptr<MainMenuUI> m_mainMenuUI;
    std::unique_ptr<PauseMenuUI> m_pauseMenuUI;
    std::unique_ptr<SettingsUI> m_settingsUI;
    std::unique_ptr<HUD> m_hud;

    bool m_uiInitialized{false};
    bool m_loadingRequested{false};
    std::chrono::steady_clock::time_point m_loadingStart{};
    core::GameState m_previousState{core::GameState::MainMenu};
};

} // namespace poorcraft::ui
