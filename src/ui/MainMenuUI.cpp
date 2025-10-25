#include "poorcraft/ui/MainMenuUI.h"

#include "poorcraft/core/GameState.h"

#include <imgui.h>

namespace poorcraft::ui
{
MainMenuUI::MainMenuUI(core::GameStateManager& gameStateManager)
    : m_gameStateManager(gameStateManager)
{}

void MainMenuUI::render()
{
    const ImGuiViewport* viewport = ImGui::GetMainViewport();
    const ImVec2 center = viewport->GetCenter();
    ImGui::SetNextWindowPos(center, ImGuiCond_Always, ImVec2(0.5f, 0.5f));
    ImGui::SetNextWindowSize(ImVec2(360.0f, 260.0f));

    constexpr ImGuiWindowFlags kWindowFlags = ImGuiWindowFlags_NoResize | ImGuiWindowFlags_NoCollapse |
                                              ImGuiWindowFlags_NoSavedSettings | ImGuiWindowFlags_NoMove;

    if(ImGui::Begin("PoorCraft Main Menu", nullptr, kWindowFlags))
    {
        ImGui::TextUnformatted("PoorCraft");
        ImGui::Separator();

        ImGui::Spacing();
        if(ImGui::Button("Play", ImVec2(-FLT_MIN, 0.0f)))
        {
            m_gameStateManager.setState(core::GameState::Loading);
        }

        ImGui::Spacing();
        if(ImGui::Button("Settings", ImVec2(-FLT_MIN, 0.0f)))
        {
            m_gameStateManager.pushState(core::GameState::Settings);
        }

        ImGui::Spacing();
        if(ImGui::Button("Quit", ImVec2(-FLT_MIN, 0.0f)))
        {
            m_gameStateManager.setState(core::GameState::Quitting);
        }
    }
    ImGui::End();
}
} // namespace poorcraft::ui
