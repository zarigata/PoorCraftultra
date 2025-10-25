#pragma once

#include <cstdint>

namespace poorcraft::common {

enum class FaceDirection : std::uint8_t {
    PosX = 0,
    NegX,
    PosY,
    NegY,
    PosZ,
    NegZ,
    Count
};

} // namespace poorcraft::common
