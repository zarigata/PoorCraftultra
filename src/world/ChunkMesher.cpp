#include "poorcraft/world/ChunkMesher.h"

#include "poorcraft/rendering/TextureAtlas.h"
#include "poorcraft/world/Block.h"

#include <algorithm>
#include <array>
#include <vector>

namespace poorcraft::world {
namespace {

struct FaceAxes {
    int uAxis{};
    int vAxis{};
    int wAxis{};
    glm::ivec3 normal{};
};

constexpr std::array<FaceAxes, static_cast<int>(ChunkMesher::FaceDirection::Count)> FACE_AXES{ {
    {2, 1, 0, {1, 0, 0}},  // +X
    {2, 1, 0, {-1, 0, 0}}, // -X
    {0, 2, 1, {0, 1, 0}},  // +Y
    {0, 2, 1, {0, -1, 0}}, // -Y
    {0, 1, 2, {0, 0, 1}},  // +Z
    {0, 1, 2, {0, 0, -1}}, // -Z
} };

glm::ivec3 directionVector(ChunkMesher::FaceDirection direction) {
    switch (direction) {
    case ChunkMesher::FaceDirection::PosX:
        return {1, 0, 0};
    case ChunkMesher::FaceDirection::NegX:
        return {-1, 0, 0};
    case ChunkMesher::FaceDirection::PosY:
        return {0, 1, 0};
    case ChunkMesher::FaceDirection::NegY:
        return {0, -1, 0};
    case ChunkMesher::FaceDirection::PosZ:
        return {0, 0, 1};
    case ChunkMesher::FaceDirection::NegZ:
        return {0, 0, -1};
    default:
        return {0, 0, 0};
    }
}

glm::ivec3 tangentVector(ChunkMesher::FaceDirection direction) {
    switch (direction) {
    case ChunkMesher::FaceDirection::PosX:
    case ChunkMesher::FaceDirection::NegX:
        return {0, 0, 1};
    case ChunkMesher::FaceDirection::PosY:
    case ChunkMesher::FaceDirection::NegY:
        return {1, 0, 0};
    case ChunkMesher::FaceDirection::PosZ:
    case ChunkMesher::FaceDirection::NegZ:
        return {1, 0, 0};
    default:
        return {0, 0, 0};
    }
}

glm::ivec3 bitangentVector(ChunkMesher::FaceDirection direction) {
    switch (direction) {
    case ChunkMesher::FaceDirection::PosX:
    case ChunkMesher::FaceDirection::NegX:
        return {0, 1, 0};
    case ChunkMesher::FaceDirection::PosY:
    case ChunkMesher::FaceDirection::NegY:
        return {0, 0, 1};
    case ChunkMesher::FaceDirection::PosZ:
    case ChunkMesher::FaceDirection::NegZ:
        return {0, 1, 0};
    default:
        return {0, 0, 0};
    }
}

int neighborIndexFromDirection(ChunkMesher::FaceDirection direction) {
    switch (direction) {
    case ChunkMesher::FaceDirection::PosX:
        return 0;
    case ChunkMesher::FaceDirection::NegX:
        return 1;
    case ChunkMesher::FaceDirection::PosY:
        return 2;
    case ChunkMesher::FaceDirection::NegY:
        return 3;
    case ChunkMesher::FaceDirection::PosZ:
        return 4;
    case ChunkMesher::FaceDirection::NegZ:
        return 5;
    default:
        return -1;
    }
}

inline bool blockVisible(BlockType type) {
    return block::isSolid(type);
}

BlockType sampleBlockWithNeighbors(
    const Chunk& chunk,
    const glm::ivec3& position,
    const Chunk* neighbors[6]
) {
    glm::ivec3 pos = position;
    const Chunk* currentChunk = &chunk;

    if (pos.y < 0 || pos.y >= CHUNK_SIZE_Y) {
        return BlockType::Air;
    }

    // Handle X axis crossing
    if (pos.x < 0) {
        if (const Chunk* neighbor = neighbors[neighborIndexFromDirection(ChunkMesher::FaceDirection::NegX)]) {
            currentChunk = neighbor;
            pos.x += CHUNK_SIZE_X;
        } else {
            return BlockType::Air;
        }
    } else if (pos.x >= CHUNK_SIZE_X) {
        if (const Chunk* neighbor = neighbors[neighborIndexFromDirection(ChunkMesher::FaceDirection::PosX)]) {
            currentChunk = neighbor;
            pos.x -= CHUNK_SIZE_X;
        } else {
            return BlockType::Air;
        }
    }

    // Handle Z axis crossing (including diagonals)
    if (pos.z < 0) {
        // If we already crossed X boundary, approximate diagonal by checking edge neighbors
        if (currentChunk != &chunk) {
            // We're in an X neighbor; check if both edge samples are solid
            const glm::ivec3 xEdge = position;
            const glm::ivec3 zEdge = {chunk.getPosition().x * CHUNK_SIZE_X + position.x, position.y, chunk.getPosition().z * CHUNK_SIZE_Z};
            const BlockType xSample = sampleBlockWithNeighbors(chunk, xEdge, neighbors);
            const BlockType zSample = sampleBlockWithNeighbors(chunk, {position.x, position.y, -1}, neighbors);
            // If both edges are solid, treat diagonal as solid to avoid under-occlusion
            if (block::isSolid(xSample) && block::isSolid(zSample)) {
                return xSample;
            }
            return BlockType::Air;
        }
        if (const Chunk* neighbor = neighbors[neighborIndexFromDirection(ChunkMesher::FaceDirection::NegZ)]) {
            currentChunk = neighbor;
            pos.z += CHUNK_SIZE_Z;
        } else {
            return BlockType::Air;
        }
    } else if (pos.z >= CHUNK_SIZE_Z) {
        if (currentChunk != &chunk) {
            // Diagonal approximation for +Z
            const glm::ivec3 xEdge = position;
            const glm::ivec3 zEdge = {chunk.getPosition().x * CHUNK_SIZE_X + position.x, position.y, chunk.getPosition().z * CHUNK_SIZE_Z + CHUNK_SIZE_Z};
            const BlockType xSample = sampleBlockWithNeighbors(chunk, xEdge, neighbors);
            const BlockType zSample = sampleBlockWithNeighbors(chunk, {position.x, position.y, CHUNK_SIZE_Z}, neighbors);
            if (block::isSolid(xSample) && block::isSolid(zSample)) {
                return xSample;
            }
            return BlockType::Air;
        }
        if (const Chunk* neighbor = neighbors[neighborIndexFromDirection(ChunkMesher::FaceDirection::PosZ)]) {
            currentChunk = neighbor;
            pos.z -= CHUNK_SIZE_Z;
        } else {
            return BlockType::Air;
        }
    }

    return currentChunk->getBlock(pos.x, pos.y, pos.z);
}

} // namespace

void ChunkMesher::generateMesh(
    const Chunk& chunk,
    ChunkMesh& outMesh,
    const Chunk* neighbors[6],
    const rendering::TextureAtlas& atlas
) {
    outMesh.clear();

    for (int direction = 0; direction < static_cast<int>(FaceDirection::Count); ++direction) {
        generateFaceQuads(chunk, static_cast<FaceDirection>(direction), outMesh, neighbors, atlas);
    }
}

bool ChunkMesher::shouldCreateFace(
    const Chunk& chunk,
    int x,
    int y,
    int z,
    int dx,
    int dy,
    int dz,
    const Chunk* neighbors[6]
) {
    const BlockType current = chunk.getBlock(x, y, z);
    if (!blockVisible(current)) {
        return false;
    }

    const int nx = x + dx;
    const int ny = y + dy;
    const int nz = z + dz;

    if (nx >= 0 && nx < CHUNK_SIZE_X && ny >= 0 && ny < CHUNK_SIZE_Y && nz >= 0 && nz < CHUNK_SIZE_Z) {
        const BlockType neighborType = chunk.getBlock(nx, ny, nz);
        return !block::isSolid(neighborType);
    }

    const ChunkMesher::FaceDirection direction =
        (dx == 1) ? FaceDirection::PosX : (dx == -1) ? FaceDirection::NegX :
        (dy == 1) ? FaceDirection::PosY : (dy == -1) ? FaceDirection::NegY :
        (dz == 1) ? FaceDirection::PosZ : FaceDirection::NegZ;

    const int neighborIndex = neighborIndexFromDirection(direction);
    if (neighborIndex < 0) {
        return true;
    }

    const Chunk* neighborChunk = neighbors[neighborIndex];
    if (!neighborChunk) {
        return true;
    }

    int neighborX = nx;
    int neighborY = ny;
    int neighborZ = nz;

    if (neighborX < 0) neighborX += CHUNK_SIZE_X;
    if (neighborX >= CHUNK_SIZE_X) neighborX -= CHUNK_SIZE_X;
    if (neighborZ < 0) neighborZ += CHUNK_SIZE_Z;
    if (neighborZ >= CHUNK_SIZE_Z) neighborZ -= CHUNK_SIZE_Z;

    const BlockType neighborType = neighborChunk->getBlock(neighborX, neighborY, neighborZ);
    return !block::isSolid(neighborType);
}

void ChunkMesher::generateFaceQuads(
    const Chunk& chunk,
    FaceDirection direction,
    ChunkMesh& outMesh,
    const Chunk* neighbors[6],
    const rendering::TextureAtlas& atlas
) {
    const auto axes = FACE_AXES[static_cast<int>(direction)];
    const glm::ivec3 dirVec = directionVector(direction);

    const int uLimit = (axes.uAxis == 0) ? CHUNK_SIZE_X : (axes.uAxis == 1) ? CHUNK_SIZE_Y : CHUNK_SIZE_Z;
    const int vLimit = (axes.vAxis == 0) ? CHUNK_SIZE_X : (axes.vAxis == 1) ? CHUNK_SIZE_Y : CHUNK_SIZE_Z;
    const int wLimit = (axes.wAxis == 0) ? CHUNK_SIZE_X : (axes.wAxis == 1) ? CHUNK_SIZE_Y : CHUNK_SIZE_Z;

    std::vector<BlockType> mask(static_cast<std::size_t>(uLimit * vLimit));

    for (int w = 0; w < wLimit; ++w) {
        for (int v = 0; v < vLimit; ++v) {
            for (int u = 0; u < uLimit; ++u) {
                glm::ivec3 pos{};
                pos[axes.uAxis] = u;
                pos[axes.vAxis] = v;
                pos[axes.wAxis] = w;

                const bool createFace = shouldCreateFace(
                    chunk,
                    pos.x,
                    pos.y,
                    pos.z,
                    dirVec.x,
                    dirVec.y,
                    dirVec.z,
                    neighbors
                );

                mask[u + v * uLimit] = createFace ? chunk.getBlock(pos.x, pos.y, pos.z) : BlockType::Air;
            }
        }

        for (int v = 0; v < vLimit; ++v) {
            for (int u = 0; u < uLimit;) {
                const BlockType blockType = mask[u + v * uLimit];
                if (blockType == BlockType::Air) {
                    ++u;
                    continue;
                }

                int width = 1;
                while (u + width < uLimit && mask[(u + width) + v * uLimit] == blockType) {
                    ++width;
                }

                int height = 1;
                bool done = false;
                while (v + height < vLimit && !done) {
                    for (int k = 0; k < width; ++k) {
                        if (mask[(u + k) + (v + height) * uLimit] != blockType) {
                            done = true;
                            break;
                        }
                    }
                    if (!done) {
                        ++height;
                    }
                }

                for (int dv = 0; dv < height; ++dv) {
                    for (int du = 0; du < width; ++du) {
                        mask[(u + du) + (v + dv) * uLimit] = BlockType::Air;
                    }
                }

                glm::vec3 corners[4];
                glm::vec2 uvs[4];

                for (int i = 0; i < 4; ++i) {
                    corners[i] = glm::vec3(0.0f);
                }

                auto setCorner = [&](int cornerIndex, int uOffset, int vOffset, int wOffset) {
                    glm::ivec3 cornerPos = {0, 0, 0};
                    cornerPos[axes.uAxis] = u + uOffset;
                    cornerPos[axes.vAxis] = v + vOffset;
                    cornerPos[axes.wAxis] = w + wOffset;
                    corners[cornerIndex] = glm::vec3(cornerPos);
                };

                const int front = (dirVec[axes.wAxis] > 0) ? 1 : 0;

                setCorner(0, 0, 0, front);
                setCorner(1, width, 0, front);
                setCorner(2, width, height, front);
                setCorner(3, 0, height, front);

                for (int i = 0; i < 4; ++i) {
                    corners[i] *= BLOCK_SIZE;
                }

                const rendering::AtlasRegion region = atlas.getRegion(blockType, direction);
                const glm::ivec3 tangentAxis = tangentVector(direction);
                const glm::ivec3 bitangentAxis = bitangentVector(direction);
                glm::vec3 normal = glm::vec3(dirVec);

                // Split merged quad into 1x1 tiles to ensure proper texture tiling
                for (int tileV = 0; tileV < height; ++tileV) {
                    for (int tileU = 0; tileU < width; ++tileU) {
                        glm::vec3 tileCorners[4];
                        glm::vec2 tileUvs[4];
                        float tileAoValues[4];

                        // Compute tile corners
                        auto setTileCorner = [&](int cornerIndex, int uOffset, int vOffset) {
                            glm::ivec3 cornerPos = {0, 0, 0};
                            cornerPos[axes.uAxis] = u + tileU + uOffset;
                            cornerPos[axes.vAxis] = v + tileV + vOffset;
                            cornerPos[axes.wAxis] = w + front;
                            tileCorners[cornerIndex] = glm::vec3(cornerPos) * BLOCK_SIZE;
                        };

                        setTileCorner(0, 0, 0);
                        setTileCorner(1, 1, 0);
                        setTileCorner(2, 1, 1);
                        setTileCorner(3, 0, 1);

                        // Full atlas region UVs for each 1x1 tile
                        tileUvs[0] = region.uvMin;
                        tileUvs[1] = glm::vec2(region.uvMax.x, region.uvMin.y);
                        tileUvs[2] = region.uvMax;
                        tileUvs[3] = glm::vec2(region.uvMin.x, region.uvMax.y);

                        // Calculate AO for each corner of the tile
                        for (int i = 0; i < 4; ++i) {
                            const bool positiveTangent = (i == 1 || i == 2);
                            const bool positiveBitangent = (i == 2 || i == 3);

                            glm::ivec3 vertexBlockPos{0};
                            vertexBlockPos[axes.uAxis] = u + tileU + (positiveTangent ? 1 : 0);
                            vertexBlockPos[axes.vAxis] = v + tileV + (positiveBitangent ? 1 : 0);
                            vertexBlockPos[axes.wAxis] = w;

                            const glm::ivec3 tangentDir = positiveTangent ? tangentAxis : -tangentAxis;
                            const glm::ivec3 bitangentDir = positiveBitangent ? bitangentAxis : -bitangentAxis;

                            tileAoValues[i] = calculateVertexAO(
                                chunk,
                                vertexBlockPos,
                                dirVec,
                                tangentDir,
                                bitangentDir,
                                neighbors
                            );
                        }

                        outMesh.addQuad(tileCorners, normal, tileUvs, tileAoValues);
                    }
                }
            }
        }
    }
}

float ChunkMesher::calculateVertexAO(
    const Chunk& chunk,
    const glm::ivec3& blockPos,
    const glm::ivec3& normal,
    const glm::ivec3& tangent,
    const glm::ivec3& bitangent,
    const Chunk* neighbors[6]
) {
    std::array<glm::ivec3, 8> offsets{
        tangent,
        -tangent,
        bitangent,
        -bitangent,
        tangent + bitangent,
        tangent - bitangent,
        -tangent + bitangent,
        -tangent - bitangent
    };

    int solidCount = 0;
    for (const auto& offset : offsets) {
        const glm::ivec3 samplePos = blockPos + offset;
        const BlockType sampleType = sampleBlockWithNeighbors(chunk, samplePos, neighbors);
        if (block::isSolid(sampleType)) {
            ++solidCount;
        }
    }

    const float occlusion = static_cast<float>(solidCount) / 8.0f;
    const float ao = 1.0f - (occlusion * 0.75f);
    return std::clamp(ao, 0.0f, 1.0f);
}

} // namespace poorcraft::world
