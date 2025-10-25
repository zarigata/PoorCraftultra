#pragma once

#include <functional>
#include <iosfwd>

namespace poorcraft::core {

enum class GameState {
    MainMenu,
    Loading,
    Playing,
    Paused,
    Settings,
    Quitting
};

class GameStateManager {
public:
    using StateChangeCallback = std::function<void(GameState previous, GameState current)>;

    GameStateManager();

    GameState getCurrentState() const noexcept;

    void setState(GameState newState);

    void pushState(GameState state);

    void popState();

    void update();

    bool shouldQuit() const noexcept;

    void setOnStateChangeCallback(StateChangeCallback callback);

private:
    bool isTransitionAllowed(GameState from, GameState to) const noexcept;

    void emitStateChange(GameState previous, GameState current);

    GameState m_currentState;
    GameState m_previousState;
    StateChangeCallback m_onStateChange;
};

std::ostream& operator<<(std::ostream& out, GameState state);

} // namespace poorcraft::core
