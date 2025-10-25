#pragma once

#include "poorcraft/core/GameState.h"

namespace poorcraft::ui {

class PauseMenuUI {
public:
    explicit PauseMenuUI(core::GameStateManager& gameStateManager);

    void render();

private:
    core::GameStateManager& m_gameStateManager;
};

} // namespace poorcraft::ui
