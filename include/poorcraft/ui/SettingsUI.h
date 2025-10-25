#pragma once

#include <cstdint>

namespace poorcraft::core {
class GameStateManager;
}

namespace poorcraft::rendering {
class Renderer;
}

namespace poorcraft::world {
class ChunkManager;
}

namespace poorcraft::ui {

class SettingsUI {
public:
    SettingsUI(core::GameStateManager& gameStateManager, rendering::Renderer& renderer, world::ChunkManager& chunkManager);

    void loadSettings();
    void render();

private:
    void applySettings();

    core::GameStateManager& m_gameStateManager;
    rendering::Renderer& m_renderer;
    world::ChunkManager& m_chunkManager;

    int m_renderDistance{0};
    int m_pendingRenderDistance{0};
    bool m_pendingVSync{false};
    bool m_settingsLoaded{false};
};

} // namespace poorcraft::ui
