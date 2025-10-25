#pragma once

#include "poorcraft/common/FaceDirection.h"
#include "poorcraft/world/Chunk.h"
#include "poorcraft/world/ChunkMesh.h"

namespace poorcraft::rendering {
class TextureAtlas;
} // namespace poorcraft::rendering

namespace poorcraft::world {

class ChunkMesher {
public:
    using FaceDirection = common::FaceDirection;

    static void generateMesh(
        const Chunk& chunk,
        ChunkMesh& outMesh,
        const Chunk* neighbors[6],
        const rendering::TextureAtlas& atlas
    );

private:
    static bool shouldCreateFace(
        const Chunk& chunk,
        int x,
        int y,
        int z,
        int dx,
        int dy,
        int dz,
        const Chunk* neighbors[6]
    );

    static void generateFaceQuads(
        const Chunk& chunk,
        FaceDirection direction,
        ChunkMesh& outMesh,
        const Chunk* neighbors[6],
        const rendering::TextureAtlas& atlas
    );

    static float calculateVertexAO(
        const Chunk& chunk,
        const glm::ivec3& blockPos,
        const glm::ivec3& normal,
        const glm::ivec3& tangent,
        const glm::ivec3& bitangent,
        const Chunk* neighbors[6]
    );
};

} // namespace poorcraft::world
