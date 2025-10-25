#pragma once

#include "poorcraft/world/Block.h"

#include <array>

namespace poorcraft::core {

inline constexpr int HOTBAR_SIZE = 9;

class Inventory {
public:
    Inventory();

    [[nodiscard]] world::BlockType getSelectedBlock() const;
    void setSelectedSlot(int slot);
    [[nodiscard]] int getSelectedSlot() const noexcept;

    void nextSlot();
    void previousSlot();

    [[nodiscard]] world::BlockType getSlot(int index) const;
    void setSlot(int index, world::BlockType type);

    [[nodiscard]] const std::array<world::BlockType, HOTBAR_SIZE>& getHotbar() const noexcept;

private:
    std::array<world::BlockType, HOTBAR_SIZE> m_hotbar{};
    int m_selectedSlot{0};
};

} // namespace poorcraft::core
