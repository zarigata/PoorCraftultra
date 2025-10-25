#include "poorcraft/world/TerrainGenerator.h"

#include "poorcraft/world/Block.h"

#include <algorithm>

namespace poorcraft::world {

namespace {
constexpr float BASE_HEIGHT = 64.0f;
constexpr float HEIGHT_VARIATION = 32.0f;
constexpr float FREQUENCY = 0.01f;
}

TerrainGenerator::TerrainGenerator(std::uint32_t seed)
    : m_seed(seed) {
    initializeNoise();
}

void TerrainGenerator::initializeNoise() {
    auto simplex = FastNoise::New<FastNoise::Simplex>();
    auto fractal = FastNoise::New<FastNoise::FractalFBm>();
    fractal->SetSource(simplex);
    fractal->SetOctaveCount(5);
    m_noiseGenerator = fractal;
}

float TerrainGenerator::getHeight(int worldX, int worldZ) const {
    if (!m_noiseGenerator) {
        return BASE_HEIGHT;
    }

    const float noise = m_noiseGenerator->GenSingle2D(static_cast<float>(worldX) * FREQUENCY, static_cast<float>(worldZ) * FREQUENCY, m_seed);
    const float height = BASE_HEIGHT + noise * HEIGHT_VARIATION;
    return std::clamp(height, 0.0f, static_cast<float>(CHUNK_SIZE_Y - 1));
}

void TerrainGenerator::generateChunk(Chunk& chunk) {
    const glm::vec3 origin = chunk.getWorldPosition();
    for (int z = 0; z < CHUNK_SIZE_Z; ++z) {
        for (int x = 0; x < CHUNK_SIZE_X; ++x) {
            const int worldX = static_cast<int>(origin.x) + x;
            const int worldZ = static_cast<int>(origin.z) + z;
            const float height = getHeight(worldX, worldZ);
            const int surfaceY = static_cast<int>(height);

            if (surfaceY < 0) {
                continue;
            }

            const int cappedSurfaceY = std::min(surfaceY, CHUNK_SIZE_Y - 1);
            const int dirtStartY = std::clamp(surfaceY - 3, 0, CHUNK_SIZE_Y - 1);

            for (int y = 0; y < dirtStartY && y < CHUNK_SIZE_Y; ++y) {
                chunk.setBlock(x, y, z, BlockType::Stone);
            }

            for (int y = dirtStartY; y < cappedSurfaceY && y < CHUNK_SIZE_Y; ++y) {
                chunk.setBlock(x, y, z, BlockType::Dirt);
            }

            if (cappedSurfaceY >= 0 && cappedSurfaceY < CHUNK_SIZE_Y) {
                chunk.setBlock(x, cappedSurfaceY, z, BlockType::Grass);
            }
        }
    }

    chunk.setGenerated(true);
    chunk.setDirty(true);
}

} // namespace poorcraft::world
