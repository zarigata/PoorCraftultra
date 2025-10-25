#include "poorcraft/world/ChunkMesher.h"

#include "poorcraft/world/Block.h"

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

} // namespace

void ChunkMesher::generateMesh(const Chunk& chunk, ChunkMesh& outMesh, const Chunk* neighbors[6]) {
    outMesh.clear();

    for (int direction = 0; direction < static_cast<int>(FaceDirection::Count); ++direction) {
        generateFaceQuads(chunk, static_cast<FaceDirection>(direction), outMesh, neighbors);
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
    const Chunk* neighbors[6]
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

                uvs[0] = glm::vec2(0.0f, 0.0f);
                uvs[1] = glm::vec2(static_cast<float>(width), 0.0f);
                uvs[2] = glm::vec2(static_cast<float>(width), static_cast<float>(height));
                uvs[3] = glm::vec2(0.0f, static_cast<float>(height));

                glm::vec3 normal = glm::vec3(dirVec);
                outMesh.addQuad(corners, normal, uvs);
            }
        }
    }
}

} // namespace poorcraft::world
