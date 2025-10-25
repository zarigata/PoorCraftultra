#include "poorcraft/core/Inventory.h"

#include <algorithm>

namespace poorcraft::core {

Inventory::Inventory() {
    m_hotbar.fill(world::BlockType::Air);
    m_hotbar[0] = world::BlockType::Grass;
    if (HOTBAR_SIZE > 1) {
        m_hotbar[1] = world::BlockType::Dirt;
    }
    if (HOTBAR_SIZE > 2) {
        m_hotbar[2] = world::BlockType::Stone;
    }
    m_selectedSlot = 0;
}

world::BlockType Inventory::getSelectedBlock() const {
    return m_hotbar[m_selectedSlot];
}

void Inventory::setSelectedSlot(int slot) {
    m_selectedSlot = std::clamp(slot, 0, HOTBAR_SIZE - 1);
}

int Inventory::getSelectedSlot() const noexcept {
    return m_selectedSlot;
}

void Inventory::nextSlot() {
    m_selectedSlot = (m_selectedSlot + 1) % HOTBAR_SIZE;
}

void Inventory::previousSlot() {
    m_selectedSlot = (m_selectedSlot - 1 + HOTBAR_SIZE) % HOTBAR_SIZE;
}

world::BlockType Inventory::getSlot(int index) const {
    if (index < 0 || index >= HOTBAR_SIZE) {
        return world::BlockType::Air;
    }
    return m_hotbar[index];
}

void Inventory::setSlot(int index, world::BlockType type) {
    if (index < 0 || index >= HOTBAR_SIZE) {
        return;
    }
    m_hotbar[index] = type;
}

const std::array<world::BlockType, HOTBAR_SIZE>& Inventory::getHotbar() const noexcept {
    return m_hotbar;
}

} // namespace poorcraft::core
