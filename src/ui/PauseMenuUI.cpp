#include "poorcraft/ui/PauseMenuUI.h"

#include "poorcraft/core/GameState.h"

#include <imgui.h>

namespace poorcraft::ui
{
PauseMenuUI::PauseMenuUI(core::GameStateManager& gameStateManager)
    : m_gameStateManager(gameStateManager)
{}

void PauseMenuUI::render()
{
    const ImGuiViewport* viewport = ImGui::GetMainViewport();
    const ImVec2 center = viewport->GetCenter();
    ImGui::SetNextWindowPos(center, ImGuiCond_Always, ImVec2(0.5f, 0.5f));
    ImGui::SetNextWindowSize(ImVec2(320.0f, 220.0f));

    constexpr ImGuiWindowFlags kWindowFlags = ImGuiWindowFlags_NoResize | ImGuiWindowFlags_NoCollapse |
                                              ImGuiWindowFlags_NoSavedSettings | ImGuiWindowFlags_NoMove;

    if(ImGui::Begin("Pause Menu", nullptr, kWindowFlags))
    {
        ImGui::TextUnformatted("Paused");
        ImGui::Separator();

        if(ImGui::Button("Resume", ImVec2(-FLT_MIN, 0.0f)))
        {
            m_gameStateManager.popState();
        }

        ImGui::Spacing();
        if(ImGui::Button("Settings", ImVec2(-FLT_MIN, 0.0f)))
        {
            m_gameStateManager.pushState(core::GameState::Settings);
        }

        ImGui::Spacing();
        if(ImGui::Button("Quit to Main Menu", ImVec2(-FLT_MIN, 0.0f)))
        {
            m_gameStateManager.setState(core::GameState::MainMenu);
        }
    }
    ImGui::End();
}
} // namespace poorcraft::ui
