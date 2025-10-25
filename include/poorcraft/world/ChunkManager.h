#pragma once

#include "poorcraft/world/Chunk.h"
#include "poorcraft/world/ChunkMesh.h"
#include "poorcraft/world/TerrainGenerator.h"

#include "poorcraft/rendering/Renderer.h"

#include <glm/glm.hpp>

#include <memory>
#include <unordered_map>
#include <vector>

namespace poorcraft::world {

struct ChunkData {
    std::unique_ptr<Chunk> chunk;
    std::unique_ptr<ChunkMesh> mesh;
    rendering::Renderer::BufferHandle vertexBuffer{0};
    rendering::Renderer::BufferHandle indexBuffer{0};
};

class ChunkManager {
public:
    ChunkManager(rendering::Renderer& renderer, std::uint32_t seed);
    virtual ~ChunkManager() = default;

    void update(const glm::vec3& cameraPosition);
    void render();

    void setRenderDistance(int distance) noexcept { m_renderDistance = distance; }
    [[nodiscard]] int getRenderDistance() const noexcept { return m_renderDistance; }
    [[nodiscard]] std::size_t getLoadedChunkCount() const noexcept { return m_chunks.size(); }

    [[nodiscard]] virtual BlockType getBlockAt(const glm::vec3& worldPosition) const;
    [[nodiscard]] virtual BlockType getBlockAt(int blockX, int blockY, int blockZ) const;
    [[nodiscard]] virtual bool isBlockSolid(const glm::vec3& worldPosition) const;
    [[nodiscard]] virtual bool isBlockSolidAt(int blockX, int blockY, int blockZ) const;

private:
    [[nodiscard]] ChunkPosition worldToChunkPosition(const glm::vec3& worldPos) const;
    void loadChunk(const ChunkPosition& position);
    void unloadChunk(const ChunkPosition& position);
    void meshChunk(ChunkData& data);
    [[nodiscard]] std::vector<ChunkPosition> getChunksInRadius(const ChunkPosition& center, int radius) const;

    rendering::Renderer& m_renderer;
    std::unique_ptr<TerrainGenerator> m_terrainGenerator;
    std::unordered_map<ChunkPosition, ChunkData, ChunkPositionHash> m_chunks;
    int m_renderDistance{8};
    ChunkPosition m_lastCenter{};
    bool m_hasCenter{false};
};

} // namespace poorcraft::world
