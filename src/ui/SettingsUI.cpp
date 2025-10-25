#include "poorcraft/ui/SettingsUI.h"

#include "poorcraft/core/GameState.h"
#include "poorcraft/rendering/Renderer.h"
#include "poorcraft/ui/UIManager.h"
#include "poorcraft/world/ChunkManager.h"

#include <imgui.h>

namespace poorcraft::ui
{
SettingsUI::SettingsUI(core::GameStateManager& gameStateManager, rendering::Renderer& renderer, world::ChunkManager& chunkManager)
    : m_gameStateManager(gameStateManager)
    , m_renderer(renderer)
    , m_chunkManager(chunkManager)
{}

void SettingsUI::loadSettings()
{
    m_renderDistance = m_chunkManager.getRenderDistance();
    m_pendingRenderDistance = m_renderDistance;
    m_pendingVSync = m_renderer.isVSyncEnabled();
    m_settingsLoaded = true;
}

void SettingsUI::render()
{
    if(!m_settingsLoaded)
    {
        loadSettings();
    }

    const ImGuiViewport* viewport = ImGui::GetMainViewport();
    const ImVec2 center = viewport->GetCenter();
    ImGui::SetNextWindowPos(center, ImGuiCond_Always, ImVec2(0.5f, 0.5f));
    ImGui::SetNextWindowSize(ImVec2(420.0f, 260.0f));

    constexpr ImGuiWindowFlags kWindowFlags = ImGuiWindowFlags_NoResize | ImGuiWindowFlags_NoCollapse |
                                              ImGuiWindowFlags_NoSavedSettings | ImGuiWindowFlags_NoMove;

    if(ImGui::Begin("Settings", nullptr, kWindowFlags))
    {
        ImGui::TextUnformatted("Graphics");
        ImGui::Separator();

        ImGui::SliderInt("Render Distance", &m_pendingRenderDistance, 2, 16, "%d chunks");
        if(ImGui::IsItemDeactivatedAfterEdit())
        {
            m_pendingRenderDistance = std::clamp(m_pendingRenderDistance, 2, 32);
        }

        ImGui::Checkbox("VSync", &m_pendingVSync);

        ImGui::Spacing();
        if(ImGui::Button("Apply"))
        {
            applySettings();
            m_gameStateManager.popState();
        }
        ImGui::SameLine();
        if(ImGui::Button("Cancel"))
        {
            m_settingsLoaded = false;
            m_gameStateManager.popState();
        }
    }
    ImGui::End();
}

void SettingsUI::applySettings()
{
    if(!m_settingsLoaded)
    {
        return;
    }

    if(m_pendingRenderDistance != m_renderDistance)
    {
        m_chunkManager.setRenderDistance(m_pendingRenderDistance);
        m_renderDistance = m_pendingRenderDistance;
    }

    const bool currentVSync = m_renderer.isVSyncEnabled();
    if(m_pendingVSync != currentVSync)
    {
        m_renderer.setVSync(m_pendingVSync);
    }

    m_settingsLoaded = false;
}
} // namespace poorcraft::ui
