#pragma once

#include "poorcraft/world/Chunk.h"

#include <cstdint>

#include <FastNoise/FastNoise.h>

namespace poorcraft::world {

class TerrainGenerator {
public:
    explicit TerrainGenerator(std::uint32_t seed);

    void generateChunk(Chunk& chunk);
    float getHeight(int worldX, int worldZ) const;

private:
    void initializeNoise();

    std::uint32_t m_seed{};
    FastNoise::SmartNode<> m_noiseGenerator;
};

} // namespace poorcraft::world
