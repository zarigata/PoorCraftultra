#include "poorcraft/ui/UIManager.h"

#include "poorcraft/core/GameState.h"
#include "poorcraft/core/Input.h"
#include "poorcraft/core/Inventory.h"
#include "poorcraft/core/Timer.h"
#include "poorcraft/rendering/Renderer.h"
#include "poorcraft/ui/HUD.h"
#include "poorcraft/ui/MainMenuUI.h"
#include "poorcraft/ui/PauseMenuUI.h"
#include "poorcraft/ui/SettingsUI.h"
#include "poorcraft/world/ChunkManager.h"

#include <imgui.h>
#include <backends/imgui_impl_sdl2.h>

#include <algorithm>
#include <cfloat>

namespace poorcraft::ui
{
UIManager::UIManager(
    core::GameStateManager& gameStateManager,
    rendering::Renderer& renderer,
    core::Input& input,
    core::Timer& timer,
    core::Inventory& inventory,
    world::ChunkManager& chunkManager)
    : m_gameStateManager(gameStateManager)
    , m_renderer(renderer)
    , m_input(input)
    , m_timer(timer)
    , m_inventory(inventory)
    , m_chunkManager(chunkManager)
{}

void UIManager::initialize()
{
    if(m_uiInitialized)
    {
        return;
    }

    m_mainMenuUI = std::make_unique<MainMenuUI>(m_gameStateManager);
    m_pauseMenuUI = std::make_unique<PauseMenuUI>(m_gameStateManager);
    m_settingsUI = std::make_unique<SettingsUI>(m_gameStateManager, m_renderer, m_chunkManager);
    m_hud = std::make_unique<HUD>(m_timer, m_inventory, m_chunkManager);

    m_gameStateManager.setOnStateChangeCallback([this](core::GameState previous, core::GameState current) {
        handleStateChange(previous, current);
    });

    handleStateChange(m_gameStateManager.getCurrentState(), m_gameStateManager.getCurrentState());
    m_uiInitialized = true;
}

void UIManager::shutdown()
{
    if(!m_uiInitialized)
    {
        return;
    }

    m_gameStateManager.setOnStateChangeCallback({});

    m_mainMenuUI.reset();
    m_pauseMenuUI.reset();
    m_settingsUI.reset();
    m_hud.reset();

    m_uiInitialized = false;
    m_loadingRequested = false;
}

void UIManager::processEvent(const SDL_Event& event)
{
    if(ImGui::GetCurrentContext() == nullptr)
    {
        return;
    }

    ImGui_ImplSDL2_ProcessEvent(&event);
}

void UIManager::render()
{
    if(!m_uiInitialized || ImGui::GetCurrentContext() == nullptr)
    {
        return;
    }

    updateLoadingState();

    m_renderer.beginUIPass();

    const auto state = m_gameStateManager.getCurrentState();

    switch(state)
    {
    case core::GameState::MainMenu:
        if(m_mainMenuUI)
        {
            m_mainMenuUI->render();
        }
        break;
    case core::GameState::Loading:
    {
        constexpr ImGuiWindowFlags flags = ImGuiWindowFlags_NoDecoration | ImGuiWindowFlags_NoMove | ImGuiWindowFlags_NoSavedSettings;
        const ImVec2 center = ImGui::GetMainViewport()->GetCenter();
        ImGui::SetNextWindowPos(center, ImGuiCond_Always, ImVec2(0.5f, 0.5f));
        ImGui::SetNextWindowSize(ImVec2(260.0f, 90.0f));
        if(ImGui::Begin("Loading", nullptr, flags))
        {
            ImGui::TextUnformatted("Loading world...");
            const float elapsed = m_loadingRequested ? std::chrono::duration<float>(std::chrono::steady_clock::now() - m_loadingStart).count() : 0.0f;
            const float progress = std::clamp(elapsed / 1.0f, 0.0f, 1.0f);
            ImGui::ProgressBar(progress, ImVec2(-FLT_MIN, 0.0f));
        }
        ImGui::End();
    }
        break;
    case core::GameState::Paused:
        if(m_pauseMenuUI)
        {
            m_pauseMenuUI->render();
        }
        [[fallthrough]];
    case core::GameState::Playing:
        if(m_hud)
        {
            m_hud->setPaused(state == core::GameState::Paused);
            m_hud->render();
        }
        break;
    case core::GameState::Settings:
        if(m_settingsUI)
        {
            m_settingsUI->render();
        }
        break;
    case core::GameState::Quitting:
        break;
    }

    m_renderer.renderUI();
}

bool UIManager::wantsCaptureMouse() const
{
    if(ImGui::GetCurrentContext() == nullptr)
    {
        return false;
    }
    return ImGui::GetIO().WantCaptureMouse;
}

bool UIManager::wantsCaptureKeyboard() const
{
    if(ImGui::GetCurrentContext() == nullptr)
    {
        return false;
    }
    return ImGui::GetIO().WantCaptureKeyboard;
}

void UIManager::handleStateChange(core::GameState previous, core::GameState current)
{
    m_previousState = previous;

    switch(current)
    {
    case core::GameState::Playing:
        m_input.setRelativeMouseMode(true);
        m_loadingRequested = false;
        break;
    case core::GameState::Paused:
    case core::GameState::MainMenu:
    case core::GameState::Settings:
    case core::GameState::Loading:
        m_input.setRelativeMouseMode(false);
        break;
    case core::GameState::Quitting:
        break;
    }

    if(current == core::GameState::Loading)
    {
        m_loadingRequested = true;
        m_loadingStart = std::chrono::steady_clock::now();
    }

    if(current == core::GameState::Settings && m_settingsUI)
    {
        m_settingsUI->loadSettings();
    }
}

void UIManager::updateLoadingState()
{
    if(!m_loadingRequested)
    {
        return;
    }

    if(m_gameStateManager.getCurrentState() != core::GameState::Loading)
    {
        m_loadingRequested = false;
        return;
    }

    constexpr std::chrono::milliseconds kMinimumLoadingDuration(750);
    const auto now = std::chrono::steady_clock::now();
    if(now - m_loadingStart >= kMinimumLoadingDuration)
    {
        m_loadingRequested = false;
        m_gameStateManager.setState(core::GameState::Playing);
    }
}

} // namespace poorcraft::ui
