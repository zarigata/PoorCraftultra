#pragma once

namespace poorcraft::core {
class Timer;
class Inventory;
}

namespace poorcraft::world {
class ChunkManager;
}

namespace poorcraft::ui {

class HUD {
public:
    HUD(core::Timer& timer, core::Inventory& inventory, world::ChunkManager& chunkManager);

    void render();

    void setPaused(bool paused) { m_isPaused = paused; }

private:
    void renderCrosshair();
    void renderFPSOverlay();
    void renderHotbar();

    core::Timer& m_timer;
    core::Inventory& m_inventory;
    world::ChunkManager& m_chunkManager;
    bool m_isPaused{false};
};

} // namespace poorcraft::ui
