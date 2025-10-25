#include "poorcraft/core/GameState.h"

#include <iostream>

namespace poorcraft::core {

namespace {
const char* toString(GameState state) {
    switch (state) {
    case GameState::MainMenu:
        return "MainMenu";
    case GameState::Loading:
        return "Loading";
    case GameState::Playing:
        return "Playing";
    case GameState::Paused:
        return "Paused";
    case GameState::Settings:
        return "Settings";
    case GameState::Quitting:
        return "Quitting";
    }
    return "Unknown";
}
} // namespace

GameStateManager::GameStateManager()
    : m_currentState(GameState::MainMenu)
    , m_previousState(GameState::MainMenu) {
}

GameState GameStateManager::getCurrentState() const noexcept {
    return m_currentState;
}

void GameStateManager::setState(GameState newState) {
    if (newState == m_currentState) {
        return;
    }

    if (!isTransitionAllowed(m_currentState, newState)) {
        std::cerr << "Invalid game state transition from " << toString(m_currentState) << " to "
                  << toString(newState) << '\n';
        return;
    }

    const auto previous = m_currentState;
    m_currentState = newState;
    emitStateChange(previous, m_currentState);
}

void GameStateManager::pushState(GameState state) {
    if (state == m_currentState) {
        return;
    }

    m_previousState = m_currentState;
    setState(state);
}

void GameStateManager::popState() {
    if (m_previousState == m_currentState) {
        return;
    }

    setState(m_previousState);
}

void GameStateManager::update() {
    // Placeholder for future per-frame state logic.
}

bool GameStateManager::shouldQuit() const noexcept {
    return m_currentState == GameState::Quitting;
}

void GameStateManager::setOnStateChangeCallback(StateChangeCallback callback) {
    m_onStateChange = std::move(callback);
}

bool GameStateManager::isTransitionAllowed(GameState from, GameState to) const noexcept {
    if (to == GameState::Quitting) {
        return true;
    }

    switch (from) {
    case GameState::MainMenu:
        return to == GameState::Loading || to == GameState::Settings;
    case GameState::Loading:
        return to == GameState::Playing || to == GameState::MainMenu;
    case GameState::Playing:
        return to == GameState::Paused || to == GameState::Settings || to == GameState::MainMenu;
    case GameState::Paused:
        return to == GameState::Playing || to == GameState::Settings || to == GameState::MainMenu;
    case GameState::Settings:
        return to == m_previousState || to == GameState::MainMenu || to == GameState::Playing
            || to == GameState::Paused;
    case GameState::Quitting:
        return false;
    }

    return false;
}

void GameStateManager::emitStateChange(GameState previous, GameState current) {
    if (m_onStateChange) {
        m_onStateChange(previous, current);
    }

    std::cout << "Game state changed from " << toString(previous) << " to " << toString(current)
              << '\n';
}

std::ostream& operator<<(std::ostream& out, GameState state) {
    return out << toString(state);
}

} // namespace poorcraft::core
