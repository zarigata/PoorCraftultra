#include "poorcraft/world/ChunkManager.h"

#include "poorcraft/rendering/Renderer.h"
#include "poorcraft/world/ChunkManager.h"
#include "poorcraft/rendering/TextureAtlas.h"
#include "poorcraft/world/ChunkMesher.h"

#include <algorithm>
#include <cmath>
#include <queue>

#include <glm/gtc/matrix_transform.hpp>

namespace poorcraft::world {
namespace {
int distanceSquared(const ChunkPosition& a, const ChunkPosition& b) {
    const int dx = a.x - b.x;
    const int dz = a.z - b.z;
    return dx * dx + dz * dz;
}

int floorDivInt(int value, int divisor) {
    int quotient = value / divisor;
    if (value < 0 && (value % divisor)) {
        --quotient;
    }
    return quotient;
}
}

ChunkManager::ChunkManager(
    rendering::Renderer& renderer,
    rendering::TextureAtlas& atlas,
    std::uint32_t seed
)
    : m_renderer(renderer)
    , m_textureAtlas(atlas)
    , m_terrainGenerator(std::make_unique<TerrainGenerator>(seed)) {}

void ChunkManager::update(const glm::vec3& cameraPosition) {
    const ChunkPosition center = worldToChunkPosition(cameraPosition);
    if (!m_hasCenter || center.x != m_lastCenter.x || center.z != m_lastCenter.z) {
        m_lastCenter = center;
        m_hasCenter = true;

        const auto desiredChunks = getChunksInRadius(center, m_renderDistance);
        std::unordered_map<ChunkPosition, bool, ChunkPositionHash> desiredMap;
        desiredMap.reserve(desiredChunks.size());
        for (const auto& pos : desiredChunks) {
            desiredMap[pos] = true;
            if (m_chunks.find(pos) == m_chunks.end()) {
                loadChunk(pos);
            }
        }

        std::vector<ChunkPosition> toUnload;
        for (const auto& entry : m_chunks) {
            if (desiredMap.find(entry.first) == desiredMap.end()) {
                toUnload.push_back(entry.first);
            }
        }

        for (const auto& pos : toUnload) {
            unloadChunk(pos);
        }
    }

    for (auto& [position, data] : m_chunks) {
        if (data.chunk && data.chunk->isDirty()) {
            meshChunk(data);
            data.chunk->setDirty(false);
        }
    }
}

void ChunkManager::render() {
    for (auto& [position, data] : m_chunks) {
        if (!data.mesh || data.mesh->isEmpty() || !data.mesh->isUploaded()) {
            continue;
        }

        const glm::mat4 model = glm::translate(glm::mat4(1.0f), data.chunk->getWorldPosition());
        m_renderer.drawIndexed(data.vertexBuffer, data.indexBuffer, static_cast<std::uint32_t>(data.mesh->getIndexCount()), model);
    }
}

ChunkPosition ChunkManager::worldToChunkPosition(const glm::vec3& worldPos) const {
    const int blockX = static_cast<int>(std::floor(worldPos.x / BLOCK_SIZE));
    const int blockZ = static_cast<int>(std::floor(worldPos.z / BLOCK_SIZE));

    return {floorDivInt(blockX, CHUNK_SIZE_X), floorDivInt(blockZ, CHUNK_SIZE_Z)};
}

BlockType ChunkManager::getBlockAt(const glm::vec3& worldPosition) const {
    const int blockX = static_cast<int>(std::floor(worldPosition.x / BLOCK_SIZE));
    const int blockY = static_cast<int>(std::floor(worldPosition.y / BLOCK_SIZE));
    const int blockZ = static_cast<int>(std::floor(worldPosition.z / BLOCK_SIZE));

    return getBlockAt(blockX, blockY, blockZ);
}

BlockType ChunkManager::getBlockAt(int blockX, int blockY, int blockZ) const {
    if (blockY < 0 || blockY >= CHUNK_SIZE_Y) {
        return BlockType::Air;
    }

    const ChunkPosition chunkPos{floorDivInt(blockX, CHUNK_SIZE_X), floorDivInt(blockZ, CHUNK_SIZE_Z)};
    const auto chunkIt = m_chunks.find(chunkPos);
    if (chunkIt == m_chunks.end() || !chunkIt->second.chunk) {
        // Treat missing chunks as empty space to keep player movement responsive while streaming; callers should ensure
        // nearby chunks are requested early enough to avoid noticeable gaps.
        return BlockType::Air;
    }

    const int chunkOriginX = chunkPos.x * CHUNK_SIZE_X;
    const int chunkOriginZ = chunkPos.z * CHUNK_SIZE_Z;
    const int localX = blockX - chunkOriginX;
    const int localY = blockY;
    const int localZ = blockZ - chunkOriginZ;

    return chunkIt->second.chunk->getBlock(localX, localY, localZ);
}

bool ChunkManager::isBlockSolid(const glm::vec3& worldPosition) const {
    const int blockX = static_cast<int>(std::floor(worldPosition.x / BLOCK_SIZE));
    const int blockY = static_cast<int>(std::floor(worldPosition.y / BLOCK_SIZE));
    const int blockZ = static_cast<int>(std::floor(worldPosition.z / BLOCK_SIZE));

    return isBlockSolidAt(blockX, blockY, blockZ);
}

bool ChunkManager::isBlockSolidAt(int blockX, int blockY, int blockZ) const {
    return block::isSolid(getBlockAt(blockX, blockY, blockZ));
}

bool ChunkManager::setBlockAt(const glm::vec3& worldPosition, BlockType type) {
    const int blockX = static_cast<int>(std::floor(worldPosition.x / BLOCK_SIZE));
    const int blockY = static_cast<int>(std::floor(worldPosition.y / BLOCK_SIZE));
    const int blockZ = static_cast<int>(std::floor(worldPosition.z / BLOCK_SIZE));
    return setBlockAt(blockX, blockY, blockZ, type);
}

bool ChunkManager::setBlockAt(int blockX, int blockY, int blockZ, BlockType type) {
    if (blockY < 0 || blockY >= CHUNK_SIZE_Y) {
        return false;
    }

    const ChunkPosition chunkPos{floorDivInt(blockX, CHUNK_SIZE_X), floorDivInt(blockZ, CHUNK_SIZE_Z)};
    const auto chunkIt = m_chunks.find(chunkPos);
    if (chunkIt == m_chunks.end() || !chunkIt->second.chunk || !chunkIt->second.chunk->isGenerated()) {
        return false;
    }

    const int chunkOriginX = chunkPos.x * CHUNK_SIZE_X;
    const int chunkOriginZ = chunkPos.z * CHUNK_SIZE_Z;
    const int localX = blockX - chunkOriginX;
    const int localZ = blockZ - chunkOriginZ;
    const int localY = blockY;

    if (localX < 0 || localX >= CHUNK_SIZE_X || localZ < 0 || localZ >= CHUNK_SIZE_Z) {
        return false;
    }

    chunkIt->second.chunk->setBlock(localX, localY, localZ, type);

    const auto markNeighborDirty = [this](const ChunkPosition& neighborPos) {
        const auto neighborIt = m_chunks.find(neighborPos);
        if (neighborIt != m_chunks.end() && neighborIt->second.chunk) {
            neighborIt->second.chunk->setDirty(true);
        }
    };

    if (localX == 0) {
        markNeighborDirty({chunkPos.x - 1, chunkPos.z});
    } else if (localX == CHUNK_SIZE_X - 1) {
        markNeighborDirty({chunkPos.x + 1, chunkPos.z});
    }

    if (localZ == 0) {
        markNeighborDirty({chunkPos.x, chunkPos.z - 1});
    } else if (localZ == CHUNK_SIZE_Z - 1) {
        markNeighborDirty({chunkPos.x, chunkPos.z + 1});
    }

    chunkIt->second.chunk->setDirty(true);
    return true;
}

void ChunkManager::loadChunk(const ChunkPosition& position) {
    ChunkData data{};
    data.chunk = std::make_unique<Chunk>(position);
    m_terrainGenerator->generateChunk(*data.chunk);
    data.chunk->setDirty(true);
    data.mesh = std::make_unique<ChunkMesh>();

    auto [it, inserted] = m_chunks.emplace(position, std::move(data));
    if (!inserted) {
        return;
    }

    const ChunkPosition neighborPositions[] = {
        {position.x + 1, position.z},
        {position.x - 1, position.z},
        {position.x, position.z + 1},
        {position.x, position.z - 1},
    };

    for (const auto& neighborPos : neighborPositions) {
        if (auto neighborIt = m_chunks.find(neighborPos); neighborIt != m_chunks.end() && neighborIt->second.chunk) {
            neighborIt->second.chunk->setDirty(true);
        }
    }
}

void ChunkManager::unloadChunk(const ChunkPosition& position) {
    auto it = m_chunks.find(position);
    if (it == m_chunks.end()) {
        return;
    }

    const ChunkPosition neighborPositions[] = {
        {position.x + 1, position.z},
        {position.x - 1, position.z},
        {position.x, position.z + 1},
        {position.x, position.z - 1},
    };

    for (const auto& neighborPos : neighborPositions) {
        if (auto neighborIt = m_chunks.find(neighborPos); neighborIt != m_chunks.end() && neighborIt->second.chunk) {
            neighborIt->second.chunk->setDirty(true);
        }
    }

    if (it->second.vertexBuffer != 0) {
        m_renderer.destroyBuffer(it->second.vertexBuffer);
    }
    if (it->second.indexBuffer != 0) {
        m_renderer.destroyBuffer(it->second.indexBuffer);
    }

    m_chunks.erase(it);
}

void ChunkManager::meshChunk(ChunkData& data) {
    const ChunkPosition pos = data.chunk->getPosition();
    const ChunkPosition neighborsPos[6] = {
        {pos.x + 1, pos.z},
        {pos.x - 1, pos.z},
        {pos.x, pos.z},
        {pos.x, pos.z},
        {pos.x, pos.z + 1},
        {pos.x, pos.z - 1},
    };

    const Chunk* neighborChunks[6]{};
    if (auto it = m_chunks.find(neighborsPos[0]); it != m_chunks.end()) {
        neighborChunks[0] = it->second.chunk.get();
    }
    if (auto it = m_chunks.find(neighborsPos[1]); it != m_chunks.end()) {
        neighborChunks[1] = it->second.chunk.get();
    }
    neighborChunks[2] = nullptr;
    neighborChunks[3] = nullptr;
    if (auto it = m_chunks.find(neighborsPos[4]); it != m_chunks.end()) {
        neighborChunks[4] = it->second.chunk.get();
    }
    if (auto it = m_chunks.find(neighborsPos[5]); it != m_chunks.end()) {
        neighborChunks[5] = it->second.chunk.get();
    }

    data.mesh->clear();
    ChunkMesher::generateMesh(*data.chunk, *data.mesh, neighborChunks, m_textureAtlas);

    if (data.mesh->isEmpty()) {
        return;
    }

    if (data.vertexBuffer != 0) {
        m_renderer.destroyBuffer(data.vertexBuffer);
    }
    if (data.indexBuffer != 0) {
        m_renderer.destroyBuffer(data.indexBuffer);
    }

    data.vertexBuffer = m_renderer.createVertexBuffer(data.mesh->getVertices().data(), data.mesh->getVertices().size() * sizeof(ChunkVertex));
    data.indexBuffer = m_renderer.createIndexBuffer(data.mesh->getIndices().data(), data.mesh->getIndices().size() * sizeof(std::uint32_t));
    data.mesh->setUploaded(true);
}

std::vector<ChunkPosition> ChunkManager::getChunksInRadius(const ChunkPosition& center, int radius) const {
    std::vector<ChunkPosition> positions;
    const int diameter = radius * 2 + 1;
    positions.reserve(static_cast<std::size_t>(diameter * diameter));
    for (int dz = -radius; dz <= radius; ++dz) {
        for (int dx = -radius; dx <= radius; ++dx) {
            positions.push_back({center.x + dx, center.z + dz});
        }
    }
    return positions;
}

} // namespace poorcraft::world
