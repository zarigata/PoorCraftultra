#include "poorcraft/ui/HUD.h"

#include "poorcraft/core/Inventory.h"
#include "poorcraft/core/Timer.h"
#include "poorcraft/world/Block.h"
#include "poorcraft/world/ChunkManager.h"

#include <imgui.h>

#include <algorithm>
#include <array>
#include <string>

namespace poorcraft::ui
{
namespace
{
constexpr float kCrosshairSize = 12.0f;
constexpr float kCrosshairThickness = 2.0f;
constexpr float kHotbarSlotSize = 48.0f;
constexpr float kHotbarPadding = 6.0f;
constexpr float kHotbarBorderThickness = 2.0f;
} // namespace

HUD::HUD(core::Timer& timer, core::Inventory& inventory, world::ChunkManager& chunkManager)
    : m_timer(timer)
    , m_inventory(inventory)
    , m_chunkManager(chunkManager)
{}

void HUD::render()
{
    renderCrosshair();
    renderFPSOverlay();
    renderHotbar();
}

void HUD::renderCrosshair()
{
    ImGuiIO& io = ImGui::GetIO();
    const ImVec2 center(io.DisplaySize.x * 0.5f, io.DisplaySize.y * 0.5f);
    ImDrawList* drawList = ImGui::GetForegroundDrawList();

    const ImU32 color = m_isPaused ? IM_COL32(255, 255, 255, 80) : IM_COL32(255, 255, 255, 200);

    drawList->AddLine(
        ImVec2(center.x - kCrosshairSize, center.y),
        ImVec2(center.x + kCrosshairSize, center.y),
        color,
        kCrosshairThickness);
    drawList->AddLine(
        ImVec2(center.x, center.y - kCrosshairSize),
        ImVec2(center.x, center.y + kCrosshairSize),
        color,
        kCrosshairThickness);
}

void HUD::renderFPSOverlay()
{
    ImGui::SetNextWindowPos(ImVec2(12.0f, 12.0f), ImGuiCond_Always, ImVec2(0.0f, 0.0f));
    ImGui::SetNextWindowBgAlpha(m_isPaused ? 0.35f : 0.20f);

    constexpr ImGuiWindowFlags flags = ImGuiWindowFlags_NoDecoration | ImGuiWindowFlags_NoMove |
                                       ImGuiWindowFlags_NoSavedSettings | ImGuiWindowFlags_AlwaysAutoResize |
                                       ImGuiWindowFlags_NoFocusOnAppearing;

    if(ImGui::Begin("HUD_FPS", nullptr, flags))
    {
        const double fps = m_timer.getFPS();
        const int renderDistance = m_chunkManager.getRenderDistance();
        ImGui::Text("FPS: %.1f", fps);
        ImGui::Text("Render Distance: %d", renderDistance);
        ImGui::Text("Hotbar Slot: %d", m_inventory.getSelectedSlot() + 1);
    }
    ImGui::End();
}

void HUD::renderHotbar()
{
    ImGuiIO& io = ImGui::GetIO();
    const ImVec2 windowSize = ImVec2((kHotbarSlotSize + kHotbarPadding) * static_cast<float>(core::HOTBAR_SIZE) + kHotbarPadding, kHotbarSlotSize + kHotbarPadding * 3.0f);

    const ImVec2 pos(
        io.DisplaySize.x * 0.5f - windowSize.x * 0.5f,
        io.DisplaySize.y - windowSize.y - 24.0f);

    ImGui::SetNextWindowPos(pos, ImGuiCond_Always);
    ImGui::SetNextWindowSize(windowSize);
    ImGui::SetNextWindowBgAlpha(m_isPaused ? 0.35f : 0.20f);

    constexpr ImGuiWindowFlags flags = ImGuiWindowFlags_NoDecoration | ImGuiWindowFlags_NoMove |
                                       ImGuiWindowFlags_NoSavedSettings | ImGuiWindowFlags_NoFocusOnAppearing;

    if(ImGui::Begin("HUD_Hotbar", nullptr, flags))
    {
        ImGui::PushStyleVar(ImGuiStyleVar_ItemSpacing, ImVec2(kHotbarPadding, 0.0f));

        const auto& hotbar = m_inventory.getHotbar();
        const int selectedSlot = m_inventory.getSelectedSlot();

        for(int i = 0; i < core::HOTBAR_SIZE; ++i)
        {
            ImGui::BeginGroup();

            const bool selected = (i == selectedSlot);
            const ImVec4 borderColor = selected ? ImVec4(1.0f, 0.8f, 0.2f, 1.0f) : ImVec4(1.0f, 1.0f, 1.0f, 0.35f);
            const ImVec4 bgColor = selected ? ImVec4(0.3f, 0.25f, 0.05f, 0.6f) : ImVec4(0.1f, 0.1f, 0.1f, 0.4f);

            const ImVec2 cursorPos = ImGui::GetCursorScreenPos();
            ImDrawList* drawList = ImGui::GetWindowDrawList();
            const ImVec2 rectMin(cursorPos.x, cursorPos.y);
            const ImVec2 rectMax(cursorPos.x + kHotbarSlotSize, cursorPos.y + kHotbarSlotSize);

            drawList->AddRectFilled(rectMin, rectMax, ImGui::ColorConvertFloat4ToU32(bgColor), 6.0f);
            drawList->AddRect(rectMin, rectMax, ImGui::ColorConvertFloat4ToU32(borderColor), 6.0f, 0, kHotbarBorderThickness);

            ImGui::InvisibleButton("##HotbarSlot", ImVec2(kHotbarSlotSize, kHotbarSlotSize));

            const auto blockName = world::block::getName(hotbar[i]);
            const ImVec2 textSize = ImGui::CalcTextSize(blockName);
            ImGui::SetCursorScreenPos(ImVec2(
                cursorPos.x + (kHotbarSlotSize - textSize.x) * 0.5f,
                cursorPos.y + (kHotbarSlotSize - textSize.y) * 0.5f));
            ImGui::TextUnformatted(blockName);

            ImGui::EndGroup();
            if(i != core::HOTBAR_SIZE - 1)
            {
                ImGui::SameLine(0.0f, kHotbarPadding);
            }
        }

        ImGui::PopStyleVar();
    }
    ImGui::End();
}

} // namespace poorcraft::ui
